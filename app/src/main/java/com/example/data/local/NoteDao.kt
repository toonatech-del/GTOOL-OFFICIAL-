package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for note operations and multi-match semantic searches.
 */
@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Upsert
    suspend fun upsertMemory(memory: MemoryEntity): Long

    @Query("SELECT * FROM memories ORDER BY created_at DESC")
    suspend fun getAllNotes(): List<MemoryEntity>

    /**
     * Smart Multi-Match Database Query:
     * Matches ANY primary telecom/bill keyword AND ANY expiry keyword.
     */
    @Query(
        """
        SELECT * FROM memories 
        WHERE (
            title LIKE '%recharge%' OR title LIKE '%plan%' OR title LIKE '%validity%' OR title LIKE '%jio%' OR title LIKE '%airtel%' OR title LIKE '%vi%' OR title LIKE '%bsnl%'
            OR extracted_text LIKE '%recharge%' OR extracted_text LIKE '%plan%' OR extracted_text LIKE '%validity%' OR extracted_text LIKE '%jio%' OR extracted_text LIKE '%airtel%' OR extracted_text LIKE '%vi%' OR extracted_text LIKE '%bsnl%'
            OR summary LIKE '%recharge%' OR summary LIKE '%plan%' OR summary LIKE '%validity%' OR summary LIKE '%jio%' OR summary LIKE '%airtel%'
        ) AND (
            title LIKE '%expir%' OR title LIKE '%valid till%' OR title LIKE '%due%' OR title LIKE '%khtm%' OR title LIKE '%khatam%' OR title LIKE '%ending%'
            OR extracted_text LIKE '%expir%' OR extracted_text LIKE '%valid till%' OR extracted_text LIKE '%due%' OR extracted_text LIKE '%khtm%' OR extracted_text LIKE '%khatam%' OR extracted_text LIKE '%ending%'
            OR summary LIKE '%expir%' OR summary LIKE '%valid till%' OR summary LIKE '%due%' OR summary LIKE '%khtm%' OR summary LIKE '%khatam%'
        )
        ORDER BY created_at DESC LIMIT 5
        """
    )
    suspend fun searchTelecomExpiryNotes(): List<MemoryEntity>

    @Query(
        """
        SELECT * FROM memories 
        WHERE (
            title LIKE '%' || :keyword1 || '%' OR extracted_text LIKE '%' || :keyword1 || '%' OR summary LIKE '%' || :keyword1 || '%'
            OR title LIKE '%' || :keyword2 || '%' OR extracted_text LIKE '%' || :keyword2 || '%' OR summary LIKE '%' || :keyword2 || '%'
        )
        ORDER BY created_at DESC LIMIT 10
        """
    )
    suspend fun searchNotesByKeywords(keyword1: String, keyword2: String): List<MemoryEntity>

    // Tokenized wildcard search: matches individual tokens, ignoring symbols/asterisks
    @Query("""
        SELECT * FROM memories 
        WHERE (:w1 = '' OR title LIKE '%' || :w1 || '%' OR extracted_text LIKE '%' || :w1 || '%' OR summary LIKE '%' || :w1 || '%')
          AND (:w2 = '' OR title LIKE '%' || :w2 || '%' OR extracted_text LIKE '%' || :w2 || '%' OR summary LIKE '%' || :w2 || '%')
        ORDER BY created_at DESC
    """)
    suspend fun searchNotesFlexible(w1: String, w2: String): List<MemoryEntity>

    // Broad fallback search
    @Query("""
        SELECT * FROM memories 
        WHERE title LIKE '%' || :keyword || '%' OR extracted_text LIKE '%' || :keyword || '%' OR summary LIKE '%' || :keyword || '%'
        ORDER BY created_at DESC
    """)
    suspend fun searchNotesBroad(keyword: String): List<MemoryEntity>

    // Cleanup identical duplicate notes
    @Query("""
        DELETE FROM memories WHERE id NOT IN (
            SELECT MIN(id) FROM memories GROUP BY title, extracted_text
        )
    """)
    suspend fun removeDuplicates()
}
