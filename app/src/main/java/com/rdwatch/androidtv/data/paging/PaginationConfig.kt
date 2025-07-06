package com.rdwatch.androidtv.data.paging

import androidx.paging.PagingConfig

/**
 * Pagination configuration for different use cases in the File Browser
 */
object PaginationConfig {
    
    /**
     * Standard pagination config for file browser
     */
    val STANDARD_CONFIG = PagingConfig(
        pageSize = 20,
        prefetchDistance = 5,
        enablePlaceholders = false,
        initialLoadSize = 40,
        maxSize = 200 // Keep max 200 items in memory
    )
    
    /**
     * High performance config for large datasets
     */
    val HIGH_PERFORMANCE_CONFIG = PagingConfig(
        pageSize = 50,
        prefetchDistance = 10,
        enablePlaceholders = true,
        initialLoadSize = 100,
        maxSize = 500
    )
    
    /**
     * Compact config for limited memory scenarios
     */
    val COMPACT_CONFIG = PagingConfig(
        pageSize = 10,
        prefetchDistance = 3,
        enablePlaceholders = false,
        initialLoadSize = 20,
        maxSize = 100
    )
    
    /**
     * Search results config (smaller pages for faster results)
     */
    val SEARCH_CONFIG = PagingConfig(
        pageSize = 15,
        prefetchDistance = 3,
        enablePlaceholders = false,
        initialLoadSize = 30,
        maxSize = 150
    )
    
    /**
     * Grid view config (optimized for grid display)
     */
    val GRID_CONFIG = PagingConfig(
        pageSize = 24, // Divisible by common grid column counts (2, 3, 4, 6)
        prefetchDistance = 6,
        enablePlaceholders = false,
        initialLoadSize = 48,
        maxSize = 240
    )
    
    /**
     * Get appropriate config based on scenario
     */
    fun getConfigForScenario(scenario: PaginationScenario): PagingConfig {
        return when (scenario) {
            PaginationScenario.STANDARD -> STANDARD_CONFIG
            PaginationScenario.HIGH_PERFORMANCE -> HIGH_PERFORMANCE_CONFIG
            PaginationScenario.COMPACT -> COMPACT_CONFIG
            PaginationScenario.SEARCH -> SEARCH_CONFIG
            PaginationScenario.GRID_VIEW -> GRID_CONFIG
        }
    }
}

/**
 * Different pagination scenarios
 */
enum class PaginationScenario {
    STANDARD,
    HIGH_PERFORMANCE,
    COMPACT,
    SEARCH,
    GRID_VIEW
}