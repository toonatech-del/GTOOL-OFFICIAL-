package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val type: String, // e.g., "reminder", "system"
    val timestamp: Long,
    val isRead: Boolean = false,
    val scheduledTime: Long? = null,
    val reminderId: Long? = null
)
