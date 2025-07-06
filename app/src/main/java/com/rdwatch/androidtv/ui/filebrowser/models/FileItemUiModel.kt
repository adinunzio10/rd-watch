package com.rdwatch.androidtv.ui.filebrowser.models

import java.util.Date

/**
 * UI Model for a file item in the Real Debrid file browser
 */
data class FileItemUiModel(
    val id: String,
    val filename: String,
    val filesize: Long,
    val mimeType: String,
    val downloadUrl: String,
    val streamUrl: String? = null,
    val host: String,
    val dateAdded: Date,
    val isStreamable: Boolean = false
) {
    /**
     * Gets the file extension from the filename
     */
    val fileExtension: String
        get() = filename.substringAfterLast('.', "").lowercase()
    
    /**
     * Gets the file type category based on MIME type
     */
    val fileTypeCategory: FileTypeCategory
        get() = when {
            mimeType.startsWith("video/") -> FileTypeCategory.VIDEO
            mimeType.startsWith("audio/") -> FileTypeCategory.AUDIO
            mimeType.startsWith("image/") -> FileTypeCategory.IMAGE
            mimeType.startsWith("text/") -> FileTypeCategory.TEXT
            mimeType.contains("zip") || mimeType.contains("archive") -> FileTypeCategory.ARCHIVE
            else -> FileTypeCategory.OTHER
        }
    
    /**
     * Gets a formatted string for the file size
     */
    val formattedFileSize: String
        get() = formatFileSize(filesize)
    
    /**
     * Checks if the file is a video file
     */
    val isVideoFile: Boolean
        get() = fileTypeCategory == FileTypeCategory.VIDEO
    
    /**
     * Checks if the file is an audio file
     */
    val isAudioFile: Boolean
        get() = fileTypeCategory == FileTypeCategory.AUDIO
    
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

