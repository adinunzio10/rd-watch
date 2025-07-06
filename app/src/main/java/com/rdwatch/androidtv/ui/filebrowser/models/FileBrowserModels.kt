package com.rdwatch.androidtv.ui.filebrowser.models

import com.rdwatch.androidtv.network.models.TorrentFile
import java.util.Date

/**
 * Enhanced UI Model for the Direct Account File Browser
 * 
 * Supports both individual downloads and torrent files
 */
data class AccountFileItem(
    val id: String,
    val filename: String,
    val filesize: Long,
    val source: FileSource,
    val mimeType: String? = null,
    val downloadUrl: String? = null,
    val streamUrl: String? = null,
    val host: String? = null,
    val dateAdded: Date,
    val isStreamable: Boolean = false,
    
    // Torrent-specific fields
    val parentTorrentId: String? = null,
    val parentTorrentName: String? = null,
    val torrentProgress: Float? = null,
    val torrentStatus: String? = null,
    
    // Additional metadata
    val isSelected: Boolean = false,
    val alternativeUrls: List<String> = emptyList()
) {
    /**
     * Gets the file extension from the filename
     */
    val fileExtension: String
        get() = filename.substringAfterLast('.', "").lowercase()
    
    /**
     * Gets the file type category based on extension and MIME type
     */
    val fileTypeCategory: FileTypeCategory
        get() = determineFileType()
    
    /**
     * Gets a formatted string for the file size
     */
    val formattedFileSize: String
        get() = formatFileSize(filesize)
    
    /**
     * Checks if the file is playable (video or audio)
     */
    val isPlayableFile: Boolean
        get() = fileTypeCategory in listOf(FileTypeCategory.VIDEO, FileTypeCategory.AUDIO)
    
    /**
     * Gets display name for the file source
     */
    val sourceDisplayName: String
        get() = when (source) {
            FileSource.DOWNLOAD -> "Download"
            FileSource.TORRENT -> "Torrent"
        }
    
    /**
     * Gets the effective availability status
     */
    val availabilityStatus: FileAvailabilityStatus
        get() = when {
            source == FileSource.DOWNLOAD -> FileAvailabilityStatus.READY
            source == FileSource.TORRENT && torrentStatus == "downloaded" -> FileAvailabilityStatus.READY
            source == FileSource.TORRENT && torrentStatus in listOf("downloading", "queued") -> FileAvailabilityStatus.DOWNLOADING
            else -> FileAvailabilityStatus.UNAVAILABLE
        }
    
    private fun determineFileType(): FileTypeCategory {
        // First check MIME type if available
        mimeType?.let { mime ->
            when {
                mime.startsWith("video/") -> return FileTypeCategory.VIDEO
                mime.startsWith("audio/") -> return FileTypeCategory.AUDIO
                mime.startsWith("image/") -> return FileTypeCategory.IMAGE
                mime.startsWith("text/") -> return FileTypeCategory.TEXT
                mime.contains("zip") || mime.contains("archive") -> return FileTypeCategory.ARCHIVE
            }
        }
        
        // Fall back to extension-based detection
        return when (fileExtension) {
            // Video extensions
            in VIDEO_EXTENSIONS -> FileTypeCategory.VIDEO
            // Audio extensions  
            in AUDIO_EXTENSIONS -> FileTypeCategory.AUDIO
            // Image extensions
            in IMAGE_EXTENSIONS -> FileTypeCategory.IMAGE
            // Text/Document extensions
            in TEXT_EXTENSIONS -> FileTypeCategory.TEXT
            // Archive extensions
            in ARCHIVE_EXTENSIONS -> FileTypeCategory.ARCHIVE
            // Subtitle extensions
            in SUBTITLE_EXTENSIONS -> FileTypeCategory.SUBTITLE
            else -> FileTypeCategory.OTHER
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
    
    companion object {
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", 
            "mpg", "mpeg", "ts", "m2ts", "vob", "ogv", "divx", "xvid"
        )
        
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "aac", "ogg", "wma", "m4a", "opus", "ape", 
            "ac3", "dts", "mka", "aiff", "au", "ra", "amr"
        )
        
        private val IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "svg", 
            "ico", "psd", "raw", "cr2", "nef", "arw"
        )
        
        private val TEXT_EXTENSIONS = setOf(
            "txt", "md", "pdf", "doc", "docx", "rtf", "odt", "pages", "tex", 
            "html", "htm", "xml", "json", "csv", "log", "nfo", "readme"
        )
        
        private val ARCHIVE_EXTENSIONS = setOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "z", "lz", "lzma",
            "cab", "iso", "dmg", "pkg", "deb", "rpm"
        )
        
        private val SUBTITLE_EXTENSIONS = setOf(
            "srt", "vtt", "ass", "ssa", "sub", "idx", "sup", "smi", "sami"
        )
    }
}

/**
 * Source of the file (direct download or from torrent)
 */
enum class FileSource {
    DOWNLOAD,
    TORRENT
}

/**
 * Enhanced file type categories with more granular classification
 */
enum class FileTypeCategory(val displayName: String, val iconName: String) {
    VIDEO("Video", "play_circle"),
    AUDIO("Audio", "music_note"),
    IMAGE("Image", "image"),
    TEXT("Document", "description"),
    ARCHIVE("Archive", "archive"),
    SUBTITLE("Subtitle", "closed_caption"),
    OTHER("Other", "insert_drive_file")
}

/**
 * File availability status
 */
enum class FileAvailabilityStatus(val displayName: String) {
    READY("Ready"),
    DOWNLOADING("Downloading"),
    UNAVAILABLE("Unavailable")
}

/**
 * Enhanced sort options with more categories
 */
enum class FileEnhancedSortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_ASC("Size (Smallest)"),
    SIZE_DESC("Size (Largest)"),
    DATE_ASC("Date (Oldest)"),
    DATE_DESC("Date (Newest)"),
    TYPE_ASC("Type (A-Z)"),
    TYPE_DESC("Type (Z-A)"),
    SOURCE_ASC("Source (Downloads First)"),
    SOURCE_DESC("Source (Torrents First)"),
    STATUS_ASC("Status (Ready First)"),
    STATUS_DESC("Status (Downloading First)")
}

/**
 * File filter criteria for advanced filtering
 */
data class FileFilterCriteria(
    val searchQuery: String = "",
    val fileTypes: Set<FileTypeCategory> = emptySet(),
    val sources: Set<FileSource> = emptySet(),
    val availabilityStatus: Set<FileAvailabilityStatus> = emptySet(),
    val minFileSize: Long? = null,
    val maxFileSize: Long? = null,
    val dateAfter: Date? = null,
    val dateBefore: Date? = null,
    val isStreamableOnly: Boolean = false
) {
    /**
     * Checks if any filters are active
     */
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
                fileTypes.isNotEmpty() ||
                sources.isNotEmpty() ||
                availabilityStatus.isNotEmpty() ||
                minFileSize != null ||
                maxFileSize != null ||
                dateAfter != null ||
                dateBefore != null ||
                isStreamableOnly
}

/**
 * Storage usage information from Real Debrid user endpoint
 */
data class StorageUsageInfo(
    val totalSpaceBytes: Long = 0L,
    val usedSpaceBytes: Long = 0L,
    val freeSpaceBytes: Long = 0L,
    val fileCount: Int = 0,
    val torrentCount: Int = 0,
    val downloadCount: Int = 0
) {
    val usedSpacePercentage: Float
        get() = if (totalSpaceBytes > 0) {
            (usedSpaceBytes.toFloat() / totalSpaceBytes.toFloat()) * 100f
        } else 0f
    
    val formattedTotalSpace: String
        get() = formatBytes(totalSpaceBytes)
    
    val formattedUsedSpace: String
        get() = formatBytes(usedSpaceBytes)
    
    val formattedFreeSpace: String
        get() = formatBytes(freeSpaceBytes)
    
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

/**
 * Bulk operation types
 */
enum class BulkOperationType(val displayName: String) {
    DELETE("Delete Selected"),
    DOWNLOAD("Download Selected"),
    PLAY("Play Selected"),
    ADD_TO_FAVORITES("Add to Favorites")
}

/**
 * Result of a bulk operation
 */
data class BulkOperationResult(
    val operationType: BulkOperationType,
    val totalItems: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String> = emptyList()
) {
    val isPartialSuccess: Boolean
        get() = successCount > 0 && failureCount > 0
    
    val isCompleteSuccess: Boolean
        get() = successCount == totalItems && failureCount == 0
    
    val isCompleteFailure: Boolean
        get() = successCount == 0 && failureCount == totalItems
}