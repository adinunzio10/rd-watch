package com.rdwatch.androidtv.ui.filebrowser.models

/**
 * UI events that can be triggered from the file browser
 */
sealed class FileBrowserEvent {
    data class ShowError(val message: String) : FileBrowserEvent()
    data class ShowSuccess(val message: String) : FileBrowserEvent()
    data class NavigateToPlayer(val url: String, val title: String) : FileBrowserEvent()
    data class ShowConfirmDialog(
        val title: String,
        val message: String,
        val onConfirm: () -> Unit
    ) : FileBrowserEvent()
    data class ShowFileDetails(val file: FileItem) : FileBrowserEvent()
    object ShowLoading : FileBrowserEvent()
    object HideLoading : FileBrowserEvent()
    object NavigateBack : FileBrowserEvent()
    data class UpdateSelectionMode(val isMultiSelect: Boolean) : FileBrowserEvent()
    data class BulkOperationStarted(val operationType: BulkOperationType) : FileBrowserEvent()
    data class BulkOperationProgress(val progress: Float, val currentItem: String) : FileBrowserEvent()
    data class BulkOperationCompleted(val result: BulkOperationResult) : FileBrowserEvent()
    data class BulkOperationFailed(val error: String) : FileBrowserEvent()
    object RefreshStarted : FileBrowserEvent()
    object RefreshCompleted : FileBrowserEvent()
    data class RefreshFailed(val error: String) : FileBrowserEvent()
    data class RecoveryStarted(val operationType: BulkOperationType) : FileBrowserEvent()
    data class RecoveryCompleted(val recoveredCount: Int) : FileBrowserEvent()
    data class ViewModeChanged(val viewMode: ViewMode) : FileBrowserEvent()
}

/**
 * Navigation events specific to file browser
 */
sealed class FileBrowserNavigationEvent {
    data class OpenFile(val file: FileItem.File) : FileBrowserNavigationEvent()
    data class OpenFolder(val folder: FileItem.Folder) : FileBrowserNavigationEvent()
    data class OpenTorrent(val torrent: FileItem.Torrent) : FileBrowserNavigationEvent()
    object GoBack : FileBrowserNavigationEvent()
    object GoToRoot : FileBrowserNavigationEvent()
}

/**
 * Context menu actions for file items
 */
enum class FileContextAction(val displayName: String, val requiresSelection: Boolean = false) {
    PLAY("Play", false),
    DOWNLOAD("Download", false),
    DELETE("Delete", false),
    RENAME("Rename", false),
    DETAILS("View Details", false),
    SELECT_ALL("Select All", false),
    DESELECT_ALL("Deselect All", true),
    COPY_LINK("Copy Link", false),
    SHARE("Share", false);
    
    companion object {
        /**
         * Get available actions for a specific file type
         */
        fun getActionsForItem(item: FileItem, isSelected: Boolean): List<FileContextAction> {
            return when (item) {
                is FileItem.File -> {
                    buildList {
                        if (item.isPlayable) add(PLAY)
                        add(DOWNLOAD)
                        add(DELETE)
                        add(DETAILS)
                        add(COPY_LINK)
                        add(SHARE)
                        if (!isSelected) add(SELECT_ALL)
                        if (isSelected) add(DESELECT_ALL)
                    }
                }
                is FileItem.Torrent -> {
                    buildList {
                        add(DELETE)
                        add(DETAILS)
                        if (!isSelected) add(SELECT_ALL)
                        if (isSelected) add(DESELECT_ALL)
                    }
                }
                is FileItem.Folder -> {
                    buildList {
                        add(DETAILS)
                        if (!isSelected) add(SELECT_ALL)
                        if (isSelected) add(DESELECT_ALL)
                    }
                }
            }
        }
    }
}