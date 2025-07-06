package com.rdwatch.androidtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity representing file type statistics for analytics and categorization
 * 
 * This table tracks aggregated statistics for different file types
 * to provide insights into storage usage patterns.
 */
@Entity(
    tableName = "file_type_stats",
    indices = [
        Index(value = ["fileType"])
    ]
)
data class FileTypeStatsEntity(
    @PrimaryKey
    val id: String, // Composite: "fileType_timestamp" or just "fileType" for current
    
    val fileType: String, // FileTypeCategory.name
    val fileCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    
    /**
     * Gets the average file size for this type
     */
    val averageFileSizeBytes: Long
        get() = if (fileCount > 0) totalSizeBytes / fileCount else 0L
    
    /**
     * Formats the total size to human-readable string
     */
    val formattedTotalSize: String
        get() = formatBytes(totalSizeBytes)
    
    /**
     * Formats the average file size to human-readable string
     */
    val formattedAverageSize: String
        get() = formatBytes(averageFileSizeBytes)
    
    /**
     * Checks if the statistics are still fresh (within TTL)
     */
    fun isFresh(ttlMillis: Long = 3600_000): Boolean { // 1 hour default TTL
        return System.currentTimeMillis() - lastUpdated < ttlMillis
    }
    
    /**
     * Creates a copy with updated statistics
     */
    fun withUpdatedStats(additionalFiles: Int, additionalSize: Long): FileTypeStatsEntity {
        return copy(
            fileCount = fileCount + additionalFiles,
            totalSizeBytes = totalSizeBytes + additionalSize,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Formats bytes to human-readable string
     */
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
    
    companion object {
        /**
         * Creates a new statistics entity for a file type
         */
        fun forFileType(fileType: String): FileTypeStatsEntity {
            return FileTypeStatsEntity(
                id = fileType,
                fileType = fileType,
                fileCount = 0,
                totalSizeBytes = 0L
            )
        }
        
        /**
         * Creates statistics entity from file type category
         */
        fun fromFileTypeCategory(category: com.rdwatch.androidtv.ui.filebrowser.models.FileTypeCategory): FileTypeStatsEntity {
            return FileTypeStatsEntity(
                id = category.name,
                fileType = category.name,
                fileCount = 0,
                totalSizeBytes = 0L
            )
        }
        
        /**
         * Creates statistics with initial values
         */
        fun withInitialStats(
            fileType: String,
            initialFileCount: Int,
            initialTotalSize: Long
        ): FileTypeStatsEntity {
            return FileTypeStatsEntity(
                id = fileType,
                fileType = fileType,
                fileCount = initialFileCount,
                totalSizeBytes = initialTotalSize
            )
        }
    }
}