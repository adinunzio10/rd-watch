package com.rdwatch.androidtv.ui.filebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.ui.filebrowser.models.FileItemUiModel
import com.rdwatch.androidtv.ui.filebrowser.models.FileSortOption
import com.rdwatch.androidtv.ui.filebrowser.state.FileBrowserUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Real Debrid File Browser
 * 
 * Manages file list state, sorting, filtering, selection, and user interactions.
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val realDebridRepository: RealDebridContentRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()
    
    private val _allFiles = MutableStateFlow<List<FileItemUiModel>>(emptyList())
    
    init {
        observeFileFiltering()
        loadFiles()
    }
    
    /**
     * Observes changes to search query, sort option, and all files to update filtered list
     */
    private fun observeFileFiltering() {
        combine(
            _allFiles,
            _uiState.map { it.searchQuery },
            _uiState.map { it.sortOption }
        ) { files, query, sortOption ->
            filterAndSortFiles(files, query, sortOption)
        }.onEach { filteredFiles ->
            _uiState.update { it.copy(filteredFiles = filteredFiles) }
        }.launchIn(viewModelScope)
    }
    
    /**
     * Loads files from Real Debrid repository
     */
    fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            realDebridRepository.getAllContent().collect { result ->
                when (result) {
                    is com.rdwatch.androidtv.repository.base.Result.Success -> {
                        val fileItems = result.data.map { content ->
                            FileItemUiModel(
                                id = content.realDebridId ?: content.id.toString(),
                                filename = content.title,
                                filesize = (content.duration ?: 0) * 1024L * 1024L, // Approximate size
                                mimeType = "video/mp4", // Default for now
                                downloadUrl = "", // Will be populated when needed
                                streamUrl = null,
                                host = "Real-Debrid",
                                dateAdded = content.addedDate,
                                isStreamable = true
                            )
                        }
                        _allFiles.value = fileItems
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is com.rdwatch.androidtv.repository.base.Result.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Failed to load files"
                            )
                        }
                    }
                    is com.rdwatch.androidtv.repository.base.Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                }
            }
        }
    }
    
    /**
     * Refreshes the file list
     */
    fun refreshFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            // Trigger sync and then reload
            realDebridRepository.syncContent()
            loadFiles()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
    
    /**
     * Sets the search query for filtering files
     */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    /**
     * Sets the sort option for ordering files
     */
    fun setSortOption(sortOption: FileSortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
    }
    
    /**
     * Toggles selection mode on/off
     */
    fun toggleSelectionMode() {
        _uiState.update { 
            it.copy(
                isSelectionMode = !it.isSelectionMode,
                selectedFiles = if (it.isSelectionMode) emptySet() else it.selectedFiles
            )
        }
    }
    
    /**
     * Toggles selection of a specific file
     */
    fun toggleFileSelection(file: FileItemUiModel) {
        _uiState.update { state ->
            val newSelection = if (state.selectedFiles.contains(file.id)) {
                state.selectedFiles - file.id
            } else {
                state.selectedFiles + file.id
            }
            state.copy(selectedFiles = newSelection)
        }
    }
    
    /**
     * Clears all file selections
     */
    fun clearSelection() {
        _uiState.update { it.copy(selectedFiles = emptySet()) }
    }
    
    /**
     * Selects all visible files
     */
    fun selectAllFiles() {
        _uiState.update { state ->
            val allFileIds = state.filteredFiles.map { it.id }.toSet()
            state.copy(selectedFiles = allFileIds)
        }
    }
    
    /**
     * Deletes selected files
     */
    fun deleteSelectedFiles() {
        val selectedIds = _uiState.value.selectedFiles
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                selectedIds.forEach { fileId ->
                    realDebridRepository.deleteDownload(fileId)
                }
                // Refresh the file list after deletion
                loadFiles()
                // Clear selection
                clearSelection()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to delete files"
                    )
                }
            }
        }
    }
    
    /**
     * Plays selected files (if streamable)
     */
    fun playSelectedFiles() {
        // This will be implemented when integrating with the player
        // For now, just clear selection
        clearSelection()
    }
    
    /**
     * Deletes a single file
     */
    fun deleteFile(file: FileItemUiModel) {
        viewModelScope.launch {
            try {
                realDebridRepository.deleteDownload(file.id)
                // Refresh the file list after deletion
                loadFiles()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to delete file"
                    )
                }
            }
        }
    }
    
    /**
     * Filters and sorts files based on query and sort option
     */
    private fun filterAndSortFiles(
        files: List<FileItemUiModel>,
        query: String,
        sortOption: FileSortOption
    ): List<FileItemUiModel> {
        // Apply search filter
        val filtered = if (query.isBlank()) {
            files
        } else {
            files.filter { file ->
                file.filename.contains(query, ignoreCase = true) ||
                file.mimeType.contains(query, ignoreCase = true)
            }
        }
        
        // Apply sorting
        return when (sortOption) {
            FileSortOption.NAME_ASC -> filtered.sortedBy { it.filename.lowercase() }
            FileSortOption.NAME_DESC -> filtered.sortedByDescending { it.filename.lowercase() }
            FileSortOption.SIZE_ASC -> filtered.sortedBy { it.filesize }
            FileSortOption.SIZE_DESC -> filtered.sortedByDescending { it.filesize }
            FileSortOption.DATE_ASC -> filtered.sortedBy { it.dateAdded }
            FileSortOption.DATE_DESC -> filtered.sortedByDescending { it.dateAdded }
            FileSortOption.TYPE_ASC -> filtered.sortedBy { it.mimeType }
            FileSortOption.TYPE_DESC -> filtered.sortedByDescending { it.mimeType }
        }
    }
}