package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SearchHistoryDao {
    
    @Query("SELECT * FROM search_history WHERE user_id = :userId ORDER BY search_date DESC")
    fun getSearchHistoryByUser(userId: Long): Flow<List<SearchHistoryEntity>>
    
    @Query("SELECT * FROM search_history WHERE user_id = :userId ORDER BY search_date DESC LIMIT :limit")
    fun getRecentSearchHistory(userId: Long, limit: Int = 50): Flow<List<SearchHistoryEntity>>
    
    @Query("SELECT * FROM search_history WHERE user_id = :userId AND search_type = :searchType ORDER BY search_date DESC")
    fun getSearchHistoryByType(userId: Long, searchType: String): Flow<List<SearchHistoryEntity>>
    
    @Query("""
        SELECT * FROM search_history 
        WHERE user_id = :userId 
        AND search_query LIKE '%' || :query || '%'
        ORDER BY search_date DESC
    """)
    fun searchInHistory(userId: Long, query: String): Flow<List<SearchHistoryEntity>>
    
    @Query("""
        SELECT DISTINCT search_query FROM search_history 
        WHERE user_id = :userId 
        AND search_query LIKE '%' || :partialQuery || '%'
        ORDER BY search_date DESC 
        LIMIT :limit
    """)
    suspend fun getSearchSuggestions(userId: Long, partialQuery: String, limit: Int = 10): List<String>
    
    @Query("""
        SELECT search_query FROM search_history 
        WHERE user_id = :userId 
        GROUP BY search_query 
        ORDER BY COUNT(*) DESC, search_date DESC 
        LIMIT :limit
    """)
    suspend fun getPopularSearchQueries(userId: Long, limit: Int = 20): List<String>
    
    @Query("""
        SELECT * FROM search_history 
        WHERE user_id = :userId 
        AND search_date BETWEEN :startDate AND :endDate 
        ORDER BY search_date DESC
    """)
    fun getSearchHistoryByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SearchHistoryEntity>>
    
    @Query("SELECT DISTINCT search_type FROM search_history WHERE user_id = :userId ORDER BY search_type")
    suspend fun getSearchTypesByUser(userId: Long): List<String>
    
    @Query("SELECT COUNT(*) FROM search_history WHERE user_id = :userId")
    suspend fun getSearchCountByUser(userId: Long): Int
    
    @Query("SELECT AVG(response_time_ms) FROM search_history WHERE user_id = :userId AND response_time_ms IS NOT NULL")
    suspend fun getAverageResponseTimeByUser(userId: Long): Float?
    
    @Query("SELECT COUNT(*) FROM search_history WHERE user_id = :userId AND search_query = :query")
    suspend fun getSearchCountForQuery(userId: Long, query: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(searchHistory: SearchHistoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistoryBatch(searchHistories: List<SearchHistoryEntity>)
    
    @Update
    suspend fun updateSearchHistory(searchHistory: SearchHistoryEntity)
    
    @Delete
    suspend fun deleteSearchHistory(searchHistory: SearchHistoryEntity)
    
    @Query("DELETE FROM search_history WHERE search_id = :searchId")
    suspend fun deleteSearchHistoryById(searchId: Long)
    
    @Query("DELETE FROM search_history WHERE user_id = :userId")
    suspend fun deleteAllSearchHistoryForUser(userId: Long)
    
    @Query("DELETE FROM search_history WHERE user_id = :userId AND search_query = :query")
    suspend fun deleteSearchHistoryForQuery(userId: Long, query: String)
    
    @Query("DELETE FROM search_history WHERE search_date < :cutoffDate")
    suspend fun deleteOldSearchHistory(cutoffDate: Date)
    
    @Query("""
        DELETE FROM search_history 
        WHERE search_id NOT IN (
            SELECT search_id FROM search_history 
            WHERE user_id = :userId 
            ORDER BY search_date DESC 
            LIMIT :keepCount
        ) AND user_id = :userId
    """)
    suspend fun cleanupOldSearchHistory(userId: Long, keepCount: Int = 1000)
}