package com.rdwatch.androidtv.repository.bulk

import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.min

/**
 * Utility functions and extensions for bulk operations
 */
object BulkOperationsUtils {
    
    /**
     * Validate files for a specific operation type
     */
    fun validateFilesForOperation(
        files: List<AccountFileItem>,
        operationType: BulkOperationType
    ): ValidationResult {
        if (files.isEmpty()) {
            return ValidationResult.Invalid("No files selected")
        }
        
        return when (operationType) {
            BulkOperationType.DELETE -> {
                // All files can be deleted
                ValidationResult.Valid
            }
            BulkOperationType.DOWNLOAD -> {
                // All files can be downloaded
                ValidationResult.Valid
            }
            BulkOperationType.PLAY -> {
                val playableFiles = files.filter { it.isPlayableFile && it.isStreamable }
                if (playableFiles.isEmpty()) {
                    ValidationResult.Invalid("No playable files selected")
                } else if (playableFiles.size < files.size) {
                    ValidationResult.Warning(
                        "Only ${playableFiles.size} of ${files.size} files are playable",
                        playableFiles
                    )
                } else {
                    ValidationResult.Valid
                }
            }
            BulkOperationType.ADD_TO_FAVORITES -> {
                // All files can be added to favorites
                ValidationResult.Valid
            }
        }
    }
    
    /**
     * Calculate optimal batch size for operations based on operation type and network conditions
     */
    fun calculateOptimalBatchSize(
        operationType: BulkOperationType,
        totalFiles: Int,
        networkCondition: NetworkCondition = NetworkCondition.GOOD
    ): Int {
        val baseSize = when (operationType) {
            BulkOperationType.DELETE -> 5 // Conservative for API calls
            BulkOperationType.DOWNLOAD -> 3 // More conservative due to potential large responses
            BulkOperationType.PLAY -> 2 // Most conservative due to streaming URL generation
            BulkOperationType.ADD_TO_FAVORITES -> 10 // Fastest operation
        }
        
        val networkMultiplier = when (networkCondition) {
            NetworkCondition.POOR -> 0.5f
            NetworkCondition.FAIR -> 0.75f
            NetworkCondition.GOOD -> 1.0f
            NetworkCondition.EXCELLENT -> 1.5f
        }
        
        val adjustedSize = (baseSize * networkMultiplier).toInt().coerceAtLeast(1)
        return min(adjustedSize, totalFiles)
    }
    
    /**
     * Estimate operation duration based on file count and operation type
     */
    fun estimateOperationDuration(
        files: List<AccountFileItem>,
        operationType: BulkOperationType
    ): OperationDurationEstimate {
        val baseTimePerFileMs = when (operationType) {
            BulkOperationType.DELETE -> 500L // 0.5 seconds per file
            BulkOperationType.DOWNLOAD -> 1000L // 1 second per file (URL generation)
            BulkOperationType.PLAY -> 1500L // 1.5 seconds per file (streaming URL)
            BulkOperationType.ADD_TO_FAVORITES -> 200L // 0.2 seconds per file
        }
        
        val totalEstimatedMs = files.size * baseTimePerFileMs
        val minEstimateMs = (totalEstimatedMs * 0.7).toLong() // -30% for best case
        val maxEstimateMs = (totalEstimatedMs * 1.5).toLong() // +50% for worst case
        
        return OperationDurationEstimate(
            estimatedMs = totalEstimatedMs,
            minEstimateMs = minEstimateMs,
            maxEstimateMs = maxEstimateMs,
            formattedEstimate = formatDuration(totalEstimatedMs)
        )
    }
    
    /**
     * Group files by source for optimized batch processing
     */
    fun groupFilesBySource(files: List<AccountFileItem>): Map<FileSource, List<AccountFileItem>> {
        return files.groupBy { it.source }
    }
    
    /**
     * Prioritize files for processing (e.g., smaller files first for faster feedback)
     */
    fun prioritizeFiles(
        files: List<AccountFileItem>,
        strategy: PrioritizationStrategy
    ): List<AccountFileItem> {
        return when (strategy) {
            PrioritizationStrategy.SMALLEST_FIRST -> files.sortedBy { it.filesize }
            PrioritizationStrategy.LARGEST_FIRST -> files.sortedByDescending { it.filesize }
            PrioritizationStrategy.NEWEST_FIRST -> files.sortedByDescending { it.dateAdded }
            PrioritizationStrategy.OLDEST_FIRST -> files.sortedBy { it.dateAdded }
            PrioritizationStrategy.ALPHABETICAL -> files.sortedBy { it.filename }
            PrioritizationStrategy.BY_TYPE -> files.sortedBy { it.fileTypeCategory.ordinal }
            PrioritizationStrategy.STREAMABLE_FIRST -> files.sortedByDescending { it.isStreamable }
            PrioritizationStrategy.ORIGINAL_ORDER -> files
        }
    }
    
    /**
     * Create a progress flow that emits at regular intervals for UI updates
     */
    fun createProgressFlow(
        totalItems: Int,
        durationMs: Long,
        updateIntervalMs: Long = 100L
    ): Flow<SimulatedProgress> = flow {
        val updates = (durationMs / updateIntervalMs).toInt()
        val itemsPerUpdate = totalItems.toFloat() / updates
        
        var completedItems = 0f
        
        repeat(updates) { index ->
            delay(updateIntervalMs)
            completedItems += itemsPerUpdate
            
            val progress = SimulatedProgress(
                completedItems = completedItems.toInt().coerceAtMost(totalItems),
                totalItems = totalItems,
                progressPercentage = ((completedItems / totalItems) * 100f).coerceAtMost(100f),
                isCompleted = index == updates - 1
            )
            
            emit(progress)
        }
    }
    
    /**
     * Check if operation should be throttled based on recent activity
     */
    fun shouldThrottleOperation(
        recentOperationCount: Int,
        timeWindowMinutes: Int = 5
    ): Boolean {
        val maxOperationsPerWindow = when (timeWindowMinutes) {
            1 -> 5
            5 -> 20
            15 -> 50
            else -> 100
        }
        
        return recentOperationCount >= maxOperationsPerWindow
    }
    
    /**
     * Format file list for operation summary
     */
    fun formatOperationSummary(
        files: List<AccountFileItem>,
        operationType: BulkOperationType
    ): OperationSummary {
        val typeBreakdown = files.groupBy { it.fileTypeCategory }
            .mapValues { (_, filesOfType) -> filesOfType.size }
        
        val sourceBreakdown = files.groupBy { it.source }
            .mapValues { (_, filesOfSource) -> filesOfSource.size }
        
        val totalSize = files.sumOf { it.filesize }
        val streamableCount = files.count { it.isStreamable }
        
        return OperationSummary(
            operationType = operationType,
            totalFiles = files.size,
            totalSize = totalSize,
            formattedTotalSize = formatFileSize(totalSize),
            typeBreakdown = typeBreakdown,
            sourceBreakdown = sourceBreakdown,
            streamableCount = streamableCount,
            estimatedDuration = estimateOperationDuration(files, operationType)
        )
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
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

/**
 * Validation result for operation
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
    data class Warning(val message: String, val validFiles: List<AccountFileItem>) : ValidationResult()
}

/**
 * Network condition for operation optimization
 */
enum class NetworkCondition {
    POOR, FAIR, GOOD, EXCELLENT
}

/**
 * Duration estimate for operation
 */
data class OperationDurationEstimate(
    val estimatedMs: Long,
    val minEstimateMs: Long,
    val maxEstimateMs: Long,
    val formattedEstimate: String
)

/**
 * Prioritization strategies for file processing
 */
enum class PrioritizationStrategy(val displayName: String) {
    SMALLEST_FIRST("Smallest Files First"),
    LARGEST_FIRST("Largest Files First"),
    NEWEST_FIRST("Newest Files First"),
    OLDEST_FIRST("Oldest Files First"),
    ALPHABETICAL("Alphabetical Order"),
    BY_TYPE("By File Type"),
    STREAMABLE_FIRST("Streamable Files First"),
    ORIGINAL_ORDER("Original Order")
}

/**
 * Simulated progress for UI testing
 */
data class SimulatedProgress(
    val completedItems: Int,
    val totalItems: Int,
    val progressPercentage: Float,
    val isCompleted: Boolean
)

/**
 * Operation summary for UI display
 */
data class OperationSummary(
    val operationType: BulkOperationType,
    val totalFiles: Int,
    val totalSize: Long,
    val formattedTotalSize: String,
    val typeBreakdown: Map<FileTypeCategory, Int>,
    val sourceBreakdown: Map<FileSource, Int>,
    val streamableCount: Int,
    val estimatedDuration: OperationDurationEstimate
)