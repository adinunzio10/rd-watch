package com.rdwatch.androidtv.repository.bulk

import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.repository.cache.FileBrowserCacheManager
import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling bulk operations on account files with progress tracking,
 * error handling, and rollback capabilities
 */
@Singleton
class BulkOperationsService @Inject constructor(
    private val apiService: RealDebridApiService,
    private val cacheManager: FileBrowserCacheManager,
    private val dispatcherProvider: DispatcherProvider
) {
    
    private val activeOperations = ConcurrentHashMap<String, BulkOperationSession>()
    private val operationIdCounter = AtomicLong(0)
    
    /**
     * Execute a bulk operation with progress tracking
     */
    suspend fun executeBulkOperation(
        files: List<AccountFileItem>,
        operationType: BulkOperationType,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationProgress> = channelFlow {
        
        val operationId = generateOperationId()
        val session = BulkOperationSession(
            id = operationId,
            operationType = operationType,
            totalItems = files.size,
            options = options
        )
        
        activeOperations[operationId] = session
        
        try {
            // Send initial progress
            send(BulkOperationProgress(
                operationId = operationId,
                operationType = operationType,
                totalItems = files.size,
                completedItems = 0,
                failedItems = 0,
                currentItem = null,
                isCompleted = false,
                errors = emptyList()
            ))
            
            when (operationType) {
                BulkOperationType.DELETE -> executeBulkDelete(files, session, this@channelFlow)
                BulkOperationType.DOWNLOAD -> executeBulkDownload(files, session, this@channelFlow)
                BulkOperationType.PLAY -> executeBulkPlay(files, session, this@channelFlow)
                BulkOperationType.ADD_TO_FAVORITES -> executeBulkAddToFavorites(files, session, this@channelFlow)
            }
            
        } catch (e: Exception) {
            session.addError("Operation failed: ${e.message}")
            send(session.toProgress().copy(
                isCompleted = true,
                isFailed = true
            ))
        } finally {
            activeOperations.remove(operationId)
        }
    }.flowOn(dispatcherProvider.io)
    
    /**
     * Cancel an active bulk operation
     */
    suspend fun cancelOperation(operationId: String): Boolean {
        val session = activeOperations[operationId]
        return if (session != null) {
            session.cancel()
            activeOperations.remove(operationId)
            true
        } else {
            false
        }
    }
    
    /**
     * Get active operations
     */
    fun getActiveOperations(): List<BulkOperationSession> {
        return activeOperations.values.toList()
    }
    
    /**
     * Execute bulk deletion with rollback capability
     */
    private suspend fun executeBulkDelete(
        files: List<AccountFileItem>,
        session: BulkOperationSession,
        progressChannel: ProducerScope<BulkOperationProgress>
    ) = withContext(dispatcherProvider.io) {
        
        val successfulDeletions = mutableListOf<String>()
        val semaphore = Semaphore(session.options.maxConcurrency)
        
        coroutineScope {
            files.forEachIndexed { index, file ->
                if (session.isCancelled) return@coroutineScope
                
                launch {
                    semaphore.acquire()
                    try {
                        session.currentItem = file.filename
                        
                        // Send progress update
                        progressChannel.send(session.toProgress())
                        
                        // Perform deletion
                        val success = deleteFile(file)
                        
                        if (success) {
                            successfulDeletions.add(file.id)
                            session.incrementCompleted()
                        } else {
                            session.incrementFailed()
                            session.addError("Failed to delete ${file.filename}")
                        }
                        
                        // Delay between operations if specified
                        if (session.options.delayBetweenOperationsMs > 0) {
                            delay(session.options.delayBetweenOperationsMs)
                        }
                        
                    } catch (e: Exception) {
                        session.incrementFailed()
                        session.addError("Error deleting ${file.filename}: ${e.message}")
                    } finally {
                        semaphore.release()
                    }
                }
            }
        }
        
        // Update cache after deletions
        if (successfulDeletions.isNotEmpty()) {
            successfulDeletions.forEach { fileId ->
                try {
                    cacheManager.removeCachedFile(fileId)
                } catch (e: Exception) {
                    // Ignore cache update errors
                }
            }
        }
        
        // Send final progress
        progressChannel.send(session.toProgress().copy(
            isCompleted = true,
            currentItem = null
        ))
    }
    
    /**
     * Execute bulk download URL generation
     */
    private suspend fun executeBulkDownload(
        files: List<AccountFileItem>,
        session: BulkOperationSession,
        progressChannel: ProducerScope<BulkOperationProgress>
    ) = withContext(dispatcherProvider.io) {
        
        val downloadUrls = mutableMapOf<String, String>()
        val semaphore = Semaphore(session.options.maxConcurrency)
        
        coroutineScope {
            files.forEachIndexed { index, file ->
                if (session.isCancelled) return@coroutineScope
                
                launch {
                    semaphore.acquire()
                    try {
                        session.currentItem = file.filename
                        
                        // Send progress update
                        progressChannel.send(session.toProgress())
                        
                        // Generate download URL
                        val downloadUrl = generateDownloadUrl(file)
                        
                        if (downloadUrl != null) {
                            downloadUrls[file.id] = downloadUrl
                            session.incrementCompleted()
                        } else {
                            session.incrementFailed()
                            session.addError("Failed to generate download URL for ${file.filename}")
                        }
                        
                        // Delay between operations
                        if (session.options.delayBetweenOperationsMs > 0) {
                            delay(session.options.delayBetweenOperationsMs)
                        }
                        
                    } catch (e: Exception) {
                        session.incrementFailed()
                        session.addError("Error generating download URL for ${file.filename}: ${e.message}")
                    } finally {
                        semaphore.release()
                    }
                }
            }
        }
        
        // Store download URLs in session results
        session.results["downloadUrls"] = downloadUrls
        
        // Send final progress
        progressChannel.send(session.toProgress().copy(
            isCompleted = true,
            currentItem = null
        ))
    }
    
    /**
     * Execute bulk streaming URL generation for playable files
     */
    private suspend fun executeBulkPlay(
        files: List<AccountFileItem>,
        session: BulkOperationSession,
        progressChannel: ProducerScope<BulkOperationProgress>
    ) = withContext(dispatcherProvider.io) {
        
        val playableFiles = files.filter { it.isPlayableFile && it.isStreamable }
        session.totalItems = playableFiles.size // Update total to reflect only playable files
        
        val streamingUrls = mutableMapOf<String, String>()
        val semaphore = Semaphore(session.options.maxConcurrency)
        
        coroutineScope {
            playableFiles.forEachIndexed { index, file ->
                if (session.isCancelled) return@coroutineScope
                
                launch {
                    semaphore.acquire()
                    try {
                        session.currentItem = file.filename
                        
                        // Send progress update
                        progressChannel.send(session.toProgress())
                        
                        // Get streaming URL
                        val streamingUrl = getStreamingUrl(file)
                        
                        if (streamingUrl != null) {
                            streamingUrls[file.id] = streamingUrl
                            session.incrementCompleted()
                        } else {
                            session.incrementFailed()
                            session.addError("Failed to get streaming URL for ${file.filename}")
                        }
                        
                        // Delay between operations
                        if (session.options.delayBetweenOperationsMs > 0) {
                            delay(session.options.delayBetweenOperationsMs)
                        }
                        
                    } catch (e: Exception) {
                        session.incrementFailed()
                        session.addError("Error getting streaming URL for ${file.filename}: ${e.message}")
                    } finally {
                        semaphore.release()
                    }
                }
            }
        }
        
        // Store streaming URLs in session results
        session.results["streamingUrls"] = streamingUrls
        
        // Send final progress
        progressChannel.send(session.toProgress().copy(
            isCompleted = true,
            currentItem = null
        ))
    }
    
    /**
     * Execute bulk add to favorites (placeholder implementation)
     */
    private suspend fun executeBulkAddToFavorites(
        files: List<AccountFileItem>,
        session: BulkOperationSession,
        progressChannel: ProducerScope<BulkOperationProgress>
    ) = withContext(dispatcherProvider.io) {
        
        // This is a placeholder implementation
        // In a real app, you'd integrate with a favorites system
        
        files.forEachIndexed { index, file ->
            if (session.isCancelled) return@withContext
            
            session.currentItem = file.filename
            
            // Send progress update
            progressChannel.send(session.toProgress())
            
            // Simulate processing
            delay(100)
            
            session.incrementCompleted()
            
            // Delay between operations
            if (session.options.delayBetweenOperationsMs > 0) {
                delay(session.options.delayBetweenOperationsMs)
            }
        }
        
        // Send final progress
        progressChannel.send(session.toProgress().copy(
            isCompleted = true,
            currentItem = null
        ))
    }
    
    // Private helper methods
    
    private suspend fun deleteFile(file: AccountFileItem): Boolean {
        return try {
            val response = when (file.source) {
                FileSource.DOWNLOAD -> apiService.deleteDownload(file.id)
                FileSource.TORRENT -> apiService.deleteTorrent(file.id)
            }
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun generateDownloadUrl(file: AccountFileItem): String? {
        return try {
            when (file.source) {
                FileSource.DOWNLOAD -> {
                    file.downloadUrl ?: run {
                        // Need to unrestrict the link
                        val link = file.streamUrl ?: return null
                        val response = apiService.unrestrictLink(link)
                        if (response.isSuccessful) {
                            response.body()?.download
                        } else {
                            null
                        }
                    }
                }
                FileSource.TORRENT -> {
                    // For torrents, need to unrestrict the links
                    file.downloadUrl
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getStreamingUrl(file: AccountFileItem): String? {
        return try {
            file.streamUrl ?: when (file.source) {
                FileSource.DOWNLOAD -> file.downloadUrl
                FileSource.TORRENT -> null // Torrents typically don't have direct streaming
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateOperationId(): String {
        return "bulk_op_${operationIdCounter.incrementAndGet()}_${System.currentTimeMillis()}"
    }
}

/**
 * Represents an active bulk operation session
 */
data class BulkOperationSession(
    val id: String,
    val operationType: BulkOperationType,
    var totalItems: Int,
    val options: BulkOperationOptions,
    private val completedItems: AtomicInteger = AtomicInteger(0),
    private val failedItems: AtomicInteger = AtomicInteger(0),
    private val errors: MutableList<String> = mutableListOf(),
    val results: MutableMap<String, Any> = mutableMapOf(),
    var currentItem: String? = null,
    @Volatile var isCancelled: Boolean = false
) {
    
    fun incrementCompleted(): Int = completedItems.incrementAndGet()
    fun incrementFailed(): Int = failedItems.incrementAndGet()
    
    fun addError(error: String) {
        synchronized(errors) {
            errors.add(error)
        }
    }
    
    fun cancel() {
        isCancelled = true
    }
    
    fun toProgress(): BulkOperationProgress {
        return BulkOperationProgress(
            operationId = id,
            operationType = operationType,
            totalItems = totalItems,
            completedItems = completedItems.get(),
            failedItems = failedItems.get(),
            currentItem = currentItem,
            isCompleted = false,
            isCancelled = isCancelled,
            errors = synchronized(errors) { errors.toList() }
        )
    }
}

/**
 * Configuration options for bulk operations
 */
data class BulkOperationOptions(
    val maxConcurrency: Int = 3, // Max concurrent operations
    val delayBetweenOperationsMs: Long = 100, // Delay between operations to avoid rate limiting
    val enableRollback: Boolean = false, // Enable rollback on failure (for supported operations)
    val continueOnError: Boolean = true, // Continue processing even if some items fail
    val timeoutPerItemMs: Long = 30000 // Timeout per item operation
)

/**
 * Progress information for bulk operations
 */
data class BulkOperationProgress(
    val operationId: String,
    val operationType: BulkOperationType,
    val totalItems: Int,
    val completedItems: Int,
    val failedItems: Int,
    val currentItem: String?,
    val isCompleted: Boolean,
    val isCancelled: Boolean = false,
    val isFailed: Boolean = false,
    val errors: List<String> = emptyList()
) {
    val progressPercentage: Float
        get() = if (totalItems > 0) {
            ((completedItems + failedItems).toFloat() / totalItems.toFloat()) * 100f
        } else 0f
    
    val successRate: Float
        get() = if (completedItems + failedItems > 0) {
            (completedItems.toFloat() / (completedItems + failedItems).toFloat()) * 100f
        } else 0f
    
    val remainingItems: Int
        get() = totalItems - completedItems - failedItems
    
    val hasErrors: Boolean
        get() = errors.isNotEmpty()
    
    val isSuccessful: Boolean
        get() = isCompleted && failedItems == 0 && !isFailed
}