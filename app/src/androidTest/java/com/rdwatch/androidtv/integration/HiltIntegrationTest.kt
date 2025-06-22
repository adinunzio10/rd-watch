package com.rdwatch.androidtv.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.repository.MovieRepository
import com.rdwatch.androidtv.network.ApiService
import com.rdwatch.androidtv.test.HiltInstrumentedTestBase
import com.rdwatch.androidtv.test.fake.FakeMovieRepository
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration test that verifies Hilt dependency injection works correctly
 * in an Android instrumented test environment.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltIntegrationTest : HiltInstrumentedTestBase() {

    @Inject
    lateinit var movieRepository: MovieRepository

    @Inject
    lateinit var fakeMovieRepository: FakeMovieRepository

    @Inject
    lateinit var apiService: ApiService

    @Test
    fun testDependencyInjectionWorks() {
        // Verify that all dependencies are properly injected
        assertNotNull("MovieRepository should be injected", movieRepository)
        assertNotNull("FakeMovieRepository should be injected", fakeMovieRepository)
        assertNotNull("ApiService should be injected", apiService)
    }

    @Test
    fun testRepositoryIntegrationWithFakeData() = runTest {
        // Arrange
        fakeMovieRepository.setReturnError(false)

        // Act
        val movies = movieRepository.getAllMovies().first()

        // Assert
        assertNotNull("Movies should not be null", movies)
        assertEquals("Should have 2 test movies", 2, movies.size)
    }

    @Test
    fun testFakeRepositoryTestUtilities() {
        // Test that fake repository utilities work correctly
        val testMovies = fakeMovieRepository.getTestMovies()
        assertEquals("Should have 2 test movies", 2, testMovies.size)
        assertEquals("First movie should have correct title", "Test Movie 1", testMovies[0].title)
        assertEquals("Second movie should have correct title", "Test Movie 2", testMovies[1].title)
    }

    @Test
    fun testSearchFunctionality() = runTest {
        // Arrange
        fakeMovieRepository.setReturnError(false)

        // Act
        val movies = movieRepository.searchMovies("Test Movie 1").first()

        // Assert
        assertNotNull("Movies should not be null", movies)
        assertEquals("Should find 1 movie", 1, movies.size)
        assertEquals("Should find correct movie", "Test Movie 1", movies.first().title)
    }

    @Test
    fun testGetSpecificMovie() = runTest {
        // Arrange
        fakeMovieRepository.setReturnError(false)

        // Act
        val movie = movieRepository.getMovieById(2L)

        // Assert
        assertNotNull("Movie should not be null", movie)
        assertEquals("Should return correct movie", "Test Movie 2", movie?.title)
        assertEquals("Should have correct ID", 2L, movie?.id)
    }
}