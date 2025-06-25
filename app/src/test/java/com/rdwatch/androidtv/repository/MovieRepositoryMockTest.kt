package com.rdwatch.androidtv.repository

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.repository.MovieRepository
import com.rdwatch.androidtv.test.HiltTestBase
import com.rdwatch.androidtv.test.MainDispatcherRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Test class for MovieRepository functionality.
 * Uses the TestFakeRepositoryModule which provides the fake implementation.
 */
@HiltAndroidTest
class MovieRepositoryMockTest : HiltTestBase() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Inject
    lateinit var repository: MovieRepository

    private val testMovie = Movie(
        id = 1,
        title = "Test Movie",
        description = "Test Description",
        backgroundImageUrl = "https://example.com/bg.jpg",
        cardImageUrl = "https://example.com/card.jpg",
        videoUrl = "https://example.com/video.mp4",
        studio = "Test Studio"
    )

    @Test
    fun `verify repository is injected`() {
        // Repository should be injected via TestFakeRepositoryModule
        assertNotNull("Repository should be injected", repository)
    }

    @Test
    fun `test repository returns default test data`() = runTest {
        // Act - Using the fake repository which returns predictable data
        val movies = repository.getAllMovies().firstOrNull()

        // Assert
        assertNotNull("Movies should not be null", movies)
        assertEquals("Should return 2 test movies", 2, movies.size)
    }

    @Test
    fun `test getMovieById returns correct movie`() = runTest {
        // Act
        val movie = repository.getMovieById(1L)

        // Assert
        assertNotNull("Movie should not be null", movie)
        assertEquals("Should return Test Movie 1", "Test Movie 1", movie?.title)
    }

    @Test
    fun `test searchMovies works correctly`() = runTest {
        // Act
        val movies = repository.searchMovies("Movie 1").firstOrNull()

        // Assert
        assertNotNull("Movies should not be null", movies)
        assertEquals("Should find 1 movie", 1, movies.size)
        assertEquals("Should find correct movie", "Test Movie 1", movies?.first()?.title)
    }
}