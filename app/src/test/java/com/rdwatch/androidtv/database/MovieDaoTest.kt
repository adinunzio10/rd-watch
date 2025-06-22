package com.rdwatch.androidtv.database

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.data.MovieDao
import com.rdwatch.androidtv.test.HiltTestBase
import com.rdwatch.androidtv.test.MainDispatcherRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Test class for MovieDao database operations.
 * Uses in-memory database for fast and isolated testing.
 */
@HiltAndroidTest
class MovieDaoTest : HiltTestBase() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Inject
    lateinit var movieDao: MovieDao

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
    fun `insertMovie and getMovieById works correctly`() = runTest {
        // Act
        movieDao.insertMovie(testMovie)
        val retrievedMovie = movieDao.getMovieById(1L)

        // Assert
        assertNotNull("Retrieved movie should not be null", retrievedMovie)
        assertEquals("Movie ID should match", testMovie.id, retrievedMovie?.id)
        assertEquals("Movie title should match", testMovie.title, retrievedMovie?.title)
        assertEquals("Movie description should match", testMovie.description, retrievedMovie?.description)
    }

    @Test
    fun `getAllMovies returns all inserted movies`() = runTest {
        // Arrange
        val movie2 = testMovie.copy(id = 2, title = "Test Movie 2")

        // Act
        movieDao.insertMovie(testMovie)
        movieDao.insertMovie(movie2)
        val allMovies = movieDao.getAllMovies().first()

        // Assert
        assertEquals("Should return 2 movies", 2, allMovies.size)
        assertTrue("Should contain first movie", allMovies.any { it.id == 1L })
        assertTrue("Should contain second movie", allMovies.any { it.id == 2L })
    }

    @Test
    fun `insertMovies inserts multiple movies at once`() = runTest {
        // Arrange
        val movies = listOf(
            testMovie,
            testMovie.copy(id = 2, title = "Test Movie 2"),
            testMovie.copy(id = 3, title = "Test Movie 3")
        )

        // Act
        movieDao.insertMovies(movies)
        val allMovies = movieDao.getAllMovies().first()

        // Assert
        assertEquals("Should return 3 movies", 3, allMovies.size)
    }

    @Test
    fun `updateMovie updates existing movie`() = runTest {
        // Arrange
        movieDao.insertMovie(testMovie)
        val updatedMovie = testMovie.copy(title = "Updated Title", description = "Updated Description")

        // Act
        movieDao.updateMovie(updatedMovie)
        val retrievedMovie = movieDao.getMovieById(1L)

        // Assert
        assertNotNull("Retrieved movie should not be null", retrievedMovie)
        assertEquals("Title should be updated", "Updated Title", retrievedMovie?.title)
        assertEquals("Description should be updated", "Updated Description", retrievedMovie?.description)
    }

    @Test
    fun `deleteMovie removes movie from database`() = runTest {
        // Arrange
        movieDao.insertMovie(testMovie)
        val beforeDelete = movieDao.getMovieById(1L)

        // Act
        movieDao.deleteMovie(testMovie)
        val afterDelete = movieDao.getMovieById(1L)

        // Assert
        assertNotNull("Movie should exist before delete", beforeDelete)
        assertNull("Movie should not exist after delete", afterDelete)
    }

    @Test
    fun `deleteAllMovies clears all movies`() = runTest {
        // Arrange
        movieDao.insertMovie(testMovie)
        movieDao.insertMovie(testMovie.copy(id = 2, title = "Movie 2"))
        val beforeDelete = movieDao.getAllMovies().first()

        // Act
        movieDao.deleteAllMovies()
        val afterDelete = movieDao.getAllMovies().first()

        // Assert
        assertTrue("Should have movies before delete", beforeDelete.isNotEmpty())
        assertTrue("Should have no movies after delete", afterDelete.isEmpty())
    }

    @Test
    fun `searchMovies finds movies by title`() = runTest {
        // Arrange
        val movies = listOf(
            testMovie.copy(id = 1, title = "Action Movie"),
            testMovie.copy(id = 2, title = "Comedy Film"),
            testMovie.copy(id = 3, title = "Action Hero")
        )
        movieDao.insertMovies(movies)

        // Act
        val actionMovies = movieDao.searchMovies("Action").first()

        // Assert
        assertEquals("Should find 2 action movies", 2, actionMovies.size)
        assertTrue("Should contain Action Movie", actionMovies.any { it.title == "Action Movie" })
        assertTrue("Should contain Action Hero", actionMovies.any { it.title == "Action Hero" })
    }

    @Test
    fun `getMovieById returns null for non-existent movie`() = runTest {
        // Act
        val nonExistentMovie = movieDao.getMovieById(999L)

        // Assert
        assertNull("Should return null for non-existent movie", nonExistentMovie)
    }
}