package com.rdwatch.androidtv.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.entities.ContentEntity
import com.rdwatch.androidtv.data.entities.ContentSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*

/**
 * Test class for ContentDao
 * Tests all CRUD operations, queries, and content management functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ContentDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var contentDao: ContentDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries()
            .build()
        contentDao = database.contentDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveContent() = runTest {
        // Given
        val content = createTestContentEntity(
            title = "Test Movie",
            source = ContentSource.REAL_DEBRID,
            realDebridId = "rd_123"
        )

        // When
        val insertedId = contentDao.insertContent(content)
        val retrieved = contentDao.getContentById(insertedId)

        // Then
        assertNotNull(retrieved)
        assertEquals(content.title, retrieved?.title)
        assertEquals(content.source, retrieved?.source)
        assertEquals(content.realDebridId, retrieved?.realDebridId)
        assertTrue(insertedId > 0)
    }

    @Test
    fun getAllContentOrderedByAddedDate() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val contents = listOf(
            createTestContentEntity(
                title = "Oldest Movie",
                addedDate = Date(currentTime - 172800000L) // 2 days ago
            ),
            createTestContentEntity(
                title = "Newest Movie",
                addedDate = Date(currentTime) // now
            ),
            createTestContentEntity(
                title = "Middle Movie",
                addedDate = Date(currentTime - 86400000L) // 1 day ago
            )
        )

        // When
        contents.forEach { contentDao.insertContent(it) }
        val allContent = contentDao.getAllContent().first()

        // Then
        assertEquals(3, allContent.size)
        assertEquals("Newest Movie", allContent[0].title)
        assertEquals("Middle Movie", allContent[1].title)
        assertEquals("Oldest Movie", allContent[2].title)
    }

    @Test
    fun getContentBySource() = runTest {
        // Given
        val contents = listOf(
            createTestContentEntity(title = "Local Movie 1", source = ContentSource.LOCAL),
            createTestContentEntity(title = "RD Movie 1", source = ContentSource.REAL_DEBRID),
            createTestContentEntity(title = "Local Movie 2", source = ContentSource.LOCAL),
            createTestContentEntity(title = "RD Movie 2", source = ContentSource.REAL_DEBRID)
        )

        // When
        contents.forEach { contentDao.insertContent(it) }
        val localContent = contentDao.getContentBySource(ContentSource.LOCAL).first()
        val rdContent = contentDao.getContentBySource(ContentSource.REAL_DEBRID).first()

        // Then
        assertEquals(2, localContent.size)
        assertEquals(2, rdContent.size)
        localContent.forEach { assertEquals(ContentSource.LOCAL, it.source) }
        rdContent.forEach { assertEquals(ContentSource.REAL_DEBRID, it.source) }
    }

    @Test
    fun searchContent() = runTest {
        // Given
        val contents = listOf(
            createTestContentEntity(title = "Action Movie 2023"),
            createTestContentEntity(title = "Comedy Show"),
            createTestContentEntity(title = "Action Hero Series"),
            createTestContentEntity(title = "Drama Film")
        )

        // When
        contents.forEach { contentDao.insertContent(it) }
        val actionResults = contentDao.searchContent("Action").first()
        val movieResults = contentDao.searchContent("Movie").first()
        val showResults = contentDao.searchContent("Show").first()

        // Then
        assertEquals(2, actionResults.size) // Action Movie and Action Hero
        assertEquals(1, movieResults.size) // Action Movie
        assertEquals(1, showResults.size) // Comedy Show
    }

    @Test
    fun getFavoriteContent() = runTest {
        // Given
        val contents = listOf(
            createTestContentEntity(title = "Favorite Movie", isFavorite = true),
            createTestContentEntity(title = "Regular Movie", isFavorite = false),
            createTestContentEntity(title = "Another Favorite", isFavorite = true)
        )

        // When
        contents.forEach { contentDao.insertContent(it) }
        val favorites = contentDao.getFavoriteContent().first()

        // Then
        assertEquals(2, favorites.size)
        favorites.forEach { assertTrue(it.isFavorite) }
    }

    @Test
    fun getWatchedContent() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val contents = listOf(
            createTestContentEntity(
                title = "Watched Movie 1",
                isWatched = true,
                lastPlayedDate = Date(currentTime - 3600000L) // 1 hour ago
            ),
            createTestContentEntity(
                title = "Unwatched Movie",
                isWatched = false
            ),
            createTestContentEntity(
                title = "Watched Movie 2",
                isWatched = true,
                lastPlayedDate = Date(currentTime - 7200000L) // 2 hours ago
            )
        )

        // When
        contents.forEach { contentDao.insertContent(it) }
        val watchedContent = contentDao.getWatchedContent().first()

        // Then
        assertEquals(2, watchedContent.size)
        watchedContent.forEach { assertTrue(it.isWatched) }
        // Should be ordered by lastPlayedDate DESC
        assertEquals("Watched Movie 1", watchedContent[0].title)
        assertEquals("Watched Movie 2", watchedContent[1].title)
    }

    @Test
    fun getRecentlyPlayed() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val contents = mutableListOf<ContentEntity>()
        
        // Create 15 content items with different play dates
        repeat(15) { index ->
            contents.add(
                createTestContentEntity(
                    title = "Movie $index",
                    lastPlayedDate = Date(currentTime - (index * 3600000L)) // Each 1 hour apart
                )
            )
        }

        // When
        contents.forEach { contentDao.insertContent(it) }
        val recentlyPlayed = contentDao.getRecentlyPlayed(10).first()

        // Then
        assertEquals(10, recentlyPlayed.size) // Limited to 10
        // Should be ordered by lastPlayedDate DESC
        assertEquals("Movie 0", recentlyPlayed[0].title) // Most recent
        assertEquals("Movie 9", recentlyPlayed[9].title) // 10th most recent
    }

    @Test
    fun searchContentBySource() = runTest {
        // Given
        val contents = listOf(
            createTestContentEntity(title = "Local Action Movie", source = ContentSource.LOCAL),
            createTestContentEntity(title = "RD Action Movie", source = ContentSource.REAL_DEBRID),
            createTestContentEntity(title = "Local Drama", source = ContentSource.LOCAL),
            createTestContentEntity(title = "RD Drama", source = ContentSource.REAL_DEBRID)
        )

        // When
        contents.forEach { contentDao.insertContent(it) }
        val localActionResults = contentDao.searchContentBySource("Action", ContentSource.LOCAL).first()
        val rdActionResults = contentDao.searchContentBySource("Action", ContentSource.REAL_DEBRID).first()

        // Then
        assertEquals(1, localActionResults.size)
        assertEquals(1, rdActionResults.size)
        assertEquals("Local Action Movie", localActionResults[0].title)
        assertEquals("RD Action Movie", rdActionResults[0].title)
    }

    @Test
    fun updateContent() = runTest {
        // Given
        val originalContent = createTestContentEntity(
            title = "Original Title",
            rating = 7.0f
        )

        // When
        val insertedId = contentDao.insertContent(originalContent)
        val updatedContent = originalContent.copy(
            id = insertedId,
            title = "Updated Title",
            rating = 8.5f
        )
        contentDao.updateContent(updatedContent)
        val retrieved = contentDao.getContentById(insertedId)

        // Then
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved?.title)
        assertEquals(8.5f, retrieved?.rating)
    }

    @Test
    fun upsertContent() = runTest {
        // Given
        val content = createTestContentEntity(title = "Upsert Test")

        // When - First upsert (insert)
        contentDao.upsertContent(content)
        val allContentAfterInsert = contentDao.getAllContent().first()

        // Then
        assertEquals(1, allContentAfterInsert.size)
        assertEquals("Upsert Test", allContentAfterInsert[0].title)

        // When - Second upsert (update)
        val updatedContent = allContentAfterInsert[0].copy(title = "Updated Upsert Test")
        contentDao.upsertContent(updatedContent)
        val allContentAfterUpdate = contentDao.getAllContent().first()

        // Then
        assertEquals(1, allContentAfterUpdate.size)
        assertEquals("Updated Upsert Test", allContentAfterUpdate[0].title)
    }

    @Test
    fun upsertContentList() = runTest {
        // Given
        val contents = listOf(
            createTestContentEntity(title = "Movie 1"),
            createTestContentEntity(title = "Movie 2"),
            createTestContentEntity(title = "Movie 3")
        )

        // When - First upsert (insert all)
        contentDao.upsertContent(contents)
        val afterInsert = contentDao.getAllContent().first()

        // Then
        assertEquals(3, afterInsert.size)

        // When - Second upsert (update existing)
        val updatedContents = afterInsert.map { it.copy(rating = 9.0f) }
        contentDao.upsertContent(updatedContents)
        val afterUpdate = contentDao.getAllContent().first()

        // Then
        assertEquals(3, afterUpdate.size)
        afterUpdate.forEach { assertEquals(9.0f, it.rating) }
    }

    @Test
    fun deleteContent() = runTest {
        // Given
        val content = createTestContentEntity(title = "To Be Deleted")

        // When
        val insertedId = contentDao.insertContent(content)
        val beforeDelete = contentDao.getContentById(insertedId)
        val contentToDelete = beforeDelete!!.copy(id = insertedId)
        contentDao.deleteContent(contentToDelete)
        val afterDelete = contentDao.getContentById(insertedId)

        // Then
        assertNotNull(beforeDelete)
        assertNull(afterDelete)
    }

    @Test
    fun deleteContentById() = runTest {
        // Given
        val content = createTestContentEntity(title = "To Be Deleted By ID")

        // When
        val insertedId = contentDao.insertContent(content)
        val beforeDelete = contentDao.getContentById(insertedId)
        contentDao.deleteContentById(insertedId)
        val afterDelete = contentDao.getContentById(insertedId)

        // Then
        assertNotNull(beforeDelete)
        assertNull(afterDelete)
    }

    @Test
    fun updateFavoriteStatus() = runTest {
        // Given
        val content = createTestContentEntity(title = "Favorite Test", isFavorite = false)

        // When
        val insertedId = contentDao.insertContent(content)
        contentDao.updateFavoriteStatus(insertedId, true)
        val updated = contentDao.getContentById(insertedId)

        // Then
        assertNotNull(updated)
        assertTrue(updated?.isFavorite ?: false)
    }

    @Test
    fun updateWatchedStatus() = runTest {
        // Given
        val content = createTestContentEntity(title = "Watched Test", isWatched = false)

        // When
        val insertedId = contentDao.insertContent(content)
        contentDao.updateWatchedStatus(insertedId, true)
        val updated = contentDao.getContentById(insertedId)

        // Then
        assertNotNull(updated)
        assertTrue(updated?.isWatched ?: false)
    }

    @Test
    fun updatePlayedInfo() = runTest {
        // Given
        val content = createTestContentEntity(title = "Play Test", playCount = 0)

        // When
        val insertedId = contentDao.insertContent(content)
        val beforePlay = contentDao.getContentById(insertedId)
        
        // Update played info
        val playTime = System.currentTimeMillis()
        contentDao.updatePlayedInfo(insertedId, playTime)
        val afterPlay = contentDao.getContentById(insertedId)

        // Then
        assertNotNull(beforePlay)
        assertNotNull(afterPlay)
        assertEquals(0, beforePlay?.playCount)
        assertEquals(1, afterPlay?.playCount)
        assertEquals(Date(playTime), afterPlay?.lastPlayedDate)
    }

    @Test
    fun insertContentIgnoreConflict() = runTest {
        // Given
        val content1 = createTestContentEntity(title = "Same Title")
        val content2 = createTestContentEntity(title = "Same Title")

        // When
        val id1 = contentDao.insertContent(content1)
        val id2 = contentDao.insertContent(content2)

        // Then
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
        assertNotEquals(id1, id2) // Should create separate entries even with same title
    }

    @Test
    fun handleNullOptionalFields() = runTest {
        // Given
        val contentWithNulls = ContentEntity(
            title = "Minimal Content",
            source = ContentSource.LOCAL,
            year = null,
            quality = null,
            realDebridId = null,
            posterUrl = null,
            backdropUrl = null,
            description = null,
            duration = null,
            rating = null,
            genres = null,
            cast = null,
            director = null,
            imdbId = null,
            tmdbId = null,
            lastPlayedDate = null
        )

        // When
        val insertedId = contentDao.insertContent(contentWithNulls)
        val retrieved = contentDao.getContentById(insertedId)

        // Then
        assertNotNull(retrieved)
        assertEquals("Minimal Content", retrieved?.title)
        assertEquals(ContentSource.LOCAL, retrieved?.source)
        assertNull(retrieved?.year)
        assertNull(retrieved?.quality)
        assertNull(retrieved?.realDebridId)
        assertNull(retrieved?.description)
        assertNull(retrieved?.rating)
        assertNull(retrieved?.lastPlayedDate)
    }

    @Test
    fun defaultValuesForBooleanFields() = runTest {
        // Given
        val content = ContentEntity(
            title = "Default Values Test",
            source = ContentSource.LOCAL
            // Not setting isFavorite, isWatched, playCount - should use defaults
        )

        // When
        val insertedId = contentDao.insertContent(content)
        val retrieved = contentDao.getContentById(insertedId)

        // Then
        assertNotNull(retrieved)
        assertFalse(retrieved?.isFavorite ?: true)
        assertFalse(retrieved?.isWatched ?: true)
        assertEquals(0, retrieved?.playCount)
    }

    @Test
    fun flowUpdatesOnDataChange() = runTest {
        // Given
        val content = createTestContentEntity(title = "Flow Test", isFavorite = false)

        // When
        val insertedId = contentDao.insertContent(content)
        val initialFavorites = contentDao.getFavoriteContent().first()

        contentDao.updateFavoriteStatus(insertedId, true)
        val updatedFavorites = contentDao.getFavoriteContent().first()

        // Then
        assertEquals(0, initialFavorites.size)
        assertEquals(1, updatedFavorites.size)
        assertEquals("Flow Test", updatedFavorites[0].title)
    }

    /**
     * Helper function to create test ContentEntity with defaults
     */
    private fun createTestContentEntity(
        title: String = "Test Content",
        year: Int? = 2023,
        quality: String? = "1080p",
        source: ContentSource = ContentSource.REAL_DEBRID,
        realDebridId: String? = "rd_test_id",
        posterUrl: String? = "https://example.com/poster.jpg",
        backdropUrl: String? = "https://example.com/backdrop.jpg",
        description: String? = "Test description",
        duration: Int? = 120,
        rating: Float? = 8.0f,
        genres: List<String>? = listOf("Action", "Drama"),
        cast: List<String>? = listOf("Actor 1", "Actor 2"),
        director: String? = "Test Director",
        imdbId: String? = "tt1234567",
        tmdbId: Int? = 123456,
        addedDate: Date = Date(),
        lastPlayedDate: Date? = null,
        playCount: Int = 0,
        isFavorite: Boolean = false,
        isWatched: Boolean = false
    ): ContentEntity {
        return ContentEntity(
            title = title,
            year = year,
            quality = quality,
            source = source,
            realDebridId = realDebridId,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            description = description,
            duration = duration,
            rating = rating,
            genres = genres,
            cast = cast,
            director = director,
            imdbId = imdbId,
            tmdbId = tmdbId,
            addedDate = addedDate,
            lastPlayedDate = lastPlayedDate,
            playCount = playCount,
            isFavorite = isFavorite,
            isWatched = isWatched
        )
    }
}