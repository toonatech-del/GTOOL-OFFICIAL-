package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Database table: memories
 * Columns: id, type (note/pdf/image/voice), title, extracted_text, image_uri, created_at
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "type")
    val type: String, // "note", "pdf", "image", "voice"

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "extracted_text")
    val extractedText: String,

    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "size_text")
    val sizeText: String = "",

    @ColumnInfo(name = "summary")
    val summary: String = "",

    @ColumnInfo(name = "subtitle")
    val subtitle: String = "",

    @ColumnInfo(name = "tag")
    val tag: String? = null,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "is_synced_to_drive")
    val isSyncedToDrive: Boolean = false,

    @ColumnInfo(name = "drive_file_id")
    val driveFileId: String? = null,

    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null
)

/**
 * FTS4 virtual table: memories_fts
 * Standalone FTS table to avoid rowid synchronization constraints with String primary keys
 */
@Entity(tableName = "memories_fts")
@Fts4
data class MemoryFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int? = null,

    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "extracted_text")
    val extractedText: String
)
