package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "PEMASUKAN" or "PENGELUARAN"
    val category: String,
    val timestamp: Long,
    val dateString: String, // "YYYY-MM-DD"
    val monthString: String, // "YYYY-MM" for reports
    val description: String,
    val syncStatus: String = "PENDING" // "PENDING", "SYNCED", "ONGOING"
)
