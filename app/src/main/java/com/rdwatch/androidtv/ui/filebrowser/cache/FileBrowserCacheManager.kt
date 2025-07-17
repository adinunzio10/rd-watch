package com.rdwatch.androidtv.ui.filebrowser.cache

import com.rdwatch.androidtv.ui.filebrowser.models.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized cache manager for file browser operations
 * Handles hierarchical caching with different TTL for different types of content
 */
@Singleton
class FileBrowserCacheManager
    @Inject
    constructor() {
        // Cache TTL configurations
        private val rootContentTTL = TimeUnit.MINUTES.toMillis(5) // 5 minutes
        private val torrentFilesTTL = TimeUnit.MINUTES.toMillis(10) // 10 minutes (more stable)
        private val playbackUrlTTL = TimeUnit.MINUTES.toMillis(30) // 30 minutes (URLs are stable)
        private val metadataTTL = TimeUnit.HOURS.toMillis(24) // 24 hours (metadata rarely changes)

        // Cache storage
        private val _rootContent = MutableStateFlow<CachedData<List<FileItem>>?>(null)
        val rootContent: StateFlow<CachedData<List<FileItem>>?> = _rootContent.asStateFlow()

        private val torrentFilesCache = ConcurrentHashMap<String, CachedData<List<FileItem.File>>>()
        private val playbackUrlCache = ConcurrentHashMap<String, CachedData<String>>()
        private val metadataCache = ConcurrentHashMap<String, CachedData<FileItem>>()

        // Search and filter caches
        private val searchResultsCache = ConcurrentHashMap<String, CachedData<List<FileItem>>>()
        private val filterResultsCache = ConcurrentHashMap<String, CachedData<List<FileItem>>>()

        // Cache management
        private val cacheMutex = Mutex()

        /**
         * Cache root content with proper TTL
         */
        suspend fun cacheRootContent(content: List<FileItem>) {
            cacheMutex.withLock {
                _rootContent.value = CachedData(content, System.currentTimeMillis())
            }
        }

        /**
         * Get cached root content if still valid
         */
        fun getCachedRootContent(): List<FileItem>? {
            val cached = _rootContent.value
            return if (cached != null && cached.isValid(rootContentTTL)) {
                cached.data
            } else {
                null
            }
        }

        /**
         * Cache torrent files with extended TTL
         */
        suspend fun cacheTorrentFiles(
            torrentId: String,
            files: List<FileItem.File>,
        ) {
            cacheMutex.withLock {
                torrentFilesCache[torrentId] = CachedData(files, System.currentTimeMillis())
            }
        }

        /**
         * Get cached torrent files if still valid
         */
        fun getCachedTorrentFiles(torrentId: String): List<FileItem.File>? {
            val cached = torrentFilesCache[torrentId]
            return if (cached != null && cached.isValid(torrentFilesTTL)) {
                cached.data
            } else {
                null
            }
        }

        /**
         * Cache playback URL with extended TTL
         */
        suspend fun cachePlaybackUrl(
            fileId: String,
            url: String,
        ) {
            cacheMutex.withLock {
                playbackUrlCache[fileId] = CachedData(url, System.currentTimeMillis())
            }
        }

        /**
         * Get cached playback URL if still valid
         */
        fun getCachedPlaybackUrl(fileId: String): String? {
            val cached = playbackUrlCache[fileId]
            return if (cached != null && cached.isValid(playbackUrlTTL)) {
                cached.data
            } else {
                null
            }
        }

        /**
         * Cache file metadata with long TTL
         */
        suspend fun cacheMetadata(
            itemId: String,
            metadata: FileItem,
        ) {
            cacheMutex.withLock {
                metadataCache[itemId] = CachedData(metadata, System.currentTimeMillis())
            }
        }

        /**
         * Get cached metadata if still valid
         */
        fun getCachedMetadata(itemId: String): FileItem? {
            val cached = metadataCache[itemId]
            return if (cached != null && cached.isValid(metadataTTL)) {
                cached.data
            } else {
                null
            }
        }

        /**
         * Cache search results with short TTL
         */
        suspend fun cacheSearchResults(
            query: String,
            results: List<FileItem>,
        ) {
            cacheMutex.withLock {
                val cacheKey = "search_$query"
                searchResultsCache[cacheKey] = CachedData(results, System.currentTimeMillis())
            }
        }

        /**
         * Get cached search results if still valid
         */
        fun getCachedSearchResults(query: String): List<FileItem>? {
            val cacheKey = "search_$query"
            val cached = searchResultsCache[cacheKey]
            return if (cached != null && cached.isValid(rootContentTTL)) {
                cached.data
            } else {
                null
            }
        }

        /**
         * Cache filter results with short TTL
         */
        suspend fun cacheFilterResults(
            filterKey: String,
            results: List<FileItem>,
        ) {
            cacheMutex.withLock {
                filterResultsCache[filterKey] = CachedData(results, System.currentTimeMillis())
            }
        }

        /**
         * Get cached filter results if still valid
         */
        fun getCachedFilterResults(filterKey: String): List<FileItem>? {
            val cached = filterResultsCache[filterKey]
            return if (cached != null && cached.isValid(rootContentTTL)) {
                cached.data
            } else {
                null
            }
        }

        /**
         * Invalidate all caches
         */
        suspend fun invalidateAll() {
            cacheMutex.withLock {
                _rootContent.value = null
                torrentFilesCache.clear()
                playbackUrlCache.clear()
                searchResultsCache.clear()
                filterResultsCache.clear()
                // Keep metadata cache as it has longer TTL
            }
        }

        /**
         * Invalidate specific torrent cache
         */
        suspend fun invalidateTorrent(torrentId: String) {
            cacheMutex.withLock {
                torrentFilesCache.remove(torrentId)
                // Remove related playback URLs
                playbackUrlCache.keys.removeAll { it.startsWith("${torrentId}_") }
                // Invalidate root content to reflect changes
                _rootContent.value = null
            }
        }

        /**
         * Invalidate search and filter caches
         */
        suspend fun invalidateSearchAndFilter() {
            cacheMutex.withLock {
                searchResultsCache.clear()
                filterResultsCache.clear()
            }
        }

        /**
         * Get cache statistics
         */
        fun getCacheStats(): CacheStats {
            return CacheStats(
                rootContentCached = _rootContent.value != null,
                torrentFilesCacheSize = torrentFilesCache.size,
                playbackUrlCacheSize = playbackUrlCache.size,
                metadataCacheSize = metadataCache.size,
                searchResultsCacheSize = searchResultsCache.size,
                filterResultsCacheSize = filterResultsCache.size,
            )
        }

        /**
         * Clean expired cache entries
         */
        suspend fun cleanExpiredEntries() {
            cacheMutex.withLock {
                val currentTime = System.currentTimeMillis()

                // Clean root content if expired
                _rootContent.value?.let { cached ->
                    if (!cached.isValid(rootContentTTL)) {
                        _rootContent.value = null
                    }
                }

                // Clean torrent files cache
                torrentFilesCache.entries.removeAll { (_, cached) ->
                    !cached.isValid(torrentFilesTTL)
                }

                // Clean playback URL cache
                playbackUrlCache.entries.removeAll { (_, cached) ->
                    !cached.isValid(playbackUrlTTL)
                }

                // Clean metadata cache
                metadataCache.entries.removeAll { (_, cached) ->
                    !cached.isValid(metadataTTL)
                }

                // Clean search results cache
                searchResultsCache.entries.removeAll { (_, cached) ->
                    !cached.isValid(rootContentTTL)
                }

                // Clean filter results cache
                filterResultsCache.entries.removeAll { (_, cached) ->
                    !cached.isValid(rootContentTTL)
                }
            }
        }

        /**
         * Generate cache key for filter operations
         */
        fun generateFilterCacheKey(
            sortBy: String,
            sortOrder: String,
            fileTypes: Set<String>,
            statusFilter: Set<String>,
            searchQuery: String,
        ): String {
            val parts =
                listOf(
                    "sort_${sortBy}_$sortOrder",
                    "types_${fileTypes.sorted().joinToString(",")}",
                    "status_${statusFilter.sorted().joinToString(",")}",
                    "search_$searchQuery",
                )
            return parts.joinToString("|")
        }
    }

/**
 * Data class for cached values with validation
 */
data class CachedData<T>(
    val data: T,
    val timestamp: Long,
) {
    fun isValid(ttlMs: Long): Boolean {
        return System.currentTimeMillis() - timestamp < ttlMs
    }

    fun isExpired(ttlMs: Long): Boolean = !isValid(ttlMs)

    fun age(): Long = System.currentTimeMillis() - timestamp
}

/**
 * Cache statistics data class
 */
data class CacheStats(
    val rootContentCached: Boolean,
    val torrentFilesCacheSize: Int,
    val playbackUrlCacheSize: Int,
    val metadataCacheSize: Int,
    val searchResultsCacheSize: Int,
    val filterResultsCacheSize: Int,
) {
    val totalCacheSize: Int
        get() =
            torrentFilesCacheSize + playbackUrlCacheSize + metadataCacheSize +
                searchResultsCacheSize + filterResultsCacheSize
}
