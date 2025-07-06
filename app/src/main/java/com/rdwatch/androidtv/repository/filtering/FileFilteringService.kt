package com.rdwatch.androidtv.repository.filtering

import com.rdwatch.androidtv.ui.filebrowser.models.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced filtering service for account files with predefined filters and custom criteria
 */
@Singleton
class FileFilteringService @Inject constructor() {
    
    /**
     * Predefined filter presets for common use cases
     */
    enum class FilterPreset(val displayName: String, val criteria: FileFilterCriteria) {
        ALL_FILES("All Files", FileFilterCriteria()),
        VIDEOS_ONLY("Videos Only", FileFilterCriteria(fileTypes = setOf(FileTypeCategory.VIDEO))),
        AUDIO_ONLY("Audio Only", FileFilterCriteria(fileTypes = setOf(FileTypeCategory.AUDIO))),
        STREAMABLE_ONLY("Streamable Only", FileFilterCriteria(isStreamableOnly = true)),
        RECENT_FILES("Recent Files", FileFilterCriteria(dateAfter = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)))),
        LARGE_FILES("Large Files (>1GB)", FileFilterCriteria(minFileSize = 1024L * 1024L * 1024L)),
        DOWNLOADS_ONLY("Downloads Only", FileFilterCriteria(sources = setOf(FileSource.DOWNLOAD))),
        TORRENTS_ONLY("Torrents Only", FileFilterCriteria(sources = setOf(FileSource.TORRENT))),
        READY_FILES("Ready Files", FileFilterCriteria(availabilityStatus = setOf(FileAvailabilityStatus.READY))),
        DOWNLOADING_FILES("Downloading", FileFilterCriteria(availabilityStatus = setOf(FileAvailabilityStatus.DOWNLOADING)))
    }
    
    /**
     * Apply a filter preset to files
     */
    fun applyFilterPreset(files: List<AccountFileItem>, preset: FilterPreset): List<AccountFileItem> {
        return applyFilter(files, preset.criteria)
    }
    
    /**
     * Apply custom filter criteria to files
     */
    fun applyFilter(files: List<AccountFileItem>, criteria: FileFilterCriteria): List<AccountFileItem> {
        return files.filter { file ->
            matchesAllCriteria(file, criteria)
        }
    }
    
    /**
     * Create a compound filter that combines multiple criteria with AND logic
     */
    fun createAndFilter(vararg criteria: FileFilterCriteria): FileFilterCriteria {
        val combinedTypes = criteria.flatMap { it.fileTypes }.toSet()
        val combinedSources = criteria.flatMap { it.sources }.toSet()
        val combinedStatuses = criteria.flatMap { it.availabilityStatus }.toSet()
        val combinedQueries = criteria.map { it.searchQuery }.filter { it.isNotBlank() }.joinToString(" ")
        
        val minSize = criteria.mapNotNull { it.minFileSize }.maxOrNull()
        val maxSize = criteria.mapNotNull { it.maxFileSize }.minOrNull()
        val latestDateAfter = criteria.mapNotNull { it.dateAfter }.maxOrNull()
        val earliestDateBefore = criteria.mapNotNull { it.dateBefore }.minOrNull()
        
        val isStreamableOnly = criteria.any { it.isStreamableOnly }
        
        return FileFilterCriteria(
            searchQuery = combinedQueries,
            fileTypes = combinedTypes,
            sources = combinedSources,
            availabilityStatus = combinedStatuses,
            minFileSize = minSize,
            maxFileSize = maxSize,
            dateAfter = latestDateAfter,
            dateBefore = earliestDateBefore,
            isStreamableOnly = isStreamableOnly
        )
    }
    
    /**
     * Create a compound filter that combines multiple criteria with OR logic for types
     */
    fun createOrFilter(vararg criteria: FileFilterCriteria): FileFilterCriteria {
        // For OR logic, we take the union of types, sources, and statuses
        val allTypes = criteria.flatMap { it.fileTypes }.toSet()
        val allSources = criteria.flatMap { it.sources }.toSet()
        val allStatuses = criteria.flatMap { it.availabilityStatus }.toSet()
        
        // For other criteria, we take the most permissive values
        val minSize = criteria.mapNotNull { it.minFileSize }.minOrNull()
        val maxSize = criteria.mapNotNull { it.maxFileSize }.maxOrNull()
        val earliestDateAfter = criteria.mapNotNull { it.dateAfter }.minOrNull()
        val latestDateBefore = criteria.mapNotNull { it.dateBefore }.maxOrNull()
        
        val anyStreamableOnly = criteria.any { it.isStreamableOnly }
        
        return FileFilterCriteria(
            searchQuery = criteria.firstOrNull { it.searchQuery.isNotBlank() }?.searchQuery ?: "",
            fileTypes = allTypes,
            sources = allSources,
            availabilityStatus = allStatuses,
            minFileSize = minSize,
            maxFileSize = maxSize,
            dateAfter = earliestDateAfter,
            dateBefore = latestDateBefore,
            isStreamableOnly = anyStreamableOnly
        )
    }
    
    /**
     * Filter files by file extension
     */
    fun filterByExtensions(files: List<AccountFileItem>, extensions: Set<String>): List<AccountFileItem> {
        val normalizedExtensions = extensions.map { it.lowercase().removePrefix(".") }.toSet()
        return files.filter { file ->
            file.fileExtension in normalizedExtensions
        }
    }
    
    /**
     * Filter files by filename patterns using regex
     */
    fun filterByPattern(files: List<AccountFileItem>, pattern: String, ignoreCase: Boolean = true): List<AccountFileItem> {
        return try {
            val regex = if (ignoreCase) {
                Regex(pattern, RegexOption.IGNORE_CASE)
            } else {
                Regex(pattern)
            }
            files.filter { file ->
                regex.containsMatchIn(file.filename)
            }
        } catch (e: Exception) {
            // If regex is invalid, fall back to simple contains check
            files.filter { file ->
                file.filename.contains(pattern, ignoreCase = ignoreCase)
            }
        }
    }
    
    /**
     * Filter files by host
     */
    fun filterByHost(files: List<AccountFileItem>, hosts: Set<String>): List<AccountFileItem> {
        return files.filter { file ->
            file.host?.let { host ->
                hosts.any { it.equals(host, ignoreCase = true) }
            } ?: false
        }
    }
    
    /**
     * Filter files by torrent status (for torrent files only)
     */
    fun filterByTorrentStatus(files: List<AccountFileItem>, statuses: Set<String>): List<AccountFileItem> {
        return files.filter { file ->
            file.source == FileSource.TORRENT && file.torrentStatus in statuses
        }
    }
    
    /**
     * Filter files by progress range (for torrent files only)
     */
    fun filterByProgressRange(files: List<AccountFileItem>, minProgress: Float, maxProgress: Float): List<AccountFileItem> {
        return files.filter { file ->
            file.source == FileSource.TORRENT && 
            file.torrentProgress?.let { progress ->
                progress >= minProgress && progress <= maxProgress
            } ?: false
        }
    }
    
    /**
     * Create size-based filter presets
     */
    fun createSizeFilter(sizeCategory: SizeCategory): FileFilterCriteria {
        return when (sizeCategory) {
            SizeCategory.TINY -> FileFilterCriteria(maxFileSize = 10L * 1024L * 1024L) // < 10 MB
            SizeCategory.SMALL -> FileFilterCriteria(
                minFileSize = 10L * 1024L * 1024L,
                maxFileSize = 100L * 1024L * 1024L
            ) // 10 MB - 100 MB
            SizeCategory.MEDIUM -> FileFilterCriteria(
                minFileSize = 100L * 1024L * 1024L,
                maxFileSize = 1024L * 1024L * 1024L
            ) // 100 MB - 1 GB
            SizeCategory.LARGE -> FileFilterCriteria(
                minFileSize = 1024L * 1024L * 1024L,
                maxFileSize = 5L * 1024L * 1024L * 1024L
            ) // 1 GB - 5 GB
            SizeCategory.HUGE -> FileFilterCriteria(minFileSize = 5L * 1024L * 1024L * 1024L) // > 5 GB
        }
    }
    
    /**
     * Create date-based filter presets
     */
    fun createDateFilter(dateRange: DateRange): FileFilterCriteria {
        val now = System.currentTimeMillis()
        return when (dateRange) {
            DateRange.TODAY -> FileFilterCriteria(
                dateAfter = Date(now - TimeUnit.DAYS.toMillis(1))
            )
            DateRange.THIS_WEEK -> FileFilterCriteria(
                dateAfter = Date(now - TimeUnit.DAYS.toMillis(7))
            )
            DateRange.THIS_MONTH -> FileFilterCriteria(
                dateAfter = Date(now - TimeUnit.DAYS.toMillis(30))
            )
            DateRange.THIS_YEAR -> FileFilterCriteria(
                dateAfter = Date(now - TimeUnit.DAYS.toMillis(365))
            )
            DateRange.OLDER -> FileFilterCriteria(
                dateBefore = Date(now - TimeUnit.DAYS.toMillis(365))
            )
        }
    }
    
    /**
     * Get files that don't match any filter (complement)
     */
    fun getComplementFiles(allFiles: List<AccountFileItem>, filteredFiles: List<AccountFileItem>): List<AccountFileItem> {
        val filteredIds = filteredFiles.map { it.id }.toSet()
        return allFiles.filter { it.id !in filteredIds }
    }
    
    /**
     * Get quick filter suggestions based on existing files
     */
    fun getFilterSuggestions(files: List<AccountFileItem>): FilterSuggestions {
        val commonExtensions = files.groupBy { it.fileExtension }
            .filter { (_, filesWithExt) -> filesWithExt.size >= 3 }
            .keys.take(10)
        
        val commonHosts = files.mapNotNull { it.host }
            .groupBy { it }
            .filter { (_, filesWithHost) -> filesWithHost.size >= 2 }
            .keys.take(5)
        
        val sizeRanges = listOf(
            "Small (< 100MB)" to files.count { it.filesize < 100L * 1024L * 1024L },
            "Medium (100MB - 1GB)" to files.count { it.filesize in (100L * 1024L * 1024L)..(1024L * 1024L * 1024L) },
            "Large (> 1GB)" to files.count { it.filesize > 1024L * 1024L * 1024L }
        ).filter { it.second > 0 }
        
        return FilterSuggestions(
            commonExtensions = commonExtensions,
            commonHosts = commonHosts,
            sizeRanges = sizeRanges.map { it.first }
        )
    }
    
    // Private helper methods
    
    private fun matchesAllCriteria(file: AccountFileItem, criteria: FileFilterCriteria): Boolean {
        // Search query filter
        if (criteria.searchQuery.isNotBlank()) {
            val query = criteria.searchQuery.lowercase()
            val filename = file.filename.lowercase()
            val parentName = file.parentTorrentName?.lowercase() ?: ""
            
            if (!filename.contains(query) && !parentName.contains(query)) {
                return false
            }
        }
        
        // File type filter
        if (criteria.fileTypes.isNotEmpty() && !criteria.fileTypes.contains(file.fileTypeCategory)) {
            return false
        }
        
        // Source filter
        if (criteria.sources.isNotEmpty() && !criteria.sources.contains(file.source)) {
            return false
        }
        
        // Availability status filter
        if (criteria.availabilityStatus.isNotEmpty() && !criteria.availabilityStatus.contains(file.availabilityStatus)) {
            return false
        }
        
        // File size filters
        criteria.minFileSize?.let { minSize ->
            if (file.filesize < minSize) return false
        }
        
        criteria.maxFileSize?.let { maxSize ->
            if (file.filesize > maxSize) return false
        }
        
        // Date filters
        criteria.dateAfter?.let { afterDate ->
            if (file.dateAdded.before(afterDate)) return false
        }
        
        criteria.dateBefore?.let { beforeDate ->
            if (file.dateAdded.after(beforeDate)) return false
        }
        
        // Streamable filter
        if (criteria.isStreamableOnly && !file.isStreamable) {
            return false
        }
        
        return true
    }
}

/**
 * Size categories for filtering
 */
enum class SizeCategory(val displayName: String) {
    TINY("Tiny (< 10MB)"),
    SMALL("Small (10MB - 100MB)"),
    MEDIUM("Medium (100MB - 1GB)"),
    LARGE("Large (1GB - 5GB)"),
    HUGE("Huge (> 5GB)")
}

/**
 * Date range categories for filtering
 */
enum class DateRange(val displayName: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    OLDER("Older than 1 Year")
}

/**
 * Filter suggestions based on file analysis
 */
data class FilterSuggestions(
    val commonExtensions: List<String>,
    val commonHosts: List<String>,
    val sizeRanges: List<String>
)