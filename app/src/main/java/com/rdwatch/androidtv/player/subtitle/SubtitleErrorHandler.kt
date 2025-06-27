package com.rdwatch.androidtv.player.subtitle

import android.util.Log
import com.rdwatch.androidtv.player.subtitle.parser.SubtitleParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles subtitle-related errors and provides fallback mechanisms
 */
@Singleton
class SubtitleErrorHandler @Inject constructor() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _errors = MutableStateFlow<List<SubtitleError>>(emptyList())
    val errors: StateFlow<List<SubtitleError>> = _errors.asStateFlow()
    
    private val _retryAttempts = mutableMapOf<String, Int>()
    private val maxRetryAttempts = 3
    
    /**
     * Handle subtitle loading errors
     */
    fun handleError(
        url: String,
        exception: Exception,
        context: SubtitleErrorContext = SubtitleErrorContext.LOADING
    ): SubtitleError {
        val error = createSubtitleError(url, exception, context)
        addError(error)
        
        Log.w(TAG, "Subtitle error: ${error.message}", exception)
        
        // Determine if retry is appropriate
        if (shouldRetry(error)) {
            scheduleRetry(url, error)
        }
        
        return error
    }
    
    /**
     * Handle parsing errors with detailed information
     */
    fun handleParsingError(
        url: String,
        exception: SubtitleParsingException,
        content: String? = null
    ): SubtitleError {
        val error = SubtitleError(
            id = generateErrorId(),
            url = url,
            type = SubtitleErrorType.PARSING_ERROR,
            message = exception.message ?: "Failed to parse subtitle",
            severity = SubtitleErrorSeverity.HIGH,
            timestamp = System.currentTimeMillis(),
            retryable = false,
            context = SubtitleErrorContext.PARSING,
            details = mapOf(
                "format" to (exception.format?.name ?: "unknown"),
                "lineNumber" to (exception.lineNumber?.toString() ?: "unknown"),
                "contentLength" to (content?.length?.toString() ?: "unknown")
            )
        )
        
        addError(error)
        Log.e(TAG, "Subtitle parsing error: ${error.message}", exception)
        
        return error
    }
    
    /**
     * Handle synchronization errors
     */
    fun handleSyncError(
        message: String,
        cause: Exception? = null
    ): SubtitleError {
        val error = SubtitleError(
            id = generateErrorId(),
            url = "",
            type = SubtitleErrorType.SYNC_ERROR,
            message = message,
            severity = SubtitleErrorSeverity.MEDIUM,
            timestamp = System.currentTimeMillis(),
            retryable = true,
            context = SubtitleErrorContext.SYNCHRONIZATION,
            details = cause?.let { mapOf("cause" to it.message.orEmpty()) } ?: emptyMap()
        )
        
        addError(error)
        Log.w(TAG, "Subtitle sync error: $message", cause)
        
        return error
    }
    
    /**
     * Clear all errors
     */
    fun clearErrors() {
        _errors.value = emptyList()
        _retryAttempts.clear()
    }
    
    /**
     * Clear errors for specific URL
     */
    fun clearErrorsForUrl(url: String) {
        _errors.value = _errors.value.filter { it.url != url }
        _retryAttempts.remove(url)
    }
    
    /**
     * Get errors for specific URL
     */
    fun getErrorsForUrl(url: String): List<SubtitleError> {
        return _errors.value.filter { it.url == url }
    }
    
    /**
     * Check if URL has critical errors
     */
    fun hasCriticalErrors(url: String): Boolean {
        return getErrorsForUrl(url).any { 
            it.severity == SubtitleErrorSeverity.CRITICAL 
        }
    }
    
    /**
     * Get retry count for URL
     */
    fun getRetryCount(url: String): Int {
        return _retryAttempts[url] ?: 0
    }
    
    /**
     * Manually trigger retry for URL
     */
    fun retryUrl(url: String, retryCallback: suspend (String) -> Boolean) {
        scope.launch {
            val currentAttempts = _retryAttempts[url] ?: 0
            if (currentAttempts < maxRetryAttempts) {
                _retryAttempts[url] = currentAttempts + 1
                
                try {
                    val success = retryCallback(url)
                    if (success) {
                        clearErrorsForUrl(url)
                    }
                } catch (e: Exception) {
                    handleError(url, e, SubtitleErrorContext.RETRY)
                }
            }
        }
    }
    
    /**
     * Create appropriate fallback subtitle
     */
    fun createFallbackSubtitle(
        originalUrl: String,
        fallbackUrls: List<String> = emptyList()
    ): SubtitleFallback {
        return SubtitleFallback(
            originalUrl = originalUrl,
            fallbackUrls = fallbackUrls,
            strategy = FallbackStrategy.ALTERNATIVE_SOURCES,
            message = "Using fallback subtitle source"
        )
    }
    
    private fun createSubtitleError(
        url: String,
        exception: Exception,
        context: SubtitleErrorContext
    ): SubtitleError {
        val type = when (exception) {
            is IOException -> when (exception) {
                is SocketTimeoutException -> SubtitleErrorType.TIMEOUT
                is UnknownHostException -> SubtitleErrorType.NETWORK_ERROR
                else -> SubtitleErrorType.IO_ERROR
            }
            is SubtitleParsingException -> SubtitleErrorType.PARSING_ERROR
            is SecurityException -> SubtitleErrorType.PERMISSION_ERROR
            else -> SubtitleErrorType.UNKNOWN
        }
        
        val severity = when (type) {
            SubtitleErrorType.NETWORK_ERROR,
            SubtitleErrorType.TIMEOUT -> SubtitleErrorSeverity.MEDIUM
            SubtitleErrorType.PARSING_ERROR,
            SubtitleErrorType.FORMAT_UNSUPPORTED -> SubtitleErrorSeverity.HIGH
            SubtitleErrorType.PERMISSION_ERROR -> SubtitleErrorSeverity.CRITICAL
            else -> SubtitleErrorSeverity.LOW
        }
        
        val retryable = when (type) {
            SubtitleErrorType.NETWORK_ERROR,
            SubtitleErrorType.TIMEOUT,
            SubtitleErrorType.IO_ERROR -> true
            else -> false
        }
        
        return SubtitleError(
            id = generateErrorId(),
            url = url,
            type = type,
            message = exception.message ?: "Unknown subtitle error",
            severity = severity,
            timestamp = System.currentTimeMillis(),
            retryable = retryable,
            context = context,
            details = extractErrorDetails(exception)
        )
    }
    
    private fun shouldRetry(error: SubtitleError): Boolean {
        if (!error.retryable) return false
        
        val currentAttempts = _retryAttempts[error.url] ?: 0
        return currentAttempts < maxRetryAttempts
    }
    
    private fun scheduleRetry(url: String, error: SubtitleError) {
        scope.launch {
            val currentAttempts = _retryAttempts[url] ?: 0
            _retryAttempts[url] = currentAttempts + 1
            
            // Exponential backoff
            val delayMs = (1000L * (1 shl currentAttempts)).coerceAtMost(30000L)
            kotlinx.coroutines.delay(delayMs)
            
            // This would trigger a retry in the actual implementation
            // For now, we just log the retry attempt
            Log.i(TAG, "Retrying subtitle load for $url (attempt ${currentAttempts + 1})")
        }
    }
    
    private fun addError(error: SubtitleError) {
        val currentErrors = _errors.value.toMutableList()
        currentErrors.add(error)
        
        // Keep only the last 50 errors to prevent memory issues
        if (currentErrors.size > 50) {
            currentErrors.removeAt(0)
        }
        
        _errors.value = currentErrors
    }
    
    private fun extractErrorDetails(exception: Exception): Map<String, String> {
        val details = mutableMapOf<String, String>()
        
        details["exceptionType"] = exception::class.simpleName ?: "Unknown"
        exception.cause?.let { 
            details["cause"] = it.message ?: "Unknown cause"
        }
        
        when (exception) {
            is SubtitleParsingException -> {
                exception.format?.let { details["format"] = it.name }
                exception.lineNumber?.let { details["lineNumber"] = it.toString() }
            }
            is IOException -> {
                details["ioType"] = exception::class.simpleName ?: "IOException"
            }
        }
        
        return details
    }
    
    private fun generateErrorId(): String {
        return "err_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
    
    companion object {
        private const val TAG = "SubtitleErrorHandler"
    }
}

/**
 * Represents a subtitle error
 */
data class SubtitleError(
    val id: String,
    val url: String,
    val type: SubtitleErrorType,
    val message: String,
    val severity: SubtitleErrorSeverity,
    val timestamp: Long,
    val retryable: Boolean,
    val context: SubtitleErrorContext,
    val details: Map<String, String> = emptyMap()
) {
    val formattedTimestamp: String
        get() = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
}

/**
 * Types of subtitle errors
 */
enum class SubtitleErrorType {
    NETWORK_ERROR,
    TIMEOUT,
    IO_ERROR,
    PARSING_ERROR,
    FORMAT_UNSUPPORTED,
    PERMISSION_ERROR,
    SYNC_ERROR,
    UNKNOWN
}

/**
 * Severity levels for errors
 */
enum class SubtitleErrorSeverity(val level: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4)
}

/**
 * Context where the error occurred
 */
enum class SubtitleErrorContext {
    LOADING,
    PARSING,
    SYNCHRONIZATION,
    RENDERING,
    RETRY,
    UNKNOWN
}

/**
 * Fallback configuration for failed subtitles
 */
data class SubtitleFallback(
    val originalUrl: String,
    val fallbackUrls: List<String>,
    val strategy: FallbackStrategy,
    val message: String
)

/**
 * Fallback strategies
 */
enum class FallbackStrategy {
    ALTERNATIVE_SOURCES,
    DISABLE_SUBTITLES,
    USE_EMBEDDED,
    MANUAL_SELECTION
}

/**
 * Error recovery options
 */
data class ErrorRecoveryOptions(
    val enableAutoRetry: Boolean = true,
    val maxRetryAttempts: Int = 3,
    val retryDelayMs: Long = 1000L,
    val enableFallback: Boolean = true,
    val fallbackSources: List<String> = emptyList(),
    val notifyUser: Boolean = true
)