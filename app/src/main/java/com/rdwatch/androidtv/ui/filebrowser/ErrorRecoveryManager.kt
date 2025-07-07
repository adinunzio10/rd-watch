package com.rdwatch.androidtv.ui.filebrowser

import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.filebrowser.repository.FileBrowserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages error recovery and retry logic for file browser operations
 * Provides intelligent retry mechanisms and error analysis
 */
@Singleton
class ErrorRecoveryManager @Inject constructor(
    private val repository: FileBrowserRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    
    private val _recoveryState = MutableStateFlow(RecoveryState())
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()
    
    private val failureHistory = mutableListOf<OperationFailure>()
    
    /**
     * Attempt to recover from a bulk operation failure
     * @param result The failed bulk operation result
     * @param config Recovery configuration
     * @return Result of the recovery attempt
     */
    suspend fun recoverFromBulkFailure(
        result: BulkOperationResult,
        config: RecoveryConfig = RecoveryConfig()
    ): Result<BulkOperationResult> = withContext(dispatcherProvider.io) {
        
        if (result.isCompleteSuccess) {
            return@withContext Result.Success(result)
        }
        
        _recoveryState.value = RecoveryState(
            isRecovering = true,
            operationType = result.operationType,
            totalFailures = result.failedCount,
            currentAttempt = 1,
            maxAttempts = config.maxRetries
        )
        
        val retryableErrors = result.errors.filter { it.isRetryable }
        if (retryableErrors.isEmpty()) {
            _recoveryState.value = RecoveryState(
                isRecovering = false,
                operationType = result.operationType,
                totalFailures = result.failedCount,
                recoveredCount = 0,
                finalResult = result
            )
            return@withContext Result.Success(result)
        }
        
        var recoveredCount = 0
        val remainingErrors = mutableListOf<BulkOperationError>()
        
        for (attempt in 1..config.maxRetries) {
            _recoveryState.value = _recoveryState.value.copy(currentAttempt = attempt)
            
            val itemsToRetry = retryableErrors.map { it.itemId }.toSet()
            
            try {
                val retryResult = when (result.operationType) {
                    BulkOperationType.DELETE -> retryBulkDelete(itemsToRetry, config)
                    BulkOperationType.DOWNLOAD -> retryBulkDownload(itemsToRetry, config)
                    else -> Result.Error(Exception("Unsupported operation type for recovery"))
                }
                
                when (retryResult) {
                    is Result.Success -> {
                        recoveredCount += itemsToRetry.size
                        break // Success, no need to retry
                    }
                    is Result.Error -> {
                        // Analyze the error and decide whether to continue retrying
                        val errorAnalysis = analyzeError(retryResult.exception)
                        if (!errorAnalysis.isRetryable) {
                            break // Non-retryable error, stop trying
                        }
                        
                        // Add to failure history
                        failureHistory.add(
                            OperationFailure(
                                operationType = result.operationType,
                                error = retryResult.exception,
                                timestamp = System.currentTimeMillis(),
                                attempt = attempt
                            )
                        )
                        
                        // Wait before next retry
                        delay(config.retryDelayMs * attempt)
                    }
                    is Result.Loading -> {
                        // Shouldn't happen in recovery context
                        break
                    }
                }
            } catch (e: Exception) {
                failureHistory.add(
                    OperationFailure(
                        operationType = result.operationType,
                        error = e,
                        timestamp = System.currentTimeMillis(),
                        attempt = attempt
                    )
                )
                
                if (attempt == config.maxRetries) {
                    remainingErrors.addAll(retryableErrors)
                } else {
                    delay(config.retryDelayMs * attempt)
                }
            }
        }
        
        val finalResult = BulkOperationResult(
            operationType = result.operationType,
            totalItems = result.totalItems,
            successCount = result.successCount + recoveredCount,
            failedCount = result.failedCount - recoveredCount,
            errors = remainingErrors,
            rollbackActions = result.rollbackActions
        )
        
        _recoveryState.value = RecoveryState(
            isRecovering = false,
            operationType = result.operationType,
            totalFailures = result.failedCount,
            recoveredCount = recoveredCount,
            finalResult = finalResult
        )
        
        Result.Success(finalResult)
    }
    
    /**
     * Analyze error to determine if it's retryable and suggest recovery strategy
     * @param error The error to analyze
     * @return Error analysis result
     */
    fun analyzeError(error: Throwable): ErrorAnalysis {
        val errorMessage = error.message?.lowercase() ?: ""
        
        return when {
            // Network-related errors (usually retryable)
            errorMessage.contains("network") || 
            errorMessage.contains("connection") ||
            errorMessage.contains("timeout") ||
            errorMessage.contains("socket") -> {
                ErrorAnalysis(
                    isRetryable = true,
                    errorType = ErrorType.NETWORK,
                    suggestedAction = "Check network connection and retry",
                    retryDelayMs = 2000L
                )
            }
            
            // API rate limiting (retryable with longer delay)
            errorMessage.contains("rate limit") ||
            errorMessage.contains("too many requests") ||
            errorMessage.contains("429") -> {
                ErrorAnalysis(
                    isRetryable = true,
                    errorType = ErrorType.RATE_LIMIT,
                    suggestedAction = "Wait and retry with exponential backoff",
                    retryDelayMs = 5000L
                )
            }
            
            // Server errors (usually retryable)
            errorMessage.contains("server error") ||
            errorMessage.contains("500") ||
            errorMessage.contains("502") ||
            errorMessage.contains("503") -> {
                ErrorAnalysis(
                    isRetryable = true,
                    errorType = ErrorType.SERVER,
                    suggestedAction = "Server issue, retry later",
                    retryDelayMs = 3000L
                )
            }
            
            // Authentication errors (not retryable without re-auth)
            errorMessage.contains("unauthorized") ||
            errorMessage.contains("authentication") ||
            errorMessage.contains("401") ||
            errorMessage.contains("403") -> {
                ErrorAnalysis(
                    isRetryable = false,
                    errorType = ErrorType.AUTHENTICATION,
                    suggestedAction = "Re-authenticate and try again",
                    retryDelayMs = 0L
                )
            }
            
            // Client errors (usually not retryable)
            errorMessage.contains("bad request") ||
            errorMessage.contains("400") ||
            errorMessage.contains("404") ||
            errorMessage.contains("not found") -> {
                ErrorAnalysis(
                    isRetryable = false,
                    errorType = ErrorType.CLIENT,
                    suggestedAction = "Check request parameters",
                    retryDelayMs = 0L
                )
            }
            
            // Unknown errors (try once more)
            else -> {
                ErrorAnalysis(
                    isRetryable = true,
                    errorType = ErrorType.UNKNOWN,
                    suggestedAction = "Unknown error, try once more",
                    retryDelayMs = 1000L
                )
            }
        }
    }
    
    /**
     * Get failure statistics for analysis
     * @return Failure statistics
     */
    fun getFailureStatistics(): FailureStatistics {
        val now = System.currentTimeMillis()
        val recentFailures = failureHistory.filter { now - it.timestamp < 3600000L } // Last hour
        
        return FailureStatistics(
            totalFailures = failureHistory.size,
            recentFailures = recentFailures.size,
            mostCommonError = getMostCommonError(),
            averageRetryAttempts = failureHistory.map { it.attempt }.average().toFloat(),
            lastFailureTime = failureHistory.maxByOrNull { it.timestamp }?.timestamp ?: 0L
        )
    }
    
    /**
     * Clear failure history
     */
    fun clearFailureHistory() {
        failureHistory.clear()
    }
    
    private suspend fun retryBulkDelete(
        itemIds: Set<String>,
        config: RecoveryConfig
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        repository.bulkDeleteItems(itemIds) { itemId, progress ->
            // Progress callback if needed
        }
    }
    
    private suspend fun retryBulkDownload(
        fileIds: Set<String>,
        config: RecoveryConfig
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        repository.bulkDownloadFiles(fileIds) { fileId, progress ->
            // Progress callback if needed
        }
    }
    
    private fun getMostCommonError(): String? {
        if (failureHistory.isEmpty()) return null
        
        return failureHistory
            .groupBy { it.error.javaClass.simpleName }
            .maxByOrNull { it.value.size }
            ?.key
    }
}

/**
 * Configuration for error recovery
 */
data class RecoveryConfig(
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000L,
    val exponentialBackoff: Boolean = true,
    val maxDelayMs: Long = 30000L,
    val analyzeErrors: Boolean = true
)


/**
 * Analysis of an error
 */
data class ErrorAnalysis(
    val isRetryable: Boolean,
    val errorType: ErrorType,
    val suggestedAction: String,
    val retryDelayMs: Long
)

/**
 * Types of errors that can occur
 */
enum class ErrorType {
    NETWORK,
    RATE_LIMIT,
    SERVER,
    AUTHENTICATION,
    CLIENT,
    UNKNOWN
}

/**
 * Record of an operation failure
 */
data class OperationFailure(
    val operationType: BulkOperationType,
    val error: Throwable,
    val timestamp: Long,
    val attempt: Int
)

/**
 * Statistics about operation failures
 */
data class FailureStatistics(
    val totalFailures: Int,
    val recentFailures: Int,
    val mostCommonError: String?,
    val averageRetryAttempts: Float,
    val lastFailureTime: Long
)