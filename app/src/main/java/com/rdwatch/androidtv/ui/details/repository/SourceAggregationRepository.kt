package com.rdwatch.androidtv.ui.details.repository

import com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for aggregating sources from multiple providers
 */
interface SourceAggregationRepository {
    
    /**
     * Get all available sources for a content item
     * @param contentDetail The content to get sources for
     * @param forceRefresh Force refresh from scrapers
     * @return Flow of source metadata list
     */
    fun getSources(
        contentDetail: ContentDetail,
        forceRefresh: Boolean = false
    ): Flow<List<SourceMetadata>>
    
    /**
     * Get sources from a specific provider
     * @param contentDetail The content to get sources for
     * @param providerId The provider ID to get sources from
     * @return Flow of source metadata list
     */
    fun getSourcesFromProvider(
        contentDetail: ContentDetail,
        providerId: String
    ): Flow<List<SourceMetadata>>
    
    /**
     * Get cached/debrid sources
     * @param contentDetail The content to get sources for
     * @return Flow of cached source metadata list
     */
    fun getCachedSources(
        contentDetail: ContentDetail
    ): Flow<List<SourceMetadata>>
    
    /**
     * Check if a source is cached in debrid service
     * @param source The source to check
     * @return True if cached
     */
    suspend fun isSourceCached(source: SourceMetadata): Boolean
    
    /**
     * Add source to debrid service
     * @param source The source to add
     * @return Updated source metadata with debrid info
     */
    suspend fun addToDebrid(source: SourceMetadata): SourceMetadata
    
    /**
     * Get streaming URL for a source
     * @param source The source to get URL for
     * @return Streaming URL
     */
    suspend fun getStreamingUrl(source: SourceMetadata): String
    
    /**
     * Refresh source health information (seeders, availability, etc.)
     * @param sources Sources to refresh
     * @return Updated sources with fresh health info
     */
    suspend fun refreshSourceHealth(sources: List<SourceMetadata>): List<SourceMetadata>
    
    /**
     * Get available providers
     * @return List of available provider IDs
     */
    suspend fun getAvailableProviders(): List<String>
    
    /**
     * Clear cached sources
     */
    suspend fun clearCache()
}