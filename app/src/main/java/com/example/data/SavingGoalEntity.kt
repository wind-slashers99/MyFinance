package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String, // e.g. "Desember 2026" or "2026-12"
    val category: String // "KENDARAAN", "GADGET", "LIBURAN", "DANA_DARURAT", "LAINNYA"
)
