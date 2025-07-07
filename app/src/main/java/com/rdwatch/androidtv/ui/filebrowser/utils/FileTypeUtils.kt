package com.rdwatch.androidtv.ui.filebrowser.utils

import com.rdwatch.androidtv.ui.filebrowser.models.FileType
import com.rdwatch.androidtv.ui.filebrowser.models.FileItem
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

/**
 * Utility class for file type detection and advanced filtering operations
 */
object FileTypeUtils {
    
    /**
     * Enhanced file type detection with additional video formats
     */
    fun detectFileType(fileName: String): FileType {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            // Video formats (comprehensive list)
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg",
            "3gp", "3g2", "asf", "divx", "dv", "f4v", "m2ts", "m4p", "m4v", "mts",
            "ogv", "qt", "rm", "rmvb", "ts", "vob", "xvid" -> FileType.VIDEO
            
            // Audio formats (comprehensive list)
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus", "ape", "ac3",
            "aiff", "amr", "au", "dts", "gsm", "m4b", "m4p", "m4r", "mka", "mpc",
            "ra", "shn", "tak", "tta", "voc", "vqf", "w64", "wv" -> FileType.AUDIO
            
            // Document formats
            "pdf", "doc", "docx", "txt", "odt", "rtf", "xls", "xlsx", "ppt", "pptx",
            "odp", "ods", "csv", "md", "html", "htm", "xml", "json", "epub", "mobi" -> FileType.DOCUMENT
            
            // Image formats
            "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "tiff", "tif", "ico",
            "psd", "ai", "eps", "raw", "cr2", "nef", "orf", "sr2", "rw2", "dng" -> FileType.IMAGE
            
            // Archive formats
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "lzma", "cab", "iso",
            "dmg", "deb", "rpm", "msi", "exe", "jar", "war", "ear" -> FileType.ARCHIVE
            
            // Subtitle formats
            "srt", "ass", "vtt", "sub", "ssa", "idx", "sup", "usf", "jss", "psb",
            "rt", "smi", "stl", "ttml", "dfxp", "sbv", "lrc" -> FileType.SUBTITLE
            
            else -> FileType.OTHER
        }
    }
    
    /**
     * Check if a file is playable based on its type
     */
    fun isPlayable(fileName: String): Boolean {
        val fileType = detectFileType(fileName)
        return fileType == FileType.VIDEO || fileType == FileType.AUDIO
    }
    
    /**
     * Get MIME type for a file
     */
    fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            // Video MIME types
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "m4v" -> "video/x-m4v"
            "mpg", "mpeg" -> "video/mpeg"
            "3gp" -> "video/3gpp"
            "ogv" -> "video/ogg"
            
            // Audio MIME types
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "wma" -> "audio/x-ms-wma"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            "ape" -> "audio/x-ape"
            "ac3" -> "audio/ac3"
            
            // Document MIME types
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt" -> "text/plain"
            "odt" -> "application/vnd.oasis.opendocument.text"
            "rtf" -> "application/rtf"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "html", "htm" -> "text/html"
            "xml" -> "application/xml"
            "json" -> "application/json"
            "csv" -> "text/csv"
            "md" -> "text/markdown"
            
            // Image MIME types
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            "tiff", "tif" -> "image/tiff"
            "ico" -> "image/x-icon"
            
            // Archive MIME types
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            "bz2" -> "application/x-bzip2"
            "xz" -> "application/x-xz"
            "iso" -> "application/x-iso9660-image"
            
            // Subtitle MIME types
            "srt" -> "application/x-subrip"
            "ass", "ssa" -> "text/x-ssa"
            "vtt" -> "text/vtt"
            "sub" -> "text/x-microdvd"
            
            else -> null
        }
    }
    
    /**
     * Format file size in human-readable format
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
    
    /**
     * Format duration in human-readable format
     */
    fun formatDuration(milliseconds: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(milliseconds)
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
    
    /**
     * Get file icon identifier based on file type
     */
    fun getFileIcon(fileType: FileType): String {
        return when (fileType) {
            FileType.VIDEO -> "video_file"
            FileType.AUDIO -> "audio_file"
            FileType.DOCUMENT -> "document_file"
            FileType.IMAGE -> "image_file"
            FileType.ARCHIVE -> "archive_file"
            FileType.SUBTITLE -> "subtitle_file"
            FileType.OTHER -> "unknown_file"
        }
    }
    
    /**
     * Check if two file types are compatible for batch operations
     */
    fun areCompatibleTypes(type1: FileType, type2: FileType): Boolean {
        return when {
            type1 == type2 -> true
            type1 == FileType.VIDEO && type2 == FileType.AUDIO -> true
            type1 == FileType.AUDIO && type2 == FileType.VIDEO -> true
            type1 == FileType.VIDEO && type2 == FileType.SUBTITLE -> true
            type1 == FileType.SUBTITLE && type2 == FileType.VIDEO -> true
            else -> false
        }
    }
    
    /**
     * Get suggested file types for filtering based on content
     */
    fun getSuggestedFileTypes(items: List<FileItem>): Set<FileType> {
        val typeDistribution = mutableMapOf<FileType, Int>()
        
        items.forEach { item ->
            when (item) {
                is FileItem.File -> {
                    val fileType = detectFileType(item.name)
                    typeDistribution[fileType] = typeDistribution.getOrDefault(fileType, 0) + 1
                }
                is FileItem.Torrent -> {
                    item.files.forEach { file ->
                        val fileType = detectFileType(file.name)
                        typeDistribution[fileType] = typeDistribution.getOrDefault(fileType, 0) + 1
                    }
                }
                is FileItem.Folder -> {
                    // Folders don't contribute to type distribution
                }
            }
        }
        
        // Return types that have at least 10% of total files or at least 5 files
        val totalFiles = typeDistribution.values.sum()
        val threshold = maxOf(totalFiles * 0.1, 5.0).toInt()
        
        return typeDistribution.filter { (_, count) -> count >= threshold }.keys
    }
    
    /**
     * Validate file extension against expected type
     */
    fun validateFileExtension(fileName: String, expectedType: FileType): Boolean {
        val actualType = detectFileType(fileName)
        return actualType == expectedType
    }
    
    /**
     * Get file quality indicator based on file name patterns
     */
    fun getQualityIndicator(fileName: String): String? {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.contains("4k") || lowerName.contains("2160p") -> "4K"
            lowerName.contains("1080p") -> "1080p"
            lowerName.contains("720p") -> "720p"
            lowerName.contains("480p") -> "480p"
            lowerName.contains("360p") -> "360p"
            lowerName.contains("hdr") -> "HDR"
            lowerName.contains("uhd") -> "UHD"
            lowerName.contains("bluray") || lowerName.contains("bd") -> "BluRay"
            lowerName.contains("webrip") -> "WebRip"
            lowerName.contains("webdl") -> "WebDL"
            lowerName.contains("dvdrip") -> "DVDRip"
            lowerName.contains("cam") -> "CAM"
            lowerName.contains("ts") -> "TS"
            else -> null
        }
    }
}