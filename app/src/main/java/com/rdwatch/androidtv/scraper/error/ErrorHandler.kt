package com.rdwatch.androidtv.scraper.error

import com.rdwatch.androidtv.scraper.models.ManifestCacheException
import com.rdwatch.androidtv.scraper.models.ManifestException
import com.rdwatch.androidtv.scraper.models.ManifestNetworkException
import com.rdwatch.androidtv.scraper.models.ManifestParsingException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ManifestStorageException
import com.rdwatch.androidtv.scraper.models.ManifestValidationException
import kotlinx.coroutines.delay
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive error handler with retry logic and recovery strategies
 */
@Singleton
class ManifestErrorHandler @Inject constructor() {
    
    /**
     * Handle errors with appropriate recovery strategies
     */
    suspend fun <T> handleWithRecovery(
        operation: suspend () -> ManifestResult<T>,
        config: ErrorHandlingConfig = ErrorHandlingConfig()
    ): ManifestResult<T> {
        var lastException: ManifestException? = null
        
        repeat(config.maxRetries + 1) { attempt ->
            try {
                val result = operation()
                
                when (result) {
                    is ManifestResult.Success -> return result
                    is ManifestResult.Error -> {
                        lastException = result.exception
                        
                        // Check if error is retryable
                        if (!isRetryableError(result.exception)) {
                            return result
                        }
                        
                        // Apply recovery strategy if available
                        val recoveryResult = tryRecovery<T>(result.exception, config)
                        if (recoveryResult != null) {
                            return recoveryResult
                        }
                        
                        // Wait before retry (exponential backoff)
                        if (attempt < config.maxRetries) {
                            val delayMs = calculateBackoffDelay(attempt, config)
                            delay(delayMs)
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = when (e) {
                    is ManifestException -> e
                    else -> ManifestStorageException("Unexpected error: ${e.message}", e, operation = "handleWithRecovery")
                }
                
                if (!isRetryableError(lastException) || attempt == config.maxRetries) {
                    return ManifestResult.Error(lastException)
                }
                
                val delayMs = calculateBackoffDelay(attempt, config)
                delay(delayMs)
            }
        }
        
        // All retries exhausted
        return ManifestResult.Error(
            lastException ?: ManifestStorageException("Unknown error after ${config.maxRetries} retries", operation = "handleWithRecovery")
        )
    }
    
    /**
     * Categorize errors for better handling
     */
    fun categorizeError(exception: ManifestException): ErrorCategory {
        return when (exception) {
            is ManifestNetworkException -> when {
                exception.statusCode in 400..499 -> ErrorCategory.CLIENT_ERROR
                exception.statusCode in 500..599 -> ErrorCategory.SERVER_ERROR
                exception.cause is SocketTimeoutException -> ErrorCategory.TIMEOUT
                exception.cause is UnknownHostException -> ErrorCategory.NETWORK_CONNECTIVITY
                else -> ErrorCategory.NETWORK_CONNECTIVITY
            }
            is ManifestParsingException -> ErrorCategory.DATA_FORMAT
            is ManifestValidationException -> ErrorCategory.VALIDATION
            is ManifestStorageException -> ErrorCategory.STORAGE
            is ManifestCacheException -> ErrorCategory.CACHE
            else -> ErrorCategory.UNKNOWN
        }
    }
    
    /**
     * Generate user-friendly error messages
     */
    fun getUserFriendlyMessage(exception: ManifestException): String {
        return when (categorizeError(exception)) {
            ErrorCategory.NETWORK_CONNECTIVITY -> "Unable to connect to the server. Please check your internet connection."
            ErrorCategory.TIMEOUT -> "The request timed out. The server may be temporarily unavailable."
            ErrorCategory.SERVER_ERROR -> "The server is experiencing issues. Please try again later."
            ErrorCategory.CLIENT_ERROR -> "The request was invalid. Please check the manifest URL."
            ErrorCategory.DATA_FORMAT -> "The manifest file format is invalid or corrupted."
            ErrorCategory.VALIDATION -> "The manifest contains invalid data or missing required fields."
            ErrorCategory.STORAGE -> "Unable to save or retrieve manifest data from local storage."
            ErrorCategory.CACHE -> "Cache operation failed. The manifest may still be available from the server."
            ErrorCategory.UNKNOWN -> "An unexpected error occurred. Please try again."
        }
    }
    
    /**
     * Get suggested actions for error recovery
     */
    fun getSuggestedActions(exception: ManifestException): List<String> {
        return when (categorizeError(exception)) {
            ErrorCategory.NETWORK_CONNECTIVITY -> listOf(
                "Check your internet connection",
                "Try again in a few moments",
                "Verify the manifest URL is correct"
            )
            ErrorCategory.TIMEOUT -> listOf(
                "Try again with a longer timeout",
                "Check if the server is responding",
                "Contact the manifest provider"
            )
            ErrorCategory.SERVER_ERROR -> listOf(
                "Wait a few minutes and try again",
                "Contact the manifest provider",
                "Check if there's a service status page"
            )
            ErrorCategory.CLIENT_ERROR -> listOf(
                "Verify the manifest URL is correct",
                "Check if authentication is required",
                "Ensure the manifest is publicly accessible"
            )
            ErrorCategory.DATA_FORMAT -> listOf(
                "Verify the manifest file is valid JSON",
                "Check the manifest follows Stremio format",
                "Contact the manifest provider about the format issue"
            )
            ErrorCategory.VALIDATION -> listOf(
                "Check that all required fields are present",
                "Verify field formats are correct",
                "Review the validation error details"
            )
            ErrorCategory.STORAGE -> listOf(
                "Check available storage space",
                "Restart the application",
                "Clear the application cache"
            )
            ErrorCategory.CACHE -> listOf(
                "Clear the cache and try again",
                "The data may still be available from the server"
            )
            ErrorCategory.UNKNOWN -> listOf(
                "Try the operation again",
                "Restart the application",
                "Report this issue if it persists"
            )
        }
    }
    
    /**
     * Check if an error is retryable
     */
    private fun isRetryableError(exception: ManifestException): Boolean {
        return when (categorizeError(exception)) {
            ErrorCategory.NETWORK_CONNECTIVITY,
            ErrorCategory.TIMEOUT,
            ErrorCategory.SERVER_ERROR,
            ErrorCategory.CACHE -> true
            ErrorCategory.CLIENT_ERROR,
            ErrorCategory.DATA_FORMAT,
            ErrorCategory.VALIDATION,
            ErrorCategory.STORAGE,
            ErrorCategory.UNKNOWN -> false
        }
    }
    
    /**
     * Try recovery strategies
     */
    private suspend fun <T> tryRecovery(
        exception: ManifestException,
        config: ErrorHandlingConfig
    ): ManifestResult<T>? {
        return when (categorizeError(exception)) {
            ErrorCategory.CACHE -> {
                // For cache errors, we could try to bypass cache
                null // Let the calling code handle fallback
            }
            ErrorCategory.NETWORK_CONNECTIVITY -> {
                // Could try alternative endpoints if available
                null
            }
            else -> null
        }
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoffDelay(attempt: Int, config: ErrorHandlingConfig): Long {
        val baseDelay = config.baseDelayMs
        val maxDelay = config.maxDelayMs
        val jitter = config.jitterMs
        
        val exponentialDelay = (baseDelay * Math.pow(config.backoffMultiplier, attempt.toDouble())).toLong()
        val delayWithJitter = exponentialDelay + (Math.random() * jitter).toLong()
        
        return minOf(delayWithJitter, maxDelay)
    }
    
    /**
     * Create detailed error report
     */
    fun createErrorReport(exception: ManifestException): ErrorReport {
        val category = categorizeError(exception)
        val timestamp = System.currentTimeMillis()
        
        return ErrorReport(
            timestamp = timestamp,
            category = category,
            exception = exception,
            userMessage = getUserFriendlyMessage(exception),
            suggestedActions = getSuggestedActions(exception),
            isRetryable = isRetryableError(exception),
            technicalDetails = extractTechnicalDetails(exception)
        )
    }
    
    private fun extractTechnicalDetails(exception: ManifestException): Map<String, String> {
        val details = mutableMapOf<String, String>()
        
        details["exceptionType"] = exception::class.simpleName ?: "Unknown"
        details["message"] = exception.message ?: "No message"
        
        when (exception) {
            is ManifestNetworkException -> {
                exception.url?.let { details["url"] = it }
                exception.statusCode?.let { details["statusCode"] = it.toString() }
            }
            is ManifestParsingException -> {
                exception.url?.let { details["url"] = it }
                exception.format?.let { details["format"] = it }
            }
            is ManifestValidationException -> {
                details["validationErrorCount"] = exception.validationErrors.size.toString()
                details["firstValidationError"] = exception.validationErrors.firstOrNull()?.message ?: "No details"
            }
            is ManifestStorageException -> {
                exception.operation?.let { details["operation"] = it }
            }
            is ManifestCacheException -> {
                exception.cacheKey?.let { details["cacheKey"] = it }
            }
        }
        
        exception.cause?.let { cause ->
            details["causeType"] = cause::class.simpleName ?: "Unknown"
            details["causeMessage"] = cause.message ?: "No message"
        }
        
        return details
    }
}

/**
 * Error handling configuration
 */
data class ErrorHandlingConfig(
    val maxRetries: Int = 3,
    val baseDelayMs: Long = 1000L,
    val maxDelayMs: Long = 30000L,
    val backoffMultiplier: Double = 2.0,
    val jitterMs: Long = 500L,
    val enableRecovery: Boolean = true
) {
    companion object {
        fun forNetworkOperations() = ErrorHandlingConfig(
            maxRetries = 3,
            baseDelayMs = 2000L,
            maxDelayMs = 30000L
        )
        
        fun forStorageOperations() = ErrorHandlingConfig(
            maxRetries = 2,
            baseDelayMs = 500L,
            maxDelayMs = 5000L
        )
        
        fun forCacheOperations() = ErrorHandlingConfig(
            maxRetries = 1,
            baseDelayMs = 100L,
            maxDelayMs = 1000L
        )
        
        fun noRetry() = ErrorHandlingConfig(
            maxRetries = 0
        )
    }
}

/**
 * Error categories for handling
 */
enum class ErrorCategory {
    NETWORK_CONNECTIVITY,
    TIMEOUT,
    SERVER_ERROR,
    CLIENT_ERROR,
    DATA_FORMAT,
    VALIDATION,
    STORAGE,
    CACHE,
    UNKNOWN
}

/**
 * Comprehensive error report
 */
data class ErrorHandlerReport(
    val timestamp: Long,
    val category: ErrorCategory,
    val exception: ManifestException,
    val userMessage: String,
    val suggestedActions: List<String>,
    val isRetryable: Boolean,
    val technicalDetails: Map<String, String>
) {
    val formattedTimestamp: String
        get() = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
}