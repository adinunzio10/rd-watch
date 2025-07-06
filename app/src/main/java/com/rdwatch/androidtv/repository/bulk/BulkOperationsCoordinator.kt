package com.rdwatch.androidtv.repository.bulk

import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinator that brings together bulk operations and selection management
 * to provide a unified API for bulk file operations
 */
@Singleton
class BulkOperationsCoordinator @Inject constructor(
    private val bulkOperationsService: BulkOperationsService,
    private val bulkSelectionManager: BulkSelectionManager
) {
    
    // Expose selection state
    val selectionState: StateFlow<BulkSelectionState> = bulkSelectionManager.selectionState
    val availableFiles: StateFlow<List<AccountFileItem>> = bulkSelectionManager.availableFiles
    
    /**
     * Update available files for selection
     */
    fun updateAvailableFiles(files: List<AccountFileItem>) {
        bulkSelectionManager.updateAvailableFiles(files)
    }
    
    /**
     * Toggle selection mode
     */
    fun toggleSelectionMode() {
        bulkSelectionManager.toggleSelectionMode()
    }
    
    /**
     * Toggle file selection
     */
    fun toggleFileSelection(fileId: String) {
        bulkSelectionManager.toggleFileSelection(fileId)
    }
    
    /**
     * Select all files
     */
    fun selectAllFiles() {
        bulkSelectionManager.selectAllFiles()
    }
    
    /**
     * Clear selection
     */
    fun clearSelection() {
        bulkSelectionManager.clearSelection()
    }
    
    /**
     * Exit selection mode
     */
    fun exitSelectionMode() {
        bulkSelectionManager.exitSelectionMode()
    }
    
    /**
     * Execute operation on selected files
     */
    fun executeOperationOnSelected(
        operationType: BulkOperationType,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationProgress>? {
        val selectedFiles = bulkSelectionManager.getSelectedFiles()
        
        return if (selectedFiles.isNotEmpty()) {
            bulkOperationsService.executeBulkOperation(selectedFiles, operationType, options)
        } else {
            null
        }
    }
    
    /**
     * Execute operation on specific files
     */
    fun executeOperationOnFiles(
        files: List<AccountFileItem>,
        operationType: BulkOperationType,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationProgress> {
        return bulkOperationsService.executeBulkOperation(files, operationType, options)
    }
    
    /**
     * Cancel an active operation
     */
    suspend fun cancelOperation(operationId: String): Boolean {
        return bulkOperationsService.cancelOperation(operationId)
    }
    
    /**
     * Get selection statistics
     */
    fun getSelectionStatistics(): SelectionStatistics {
        return bulkSelectionManager.getSelectionStatistics()
    }
    
    /**
     * Get available operations for current selection
     */
    fun getAvailableOperations(): List<BulkOperationType> {
        return bulkSelectionManager.getAvailableOperations()
    }
    
    /**
     * Get suggested selection actions
     */
    fun getSuggestedSelectionActions(): List<SelectionAction> {
        return bulkSelectionManager.getSuggestedSelectionActions()
    }
    
    /**
     * Quick select by criteria
     */
    fun quickSelectByType(fileType: FileTypeCategory) {
        bulkSelectionManager.selectFilesByType(fileType)
    }
    
    fun quickSelectBySource(source: FileSource) {
        bulkSelectionManager.selectFilesBySource(source)
    }
    
    fun quickSelectStreamableFiles() {
        bulkSelectionManager.selectStreamableFiles()
    }
    
    fun quickSelectLargeFiles() {
        bulkSelectionManager.selectFilesBySizeRange(1024L * 1024L * 1024L, Long.MAX_VALUE)
    }
    
    /**
     * Invert selection
     */
    fun invertSelection() {
        bulkSelectionManager.invertSelection()
    }
    
    /**
     * Execute delete operation with confirmation
     */
    fun executeDeleteWithConfirmation(
        files: List<AccountFileItem>? = null,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationResult> {
        val filesToDelete = files ?: bulkSelectionManager.getSelectedFiles()
        
        return if (filesToDelete.isNotEmpty()) {
            bulkOperationsService.executeBulkOperation(filesToDelete, BulkOperationType.DELETE, options)
                .map { progress ->
                    BulkOperationResult(
                        operationType = BulkOperationType.DELETE,
                        totalItems = progress.totalItems,
                        successCount = progress.completedItems,
                        failureCount = progress.failedItems,
                        errors = progress.errors
                    )
                }
        } else {
            flowOf(BulkOperationResult(
                operationType = BulkOperationType.DELETE,
                totalItems = 0,
                successCount = 0,
                failureCount = 0,
                errors = listOf("No files selected for deletion")
            ))
        }
    }
    
    /**
     * Execute download operation
     */
    fun executeDownload(
        files: List<AccountFileItem>? = null,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationProgress> {
        val filesToDownload = files ?: bulkSelectionManager.getSelectedFiles()
        
        return if (filesToDownload.isNotEmpty()) {
            bulkOperationsService.executeBulkOperation(filesToDownload, BulkOperationType.DOWNLOAD, options)
        } else {
            flowOf(BulkOperationProgress(
                operationId = "empty",
                operationType = BulkOperationType.DOWNLOAD,
                totalItems = 0,
                completedItems = 0,
                failedItems = 0,
                currentItem = null,
                isCompleted = true,
                errors = listOf("No files selected for download")
            ))
        }
    }
    
    /**
     * Execute play operation (get streaming URLs)
     */
    fun executePlay(
        files: List<AccountFileItem>? = null,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationProgress> {
        val filesToPlay = files ?: bulkSelectionManager.getSelectedFiles()
        val playableFiles = filesToPlay.filter { it.isPlayableFile && it.isStreamable }
        
        return if (playableFiles.isNotEmpty()) {
            bulkOperationsService.executeBulkOperation(playableFiles, BulkOperationType.PLAY, options)
        } else {
            flowOf(BulkOperationProgress(
                operationId = "empty",
                operationType = BulkOperationType.PLAY,
                totalItems = 0,
                completedItems = 0,
                failedItems = 0,
                currentItem = null,
                isCompleted = true,
                errors = listOf("No playable files selected")
            ))
        }
    }
    
    /**
     * Get active operations
     */
    fun getActiveOperations(): List<BulkOperationSession> {
        return bulkOperationsService.getActiveOperations()
    }
    
    /**
     * Check if any operations are currently running
     */
    fun hasActiveOperations(): Boolean {
        return getActiveOperations().isNotEmpty()
    }
    
    /**
     * Get combined state for UI
     */
    fun getCombinedState(): Flow<BulkOperationsState> {
        return combine(
            selectionState,
            availableFiles
        ) { selection, files ->
            val statistics = getSelectionStatistics()
            val availableOps = getAvailableOperations()
            val suggestions = getSuggestedSelectionActions()
            val activeOps = getActiveOperations()
            
            BulkOperationsState(
                selectionState = selection,
                availableFiles = files,
                selectionStatistics = statistics,
                availableOperations = availableOps,
                suggestedActions = suggestions,
                activeOperations = activeOps,
                hasActiveOperations = activeOps.isNotEmpty()
            )
        }
    }
}

/**
 * Combined state for bulk operations UI
 */
data class BulkOperationsState(
    val selectionState: BulkSelectionState,
    val availableFiles: List<AccountFileItem>,
    val selectionStatistics: SelectionStatistics,
    val availableOperations: List<BulkOperationType>,
    val suggestedActions: List<SelectionAction>,
    val activeOperations: List<BulkOperationSession>,
    val hasActiveOperations: Boolean
) {
    val canExecuteOperations: Boolean
        get() = selectionState.hasSelection && !hasActiveOperations
    
    val shouldShowSelectionUI: Boolean
        get() = selectionState.isSelectionMode
    
    val shouldShowOperationProgress: Boolean
        get() = hasActiveOperations
}