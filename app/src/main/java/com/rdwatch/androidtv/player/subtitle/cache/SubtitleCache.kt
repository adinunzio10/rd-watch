package com.rdwatch.androidtv.player.subtitle.cache

import android.content.Context
import com.rdwatch.androidtv.data.dao.SubtitleDao
import com.rdwatch.androidtv.data.entities.SubtitleCacheEntity
import com.rdwatch.androidtv.data.entities.SubtitleFileEntity
import com.rdwatch.androidtv.data.entities.SubtitleResultEntity
import com.rdwatch.androidtv.player.subtitle.models.SubtitleFileInfo
import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest
import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages caching of subtitle search results and downloaded files.
 * Implements intelligent cache management with expiration and cleanup strategies.
 *
 * This cache system is crucial for:
 * - Reducing API calls to external subtitle providers
 * - Improving app performance and responsiveness
 * - Managing local storage efficiently
 * - Providing offline access to previously downloaded subtitles
 */
@Singleton
class SubtitleCache
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val subtitleDao: SubtitleDao,
    ) {
        private val cacheDir = File(context.cacheDir, "subtitles")
        private val subtitleFilesDir = File(context.filesDir, "subtitles")

        companion object {
            private const val DEFAULT_CACHE_EXPIRATION_HOURS = 24
            private const val MAX_CACHE_SIZE_MB = 100
            private const val MAX_FILES_PER_CONTENT = 5
        }

        init {
            // Ensure cache directories exist
            cacheDir.mkdirs()
            subtitleFilesDir.mkdirs()
        }

        /**
         * Get cached search results for a request.
         *
         * @param request The subtitle search request
         * @return Cached results if available and not expired
         */
        suspend fun getCachedResults(request: SubtitleSearchRequest): List<SubtitleSearchResult> =
            withContext(Dispatchers.IO) {
                val searchKey = request.getCacheKey()
                val cachedData = subtitleDao.getCachedSearch(searchKey)

                return@withContext cachedData?.results?.map { entity ->
                    entity.toSubtitleSearchResult()
                } ?: emptyList()
            }

        /**
         * Cache search results for future use.
         *
         * @param request The original search request
         * @param results The results to cache
         */
        suspend fun cacheResults(
            request: SubtitleSearchRequest,
            results: List<SubtitleSearchResult>,
        ) = withContext(Dispatchers.IO) {
            if (results.isEmpty()) return@withContext

            val searchKey = request.getCacheKey()
            val expirationTime = System.currentTimeMillis() + (DEFAULT_CACHE_EXPIRATION_HOURS * 60 * 60 * 1000)

            // Create cache entry
            val cacheEntity =
                SubtitleCacheEntity(
                    searchKey = searchKey,
                    contentId = request.imdbId ?: request.tmdbId ?: request.fileHash ?: request.title,
                    contentTitle = request.title,
                    contentYear = request.year,
                    season = request.season,
                    episode = request.episode,
                    expiresAt = expirationTime,
                    resultCount = results.size,
                    languages = request.languages.joinToString(","),
                    hasFileHash = request.fileHash != null,
                    hasImdbId = request.imdbId != null,
                    hasTmdbId = request.tmdbId != null,
                )

            val cacheId = subtitleDao.insertCacheEntry(cacheEntity)

            // Create result entities
            val resultEntities =
                results.map { result ->
                    result.toSubtitleResultEntity(cacheId)
                }

            subtitleDao.insertResults(resultEntities)
        }

        /**
         * Get cached subtitle file if available.
         *
         * @param result The subtitle result to check for cached file
         * @return File path if cached, null otherwise
         */
        suspend fun getCachedFile(result: SubtitleSearchResult): String? =
            withContext(Dispatchers.IO) {
                val contentId = result.getCacheId()
                val cachedFile = subtitleDao.getCachedFile(contentId, result.language)

                return@withContext if (cachedFile != null && File(cachedFile.filePath).exists()) {
                    // Update access time
                    subtitleDao.updateFileAccess(cachedFile.updateAccess())
                    cachedFile.filePath
                } else {
                    // Clean up invalid cache entry
                    cachedFile?.let { subtitleDao.deactivateFile(it.id) }
                    null
                }
            }

        /**
         * Cache a downloaded subtitle file.
         *
         * @param result The subtitle result this file belongs to
         * @param originalFilePath The path to the downloaded file
         * @return The final cached file path
         */
        suspend fun cacheFile(
            result: SubtitleSearchResult,
            originalFilePath: String,
        ): String =
            withContext(Dispatchers.IO) {
                val originalFile = File(originalFilePath)
                if (!originalFile.exists()) {
                    throw IllegalArgumentException("File does not exist: $originalFilePath")
                }

                // Generate a unique filename for the cached file
                val contentId = result.getCacheId()
                val filename = generateCachedFileName(contentId, result.language, result.format.extension)
                val cachedFile = File(subtitleFilesDir, filename)

                // Copy file to cache location
                originalFile.copyTo(cachedFile, overwrite = true)

                // Calculate checksum for integrity verification
                val checksum = calculateFileChecksum(cachedFile)

                // Create database entry
                val fileEntity =
                    SubtitleFileEntity(
                        resultId = 0, // Will be updated when we have proper foreign key setup
                        filePath = cachedFile.absolutePath,
                        originalFileName = result.fileName,
                        contentId = contentId,
                        language = result.language,
                        format = result.format,
                        fileSize = cachedFile.length(),
                        provider = result.provider,
                        downloadUrl = result.downloadUrl,
                        checksum = checksum,
                    )

                subtitleDao.insertSubtitleFile(fileEntity)

                // Clean up old files if we have too many for this content
                cleanupOldFilesForContent(contentId)

                return@withContext cachedFile.absolutePath
            }

        /**
         * Get all cached subtitle files for specific content.
         *
         * @param contentId The content identifier
         * @return List of cached subtitle file information
         */
        suspend fun getAllCachedFiles(contentId: String): List<SubtitleFileInfo> =
            withContext(Dispatchers.IO) {
                val cachedFiles = subtitleDao.getAllCachedFiles(contentId)
                return@withContext cachedFiles.mapNotNull { entity ->
                    if (File(entity.filePath).exists()) {
                        SubtitleFileInfo(
                            filePath = entity.filePath,
                            format = entity.format,
                            language = entity.language,
                            fileSize = entity.fileSize,
                            isEmbedded = false,
                            isExternal = true,
                            cacheTimestamp = entity.downloadTimestamp,
                            source = entity.provider,
                        )
                    } else {
                        // Clean up invalid cache entries
                        subtitleDao.deactivateFile(entity.id)
                        null
                    }
                }
            }

        /**
         * Clear all cached data.
         * Used for manual cache management or when storage is low.
         */
        suspend fun clearAll() =
            withContext(Dispatchers.IO) {
                // Clear database entries
                subtitleDao.clearAllCache()

                // Delete cached files
                subtitleFilesDir.listFiles()?.forEach { it.delete() }
                cacheDir.listFiles()?.forEach { it.delete() }
            }

        /**
         * Perform routine cache maintenance.
         * Should be called periodically to clean up expired entries and old files.
         */
        suspend fun performMaintenance() =
            withContext(Dispatchers.IO) {
                // Clean up expired cache entries
                subtitleDao.cleanupExpiredCache()

                // Clean up old files based on age and usage
                val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 days
                val lastAccessCutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 1 day

                val filesToCleanup = subtitleDao.getFilesForCleanup(cutoffTime, lastAccessCutoff)
                filesToCleanup.forEach { fileEntity ->
                    val file = File(fileEntity.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                    subtitleDao.deactivateFile(fileEntity.id)
                }

                // Clean up database entries for files that no longer exist
                subtitleDao.cleanupOldFiles(cutoffTime)

                // Check total cache size and clean up if necessary
                manageCacheSize()
            }

        /**
         * Get cache statistics for monitoring and debugging.
         */
        suspend fun getCacheStatistics(): CacheStatistics =
            withContext(Dispatchers.IO) {
                val dbStats = subtitleDao.getCacheStatistics()
                val fileSizeStats = calculateFileSizeStatistics()

                return@withContext CacheStatistics(
                    totalCacheEntries = dbStats.totalEntries,
                    validCacheEntries = dbStats.validEntries,
                    expiredCacheEntries = dbStats.expiredEntries,
                    averageResultsPerSearch = dbStats.avgResultsPerSearch,
                    totalCachedFiles = fileSizeStats.fileCount,
                    totalCacheSize = fileSizeStats.totalSize,
                    maxCacheSize = MAX_CACHE_SIZE_MB * 1024 * 1024L,
                )
            }

        private suspend fun cleanupOldFilesForContent(contentId: String) {
            val files = subtitleDao.getAllCachedFiles(contentId)
            if (files.size > MAX_FILES_PER_CONTENT) {
                // Keep the most recently accessed files
                val filesToRemove =
                    files
                        .sortedBy { it.lastAccessTime }
                        .take(files.size - MAX_FILES_PER_CONTENT)

                filesToRemove.forEach { fileEntity ->
                    File(fileEntity.filePath).delete()
                    subtitleDao.deactivateFile(fileEntity.id)
                }
            }
        }

        private suspend fun manageCacheSize() {
            val maxSizeBytes = MAX_CACHE_SIZE_MB * 1024 * 1024L
            val currentSize = calculateCurrentCacheSize()

            if (currentSize > maxSizeBytes) {
                // Remove least recently used files until we're under the limit
                val cutoffTime = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L) // 3 days
                subtitleDao.cleanupOldFiles(cutoffTime, 0)
            }
        }

        private fun calculateCurrentCacheSize(): Long {
            return subtitleFilesDir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        }

        private fun calculateFileSizeStatistics(): FileSizeStatistics {
            val files = subtitleFilesDir.listFiles() ?: emptyArray()
            return FileSizeStatistics(
                fileCount = files.size,
                totalSize = files.sumOf { it.length() },
            )
        }

        private fun generateCachedFileName(
            contentId: String,
            language: String,
            extension: String,
        ): String {
            val hash = contentId.hashCode().toString().take(8)
            return "${hash}_$language.$extension"
        }

        private fun calculateFileChecksum(file: File): String {
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytes = input.read(buffer)
                while (bytes != -1) {
                    digest.update(buffer, 0, bytes)
                    bytes = input.read(buffer)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        private data class FileSizeStatistics(
            val fileCount: Int,
            val totalSize: Long,
        )
    }

/**
 * Cache statistics for monitoring.
 */
data class CacheStatistics(
    val totalCacheEntries: Int,
    val validCacheEntries: Int,
    val expiredCacheEntries: Int,
    val averageResultsPerSearch: Double,
    val totalCachedFiles: Int,
    val totalCacheSize: Long,
    val maxCacheSize: Long,
) {
    val cacheUsagePercent: Float
        get() =
            if (maxCacheSize > 0) {
                (totalCacheSize.toFloat() / maxCacheSize.toFloat()) * 100f
            } else {
                0f
            }
}

// Extension functions for entity conversion
private fun SubtitleResultEntity.toSubtitleSearchResult(): SubtitleSearchResult {
    return SubtitleSearchResult(
        id = providerId,
        provider = provider,
        language = language,
        languageName = languageName,
        format = format,
        downloadUrl = downloadUrl,
        fileName = fileName,
        fileSize = fileSize,
        downloadCount = downloadCount,
        rating = rating,
        uploadDate = uploadDate,
        uploader = uploader,
        matchScore = matchScore,
        matchType = matchType,
        contentHash = contentHash,
        isVerified = isVerified,
        hearingImpaired = hearingImpaired,
        releaseGroup = releaseGroup,
        version = version,
        comments = comments,
    )
}

private fun SubtitleSearchResult.toSubtitleResultEntity(cacheId: Long): SubtitleResultEntity {
    return SubtitleResultEntity(
        cacheId = cacheId,
        providerId = id,
        provider = provider,
        downloadUrl = downloadUrl,
        language = language,
        languageName = languageName,
        format = format,
        fileName = fileName,
        fileSize = fileSize,
        downloadCount = downloadCount,
        rating = rating,
        matchScore = matchScore,
        matchType = matchType,
        uploadDate = uploadDate,
        uploader = uploader,
        isVerified = isVerified,
        hearingImpaired = hearingImpaired,
        releaseGroup = releaseGroup,
        version = version,
        comments = comments,
        contentHash = contentHash,
    )
}
