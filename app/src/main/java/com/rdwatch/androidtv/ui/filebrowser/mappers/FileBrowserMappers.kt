package com.rdwatch.androidtv.ui.filebrowser.mappers

import com.rdwatch.androidtv.network.models.TorrentFile
import com.rdwatch.androidtv.network.models.TorrentInfo
import com.rdwatch.androidtv.ui.filebrowser.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Maps Real-Debrid TorrentInfo to FileBrowser FileItem.Torrent
 */
fun TorrentInfo.toFileItem(): FileItem.Torrent {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timestamp =
        try {
            dateFormat.parse(added)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

    return FileItem.Torrent(
        id = id,
        name = filename,
        size = bytes,
        modifiedDate = timestamp,
        hash = hash,
        progress = progress,
        status = TorrentStatus.fromString(status),
        seeders = seeders,
        speed = speed,
        files = emptyList(), // Files will be populated separately when torrent is expanded
    )
}

/**
 * Maps TorrentFile to FileItem.File
 */
fun TorrentFile.toFileItem(
    torrentId: String,
    baseUrl: String? = null,
): FileItem.File {
    val extension = path.substringAfterLast('.', "").lowercase()
    val fileType = FileType.fromExtension(extension)
    val isPlayable = fileType == FileType.VIDEO || fileType == FileType.AUDIO

    return FileItem.File(
        id = "$torrentId-$id",
        name = path.substringAfterLast('/'),
        size = bytes,
        modifiedDate = System.currentTimeMillis(), // Real-Debrid doesn't provide file timestamps
        mimeType = getMimeTypeFromExtension(extension),
        downloadUrl = baseUrl?.let { "$it/$id" },
        streamUrl = if (isPlayable) baseUrl?.let { "$it/$id" } else null,
        isPlayable = isPlayable,
        progress = if (selected == 1) 100f else null,
        status = if (selected == 1) FileStatus.READY else FileStatus.UNAVAILABLE,
    )
}

/**
 * Helper function to determine MIME type from file extension
 */
private fun getMimeTypeFromExtension(extension: String): String? {
    return when (extension.lowercase()) {
        // Video
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "wmv" -> "video/x-ms-wmv"
        "flv" -> "video/x-flv"
        "webm" -> "video/webm"
        "m4v" -> "video/x-m4v"
        "mpg", "mpeg" -> "video/mpeg"

        // Audio
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "ogg" -> "audio/ogg"
        "wma" -> "audio/x-ms-wma"
        "m4a" -> "audio/mp4"
        "opus" -> "audio/opus"

        // Document
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "txt" -> "text/plain"
        "odt" -> "application/vnd.oasis.opendocument.text"
        "rtf" -> "application/rtf"

        // Image
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        "webp" -> "image/webp"

        // Archive
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "7z" -> "application/x-7z-compressed"
        "tar" -> "application/x-tar"
        "gz" -> "application/gzip"
        "bz2" -> "application/x-bzip2"

        // Subtitle
        "srt" -> "application/x-subrip"
        "ass", "ssa" -> "text/x-ssa"
        "vtt" -> "text/vtt"
        "sub" -> "text/x-microdvd"

        else -> null
    }
}

/**
 * Extension function to apply sorting to a list of FileItems
 */
fun List<FileItem>.sortedByOptions(options: SortingOptions): List<FileItem> {
    val comparator =
        when (options.sortBy) {
            SortBy.NAME -> compareBy<FileItem> { it.name.lowercase() }
            SortBy.SIZE -> compareBy { it.size }
            SortBy.DATE -> compareBy { it.modifiedDate }
            SortBy.TYPE ->
                compareBy {
                    when (it) {
                        is FileItem.Folder -> 0
                        is FileItem.Torrent -> 1
                        is FileItem.File -> 2
                    }
                }
            SortBy.STATUS ->
                compareBy {
                    when (it) {
                        is FileItem.File -> it.status.ordinal
                        is FileItem.Torrent -> it.status.ordinal
                        is FileItem.Folder -> 0
                    }
                }
        }

    return if (options.sortOrder == SortOrder.DESCENDING) {
        sortedWith(comparator.reversed())
    } else {
        sortedWith(comparator)
    }
}

/**
 * Extension function to apply filters to a list of FileItems
 */
fun List<FileItem>.filteredByOptions(options: FilterOptions): List<FileItem> {
    return filter { item ->
        // Search query filter
        val matchesSearch =
            options.searchQuery.isBlank() ||
                item.name.contains(options.searchQuery, ignoreCase = true)

        // Type-specific filters
        val matchesTypeFilter =
            when (item) {
                is FileItem.File -> {
                    val passesPlayableFilter = !options.showOnlyPlayable || item.isPlayable
                    val passesStatusFilter =
                        options.statusFilter.isEmpty() ||
                            options.statusFilter.contains(item.status)
                    val passesFileTypeFilter =
                        if (options.fileTypeFilter.isNotEmpty()) {
                            val extension = item.name.substringAfterLast('.', "").lowercase()
                            val fileType = FileType.fromExtension(extension)
                            options.fileTypeFilter.contains(fileType)
                        } else {
                            true
                        }

                    passesPlayableFilter && passesStatusFilter && passesFileTypeFilter
                }
                is FileItem.Torrent -> {
                    val passesDownloadedFilter =
                        !options.showOnlyDownloaded ||
                            item.status == TorrentStatus.DOWNLOADED
                    passesDownloadedFilter
                }
                is FileItem.Folder -> true // Folders pass all filters
            }

        matchesSearch && matchesTypeFilter
    }
}

/**
 * Extension function to filter by date range
 */
fun List<FileItem>.filteredByDateRange(
    startDate: Long,
    endDate: Long,
): List<FileItem> {
    return filter { item ->
        item.modifiedDate in startDate..endDate
    }
}

/**
 * Extension function to filter by size range
 */
fun List<FileItem>.filteredBySizeRange(
    minSize: Long,
    maxSize: Long,
): List<FileItem> {
    return filter { item ->
        item.size in minSize..maxSize
    }
}

/**
 * Extension function to filter by multiple file types
 */
fun List<FileItem>.filteredByFileTypes(fileTypes: Set<FileType>): List<FileItem> {
    return filter { item ->
        when (item) {
            is FileItem.File -> {
                val extension = item.name.substringAfterLast('.', "").lowercase()
                val fileType = FileType.fromExtension(extension)
                fileTypes.contains(fileType)
            }
            is FileItem.Torrent -> {
                // For torrents, check if they contain files of the specified types
                item.files.any { file ->
                    val extension = file.name.substringAfterLast('.', "").lowercase()
                    val fileType = FileType.fromExtension(extension)
                    fileTypes.contains(fileType)
                }
            }
            is FileItem.Folder -> true // Always include folders
        }
    }
}

/**
 * Extension function to filter by playable content only
 */
fun List<FileItem>.filteredByPlayableOnly(): List<FileItem> {
    return filter { item ->
        when (item) {
            is FileItem.File -> item.isPlayable
            is FileItem.Torrent -> item.files.any { it.isPlayable }
            is FileItem.Folder -> true // Always include folders
        }
    }
}

/**
 * Extension function to get content statistics
 */
fun List<FileItem>.getContentStats(): ContentStats {
    var totalSize = 0L
    var fileCount = 0
    var folderCount = 0
    var torrentCount = 0
    var playableCount = 0

    forEach { item ->
        totalSize += item.size
        when (item) {
            is FileItem.File -> {
                fileCount++
                if (item.isPlayable) playableCount++
            }
            is FileItem.Folder -> folderCount++
            is FileItem.Torrent -> {
                torrentCount++
                if (item.files.any { it.isPlayable }) playableCount++
            }
        }
    }

    return ContentStats(
        totalSize = totalSize,
        fileCount = fileCount,
        folderCount = folderCount,
        torrentCount = torrentCount,
        playableCount = playableCount,
    )
}

/**
 * Data class for content statistics
 */
data class ContentStats(
    val totalSize: Long,
    val fileCount: Int,
    val folderCount: Int,
    val torrentCount: Int,
    val playableCount: Int,
)

/**
 * Extension function to get file type distribution
 */
fun List<FileItem>.getFileTypeDistribution(): Map<FileType, Int> {
    val distribution = mutableMapOf<FileType, Int>()

    forEach { item ->
        when (item) {
            is FileItem.File -> {
                val extension = item.name.substringAfterLast('.', "").lowercase()
                val fileType = FileType.fromExtension(extension)
                distribution[fileType] = distribution.getOrDefault(fileType, 0) + 1
            }
            is FileItem.Torrent -> {
                item.files.forEach { file ->
                    val extension = file.name.substringAfterLast('.', "").lowercase()
                    val fileType = FileType.fromExtension(extension)
                    distribution[fileType] = distribution.getOrDefault(fileType, 0) + 1
                }
            }
            is FileItem.Folder -> {
                // Folders don't contribute to file type distribution
            }
        }
    }

    return distribution
}
