package com.rdwatch.androidtv.player.subtitle

import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.core.error.ErrorHandler
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiClient
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import com.rdwatch.androidtv.player.subtitle.api.RateLimitStatus
import com.rdwatch.androidtv.player.subtitle.cache.SubtitleCache
import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest
import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult
import com.rdwatch.androidtv.player.subtitle.ranking.SubtitleResultRanker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central orchestrator for external subtitle integration.
 * Coordinates multiple subtitle API providers, manages caching, and handles rate limiting.
 * 
 * This class serves as the main coordination point for all subtitle-related operations,
 * ensuring consistent API usage patterns and error handling across the application.
 */
@Singleton
class SubtitleApiOrchestrator @Inject constructor(
    private val apiClients: Set<@JvmSuppressWildcards SubtitleApiClient>,
    private val subtitleCache: SubtitleCache,
    private val resultRanker: SubtitleResultRanker,
    private val rateLimiter: SubtitleRateLimiter,
    private val dispatcherProvider: DispatcherProvider,
    private val errorHandler: ErrorHandler
) {
    
    /**
     * Search for subtitles across all configured APIs.
     * Implements caching, rate limiting, and result aggregation.
     * 
     * @param request The search request containing content metadata
     * @return Flow of search results as they become available
     */
    fun searchSubtitles(request: SubtitleSearchRequest): Flow<List<SubtitleSearchResult>> = flow {
        // 1. Check cache first
        val cachedResults = subtitleCache.getCachedResults(request)
        if (cachedResults.isNotEmpty()) {
            emit(cachedResults)
            return@flow
        }
        
        // 2. Coordinate parallel API calls with rate limiting
        val results = coroutineScope {
            apiClients
                .filter { it.isEnabled() && rateLimiter.canMakeRequest(it.getProvider()) }
                .map { client ->
                    async {
                        try {
                            rateLimiter.recordRequest(client.getProvider())
                            client.searchSubtitles(request)
                        } catch (e: Exception) {
                            errorHandler.handleError(e)
                            emptyList<SubtitleSearchResult>()
                        }
                    }
                }
                .awaitAll()
                .flatten()
        }
        
        // 3. Rank and sort results
        val rankedResults = resultRanker.rankResults(results, request)
        
        // 4. Cache results for future requests
        subtitleCache.cacheResults(request, rankedResults)
        
        emit(rankedResults)
    }.flowOn(dispatcherProvider.io)
    
    /**
     * Download a specific subtitle file.
     * Handles caching and provides progress updates.
     * 
     * @param result The subtitle result to download
     * @return Flow of download progress and final file path
     */
    fun downloadSubtitle(result: SubtitleSearchResult): Flow<SubtitleDownloadState> = flow {
        emit(SubtitleDownloadState.Loading)
        
        try {
            // Check if already cached
            val cachedFile = subtitleCache.getCachedFile(result)
            if (cachedFile != null) {
                emit(SubtitleDownloadState.Success(cachedFile))
                return@flow
            }
            
            // Find appropriate client for this result
            val client = apiClients.find { it.getProvider() == result.provider }
                ?: throw IllegalStateException("No client found for provider: ${result.provider}")
            
            // Download with rate limiting
            if (!rateLimiter.canMakeRequest(result.provider)) {
                emit(SubtitleDownloadState.RateLimited)
                return@flow
            }
            
            rateLimiter.recordRequest(result.provider)
            val filePath = client.downloadSubtitle(result)
            
            // Cache the downloaded file
            subtitleCache.cacheFile(result, filePath)
            
            emit(SubtitleDownloadState.Success(filePath))
            
        } catch (e: Exception) {
            errorHandler.handleError(e)
            emit(SubtitleDownloadState.Error(e))
        }
    }.flowOn(dispatcherProvider.io)
    
    /**
     * Get available subtitle API providers and their status.
     * Used for settings and debugging.
     */
    fun getProviderStatus(): Map<SubtitleApiProvider, SubtitleProviderStatus> {
        return apiClients.associate { client ->
            client.getProvider() to SubtitleProviderStatus(
                enabled = client.isEnabled(),
                rateLimitStatus = rateLimiter.getStatus(client.getProvider()),
                lastError = null // Will be implemented by error handler
            )
        }
    }
    
    /**
     * Clear all cached subtitle data.
     * Used for manual cache management.
     */
    suspend fun clearCache() {
        subtitleCache.clearAll()
    }
}

/**
 * Represents the state of a subtitle download operation.
 */
sealed class SubtitleDownloadState {
    object Loading : SubtitleDownloadState()
    object RateLimited : SubtitleDownloadState()
    data class Success(val filePath: String) : SubtitleDownloadState()
    data class Error(val exception: Throwable) : SubtitleDownloadState()
}

/**
 * Status information for a subtitle API provider.
 */
data class SubtitleProviderStatus(
    val enabled: Boolean,
    val rateLimitStatus: RateLimitStatus,
    val lastError: Throwable?
)