package com.rdwatch.androidtv.util

import android.content.Context
import com.rdwatch.androidtv.data.dao.FileHashDao
import com.rdwatch.androidtv.data.entities.FileHashEntity
import com.rdwatch.androidtv.test.HiltTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltAndroidTest
class HashCalculatorTest : HiltTestBase() {

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var fileHashDao: FileHashDao

    private lateinit var hashCalculator: HashCalculator
    private lateinit var testFile: File

    @Before
    fun setup() {
        super.setUp()
        hashCalculator = HashCalculator(context, fileHashDao)
        
        // Create a test file for hash calculation
        testFile = File.createTempFile("test_video", ".mp4")
        testFile.deleteOnExit()
    }

    @Test
    fun `calculateHash should return null for files smaller than 64KB`() = runTest {
        // Create a small test file (less than 64KB)
        val smallData = ByteArray(1024) { it.toByte() } // 1KB
        FileOutputStream(testFile).use { it.write(smallData) }

        val hash = hashCalculator.calculateHash(testFile.absolutePath, useCache = false)
        assertNull("Hash should be null for files smaller than 64KB", hash)
    }

    @Test
    fun `calculateHash should return valid hash for files larger than 64KB`() = runTest {
        // Create a test file larger than 128KB (so we can test first + last 64KB)
        val fileSize = 200 * 1024 // 200KB
        val testData = ByteArray(fileSize) { (it % 256).toByte() }
        FileOutputStream(testFile).use { it.write(testData) }

        val hash = hashCalculator.calculateHash(testFile.absolutePath, useCache = false)
        
        assertNotNull("Hash should not be null for valid file", hash)
        assertEquals("Hash should be 16 characters long", 16, hash?.length)
        assertTrue("Hash should be hexadecimal", hash?.matches(Regex("[0-9a-f]+")) == true)
    }

    @Test
    fun `calculateHash should return consistent results for same file`() = runTest {
        // Create a test file
        val fileSize = 150 * 1024 // 150KB
        val testData = ByteArray(fileSize) { (it % 256).toByte() }
        FileOutputStream(testFile).use { it.write(testData) }

        val hash1 = hashCalculator.calculateHash(testFile.absolutePath, useCache = false)
        val hash2 = hashCalculator.calculateHash(testFile.absolutePath, useCache = false)
        
        assertNotNull("First hash should not be null", hash1)
        assertNotNull("Second hash should not be null", hash2)
        assertEquals("Hashes should be identical for same file", hash1, hash2)
    }

    @Test
    fun `calculateHash should use cache when available`() = runTest {
        // Create a test file
        val fileSize = 150 * 1024 // 150KB
        val testData = ByteArray(fileSize) { (it % 256).toByte() }
        FileOutputStream(testFile).use { it.write(testData) }
        
        val filePath = testFile.absolutePath
        val fileLastModified = testFile.lastModified()
        val fileLength = testFile.length()

        // First calculation should cache the result
        val hash1 = hashCalculator.calculateHash(filePath, useCache = true)
        assertNotNull("First hash should not be null", hash1)

        // Verify hash was cached
        val cachedHash = fileHashDao.getCachedHash(filePath, fileLength, fileLastModified)
        assertNotNull("Hash should be cached", cachedHash)
        assertEquals("Cached hash should match calculated hash", hash1, cachedHash?.hashValue)

        // Second calculation should use cache
        val hash2 = hashCalculator.calculateHash(filePath, useCache = true)
        assertEquals("Second hash should match first (from cache)", hash1, hash2)
    }

    @Test
    fun `calculateHash should recalculate when file changes`() = runTest {
        // Create initial test file
        val initialData = ByteArray(150 * 1024) { 1 }
        FileOutputStream(testFile).use { it.write(initialData) }
        
        val hash1 = hashCalculator.calculateHash(testFile.absolutePath, useCache = true)
        assertNotNull("Initial hash should not be null", hash1)

        // Wait a bit to ensure different timestamp
        Thread.sleep(10)
        
        // Modify the file
        val modifiedData = ByteArray(150 * 1024) { 2 }
        FileOutputStream(testFile).use { it.write(modifiedData) }

        val hash2 = hashCalculator.calculateHash(testFile.absolutePath, useCache = true)
        assertNotNull("Modified hash should not be null", hash2)
        assertNotEquals("Hash should change when file content changes", hash1, hash2)
    }

    @Test
    fun `calculateHash should handle non-existent file gracefully`() = runTest {
        val nonExistentPath = "/path/that/does/not/exist.mp4"
        val hash = hashCalculator.calculateHash(nonExistentPath, useCache = false)
        assertNull("Hash should be null for non-existent file", hash)
    }

    @Test
    fun `clearCachedHash should remove hash from cache`() = runTest {
        // Create and cache a hash
        val fileSize = 150 * 1024
        val testData = ByteArray(fileSize) { (it % 256).toByte() }
        FileOutputStream(testFile).use { it.write(testData) }
        
        val filePath = testFile.absolutePath
        hashCalculator.calculateHash(filePath, useCache = true)
        
        // Verify hash is cached
        val cachedBefore = fileHashDao.getHashByPath(filePath)
        assertNotNull("Hash should be cached initially", cachedBefore)

        // Clear cache
        hashCalculator.clearCachedHash(filePath)

        // Verify hash is removed from cache
        val cachedAfter = fileHashDao.getHashByPath(filePath)
        assertNull("Hash should be removed from cache", cachedAfter)
    }

    @Test
    fun `getCacheStats should return correct statistics`() = runTest {
        // Initially should have no cached hashes
        val initialStats = hashCalculator.getCacheStats()
        assertEquals("Initial cache should be empty", 0, initialStats.totalEntries)

        // Add some cached hashes
        val fileSize = 150 * 1024
        val testData = ByteArray(fileSize) { (it % 256).toByte() }
        FileOutputStream(testFile).use { it.write(testData) }
        
        hashCalculator.calculateHash(testFile.absolutePath, useCache = true)

        val statsAfter = hashCalculator.getCacheStats()
        assertEquals("Cache should have one entry", 1, statsAfter.totalEntries)
    }

    @Test
    fun `OpenSubtitles hash algorithm should match expected format`() = runTest {
        // Create a file with known content to test the algorithm
        val fileSize = 200 * 1024 // 200KB
        val testData = ByteArray(fileSize)
        
        // Fill first 64KB with pattern
        for (i in 0 until 65536) {
            testData[i] = (i % 256).toByte()
        }
        
        // Fill middle with zeros
        for (i in 65536 until fileSize - 65536) {
            testData[i] = 0
        }
        
        // Fill last 64KB with reverse pattern
        for (i in (fileSize - 65536) until fileSize) {
            testData[i] = ((fileSize - i) % 256).toByte()
        }
        
        FileOutputStream(testFile).use { it.write(testData) }

        val hash = hashCalculator.calculateHash(testFile.absolutePath, useCache = false)
        
        assertNotNull("Hash should not be null", hash)
        assertEquals("Hash should be 16 characters", 16, hash?.length)
        assertTrue("Hash should be valid hex", hash?.matches(Regex("[0-9a-f]+")) == true)
        
        // The hash should incorporate file size (200KB = 204800 bytes)
        // Since we can't predict exact hash without implementing the algorithm here,
        // we just verify it's a valid format and not a default/error value
        assertNotEquals("Hash should not be all zeros", "0000000000000000", hash)
        assertNotEquals("Hash should not be all f's", "ffffffffffffffff", hash)
    }
}