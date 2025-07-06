package com.rdwatch.androidtv.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.entities.FileTypeStatsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class FileTypeStatsDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var fileTypeStatsDao: FileTypeStatsDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        fileTypeStatsDao = database.fileTypeStatsDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetFileTypeStats() = runTest {
        val testStats = createTestFileTypeStats("VIDEO")
        
        fileTypeStatsDao.insertFileTypeStats(testStats)
        val retrieved = fileTypeStatsDao.getFileTypeStatsByType("VIDEO")
        
        assertNotNull(retrieved)
        assertEquals(testStats.fileType, retrieved.fileType)
        assertEquals(testStats.fileCount, retrieved.fileCount)
        assertEquals(testStats.totalSizeBytes, retrieved.totalSizeBytes)
    }

    @Test
    fun upsertFileTypeStats() = runTest {
        val initialStats = createTestFileTypeStats("AUDIO", 5, 1000L)
        val updatedStats = initialStats.copy(
            fileCount = 10,
            totalSizeBytes = 2000L
        )
        
        fileTypeStatsDao.upsertFileTypeStats(initialStats)
        fileTypeStatsDao.upsertFileTypeStats(updatedStats)
        
        val retrieved = fileTypeStatsDao.getFileTypeStatsByType("AUDIO")
        assertNotNull(retrieved)
        assertEquals(10, retrieved.fileCount)
        assertEquals(2000L, retrieved.totalSizeBytes)
    }

    @Test
    fun getAllFileTypeStats() = runTest {
        val testStats = listOf(
            createTestFileTypeStats("VIDEO", 10, 5000L),
            createTestFileTypeStats("AUDIO", 5, 1000L),
            createTestFileTypeStats("IMAGE", 20, 500L)
        )
        
        fileTypeStatsDao.insertFileTypeStats(testStats)
        val allStats = fileTypeStatsDao.getAllFileTypeStats().first()
        
        assertEquals(3, allStats.size)
    }

    @Test
    fun incrementStats() = runTest {
        val testStats = createTestFileTypeStats("VIDEO", 5, 1000L)
        fileTypeStatsDao.insertFileTypeStats(testStats)
        
        fileTypeStatsDao.incrementStats("VIDEO", 3, 500L)
        
        val retrieved = fileTypeStatsDao.getFileTypeStatsByType("VIDEO")
        assertNotNull(retrieved)
        assertEquals(8, retrieved.fileCount) // 5 + 3
        assertEquals(1500L, retrieved.totalSizeBytes) // 1000 + 500
    }

    @Test
    fun decrementStats() = runTest {
        val testStats = createTestFileTypeStats("AUDIO", 10, 2000L)
        fileTypeStatsDao.insertFileTypeStats(testStats)
        
        fileTypeStatsDao.decrementStats("AUDIO", 2, 400L)
        
        val retrieved = fileTypeStatsDao.getFileTypeStatsByType("AUDIO")
        assertNotNull(retrieved)
        assertEquals(8, retrieved.fileCount) // 10 - 2
        assertEquals(1600L, retrieved.totalSizeBytes) // 2000 - 400
    }

    @Test
    fun getTotalFileCount() = runTest {
        val testStats = listOf(
            createTestFileTypeStats("VIDEO", 10, 5000L),
            createTestFileTypeStats("AUDIO", 5, 1000L),
            createTestFileTypeStats("IMAGE", 15, 500L)
        )
        
        fileTypeStatsDao.insertFileTypeStats(testStats)
        val totalCount = fileTypeStatsDao.getTotalFileCount()
        
        assertEquals(30, totalCount) // 10 + 5 + 15
    }

    @Test
    fun getTotalSizeBytes() = runTest {
        val testStats = listOf(
            createTestFileTypeStats("VIDEO", 10, 5000L),
            createTestFileTypeStats("AUDIO", 5, 1000L),
            createTestFileTypeStats("IMAGE", 15, 500L)
        )
        
        fileTypeStatsDao.insertFileTypeStats(testStats)
        val totalSize = fileTypeStatsDao.getTotalSizeBytes()
        
        assertEquals(6500L, totalSize) // 5000 + 1000 + 500
    }

    @Test
    fun getTopFileTypesByCount() = runTest {
        val testStats = listOf(
            createTestFileTypeStats("VIDEO", 20, 5000L),
            createTestFileTypeStats("AUDIO", 15, 1000L),
            createTestFileTypeStats("IMAGE", 5, 500L),
            createTestFileTypeStats("TEXT", 25, 100L)
        )
        
        fileTypeStatsDao.insertFileTypeStats(testStats)
        val topTypes = fileTypeStatsDao.getTopFileTypesByCount(2)
        
        assertEquals(2, topTypes.size)
        assertEquals("TEXT", topTypes[0].fileType) // 25 files
        assertEquals("VIDEO", topTypes[1].fileType) // 20 files
    }

    @Test
    fun getTopFileTypesBySize() = runTest {
        val testStats = listOf(
            createTestFileTypeStats("VIDEO", 10, 8000L),
            createTestFileTypeStats("AUDIO", 5, 2000L),
            createTestFileTypeStats("IMAGE", 15, 500L),
            createTestFileTypeStats("TEXT", 25, 100L)
        )
        
        fileTypeStatsDao.insertFileTypeStats(testStats)
        val topTypes = fileTypeStatsDao.getTopFileTypesBySize(2)
        
        assertEquals(2, topTypes.size)
        assertEquals("VIDEO", topTypes[0].fileType) // 8000 bytes
        assertEquals("AUDIO", topTypes[1].fileType) // 2000 bytes
    }

    @Test
    fun existsByFileType() = runTest {
        val testStats = createTestFileTypeStats("VIDEO")
        fileTypeStatsDao.insertFileTypeStats(testStats)
        
        val existsVideo = fileTypeStatsDao.existsByFileType("VIDEO")
        val existsAudio = fileTypeStatsDao.existsByFileType("AUDIO")
        
        assertTrue(existsVideo)
        assertEquals(false, existsAudio)
    }

    @Test
    fun deleteFileTypeStats() = runTest {
        val testStats = createTestFileTypeStats("IMAGE")
        fileTypeStatsDao.insertFileTypeStats(testStats)
        
        fileTypeStatsDao.deleteFileTypeStatsByType("IMAGE")
        val retrieved = fileTypeStatsDao.getFileTypeStatsByType("IMAGE")
        
        assertNull(retrieved)
    }

    @Test
    fun ensureFileTypeExists() = runTest {
        // Test that ensureFileTypeExists creates a new entry
        fileTypeStatsDao.ensureFileTypeExists("NEW_TYPE")
        
        val exists = fileTypeStatsDao.existsByFileType("NEW_TYPE")
        assertTrue(exists)
        
        val stats = fileTypeStatsDao.getFileTypeStatsByType("NEW_TYPE")
        assertNotNull(stats)
        assertEquals("NEW_TYPE", stats.fileType)
        assertEquals(0, stats.fileCount)
        assertEquals(0L, stats.totalSizeBytes)
    }

    @Test
    fun addFileToStats() = runTest {
        // Test adding a file creates the type if it doesn't exist
        fileTypeStatsDao.addFileToStats("VIDEO", 1000L)
        
        val stats = fileTypeStatsDao.getFileTypeStatsByType("VIDEO")
        assertNotNull(stats)
        assertEquals(1, stats.fileCount)
        assertEquals(1000L, stats.totalSizeBytes)
        
        // Test adding another file increments existing stats
        fileTypeStatsDao.addFileToStats("VIDEO", 2000L)
        
        val updatedStats = fileTypeStatsDao.getFileTypeStatsByType("VIDEO")
        assertNotNull(updatedStats)
        assertEquals(2, updatedStats.fileCount)
        assertEquals(3000L, updatedStats.totalSizeBytes)
    }

    private fun createTestFileTypeStats(
        fileType: String,
        fileCount: Int = 10,
        totalSizeBytes: Long = 1024L
    ): FileTypeStatsEntity {
        return FileTypeStatsEntity(
            id = fileType,
            fileType = fileType,
            fileCount = fileCount,
            totalSizeBytes = totalSizeBytes,
            lastUpdated = System.currentTimeMillis()
        )
    }
}