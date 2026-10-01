package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UniversalSearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchItem(entity: UniversalSearchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(fts: UniversalSearchFtsEntity)

    @Query("DELETE FROM universal_search_index WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM universal_search_index WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): UniversalSearchEntity?

    @Query("SELECT * FROM universal_search_index ORDER BY created_at DESC")
    fun getAllItems(): Flow<List<UniversalSearchEntity>>

    /**
     * FTS MATCH Prefix query
     */
    @Query(
        """
        SELECT universal_search_index.* 
        FROM universal_search_index 
        JOIN universal_search_index_fts ON universal_search_index.id = universal_search_index_fts.id 
        WHERE universal_search_index_fts MATCH :ftsQuery
        ORDER BY universal_search_index.created_at DESC
        """
    )
    fun searchFts(ftsQuery: String): Flow<List<UniversalSearchEntity>>

    /**
     * Fallback LIKE query
     */
    @Query(
        """
        SELECT * FROM universal_search_index 
        WHERE title LIKE '%' || :query || '%' 
           OR content_text LIKE '%' || :query || '%' 
           OR metadata_json LIKE '%' || :query || '%'
        ORDER BY created_at DESC
        """
    )
    fun searchLike(query: String): Flow<List<UniversalSearchEntity>>

    @Query(
        """
        SELECT * FROM universal_search_index 
        WHERE (:w1 = '' OR title LIKE '%' || :w1 || '%' OR content_text LIKE '%' || :w1 || '%')
          AND (:w2 = '' OR title LIKE '%' || :w2 || '%' OR content_text LIKE '%' || :w2 || '%')
        ORDER BY created_at DESC
        """
    )
    fun searchLikeFlexible(w1: String, w2: String): Flow<List<UniversalSearchEntity>>

    @Query(
        """
        SELECT * FROM universal_search_index 
        WHERE (:w1 != '' AND (title LIKE '%' || :w1 || '%' OR content_text LIKE '%' || :w1 || '%' OR metadata_json LIKE '%' || :w1 || '%'))
           OR (:w2 != '' AND (title LIKE '%' || :w2 || '%' OR content_text LIKE '%' || :w2 || '%' OR metadata_json LIKE '%' || :w2 || '%'))
        ORDER BY created_at DESC
        """
    )
    fun searchLikeAny(w1: String, w2: String): Flow<List<UniversalSearchEntity>>
}
