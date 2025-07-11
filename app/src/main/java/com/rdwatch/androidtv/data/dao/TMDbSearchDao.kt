package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for TMDb search, credits, recommendations, and other auxiliary data operations
 * Provides access to cached search results and related data
 */
@Dao
interface TMDbSearchDao {
    
    // Search Results Operations
    
    /**
     * Get search results by query and type
     * @param query Search query
     * @param searchType Type of search (movie, tv, multi)
     * @param page Page number
     * @return Flow of search result entity or null if not found
     */
    @Query("SELECT * FROM tmdb_search_results WHERE query = :query AND searchType = :searchType AND page = :page")
    fun getSearchResults(query: String, searchType: String, page: Int): Flow<TMDbSearchResultEntity?>
    
    /**
     * Get search results by query and type (suspend version)
     * @param query Search query
     * @param searchType Type of search (movie, tv, multi)
     * @param page Page number
     * @return Search result entity or null if not found
     */
    @Query("SELECT * FROM tmdb_search_results WHERE query = :query AND searchType = :searchType AND page = :page")
    suspend fun getSearchResultsSuspend(query: String, searchType: String, page: Int): TMDbSearchResultEntity?
    
    /**
     * Get all search results for a query
     * @param query Search query
     * @param searchType Type of search (movie, tv, multi)
     * @return Flow of all search results for the query
     */
    @Query("SELECT * FROM tmdb_search_results WHERE query = :query AND searchType = :searchType ORDER BY page")
    fun getAllSearchResults(query: String, searchType: String): Flow<List<TMDbSearchResultEntity>>
    
    /**
     * Get recent search queries
     * @param limit Maximum number of queries to return
     * @return Flow of recent search queries
     */
    @Query("SELECT DISTINCT query FROM tmdb_search_results ORDER BY lastUpdated DESC LIMIT :limit")
    fun getRecentSearchQueries(limit: Int = 10): Flow<List<String>>
    
    /**
     * Insert or update search results
     * @param searchResult Search result entity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResult(searchResult: TMDbSearchResultEntity)
    
    /**
     * Delete search results by query
     * @param query Search query
     */
    @Query("DELETE FROM tmdb_search_results WHERE query = :query")
    suspend fun deleteSearchResults(query: String)
    
    /**
     * Delete all search results
     */
    @Query("DELETE FROM tmdb_search_results")
    suspend fun deleteAllSearchResults()
    
    /**
     * Delete search results older than specified timestamp
     * @param timestamp Cutoff timestamp
     */
    @Query("DELETE FROM tmdb_search_results WHERE lastUpdated < :timestamp")
    suspend fun deleteSearchResultsOlderThan(timestamp: Long)
    
    // Credits Operations
    
    /**
     * Get credits by content ID and type
     * @param contentId Content ID (movie or TV show)
     * @param contentType Content type (movie or tv)
     * @return Flow of credits entity or null if not found
     */
    @Query("SELECT * FROM tmdb_credits WHERE contentId = :contentId AND contentType = :contentType")
    fun getCredits(contentId: Int, contentType: String): Flow<TMDbCreditsEntity?>
    
    /**
     * Get credits by content ID and type (suspend version)
     * @param contentId Content ID (movie or TV show)
     * @param contentType Content type (movie or tv)
     * @return Credits entity or null if not found
     */
    @Query("SELECT * FROM tmdb_credits WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun getCreditsSuspend(contentId: Int, contentType: String): TMDbCreditsEntity?
    
    /**
     * Insert or update credits
     * @param credits Credits entity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredits(credits: TMDbCreditsEntity)
    
    /**
     * Delete credits by content ID and type
     * @param contentId Content ID
     * @param contentType Content type
     */
    @Query("DELETE FROM tmdb_credits WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun deleteCredits(contentId: Int, contentType: String)
    
    /**
     * Delete all credits
     */
    @Query("DELETE FROM tmdb_credits")
    suspend fun deleteAllCredits()
    
    /**
     * Delete credits older than specified timestamp
     * @param timestamp Cutoff timestamp
     */
    @Query("DELETE FROM tmdb_credits WHERE lastUpdated < :timestamp")
    suspend fun deleteCreditsOlderThan(timestamp: Long)
    
    // Recommendations Operations
    
    /**
     * Get recommendations by content ID, type, and recommendation type
     * @param contentId Content ID
     * @param contentType Content type (movie or tv)
     * @param recommendationType Type of recommendation (recommendations, similar, popular, etc.)
     * @param page Page number
     * @return Flow of recommendation entity or null if not found
     */
    @Query("SELECT * FROM tmdb_recommendations WHERE contentId = :contentId AND contentType = :contentType AND recommendationType = :recommendationType AND page = :page")
    fun getRecommendations(contentId: Int, contentType: String, recommendationType: String, page: Int): Flow<TMDbRecommendationEntity?>
    
    /**
     * Get recommendations by content ID, type, and recommendation type (suspend version)
     * @param contentId Content ID
     * @param contentType Content type (movie or tv)
     * @param recommendationType Type of recommendation (recommendations, similar, popular, etc.)
     * @param page Page number
     * @return Recommendation entity or null if not found
     */
    @Query("SELECT * FROM tmdb_recommendations WHERE contentId = :contentId AND contentType = :contentType AND recommendationType = :recommendationType AND page = :page")
    suspend fun getRecommendationsSuspend(contentId: Int, contentType: String, recommendationType: String, page: Int): TMDbRecommendationEntity?
    
    /**
     * Get all recommendations for content
     * @param contentId Content ID
     * @param contentType Content type (movie or tv)
     * @param recommendationType Type of recommendation
     * @return Flow of all recommendation pages
     */
    @Query("SELECT * FROM tmdb_recommendations WHERE contentId = :contentId AND contentType = :contentType AND recommendationType = :recommendationType ORDER BY page")
    fun getAllRecommendations(contentId: Int, contentType: String, recommendationType: String): Flow<List<TMDbRecommendationEntity>>
    
    /**
     * Insert or update recommendations
     * @param recommendations Recommendation entity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: TMDbRecommendationEntity)
    
    /**
     * Delete recommendations by content ID and type
     * @param contentId Content ID
     * @param contentType Content type
     * @param recommendationType Type of recommendation
     */
    @Query("DELETE FROM tmdb_recommendations WHERE contentId = :contentId AND contentType = :contentType AND recommendationType = :recommendationType")
    suspend fun deleteRecommendations(contentId: Int, contentType: String, recommendationType: String)
    
    /**
     * Delete all recommendations
     */
    @Query("DELETE FROM tmdb_recommendations")
    suspend fun deleteAllRecommendations()
    
    /**
     * Delete recommendations older than specified timestamp
     * @param timestamp Cutoff timestamp
     */
    @Query("DELETE FROM tmdb_recommendations WHERE lastUpdated < :timestamp")
    suspend fun deleteRecommendationsOlderThan(timestamp: Long)
    
    // Images Operations
    
    /**
     * Get images by content ID and type
     * @param contentId Content ID
     * @param contentType Content type (movie or tv)
     * @return Flow of images entity or null if not found
     */
    @Query("SELECT * FROM tmdb_images WHERE contentId = :contentId AND contentType = :contentType")
    fun getImages(contentId: Int, contentType: String): Flow<TMDbImagesEntity?>
    
    /**
     * Get images by content ID and type (suspend version)
     * @param contentId Content ID
     * @param contentType Content type (movie or tv)
     * @return Images entity or null if not found
     */
    @Query("SELECT * FROM tmdb_images WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun getImagesSuspend(contentId: Int, contentType: String): TMDbImagesEntity?
    
    /**
     * Insert or update images
     * @param images Images entity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: TMDbImagesEntity)
    
    /**
     * Delete images by content ID and type
     * @param contentId Content ID
     * @param contentType Content type
     */
    @Query("DELETE FROM tmdb_images WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun deleteImages(contentId: Int, contentType: String)
    
    /**
     * Delete all images
     */
    @Query("DELETE FROM tmdb_images")
    suspend fun deleteAllImages()
    
    /**
     * Delete images older than specified timestamp
     * @param timestamp Cutoff timestamp
     */
    @Query("DELETE FROM tmdb_images WHERE lastUpdated < :timestamp")
    suspend fun deleteImagesOlderThan(timestamp: Long)
    
    // Videos Operations
    
    /**
     * Get videos by content ID and type
     * @param contentId Content ID
     * @param contentType Content type (movie or tv)
     * @return Flow of videos entity or null if not found
     */
    @Query("SELECT * FROM tmdb_videos WHERE contentId = :contentId AND contentType = :contentType")
    fun getVideos(contentId: Int, contentType: String): Flow<TMDbVideosEntity?>
    
    /**
     * Get videos by content ID and type (suspend version)
     * @param contentId Content ID
     * @param contentType Content type (movie or tv)
     * @return Videos entity or null if not found
     */
    @Query("SELECT * FROM tmdb_videos WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun getVideosSuspend(contentId: Int, contentType: String): TMDbVideosEntity?
    
    /**
     * Insert or update videos
     * @param videos Videos entity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: TMDbVideosEntity)
    
    /**
     * Delete videos by content ID and type
     * @param contentId Content ID
     * @param contentType Content type
     */
    @Query("DELETE FROM tmdb_videos WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun deleteVideos(contentId: Int, contentType: String)
    
    /**
     * Delete all videos
     */
    @Query("DELETE FROM tmdb_videos")
    suspend fun deleteAllVideos()
    
    /**
     * Delete videos older than specified timestamp
     * @param timestamp Cutoff timestamp
     */
    @Query("DELETE FROM tmdb_videos WHERE lastUpdated < :timestamp")
    suspend fun deleteVideosOlderThan(timestamp: Long)
    
    // Genres Operations
    
    /**
     * Get all genres by media type
     * @param mediaType Media type (movie or tv)
     * @return Flow of genre entities
     */
    @Query("SELECT * FROM tmdb_genres WHERE mediaType = :mediaType ORDER BY lastUpdated DESC")
    fun getGenres(mediaType: String): Flow<List<TMDbGenreEntity>>
    
    /**
     * Get genre by ID
     * @param genreId Genre ID
     * @param mediaType Media type (movie or tv)
     * @return Flow of genre entity or null if not found
     */
    @Query("SELECT * FROM tmdb_genres WHERE id = :genreId AND mediaType = :mediaType")
    fun getGenre(genreId: Int, mediaType: String): Flow<TMDbGenreEntity?>
    
    /**
     * Insert or update genres
     * @param genres List of genre entities to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(genres: List<TMDbGenreEntity>)
    
    /**
     * Delete all genres
     */
    @Query("DELETE FROM tmdb_genres")
    suspend fun deleteAllGenres()
    
    /**
     * Delete genres older than specified timestamp
     * @param timestamp Cutoff timestamp
     */
    @Query("DELETE FROM tmdb_genres WHERE lastUpdated < :timestamp")
    suspend fun deleteGenresOlderThan(timestamp: Long)
    
    // Configuration Operations
    
    /**
     * Get TMDb configuration
     * @return Flow of configuration entity or null if not found
     */
    @Query("SELECT * FROM tmdb_config WHERE id = 'config'")
    fun getConfig(): Flow<TMDbConfigEntity?>
    
    /**
     * Get TMDb configuration (suspend version)
     * @return Configuration entity or null if not found
     */
    @Query("SELECT * FROM tmdb_config WHERE id = 'config'")
    suspend fun getConfigSuspend(): TMDbConfigEntity?
    
    /**
     * Insert or update configuration
     * @param config Configuration entity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: TMDbConfigEntity)
    
    /**
     * Delete configuration
     */
    @Query("DELETE FROM tmdb_config")
    suspend fun deleteConfig()
}