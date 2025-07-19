package com.rdwatch.androidtv.ui.search

import com.rdwatch.androidtv.data.dao.SearchHistoryDao
import com.rdwatch.androidtv.data.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages search history with Room database persistence
 * Provides search suggestions and history management
 */
@Singleton
class SearchHistoryManager
    @Inject
    constructor(
        private val searchHistoryDao: SearchHistoryDao,
    ) {
        /**
         * Add a search query to history
         */
        suspend fun addSearchQuery(
            userId: Long,
            query: String,
            searchType: String = "general",
            resultsCount: Int = 0,
            responseTimeMs: Long? = null,
            filtersJson: String? = null,
        ) {
            if (query.isBlank()) return

            val searchHistoryEntity =
                SearchHistoryEntity(
                    userId = userId,
                    searchQuery = query.trim(),
                    searchType = searchType,
                    resultsCount = resultsCount,
                    searchDate = Date(),
                    filtersJson = filtersJson,
                    responseTimeMs = responseTimeMs,
                )

            searchHistoryDao.insertSearchHistory(searchHistoryEntity)
        }

        /**
         * Get recent search history for a user
         */
        fun getRecentSearchHistory(
            userId: Long,
            limit: Int = 20,
        ): Flow<List<String>> {
            return searchHistoryDao.getRecentSearchHistory(userId, limit)
                .map { entities -> entities.map { it.searchQuery } }
        }

        /**
         * Get search suggestions based on partial query
         */
        suspend fun getSearchSuggestions(
            userId: Long,
            partialQuery: String,
            limit: Int = 10,
        ): List<String> {
            if (partialQuery.isBlank()) return emptyList()

            return searchHistoryDao.getSearchSuggestions(userId, partialQuery.trim(), limit)
        }

        /**
         * Get popular search queries for a user
         */
        suspend fun getPopularSearchQueries(
            userId: Long,
            limit: Int = 10,
        ): List<String> {
            return searchHistoryDao.getPopularSearchQueries(userId, limit)
        }

        /**
         * Search within search history
         */
        fun searchInHistory(
            userId: Long,
            query: String,
        ): Flow<List<SearchHistoryItem>> {
            return searchHistoryDao.searchInHistory(userId, query)
                .map { entities -> entities.map { it.toSearchHistoryItem() } }
        }

        /**
         * Get search history by type
         */
        fun getSearchHistoryByType(
            userId: Long,
            searchType: String,
        ): Flow<List<SearchHistoryItem>> {
            return searchHistoryDao.getSearchHistoryByType(userId, searchType)
                .map { entities -> entities.map { it.toSearchHistoryItem() } }
        }

        /**
         * Get search history within date range
         */
        fun getSearchHistoryByDateRange(
            userId: Long,
            startDate: Date,
            endDate: Date,
        ): Flow<List<SearchHistoryItem>> {
            return searchHistoryDao.getSearchHistoryByDateRange(userId, startDate, endDate)
                .map { entities -> entities.map { it.toSearchHistoryItem() } }
        }

        /**
         * Delete a specific search query from history
         */
        suspend fun deleteSearchQuery(
            userId: Long,
            query: String,
        ) {
            searchHistoryDao.deleteSearchHistoryForQuery(userId, query)
        }

        /**
         * Delete search history by ID
         */
        suspend fun deleteSearchHistoryById(searchId: Long) {
            searchHistoryDao.deleteSearchHistoryById(searchId)
        }

        /**
         * Clear all search history for a user
         */
        suspend fun clearAllHistory(userId: Long) {
            searchHistoryDao.deleteAllSearchHistoryForUser(userId)
        }

        /**
         * Get search statistics for a user
         */
        suspend fun getSearchStatistics(userId: Long): SearchStatistics {
            val totalSearches = searchHistoryDao.getSearchCountByUser(userId)
            val averageResponseTime = searchHistoryDao.getAverageResponseTimeByUser(userId)
            val searchTypes = searchHistoryDao.getSearchTypesByUser(userId)

            return SearchStatistics(
                totalSearches = totalSearches,
                averageResponseTimeMs = averageResponseTime,
                uniqueSearchTypes = searchTypes.size,
                searchTypes = searchTypes,
            )
        }

        /**
         * Get search count for a specific query
         */
        suspend fun getSearchCountForQuery(
            userId: Long,
            query: String,
        ): Int {
            return searchHistoryDao.getSearchCountForQuery(userId, query)
        }

        /**
         * Clean up old search history entries
         */
        suspend fun cleanupOldHistory(
            userId: Long,
            keepCount: Int = 1000,
        ) {
            searchHistoryDao.cleanupOldSearchHistory(userId, keepCount)
        }

        /**
         * Delete search history older than specified date
         */
        suspend fun deleteOldHistory(cutoffDate: Date) {
            searchHistoryDao.deleteOldSearchHistory(cutoffDate)
        }

        /**
         * Get trending search queries (most searched recently)
         */
        suspend fun getTrendingQueries(
            userId: Long,
            days: Int = 7,
            limit: Int = 10,
        ): List<TrendingQuery> {
            val cutoffDate = Date(System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L))
            val recentHistory =
                searchHistoryDao.getSearchHistoryByDateRange(
                    userId,
                    cutoffDate,
                    Date(),
                )

            // Convert to flow and collect to get the list
            return recentHistory.map { entities ->
                entities.groupBy { it.searchQuery }
                    .map { (query, searches) ->
                        TrendingQuery(
                            query = query,
                            searchCount = searches.size,
                            lastSearched = searches.maxOfOrNull { it.searchDate } ?: Date(),
                            averageResults = searches.mapNotNull { it.resultsCount }.average().toInt(),
                        )
                    }
                    .sortedByDescending { it.searchCount }
                    .take(limit)
            }.let { flow ->
                // Since we need to return List, collect the flow
                var result = emptyList<TrendingQuery>()
                flow.collect { result = it }
                result
            }
        }

        /**
         * Export search history as JSON
         */
        suspend fun exportSearchHistory(userId: Long): String {
            val history = searchHistoryDao.getSearchHistoryByUser(userId)

            return history.map { entities ->
                entities.map { entity ->
                    mapOf(
                        "query" to entity.searchQuery,
                        "type" to entity.searchType,
                        "resultsCount" to entity.resultsCount,
                        "date" to entity.searchDate.time,
                        "responseTime" to entity.responseTimeMs,
                        "filters" to entity.filtersJson,
                    )
                }
            }.let { flow ->
                // Convert to JSON string (simplified)
                var result = "[]"
                flow.collect {
                    result = it.toString() // In real implementation, use proper JSON serialization
                }
                result
            }
        }

        /**
         * Get smart search suggestions based on user behavior
         */
        suspend fun getSmartSuggestions(
            userId: Long,
            context: SearchContext = SearchContext(),
        ): List<SmartSuggestion> {
            val popularQueries = getPopularSearchQueries(userId, 20)
            val recentQueries = getRecentSearchHistory(userId, 10)

            val suggestions = mutableListOf<SmartSuggestion>()

            // Add recent queries as quick suggestions
            recentQueries.collect { queries ->
                queries.take(5).forEach { query ->
                    suggestions.add(
                        SmartSuggestion(
                            query = query,
                            type = SuggestionType.RECENT,
                            confidence = 0.8,
                            reason = "Recently searched",
                        ),
                    )
                }
            }

            // Add popular queries
            popularQueries.take(3).forEach { query ->
                if (suggestions.none { it.query == query }) {
                    suggestions.add(
                        SmartSuggestion(
                            query = query,
                            type = SuggestionType.POPULAR,
                            confidence = 0.7,
                            reason = "Frequently searched",
                        ),
                    )
                }
            }

            // Add contextual suggestions based on time of day, season, etc.
            if (context.includeContextual) {
                suggestions.addAll(getContextualSuggestions(context))
            }

            return suggestions.sortedByDescending { it.confidence }.take(10)
        }

        /**
         * Get contextual suggestions based on current context
         */
        private fun getContextualSuggestions(context: SearchContext): List<SmartSuggestion> {
            val suggestions = mutableListOf<SmartSuggestion>()

            // Time-based suggestions
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when {
                hour in 6..11 -> {
                    suggestions.add(
                        SmartSuggestion(
                            query = "morning shows",
                            type = SuggestionType.CONTEXTUAL,
                            confidence = 0.6,
                            reason = "Good morning viewing",
                        ),
                    )
                }
                hour in 12..17 -> {
                    suggestions.add(
                        SmartSuggestion(
                            query = "afternoon movies",
                            type = SuggestionType.CONTEXTUAL,
                            confidence = 0.6,
                            reason = "Afternoon entertainment",
                        ),
                    )
                }
                hour in 18..23 -> {
                    suggestions.add(
                        SmartSuggestion(
                            query = "prime time",
                            type = SuggestionType.CONTEXTUAL,
                            confidence = 0.6,
                            reason = "Prime time viewing",
                        ),
                    )
                }
            }

            // Season-based suggestions
            val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
            when (month) {
                11, 0, 1 ->
                    suggestions.add(
                        SmartSuggestion(
                            query = "winter movies",
                            type = SuggestionType.CONTEXTUAL,
                            confidence = 0.5,
                            reason = "Winter season",
                        ),
                    )
                2, 3, 4 ->
                    suggestions.add(
                        SmartSuggestion(
                            query = "spring shows",
                            type = SuggestionType.CONTEXTUAL,
                            confidence = 0.5,
                            reason = "Spring season",
                        ),
                    )
            }

            return suggestions
        }
    }

/**
 * Extension function to convert entity to domain model
 */
private fun SearchHistoryEntity.toSearchHistoryItem(): SearchHistoryItem {
    return SearchHistoryItem(
        id = searchId,
        query = searchQuery,
        type = searchType,
        resultsCount = resultsCount,
        date = searchDate,
        responseTimeMs = responseTimeMs,
        filtersJson = filtersJson,
    )
}

/**
 * Domain model for search history item
 */
data class SearchHistoryItem(
    val id: Long,
    val query: String,
    val type: String,
    val resultsCount: Int,
    val date: Date,
    val responseTimeMs: Long?,
    val filtersJson: String?,
)

/**
 * Search statistics for a user
 */
data class SearchStatistics(
    val totalSearches: Int,
    val averageResponseTimeMs: Float?,
    val uniqueSearchTypes: Int,
    val searchTypes: List<String>,
)

/**
 * Trending query information
 */
data class TrendingQuery(
    val query: String,
    val searchCount: Int,
    val lastSearched: Date,
    val averageResults: Int,
)

/**
 * Smart suggestion with context
 */
data class SmartSuggestion(
    val query: String,
    val type: SuggestionType,
    val confidence: Double,
    val reason: String,
)

/**
 * Types of suggestions
 */
enum class SuggestionType {
    RECENT,
    POPULAR,
    TRENDING,
    CONTEXTUAL,
    PERSONALIZED,
}

/**
 * Context for generating smart suggestions
 */
data class SearchContext(
    val timeOfDay: Int? = null,
    val dayOfWeek: Int? = null,
    val season: String? = null,
    val includeContextual: Boolean = true,
    val includePersonalized: Boolean = true,
)
