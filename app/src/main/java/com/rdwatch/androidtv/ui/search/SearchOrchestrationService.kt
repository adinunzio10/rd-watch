package com.rdwatch.androidtv.ui.search

import com.rdwatch.androidtv.scraper.ScraperManifestManager
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Orchestrates search operations across multiple scrapers
 * Manages concurrent searches, rate limiting, and result aggregation
 */
@Singleton
class SearchOrchestrationService
    @Inject
    constructor(
        private val manifestManager: ScraperManifestManager,
        private val rateLimiter: SearchRateLimiter,
    ) {
        private val searchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val activeSearches = ConcurrentHashMap<String, Job>()
        private val searchCounter = AtomicInteger(0)

        /**
         * Perform multi-scraper search with orchestration
         */
        suspend fun performSearch(
            query: String,
            filters: SearchFilters = SearchFilters(),
            config: SearchConfig = SearchConfig(),
        ): Flow<SearchOrchestrationResult> =
            flow {
                val searchId = generateSearchId()

                try {
                    emit(SearchOrchestrationResult.Started(searchId, query))

                    // Get enabled scrapers
                    val scrapersResult = manifestManager.getEnabledManifests()
                    when (scrapersResult) {
                        is ManifestResult.Success -> {
                            val scrapers = scrapersResult.data

                            if (scrapers.isEmpty()) {
                                emit(SearchOrchestrationResult.Error(searchId, "No scrapers available"))
                                return@flow
                            }

                            // Filter scrapers based on search filters
                            val filteredScrapers = filterScrapers(scrapers, filters)

                            if (filteredScrapers.isEmpty()) {
                                emit(SearchOrchestrationResult.Error(searchId, "No matching scrapers for current filters"))
                                return@flow
                            }

                            emit(SearchOrchestrationResult.ScrapersSelected(searchId, filteredScrapers.size))

                            // Execute search across scrapers
                            executeMultiScraperSearch(
                                searchId = searchId,
                                query = query,
                                scrapers = filteredScrapers,
                                filters = filters,
                                config = config,
                            ).collect { result ->
                                emit(result)
                            }
                        }
                        is ManifestResult.Error -> {
                            emit(SearchOrchestrationResult.Error(searchId, "Failed to load scrapers: ${scrapersResult.exception.message}"))
                        }
                    }
                } catch (e: Exception) {
                    emit(SearchOrchestrationResult.Error(searchId, "Search orchestration failed: ${e.message}"))
                }
            }.flowOn(Dispatchers.IO)

        /**
         * Cancel active search by ID
         */
        fun cancelSearch(searchId: String) {
            activeSearches[searchId]?.cancel()
            activeSearches.remove(searchId)
        }

        /**
         * Cancel all active searches
         */
        fun cancelAllSearches() {
            activeSearches.values.forEach { it.cancel() }
            activeSearches.clear()
        }

        /**
         * Execute search across multiple scrapers with coordination
         */
        private suspend fun executeMultiScraperSearch(
            searchId: String,
            query: String,
            scrapers: List<ScraperManifest>,
            filters: SearchFilters,
            config: SearchConfig,
        ): Flow<SearchOrchestrationResult> =
            channelFlow {
                val searchJob =
                    searchScope.launch {
                        val results = ConcurrentHashMap<String, ScraperSearchResult>()
                        val errors = ConcurrentHashMap<String, String>()
                        var completedSearches = 0
                        var totalSearches = scrapers.size

                        // Create search channels for coordination
                        val progressChannel = Channel<ScraperProgress>(Channel.UNLIMITED)
                        val resultChannel = Channel<ScraperSearchResult>(Channel.UNLIMITED)
                        val errorChannel = Channel<ScraperError>(Channel.UNLIMITED)

                        // Launch individual scraper searches
                        val scraperJobs =
                            scrapers.map { scraper ->
                                searchScope.launch {
                                    try {
                                        // Apply rate limiting per scraper
                                        rateLimiter.acquirePermit(scraper.id)

                                        // Execute scraper search
                                        executeScraperSearch(
                                            searchId = searchId,
                                            scraper = scraper,
                                            query = query,
                                            filters = filters,
                                            config = config,
                                            progressChannel = progressChannel,
                                            resultChannel = resultChannel,
                                            errorChannel = errorChannel,
                                        )
                                    } catch (e: Exception) {
                                        errorChannel.trySend(ScraperError(scraper.id, e.message ?: "Unknown error"))
                                    } finally {
                                        rateLimiter.releasePermit(scraper.id)
                                    }
                                }
                            }

                        // Process results as they arrive
                        launch {
                            for (progress in progressChannel) {
                                trySend(SearchOrchestrationResult.Progress(searchId, progress.scraperId, progress.status))
                            }
                        }

                        launch {
                            for (result in resultChannel) {
                                results[result.scraperId] = result
                                completedSearches++

                                // Emit partial results
                                val allResults = results.values.flatMap { it.items }
                                trySend(
                                    SearchOrchestrationResult.PartialResults(
                                        searchId = searchId,
                                        results = allResults,
                                        completedScrapers = completedSearches,
                                        totalScrapers = totalSearches,
                                    ),
                                )

                                // Check if search should complete early
                                if (config.earlyCompletion && allResults.size >= config.maxResults) {
                                    scraperJobs.forEach { it.cancel() }
                                    progressChannel.close()
                                    resultChannel.close()
                                    errorChannel.close()
                                }
                            }
                        }

                        launch {
                            for (error in errorChannel) {
                                errors[error.scraperId] = error.message
                                completedSearches++

                                trySend(SearchOrchestrationResult.ScraperError(searchId, error.scraperId, error.message))
                            }
                        }

                        // Wait for all searches to complete
                        scraperJobs.joinAll()

                        // Close channels
                        progressChannel.close()
                        resultChannel.close()
                        errorChannel.close()

                        // Emit final results
                        val finalResults = results.values.flatMap { it.items }
                        trySend(
                            SearchOrchestrationResult.Completed(
                                searchId = searchId,
                                results = finalResults,
                                successfulScrapers = results.size,
                                failedScrapers = errors.size,
                                errors = errors.toMap(),
                            ),
                        )
                    }

                activeSearches[searchId] = searchJob

                try {
                    searchJob.join()
                } finally {
                    activeSearches.remove(searchId)
                }
            }

        /**
         * Execute search on a single scraper
         */
        private suspend fun executeScraperSearch(
            searchId: String,
            scraper: ScraperManifest,
            query: String,
            filters: SearchFilters,
            config: SearchConfig,
            progressChannel: Channel<ScraperProgress>,
            resultChannel: Channel<ScraperSearchResult>,
            errorChannel: Channel<ScraperError>,
        ) {
            try {
                progressChannel.trySend(ScraperProgress(scraper.id, "Starting search"))

                // Simulate scraper-specific search logic
                // In real implementation, this would integrate with actual scraper APIs
                delay(config.scraperTimeoutMs / 4) // Simulate network delay

                progressChannel.trySend(ScraperProgress(scraper.id, "Fetching results"))

                // Simulate search execution with timeout
                withTimeout(config.scraperTimeoutMs) {
                    // Mock search results for demonstration
                    val mockResults = generateMockResults(scraper, query, filters)

                    progressChannel.trySend(ScraperProgress(scraper.id, "Processing results"))

                    delay(500) // Simulate processing time

                    resultChannel.trySend(
                        ScraperSearchResult(
                            scraperId = scraper.id,
                            scraperName = scraper.name,
                            items = mockResults,
                            responseTimeMs = 1000L,
                            totalResults = mockResults.size,
                        ),
                    )

                    progressChannel.trySend(ScraperProgress(scraper.id, "Completed"))
                }
            } catch (e: TimeoutCancellationException) {
                errorChannel.trySend(ScraperError(scraper.id, "Search timeout after ${config.scraperTimeoutMs}ms"))
            } catch (e: Exception) {
                errorChannel.trySend(ScraperError(scraper.id, e.message ?: "Search failed"))
            }
        }

        /**
         * Filter scrapers based on search criteria
         */
        private fun filterScrapers(
            scrapers: List<ScraperManifest>,
            filters: SearchFilters,
        ): List<ScraperManifest> {
            return scrapers.filter { scraper ->
                // Filter by content type
                if (filters.contentTypes.isNotEmpty()) {
                    // Check if scraper supports requested content types
                    filters.contentTypes.any { contentType ->
                        scraper.metadata.capabilities.any { capability ->
                            capability.toString().contains(contentType, ignoreCase = true)
                        }
                    }
                } else {
                    true
                }
            }.sortedBy { it.priorityOrder } // Sort by priority
        }

        /**
         * Generate mock search results for demonstration
         * In real implementation, this would be replaced with actual scraper integration
         */
        private suspend fun generateMockResults(
            scraper: ScraperManifest,
            query: String,
            filters: SearchFilters,
        ): List<SearchResultItem> {
            // Simulate variable result counts and quality
            val resultCount = (1..10).random()

            return (1..resultCount).map { index ->
                SearchResultItem(
                    id = "${scraper.id}_$index",
                    title = "$query Result $index from ${scraper.name}",
                    description = "This is a search result from ${scraper.name} scraper matching '$query'",
                    year = Random.nextInt(2000, 2025),
                    rating = Random.nextFloat() * 3.5f + 6.0f, // Generates 6.0 to 9.5
                    scraperSource = scraper.name,
                )
            }
        }

        private fun generateSearchId(): String {
            return "search_${System.currentTimeMillis()}_${searchCounter.incrementAndGet()}"
        }

        /**
         * Clean up resources
         */
        fun shutdown() {
            cancelAllSearches()
            searchScope.cancel()
        }
    }

/**
 * Results from search orchestration process
 */
sealed class SearchOrchestrationResult {
    data class Started(val searchId: String, val query: String) : SearchOrchestrationResult()

    data class ScrapersSelected(val searchId: String, val scraperCount: Int) : SearchOrchestrationResult()

    data class Progress(val searchId: String, val scraperId: String, val status: String) : SearchOrchestrationResult()

    data class PartialResults(
        val searchId: String,
        val results: List<SearchResultItem>,
        val completedScrapers: Int,
        val totalScrapers: Int,
    ) : SearchOrchestrationResult()

    data class ScraperError(val searchId: String, val scraperId: String, val error: String) : SearchOrchestrationResult()

    data class Completed(
        val searchId: String,
        val results: List<SearchResultItem>,
        val successfulScrapers: Int,
        val failedScrapers: Int,
        val errors: Map<String, String>,
    ) : SearchOrchestrationResult()

    data class Error(val searchId: String, val error: String) : SearchOrchestrationResult()
}

/**
 * Search configuration options
 */
data class SearchConfig(
    val maxResults: Int = 100,
    val scraperTimeoutMs: Long = 10000L,
    val earlyCompletion: Boolean = true,
    val parallelSearches: Boolean = true,
    val retryFailedScrapers: Boolean = false,
    val maxRetries: Int = 1,
)

/**
 * Search filters for targeting specific content
 */
data class SearchFilters(
    val contentTypes: List<String> = emptyList(), // ["movie", "tv", "anime", etc.]
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val minRating: Float? = null,
    val genres: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val qualityPreferences: List<String> = emptyList(), // ["1080p", "720p", "4K"]
    val excludeAdult: Boolean = true,
)

/**
 * Internal data classes for orchestration
 */
private data class ScraperProgress(
    val scraperId: String,
    val status: String,
)

private data class ScraperSearchResult(
    val scraperId: String,
    val scraperName: String,
    val items: List<SearchResultItem>,
    val responseTimeMs: Long,
    val totalResults: Int,
)

private data class ScraperError(
    val scraperId: String,
    val message: String,
)
