package com.rdwatch.androidtv.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.AppDatabase
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
 * Test class for TorrentDao
 * Tests all CRUD operations, queries, and file browser specific functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TorrentDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var torrentDao: TorrentDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries()
            .build()
        torrentDao = database.torrentDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTorrent() = runTest {
        // Given
        val torrent = TestDataFactory.createTorrentEntity(
            id = "test_insert_id",
            filename = "Test.Movie.mkv"
        )

        // When
        torrentDao.insertTorrent(torrent)
        val retrieved = torrentDao.getTorrentById("test_insert_id")

        // Then
        assertNotNull(retrieved)
        assertEquals(torrent.id, retrieved?.id)
        assertEquals(torrent.filename, retrieved?.filename)
        assertEquals(torrent.hash, retrieved?.hash)
        assertEquals(torrent.bytes, retrieved?.bytes)
        assertEquals(torrent.status, retrieved?.status)
    }

    @Test
    fun insertMultipleTorrentsAndRetrieveAll() = runTest {
        // Given
        val torrents = TestDataFactory.createTorrentEntityList(5)

        // When
        torrentDao.insertTorrents(torrents)
        val allTorrents = torrentDao.getAllTorrents().first()

        // Then
        assertEquals(5, allTorrents.size)
        // Verify order - should be ordered by added date DESC
        assertTrue(allTorrents[0].added >= allTorrents[1].added)
    }

    @Test
    fun getTorrentByHash() = runTest {
        // Given
        val torrent = TestDataFactory.createTorrentEntity(
            id = "hash_test_id",
            hash = "unique_hash_123"
        )

        // When
        torrentDao.insertTorrent(torrent)
        val retrieved = torrentDao.getTorrentByHash("unique_hash_123")

        // Then
        assertNotNull(retrieved)
        assertEquals("hash_test_id", retrieved?.id)
        assertEquals("unique_hash_123", retrieved?.hash)
    }

    @Test
    fun getTorrentsByStatus() = runTest {
        // Given
        val downloadingTorrents = TestDataFactory.createTorrentEntityList(3, 0, "downloading")
        val downloadedTorrents = TestDataFactory.createTorrentEntityList(2, 3, "downloaded")
        val allTorrents = downloadingTorrents + downloadedTorrents

        // When
        torrentDao.insertTorrents(allTorrents)
        val downloading = torrentDao.getTorrentsByStatus("downloading").first()
        val downloaded = torrentDao.getTorrentsByStatus("downloaded").first()

        // Then
        assertEquals(3, downloading.size)
        assertEquals(2, downloaded.size)
        downloading.forEach { assertEquals("downloading", it.status) }
        downloaded.forEach { assertEquals("downloaded", it.status) }
    }

    @Test
    fun searchTorrents() = runTest {
        // Given
        val torrents = listOf(
            TestDataFactory.createTorrentEntity(id = "1", filename = "Action.Movie.2023.mkv"),
            TestDataFactory.createTorrentEntity(id = "2", filename = "Comedy.Show.2023.mp4"),
            TestDataFactory.createTorrentEntity(id = "3", filename = "Drama.Series.S01.mkv"),
            TestDataFactory.createTorrentEntity(id = "4", filename = "Action.Hero.2022.mkv")
        )

        // When
        torrentDao.insertTorrents(torrents)
        val actionMovies = torrentDao.searchTorrents("Action").first()
        val movieResults = torrentDao.searchTorrents("Movie").first()
        val year2023 = torrentDao.searchTorrents("2023").first()

        // Then
        assertEquals(2, actionMovies.size) // Action.Movie and Action.Hero
        assertEquals(1, movieResults.size) // Action.Movie
        assertEquals(2, year2023.size) // Action.Movie and Comedy.Show
    }

    @Test
    fun getActiveTorrents() = runTest {
        // Given
        val torrents = listOf(
            TestDataFactory.createTorrentEntity(id = "1", status = "downloading"),
            TestDataFactory.createTorrentEntity(id = "2", status = "queued"),
            TestDataFactory.createTorrentEntity(id = "3", status = "downloaded"),
            TestDataFactory.createTorrentEntity(id = "4", status = "error")
        )

        // When
        torrentDao.insertTorrents(torrents)
        val activeTorrents = torrentDao.getActiveTorrents().first()

        // Then
        assertEquals(2, activeTorrents.size)
        val activeStatuses = activeTorrents.map { it.status }
        assertTrue(activeStatuses.contains("downloading"))
        assertTrue(activeStatuses.contains("queued"))
    }

    @Test
    fun getCompletedTorrents() = runTest {
        // Given
        val torrents = listOf(
            TestDataFactory.createTorrentEntity(id = "1", status = "downloading"),
            TestDataFactory.createTorrentEntity(id = "2", status = "downloaded"),
            TestDataFactory.createTorrentEntity(id = "3", status = "downloaded"),
            TestDataFactory.createTorrentEntity(id = "4", status = "error")
        )

        // When
        torrentDao.insertTorrents(torrents)
        val completedTorrents = torrentDao.getCompletedTorrents().first()

        // Then
        assertEquals(2, completedTorrents.size)
        completedTorrents.forEach { assertEquals("downloaded", it.status) }
    }

    @Test
    fun updateTorrent() = runTest {
        // Given
        val originalTorrent = TestDataFactory.createTorrentEntity(
            id = "update_test",
            filename = "Original.Name.mkv",
            progress = 50f
        )

        // When
        torrentDao.insertTorrent(originalTorrent)
        val updatedTorrent = originalTorrent.copy(
            filename = "Updated.Name.mkv",
            progress = 100f,
            status = "downloaded"
        )
        torrentDao.updateTorrent(updatedTorrent)
        val retrieved = torrentDao.getTorrentById("update_test")

        // Then
        assertNotNull(retrieved)
        assertEquals("Updated.Name.mkv", retrieved?.filename)
        assertEquals(100f, retrieved?.progress)
        assertEquals("downloaded", retrieved?.status)
    }

    @Test
    fun upsertTorrent() = runTest {
        // Given
        val torrent = TestDataFactory.createTorrentEntity(
            id = "upsert_test",
            filename = "Original.mkv"
        )

        // When - First upsert (insert)
        torrentDao.upsertTorrent(torrent)
        val afterInsert = torrentDao.getTorrentById("upsert_test")

        // Then
        assertNotNull(afterInsert)
        assertEquals("Original.mkv", afterInsert?.filename)

        // When - Second upsert (update)
        val updatedTorrent = torrent.copy(filename = "Updated.mkv")
        torrentDao.upsertTorrent(updatedTorrent)
        val afterUpdate = torrentDao.getTorrentById("upsert_test")

        // Then
        assertNotNull(afterUpdate)
        assertEquals("Updated.mkv", afterUpdate?.filename)
    }

    @Test
    fun upsertMultipleTorrents() = runTest {
        // Given
        val torrents = TestDataFactory.createTorrentEntityList(3)

        // When - First upsert (insert all)
        torrentDao.upsertTorrents(torrents)
        val afterInsert = torrentDao.getAllTorrents().first()

        // Then
        assertEquals(3, afterInsert.size)

        // When - Second upsert (update existing)
        val updatedTorrents = torrents.map { it.copy(progress = 100f, status = "downloaded") }
        torrentDao.upsertTorrents(updatedTorrents)
        val afterUpdate = torrentDao.getAllTorrents().first()

        // Then
        assertEquals(3, afterUpdate.size)
        afterUpdate.forEach { 
            assertEquals(100f, it.progress)
            assertEquals("downloaded", it.status)
        }
    }

    @Test
    fun deleteTorrent() = runTest {
        // Given
        val torrent = TestDataFactory.createTorrentEntity(id = "delete_test")

        // When
        torrentDao.insertTorrent(torrent)
        val beforeDelete = torrentDao.getTorrentById("delete_test")
        torrentDao.deleteTorrent(torrent)
        val afterDelete = torrentDao.getTorrentById("delete_test")

        // Then
        assertNotNull(beforeDelete)
        assertNull(afterDelete)
    }

    @Test
    fun deleteTorrentById() = runTest {
        // Given
        val torrent = TestDataFactory.createTorrentEntity(id = "delete_by_id_test")

        // When
        torrentDao.insertTorrent(torrent)
        val beforeDelete = torrentDao.getTorrentById("delete_by_id_test")
        torrentDao.deleteTorrentById("delete_by_id_test")
        val afterDelete = torrentDao.getTorrentById("delete_by_id_test")

        // Then
        assertNotNull(beforeDelete)
        assertNull(afterDelete)
    }

    @Test
    fun updateTorrentProgress() = runTest {
        // Given
        val torrent = TestDataFactory.createTorrentEntity(
            id = "progress_test",
            progress = 0f,
            status = "downloading"
        )

        // When
        torrentDao.insertTorrent(torrent)
        torrentDao.updateTorrentProgress("progress_test", 75f, "downloading")
        val updated = torrentDao.getTorrentById("progress_test")

        // Then
        assertNotNull(updated)
        assertEquals(75f, updated?.progress)
        assertEquals("downloading", updated?.status)
    }

    @Test
    fun updateTorrentStats() = runTest {
        // Given
        val torrent = TestDataFactory.createTorrentEntity(
            id = "stats_test",
            speed = 1000L,
            seeders = 10
        )

        // When
        torrentDao.insertTorrent(torrent)
        torrentDao.updateTorrentStats("stats_test", 5000L, 25)
        val updated = torrentDao.getTorrentById("stats_test")

        // Then
        assertNotNull(updated)
        assertEquals(5000L, updated?.speed)
        assertEquals(25, updated?.seeders)
    }

    @Test
    fun deleteErroredTorrentsBefore() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val oldDate = Date(currentTime - 86400000L) // 1 day ago
        val recentDate = Date(currentTime - 3600000L) // 1 hour ago
        
        val torrents = listOf(
            TestDataFactory.createTorrentEntity(id = "old_error", status = "error", added = oldDate),
            TestDataFactory.createTorrentEntity(id = "recent_error", status = "error", added = recentDate),
            TestDataFactory.createTorrentEntity(id = "old_ok", status = "downloaded", added = oldDate),
            TestDataFactory.createTorrentEntity(id = "recent_ok", status = "downloaded", added = recentDate)
        )

        // When
        torrentDao.insertTorrents(torrents)
        val beforeCleanup = torrentDao.getAllTorrents().first()
        
        // Delete errored torrents older than 30 minutes
        torrentDao.deleteErroredTorrentsBefore(currentTime - 1800000L) // 30 min ago
        val afterCleanup = torrentDao.getAllTorrents().first()

        // Then
        assertEquals(4, beforeCleanup.size)
        assertEquals(3, afterCleanup.size) // old_error should be deleted
        
        val remainingIds = afterCleanup.map { it.id }
        assertFalse(remainingIds.contains("old_error"))
        assertTrue(remainingIds.contains("recent_error"))
        assertTrue(remainingIds.contains("old_ok"))
        assertTrue(remainingIds.contains("recent_ok"))
    }

    @Test
    fun getActiveTorrentCount() = runTest {
        // Given
        val torrents = listOf(
            TestDataFactory.createTorrentEntity(id = "1", status = "downloading"),
            TestDataFactory.createTorrentEntity(id = "2", status = "queued"),
            TestDataFactory.createTorrentEntity(id = "3", status = "downloading"),
            TestDataFactory.createTorrentEntity(id = "4", status = "downloaded"),
            TestDataFactory.createTorrentEntity(id = "5", status = "error")
        )

        // When
        torrentDao.insertTorrents(torrents)
        val count = torrentDao.getActiveTorrentCount()

        // Then
        assertEquals(3, count) // 2 downloading + 1 queued
    }

    @Test
    fun onConflictReplaceStrategy() = runTest {
        // Given
        val originalTorrent = TestDataFactory.createTorrentEntity(
            id = "conflict_test",
            filename = "Original.mkv",
            progress = 50f
        )

        // When
        torrentDao.insertTorrent(originalTorrent)
        val afterFirstInsert = torrentDao.getTorrentById("conflict_test")

        // Insert same ID with different data (should replace)
        val replacementTorrent = originalTorrent.copy(
            filename = "Replacement.mkv",
            progress = 100f
        )
        torrentDao.insertTorrent(replacementTorrent)
        val afterReplacement = torrentDao.getTorrentById("conflict_test")

        // Then
        assertNotNull(afterFirstInsert)
        assertEquals("Original.mkv", afterFirstInsert?.filename)
        assertEquals(50f, afterFirstInsert?.progress)

        assertNotNull(afterReplacement)
        assertEquals("Replacement.mkv", afterReplacement?.filename)
        assertEquals(100f, afterReplacement?.progress)
    }

    @Test
    fun flowUpdatesWhenDataChanges() = runTest {
        // Given
        val torrent = TestDataFactory.createTorrentEntity(
            id = "flow_test",
            status = "downloading"
        )

        // When
        torrentDao.insertTorrent(torrent)
        val initialActive = torrentDao.getActiveTorrents().first()

        torrentDao.updateTorrentProgress("flow_test", 100f, "downloaded")
        val updatedActive = torrentDao.getActiveTorrents().first()

        // Then
        assertEquals(1, initialActive.size)
        assertEquals(0, updatedActive.size) // Should be empty after status change
    }

    @Test
    fun handleNullValues() = runTest {
        // Given
        val torrentWithNulls = TestDataFactory.createTorrentEntity(
            id = "null_test",
            speed = null,
            seeders = null,
            created = null,
            ended = null
        )

        // When
        torrentDao.insertTorrent(torrentWithNulls)
        val retrieved = torrentDao.getTorrentById("null_test")

        // Then
        assertNotNull(retrieved)
        assertNull(retrieved?.speed)
        assertNull(retrieved?.seeders)
        assertNull(retrieved?.created)
        assertNull(retrieved?.ended)
    }

    @Test
    fun orderingByAddedDateDesc() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val torrents = listOf(
            TestDataFactory.createTorrentEntity(
                id = "oldest",
                added = Date(currentTime - 172800000L) // 2 days ago
            ),
            TestDataFactory.createTorrentEntity(
                id = "newest",
                added = Date(currentTime) // now
            ),
            TestDataFactory.createTorrentEntity(
                id = "middle",
                added = Date(currentTime - 86400000L) // 1 day ago
            )
        )

        // When
        torrentDao.insertTorrents(torrents)
        val allTorrents = torrentDao.getAllTorrents().first()

        // Then
        assertEquals(3, allTorrents.size)
        assertEquals("newest", allTorrents[0].id)
        assertEquals("middle", allTorrents[1].id)
        assertEquals("oldest", allTorrents[2].id)
    }
}