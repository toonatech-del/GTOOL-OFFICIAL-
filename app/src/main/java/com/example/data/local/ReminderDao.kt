package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY scheduledTime ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY scheduledTime ASC")
    suspend fun getActiveRemindersList(): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY scheduledTime ASC")
    suspend fun getAllRemindersList(): List<ReminderEntity>

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markAsCompleted(id: Long)

    @Query("SELECT COUNT(*) FROM reminders WHERE description = :title AND ABS(scheduledTime - :triggerTime) < 86400000")
    suspend fun hasExistingReminder(title: String, triggerTime: Long): Int

    @Query("""
        DELETE FROM reminders 
        WHERE id NOT IN (
            SELECT MIN(id) 
            FROM reminders 
            GROUP BY description, scheduledTime
        )
    """)
    suspend fun deletePendingDuplicates()

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
}
