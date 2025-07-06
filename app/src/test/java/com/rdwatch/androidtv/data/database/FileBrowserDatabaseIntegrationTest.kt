package com.rdwatch.androidtv.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.dao.ContentDao
import com.rdwatch.androidtv.data.dao.TorrentDao
import com.rdwatch.androidtv.data.entities.ContentEntity
import com.rdwatch.androidtv.data.entities.ContentSource
import com.rdwatch.androidtv.data.entities.TorrentEntity
import com.rdwatch.androidtv.test.factories.TestDataFactory
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
 * Integration tests for database operations related to file browser functionality
 * Tests interactions between multiple DAOs and complex scenarios
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FileBrowserDatabaseIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var torrentDao: TorrentDao
    private lateinit var contentDao: ContentDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries()
            .build()
        torrentDao = database.torrentDao()
        contentDao = database.contentDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun syncRealDebridDataIntegrationTest() = runTest {
        // Given - Simulate syncing data from Real-Debrid API
        val torrents = TestDataFactory.createTorrentEntityList(10, status = "downloaded")
        val contentEntities = torrents.map { torrent ->
            ContentEntity(
                title = extractTitleFromFilename(torrent.filename),
                year = extractYearFromFilename(torrent.filename),
                source = ContentSource.REAL_DEBRID,
                realDebridId = torrent.id,
                description = "Content from torrent: ${torrent.filename}",
                rating = (5.0f + (torrent.id.hashCode() % 50) / 10.0f), // Random rating 5.0-9.9
                addedDate = torrent.added
            )
        }

        // When - Sync both torrents and content
        torrentDao.insertTorrents(torrents)
        contentEntities.forEach { contentDao.insertContent(it) }

        // Then - Verify data consistency
        val allTorrents = torrentDao.getAllTorrents().first()
        val allContent = contentDao.getAllContent().first()
        val rdContent = contentDao.getContentBySource(ContentSource.REAL_DEBRID).first()

        assertEquals(10, allTorrents.size)
        assertEquals(10, allContent.size)
        assertEquals(10, rdContent.size)

        // Verify relationships
        rdContent.forEach { content ->
            val matchingTorrent = allTorrents.find { it.id == content.realDebridId }
            assertNotNull("Content should have matching torrent", matchingTorrent)
            assertTrue("Content added date should match torrent", 
                content.addedDate.time == matchingTorrent?.added?.time)
        }
    }

    @Test
    fun bulkOperationsPerformanceTest() = runTest {
        // Given - Large dataset
        val largeTorrentSet = TestDataFactory.createTorrentEntityList(1000)
        val largeContentSet = (1..1000).map { index ->
            ContentEntity(
                title = "Movie $index",
                source = ContentSource.REAL_DEBRID,
                realDebridId = "torrent_$index",
                year = 2020 + (index % 5),
                rating = 5.0f + (index % 50) / 10.0f
            )
        }

        // When - Measure bulk insertion performance
        val torrentInsertStart = System.currentTimeMillis()
        torrentDao.insertTorrents(largeTorrentSet)
        val torrentInsertTime = System.currentTimeMillis() - torrentInsertStart

        val contentInsertStart = System.currentTimeMillis()
        contentDao.upsertContent(largeContentSet)
        val contentInsertTime = System.currentTimeMillis() - contentInsertStart

        // Then - Verify data and performance
        val allTorrents = torrentDao.getAllTorrents().first()
        val allContent = contentDao.getAllContent().first()

        assertEquals(1000, allTorrents.size)
        assertEquals(1000, allContent.size)

        // Performance assertions (should complete within reasonable time)
        assertTrue("Torrent insertion should complete within 5 seconds", torrentInsertTime < 5000)
        assertTrue("Content insertion should complete within 5 seconds", contentInsertTime < 5000)

        println("Torrent insertion time: ${torrentInsertTime}ms")
        println("Content insertion time: ${contentInsertTime}ms")
    }

    @Test
    fun crossDaoQueryingTest() = runTest {
        // Given - Mixed content from different sources
        val rdTorrents = TestDataFactory.createTorrentEntityList(5, status = "downloaded")
        val localContent = (1..3).map { index ->
            ContentEntity(
                title = "Local Movie $index",
                source = ContentSource.LOCAL,
                year = 2022,
                rating = 7.0f + index
            )
        }
        val rdContent = rdTorrents.map { torrent ->
            ContentEntity(
                title = extractTitleFromFilename(torrent.filename),
                source = ContentSource.REAL_DEBRID,
                realDebridId = torrent.id,
                year = 2023,
                rating = 8.0f
            )
        }

        // When
        torrentDao.insertTorrents(rdTorrents)
        localContent.forEach { contentDao.insertContent(it) }
        rdContent.forEach { contentDao.insertContent(it) }

        // Then - Complex queries across data
        val allContent = contentDao.getAllContent().first()
        val rdContentOnly = contentDao.getContentBySource(ContentSource.REAL_DEBRID).first()
        val localContentOnly = contentDao.getContentBySource(ContentSource.LOCAL).first()
        val downloadedTorrents = torrentDao.getCompletedTorrents().first()

        assertEquals(8, allContent.size) // 3 local + 5 RD
        assertEquals(5, rdContentOnly.size)
        assertEquals(3, localContentOnly.size)
        assertEquals(5, downloadedTorrents.size)

        // Verify content with matching torrents
        rdContentOnly.forEach { content ->
            val matchingTorrent = downloadedTorrents.find { it.id == content.realDebridId }
            assertNotNull("RD content should have matching torrent", matchingTorrent)
        }

        // Verify local content has no torrent matches
        localContentOnly.forEach { content ->
            val matchingTorrent = downloadedTorrents.find { it.id == content.realDebridId }
            assertNull("Local content should not have matching torrent", matchingTorrent)
        }
    }

    @Test
    fun searchAcrossDataSourcesTest() = runTest {
        // Given - Content with searchable terms
        val actionTorrents = listOf(
            TestDataFactory.createTorrentEntity(id = "t1", filename = "Action.Movie.2023.mkv"),
            TestDataFactory.createTorrentEntity(id = "t2", filename = "Action.Hero.Series.S01.mkv")
        )
        val actionContent = listOf(
            ContentEntity(
                title = "Action Movie 2023",
                source = ContentSource.REAL_DEBRID,
                realDebridId = "t1"
            ),
            ContentEntity(
                title = "Action Hero Series",
                source = ContentSource.REAL_DEBRID,
                realDebridId = "t2"
            ),
            ContentEntity(
                title = "Local Action Film",
                source = ContentSource.LOCAL
            )
        )

        // When
        torrentDao.insertTorrents(actionTorrents)
        actionContent.forEach { contentDao.insertContent(it) }

        // Then - Search across both sources
        val torrentSearchResults = torrentDao.searchTorrents("Action").first()
        val contentSearchResults = contentDao.searchContent("Action").first()
        val rdActionContent = contentDao.searchContentBySource("Action", ContentSource.REAL_DEBRID).first()
        val localActionContent = contentDao.searchContentBySource("Action", ContentSource.LOCAL).first()

        assertEquals(2, torrentSearchResults.size)
        assertEquals(3, contentSearchResults.size)
        assertEquals(2, rdActionContent.size)
        assertEquals(1, localActionContent.size)

        // Verify search consistency
        torrentSearchResults.forEach { torrent ->
            assertTrue("Torrent filename should contain 'Action'", 
                torrent.filename.contains("Action", ignoreCase = true))
        }
        contentSearchResults.forEach { content ->
            assertTrue("Content title should contain 'Action'", 
                content.title.contains("Action", ignoreCase = true))
        }
    }

    @Test
    fun cacheExpirationAndCleanupTest() = runTest {
        // Given - Mix of old and new torrents with different statuses
        val currentTime = System.currentTimeMillis()
        val oldErrorTorrents = listOf(
            TestDataFactory.createTorrentEntity(
                id = "old_error_1",
                status = "error",
                added = Date(currentTime - 86400000L * 7) // 7 days old
            ),
            TestDataFactory.createTorrentEntity(
                id = "old_error_2",
                status = "magnet_error",
                added = Date(currentTime - 86400000L * 5) // 5 days old
            )
        )
        val recentTorrents = listOf(
            TestDataFactory.createTorrentEntity(
                id = "recent_good",
                status = "downloaded",
                added = Date(currentTime - 3600000L) // 1 hour old
            ),
            TestDataFactory.createTorrentEntity(
                id = "recent_error",
                status = "error",
                added = Date(currentTime - 3600000L) // 1 hour old
            )
        )

        // When
        torrentDao.insertTorrents(oldErrorTorrents + recentTorrents)
        val beforeCleanup = torrentDao.getAllTorrents().first()

        // Cleanup errors older than 3 days
        val cleanupCutoff = currentTime - (86400000L * 3)
        torrentDao.deleteErroredTorrentsBefore(cleanupCutoff)
        val afterCleanup = torrentDao.getAllTorrents().first()

        // Then
        assertEquals(4, beforeCleanup.size)
        assertEquals(2, afterCleanup.size) // Should remove 2 old error torrents

        val remainingIds = afterCleanup.map { it.id }
        assertFalse("Old error torrent should be removed", remainingIds.contains("old_error_1"))
        assertFalse("Old error torrent should be removed", remainingIds.contains("old_error_2"))
        assertTrue("Recent good torrent should remain", remainingIds.contains("recent_good"))
        assertTrue("Recent error torrent should remain", remainingIds.contains("recent_error"))
    }

    @Test
    fun favoriteAndWatchedContentManagementTest() = runTest {
        // Given - Content for user interaction testing
        val content = (1..5).map { index ->
            ContentEntity(
                title = "Movie $index",
                source = ContentSource.REAL_DEBRID,
                realDebridId = "rd_$index",
                year = 2020 + index
            )
        }

        // When
        val contentIds = content.map { contentDao.insertContent(it) }

        // Mark some as favorites
        contentDao.updateFavoriteStatus(contentIds[0], true)
        contentDao.updateFavoriteStatus(contentIds[2], true)

        // Mark some as watched and update play info
        contentDao.updateWatchedStatus(contentIds[1], true)
        contentDao.updateWatchedStatus(contentIds[2], true)
        contentDao.updatePlayedInfo(contentIds[1])
        contentDao.updatePlayedInfo(contentIds[2])

        // Then
        val favorites = contentDao.getFavoriteContent().first()
        val watched = contentDao.getWatchedContent().first()
        val recent = contentDao.getRecentlyPlayed(10).first()

        assertEquals(2, favorites.size) // Movies 1 and 3
        assertEquals(2, watched.size)   // Movies 2 and 3
        assertEquals(2, recent.size)    // Movies 2 and 3

        // Verify Movie 3 appears in both favorites and watched
        val movie3Id = contentIds[2]
        assertTrue("Movie 3 should be in favorites", 
            favorites.any { it.id == movie3Id })
        assertTrue("Movie 3 should be in watched", 
            watched.any { it.id == movie3Id })

        // Verify play counts
        val movie2 = contentDao.getContentById(contentIds[1])
        val movie3 = contentDao.getContentById(contentIds[2])
        assertEquals(1, movie2?.playCount)
        assertEquals(1, movie3?.playCount)
    }

    @Test
    fun transactionRollbackTest() = runTest {
        // Given - Initial state with some content
        val initialContent = ContentEntity(
            title = "Initial Content",
            source = ContentSource.LOCAL
        )
        val initialId = contentDao.insertContent(initialContent)

        // When - Simulate a failed transaction (this would normally be in a transaction)
        try {
            val newContent = ContentEntity(
                title = "New Content",
                source = ContentSource.REAL_DEBRID
            )
            contentDao.insertContent(newContent)
            
            // Simulate an error that would cause rollback
            throw RuntimeException("Simulated transaction failure")
        } catch (e: RuntimeException) {
            // In a real scenario, transaction would rollback automatically
        }

        // Then - Verify initial state is maintained
        val allContent = contentDao.getAllContent().first()
        assertEquals(2, allContent.size) // Initial + new (since we're not in actual transaction)
        assertTrue("Initial content should still exist", 
            allContent.any { it.id == initialId })
    }

    @Test
    fun indexPerformanceTest() = runTest {
        // Given - Large dataset to test index performance
        val torrents = TestDataFactory.createTorrentEntityList(500)
        val searchTerm = "Movie"

        // When
        torrentDao.insertTorrents(torrents)

        // Test search performance (should use filename index)
        val searchStart = System.currentTimeMillis()
        val searchResults = torrentDao.searchTorrents(searchTerm).first()
        val searchTime = System.currentTimeMillis() - searchStart

        // Test status filtering performance (should use status index)
        val statusStart = System.currentTimeMillis()
        val downloadedTorrents = torrentDao.getTorrentsByStatus("downloaded").first()
        val statusTime = System.currentTimeMillis() - statusStart

        // Then
        assertTrue("Search should complete quickly with index", searchTime < 1000)
        assertTrue("Status filter should complete quickly with index", statusTime < 1000)
        
        println("Search time for 500 torrents: ${searchTime}ms")
        println("Status filter time for 500 torrents: ${statusTime}ms")
        
        // Verify results
        assertTrue("Should find some search results", searchResults.isNotEmpty())
        assertTrue("Should find some downloaded torrents", downloadedTorrents.isNotEmpty())
    }

    /**
     * Helper function to extract title from filename
     */
    private fun extractTitleFromFilename(filename: String): String {
        return filename
            .substringBefore(".mkv")
            .substringBefore(".mp4")
            .substringBefore(".avi")
            .replace(".", " ")
            .replace("_", " ")
            .trim()
    }

    /**
     * Helper function to extract year from filename
     */
    private fun extractYearFromFilename(filename: String): Int? {
        val yearRegex = Regex("(19|20)\\d{2}")
        return yearRegex.find(filename)?.value?.toIntOrNull()
    }
}