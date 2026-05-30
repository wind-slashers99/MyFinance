package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills_recurring")
data class BillRecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "HUTANG", "PIUTANG", "TAGIHAN"
    val dueDate: String, // e.g. "Setiap Tanggal 5", "Tanggal 12 Juni"
    val isPaid: Boolean,
    val notes: String = ""
)
