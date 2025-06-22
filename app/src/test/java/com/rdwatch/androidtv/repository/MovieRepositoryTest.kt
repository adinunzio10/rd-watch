package com.rdwatch.androidtv.repository

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.repository.MovieRepository
import com.rdwatch.androidtv.test.HiltTestBase
import com.rdwatch.androidtv.test.MainDispatcherRule
import com.rdwatch.androidtv.test.fake.FakeMovieRepository
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Test class for MovieRepository functionality using fake implementations.
 * This demonstrates how to test repositories with predictable data.
 */
@HiltAndroidTest
class MovieRepositoryTest : HiltTestBase() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Inject
    lateinit var repository: MovieRepository

    @Inject
    lateinit var fakeRepository: FakeMovieRepository

    @Test
    fun `getAllMovies emits test data`() = runTest {
        // Arrange
        fakeRepository.setReturnError(false)

        // Act
        val movies = repository.getAllMovies().first()

        // Assert
        assertNotNull("Movies list should not be null", movies)
        assertEquals("Should emit 2 test movies", 2, movies.size)
        assertTrue("Should contain test movie", movies.any { it.title == "Test Movie 1" })
    }

    @Test
    fun `getMovieById returns correct movie`() = runTest {
        // Arrange
        fakeRepository.setReturnError(false)

        // Act
        val movie = repository.getMovieById(1L)

        // Assert
        assertNotNull("Movie should not be null", movie)
        assertEquals("Should return correct movie", "Test Movie 1", movie?.title)
        assertEquals("Should have correct ID", 1L, movie?.id)
    }

    @Test
    fun `getMovieById returns null for non-existent id`() = runTest {
        // Arrange
        fakeRepository.setReturnError(false)

        // Act
        val movie = repository.getMovieById(999L)

        // Assert
        assertNull("Movie should be null for non-existent ID", movie)
    }

    @Test
    fun `searchMovies filters by title`() = runTest {
        // Arrange
        fakeRepository.setReturnError(false)

        // Act
        val movies = repository.searchMovies("Movie 1").first()

        // Assert
        assertNotNull("Movies list should not be null", movies)
        assertEquals("Should return 1 filtered movie", 1, movies.size)
        assertEquals("Should return correct movie", "Test Movie 1", movies.first().title)
    }

    @Test
    fun `searchMovies filters by description`() = runTest {
        // Arrange
        fakeRepository.setReturnError(false)

        // Act
        val movies = repository.searchMovies("description 2").first()

        // Assert
        assertNotNull("Movies list should not be null", movies)
        assertEquals("Should return 1 filtered movie", 1, movies.size)
        assertEquals("Should return correct movie", "Test Movie 2", movies.first().title)
    }

    @Test
    fun `refreshMovies succeeds when not configured for error`() = runTest {
        // Arrange
        fakeRepository.setReturnError(false)

        // Act
        val result = repository.refreshMovies()

        // Assert
        assertTrue("Refresh should succeed", result.isSuccess)
    }

    @Test
    fun `refreshMovies fails when configured for error`() = runTest {
        // Arrange
        fakeRepository.setReturnError(true)

        // Act
        val result = repository.refreshMovies()

        // Assert
        assertTrue("Refresh should fail", result.isFailure)
        assertNotNull("Should have exception", result.exceptionOrNull())
    }

    @Test
    fun `insertMovie adds movie to collection`() = runTest {
        // Arrange
        fakeRepository.setReturnError(false)
        val newMovie = Movie(
            id = 3,
            title = "New Movie",
            description = "New Description",
            backgroundImageUrl = "https://example.com/new_bg.jpg",
            cardImageUrl = "https://example.com/new_card.jpg",
            videoUrl = "https://example.com/new_video.mp4",
            studio = "New Studio"
        )

        // Act
        repository.insertMovie(newMovie)
        val movies = repository.getAllMovies().first()

        // Assert
        assertEquals("Should have 3 movies", 3, movies.size)
        assertTrue("Should contain new movie", movies.any { it.title == "New Movie" })
    }

    @Test
    fun `deleteMovie removes movie from collection`() = runTest {
        // Arrange
        fakeRepository.setReturnError(false)
        val movieToDelete = fakeRepository.getTestMovies().first()

        // Act
        repository.deleteMovie(movieToDelete)
        val movies = repository.getAllMovies().first()

        // Assert
        assertEquals("Should have 1 movie", 1, movies.size)
        assertFalse("Should not contain deleted movie", movies.any { it.id == movieToDelete.id })
    }

    @Test
    fun `deleteAllMovies clears all movies`() = runTest {
        // Arrange
        fakeRepository.setReturnError(false)

        // Act
        repository.deleteAllMovies()
        val movies = repository.getAllMovies().first()

        // Assert
        assertTrue("Should have no movies", movies.isEmpty())
    }
}