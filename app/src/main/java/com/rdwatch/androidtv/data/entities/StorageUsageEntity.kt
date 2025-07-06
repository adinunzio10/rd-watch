package com.rdwatch.androidtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity representing storage usage statistics from Real-Debrid account
 * 
 * This is a cache table that typically contains a single row with current
 * storage usage information to avoid repeated API calls.
 */
@Entity(
    tableName = "storage_usage",
    indices = [
        Index(value = ["lastUpdated"])
    ]
)
data class StorageUsageEntity(
    @PrimaryKey
    val id: String = "current", // Single row cache
    
    val totalSpaceBytes: Long = 0L,
    val usedSpaceBytes: Long = 0L,
    val freeSpaceBytes: Long = 0L,
    val fileCount: Int = 0,
    val torrentCount: Int = 0,
    val downloadCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    
    /**
     * Calculates the percentage of used space
     */
    val usedSpacePercentage: Float
        get() = if (totalSpaceBytes > 0) {
            (usedSpaceBytes.toFloat() / totalSpaceBytes.toFloat()) * 100f
        } else 0f
    
    /**
     * Converts entity to UI model
     */
    fun toStorageUsageInfo() = com.rdwatch.androidtv.ui.filebrowser.models.StorageUsageInfo(
        totalSpaceBytes = totalSpaceBytes,
        usedSpaceBytes = usedSpaceBytes,
        freeSpaceBytes = freeSpaceBytes,
        fileCount = fileCount,
        torrentCount = torrentCount,
        downloadCount = downloadCount
    )
    
    /**
     * Checks if the cached storage data is still fresh (within TTL)
     */
    fun isFresh(ttlMillis: Long = 600_000): Boolean { // 10 minutes default TTL
        return System.currentTimeMillis() - lastUpdated < ttlMillis
    }
    
    /**
     * Formats bytes to human-readable string
     */
    fun formatBytes(bytes: Long): String {
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
         * Creates entity from UI model
         */
        fun fromStorageUsageInfo(info: com.rdwatch.androidtv.ui.filebrowser.models.StorageUsageInfo): StorageUsageEntity {
            return StorageUsageEntity(
                totalSpaceBytes = info.totalSpaceBytes,
                usedSpaceBytes = info.usedSpaceBytes,
                freeSpaceBytes = info.freeSpaceBytes,
                fileCount = info.fileCount,
                torrentCount = info.torrentCount,
                downloadCount = info.downloadCount
            )
        }
        
        /**
         * Creates an empty storage usage entity
         */
        fun empty(): StorageUsageEntity {
            return StorageUsageEntity(
                totalSpaceBytes = 0L,
                usedSpaceBytes = 0L,
                freeSpaceBytes = 0L,
                fileCount = 0,
                torrentCount = 0,
                downloadCount = 0
            )
        }
    }
}