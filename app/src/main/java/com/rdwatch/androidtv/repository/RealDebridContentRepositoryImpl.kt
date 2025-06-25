package com.rdwatch.androidtv.repository

import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.data.entities.ContentEntity
import com.rdwatch.androidtv.data.mappers.RealDebridMappers.toContentEntity
import com.rdwatch.androidtv.data.mappers.RealDebridMappers.toContentEntityFromDownload
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.repository.base.safeCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of RealDebridContentRepository
 */
@Singleton
class RealDebridContentRepositoryImpl @Inject constructor(
    private val apiService: RealDebridApiService,
    private val dispatcherProvider: DispatcherProvider
) : RealDebridContentRepository {
    
    // Performance optimizations: Memory cache with expiration
    private val torrentsCache = MutableStateFlow<CachedData<List<ContentEntity>>>(CachedData.empty())
    private val downloadsCache = MutableStateFlow<CachedData<List<ContentEntity>>>(CachedData.empty())
    
    // Individual content cache for fast lookups
    private val individualContentCache = ConcurrentHashMap<String, CachedData<ContentEntity>>()
    
    // Sync mutex to prevent concurrent API calls
    private val syncMutex = Mutex()
    
    // Cache duration in milliseconds (5 minutes)
    private val cacheExpirationMs = TimeUnit.MINUTES.toMillis(5)
    
    override fun getTorrents(): Flow<Result<List<ContentEntity>>> = flow {
        emit(Result.Loading)
        
        val cachedTorrents = torrentsCache.value
        
        // Emit cached data first if available and not expired
        if (cachedTorrents.isValid(cacheExpirationMs)) {
            emit(Result.Success(cachedTorrents.data))
        }
        
        // Skip API call if cache is still fresh
        if (cachedTorrents.isValid(cacheExpirationMs)) {
            return@flow
        }
        
        // Fetch fresh data from API with sync protection
        val result = syncMutex.withLock {
            // Double-check cache after acquiring lock
            val recentCache = torrentsCache.value
            if (recentCache.isValid(cacheExpirationMs)) {
                return@withLock Result.Success(recentCache.data)
            }
            
            withContext(dispatcherProvider.io) {
                safeCall {
                    val response = apiService.getTorrents(limit = 100)
                    if (response.isSuccessful) {
                        response.body()?.map { it.toContentEntity() } ?: emptyList()
                    } else {
                        throw Exception("Failed to fetch torrents: ${response.code()}")
                    }
                }
            }
        }
        
        // Update cache and emit result
        when (result) {
            is Result.Success -> {
                torrentsCache.value = CachedData(result.data, System.currentTimeMillis())
                emit(result)
            }
            is Result.Error -> emit(result)
            is Result.Loading -> {} // Already emitted
        }
    }.flowOn(dispatcherProvider.io)
    .distinctUntilChanged() // Prevent duplicate emissions
    
    override fun getDownloads(): Flow<Result<List<ContentEntity>>> = flow {
        emit(Result.Loading)
        
        val cachedDownloads = downloadsCache.value
        
        // Emit cached data first if available and not expired
        if (cachedDownloads.isValid(cacheExpirationMs)) {
            emit(Result.Success(cachedDownloads.data))
        }
        
        // Skip API call if cache is still fresh
        if (cachedDownloads.isValid(cacheExpirationMs)) {
            return@flow
        }
        
        // Fetch fresh data from API with sync protection
        val result = syncMutex.withLock {
            // Double-check cache after acquiring lock
            val recentCache = downloadsCache.value
            if (recentCache.isValid(cacheExpirationMs)) {
                return@withLock Result.Success(recentCache.data)
            }
            
            withContext(dispatcherProvider.io) {
                safeCall {
                    val response = apiService.getDownloads(limit = 100)
                    if (response.isSuccessful) {
                        response.body()?.map { it.toContentEntityFromDownload() } ?: emptyList()
                    } else {
                        throw Exception("Failed to fetch downloads: ${response.code()}")
                    }
                }
            }
        }
        
        // Update cache and emit result
        when (result) {
            is Result.Success -> {
                downloadsCache.value = CachedData(result.data, System.currentTimeMillis())
                emit(result)
            }
            is Result.Error -> emit(result)
            is Result.Loading -> {} // Already emitted
        }
    }.flowOn(dispatcherProvider.io)
    .distinctUntilChanged() // Prevent duplicate emissions
    
    override suspend fun getTorrentInfo(id: String): Result<ContentEntity?> = withContext(dispatcherProvider.io) {
        // Check individual cache first
        val cachedContent = individualContentCache[id]
        if (cachedContent != null && cachedContent.isValid(cacheExpirationMs)) {
            return@withContext Result.Success(cachedContent.data)
        }
        
        val result = safeCall {
            val response = apiService.getTorrentInfo(id)
            if (response.isSuccessful) {
                response.body()?.toContentEntity()
            } else {
                throw Exception("Failed to fetch torrent info: ${response.code()}")
            }
        }
        
        // Cache the result
        when (result) {
            is Result.Success -> {
                result.data?.let { content ->
                    individualContentCache[id] = CachedData(content, System.currentTimeMillis())
                }
            }
            else -> {} // Don't cache errors
        }
        
        result
    }
    
    override suspend fun unrestrictLink(link: String): Result<String> = withContext(dispatcherProvider.io) {
        safeCall {
            val response = apiService.unrestrictLink(link)
            if (response.isSuccessful) {
                response.body()?.download ?: throw Exception("No download link in response")
            } else {
                throw Exception("Failed to unrestrict link: ${response.code()}")
            }
        }
    }
    
    override suspend fun syncContent(): Result<Unit> = withContext(dispatcherProvider.io) {
        syncMutex.withLock {
            safeCall {
                val currentTime = System.currentTimeMillis()
                
                // Fetch torrents
                val torrentsResponse = apiService.getTorrents(limit = 100)
                if (torrentsResponse.isSuccessful) {
                    val torrents = torrentsResponse.body()?.map { it.toContentEntity() } ?: emptyList()
                    torrentsCache.value = CachedData(torrents, currentTime)
                }
                
                // Fetch downloads
                val downloadsResponse = apiService.getDownloads(limit = 100)
                if (downloadsResponse.isSuccessful) {
                    val downloads = downloadsResponse.body()?.map { it.toContentEntityFromDownload() } ?: emptyList()
                    downloadsCache.value = CachedData(downloads, currentTime)
                }
                
                // Clear individual cache to prevent stale data
                individualContentCache.clear()
                
                Unit
            }
        }
    }
    
    override fun getAllContent(): Flow<Result<List<ContentEntity>>> = combine(
        getTorrents(),
        getDownloads()
    ) { torrentsResult, downloadsResult ->
        when {
            torrentsResult is Result.Loading || downloadsResult is Result.Loading -> Result.Loading
            torrentsResult is Result.Error -> torrentsResult
            downloadsResult is Result.Error -> downloadsResult
            torrentsResult is Result.Success && downloadsResult is Result.Success -> {
                Result.Success(torrentsResult.data + downloadsResult.data)
            }
            else -> Result.Error(Exception("Unexpected state"))
        }
    }.flowOn(dispatcherProvider.io)
    .distinctUntilChanged() // Prevent duplicate emissions
    
    override fun searchContent(query: String): Flow<Result<List<ContentEntity>>> = getAllContent()
        .map { result ->
            when (result) {
                is Result.Success -> {
                    val filteredContent = result.data.filter { content ->
                        content.title.contains(query, ignoreCase = true) ||
                        content.description?.contains(query, ignoreCase = true) == true
                    }
                    Result.Success(filteredContent)
                }
                is Result.Error -> result
                is Result.Loading -> result
            }
        }
        .flowOn(dispatcherProvider.io)
        .distinctUntilChanged() // Prevent duplicate emissions
    
    override suspend fun deleteTorrent(id: String): Result<Unit> = withContext(dispatcherProvider.io) {
        val result = safeCall {
            val response = apiService.deleteTorrent(id)
            if (response.isSuccessful) {
                Unit
            } else {
                throw Exception("Failed to delete torrent: ${response.code()}")
            }
        }
        
        // Invalidate cache on successful deletion
        if (result is Result.Success) {
            // Clear caches to force refresh
            torrentsCache.value = CachedData.empty()
            individualContentCache.remove(id)
        }
        
        result
    }
    
    override suspend fun deleteDownload(id: String): Result<Unit> = withContext(dispatcherProvider.io) {
        val result = safeCall {
            val response = apiService.deleteDownload(id)
            if (response.isSuccessful) {
                Unit
            } else {
                throw Exception("Failed to delete download: ${response.code()}")
            }
        }
        
        // Invalidate cache on successful deletion
        if (result is Result.Success) {
            // Clear caches to force refresh
            downloadsCache.value = CachedData.empty()
            individualContentCache.remove(id)
        }
        
        result
    }
}

/**
 * Data class for cached values with expiration
 */
private data class CachedData<T>(
    val data: T,
    val timestamp: Long
) {
    companion object {
        fun <T> empty(): CachedData<T> where T : Collection<*> {
            @Suppress("UNCHECKED_CAST")
            return CachedData(emptyList<Any>() as T, 0L)
        }
    }
    
    fun isValid(expirationMs: Long): Boolean {
        return System.currentTimeMillis() - timestamp < expirationMs
    }
}