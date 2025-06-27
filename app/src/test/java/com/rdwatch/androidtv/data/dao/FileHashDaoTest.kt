package com.rdwatch.androidtv.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.entities.FileHashEntity
import com.rdwatch.androidtv.test.HiltTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FileHashDaoTest : HiltTestBase() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var fileHashDao: FileHashDao

    @Test
    fun insertAndRetrieveHash() = runTest {
        val testHash = FileHashEntity(
            filePath = "/test/path/video.mp4",
            hashValue = "1234567890abcdef",
            fileSize = 1024000L,
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val id = fileHashDao.insertOrUpdateHash(testHash)
        assertTrue("Insert should return positive ID", id > 0)

        val retrieved = fileHashDao.getHashByPath(testHash.filePath)
        assertNotNull("Retrieved hash should not be null", retrieved)
        assertEquals("File paths should match", testHash.filePath, retrieved?.filePath)
        assertEquals("Hash values should match", testHash.hashValue, retrieved?.hashValue)
        assertEquals("File sizes should match", testHash.fileSize, retrieved?.fileSize)
    }

    @Test
    fun getCachedHashWithValidation() = runTest {
        val testHash = FileHashEntity(
            filePath = "/test/path/video.mp4",
            hashValue = "1234567890abcdef",
            fileSize = 1024000L,
            lastModified = 1234567890L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        fileHashDao.insertOrUpdateHash(testHash)

        // Should find hash with matching metadata
        val validHash = fileHashDao.getCachedHash(
            testHash.filePath,
            testHash.fileSize,
            testHash.lastModified
        )
        assertNotNull("Should find hash with matching metadata", validHash)
        assertEquals("Hash values should match", testHash.hashValue, validHash?.hashValue)

        // Should not find hash with different file size
        val invalidSizeHash = fileHashDao.getCachedHash(
            testHash.filePath,
            testHash.fileSize + 1000,
            testHash.lastModified
        )
        assertNull("Should not find hash with different file size", invalidSizeHash)

        // Should not find hash with different last modified time
        val invalidTimeHash = fileHashDao.getCachedHash(
            testHash.filePath,
            testHash.fileSize,
            testHash.lastModified + 1000
        )
        assertNull("Should not find hash with different last modified time", invalidTimeHash)
    }

    @Test
    fun updateExistingHash() = runTest {
        val originalHash = FileHashEntity(
            filePath = "/test/path/video.mp4",
            hashValue = "1234567890abcdef",
            fileSize = 1024000L,
            lastModified = 1234567890L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val id = fileHashDao.insertOrUpdateHash(originalHash)

        // Update with same path but different content
        val updatedHash = originalHash.copy(
            id = id,
            hashValue = "fedcba0987654321",
            fileSize = 2048000L,
            lastModified = 1234567891L,
            updatedAt = System.currentTimeMillis()
        )

        fileHashDao.updateHash(updatedHash)

        val retrieved = fileHashDao.getHashByPath(originalHash.filePath)
        assertNotNull("Updated hash should be retrievable", retrieved)
        assertEquals("Hash value should be updated", updatedHash.hashValue, retrieved?.hashValue)
        assertEquals("File size should be updated", updatedHash.fileSize, retrieved?.fileSize)
        assertEquals("Last modified should be updated", updatedHash.lastModified, retrieved?.lastModified)
    }

    @Test
    fun deleteHashByPath() = runTest {
        val testHash = FileHashEntity(
            filePath = "/test/path/video.mp4",
            hashValue = "1234567890abcdef",
            fileSize = 1024000L,
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        fileHashDao.insertOrUpdateHash(testHash)

        // Verify hash exists
        val beforeDelete = fileHashDao.getHashByPath(testHash.filePath)
        assertNotNull("Hash should exist before deletion", beforeDelete)

        // Delete hash
        fileHashDao.deleteHashByPath(testHash.filePath)

        // Verify hash is deleted
        val afterDelete = fileHashDao.getHashByPath(testHash.filePath)
        assertNull("Hash should not exist after deletion", afterDelete)
    }

    @Test
    fun deleteOldEntries() = runTest {
        val currentTime = System.currentTimeMillis()
        val oldTime = currentTime - (8 * 24 * 60 * 60 * 1000L) // 8 days ago
        val recentTime = currentTime - (6 * 24 * 60 * 60 * 1000L) // 6 days ago

        // Insert old hash
        val oldHash = FileHashEntity(
            filePath = "/test/path/old_video.mp4",
            hashValue = "1111111111111111",
            fileSize = 1024000L,
            lastModified = oldTime,
            createdAt = oldTime,
            updatedAt = oldTime
        )

        // Insert recent hash
        val recentHash = FileHashEntity(
            filePath = "/test/path/recent_video.mp4",
            hashValue = "2222222222222222",
            fileSize = 1024000L,
            lastModified = recentTime,
            createdAt = recentTime,
            updatedAt = recentTime
        )

        fileHashDao.insertOrUpdateHash(oldHash)
        fileHashDao.insertOrUpdateHash(recentHash)

        // Delete entries older than 7 days
        val cutoffTime = currentTime - (7 * 24 * 60 * 60 * 1000L)
        fileHashDao.deleteOldEntries(cutoffTime)

        // Old hash should be deleted
        val oldRetrieved = fileHashDao.getHashByPath(oldHash.filePath)
        assertNull("Old hash should be deleted", oldRetrieved)

        // Recent hash should remain
        val recentRetrieved = fileHashDao.getHashByPath(recentHash.filePath)
        assertNotNull("Recent hash should remain", recentRetrieved)
    }

    @Test
    fun getAllHashesFlow() = runTest {
        // Initially should be empty
        val initialHashes = fileHashDao.getAllHashes().first()
        assertTrue("Initial list should be empty", initialHashes.isEmpty())

        // Insert test hashes
        val hash1 = FileHashEntity(
            filePath = "/test/path/video1.mp4",
            hashValue = "1111111111111111",
            fileSize = 1024000L,
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val hash2 = FileHashEntity(
            filePath = "/test/path/video2.mp4",
            hashValue = "2222222222222222",
            fileSize = 2048000L,
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis() + 1000,
            updatedAt = System.currentTimeMillis() + 1000
        )

        fileHashDao.insertOrUpdateHash(hash1)
        fileHashDao.insertOrUpdateHash(hash2)

        // Retrieve all hashes
        val allHashes = fileHashDao.getAllHashes().first()
        assertEquals("Should have 2 hashes", 2, allHashes.size)
        
        // Should be ordered by created_at DESC (newest first)
        assertEquals("Newest hash should be first", hash2.filePath, allHashes[0].filePath)
        assertEquals("Oldest hash should be second", hash1.filePath, allHashes[1].filePath)
    }

    @Test
    fun getHashCount() = runTest {
        // Initially should be 0
        val initialCount = fileHashDao.getHashCount()
        assertEquals("Initial count should be 0", 0, initialCount)

        // Insert hash
        val testHash = FileHashEntity(
            filePath = "/test/path/video.mp4",
            hashValue = "1234567890abcdef",
            fileSize = 1024000L,
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        fileHashDao.insertOrUpdateHash(testHash)

        val afterInsertCount = fileHashDao.getHashCount()
        assertEquals("Count should be 1 after insert", 1, afterInsertCount)
    }

    @Test
    fun hashExists() = runTest {
        val testHash = FileHashEntity(
            filePath = "/test/path/video.mp4",
            hashValue = "1234567890abcdef",
            fileSize = 1024000L,
            lastModified = 1234567890L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Should not exist initially
        val initialExists = fileHashDao.hashExists(
            testHash.filePath,
            testHash.fileSize,
            testHash.lastModified
        )
        assertFalse("Hash should not exist initially", initialExists)

        // Insert hash
        fileHashDao.insertOrUpdateHash(testHash)

        // Should exist with exact metadata
        val existsAfterInsert = fileHashDao.hashExists(
            testHash.filePath,
            testHash.fileSize,
            testHash.lastModified
        )
        assertTrue("Hash should exist after insert", existsAfterInsert)

        // Should not exist with different metadata
        val existsWithDifferentSize = fileHashDao.hashExists(
            testHash.filePath,
            testHash.fileSize + 1000,
            testHash.lastModified
        )
        assertFalse("Hash should not exist with different file size", existsWithDifferentSize)
    }

    @Test
    fun deleteAllHashes() = runTest {
        // Insert multiple hashes
        val hash1 = FileHashEntity(
            filePath = "/test/path/video1.mp4",
            hashValue = "1111111111111111",
            fileSize = 1024000L,
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val hash2 = FileHashEntity(
            filePath = "/test/path/video2.mp4",
            hashValue = "2222222222222222",
            fileSize = 2048000L,
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        fileHashDao.insertOrUpdateHash(hash1)
        fileHashDao.insertOrUpdateHash(hash2)

        // Verify hashes exist
        val beforeDeleteCount = fileHashDao.getHashCount()
        assertEquals("Should have 2 hashes before delete", 2, beforeDeleteCount)

        // Delete all hashes
        fileHashDao.deleteAllHashes()

        // Verify all hashes are deleted
        val afterDeleteCount = fileHashDao.getHashCount()
        assertEquals("Should have 0 hashes after delete all", 0, afterDeleteCount)
    }
}