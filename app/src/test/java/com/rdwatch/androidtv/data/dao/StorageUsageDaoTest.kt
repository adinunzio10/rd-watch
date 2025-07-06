package com.rdwatch.androidtv.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.entities.StorageUsageEntity
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
class StorageUsageDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var storageUsageDao: StorageUsageDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        storageUsageDao = database.storageUsageDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetCurrentStorageUsage() = runTest {
        val testUsage = createTestStorageUsage()
        
        storageUsageDao.insertStorageUsage(testUsage)
        val retrieved = storageUsageDao.getCurrentStorageUsage()
        
        assertNotNull(retrieved)
        assertEquals(testUsage.totalSpaceBytes, retrieved.totalSpaceBytes)
        assertEquals(testUsage.usedSpaceBytes, retrieved.usedSpaceBytes)
        assertEquals(testUsage.fileCount, retrieved.fileCount)
    }

    @Test
    fun upsertStorageUsage() = runTest {
        val initialUsage = createTestStorageUsage()
        val updatedUsage = initialUsage.copy(
            usedSpaceBytes = 2048L,
            fileCount = 10
        )
        
        storageUsageDao.upsertStorageUsage(initialUsage)
        storageUsageDao.upsertStorageUsage(updatedUsage)
        
        val retrieved = storageUsageDao.getCurrentStorageUsage()
        assertNotNull(retrieved)
        assertEquals(2048L, retrieved.usedSpaceBytes)
        assertEquals(10, retrieved.fileCount)
    }

    @Test
    fun getCurrentStorageUsageFlow() = runTest {
        val testUsage = createTestStorageUsage()
        
        storageUsageDao.insertStorageUsage(testUsage)
        val flow = storageUsageDao.getCurrentStorageUsageFlow()
        val retrieved = flow.first()
        
        assertNotNull(retrieved)
        assertEquals(testUsage.id, retrieved.id)
    }

    @Test
    fun updateSpecificFields() = runTest {
        val testUsage = createTestStorageUsage()
        storageUsageDao.insertStorageUsage(testUsage)
        
        storageUsageDao.updateFileCount(25)
        storageUsageDao.updateUsedSpace(5000L)
        
        val retrieved = storageUsageDao.getCurrentStorageUsage()
        assertNotNull(retrieved)
        assertEquals(25, retrieved.fileCount)
        assertEquals(5000L, retrieved.usedSpaceBytes)
    }

    @Test
    fun incrementOperations() = runTest {
        val testUsage = createTestStorageUsage()
        storageUsageDao.insertStorageUsage(testUsage)
        
        storageUsageDao.incrementFileCount(5)
        storageUsageDao.incrementUsedSpace(1000L)
        
        val retrieved = storageUsageDao.getCurrentStorageUsage()
        assertNotNull(retrieved)
        assertEquals(10, retrieved.fileCount) // 5 + 5
        assertEquals(2024L, retrieved.usedSpaceBytes) // 1024 + 1000
    }

    @Test
    fun decrementOperations() = runTest {
        val testUsage = createTestStorageUsage(fileCount = 10, usedSpace = 2000L)
        storageUsageDao.insertStorageUsage(testUsage)
        
        storageUsageDao.decrementFileCount(3)
        storageUsageDao.decrementUsedSpace(500L)
        
        val retrieved = storageUsageDao.getCurrentStorageUsage()
        assertNotNull(retrieved)
        assertEquals(7, retrieved.fileCount) // 10 - 3
        assertEquals(1500L, retrieved.usedSpaceBytes) // 2000 - 500
    }

    @Test
    fun hasCurrentStorageUsage() = runTest {
        var hasUsage = storageUsageDao.hasCurrentStorageUsage()
        assertEquals(false, hasUsage)
        
        val testUsage = createTestStorageUsage()
        storageUsageDao.insertStorageUsage(testUsage)
        
        hasUsage = storageUsageDao.hasCurrentStorageUsage()
        assertTrue(hasUsage)
    }

    @Test
    fun deleteStorageUsage() = runTest {
        val testUsage = createTestStorageUsage()
        storageUsageDao.insertStorageUsage(testUsage)
        
        storageUsageDao.deleteStorageUsageById("current")
        val retrieved = storageUsageDao.getCurrentStorageUsage()
        
        assertNull(retrieved)
    }

    @Test
    fun getFreshStorageUsage() = runTest {
        val testUsage = createTestStorageUsage()
        storageUsageDao.insertStorageUsage(testUsage)
        
        // Should be fresh since just inserted
        val fresh = storageUsageDao.getFreshStorageUsage(300_000L) // 5 minutes
        assertNotNull(fresh)
        
        // Test with very short TTL
        val stale = storageUsageDao.getFreshStorageUsage(1L) // 1 millisecond
        assertNull(stale)
    }

    private fun createTestStorageUsage(
        totalSpace: Long = 10_000L,
        usedSpace: Long = 1_024L,
        freeSpace: Long = 8_976L,
        fileCount: Int = 5,
        torrentCount: Int = 2,
        downloadCount: Int = 3
    ): StorageUsageEntity {
        return StorageUsageEntity(
            id = "current",
            totalSpaceBytes = totalSpace,
            usedSpaceBytes = usedSpace,
            freeSpaceBytes = freeSpace,
            fileCount = fileCount,
            torrentCount = torrentCount,
            downloadCount = downloadCount,
            lastUpdated = System.currentTimeMillis()
        )
    }
}