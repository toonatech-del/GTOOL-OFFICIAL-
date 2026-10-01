package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val scheduledTime: Long, // Epoch millis
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
