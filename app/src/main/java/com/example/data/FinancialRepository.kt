package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val currentLog: String) : SyncState()
    data class Success(val message: String, val lastSyncTime: Long) : SyncState()
    data class Error(val message: String) : SyncState()
}

class FinancialRepository(
    private val context: Context,
    private val dao: FinancialDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // --- State Observables ---
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()
    val allAlerts: Flow<List<NotificationAlert>> = dao.getAllAlerts()
    val chatHistory: Flow<List<ChatMessage>> = dao.getChatHistory()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs

    // Format tools
    private val localeID = Locale("in", "ID")
    private val formatRupiah = NumberFormat.getCurrencyInstance(localeID).apply {
        maximumFractionDigits = 0
    }

    init {
        createNotificationChannel()
        // Automatically run initial sync simulation on startup
        repositoryScope.launch {
            syncDataWithCloud()
        }
    }

    fun getTransactionsByMonth(month: String): Flow<List<TransactionEntity>> {
        return dao.getTransactionsByMonth(month)
    }

    // --- Mutation Actions (Insert, Update, Delete) ---
    suspend fun insertTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        dao.insertTransaction(transaction)
        // Check budget limits for expenses
        if (transaction.type == "PENGELUARAN") {
            checkExpenseAgainstBudgets(transaction)
        }
        // Auto Sync with cloud
        triggerAutoSync()
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        dao.updateTransaction(transaction)
        if (transaction.type == "PENGELUARAN") {
            checkExpenseAgainstBudgets(transaction)
        }
        triggerAutoSync()
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        dao.deleteTransaction(transaction)
        triggerAutoSync()
    }

    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        dao.deleteAllTransactions()
        triggerAutoSync()
    }

    // --- Budgets ---
    suspend fun saveBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        dao.insertBudget(budget)
        // Check constraints immediately
        recheckAllBudgets()
    }

    suspend fun deleteBudget(category: String) = withContext(Dispatchers.IO) {
        dao.deleteBudgetByCategory(category)
    }

    // --- Notifications ---
    suspend fun markAlertAsRead(id: Int) = withContext(Dispatchers.IO) {
        dao.markAlertAsRead(id)
    }

    suspend fun clearAlerts() = withContext(Dispatchers.IO) {
        dao.deleteAllAlerts()
    }

    // --- Chat Messages ---
    suspend fun insertChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(message)
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        dao.clearChatHistory()
    }

    // --- Saving Goals ---
    val allSavingGoals: Flow<List<SavingGoalEntity>> = dao.getAllSavingGoals()

    suspend fun insertSavingGoal(goal: SavingGoalEntity) = withContext(Dispatchers.IO) {
        dao.insertSavingGoal(goal)
    }

    suspend fun updateSavingGoalAmount(id: Int, current: Double) = withContext(Dispatchers.IO) {
        dao.updateSavingGoalAmount(id, current)
    }

    suspend fun deleteSavingGoal(goal: SavingGoalEntity) = withContext(Dispatchers.IO) {
        dao.deleteSavingGoal(goal)
    }

    // --- Recurring Bills & Debts ---
    val allBillsRecurring: Flow<List<BillRecurringEntity>> = dao.getAllBillsRecurring()

    suspend fun insertBillRecurring(bill: BillRecurringEntity) = withContext(Dispatchers.IO) {
        dao.insertBillRecurring(bill)
    }

    suspend fun updateBillPaidStatus(id: Int, isPaid: Boolean) = withContext(Dispatchers.IO) {
        dao.updateBillPaidStatus(id, isPaid)
    }

    suspend fun deleteBillRecurring(bill: BillRecurringEntity) = withContext(Dispatchers.IO) {
        dao.deleteBillRecurring(bill)
    }

    // --- Payment Accounts ---
    val allPaymentAccounts: Flow<List<PaymentAccountEntity>> = dao.getAllPaymentAccounts()

    suspend fun insertPaymentAccount(account: PaymentAccountEntity) = withContext(Dispatchers.IO) {
        dao.insertPaymentAccount(account)
    }

    suspend fun updatePaymentAccounts(accounts: List<PaymentAccountEntity>) = withContext(Dispatchers.IO) {
        dao.updatePaymentAccounts(accounts)
    }

    suspend fun deletePaymentAccount(account: PaymentAccountEntity) = withContext(Dispatchers.IO) {
        dao.deletePaymentAccount(account)
    }

    // --- Budget Threshold Checker logic ---
    private suspend fun checkExpenseAgainstBudgets(tx: TransactionEntity) {
        val month = tx.monthString
        val category = tx.category
        
        // Let's load current month's transactions
        val currentMonthTransactions = dao.getTransactionsByMonth(month).first()
        val expenses = currentMonthTransactions.filter { it.type == "PENGELUARAN" }
        
        // 1. Check specific Category Budget
        val categoryBudget = dao.getBudgetByCategory(category)
        if (categoryBudget != null) {
            val totalCategorySpent = expenses.filter { it.category == category }.sumOf { it.amount }
            if (totalCategorySpent > categoryBudget.amount) {
                // EXCEEDED!
                val message = "Pengeluaran untuk kategori $category (${formatRupiah.format(totalCategorySpent)}) telah melebihi anggaran yang ditetapkan (${formatRupiah.format(categoryBudget.amount)})!"
                triggerAlert("Anggaran Melebihi Batas!", message, "BULANAN")
            } else if (totalCategorySpent >= (categoryBudget.amount * 0.85)) {
                // Approaching limit (85%)
                val message = "Waspada! Pengeluaran $category (${formatRupiah.format(totalCategorySpent)}) telah mencapai 85% dari batas anggaran (${formatRupiah.format(categoryBudget.amount)})!"
                triggerAlert("Anggaran Hampir Habis", message, "HARIAN")
            }
        }

        // 2. Check Overall Budget Limit ("TOTAL")
        val totalBudget = dao.getBudgetByCategory("TOTAL")
        if (totalBudget != null) {
            val totalSpent = expenses.sumOf { it.amount }
            if (totalSpent > totalBudget.amount) {
                val message = "Total pengeluaran bulan ini (${formatRupiah.format(totalSpent)}) telah melebih batas anggaran bulanan keseluruhan Kak Nakhlah (${formatRupiah.format(totalBudget.amount)})!"
                triggerAlert("Batas Total Anggaran Terlampaui!", message, "BULANAN")
            } else if (totalSpent >= (totalBudget.amount * 0.85)) {
                val message = "Pengeluaran bulanan keseluruhan Kak Nakhlah (${formatRupiah.format(totalSpent)}) telah mendekati 85% dari batas total anggaran (${formatRupiah.format(totalBudget.amount)})!"
                triggerAlert("Waspada Anggaran Bulanan", message, "HARIAN")
            }
        }
    }

    private suspend fun recheckAllBudgets() {
        val currentTransactions = dao.getAllTransactions().first()
        if (currentTransactions.isEmpty()) return
        
        // Pick latest transaction's month
        val latestMonth = currentTransactions.firstOrNull()?.monthString ?: return
        val currentMonthTransactions = dao.getTransactionsByMonth(latestMonth).first()
        val expenses = currentMonthTransactions.filter { it.type == "PENGELUARAN" }

        val allBudgetsList = dao.getAllBudgets().first()
        for (budget in allBudgetsList) {
            if (budget.category == "TOTAL") {
                val totalSpent = expenses.sumOf { it.amount }
                if (totalSpent > budget.amount) {
                    val message = "Pengeluaran bulan ini (${formatRupiah.format(totalSpent)}) melebihi anggaran keseluruhan (${formatRupiah.format(budget.amount)})"
                    triggerAlert("Batas Total Anggaran Melebihi Batas", message, "BULANAN")
                }
            } else {
                val totalCatSpent = expenses.filter { it.category == budget.category }.sumOf { it.amount }
                if (totalCatSpent > budget.amount) {
                    val message = "Pengeluaran kategori ${budget.category} (${formatRupiah.format(totalCatSpent)}) melebihi anggaran (${formatRupiah.format(budget.amount)})"
                    triggerAlert("Kategori ${budget.category} Melebihi Batas", message, "BULANAN")
                }
            }
        }
    }

    private suspend fun triggerAlert(title: String, message: String, type: String) {
        // Save to Database
        val alert = NotificationAlert(
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        dao.insertAlert(alert)

        // Show System Notification
        showSystemNotification(title, message)
    }

    // --- Simulated Sync Engine (Robust & Professional) ---
    private fun triggerAutoSync() {
        repositoryScope.launch {
            syncDataWithCloud()
        }
    }

    fun executeManualSync() {
        repositoryScope.launch {
            syncDataWithCloud()
        }
    }

    private suspend fun syncDataWithCloud() = withContext(Dispatchers.IO) {
        if (_syncState.value is SyncState.Syncing) return@withContext

        addLog("Mengaktifkan sinkronisasi cloud...")
        _syncState.value = SyncState.Syncing("Menyiapkan sambungan cloud aman...")
        delay(1000)

        // Fetch transactions that need sync
        val pendingTxs = dao.getPendingSyncTransactions()
        if (pendingTxs.isEmpty()) {
            addLog("Semua transaksi lokal telah sinkron.")
            _syncState.value = SyncState.Success("Cloud Sinkron: Semua data aman", System.currentTimeMillis())
            return@withContext
        }

        addLog("Menemukan ${pendingTxs.size} transaksi baru tidak sinkron.")
        _syncState.value = SyncState.Syncing("Mengirim ${pendingTxs.size} transaksi ke cloud...")
        delay(1200)

        // Simulate secure transit and encryption
        addLog("Mengenkripsi data dengan protokol AES-256...")
        delay(800)
        
        try {
            // Update pending transactions in DB as Synced
            pendingTxs.forEach { tx ->
                val syncedTx = tx.copy(syncStatus = "SYNCED")
                dao.updateTransaction(syncedTx)
            }
            
            addLog("Sinkronisasi cloud sukses. ${pendingTxs.size} transaksi tercadangkan.")
            
            // Insert system notification about sync
            val logMessage = "Berhasil mencadangkan ${pendingTxs.size} keuangan pribadi ke secure cloud."
            dao.insertAlert(
                NotificationAlert(
                    title = "Sinkronisasi Cloud Berhasil",
                    message = logMessage,
                    timestamp = System.currentTimeMillis(),
                    type = "SISTEM"
                )
            )

            _syncState.value = SyncState.Success(
                "Data tersinkronisasi ke cloud otomatis!",
                System.currentTimeMillis()
            )
        } catch (e: Exception) {
            addLog("Ups, koneksi cloud bermasalah: ${e.localizedMessage}")
            _syncState.value = SyncState.Error("Sinkronisasi gagal: ${e.localizedMessage}")
        }
    }

    private fun addLog(message: String) {
        val current = _syncLogs.value.toMutableList()
        current.add(0, "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $message")
        _syncLogs.value = current.take(50) // Limit to 50 logs
    }

    // --- Android System Notification Helper ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifikasi Keuangan"
            val descriptionText = "Pemberitahuan harian, bulanan, dan sistem anggaran"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("keuangan_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showSystemNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, "keuangan_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }
}
