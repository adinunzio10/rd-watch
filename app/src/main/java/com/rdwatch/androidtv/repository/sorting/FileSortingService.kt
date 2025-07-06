package com.rdwatch.androidtv.repository.sorting

import com.rdwatch.androidtv.ui.filebrowser.models.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for sorting and filtering account files with optimized algorithms
 */
@Singleton
class FileSortingService @Inject constructor() {
    
    /**
     * Sort files by the specified sort option
     */
    fun sortFiles(files: List<AccountFileItem>, sortOption: FileEnhancedSortOption): List<AccountFileItem> {
        return when (sortOption) {
            FileEnhancedSortOption.NAME_ASC -> files.sortedBy { it.filename.lowercase() }
            FileEnhancedSortOption.NAME_DESC -> files.sortedByDescending { it.filename.lowercase() }
            FileEnhancedSortOption.SIZE_ASC -> files.sortedBy { it.filesize }
            FileEnhancedSortOption.SIZE_DESC -> files.sortedByDescending { it.filesize }
            FileEnhancedSortOption.DATE_ASC -> files.sortedBy { it.dateAdded }
            FileEnhancedSortOption.DATE_DESC -> files.sortedByDescending { it.dateAdded }
            FileEnhancedSortOption.TYPE_ASC -> files.sortedBy { it.fileTypeCategory.displayName }
            FileEnhancedSortOption.TYPE_DESC -> files.sortedByDescending { it.fileTypeCategory.displayName }
            FileEnhancedSortOption.SOURCE_ASC -> files.sortedWith(
                compareBy<AccountFileItem> { it.source.ordinal }
                    .thenBy { it.filename.lowercase() }
            )
            FileEnhancedSortOption.SOURCE_DESC -> files.sortedWith(
                compareByDescending<AccountFileItem> { it.source.ordinal }
                    .thenBy { it.filename.lowercase() }
            )
            FileEnhancedSortOption.STATUS_ASC -> files.sortedWith(
                compareBy<AccountFileItem> { it.availabilityStatus.ordinal }
                    .thenByDescending { it.dateAdded }
            )
            FileEnhancedSortOption.STATUS_DESC -> files.sortedWith(
                compareByDescending<AccountFileItem> { it.availabilityStatus.ordinal }
                    .thenByDescending { it.dateAdded }
            )
        }
    }
    
    /**
     * Apply filters to a list of files
     */
    fun filterFiles(files: List<AccountFileItem>, filter: FileFilterCriteria): List<AccountFileItem> {
        return files.filter { file ->
            applyAllFilters(file, filter)
        }
    }
    
    /**
     * Sort and filter files in one operation (more efficient)
     */
    fun sortAndFilterFiles(
        files: List<AccountFileItem>,
        sortOption: FileEnhancedSortOption,
        filter: FileFilterCriteria
    ): List<AccountFileItem> {
        // Apply filters first to reduce the dataset size for sorting
        val filtered = if (filter.hasActiveFilters) {
            filterFiles(files, filter)
        } else {
            files
        }
        
        // Then sort the filtered results
        return sortFiles(filtered, sortOption)
    }
    
    /**
     * Get files by type category with sorting
     */
    fun getFilesByType(
        files: List<AccountFileItem>,
        category: FileTypeCategory,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC
    ): List<AccountFileItem> {
        val filtered = files.filter { it.fileTypeCategory == category }
        return sortFiles(filtered, sortOption)
    }
    
    /**
     * Get files by source with sorting
     */
    fun getFilesBySource(
        files: List<AccountFileItem>,
        source: FileSource,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC
    ): List<AccountFileItem> {
        val filtered = files.filter { it.source == source }
        return sortFiles(filtered, sortOption)
    }
    
    /**
     * Get files by availability status with sorting
     */
    fun getFilesByStatus(
        files: List<AccountFileItem>,
        status: FileAvailabilityStatus,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC
    ): List<AccountFileItem> {
        val filtered = files.filter { it.availabilityStatus == status }
        return sortFiles(filtered, sortOption)
    }
    
    /**
     * Search files with fuzzy matching and ranking
     */
    fun searchFiles(
        files: List<AccountFileItem>,
        query: String,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        useRanking: Boolean = true
    ): List<AccountFileItem> {
        if (query.isBlank()) return sortFiles(files, sortOption)
        
        val searchResults = files.mapNotNull { file ->
            val score = calculateSearchScore(file, query)
            if (score > 0) file to score else null
        }
        
        return if (useRanking) {
            // Sort by relevance score first, then by specified sort option
            searchResults.sortedWith(
                compareByDescending<Pair<AccountFileItem, Int>> { it.second }
                    .thenBy { getSortValue(it.first, sortOption) }
            ).map { it.first }
        } else {
            sortFiles(searchResults.map { it.first }, sortOption)
        }
    }
    
    /**
     * Get files within a size range
     */
    fun getFilesBySizeRange(
        files: List<AccountFileItem>,
        minSize: Long,
        maxSize: Long,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.SIZE_DESC
    ): List<AccountFileItem> {
        val filtered = files.filter { it.filesize in minSize..maxSize }
        return sortFiles(filtered, sortOption)
    }
    
    /**
     * Get files within a date range
     */
    fun getFilesByDateRange(
        files: List<AccountFileItem>,
        startDate: Date,
        endDate: Date,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC
    ): List<AccountFileItem> {
        val filtered = files.filter { file ->
            file.dateAdded >= startDate && file.dateAdded <= endDate
        }
        return sortFiles(filtered, sortOption)
    }
    
    /**
     * Get streamable files only
     */
    fun getStreamableFiles(
        files: List<AccountFileItem>,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC
    ): List<AccountFileItem> {
        val filtered = files.filter { it.isStreamable }
        return sortFiles(filtered, sortOption)
    }
    
    /**
     * Group files by type category
     */
    fun groupFilesByType(
        files: List<AccountFileItem>,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC
    ): Map<FileTypeCategory, List<AccountFileItem>> {
        return files.groupBy { it.fileTypeCategory }
            .mapValues { (_, filesInCategory) ->
                sortFiles(filesInCategory, sortOption)
            }
    }
    
    /**
     * Group files by source
     */
    fun groupFilesBySource(
        files: List<AccountFileItem>,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC
    ): Map<FileSource, List<AccountFileItem>> {
        return files.groupBy { it.source }
            .mapValues { (_, filesInSource) ->
                sortFiles(filesInSource, sortOption)
            }
    }
    
    /**
     * Get file statistics for filtering UI
     */
    fun getFilterStatistics(files: List<AccountFileItem>): FileFilterStatistics {
        val typeStats = files.groupBy { it.fileTypeCategory }
            .mapValues { (_, filesOfType) -> filesOfType.size }
        
        val sourceStats = files.groupBy { it.source }
            .mapValues { (_, filesOfSource) -> filesOfSource.size }
        
        val statusStats = files.groupBy { it.availabilityStatus }
            .mapValues { (_, filesOfStatus) -> filesOfStatus.size }
        
        val sizeStats = FileSizeStatistics(
            minSize = files.minOfOrNull { it.filesize } ?: 0L,
            maxSize = files.maxOfOrNull { it.filesize } ?: 0L,
            avgSize = if (files.isNotEmpty()) files.map { it.filesize }.average().toLong() else 0L,
            totalSize = files.sumOf { it.filesize }
        )
        
        val dateStats = FileDateStatistics(
            earliestDate = files.minOfOrNull { it.dateAdded } ?: Date(),
            latestDate = files.maxOfOrNull { it.dateAdded } ?: Date()
        )
        
        return FileFilterStatistics(
            totalFiles = files.size,
            typeStatistics = typeStats,
            sourceStatistics = sourceStats,
            statusStatistics = statusStats,
            sizeStatistics = sizeStats,
            dateStatistics = dateStats
        )
    }
    
    // Private helper methods
    
    private fun applyAllFilters(file: AccountFileItem, filter: FileFilterCriteria): Boolean {
        // Search query filter with fuzzy matching
        if (filter.searchQuery.isNotBlank()) {
            val score = calculateSearchScore(file, filter.searchQuery)
            if (score == 0) return false
        }
        
        // File type filter
        if (filter.fileTypes.isNotEmpty() && !filter.fileTypes.contains(file.fileTypeCategory)) {
            return false
        }
        
        // Source filter
        if (filter.sources.isNotEmpty() && !filter.sources.contains(file.source)) {
            return false
        }
        
        // Availability status filter
        if (filter.availabilityStatus.isNotEmpty() && !filter.availabilityStatus.contains(file.availabilityStatus)) {
            return false
        }
        
        // File size filters
        filter.minFileSize?.let { minSize ->
            if (file.filesize < minSize) return false
        }
        
        filter.maxFileSize?.let { maxSize ->
            if (file.filesize > maxSize) return false
        }
        
        // Date filters
        filter.dateAfter?.let { afterDate ->
            if (file.dateAdded.before(afterDate)) return false
        }
        
        filter.dateBefore?.let { beforeDate ->
            if (file.dateAdded.after(beforeDate)) return false
        }
        
        // Streamable filter
        if (filter.isStreamableOnly && !file.isStreamable) {
            return false
        }
        
        return true
    }
    
    private fun calculateSearchScore(file: AccountFileItem, query: String): Int {
        val filename = file.filename.lowercase()
        val searchQuery = query.lowercase()
        
        var score = 0
        
        // Exact match gets highest score
        if (filename == searchQuery) {
            score += 100
        }
        
        // Starts with query gets high score
        if (filename.startsWith(searchQuery)) {
            score += 50
        }
        
        // Contains query gets medium score
        if (filename.contains(searchQuery)) {
            score += 25
        }
        
        // Word boundary matches get bonus score
        val words = filename.split(" ", ".", "_", "-")
        words.forEach { word ->
            if (word.startsWith(searchQuery)) {
                score += 10
            }
            if (word == searchQuery) {
                score += 20
            }
        }
        
        // File extension match gets bonus
        if (file.fileExtension.contains(searchQuery)) {
            score += 5
        }
        
        // MIME type match gets small bonus
        file.mimeType?.let { mimeType ->
            if (mimeType.lowercase().contains(searchQuery)) {
                score += 3
            }
        }
        
        return score
    }
    
    private fun getSortValue(file: AccountFileItem, sortOption: FileEnhancedSortOption): Comparable<Any> {
        return when (sortOption) {
            FileEnhancedSortOption.NAME_ASC, FileEnhancedSortOption.NAME_DESC -> file.filename.lowercase()
            FileEnhancedSortOption.SIZE_ASC, FileEnhancedSortOption.SIZE_DESC -> file.filesize
            FileEnhancedSortOption.DATE_ASC, FileEnhancedSortOption.DATE_DESC -> file.dateAdded
            FileEnhancedSortOption.TYPE_ASC, FileEnhancedSortOption.TYPE_DESC -> file.fileTypeCategory.displayName
            FileEnhancedSortOption.SOURCE_ASC, FileEnhancedSortOption.SOURCE_DESC -> file.source.ordinal
            FileEnhancedSortOption.STATUS_ASC, FileEnhancedSortOption.STATUS_DESC -> file.availabilityStatus.ordinal
        } as Comparable<Any>
    }
}

/**
 * Statistics for filter UI components
 */
data class FileFilterStatistics(
    val totalFiles: Int,
    val typeStatistics: Map<FileTypeCategory, Int>,
    val sourceStatistics: Map<FileSource, Int>,
    val statusStatistics: Map<FileAvailabilityStatus, Int>,
    val sizeStatistics: FileSizeStatistics,
    val dateStatistics: FileDateStatistics
)

data class FileSizeStatistics(
    val minSize: Long,
    val maxSize: Long,
    val avgSize: Long,
    val totalSize: Long
) {
    val formattedMinSize: String get() = formatBytes(minSize)
    val formattedMaxSize: String get() = formatBytes(maxSize)
    val formattedAvgSize: String get() = formatBytes(avgSize)
    val formattedTotalSize: String get() = formatBytes(totalSize)
    
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return if (unitIndex == 0) {
            "${size.toInt()} ${units[unitIndex]}"
        } else {
            "${"%.1f".format(size)} ${units[unitIndex]}"
        }
    }
}

data class FileDateStatistics(
    val earliestDate: Date,
    val latestDate: Date
)