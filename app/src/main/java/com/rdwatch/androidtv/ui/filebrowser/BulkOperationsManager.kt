package com.rdwatch.androidtv.ui.filebrowser

import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.filebrowser.repository.FileBrowserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages bulk operations for file browser with progress tracking, 
 * error handling, and rollback capabilities
 */
@Singleton
class BulkOperationsManager @Inject constructor(
    private val repository: FileBrowserRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    
    private val _operationState = MutableStateFlow(BulkOperationState())
    val operationState: StateFlow<BulkOperationState> = _operationState.asStateFlow()
    
    private val _operationProgress = MutableSharedFlow<BulkOperationProgress>()
    val operationProgress: SharedFlow<BulkOperationProgress> = _operationProgress.asSharedFlow()
    
    private var currentJob: Job? = null
    private val rollbackActions = mutableListOf<RollbackAction>()
    private val processingItems = ConcurrentHashMap<String, BulkOperationItemStatus>()
    
    /**
     * Perform bulk delete operation with progress tracking
     */
    suspend fun bulkDelete(
        itemIds: Set<String>,
        itemNames: Map<String, String>,
        config: BulkOperationConfig = BulkOperationConfig()
    ): BulkOperationResult = withContext(dispatcherProvider.io) {
        startBulkOperation(BulkOperationType.DELETE, itemIds.size, config)
        
        val completedItems = AtomicInteger(0)
        val failedItems = AtomicInteger(0)
        val errors = mutableListOf<BulkOperationError>()
        
        try {
            // Process items in batches to avoid overwhelming the API
            itemIds.chunked(config.batchSize).forEach { batch ->
                ensureActive() // Check for cancellation
                
                // Process batch items in parallel
                val batchResults = batch.map { itemId ->
                    async {
                        processDeleteItem(
                            itemId = itemId,
                            itemName = itemNames[itemId] ?: "Unknown",
                            config = config,
                            onProgress = { progress ->
                                _operationProgress.tryEmit(progress)
                            }
                        )
                    }
                }.awaitAll()
                
                // Update counters and collect errors
                batchResults.forEach { result ->
                    when (result) {
                        is ItemOperationResult.Success -> {
                            completedItems.incrementAndGet()
                            if (config.enableRollback) {
                                rollbackActions.add(
                                    RollbackAction(
                                        itemId = result.itemId,
                                        itemName = result.itemName,
                                        actionType = RollbackActionType.RESTORE_DELETED,
                                        originalData = result.originalData
                                    )
                                )
                            }
                        }
                        is ItemOperationResult.Error -> {
                            failedItems.incrementAndGet()
                            errors.add(
                                BulkOperationError(
                                    itemId = result.itemId,
                                    itemName = result.itemName,
                                    error = result.error,
                                    isRetryable = result.isRetryable
                                )
                            )
                        }
                    }
                }
                
                // Update state
                updateOperationState(
                    completedItems = completedItems.get(),
                    failedItems = failedItems.get(),
                    errors = errors.toList()
                )
            }
            
            // Handle partial failures
            if (errors.isNotEmpty() && !config.continueOnError) {
                // Rollback successful operations
                if (config.enableRollback && rollbackActions.isNotEmpty()) {
                    performRollback(rollbackActions.toList())
                }
            }
            
        } catch (e: CancellationException) {
            // Operation was cancelled
            if (config.enableRollback && rollbackActions.isNotEmpty()) {
                performRollback(rollbackActions.toList())
            }
            throw e
        } catch (e: Exception) {
            // Unexpected error
            if (config.enableRollback && rollbackActions.isNotEmpty()) {
                performRollback(rollbackActions.toList())
            }
            throw e
        } finally {
            completeOperation()
        }
        
        BulkOperationResult(
            operationType = BulkOperationType.DELETE,
            totalItems = itemIds.size,
            successCount = completedItems.get(),
            failedCount = failedItems.get(),
            errors = errors.toList(),
            rollbackActions = if (config.enableRollback) rollbackActions.toList() else emptyList()
        )
    }
    
    /**
     * Perform bulk download operation with progress tracking
     */
    suspend fun bulkDownload(
        fileIds: Set<String>,
        fileNames: Map<String, String>,
        config: BulkOperationConfig = BulkOperationConfig()
    ): BulkOperationResult = withContext(dispatcherProvider.io) {
        startBulkOperation(BulkOperationType.DOWNLOAD, fileIds.size, config)
        
        val completedItems = AtomicInteger(0)
        val failedItems = AtomicInteger(0)
        val errors = mutableListOf<BulkOperationError>()
        
        try {
            // Process items in batches
            fileIds.chunked(config.batchSize).forEach { batch ->
                ensureActive()
                
                val batchResults = batch.map { fileId ->
                    async {
                        processDownloadItem(
                            fileId = fileId,
                            fileName = fileNames[fileId] ?: "Unknown",
                            config = config,
                            onProgress = { progress ->
                                _operationProgress.tryEmit(progress)
                            }
                        )
                    }
                }.awaitAll()
                
                // Update counters and collect errors
                batchResults.forEach { result ->
                    when (result) {
                        is ItemOperationResult.Success -> {
                            completedItems.incrementAndGet()
                        }
                        is ItemOperationResult.Error -> {
                            failedItems.incrementAndGet()
                            errors.add(
                                BulkOperationError(
                                    itemId = result.itemId,
                                    itemName = result.itemName,
                                    error = result.error,
                                    isRetryable = result.isRetryable
                                )
                            )
                        }
                    }
                }
                
                updateOperationState(
                    completedItems = completedItems.get(),
                    failedItems = failedItems.get(),
                    errors = errors.toList()
                )
            }
            
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e
        } finally {
            completeOperation()
        }
        
        BulkOperationResult(
            operationType = BulkOperationType.DOWNLOAD,
            totalItems = fileIds.size,
            successCount = completedItems.get(),
            failedCount = failedItems.get(),
            errors = errors.toList()
        )
    }
    
    /**
     * Cancel the current bulk operation
     */
    fun cancelOperation() {
        currentJob?.cancel()
        _operationState.value = _operationState.value.copy(
            isRunning = false,
            canCancel = false
        )
    }
    
    /**
     * Retry failed items from the last operation
     */
    suspend fun retryFailedItems(config: BulkOperationConfig = BulkOperationConfig()) {
        val currentState = _operationState.value
        if (!currentState.canRetry || currentState.errors.isEmpty()) return
        
        val failedItems = currentState.errors.filter { it.isRetryable }
        if (failedItems.isEmpty()) return
        
        val itemIds = failedItems.map { it.itemId }.toSet()
        val itemNames = failedItems.associate { it.itemId to it.itemName }
        
        when (currentState.operationType) {
            BulkOperationType.DELETE -> bulkDelete(itemIds, itemNames, config)
            BulkOperationType.DOWNLOAD -> bulkDownload(itemIds, itemNames, config)
            else -> {} // Other operations not implemented yet
        }
    }
    
    /**
     * Perform rollback of successful operations
     */
    suspend fun performRollback(actions: List<RollbackAction>) {
        // Implementation would depend on the specific rollback actions
        // For now, this is a placeholder
        actions.forEach { action ->
            when (action.actionType) {
                RollbackActionType.RESTORE_DELETED -> {
                    // Restore deleted item (would need backup/restore mechanism)
                }
                RollbackActionType.CANCEL_DOWNLOAD -> {
                    // Cancel ongoing download
                }
                else -> {} // Other rollback types
            }
        }
    }
    
    private fun startBulkOperation(
        operationType: BulkOperationType,
        totalItems: Int,
        config: BulkOperationConfig
    ) {
        rollbackActions.clear()
        processingItems.clear()
        
        currentJob = CoroutineScope(dispatcherProvider.io).launch {
            _operationState.value = BulkOperationState(
                isRunning = true,
                operationType = operationType,
                totalItems = totalItems,
                completedItems = 0,
                failedItems = 0,
                errors = emptyList(),
                canCancel = true,
                canRetry = false,
                rollbackAvailable = config.enableRollback
            )
        }
    }
    
    private fun updateOperationState(
        completedItems: Int,
        failedItems: Int,
        errors: List<BulkOperationError>
    ) {
        _operationState.value = _operationState.value.copy(
            completedItems = completedItems,
            failedItems = failedItems,
            errors = errors
        )
    }
    
    private fun completeOperation() {
        _operationState.value = _operationState.value.copy(
            isRunning = false,
            canCancel = false,
            canRetry = _operationState.value.hasErrors
        )
    }
    
    private suspend fun processDeleteItem(
        itemId: String,
        itemName: String,
        config: BulkOperationConfig,
        onProgress: (BulkOperationProgress) -> Unit
    ): ItemOperationResult {
        processingItems[itemId] = BulkOperationItemStatus.IN_PROGRESS
        
        onProgress(
            BulkOperationProgress(
                itemId = itemId,
                itemName = itemName,
                progress = 0f,
                status = BulkOperationItemStatus.IN_PROGRESS
            )
        )
        
        var retryCount = 0
        while (retryCount <= config.maxRetries) {
            try {
                val result = repository.deleteItems(setOf(itemId))
                
                return when (result) {
                    is Result.Success -> {
                        processingItems[itemId] = BulkOperationItemStatus.COMPLETED
                        onProgress(
                            BulkOperationProgress(
                                itemId = itemId,
                                itemName = itemName,
                                progress = 1f,
                                status = BulkOperationItemStatus.COMPLETED
                            )
                        )
                        ItemOperationResult.Success(
                            itemId = itemId,
                            itemName = itemName,
                            originalData = null // Would store original data for rollback
                        )
                    }
                    is Result.Error -> {
                        if (retryCount < config.maxRetries) {
                            retryCount++
                            processingItems[itemId] = BulkOperationItemStatus.RETRYING
                            onProgress(
                                BulkOperationProgress(
                                    itemId = itemId,
                                    itemName = itemName,
                                    progress = 0f,
                                    status = BulkOperationItemStatus.RETRYING
                                )
                            )
                            delay(config.retryDelayMs)
                            continue
                        } else {
                            processingItems[itemId] = BulkOperationItemStatus.FAILED
                            onProgress(
                                BulkOperationProgress(
                                    itemId = itemId,
                                    itemName = itemName,
                                    progress = 0f,
                                    status = BulkOperationItemStatus.FAILED,
                                    error = result.exception.message
                                )
                            )
                            ItemOperationResult.Error(
                                itemId = itemId,
                                itemName = itemName,
                                error = result.exception.message ?: "Unknown error",
                                isRetryable = true
                            )
                        }
                    }
                    is Result.Loading -> {
                        // Shouldn't happen, but handle gracefully
                        delay(100)
                        continue
                    }
                }
            } catch (e: Exception) {
                if (retryCount < config.maxRetries) {
                    retryCount++
                    delay(config.retryDelayMs)
                    continue
                } else {
                    processingItems[itemId] = BulkOperationItemStatus.FAILED
                    onProgress(
                        BulkOperationProgress(
                            itemId = itemId,
                            itemName = itemName,
                            progress = 0f,
                            status = BulkOperationItemStatus.FAILED,
                            error = e.message
                        )
                    )
                    return ItemOperationResult.Error(
                        itemId = itemId,
                        itemName = itemName,
                        error = e.message ?: "Unknown error",
                        isRetryable = true
                    )
                }
            }
        }
        
        // This should never be reached, but provide fallback
        return ItemOperationResult.Error(
            itemId = itemId,
            itemName = itemName,
            error = "Max retries exceeded",
            isRetryable = false
        )
    }
    
    private suspend fun processDownloadItem(
        fileId: String,
        fileName: String,
        config: BulkOperationConfig,
        onProgress: (BulkOperationProgress) -> Unit
    ): ItemOperationResult {
        processingItems[fileId] = BulkOperationItemStatus.IN_PROGRESS
        
        onProgress(
            BulkOperationProgress(
                itemId = fileId,
                itemName = fileName,
                progress = 0f,
                status = BulkOperationItemStatus.IN_PROGRESS
            )
        )
        
        var retryCount = 0
        while (retryCount <= config.maxRetries) {
            try {
                val result = repository.downloadFiles(setOf(fileId))
                
                return when (result) {
                    is Result.Success -> {
                        processingItems[fileId] = BulkOperationItemStatus.COMPLETED
                        onProgress(
                            BulkOperationProgress(
                                itemId = fileId,
                                itemName = fileName,
                                progress = 1f,
                                status = BulkOperationItemStatus.COMPLETED
                            )
                        )
                        ItemOperationResult.Success(
                            itemId = fileId,
                            itemName = fileName,
                            originalData = null
                        )
                    }
                    is Result.Error -> {
                        if (retryCount < config.maxRetries) {
                            retryCount++
                            processingItems[fileId] = BulkOperationItemStatus.RETRYING
                            onProgress(
                                BulkOperationProgress(
                                    itemId = fileId,
                                    itemName = fileName,
                                    progress = 0f,
                                    status = BulkOperationItemStatus.RETRYING
                                )
                            )
                            delay(config.retryDelayMs)
                            continue
                        } else {
                            processingItems[fileId] = BulkOperationItemStatus.FAILED
                            onProgress(
                                BulkOperationProgress(
                                    itemId = fileId,
                                    itemName = fileName,
                                    progress = 0f,
                                    status = BulkOperationItemStatus.FAILED,
                                    error = result.exception.message
                                )
                            )
                            ItemOperationResult.Error(
                                itemId = fileId,
                                itemName = fileName,
                                error = result.exception.message ?: "Unknown error",
                                isRetryable = true
                            )
                        }
                    }
                    is Result.Loading -> {
                        delay(100)
                        continue
                    }
                }
            } catch (e: Exception) {
                if (retryCount < config.maxRetries) {
                    retryCount++
                    delay(config.retryDelayMs)
                    continue
                } else {
                    processingItems[fileId] = BulkOperationItemStatus.FAILED
                    onProgress(
                        BulkOperationProgress(
                            itemId = fileId,
                            itemName = fileName,
                            progress = 0f,
                            status = BulkOperationItemStatus.FAILED,
                            error = e.message
                        )
                    )
                    return ItemOperationResult.Error(
                        itemId = fileId,
                        itemName = fileName,
                        error = e.message ?: "Unknown error",
                        isRetryable = true
                    )
                }
            }
        }
        
        return ItemOperationResult.Error(
            itemId = fileId,
            itemName = fileName,
            error = "Max retries exceeded",
            isRetryable = false
        )
    }
}

/**
 * Result of processing a single item in a bulk operation
 */
private sealed class ItemOperationResult {
    data class Success(
        val itemId: String,
        val itemName: String,
        val originalData: String? = null
    ) : ItemOperationResult()
    
    data class Error(
        val itemId: String,
        val itemName: String,
        val error: String,
        val isRetryable: Boolean = true
    ) : ItemOperationResult()
}