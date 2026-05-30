package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FinancialRepository(application, database.financialDao())

    // Observables from Repo
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts: StateFlow<List<NotificationAlert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatHistory: StateFlow<List<ChatMessage>> = repository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncState: StateFlow<SyncState> = repository.syncState
    val syncLogs: StateFlow<List<String>> = repository.syncLogs
 
    // --- Opsi 1 & 3 State Flows ---
    val allSavingGoals: StateFlow<List<SavingGoalEntity>> = repository.allSavingGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
 
    val allBillsRecurring: StateFlow<List<BillRecurringEntity>> = repository.allBillsRecurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPaymentAccounts: StateFlow<List<PaymentAccountEntity>> = repository.allPaymentAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filters
    private val _selectedMonth = MutableStateFlow(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
    val selectedMonth: StateFlow<String> = _selectedMonth

    private val _selectedTypeFilter = MutableStateFlow("SEMUA") // "SEMUA", "PEMASUKAN", "PENGELUARAN"
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter

    private val _selectedCategoryFilter = MutableStateFlow("SEMUA")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter

    // Dynamic Lists based on filter
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedMonth,
        _selectedTypeFilter,
        _selectedCategoryFilter
    ) { txs, month, type, cat ->
        txs.filter { tx ->
            val matchesMonth = tx.monthString == month
            val matchesType = type == "SEMUA" || tx.type == type
            val matchesCategory = cat == "SEMUA" || tx.category == cat
            matchesMonth && matchesType && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial calculations for current month
    val currentMonthSummary = combine(allTransactions, _selectedMonth) { txs, month ->
        val currentMonthTxs = txs.filter { it.monthString == month }
        val income = currentMonthTxs.filter { it.type == "PEMASUKAN" }.sumOf { it.amount }
        val expense = currentMonthTxs.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }
        val balance = income - expense
        Triple(income, expense, balance)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0.0, 0.0))

    // AI advisor states
    private val _aiRecommendation = MutableStateFlow<String>("Sedang menganalisa keuangan Anda...")
    val aiRecommendation: StateFlow<String> = _aiRecommendation

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    private val _isChatbotResponding = MutableStateFlow(false)
    val isChatbotResponding: StateFlow<Boolean> = _isChatbotResponding

    init {
        // Run an initial analysis when transactions are updated
        viewModelScope.launch {
            allTransactions.collect {
                generateFinancialAnalysisReport()
            }
        }
    }

    // Set Month Filters
    fun setMonthFilter(month: String) {
        _selectedMonth.value = month
    }

    fun setTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    // --- Core Mutations ---
    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        timestamp: Long,
        description: String
    ) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(timestamp))

        val transaction = TransactionEntity(
            title = title,
            amount = amount,
            type = type,
            category = category,
            timestamp = timestamp,
            dateString = sdfDate,
            monthString = sdfMonth,
            description = description
        )

        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun saveBudget(category: String, amount: Double) {
        viewModelScope.launch {
            repository.saveBudget(BudgetEntity(category, amount))
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
        }
    }

    fun markAlertAsRead(id: Int) {
        viewModelScope.launch {
            repository.markAlertAsRead(id)
        }
    }

    fun clearAlerts() {
        viewModelScope.launch {
            repository.clearAlerts()
        }
    }

    fun triggerManualSync() {
        repository.executeManualSync()
    }
 
    // --- Opsi 1 and 3 Mutators ---
    fun insertSavingGoal(title: String, targetAmount: Double, currentAmount: Double, targetDate: String, category: String) {
        viewModelScope.launch {
            repository.insertSavingGoal(SavingGoalEntity(title = title, targetAmount = targetAmount, currentAmount = currentAmount, targetDate = targetDate, category = category))
        }
    }
 
    fun updateSavingGoalAmount(id: Int, current: Double) {
        viewModelScope.launch {
            repository.updateSavingGoalAmount(id, current)
        }
    }
 
    fun deleteSavingGoal(goal: SavingGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingGoal(goal)
        }
    }
 
    fun insertBillRecurring(title: String, amount: Double, type: String, dueDate: String, notes: String) {
        viewModelScope.launch {
            repository.insertBillRecurring(BillRecurringEntity(title = title, amount = amount, type = type, dueDate = dueDate, isPaid = false, notes = notes))
        }
    }
 
    fun updateBillPaidStatus(id: Int, isPaid: Boolean) {
        viewModelScope.launch {
            repository.updateBillPaidStatus(id, isPaid)
        }
    }
 
    fun deleteBillRecurring(bill: BillRecurringEntity) {
        viewModelScope.launch {
            repository.deleteBillRecurring(bill)
        }
    }

    // --- Payment Accounts ---
    fun insertPaymentAccount(institutionName: String, accountType: String, accountNumber: String, ownerName: String, startingBalance: Double) {
        viewModelScope.launch {
            val currentMaxOrder = allPaymentAccounts.value.maxOfOrNull { it.displayOrder } ?: 0
            repository.insertPaymentAccount(
                PaymentAccountEntity(
                    institutionName = institutionName,
                    accountType = accountType,
                    accountNumber = accountNumber,
                    ownerName = ownerName,
                    currentBalance = startingBalance,
                    displayOrder = currentMaxOrder + 1
                )
            )
        }
    }

    fun updatePaymentAccountsOrder(accounts: List<PaymentAccountEntity>) {
        viewModelScope.launch {
            repository.updatePaymentAccounts(accounts)
        }
    }

    fun movePaymentAccountUp(account: PaymentAccountEntity) {
        val list = allPaymentAccounts.value
        val index = list.indexOfFirst { it.id == account.id }
        if (index > 0) {
            viewModelScope.launch {
                val prev = list[index - 1]
                val tempOrder = account.displayOrder
                val updatedAcc = account.copy(displayOrder = prev.displayOrder)
                val updatedPrev = prev.copy(displayOrder = tempOrder)
                repository.updatePaymentAccounts(listOf(updatedAcc, updatedPrev))
            }
        }
    }

    fun movePaymentAccountDown(account: PaymentAccountEntity) {
        val list = allPaymentAccounts.value
        val index = list.indexOfFirst { it.id == account.id }
        if (index != -1 && index < list.size - 1) {
            viewModelScope.launch {
                val next = list[index + 1]
                val tempOrder = account.displayOrder
                val updatedAcc = account.copy(displayOrder = next.displayOrder)
                val updatedNext = next.copy(displayOrder = tempOrder)
                repository.updatePaymentAccounts(listOf(updatedAcc, updatedNext))
            }
        }
    }

    fun deletePaymentAccount(account: PaymentAccountEntity) {
        viewModelScope.launch {
            repository.deletePaymentAccount(account)
        }
    }

    // --- Chat Room Bot Assistant with persistent chat messages ---
    fun sendMessageToAI(userText: String) {
        if (userText.trim().isEmpty()) return

        val userMessage = ChatMessage(text = userText, sender = "USER")
        viewModelScope.launch {
            repository.insertChatMessage(userMessage)
            _isChatbotResponding.value = true

            // Generate contextual prompt
            val contextPrompt = buildChatPromptWithFinancialContext(userText)
            val aiResponseText = GeminiApiClient.generateResponse(
                prompt = contextPrompt,
                systemInstruction = "Kamu adalah MyFinance AI, asisten keuangan personal yang asyik, santai, gaul, dan super akrab buat Kak Yuki. " +
                        "Panggil pengguna dengan sapaan akrab 'Kak Yuki' atau 'Yuki'. Gaya bicaramu harus santai, friendly, pakai bahasa santai sehari-hari di Indonesia (seperti 'nih', 'lho', 'bisa kok', 'yuk', 'sip', 'deh', dsb). Sama sekali JANGAN formal, kaku, atau menggunakan bahasa korporat yang membosankan (hindari kata 'Anda', gunakan 'kamu' atau 'Kak Yuki'). " +
                        "Tugasmu membantu Kak Yuki mengelola keuangannya secara menyenangkan. Hubungkan analisismu secara langsung dengan data rekening aktif, target budget bulanan baru, atau transaksi yang terekam. Jawablah sesuai konteks dengan format yang gampang dibaca, seru, dan penuh semangat!"
            )

            val aiMessage = ChatMessage(text = aiResponseText, sender = "AI")
            repository.insertChatMessage(aiMessage)
            _isChatbotResponding.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    // --- AI Savings Advisor Engine ---
    fun generateFinancialAnalysisReport() {
        viewModelScope.launch {
            if (_isAnalyzing.value) return@launch
            _isAnalyzing.value = true

            val txsList = allTransactions.value
            val budgetList = allBudgets.value
            val month = _selectedMonth.value

            if (txsList.none { it.monthString == month }) {
                _aiRecommendation.value = "Belum ada transaksi di bulan $month. Silakan tambahkan pemasukan atau pengeluaran untuk memulai analisa keuangan AI."
                _isAnalyzing.value = false
                return@launch
            }

            val currentMonthTxs = txsList.filter { it.monthString == month }
            val income = currentMonthTxs.filter { it.type == "PEMASUKAN" }.sumOf { it.amount }
            val expense = currentMonthTxs.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }

            // Group expense by category
            val categoriesSpent = currentMonthTxs.filter { it.type == "PENGELUARAN" }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val localeID = Locale("in", "ID")
            val rupiah = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

            val promptBuilder = StringBuilder()
            promptBuilder.append("Berikut adalah riwayat transaksi keuangan bulanan saya untuk periode $month:\n")
            promptBuilder.append("- Total Pemasukan: ${rupiah.format(income)}\n")
            promptBuilder.append("- Total Pengeluaran: ${rupiah.format(expense)}\n")
            promptBuilder.append("\nPengeluaran per Kategori:\n")
            categoriesSpent.forEach { (cat, amt) ->
                promptBuilder.append("- $cat: ${rupiah.format(amt)}\n")
            }

            if (budgetList.isNotEmpty()) {
                promptBuilder.append("\nAnggaran Batas Kategori:\n")
                budgetList.forEach { budget ->
                    promptBuilder.append("- ${budget.category}: ${rupiah.format(budget.amount)}\n")
                }
            }

            promptBuilder.append("\nTolong analisis riwayat pengeluaran aku tersebut ya. Berikan 3 poin konkret, spesifik, dan taktis tentang cara menghemat uang berdasarkan kategori dari transaksi real di atas, terus hitung berapa nominal rupiah potensi yang bisa dihemat. Jangan berikan saran umum abstrak, sesuaikan langsung sama angka pengeluaran aku di atas ya! Jawab dengan gaya santai, seru, bersahabat, dan panggil aku Kak Yuki.")

            val response = GeminiApiClient.generateResponse(
                prompt = promptBuilder.toString(),
                systemInstruction = "Kamu adalah perencana keuangan handal sekaligus teman bercerita yang seru dan asyik buat Kak Yuki. " +
                        "Analisis angka pengeluaran Kak Yuki dengan teliti, hitung porsi pengeluaran terbesarnya, lalu berikan 3 tips khusus berhemat yang asyik, menyemangati, dan gaul santai (JANGAN formal, kaku, atau membosankan)."
            )

            _aiRecommendation.value = response
            _isAnalyzing.value = false
        }
    }

    private fun buildChatPromptWithFinancialContext(userQuestion: String): String {
        val txsList = allTransactions.value.take(15) // Recent 15 transactions
        val budgetList = allBudgets.value
        val accountsList = allPaymentAccounts.value
        val summary = currentMonthSummary.value
        val localeID = Locale("in", "ID")
        val rupiah = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

        val context = StringBuilder()
        context.append("--- FINANCIAL CONTEXT ---\n")
        context.append("Periode Bulan: ${_selectedMonth.value}\n")
        context.append("Total Pemasukan Bulan ini: ${rupiah.format(summary.first)}\n")
        context.append("Total Pengeluaran Bulan ini: ${rupiah.format(summary.second)}\n")
        context.append("Sisa Saldo: ${rupiah.format(summary.third)}\n")

        if (accountsList.isNotEmpty()) {
            context.append("Daftar Akun & E-Wallet Terdaftar:\n")
            accountsList.forEach { acc ->
                context.append("- [${acc.accountType}] ${acc.institutionName} (${acc.accountNumber}) atas nama ${acc.ownerName}\n")
            }
        }

        if (budgetList.isNotEmpty()) {
            context.append("Anggaran:\n")
            budgetList.forEach { context.append("- ${it.category}: Limit ${rupiah.format(it.amount)}\n") }
        }

        if (txsList.isNotEmpty()) {
            context.append("Transaksi Terbaru:\n")
            txsList.forEach { tx ->
                context.append("- [${tx.type}] ${tx.title} kateg: ${tx.category}, nominal: ${rupiah.format(tx.amount)}\n")
            }
        }
        context.append("\nPertanyaan atau keluhan pengguna: \"$userQuestion\"\n")
        context.append("Bantu pengguna sesuai dengan data transaksi di atas, beri angka perbandingan jika mereka bertanya tentang keuangan mereka, jawab secara solutif.")

        return context.toString()
    }

    // --- PDF Downloader Trigger ---
    fun getPdfReportFile(context: Application): File? {
        val month = _selectedMonth.value
        val txs = filteredTransactions.value
        val budgetList = allBudgets.value.associate { it.category to it.amount }
        val tips = _aiRecommendation.value

        return PdfExporter.exportToPdf(
            context = context,
            monthString = month,
            transactions = txs,
            budgets = budgetList,
            aiTips = tips
        )
    }

    // --- Backup & Restore (JSON Import/Export) ---
    fun exportDatabaseToJson(context: Context, onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rootJson = org.json.JSONObject()
                
                // Transactions
                val txArray = org.json.JSONArray()
                allTransactions.value.forEach {
                    val j = org.json.JSONObject().apply {
                        put("title", it.title)
                        put("amount", it.amount)
                        put("type", it.type)
                        put("category", it.category)
                        put("timestamp", it.timestamp)
                        put("dateString", it.dateString)
                        put("monthString", it.monthString)
                        put("description", it.description)
                    }
                    txArray.put(j)
                }
                rootJson.put("transactions", txArray)
                
                // Budgets
                val budgetArray = org.json.JSONArray()
                allBudgets.value.forEach {
                    val j = org.json.JSONObject().apply {
                        put("category", it.category)
                        put("amount", it.amount)
                    }
                    budgetArray.put(j)
                }
                rootJson.put("budgets", budgetArray)

                // Saving Goals
                val goalArray = org.json.JSONArray()
                allSavingGoals.value.forEach {
                    val j = org.json.JSONObject().apply {
                        put("title", it.title)
                        put("targetAmount", it.targetAmount)
                        put("currentAmount", it.currentAmount)
                        put("targetDate", it.targetDate)
                        put("category", it.category)
                    }
                    goalArray.put(j)
                }
                rootJson.put("saving_goals", goalArray)

                // Bills Recurring
                val billArray = org.json.JSONArray()
                allBillsRecurring.value.forEach {
                    val j = org.json.JSONObject().apply {
                        put("title", it.title)
                        put("amount", it.amount)
                        put("type", it.type)
                        put("dueDate", it.dueDate)
                        put("isPaid", it.isPaid)
                        put("notes", it.notes)
                    }
                    billArray.put(j)
                }
                rootJson.put("bills", billArray)

                // Payment Accounts
                val accArray = org.json.JSONArray()
                allPaymentAccounts.value.forEach {
                    val j = org.json.JSONObject().apply {
                        put("institutionName", it.institutionName)
                        put("accountType", it.accountType)
                        put("accountNumber", it.accountNumber)
                        put("ownerName", it.ownerName)
                        put("currentBalance", it.currentBalance)
                        put("displayOrder", it.displayOrder)
                    }
                    accArray.put(j)
                }
                rootJson.put("accounts", accArray)

                // Write to cache directory
                val backupFile = File(context.cacheDir, "MyFinance_Backup.json")
                backupFile.writeText(rootJson.toString(2))
                
                withContext(Dispatchers.Main) {
                    onSuccess(backupFile)
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    onError(ex.localizedMessage ?: "Unknown error during export")
                }
            }
        }
    }

    fun importDatabaseFromJson(context: Context, jsonUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(jsonUri)?.use { stream ->
                    stream.bufferedReader().use { it.readText() }
                } ?: throw Exception("Gagal membaca file backup")
                
                val rootJson = org.json.JSONObject(jsonString)
                val db = AppDatabase.getDatabase(context)
                val dao = db.financialDao()
                
                val existingGoals = allSavingGoals.value
                val existingBills = allBillsRecurring.value
                val existingAccounts = allPaymentAccounts.value

                val txsListToInsert = mutableListOf<TransactionEntity>()
                val budgetsListToInsert = mutableListOf<BudgetEntity>()
                val goalsListToInsert = mutableListOf<SavingGoalEntity>()
                val billsListToInsert = mutableListOf<BillRecurringEntity>()
                val accountsListToInsert = mutableListOf<PaymentAccountEntity>()

                val txArray = rootJson.optJSONArray("transactions")
                if (txArray != null) {
                    for (i in 0 until txArray.length()) {
                        val item = txArray.getJSONObject(i)
                        txsListToInsert.add(
                            TransactionEntity(
                                title = item.getString("title"),
                                amount = item.getDouble("amount"),
                                type = item.getString("type"),
                                category = item.getString("category"),
                                timestamp = item.getLong("timestamp"),
                                dateString = item.getString("dateString"),
                                monthString = item.getString("monthString"),
                                description = item.optString("description", "")
                            )
                        )
                    }
                }

                val budgetArray = rootJson.optJSONArray("budgets")
                if (budgetArray != null) {
                    for (i in 0 until budgetArray.length()) {
                        val item = budgetArray.getJSONObject(i)
                        budgetsListToInsert.add(
                            BudgetEntity(
                                category = item.getString("category"),
                                amount = item.getDouble("amount")
                            )
                        )
                    }
                }

                val goalArray = rootJson.optJSONArray("saving_goals")
                if (goalArray != null) {
                    for (i in 0 until goalArray.length()) {
                        val item = goalArray.getJSONObject(i)
                        goalsListToInsert.add(
                            SavingGoalEntity(
                                title = item.getString("title"),
                                targetAmount = item.getDouble("targetAmount"),
                                currentAmount = item.getDouble("currentAmount"),
                                targetDate = item.getString("targetDate"),
                                category = item.getString("category")
                            )
                        )
                    }
                }

                val billArray = rootJson.optJSONArray("bills")
                if (billArray != null) {
                    for (i in 0 until billArray.length()) {
                        val item = billArray.getJSONObject(i)
                        billsListToInsert.add(
                            BillRecurringEntity(
                                title = item.getString("title"),
                                amount = item.getDouble("amount"),
                                type = item.getString("type"),
                                dueDate = item.getString("dueDate"),
                                isPaid = item.getBoolean("isPaid"),
                                notes = item.optString("notes", "")
                            )
                        )
                    }
                }

                val accArray = rootJson.optJSONArray("accounts")
                if (accArray != null) {
                    for (i in 0 until accArray.length()) {
                        val item = accArray.getJSONObject(i)
                        accountsListToInsert.add(
                            PaymentAccountEntity(
                                institutionName = item.getString("institutionName"),
                                accountType = item.getString("accountType"),
                                accountNumber = item.getString("accountNumber"),
                                ownerName = item.getString("ownerName"),
                                currentBalance = item.getDouble("currentBalance"),
                                displayOrder = item.optInt("displayOrder", 0)
                            )
                        )
                    }
                }

                // Execute database changes block sequentially
                dao.deleteAllTransactions()
                txsListToInsert.forEach { dao.insertTransaction(it) }
                
                budgetsListToInsert.forEach { dao.insertBudget(it) }
                
                existingGoals.forEach { dao.deleteSavingGoal(it) }
                goalsListToInsert.forEach { dao.insertSavingGoal(it) }
                
                existingBills.forEach { dao.deleteBillRecurring(it) }
                billsListToInsert.forEach { dao.insertBillRecurring(it) }
                
                existingAccounts.forEach { dao.deletePaymentAccount(it) }
                accountsListToInsert.forEach { dao.insertPaymentAccount(it) }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    onError(ex.localizedMessage ?: "Unknown error during import")
                }
            }
        }
    }
}
