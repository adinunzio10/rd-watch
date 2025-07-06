package com.rdwatch.androidtv.repository.bulk

import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages bulk selection state for the file browser with advanced selection features
 */
@Singleton
class BulkSelectionManager @Inject constructor() {
    
    private val _selectionState = MutableStateFlow(BulkSelectionState())
    val selectionState: StateFlow<BulkSelectionState> = _selectionState.asStateFlow()
    
    private val _availableFiles = MutableStateFlow<List<AccountFileItem>>(emptyList())
    val availableFiles: StateFlow<List<AccountFileItem>> = _availableFiles.asStateFlow()
    
    /**
     * Enter bulk selection mode
     */
    fun enterSelectionMode() {
        _selectionState.value = _selectionState.value.copy(
            isSelectionMode = true
        )
    }
    
    /**
     * Exit bulk selection mode and clear all selections
     */
    fun exitSelectionMode() {
        _selectionState.value = BulkSelectionState()
    }
    
    /**
     * Toggle selection mode on/off
     */
    fun toggleSelectionMode() {
        if (_selectionState.value.isSelectionMode) {
            exitSelectionMode()
        } else {
            enterSelectionMode()
        }
    }
    
    /**
     * Update the list of available files
     */
    fun updateAvailableFiles(files: List<AccountFileItem>) {
        _availableFiles.value = files
        
        // Remove any selected files that are no longer available
        val currentSelection = _selectionState.value.selectedFileIds
        val availableIds = files.map { it.id }.toSet()
        val validSelection = currentSelection.intersect(availableIds)
        
        if (validSelection.size != currentSelection.size) {
            _selectionState.value = _selectionState.value.copy(
                selectedFileIds = validSelection
            )
        }
    }
    
    /**
     * Select a single file
     */
    fun selectFile(fileId: String) {
        val currentState = _selectionState.value
        if (!currentState.isSelectionMode) return
        
        _selectionState.value = currentState.copy(
            selectedFileIds = currentState.selectedFileIds + fileId
        )
    }
    
    /**
     * Deselect a single file
     */
    fun deselectFile(fileId: String) {
        val currentState = _selectionState.value
        
        _selectionState.value = currentState.copy(
            selectedFileIds = currentState.selectedFileIds - fileId
        )
    }
    
    /**
     * Toggle selection of a single file
     */
    fun toggleFileSelection(fileId: String) {
        val currentState = _selectionState.value
        
        if (!currentState.isSelectionMode) {
            enterSelectionMode()
        }
        
        if (currentState.selectedFileIds.contains(fileId)) {
            deselectFile(fileId)
        } else {
            selectFile(fileId)
        }
    }
    
    /**
     * Select all visible files
     */
    fun selectAllFiles() {
        val currentState = _selectionState.value
        if (!currentState.isSelectionMode) {
            enterSelectionMode()
        }
        
        val allFileIds = _availableFiles.value.map { it.id }.toSet()
        _selectionState.value = currentState.copy(
            selectedFileIds = allFileIds
        )
    }
    
    /**
     * Clear all selections but stay in selection mode
     */
    fun clearSelection() {
        _selectionState.value = _selectionState.value.copy(
            selectedFileIds = emptySet()
        )
    }
    
    /**
     * Select files by type
     */
    fun selectFilesByType(fileType: FileTypeCategory) {
        val currentState = _selectionState.value
        if (!currentState.isSelectionMode) {
            enterSelectionMode()
        }
        
        val filesOfType = _availableFiles.value
            .filter { it.fileTypeCategory == fileType }
            .map { it.id }
            .toSet()
        
        _selectionState.value = currentState.copy(
            selectedFileIds = currentState.selectedFileIds + filesOfType
        )
    }
    
    /**
     * Select files by source
     */
    fun selectFilesBySource(source: FileSource) {
        val currentState = _selectionState.value
        if (!currentState.isSelectionMode) {
            enterSelectionMode()
        }
        
        val filesOfSource = _availableFiles.value
            .filter { it.source == source }
            .map { it.id }
            .toSet()
        
        _selectionState.value = currentState.copy(
            selectedFileIds = currentState.selectedFileIds + filesOfSource
        )
    }
    
    /**
     * Select files by size range
     */
    fun selectFilesBySizeRange(minSize: Long, maxSize: Long) {
        val currentState = _selectionState.value
        if (!currentState.isSelectionMode) {
            enterSelectionMode()
        }
        
        val filesInRange = _availableFiles.value
            .filter { it.filesize in minSize..maxSize }
            .map { it.id }
            .toSet()
        
        _selectionState.value = currentState.copy(
            selectedFileIds = currentState.selectedFileIds + filesInRange
        )
    }
    
    /**
     * Select streamable files only
     */
    fun selectStreamableFiles() {
        val currentState = _selectionState.value
        if (!currentState.isSelectionMode) {
            enterSelectionMode()
        }
        
        val streamableFiles = _availableFiles.value
            .filter { it.isStreamable }
            .map { it.id }
            .toSet()
        
        _selectionState.value = currentState.copy(
            selectedFileIds = currentState.selectedFileIds + streamableFiles
        )
    }
    
    /**
     * Invert current selection
     */
    fun invertSelection() {
        val currentState = _selectionState.value
        if (!currentState.isSelectionMode) {
            enterSelectionMode()
        }
        
        val allFileIds = _availableFiles.value.map { it.id }.toSet()
        val newSelection = allFileIds - currentState.selectedFileIds
        
        _selectionState.value = currentState.copy(
            selectedFileIds = newSelection
        )
    }
    
    /**
     * Get selected files as AccountFileItem objects
     */
    fun getSelectedFiles(): List<AccountFileItem> {
        val selectedIds = _selectionState.value.selectedFileIds
        return _availableFiles.value.filter { it.id in selectedIds }
    }
    
    /**
     * Get selection statistics
     */
    fun getSelectionStatistics(): SelectionStatistics {
        val selectedFiles = getSelectedFiles()
        val totalFiles = _availableFiles.value
        
        val typeBreakdown = selectedFiles.groupBy { it.fileTypeCategory }
            .mapValues { (_, files) -> files.size }
        
        val sourceBreakdown = selectedFiles.groupBy { it.source }
            .mapValues { (_, files) -> files.size }
        
        val totalSize = selectedFiles.sumOf { it.filesize }
        
        return SelectionStatistics(
            totalSelected = selectedFiles.size,
            totalAvailable = totalFiles.size,
            selectionPercentage = if (totalFiles.isNotEmpty()) {
                (selectedFiles.size.toFloat() / totalFiles.size.toFloat()) * 100f
            } else 0f,
            totalSize = totalSize,
            typeBreakdown = typeBreakdown,
            sourceBreakdown = sourceBreakdown,
            streamableCount = selectedFiles.count { it.isStreamable },
            playableCount = selectedFiles.count { it.isPlayableFile }
        )
    }
    
    /**
     * Get available operations for current selection
     */
    fun getAvailableOperations(): List<BulkOperationType> {
        val selectedFiles = getSelectedFiles()
        if (selectedFiles.isEmpty()) return emptyList()
        
        val operations = mutableListOf<BulkOperationType>()
        
        // Delete is always available
        operations.add(BulkOperationType.DELETE)
        
        // Download is available for all files
        operations.add(BulkOperationType.DOWNLOAD)
        
        // Play is available if any files are streamable and playable
        if (selectedFiles.any { it.isStreamable && it.isPlayableFile }) {
            operations.add(BulkOperationType.PLAY)
        }
        
        // Add to favorites is always available
        operations.add(BulkOperationType.ADD_TO_FAVORITES)
        
        return operations
    }
    
    /**
     * Check if a specific operation is available for current selection
     */
    fun isOperationAvailable(operation: BulkOperationType): Boolean {
        return getAvailableOperations().contains(operation)
    }
    
    /**
     * Get suggested selection actions based on current files
     */
    fun getSuggestedSelectionActions(): List<SelectionAction> {
        val files = _availableFiles.value
        val suggestions = mutableListOf<SelectionAction>()
        
        // Suggest selecting by type if there are multiple types
        val typeGroups = files.groupBy { it.fileTypeCategory }
        if (typeGroups.size > 1) {
            typeGroups.forEach { (type, filesOfType) ->
                if (filesOfType.size >= 2) {
                    suggestions.add(SelectionAction.SelectByType(type, filesOfType.size))
                }
            }
        }
        
        // Suggest selecting by source if there are multiple sources
        val sourceGroups = files.groupBy { it.source }
        if (sourceGroups.size > 1) {
            sourceGroups.forEach { (source, filesOfSource) ->
                if (filesOfSource.size >= 2) {
                    suggestions.add(SelectionAction.SelectBySource(source, filesOfSource.size))
                }
            }
        }
        
        // Suggest selecting streamable files if there are any
        val streamableCount = files.count { it.isStreamable }
        if (streamableCount > 1) {
            suggestions.add(SelectionAction.SelectStreamable(streamableCount))
        }
        
        // Suggest selecting large files if there are any
        val largeFileCount = files.count { it.filesize > 1024L * 1024L * 1024L } // > 1GB
        if (largeFileCount > 1) {
            suggestions.add(SelectionAction.SelectLargeFiles(largeFileCount))
        }
        
        return suggestions
    }
}

/**
 * State for bulk selection
 */
data class BulkSelectionState(
    val isSelectionMode: Boolean = false,
    val selectedFileIds: Set<String> = emptySet()
) {
    val hasSelection: Boolean get() = selectedFileIds.isNotEmpty()
    val selectionCount: Int get() = selectedFileIds.size
}

/**
 * Statistics about the current selection
 */
data class SelectionStatistics(
    val totalSelected: Int,
    val totalAvailable: Int,
    val selectionPercentage: Float,
    val totalSize: Long,
    val typeBreakdown: Map<FileTypeCategory, Int>,
    val sourceBreakdown: Map<FileSource, Int>,
    val streamableCount: Int,
    val playableCount: Int
) {
    val formattedTotalSize: String
        get() = formatFileSize(totalSize)
    
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

/**
 * Suggested selection actions
 */
sealed class SelectionAction(val displayName: String, val count: Int) {
    class SelectByType(val type: FileTypeCategory, count: Int) : 
        SelectionAction("Select all ${type.displayName.lowercase()} files", count)
    
    class SelectBySource(val source: FileSource, count: Int) : 
        SelectionAction("Select all ${source.name.lowercase()} files", count)
    
    class SelectStreamable(count: Int) : 
        SelectionAction("Select streamable files", count)
    
    class SelectLargeFiles(count: Int) : 
        SelectionAction("Select large files (>1GB)", count)
}