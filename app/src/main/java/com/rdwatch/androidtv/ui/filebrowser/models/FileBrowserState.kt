package com.rdwatch.androidtv.ui.filebrowser.models

import com.rdwatch.androidtv.ui.common.UiState

/**
 * Represents the overall state of the file browser
 */
data class FileBrowserState(
    val contentState: UiState<List<FileItem>> = UiState.Loading,
    val sortingOptions: SortingOptions = SortingOptions(),
    val filterOptions: FilterOptions = FilterOptions(),
    val selectedItems: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val currentPath: String = "/",
    val navigationHistory: List<String> = listOf("/"),
    val accountType: AccountType = AccountType.REAL_DEBRID,
    val paginationState: PaginationState = PaginationState(),
    val viewMode: ViewMode = ViewMode.LIST,
    val bulkOperationState: BulkOperationState? = null,
    val refreshState: RefreshState? = null,
    val errorRecoveryState: RecoveryState? = null,
)

/**
 * Represents different account types supported by the file browser
 */
enum class AccountType(val displayName: String) {
    REAL_DEBRID("Real-Debrid"),
    PREMIUMIZE("Premiumize"),
    ALL_DEBRID("AllDebrid"),
    ;

    companion object {
        fun fromString(type: String): AccountType {
            return values().find { it.name.equals(type, ignoreCase = true) } ?: REAL_DEBRID
        }
    }
}

/**
 * Represents a file or folder item in the browser
 */
sealed class FileItem {
    abstract val id: String
    abstract val name: String
    abstract val size: Long
    abstract val modifiedDate: Long

    data class File(
        override val id: String,
        override val name: String,
        override val size: Long,
        override val modifiedDate: Long,
        val mimeType: String?,
        val downloadUrl: String?,
        val streamUrl: String?,
        val isPlayable: Boolean = false,
        val progress: Float? = null,
        val status: FileStatus = FileStatus.READY,
    ) : FileItem()

    data class Folder(
        override val id: String,
        override val name: String,
        override val size: Long = 0L,
        override val modifiedDate: Long,
        val itemCount: Int = 0,
        val path: String,
    ) : FileItem()

    data class Torrent(
        override val id: String,
        override val name: String,
        override val size: Long,
        override val modifiedDate: Long,
        val hash: String,
        val progress: Float,
        val status: TorrentStatus,
        val seeders: Int?,
        val speed: Long?,
        val files: List<File> = emptyList(),
    ) : FileItem()
}

/**
 * File status for individual files
 */
enum class FileStatus {
    READY,
    DOWNLOADING,
    ERROR,
    UNAVAILABLE,
}

/**
 * Torrent status matching Real-Debrid API
 */
enum class TorrentStatus(val displayName: String) {
    MAGNET_ERROR("Magnet Error"),
    MAGNET_CONVERSION("Converting Magnet"),
    WAITING_FILES_SELECTION("Waiting File Selection"),
    QUEUED("Queued"),
    DOWNLOADING("Downloading"),
    DOWNLOADED("Downloaded"),
    ERROR("Error"),
    VIRUS("Virus Detected"),
    COMPRESSING("Compressing"),
    UPLOADING("Uploading"),
    DEAD("Dead"),
    ;

    companion object {
        fun fromString(status: String): TorrentStatus {
            return values().find { it.name.equals(status, ignoreCase = true) } ?: ERROR
        }
    }
}

/**
 * Sorting options for the file browser
 */
data class SortingOptions(
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
)

/**
 * Available sorting criteria
 */
enum class SortBy(val displayName: String) {
    NAME("Name"),
    SIZE("Size"),
    DATE("Date Modified"),
    TYPE("Type"),
    STATUS("Status"),
}

/**
 * Sort order direction
 */
enum class SortOrder {
    ASCENDING,
    DESCENDING,
}

/**
 * Filter options for the file browser
 */
data class FilterOptions(
    val showOnlyPlayable: Boolean = false,
    val showOnlyDownloaded: Boolean = false,
    val fileTypeFilter: Set<FileType> = emptySet(),
    val statusFilter: Set<FileStatus> = emptySet(),
    val searchQuery: String = "",
)

/**
 * File types for filtering
 */
enum class FileType(val displayName: String, val extensions: Set<String>) {
    VIDEO("Video", setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg")),
    AUDIO("Audio", setOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus")),
    DOCUMENT("Document", setOf("pdf", "doc", "docx", "txt", "odt", "rtf")),
    IMAGE("Image", setOf("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp")),
    ARCHIVE("Archive", setOf("zip", "rar", "7z", "tar", "gz", "bz2")),
    SUBTITLE("Subtitle", setOf("srt", "ass", "vtt", "sub", "ssa")),
    OTHER("Other", emptySet()),
    ;

    companion object {
        fun fromExtension(extension: String): FileType {
            val ext = extension.lowercase()
            return values().find { type ->
                type.extensions.contains(ext)
            } ?: OTHER
        }
    }
}

/**
 * View modes for displaying file items
 */
enum class ViewMode(val displayName: String) {
    LIST("List View"),
    TILES("Tiles View"),
    GRID("Grid View"),
}

/**
 * Actions that can be performed on file items
 */
sealed class FileBrowserAction {
    data class Navigate(val path: String) : FileBrowserAction()

    data class SelectItem(val itemId: String) : FileBrowserAction()

    data class DeselectItem(val itemId: String) : FileBrowserAction()

    object ToggleMultiSelect : FileBrowserAction()

    object ClearSelection : FileBrowserAction()

    data class SortBy(val sortBy: com.rdwatch.androidtv.ui.filebrowser.models.SortBy) : FileBrowserAction()

    object ToggleSortOrder : FileBrowserAction()

    data class UpdateFilter(val filterOptions: FilterOptions) : FileBrowserAction()

    data class Search(val query: String) : FileBrowserAction()

    data class PlayFile(val file: FileItem.File) : FileBrowserAction()

    data class DownloadFiles(val itemIds: Set<String>) : FileBrowserAction()

    data class DeleteFiles(val itemIds: Set<String>) : FileBrowserAction()

    data class BulkDownloadFiles(val itemIds: Set<String>) : FileBrowserAction()

    data class BulkDeleteFiles(val itemIds: Set<String>) : FileBrowserAction()

    object CancelBulkOperation : FileBrowserAction()

    object RetryFailedItems : FileBrowserAction()

    object NavigateBack : FileBrowserAction()

    object Refresh : FileBrowserAction()

    object PullToRefresh : FileBrowserAction()

    object ClearCache : FileBrowserAction()

    data class ChangeViewMode(val viewMode: ViewMode) : FileBrowserAction()
}

/**
 * Selection state for multi-select operations
 */
data class SelectionState(
    val selectedCount: Int = 0,
    val canDownload: Boolean = false,
    val canDelete: Boolean = false,
    val canPlay: Boolean = false,
)

/**
 * Pagination state for large file lists
 */
data class PaginationState(
    val currentPage: Int = 0,
    val pageSize: Int = 50,
    val totalItems: Int = 0,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val offset: Int = 0,
) {
    val hasMore: Boolean
        get() = hasNextPage && !isLoadingMore
}
