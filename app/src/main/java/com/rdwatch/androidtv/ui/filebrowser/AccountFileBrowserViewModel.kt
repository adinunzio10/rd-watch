package com.rdwatch.androidtv.ui.filebrowser

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.filebrowser.repository.FileBrowserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Account File Browser Screen
 * Manages UI state for browsing debrid service files with TV-optimized experience
 */
@HiltViewModel
class AccountFileBrowserViewModel @Inject constructor(
    private val fileBrowserRepository: FileBrowserRepository
) : BaseViewModel<FileBrowserState>() {
    
    private var loadContentJob: Job? = null
    private var searchJob: Job? = null
    
    // Events channel for one-time UI events
    private val _events = MutableSharedFlow<FileBrowserEvent>()
    val events: SharedFlow<FileBrowserEvent> = _events.asSharedFlow()
    
    // Raw content from repository
    private val _rawContent = MutableStateFlow<List<FileItem>>(emptyList())
    
    override fun createInitialState(): FileBrowserState {
        return FileBrowserState()
    }
    
    init {
        checkAuthenticationAndLoad()
    }
    
    /**
     * Check authentication and load initial content
     */
    private fun checkAuthenticationAndLoad() {
        launchSafely { 
            val isAuthenticated = fileBrowserRepository.isAuthenticated()
            if (isAuthenticated) {
                loadRootContent()
            } else {
                updateState { 
                    copy(
                        contentState = UiState.Error("Authentication required. Please sign in to access your files.")
                    )
                }
            }
        }
    }
    
    /**
     * Load root content (torrents/downloads)
     */
    private fun loadRootContent() {
        loadContentJob?.cancel()
        loadContentJob = viewModelScope.launch {
            updateState { copy(contentState = UiState.Loading) }
            
            fileBrowserRepository.getRootContent()
                .catch { e ->
                    updateState { 
                        copy(contentState = UiState.Error("Failed to load files: ${e.message}"))
                    }
                    _events.emit(FileBrowserEvent.ShowError("Failed to load files: ${e.message}"))
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _rawContent.value = result.data
                            val initialPageContent = result.data.take(uiState.value.paginationState.pageSize)
                            val sortedAndFilteredContent = applySortingAndFiltering(initialPageContent)
                            updateState { 
                                copy(
                                    contentState = UiState.Success(sortedAndFilteredContent),
                                    currentPath = "/",
                                    navigationHistory = listOf("/"),
                                    paginationState = paginationState.copy(
                                        totalItems = result.data.size,
                                        hasNextPage = result.data.size > paginationState.pageSize,
                                        offset = paginationState.pageSize
                                    )
                                )
                            }
                        }
                        is Result.Error -> {
                            val errorMessage = "Failed to load files: ${result.exception.message}"
                            updateState { 
                                copy(contentState = UiState.Error(errorMessage))
                            }
                            _events.emit(FileBrowserEvent.ShowError(errorMessage))
                        }
                        is Result.Loading -> {
                            updateState { copy(contentState = UiState.Loading) }
                        }
                    }
                }
        }
    }
    
    /**
     * Navigate to a specific path (folder or torrent)
     */
    fun navigateToPath(path: String, item: FileItem? = null) {
        when (item) {
            is FileItem.Torrent -> {
                launchSafely { 
                    updateState { copy(contentState = UiState.Loading) }
                    
                    when (val result = fileBrowserRepository.getTorrentFiles(item.id)) {
                        is Result.Success -> {
                            val sortedAndFilteredContent = applySortingAndFiltering(result.data)
                            updateState { 
                                copy(
                                    contentState = UiState.Success(sortedAndFilteredContent),
                                    currentPath = path,
                                    navigationHistory = uiState.value.navigationHistory + path
                                )
                            }
                        }
                        is Result.Error -> {
                            val errorMessage = "Failed to load torrent files: ${result.exception.message}"
                            updateState { 
                                copy(contentState = UiState.Error(errorMessage))
                            }
                            _events.emit(FileBrowserEvent.ShowError(errorMessage))
                        }
                        is Result.Loading -> {
                            // Keep loading state
                        }
                    }
                }
            }
            is FileItem.Folder -> {
                // For future implementation when folder navigation is supported
                updateState { 
                    copy(
                        currentPath = path,
                        navigationHistory = uiState.value.navigationHistory + path
                    )
                }
            }
            else -> {
                // Navigate to root or handle other cases
                loadRootContent()
            }
        }
    }
    
    /**
     * Navigate back in the navigation history
     */
    fun navigateBack() {
        val currentHistory = uiState.value.navigationHistory
        if (currentHistory.size > 1) {
            val newHistory = currentHistory.dropLast(1)
            val previousPath = newHistory.last()
            
            if (previousPath == "/") {
                loadRootContent()
            } else {
                // Navigate to previous path
                navigateToPath(previousPath)
            }
        } else {
            // At root, emit back navigation event
            launchSafely { 
                _events.emit(FileBrowserEvent.NavigateBack)
            }
        }
    }
    
    /**
     * Update sorting options
     */
    fun updateSorting(sortingOptions: SortingOptions) {
        updateState { copy(sortingOptions = sortingOptions) }
        
        // Re-apply sorting to current content
        val currentContent = uiState.value.contentState.dataOrNull
        if (currentContent != null) {
            val sortedContent = applySortingAndFiltering(currentContent)
            updateState { 
                copy(contentState = UiState.Success(sortedContent))
            }
        }
    }
    
    /**
     * Change view mode
     */
    fun changeViewMode(viewMode: ViewMode) {
        updateState { copy(viewMode = viewMode) }
        
        // Emit view mode change event if needed
        launchSafely {
            _events.emit(FileBrowserEvent.ViewModeChanged(viewMode))
        }
    }
    
    /**
     * Update filter options
     */
    fun updateFilter(filterOptions: FilterOptions) {
        updateState { copy(filterOptions = filterOptions) }
        
        // Re-apply filtering to raw content
        val rawContent = _rawContent.value
        if (rawContent.isNotEmpty()) {
            val filteredContent = applySortingAndFiltering(rawContent)
            updateState { 
                copy(contentState = UiState.Success(filteredContent))
            }
        }
    }
    
    /**
     * Toggle multi-select mode
     */
    fun toggleMultiSelect() {
        updateState { 
            copy(
                isMultiSelectMode = !isMultiSelectMode,
                selectedItems = if (isMultiSelectMode) emptySet() else selectedItems
            )
        }
        
        launchSafely { 
            _events.emit(FileBrowserEvent.UpdateSelectionMode(!uiState.value.isMultiSelectMode))
        }
    }
    
    /**
     * Clear all selections
     */
    fun clearSelection() {
        updateState { copy(selectedItems = emptySet()) }
    }
    
    /**
     * Select all visible items
     */
    fun selectAll() {
        val currentContent = uiState.value.contentState.dataOrNull ?: emptyList()
        val allItemIds = currentContent.map { it.id }.toSet()
        updateState { copy(selectedItems = allItemIds) }
    }
    
    /**
     * Enter bulk selection mode with long press
     */
    fun enterBulkSelectionMode(itemId: String) {
        updateState { 
            copy(
                isMultiSelectMode = true,
                selectedItems = setOf(itemId)
            )
        }
        
        launchSafely { 
            _events.emit(FileBrowserEvent.UpdateSelectionMode(true))
        }
    }
    
    /**
     * Play first playable selected file
     */
    fun playSelectedFiles() {
        val selectedIds = uiState.value.selectedItems
        val currentContent = uiState.value.contentState.dataOrNull ?: emptyList()
        val selectedItems = currentContent.filter { selectedIds.contains(it.id) }
        
        val playableFile = selectedItems.firstOrNull { item ->
            when (item) {
                is FileItem.File -> item.isPlayable
                else -> false
            }
        } as? FileItem.File
        
        if (playableFile != null) {
            selectFile(playableFile)
        } else {
            launchSafely { 
                _events.emit(FileBrowserEvent.ShowError("No playable files selected"))
            }
        }
    }
    
    /**
     * Toggle selection for a specific item
     */
    fun toggleItemSelection(itemId: String) {
        val currentSelection = uiState.value.selectedItems
        val newSelection = if (currentSelection.contains(itemId)) {
            currentSelection - itemId
        } else {
            currentSelection + itemId
        }
        
        updateState { copy(selectedItems = newSelection) }
    }
    
    /**
     * Handle file selection for playback
     */
    fun selectFile(file: FileItem.File) {
        if (file.isPlayable) {
            launchSafely { 
                updateState { copy(contentState = UiState.Loading) }
                
                when (val result = fileBrowserRepository.getPlaybackUrl(file.id)) {
                    is Result.Success -> {
                        _events.emit(FileBrowserEvent.NavigateToPlayer(result.data, file.name))
                        updateState { 
                            copy(contentState = UiState.Success(uiState.value.contentState.dataOrNull ?: emptyList()))
                        }
                    }
                    is Result.Error -> {
                        val errorMessage = "Failed to get playback URL: ${result.exception.message}"
                        updateState { 
                            copy(contentState = UiState.Success(uiState.value.contentState.dataOrNull ?: emptyList()))
                        }
                        _events.emit(FileBrowserEvent.ShowError(errorMessage))
                    }
                    is Result.Loading -> {
                        // Keep loading state
                    }
                }
            }
        } else {
            launchSafely { 
                _events.emit(FileBrowserEvent.ShowError("This file cannot be played"))
            }
        }
    }
    
    /**
     * Show file details dialog
     */
    fun showFileDetails(item: FileItem) {
        launchSafely { 
            _events.emit(FileBrowserEvent.ShowFileDetails(item))
        }
    }
    
    /**
     * Download selected files
     */
    fun downloadSelectedFiles() {
        val selectedIds = uiState.value.selectedItems
        if (selectedIds.isEmpty()) return
        
        launchSafely { 
            when (val result = fileBrowserRepository.downloadFiles(selectedIds)) {
                is Result.Success -> {
                    _events.emit(FileBrowserEvent.ShowSuccess("Download started for ${selectedIds.size} files"))
                    clearSelection()
                }
                is Result.Error -> {
                    _events.emit(FileBrowserEvent.ShowError("Download failed: ${result.exception.message}"))
                }
                is Result.Loading -> {
                    // Show loading state
                }
            }
        }
    }
    
    /**
     * Delete selected files
     */
    fun deleteSelectedFiles() {
        val selectedIds = uiState.value.selectedItems
        if (selectedIds.isEmpty()) return
        
        launchSafely { 
            _events.emit(FileBrowserEvent.ShowConfirmDialog(
                title = "Delete Files",
                message = "Are you sure you want to delete ${selectedIds.size} selected files? This action cannot be undone.",
                onConfirm = { performDelete(selectedIds) }
            ))
        }
    }
    
    /**
     * Perform actual deletion
     */
    private fun performDelete(itemIds: Set<String>) {
        launchSafely { 
            when (val result = fileBrowserRepository.deleteItems(itemIds)) {
                is Result.Success -> {
                    _events.emit(FileBrowserEvent.ShowSuccess("${itemIds.size} files deleted successfully"))
                    clearSelection()
                    refresh() // Refresh content
                }
                is Result.Error -> {
                    _events.emit(FileBrowserEvent.ShowError("Delete failed: ${result.exception.message}"))
                }
                is Result.Loading -> {
                    // Show loading state
                }
            }
        }
    }
    
    /**
     * Search content
     */
    fun searchContent(query: String) {
        updateState { copy(filterOptions = filterOptions.copy(searchQuery = query)) }
        
        if (query.isBlank()) {
            // Reset to original content
            val rawContent = _rawContent.value
            val filteredContent = applySortingAndFiltering(rawContent)
            updateState { 
                copy(contentState = UiState.Success(filteredContent))
            }
            return
        }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            updateState { copy(contentState = UiState.Loading) }
            
            fileBrowserRepository.searchContent(query)
                .catch { e ->
                    updateState { 
                        copy(contentState = UiState.Error("Search failed: ${e.message}"))
                    }
                    _events.emit(FileBrowserEvent.ShowError("Search failed: ${e.message}"))
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val sortedContent = applySortingAndFiltering(result.data)
                            updateState { 
                                copy(contentState = UiState.Success(sortedContent))
                            }
                        }
                        is Result.Error -> {
                            val errorMessage = "Search failed: ${result.exception.message}"
                            updateState { 
                                copy(contentState = UiState.Error(errorMessage))
                            }
                            _events.emit(FileBrowserEvent.ShowError(errorMessage))
                        }
                        is Result.Loading -> {
                            updateState { copy(contentState = UiState.Loading) }
                        }
                    }
                }
        }
    }
    
    /**
     * Refresh content
     */
    fun refresh() {
        launchSafely { 
            when (val result = fileBrowserRepository.refreshContent()) {
                is Result.Success -> {
                    loadRootContent()
                }
                is Result.Error -> {
                    _events.emit(FileBrowserEvent.ShowError("Refresh failed: ${result.exception.message}"))
                }
                is Result.Loading -> {
                    // Show loading state
                }
            }
        }
    }
    
    /**
     * Load more content for pagination
     */
    fun loadMoreContent() {
        val currentState = uiState.value
        if (!currentState.paginationState.hasMore) return
        
        updateState { 
            copy(paginationState = paginationState.copy(isLoadingMore = true))
        }
        
        // In a real implementation, you would make an API call with offset/limit
        // For now, we'll just simulate pagination by loading more from the raw content
        launchSafely {
            val nextPageContent = _rawContent.value.drop(currentState.paginationState.offset)
                .take(currentState.paginationState.pageSize)
            
            if (nextPageContent.isNotEmpty()) {
                val currentContent = currentState.contentState.dataOrNull ?: emptyList()
                val allContent = currentContent + nextPageContent
                val sortedAndFilteredContent = applySortingAndFiltering(allContent)
                
                updateState { 
                    copy(
                        contentState = UiState.Success(sortedAndFilteredContent),
                        paginationState = paginationState.copy(
                            currentPage = paginationState.currentPage + 1,
                            offset = paginationState.offset + paginationState.pageSize,
                            isLoadingMore = false,
                            hasNextPage = _rawContent.value.size > paginationState.offset + paginationState.pageSize
                        )
                    )
                }
            } else {
                updateState { 
                    copy(paginationState = paginationState.copy(isLoadingMore = false, hasNextPage = false))
                }
            }
        }
    }

    /**
     * Apply sorting and filtering to content with pagination
     */
    private fun applySortingAndFiltering(content: List<FileItem>): List<FileItem> {
        val currentState = uiState.value
        var filteredContent = content
        
        // Apply filters
        val filterOptions = currentState.filterOptions
        
        // Search query filter
        if (filterOptions.searchQuery.isNotBlank()) {
            filteredContent = filteredContent.filter { item ->
                item.name.contains(filterOptions.searchQuery, ignoreCase = true)
            }
        }
        
        // Playable filter
        if (filterOptions.showOnlyPlayable) {
            filteredContent = filteredContent.filter { item ->
                when (item) {
                    is FileItem.File -> item.isPlayable
                    is FileItem.Torrent -> item.files.any { it.isPlayable }
                    else -> true
                }
            }
        }
        
        // Downloaded filter
        if (filterOptions.showOnlyDownloaded) {
            filteredContent = filteredContent.filter { item ->
                when (item) {
                    is FileItem.File -> item.status == FileStatus.READY
                    is FileItem.Torrent -> item.status == TorrentStatus.DOWNLOADED
                    else -> true
                }
            }
        }
        
        // File type filter
        if (filterOptions.fileTypeFilter.isNotEmpty()) {
            filteredContent = filteredContent.filter { item ->
                when (item) {
                    is FileItem.File -> {
                        val extension = item.name.substringAfterLast('.', "")
                        val fileType = FileType.fromExtension(extension)
                        filterOptions.fileTypeFilter.contains(fileType)
                    }
                    else -> true // Keep folders and torrents
                }
            }
        }
        
        // Status filter
        if (filterOptions.statusFilter.isNotEmpty()) {
            filteredContent = filteredContent.filter { item ->
                when (item) {
                    is FileItem.File -> filterOptions.statusFilter.contains(item.status)
                    else -> true
                }
            }
        }
        
        // Apply sorting
        val sortingOptions = currentState.sortingOptions
        filteredContent = when (sortingOptions.sortBy) {
            SortBy.NAME -> filteredContent.sortedBy { it.name }
            SortBy.SIZE -> filteredContent.sortedBy { it.size }
            SortBy.DATE -> filteredContent.sortedBy { it.modifiedDate }
            SortBy.TYPE -> filteredContent.sortedBy { item ->
                when (item) {
                    is FileItem.Folder -> "1_folder"
                    is FileItem.Torrent -> "2_torrent"
                    is FileItem.File -> "3_${item.name.substringAfterLast('.', "")}"
                }
            }
            SortBy.STATUS -> filteredContent.sortedBy { item ->
                when (item) {
                    is FileItem.File -> item.status.ordinal
                    is FileItem.Torrent -> item.status.ordinal
                    else -> 0
                }
            }
        }
        
        // Apply sort order
        return if (sortingOptions.sortOrder == SortOrder.DESCENDING) {
            filteredContent.reversed()
        } else {
            filteredContent
        }
    }
    
    /**
     * Get selection state for multi-select operations
     */
    fun getSelectionState(): SelectionState {
        val selectedItems = uiState.value.selectedItems
        val contentItems = uiState.value.contentState.dataOrNull ?: emptyList()
        val selectedFileItems = contentItems.filter { selectedItems.contains(it.id) }
        
        return SelectionState(
            selectedCount = selectedItems.size,
            canDownload = selectedFileItems.isNotEmpty(),
            canDelete = selectedFileItems.isNotEmpty(),
            canPlay = selectedFileItems.any { item ->
                when (item) {
                    is FileItem.File -> item.isPlayable
                    else -> false
                }
            }
        )
    }
    
    override fun handleError(exception: Throwable) {
        super.handleError(exception)
        updateState { 
            copy(contentState = UiState.Error("An error occurred: ${exception.message}"))
        }
        
        launchSafely { 
            _events.emit(FileBrowserEvent.ShowError("An error occurred: ${exception.message}"))
        }
    }
}