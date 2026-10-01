package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemories(memories: List<MemoryEntity>)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("SELECT * FROM memories ORDER BY created_at DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY created_at DESC")
    suspend fun getAllMemoriesList(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(fts: MemoryFtsEntity)

    @Query("SELECT * FROM memories WHERE is_pinned = 1 ORDER BY created_at DESC")
    fun getImportantMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: String): MemoryEntity?

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("UPDATE memories SET is_synced_to_drive = :isSynced, drive_file_id = :driveFileId WHERE id = :id")
    suspend fun updateSyncStatus(id: String, isSynced: Boolean, driveFileId: String?)

    /**
     * Offline Full-Text Search (FTS4 MATCH query):
     * Matches keywords using FTS4 syntax.
     * Supports wildcard prefix/suffix matching: MATCH :matchQuery
     */
    @Query(
        """
        SELECT memories.* 
        FROM memories 
        JOIN memories_fts ON memories.id = memories_fts.id 
        WHERE memories_fts MATCH :matchQuery
        ORDER BY memories.created_at DESC
        """
    )
    fun searchMemoriesFts(matchQuery: String): Flow<List<MemoryEntity>>

    /**
     * Fallback LIKE search in case of syntax or empty tokens
     */
    @Query(
        """
        SELECT * FROM memories 
        WHERE title LIKE '%' || :query || '%' 
           OR extracted_text LIKE '%' || :query || '%'
           OR summary LIKE '%' || :query || '%'
        ORDER BY created_at DESC
        """
    )
    fun searchMemoriesLike(query: String): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memories 
        WHERE (:w1 = '' OR title LIKE '%' || :w1 || '%' OR extracted_text LIKE '%' || :w1 || '%' OR summary LIKE '%' || :w1 || '%')
          AND (:w2 = '' OR title LIKE '%' || :w2 || '%' OR extracted_text LIKE '%' || :w2 || '%' OR summary LIKE '%' || :w2 || '%')
        ORDER BY created_at DESC
        """
    )
    fun searchMemoriesFlexible(w1: String, w2: String): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memories 
        WHERE (:w1 != '' AND (title LIKE '%' || :w1 || '%' OR extracted_text LIKE '%' || :w1 || '%' OR summary LIKE '%' || :w1 || '%'))
           OR (:w2 != '' AND (title LIKE '%' || :w2 || '%' OR extracted_text LIKE '%' || :w2 || '%' OR summary LIKE '%' || :w2 || '%'))
        ORDER BY created_at DESC
        """
    )
    fun searchMemoriesAny(w1: String, w2: String): Flow<List<MemoryEntity>>
}
