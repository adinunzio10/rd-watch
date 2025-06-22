package com.rdwatch.androidtv.test.fake

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of MovieRepository for testing.
 * Provides predictable test data without network or database dependencies.
 */
@Singleton
class FakeMovieRepository @Inject constructor() : MovieRepository {

    private val testMovies = mutableListOf(
        Movie(
            id = 1,
            title = "Test Movie 1",
            description = "Test description 1",
            backgroundImageUrl = "https://example.com/bg1.jpg",
            cardImageUrl = "https://example.com/card1.jpg",
            videoUrl = "https://example.com/video1.mp4",
            studio = "Test Studio"
        ),
        Movie(
            id = 2,
            title = "Test Movie 2",
            description = "Test description 2",
            backgroundImageUrl = "https://example.com/bg2.jpg",
            cardImageUrl = "https://example.com/card2.jpg",
            videoUrl = "https://example.com/video2.mp4",
            studio = "Test Studio"
        )
    )

    private var shouldReturnError = false

    override fun getAllMovies(): Flow<List<Movie>> {
        return if (shouldReturnError) {
            flow { throw Exception("Test error") }
        } else {
            flowOf(testMovies.toList())
        }
    }

    override suspend fun getMovieById(movieId: Long): Movie? {
        return if (shouldReturnError) {
            throw Exception("Test error")
        } else {
            testMovies.find { it.id == movieId }
        }
    }

    override fun searchMovies(query: String): Flow<List<Movie>> {
        return if (shouldReturnError) {
            flow { throw Exception("Search failed") }
        } else {
            val filteredMovies = testMovies.filter { 
                it.title?.contains(query, ignoreCase = true) == true ||
                it.description?.contains(query, ignoreCase = true) == true
            }
            flowOf(filteredMovies)
        }
    }

    override suspend fun refreshMovies(): Result<Unit> {
        return if (shouldReturnError) {
            Result.failure(Exception("Refresh failed"))
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun insertMovie(movie: Movie) {
        if (shouldReturnError) {
            throw Exception("Insert failed")
        } else {
            testMovies.removeAll { it.id == movie.id }
            testMovies.add(movie)
        }
    }

    override suspend fun deleteMovie(movie: Movie) {
        if (shouldReturnError) {
            throw Exception("Delete failed")
        } else {
            testMovies.removeAll { it.id == movie.id }
        }
    }

    override suspend fun deleteAllMovies() {
        if (shouldReturnError) {
            throw Exception("Delete all failed")
        } else {
            testMovies.clear()
        }
    }

    // Test utilities
    fun setReturnError(returnError: Boolean) {
        shouldReturnError = returnError
    }

    fun getTestMovies(): List<Movie> = testMovies.toList()
    
    fun addTestMovie(movie: Movie) {
        testMovies.add(movie)
    }
    
    fun clearTestMovies() {
        testMovies.clear()
    }
}