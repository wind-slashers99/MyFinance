package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    fun getAllTransactionsListSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE monthString = :month ORDER BY timestamp DESC")
    fun getTransactionsByMonth(month: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncTransactions(): List<TransactionEntity>

    // --- Budgets ---
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getBudgetByCategory(category: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudgetByCategory(category: String)

    // --- Notification Alerts ---
    @Query("SELECT * FROM notification_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<NotificationAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: NotificationAlert): Long

    @Query("UPDATE notification_alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAlertAsRead(id: Int)

    @Query("DELETE FROM notification_alerts")
    suspend fun deleteAllAlerts()

    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatHistory(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()

    // --- Saving Goals ---
    @Query("SELECT * FROM saving_goals ORDER BY id DESC")
    fun getAllSavingGoals(): Flow<List<SavingGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingGoal(goal: SavingGoalEntity): Long

    @Query("UPDATE saving_goals SET currentAmount = :current WHERE id = :id")
    suspend fun updateSavingGoalAmount(id: Int, current: Double)

    @Delete
    suspend fun deleteSavingGoal(goal: SavingGoalEntity)

    // --- Recurring Bills and Debts ---
    @Query("SELECT * FROM bills_recurring ORDER BY id DESC")
    fun getAllBillsRecurring(): Flow<List<BillRecurringEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillRecurring(bill: BillRecurringEntity): Long

    @Query("UPDATE bills_recurring SET isPaid = :isPaid WHERE id = :id")
    suspend fun updateBillPaidStatus(id: Int, isPaid: Boolean)

    @Delete
    suspend fun deleteBillRecurring(bill: BillRecurringEntity)

    // --- Payment Accounts ---
    @Query("SELECT * FROM payment_accounts ORDER BY displayOrder ASC, id ASC")
    fun getAllPaymentAccounts(): Flow<List<PaymentAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentAccount(account: PaymentAccountEntity): Long

    @Update
    suspend fun updatePaymentAccounts(accounts: List<PaymentAccountEntity>)

    @Delete
    suspend fun deletePaymentAccount(account: PaymentAccountEntity)
}
