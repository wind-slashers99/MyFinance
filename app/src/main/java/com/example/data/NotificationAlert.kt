package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_alerts")
data class NotificationAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: String, // "HARIAN", "BULANAN", "SISTEM"
    val isRead: Boolean = false
)
