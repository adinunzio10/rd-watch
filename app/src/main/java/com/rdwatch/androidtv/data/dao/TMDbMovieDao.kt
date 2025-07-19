package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.TMDbMovieEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for TMDb movie data operations
 * Provides access to cached movie data with proper reactive flows
 */
@Dao
interface TMDbMovieDao {
    /**
     * Get movie by ID
     * @param movieId TMDb movie ID
     * @return Flow of movie entity or null if not found
     */
    @Query("SELECT * FROM tmdb_movies WHERE id = :movieId")
    fun getMovieById(movieId: Int): Flow<TMDbMovieEntity?>

    /**
     * Get movie by ID (suspend version for repository use)
     * @param movieId TMDb movie ID
     * @return Movie entity or null if not found
     */
    @Query("SELECT * FROM tmdb_movies WHERE id = :movieId")
    suspend fun getMovieByIdSuspend(movieId: Int): TMDbMovieEntity?

    /**
     * Get all movies ordered by popularity
     * @return Flow of all cached movies
     */
    @Query("SELECT * FROM tmdb_movies ORDER BY popularity DESC")
    fun getAllMovies(): Flow<List<TMDbMovieEntity>>

    /**
     * Get movies by genre
     * @param genreId Genre ID to filter by
     * @return Flow of movies with the specified genre
     */
    @Query("SELECT * FROM tmdb_movies WHERE genreIds LIKE '%' || :genreId || '%' ORDER BY popularity DESC")
    fun getMoviesByGenre(genreId: Int): Flow<List<TMDbMovieEntity>>

    /**
     * Get movies by release year
     * @param year Release year to filter by
     * @return Flow of movies from the specified year
     */
    @Query("SELECT * FROM tmdb_movies WHERE releaseDate LIKE :year || '%' ORDER BY popularity DESC")
    fun getMoviesByYear(year: String): Flow<List<TMDbMovieEntity>>

    /**
     * Get movies with minimum vote average
     * @param minRating Minimum vote average
     * @return Flow of movies above the rating threshold
     */
    @Query("SELECT * FROM tmdb_movies WHERE voteAverage >= :minRating ORDER BY voteAverage DESC")
    fun getMoviesByMinRating(minRating: Float): Flow<List<TMDbMovieEntity>>

    /**
     * Get popular movies (top by popularity)
     * @param limit Maximum number of movies to return
     * @return Flow of popular movies
     */
    @Query("SELECT * FROM tmdb_movies ORDER BY popularity DESC LIMIT :limit")
    fun getPopularMovies(limit: Int = 20): Flow<List<TMDbMovieEntity>>

    /**
     * Get top rated movies (top by vote average)
     * @param limit Maximum number of movies to return
     * @return Flow of top rated movies
     */
    @Query("SELECT * FROM tmdb_movies ORDER BY voteAverage DESC LIMIT :limit")
    fun getTopRatedMovies(limit: Int = 20): Flow<List<TMDbMovieEntity>>

    /**
     * Get recently added movies
     * @param limit Maximum number of movies to return
     * @return Flow of recently added movies
     */
    @Query("SELECT * FROM tmdb_movies ORDER BY lastUpdated DESC LIMIT :limit")
    fun getRecentMovies(limit: Int = 20): Flow<List<TMDbMovieEntity>>

    /**
     * Search movies by title
     * @param query Search query
     * @return Flow of movies matching the search query
     */
    @Query(
        "SELECT * FROM tmdb_movies WHERE title LIKE '%' || :query || '%' OR originalTitle LIKE '%' || :query || '%' ORDER BY popularity DESC",
    )
    fun searchMovies(query: String): Flow<List<TMDbMovieEntity>>

    /**
     * Insert or update a movie
     * @param movie Movie entity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: TMDbMovieEntity)

    /**
     * Insert or update multiple movies
     * @param movies List of movie entities to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<TMDbMovieEntity>)

    /**
     * Update a movie
     * @param movie Movie entity to update
     */
    @Update
    suspend fun updateMovie(movie: TMDbMovieEntity)

    /**
     * Delete a movie
     * @param movie Movie entity to delete
     */
    @Delete
    suspend fun deleteMovie(movie: TMDbMovieEntity)

    /**
     * Delete movie by ID
     * @param movieId TMDb movie ID
     */
    @Query("DELETE FROM tmdb_movies WHERE id = :movieId")
    suspend fun deleteMovieById(movieId: Int)

    /**
     * Delete all movies
     */
    @Query("DELETE FROM tmdb_movies")
    suspend fun deleteAllMovies()

    /**
     * Delete movies older than specified timestamp
     * @param timestamp Cutoff timestamp
     */
    @Query("DELETE FROM tmdb_movies WHERE lastUpdated < :timestamp")
    suspend fun deleteMoviesOlderThan(timestamp: Long)

    /**
     * Get count of cached movies
     * @return Number of cached movies
     */
    @Query("SELECT COUNT(*) FROM tmdb_movies")
    suspend fun getMovieCount(): Int

    /**
     * Check if a movie exists in cache
     * @param movieId TMDb movie ID
     * @return True if movie exists in cache
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tmdb_movies WHERE id = :movieId)")
    suspend fun movieExists(movieId: Int): Boolean

    /**
     * Get movies that need refresh (older than specified timestamp)
     * @param timestamp Cutoff timestamp
     * @return Flow of movies that need refresh
     */
    @Query("SELECT * FROM tmdb_movies WHERE lastUpdated < :timestamp")
    fun getMoviesNeedingRefresh(timestamp: Long): Flow<List<TMDbMovieEntity>>

    /**
     * Get movie last updated timestamp
     * @param movieId TMDb movie ID
     * @return Last updated timestamp or null if not found
     */
    @Query("SELECT lastUpdated FROM tmdb_movies WHERE id = :movieId")
    suspend fun getMovieLastUpdated(movieId: Int): Long?
}
