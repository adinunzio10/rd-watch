package com.rdwatch.androidtv.ui.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Aggregates and processes search results from multiple scrapers
 * Handles deduplication, scoring, ranking, and filtering
 */
@Singleton
class SearchResultAggregator @Inject constructor() {
    
    /**
     * Aggregate results from multiple scrapers with deduplication and ranking
     */
    fun aggregateResults(
        results: List<SearchResultItem>,
        config: AggregationConfig = AggregationConfig()
    ): AggregatedSearchResults {
        
        if (results.isEmpty()) {
            return AggregatedSearchResults(
                items = emptyList(),
                duplicatesRemoved = 0,
                totalSourceResults = 0,
                aggregationStats = AggregationStats()
            )
        }
        
        // Step 1: Group by potential duplicates
        val duplicateGroups = groupDuplicates(results, config.duplicateThreshold)
        
        // Step 2: Merge duplicate groups
        val deduplicatedResults = duplicateGroups.map { group ->
            mergeDuplicates(group, config)
        }
        
        // Step 3: Score and rank results
        val scoredResults = scoreResults(deduplicatedResults, config)
        
        // Step 4: Apply final filtering and sorting
        val finalResults = scoredResults
            .filter { it.aggregationScore >= config.minScore }
            .sortedWith(compareByDescending<AggregatedSearchResultItem> { it.aggregationScore }
                .thenByDescending { it.confidence }
                .thenByDescending { it.originalResults.size }
                .thenBy { it.title })
            .take(config.maxResults)
        
        val duplicatesRemoved = results.size - deduplicatedResults.size
        
        return AggregatedSearchResults(
            items = finalResults,
            duplicatesRemoved = duplicatesRemoved,
            totalSourceResults = results.size,
            aggregationStats = calculateAggregationStats(finalResults, results)
        )
    }
    
    /**
     * Group results that are likely duplicates based on similarity
     */
    private fun groupDuplicates(
        results: List<SearchResultItem>,
        threshold: Double
    ): List<List<SearchResultItem>> {
        val groups = mutableListOf<MutableList<SearchResultItem>>()
        val processed = mutableSetOf<Int>()
        
        for (i in results.indices) {
            if (i in processed) continue
            
            val currentGroup = mutableListOf(results[i])
            processed.add(i)
            
            for (j in (i + 1) until results.size) {
                if (j in processed) continue
                
                val similarity = calculateSimilarity(results[i], results[j])
                if (similarity >= threshold) {
                    currentGroup.add(results[j])
                    processed.add(j)
                }
            }
            
            groups.add(currentGroup)
        }
        
        return groups
    }
    
    /**
     * Calculate similarity between two search results
     */
    private fun calculateSimilarity(result1: SearchResultItem, result2: SearchResultItem): Double {
        val titleSimilarity = calculateStringSimilarity(result1.title, result2.title)
        val yearSimilarity = if (result1.year != null && result2.year != null) {
            val yearDiff = kotlin.math.abs(result1.year - result2.year)
            when {
                yearDiff == 0 -> 1.0
                yearDiff == 1 -> 0.8
                yearDiff <= 2 -> 0.6
                else -> 0.0
            }
        } else 0.5
        
        // Weight title similarity more heavily
        return (titleSimilarity * 0.8) + (yearSimilarity * 0.2)
    }
    
    /**
     * Calculate string similarity using Levenshtein distance
     */
    private fun calculateStringSimilarity(str1: String, str2: String): Double {
        val s1 = str1.lowercase().trim()
        val s2 = str2.lowercase().trim()
        
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        
        val maxLength = max(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        
        return 1.0 - (distance.toDouble() / maxLength)
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }
    
    /**
     * Merge duplicate results into a single aggregated result
     */
    private fun mergeDuplicates(
        duplicates: List<SearchResultItem>,
        config: AggregationConfig
    ): AggregatedSearchResultItem {
        
        if (duplicates.size == 1) {
            return AggregatedSearchResultItem(
                id = duplicates[0].id,
                title = duplicates[0].title,
                description = duplicates[0].description,
                thumbnailUrl = duplicates[0].thumbnailUrl,
                year = duplicates[0].year,
                rating = duplicates[0].rating,
                originalResults = duplicates,
                aggregationScore = 0.0, // Will be calculated later
                confidence = 1.0,
                sourceCount = 1
            )
        }
        
        // Choose the best title (most complete/detailed)
        val bestTitle = duplicates.maxByOrNull { it.title.length }?.title ?: duplicates[0].title
        
        // Choose the best description
        val bestDescription = duplicates
            .mapNotNull { it.description }
            .maxByOrNull { it.length }
        
        // Choose the best thumbnail
        val bestThumbnail = duplicates
            .mapNotNull { it.thumbnailUrl }
            .firstOrNull()
        
        // Aggregate year (most common, or latest if tie)
        val years = duplicates.mapNotNull { it.year }
        val bestYear = if (years.isNotEmpty()) {
            years.groupBy { it }
                .maxByOrNull { it.value.size }
                ?.key ?: years.maxOrNull()
        } else null
        
        // Aggregate rating (weighted average)
        val ratings = duplicates.mapNotNull { it.rating }
        val aggregatedRating = if (ratings.isNotEmpty()) {
            ratings.average().toFloat()
        } else null
        
        // Calculate confidence based on consensus
        val confidence = calculateConfidence(duplicates)
        
        return AggregatedSearchResultItem(
            id = "aggregated_${duplicates.joinToString("_") { it.id }}",
            title = bestTitle,
            description = bestDescription,
            thumbnailUrl = bestThumbnail,
            year = bestYear,
            rating = aggregatedRating,
            originalResults = duplicates,
            aggregationScore = 0.0, // Will be calculated later
            confidence = confidence,
            sourceCount = duplicates.size
        )
    }
    
    /**
     * Calculate confidence score based on result consensus
     */
    private fun calculateConfidence(results: List<SearchResultItem>): Double {
        if (results.size == 1) return 1.0
        
        // Factors that increase confidence:
        // - Multiple sources agree
        // - Consistent metadata (year, rating)
        // - Quality of source scrapers
        
        val sourceCount = results.size.toDouble()
        val baseConfidence = min(1.0, sourceCount / 3.0) // More sources = higher confidence
        
        // Boost confidence if metadata is consistent
        val years = results.mapNotNull { it.year }.distinct()
        val yearConsistency = if (years.size <= 1) 0.1 else 0.0
        
        val ratings = results.mapNotNull { it.rating }
        val ratingVariance = if (ratings.size > 1) {
            val avg = ratings.average()
            val variance = ratings.map { (it - avg) * (it - avg) }.average()
            max(0.0, 0.1 - variance / 10.0) // Lower variance = higher boost
        } else 0.0
        
        return min(1.0, baseConfidence + yearConsistency + ratingVariance)
    }
    
    /**
     * Score results based on relevance, quality, and aggregation factors
     */
    private fun scoreResults(
        results: List<AggregatedSearchResultItem>,
        config: AggregationConfig
    ): List<AggregatedSearchResultItem> {
        
        return results.map { result ->
            val score = calculateAggregationScore(result, config)
            result.copy(aggregationScore = score)
        }
    }
    
    /**
     * Calculate aggregation score for a result
     */
    private fun calculateAggregationScore(
        result: AggregatedSearchResultItem,
        config: AggregationConfig
    ): Double {
        var score = 0.0
        
        // Base score from source count (more sources = higher score)
        score += result.sourceCount * config.sourceCountWeight
        
        // Confidence boost
        score += result.confidence * config.confidenceWeight
        
        // Rating boost (if available)
        result.rating?.let { rating ->
            score += (rating / 10.0) * config.ratingWeight
        }
        
        // Recency boost (newer content scores higher)
        result.year?.let { year ->
            val currentYear = 2024 // Could be dynamic
            val age = currentYear - year
            val recencyScore = max(0.0, 1.0 - (age / 20.0)) // Linear decay over 20 years
            score += recencyScore * config.recencyWeight
        }
        
        // Completeness boost (more metadata = higher score)
        val completeness = listOfNotNull(
            result.description,
            result.thumbnailUrl,
            result.year,
            result.rating
        ).size / 4.0
        score += completeness * config.completenessWeight
        
        return score
    }
    
    /**
     * Calculate aggregation statistics
     */
    private fun calculateAggregationStats(
        finalResults: List<AggregatedSearchResultItem>,
        originalResults: List<SearchResultItem>
    ): AggregationStats {
        
        val sourceCounts = finalResults.map { it.sourceCount }
        val confidenceScores = finalResults.map { it.confidence }
        val aggregationScores = finalResults.map { it.aggregationScore }
        
        return AggregationStats(
            averageSourcesPerResult = if (sourceCounts.isNotEmpty()) sourceCounts.average() else 0.0,
            averageConfidence = if (confidenceScores.isNotEmpty()) confidenceScores.average() else 0.0,
            averageAggregationScore = if (aggregationScores.isNotEmpty()) aggregationScores.average() else 0.0,
            maxSourcesPerResult = sourceCounts.maxOrNull() ?: 0,
            resultsWithMultipleSources = sourceCounts.count { it > 1 },
            uniqueScraperSources = originalResults.map { it.scraperSource }.distinct().size
        )
    }
}

/**
 * Configuration for result aggregation
 */
data class AggregationConfig(
    val duplicateThreshold: Double = 0.8, // Similarity threshold for duplicate detection
    val maxResults: Int = 100,
    val minScore: Double = 0.1,
    
    // Scoring weights
    val sourceCountWeight: Double = 0.3,
    val confidenceWeight: Double = 0.25,
    val ratingWeight: Double = 0.2,
    val recencyWeight: Double = 0.15,
    val completenessWeight: Double = 0.1
)

/**
 * Aggregated search result with enhanced metadata
 */
data class AggregatedSearchResultItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val year: Int? = null,
    val rating: Float? = null,
    val originalResults: List<SearchResultItem>,
    val aggregationScore: Double,
    val confidence: Double,
    val sourceCount: Int
) {
    fun getScraperSources(): List<String> {
        return originalResults.mapNotNull { it.scraperSource }.distinct()
    }
    
    fun getBestResult(): SearchResultItem {
        return originalResults.maxByOrNull { it.rating ?: 0f } ?: originalResults.first()
    }
}

/**
 * Results of the aggregation process
 */
data class AggregatedSearchResults(
    val items: List<AggregatedSearchResultItem>,
    val duplicatesRemoved: Int,
    val totalSourceResults: Int,
    val aggregationStats: AggregationStats
)

/**
 * Statistics about the aggregation process
 */
data class AggregationStats(
    val averageSourcesPerResult: Double = 0.0,
    val averageConfidence: Double = 0.0,
    val averageAggregationScore: Double = 0.0,
    val maxSourcesPerResult: Int = 0,
    val resultsWithMultipleSources: Int = 0,
    val uniqueScraperSources: Int = 0
)

/**
 * Extension function to convert aggregated results back to simple results
 */
fun AggregatedSearchResults.toSimpleResults(): List<SearchResultItem> {
    return items.map { aggregated ->
        SearchResultItem(
            id = aggregated.id,
            title = aggregated.title,
            description = aggregated.description,
            thumbnailUrl = aggregated.thumbnailUrl,
            year = aggregated.year,
            rating = aggregated.rating,
            scraperSource = aggregated.getScraperSources().joinToString(", ")
        )
    }
}

/**
 * Flow extension for aggregating search results
 */
fun Flow<List<SearchResultItem>>.aggregateResults(
    config: AggregationConfig = AggregationConfig()
): Flow<AggregatedSearchResults> {
    return map { results ->
        SearchResultAggregator().aggregateResults(results, config)
    }
}