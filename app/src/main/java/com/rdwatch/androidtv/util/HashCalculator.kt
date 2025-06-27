package com.rdwatch.androidtv.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.rdwatch.androidtv.data.dao.FileHashDao
import com.rdwatch.androidtv.data.entities.FileHashEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for calculating file hashes using the OpenSubtitles algorithm.
 * The algorithm computes a hash from:
 * - First 64KB of the file
 * - Last 64KB of the file  
 * - File size
 * 
 * This creates a unique hash that can be used to identify video files
 * for subtitle matching services.
 */
@Singleton
class HashCalculator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileHashDao: FileHashDao
) {
    
    companion object {
        private const val TAG = "HashCalculator"
        private const val HASH_CHUNK_SIZE = 65536L // 64KB
        private const val CACHE_CLEANUP_INTERVAL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }
    
    /**
     * Calculate OpenSubtitles hash for a file with caching support.
     * 
     * @param filePath Path to the video file (can be local file path or URI string)
     * @param useCache Whether to use cached hash if available
     * @return Hash string in hexadecimal format, or null if calculation failed
     */
    suspend fun calculateHash(filePath: String, useCache: Boolean = true): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                val fileExists = file.exists()
                val fileSize = if (fileExists) file.length() else 0L
                val lastModified = if (fileExists) file.lastModified() else 0L
                
                // Check cache first if enabled and file exists
                if (useCache && fileExists) {
                    val cachedHash = fileHashDao.getCachedHash(filePath, fileSize, lastModified)
                    if (cachedHash != null) {
                        Log.d(TAG, "Using cached hash for: $filePath")
                        return@withContext cachedHash.hashValue
                    }
                }
                
                // Calculate hash
                val hash = if (fileExists) {
                    calculateFileHash(file)
                } else {
                    // Try as URI (for content:// or http:// URLs)
                    calculateUriHash(filePath)
                }
                
                // Cache the result if calculation succeeded and file is local
                if (hash != null && fileExists && useCache) {
                    try {
                        val hashEntity = FileHashEntity(
                            filePath = filePath,
                            hashValue = hash,
                            fileSize = fileSize,
                            lastModified = lastModified
                        )
                        fileHashDao.insertOrUpdateHash(hashEntity)
                        Log.d(TAG, "Cached hash for: $filePath")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to cache hash for $filePath", e)
                        // Don't fail the whole operation if caching fails
                    }
                }
                
                hash
            } catch (e: Exception) {
                Log.e(TAG, "Failed to calculate hash for: $filePath", e)
                null
            }
        }
    }
    
    /**
     * Calculate hash for a local file.
     */
    private fun calculateFileHash(file: File): String? {
        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "File does not exist or is not readable: ${file.absolutePath}")
            return null
        }
        
        val fileSize = file.length()
        if (fileSize < HASH_CHUNK_SIZE) {
            Log.w(TAG, "File too small for OpenSubtitles hash: ${file.absolutePath}")
            return null
        }
        
        return try {
            FileInputStream(file).use { fis ->
                calculateHashFromStream(fis, fileSize)
            }
        } catch (e: IOException) {
            Log.e(TAG, "IO error reading file: ${file.absolutePath}", e)
            null
        }
    }
    
    /**
     * Calculate hash for a URI (content:// or http:// URLs).
     */
    private fun calculateUriHash(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null
                
            // For URIs, we can't easily get file size, so we'll read the stream
            // and calculate size as we go. This is less efficient but necessary.
            inputStream.use { stream ->
                calculateHashFromStreamWithUnknownSize(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate hash for URI: $uriString", e)
            null
        }
    }
    
    /**
     * Calculate OpenSubtitles hash from an input stream with known file size.
     */
    private fun calculateHashFromStream(stream: InputStream, fileSize: Long): String? {
        if (fileSize < HASH_CHUNK_SIZE) {
            return null
        }
        
        try {
            var hash = fileSize
            
            // Read first 64KB
            val firstChunk = ByteArray(HASH_CHUNK_SIZE.toInt())
            val firstRead = stream.read(firstChunk)
            if (firstRead != HASH_CHUNK_SIZE.toInt()) {
                Log.w(TAG, "Could not read first chunk completely")
                return null
            }
            
            // Add first chunk to hash
            hash += bytesToLong(firstChunk)
            
            // Skip to last 64KB
            val skipBytes = fileSize - (2 * HASH_CHUNK_SIZE)
            if (skipBytes > 0) {
                var totalSkipped = 0L
                while (totalSkipped < skipBytes) {
                    val skipped = stream.skip(skipBytes - totalSkipped)
                    if (skipped <= 0) break
                    totalSkipped += skipped
                }
            }
            
            // Read last 64KB
            val lastChunk = ByteArray(HASH_CHUNK_SIZE.toInt())
            val lastRead = stream.read(lastChunk)
            if (lastRead != HASH_CHUNK_SIZE.toInt()) {
                Log.w(TAG, "Could not read last chunk completely")
                return null
            }
            
            // Add last chunk to hash
            hash += bytesToLong(lastChunk)
            
            return String.format("%016x", hash)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating hash from stream", e)
            return null
        }
    }
    
    /**
     * Calculate hash from stream when file size is unknown (for URIs).
     * This reads the entire stream to determine size and chunks.
     */
    private fun calculateHashFromStreamWithUnknownSize(stream: InputStream): String? {
        return try {
            // Read entire stream into memory (not ideal for large files)
            val allBytes = stream.readBytes()
            val fileSize = allBytes.size.toLong()
            
            if (fileSize < HASH_CHUNK_SIZE) {
                Log.w(TAG, "Stream too small for OpenSubtitles hash")
                return null
            }
            
            var hash = fileSize
            
            // Get first 64KB
            val firstChunk = allBytes.sliceArray(0 until HASH_CHUNK_SIZE.toInt())
            hash += bytesToLong(firstChunk)
            
            // Get last 64KB
            val lastChunkStart = (fileSize - HASH_CHUNK_SIZE).toInt()
            val lastChunk = allBytes.sliceArray(lastChunkStart until allBytes.size)
            hash += bytesToLong(lastChunk)
            
            String.format("%016x", hash)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating hash from unknown size stream", e)
            null
        }
    }
    
    /**
     * Convert byte array to long value for hash calculation.
     * Uses little-endian byte order as per OpenSubtitles specification.
     */
    private fun bytesToLong(bytes: ByteArray): Long {
        var hash = 0L
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        
        // Process 8 bytes at a time
        while (buffer.remaining() >= 8) {
            hash += buffer.long
        }
        
        // Handle remaining bytes
        if (buffer.remaining() > 0) {
            val remaining = ByteArray(8)
            buffer.get(remaining, 0, buffer.remaining())
            val remainingBuffer = ByteBuffer.wrap(remaining).order(ByteOrder.LITTLE_ENDIAN)
            hash += remainingBuffer.long
        }
        
        return hash
    }
    
    /**
     * Clear cached hash for a specific file.
     */
    suspend fun clearCachedHash(filePath: String) {
        withContext(Dispatchers.IO) {
            try {
                fileHashDao.deleteHashByPath(filePath)
                Log.d(TAG, "Cleared cached hash for: $filePath")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear cached hash for: $filePath", e)
            }
        }
    }
    
    /**
     * Clear all cached hashes.
     */
    suspend fun clearAllCachedHashes() {
        withContext(Dispatchers.IO) {
            try {
                fileHashDao.deleteAllHashes()
                Log.d(TAG, "Cleared all cached hashes")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear all cached hashes", e)
            }
        }
    }
    
    /**
     * Clean up old cache entries.
     */
    suspend fun cleanupOldCacheEntries() {
        withContext(Dispatchers.IO) {
            try {
                val cutoffTime = System.currentTimeMillis() - CACHE_CLEANUP_INTERVAL_MS
                fileHashDao.deleteOldEntries(cutoffTime)
                Log.d(TAG, "Cleaned up old hash cache entries")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cleanup old cache entries", e)
            }
        }
    }
    
    /**
     * Get cache statistics for monitoring.
     */
    suspend fun getCacheStats(): HashCacheStats {
        return withContext(Dispatchers.IO) {
            try {
                val count = fileHashDao.getHashCount()
                HashCacheStats(totalEntries = count)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get cache stats", e)
                HashCacheStats()
            }
        }
    }
}

/**
 * Statistics about the hash cache.
 */
data class HashCacheStats(
    val totalEntries: Int = 0
)