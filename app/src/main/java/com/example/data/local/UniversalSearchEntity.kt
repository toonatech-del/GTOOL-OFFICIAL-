package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "universal_search_index")
data class UniversalSearchEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // format: "pdf-{id}", "note-{id}", "image-{id}", "invoice-{id}", "resizer-{id}"

    @ColumnInfo(name = "module")
    val module: String, // "PDF", "Note", "Image", "Invoice", "Resizer"

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content_text")
    val contentText: String, // full raw text / transcription / body

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}", // extra data: tags, dates, line items, parameters, DPI, etc.

    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "universal_search_index_fts")
@Fts4
data class UniversalSearchFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int? = null,

    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content_text")
    val contentText: String,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String
)
