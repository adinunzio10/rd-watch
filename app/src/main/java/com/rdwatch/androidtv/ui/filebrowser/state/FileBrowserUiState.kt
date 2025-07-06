package com.rdwatch.androidtv.ui.filebrowser.state

import com.rdwatch.androidtv.ui.filebrowser.models.FileItemUiModel
import com.rdwatch.androidtv.ui.filebrowser.models.FileSortOption

/**
 * UI State for the File Browser Screen
 */
data class FileBrowserUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val filteredFiles: List<FileItemUiModel> = emptyList(),
    val searchQuery: String = "",
    val sortOption: FileSortOption = FileSortOption.DATE_DESC,
    val isSelectionMode: Boolean = false,
    val selectedFiles: Set<String> = emptySet()
)