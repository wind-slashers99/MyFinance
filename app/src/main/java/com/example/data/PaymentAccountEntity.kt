package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_accounts")
data class PaymentAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val institutionName: String, // e.g. "Bank BCA", "GoPay", "Dana"
    val accountType: String, // "BANK" or "EWALLET"
    val accountNumber: String, // e.g. "1234567890" or "08123456789"
    val ownerName: String, // Owner name
    val currentBalance: Double = 0.0, // Optional starting balance
    val displayOrder: Int = 0 // Sorting sequence
)
