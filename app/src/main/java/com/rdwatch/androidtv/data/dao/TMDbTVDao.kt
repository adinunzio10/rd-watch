package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.TMDbTVEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for TMDb TV show data operations
 * Provides access to cached TV show data with proper reactive flows
 */
@Dao
interface TMDbTVDao {
    
    /**
     * Get TV show by ID
     * @param tvId TMDb TV show ID
     * @return Flow of TV show entity or null if not found
     */
    @Query("SELECT * FROM tmdb_tv_shows WHERE id = :tvId")
    fun getTVShowById(tvId: Int): Flow<TMDbTVEntity?>
    
    /**
     * Get TV show by ID (suspend version for repository use)
     * @param tvId TMDb TV show ID
     * @return TV show entity or null if not found
     */
    @Query("SELECT * FROM tmdb_tv_shows WHERE id = :tvId")
    suspend fun getTVShowByIdSuspend(tvId: Int): TMDbTVEntity?
    
    /**
     * Get all TV shows ordered by popularity
     * @return Flow of all cached TV shows
     */
    @Query("SELECT * FROM tmdb_tv_shows ORDER BY popularity DESC")
    fun getAllTVShows(): Flow<List<TMDbTVEntity>>
    
    /**
     * Get TV shows by genre
     * @param genreId Genre ID to filter by
     * @return Flow of TV shows with the specified genre
     */
    @Query("SELECT * FROM tmdb_tv_shows WHERE genreIds LIKE '%' || :genreId || '%' ORDER BY popularity DESC")
    fun getTVShowsByGenre(genreId: Int): Flow<List<TMDbTVEntity>>
    
    /**
     * Get TV shows by first air year
     * @param year First air year to filter by
     * @return Flow of TV shows from the specified year
     */
    @Query("SELECT * FROM tmdb_tv_shows WHERE firstAirDate LIKE :year || '%' ORDER BY popularity DESC")
    fun getTVShowsByYear(year: String): Flow<List<TMDbTVEntity>>
    
    /**
     * Get TV shows with minimum vote average
     * @param minRating Minimum vote average
     * @return Flow of TV shows above the rating threshold
     */
    @Query("SELECT * FROM tmdb_tv_shows WHERE voteAverage >= :minRating ORDER BY voteAverage DESC")
    fun getTVShowsByMinRating(minRating: Float): Flow<List<TMDbTVEntity>>
    
    /**
     * Get popular TV shows (top by popularity)
     * @param limit Maximum number of TV shows to return
     * @return Flow of popular TV shows
     */
    @Query("SELECT * FROM tmdb_tv_shows ORDER BY popularity DESC LIMIT :limit")
    fun getPopularTVShows(limit: Int = 20): Flow<List<TMDbTVEntity>>
    
    /**
     * Get top rated TV shows (top by vote average)
     * @param limit Maximum number of TV shows to return
     * @return Flow of top rated TV shows
     */
    @Query("SELECT * FROM tmdb_tv_shows ORDER BY voteAverage DESC LIMIT :limit")
    fun getTopRatedTVShows(limit: Int = 20): Flow<List<TMDbTVEntity>>
    
    /**
     * Get recently added TV shows
     * @param limit Maximum number of TV shows to return
     * @return Flow of recently added TV shows
     */
    @Query("SELECT * FROM tmdb_tv_shows ORDER BY lastUpdated DESC LIMIT :limit")
    fun getRecentTVShows(limit: Int = 20): Flow<List<TMDbTVEntity>>
    
    /**
     * Get currently airing TV shows
     * @param limit Maximum number of TV shows to return
     * @return Flow of currently airing TV shows
     */
    @Query("SELECT * FROM tmdb_tv_shows WHERE inProduction = 1 ORDER BY popularity DESC LIMIT :limit")
    fun getAiringTVShows(limit: Int = 20): Flow<List<TMDbTVEntity>>
    
    /**
     * Get TV shows by status
     * @param status Status to filter by (e.g., "Ended", "Returning Series")
     * @return Flow of TV shows with the specified status
     */
    @Query("SELECT * FROM tmdb_tv_shows WHERE status = :status ORDER BY popularity DESC")
    fun getTVShowsByStatus(status: String): Flow<List<TMDbTVEntity>>
    
    /**
     * Search TV shows by name
     * @param query Search query
     * @return Flow of TV shows matching the search query
     */
    @Query("SELECT * FROM tmdb_tv_shows WHERE name LIKE '%' || :query || '%' OR originalName LIKE '%' || :query || '%' ORDER BY popularity DESC")
    fun searchTVShows(query: String): Flow<List<TMDbTVEntity>>
    
    /**
     * Insert or update a TV show
     * @param tvShow TV show entity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTVShow(tvShow: TMDbTVEntity)
    
    /**
     * Insert or update multiple TV shows
     * @param tvShows List of TV show entities to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTVShows(tvShows: List<TMDbTVEntity>)
    
    /**
     * Update a TV show
     * @param tvShow TV show entity to update
     */
    @Update
    suspend fun updateTVShow(tvShow: TMDbTVEntity)
    
    /**
     * Delete a TV show
     * @param tvShow TV show entity to delete
     */
    @Delete
    suspend fun deleteTVShow(tvShow: TMDbTVEntity)
    
    /**
     * Delete TV show by ID
     * @param tvId TMDb TV show ID
     */
    @Query("DELETE FROM tmdb_tv_shows WHERE id = :tvId")
    suspend fun deleteTVShowById(tvId: Int)
    
    /**
     * Delete all TV shows
     */
    @Query("DELETE FROM tmdb_tv_shows")
    suspend fun deleteAllTVShows()
    
    /**
     * Delete TV shows older than specified timestamp
     * @param timestamp Cutoff timestamp
     */
    @Query("DELETE FROM tmdb_tv_shows WHERE lastUpdated < :timestamp")
    suspend fun deleteTVShowsOlderThan(timestamp: Long)
    
    /**
     * Get count of cached TV shows
     * @return Number of cached TV shows
     */
    @Query("SELECT COUNT(*) FROM tmdb_tv_shows")
    suspend fun getTVShowCount(): Int
    
    /**
     * Check if a TV show exists in cache
     * @param tvId TMDb TV show ID
     * @return True if TV show exists in cache
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tmdb_tv_shows WHERE id = :tvId)")
    suspend fun tvShowExists(tvId: Int): Boolean
    
    /**
     * Get TV shows that need refresh (older than specified timestamp)
     * @param timestamp Cutoff timestamp
     * @return Flow of TV shows that need refresh
     */
    @Query("SELECT * FROM tmdb_tv_shows WHERE lastUpdated < :timestamp")
    fun getTVShowsNeedingRefresh(timestamp: Long): Flow<List<TMDbTVEntity>>
    
    /**
     * Get TV show last updated timestamp
     * @param tvId TMDb TV show ID
     * @return Last updated timestamp or null if not found
     */
    @Query("SELECT lastUpdated FROM tmdb_tv_shows WHERE id = :tvId")
    suspend fun getTVShowLastUpdated(tvId: Int): Long?
}