package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String, // "TOTAL" or specific category e.g., "Makanan"
    val amount: Double
)
