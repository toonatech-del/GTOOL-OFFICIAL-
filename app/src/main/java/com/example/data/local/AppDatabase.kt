package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MemoryEntity::class,
        MemoryFtsEntity::class,
        UniversalSearchEntity::class,
        UniversalSearchFtsEntity::class,
        ReminderEntity::class,
        NotificationEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
    abstract fun noteDao(): NoteDao
    abstract fun universalSearchDao(): UniversalSearchDao
    abstract fun reminderDao(): ReminderDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gsdcall_ai_vault.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
