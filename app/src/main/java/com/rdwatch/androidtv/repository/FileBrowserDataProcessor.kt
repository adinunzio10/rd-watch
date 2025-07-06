package com.rdwatch.androidtv.repository

import com.rdwatch.androidtv.repository.filtering.FileFilteringService
import com.rdwatch.androidtv.repository.sorting.FileSortingService
import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level service that combines sorting and filtering operations
 * with performance optimizations for large datasets
 */
@Singleton
class FileBrowserDataProcessor @Inject constructor(
    private val sortingService: FileSortingService,
    private val filteringService: FileFilteringService
) {
    
    /**
     * Process files with sorting and filtering in an optimized way
     */
    suspend fun processFiles(
        files: List<AccountFileItem>,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        filter: FileFilterCriteria = FileFilterCriteria(),
        limit: Int? = null
    ): ProcessedFileResult = withContext(Dispatchers.Default) {
        
        // Early exit for empty input
        if (files.isEmpty()) {
            return@withContext ProcessedFileResult(
                files = emptyList(),
                totalCount = 0,
                filteredCount = 0,
                statistics = FileFilterStatistics(
                    totalFiles = 0,
                    typeStatistics = emptyMap(),
                    sourceStatistics = emptyMap(),
                    statusStatistics = emptyMap(),
                    sizeStatistics = FileSizeStatistics(0, 0, 0, 0),
                    dateStatistics = FileDateStatistics(Date(), Date())
                ),
                processingTimeMs = 0
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        // Step 1: Apply filters first to reduce dataset size
        val filteredFiles = if (filter.hasActiveFilters) {
            filteringService.applyFilter(files, filter)
        } else {
            files
        }
        
        // Step 2: Sort the filtered results
        val sortedFiles = sortingService.sortFiles(filteredFiles, sortOption)
        
        // Step 3: Apply limit if specified
        val limitedFiles = if (limit != null && limit > 0) {
            sortedFiles.take(limit)
        } else {
            sortedFiles
        }
        
        // Step 4: Generate statistics
        val statistics = sortingService.getFilterStatistics(files)
        
        val processingTime = System.currentTimeMillis() - startTime
        
        ProcessedFileResult(
            files = limitedFiles,
            totalCount = files.size,
            filteredCount = filteredFiles.size,
            statistics = statistics,
            processingTimeMs = processingTime
        )
    }
    
    /**
     * Search files with advanced ranking and filtering
     */
    suspend fun searchFiles(
        files: List<AccountFileItem>,
        query: String,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        additionalFilter: FileFilterCriteria = FileFilterCriteria(),
        limit: Int? = null,
        useRanking: Boolean = true
    ): ProcessedFileResult = withContext(Dispatchers.Default) {
        
        if (files.isEmpty() || query.isBlank()) {
            return@withContext processFiles(files, sortOption, additionalFilter, limit)
        }
        
        val startTime = System.currentTimeMillis()
        
        // Combine search query with additional filters
        val searchFilter = additionalFilter.copy(searchQuery = query)
        
        // Use sorting service's search functionality for ranking
        val searchResults = sortingService.searchFiles(files, query, sortOption, useRanking)
        
        // Apply additional filters if any
        val filteredResults = if (additionalFilter.hasActiveFiltersExceptSearch()) {
            filteringService.applyFilter(searchResults, additionalFilter.copy(searchQuery = ""))
        } else {
            searchResults
        }
        
        // Apply limit
        val limitedResults = if (limit != null && limit > 0) {
            filteredResults.take(limit)
        } else {
            filteredResults
        }
        
        val statistics = sortingService.getFilterStatistics(files)
        val processingTime = System.currentTimeMillis() - startTime
        
        ProcessedFileResult(
            files = limitedResults,
            totalCount = files.size,
            filteredCount = filteredResults.size,
            statistics = statistics,
            processingTimeMs = processingTime
        )
    }
    
    /**
     * Get files grouped by category with processing
     */
    suspend fun getGroupedFiles(
        files: List<AccountFileItem>,
        groupBy: GroupByOption,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        filter: FileFilterCriteria = FileFilterCriteria()
    ): GroupedFileResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        
        // Apply filters first
        val filteredFiles = if (filter.hasActiveFilters) {
            filteringService.applyFilter(files, filter)
        } else {
            files
        }
        
        // Group files based on the specified option
        val groupedFiles = when (groupBy) {
            GroupByOption.TYPE -> {
                sortingService.groupFilesByType(filteredFiles, sortOption)
                    .mapKeys { it.key.displayName }
            }
            GroupByOption.SOURCE -> {
                sortingService.groupFilesBySource(filteredFiles, sortOption)
                    .mapKeys { it.key.name }
            }
            GroupByOption.STATUS -> {
                filteredFiles.groupBy { it.availabilityStatus.displayName }
                    .mapValues { (_, groupFiles) ->
                        sortingService.sortFiles(groupFiles, sortOption)
                    }
            }
            GroupByOption.DATE -> {
                groupFilesByDate(filteredFiles, sortOption)
            }
            GroupByOption.SIZE -> {
                groupFilesBySize(filteredFiles, sortOption)
            }
            GroupByOption.HOST -> {
                filteredFiles.groupBy { it.host ?: "Unknown" }
                    .mapValues { (_, groupFiles) ->
                        sortingService.sortFiles(groupFiles, sortOption)
                    }
            }
        }
        
        val statistics = sortingService.getFilterStatistics(files)
        val processingTime = System.currentTimeMillis() - startTime
        
        GroupedFileResult(
            groups = groupedFiles,
            totalCount = files.size,
            filteredCount = filteredFiles.size,
            statistics = statistics,
            processingTimeMs = processingTime
        )
    }
    
    /**
     * Get quick statistics without full processing
     */
    suspend fun getQuickStatistics(files: List<AccountFileItem>): FileFilterStatistics = withContext(Dispatchers.Default) {
        sortingService.getFilterStatistics(files)
    }
    
    /**
     * Get filter suggestions based on current dataset
     */
    suspend fun getFilterSuggestions(files: List<AccountFileItem>): FilterSuggestions = withContext(Dispatchers.Default) {
        filteringService.getFilterSuggestions(files)
    }
    
    /**
     * Apply a predefined filter preset
     */
    suspend fun applyFilterPreset(
        files: List<AccountFileItem>,
        preset: FileFilteringService.FilterPreset,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        limit: Int? = null
    ): ProcessedFileResult = withContext(Dispatchers.Default) {
        processFiles(files, sortOption, preset.criteria, limit)
    }
    
    /**
     * Get files by multiple criteria with OR logic
     */
    suspend fun getFilesByMultipleCriteria(
        files: List<AccountFileItem>,
        criteriaList: List<FileFilterCriteria>,
        useOrLogic: Boolean = false,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC
    ): ProcessedFileResult = withContext(Dispatchers.Default) {
        
        val combinedCriteria = if (useOrLogic) {
            filteringService.createOrFilter(*criteriaList.toTypedArray())
        } else {
            filteringService.createAndFilter(*criteriaList.toTypedArray())
        }
        
        processFiles(files, sortOption, combinedCriteria)
    }
    
    // Private helper methods
    
    private fun groupFilesByDate(files: List<AccountFileItem>, sortOption: FileEnhancedSortOption): Map<String, List<AccountFileItem>> {
        val now = Calendar.getInstance()
        val today = Calendar.getInstance()
        val thisWeek = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }
        val thisMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val thisYear = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
        
        return files.groupBy { file ->
            val fileDate = Calendar.getInstance().apply { time = file.dateAdded }
            when {
                isSameDay(fileDate, today) -> "Today"
                fileDate.after(thisWeek) -> "This Week"
                fileDate.after(thisMonth) -> "This Month" 
                fileDate.after(thisYear) -> "This Year"
                else -> "Older"
            }
        }.mapValues { (_, groupFiles) ->
            sortingService.sortFiles(groupFiles, sortOption)
        }
    }
    
    private fun groupFilesBySize(files: List<AccountFileItem>, sortOption: FileEnhancedSortOption): Map<String, List<AccountFileItem>> {
        return files.groupBy { file ->
            when {
                file.filesize < 10L * 1024L * 1024L -> "Tiny (< 10MB)"
                file.filesize < 100L * 1024L * 1024L -> "Small (10MB - 100MB)"
                file.filesize < 1024L * 1024L * 1024L -> "Medium (100MB - 1GB)"
                file.filesize < 5L * 1024L * 1024L * 1024L -> "Large (1GB - 5GB)"
                else -> "Huge (> 5GB)"
            }
        }.mapValues { (_, groupFiles) ->
            sortingService.sortFiles(groupFiles, sortOption)
        }
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

/**
 * Options for grouping files
 */
enum class GroupByOption(val displayName: String) {
    TYPE("File Type"),
    SOURCE("Source"),
    STATUS("Status"),
    DATE("Date Added"),
    SIZE("File Size"),
    HOST("Host")
}

/**
 * Result of file processing operations
 */
data class ProcessedFileResult(
    val files: List<AccountFileItem>,
    val totalCount: Int,
    val filteredCount: Int,
    val statistics: FileFilterStatistics,
    val processingTimeMs: Long
) {
    val hasResults: Boolean get() = files.isNotEmpty()
    val isFiltered: Boolean get() = filteredCount < totalCount
    val filterEfficiency: Float get() = if (totalCount > 0) filteredCount.toFloat() / totalCount else 0f
}

/**
 * Result of grouped file operations
 */
data class GroupedFileResult(
    val groups: Map<String, List<AccountFileItem>>,
    val totalCount: Int,
    val filteredCount: Int,
    val statistics: FileFilterStatistics,
    val processingTimeMs: Long
) {
    val groupCount: Int get() = groups.size
    val hasResults: Boolean get() = groups.isNotEmpty()
    val largestGroupSize: Int get() = groups.values.maxOfOrNull { it.size } ?: 0
    val smallestGroupSize: Int get() = groups.values.minOfOrNull { it.size } ?: 0
}

/**
 * Extension function to check if filter criteria has active filters except search
 */
private fun FileFilterCriteria.hasActiveFiltersExceptSearch(): Boolean {
    return fileTypes.isNotEmpty() ||
           sources.isNotEmpty() ||
           availabilityStatus.isNotEmpty() ||
           minFileSize != null ||
           maxFileSize != null ||
           dateAfter != null ||
           dateBefore != null ||
           isStreamableOnly
}