package com.rdwatch.androidtv.scraper.error

import com.rdwatch.androidtv.scraper.models.ManifestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error reporting and monitoring system
 */
@Singleton
class ManifestErrorReporter @Inject constructor(
    private val errorHandler: ManifestErrorHandler
) {
    
    private val reporterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Error tracking
    private val errorHistory = ConcurrentLinkedQueue<ErrorReport>()
    private val errorCounts = ConcurrentHashMap<ErrorCategory, Int>()
    private val errorsByManifest = ConcurrentHashMap<String, MutableList<ErrorReport>>()
    
    // Event streams
    private val errorEventFlow = MutableSharedFlow<ErrorEvent>()
    
    // Configuration
    private var maxHistorySize = 1000
    private var enableDetailedLogging = true
    
    /**
     * Report an error
     */
    fun reportError(
        exception: ManifestException,
        context: ErrorContext = ErrorContext()
    ) {
        reporterScope.launch {
            val report = errorHandler.createErrorReport(exception)
            val enhancedReport = enhanceReport(report, context)
            
            recordError(enhancedReport)
            emitErrorEvent(ErrorEvent.ErrorReported(enhancedReport))
            
            if (enableDetailedLogging) {
                logError(enhancedReport, context)
            }
        }
    }
    
    /**
     * Report successful operation (for tracking recovery)
     */
    fun reportSuccess(
        operation: String,
        context: ErrorContext = ErrorContext()
    ) {
        reporterScope.launch {
            emitErrorEvent(ErrorEvent.OperationSucceeded(operation, context))
        }
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStatistics(): ErrorStatistics {
        val totalErrors = errorHistory.size
        val recentErrors = errorHistory.takeLast(100)
        
        val errorsByCategory = errorCounts.toMap()
        val recentErrorsByCategory = recentErrors
            .groupBy { it.category }
            .mapValues { it.value.size }
        
        val errorsByManifestStats = errorsByManifest.mapValues { it.value.size }
        val mostProblematicManifests = errorsByManifestStats
            .toList()
            .sortedByDescending { it.second }
            .take(10)
            .toMap()
        
        val errorTrends = calculateErrorTrends()
        
        return ErrorStatistics(
            totalErrors = totalErrors,
            errorsByCategory = errorsByCategory,
            recentErrorsByCategory = recentErrorsByCategory,
            errorsByManifest = mostProblematicManifests,
            errorTrends = errorTrends,
            lastErrorTime = errorHistory.lastOrNull()?.timestamp,
            averageErrorsPerHour = calculateAverageErrorsPerHour(),
            commonErrorMessages = getCommonErrorMessages()
        )
    }
    
    /**
     * Get recent errors
     */
    fun getRecentErrors(limit: Int = 50): List<ErrorReport> {
        return errorHistory.takeLast(limit).toList()
    }
    
    /**
     * Get errors for specific manifest
     */
    fun getErrorsForManifest(manifestId: String): List<ErrorReport> {
        return errorsByManifest[manifestId]?.toList() ?: emptyList()
    }
    
    /**
     * Get error patterns
     */
    fun getErrorPatterns(): List<ErrorPattern> {
        val patterns = mutableListOf<ErrorPattern>()
        
        // Group errors by message similarity
        val errorGroups = errorHistory
            .groupBy { it.exception.message }
            .filter { it.value.size > 1 }
        
        errorGroups.forEach { (message, reports) ->
            if (reports.size >= 3) { // Minimum threshold for pattern
                patterns.add(
                    ErrorPattern(
                        pattern = message ?: "Unknown error",
                        occurrences = reports.size,
                        category = reports.first().category,
                        firstOccurrence = reports.minOf { it.timestamp },
                        lastOccurrence = reports.maxOf { it.timestamp },
                        affectedManifests = reports.mapNotNull { it.context?.manifestId }.distinct()
                    )
                )
            }
        }
        
        return patterns.sortedByDescending { it.occurrences }
    }
    
    /**
     * Clear error history
     */
    fun clearHistory() {
        reporterScope.launch {
            errorHistory.clear()
            errorCounts.clear()
            errorsByManifest.clear()
            emitErrorEvent(ErrorEvent.HistoryCleared)
        }
    }
    
    /**
     * Observe error events
     */
    fun observeErrors(): Flow<ErrorEvent> {
        return errorEventFlow.asSharedFlow()
    }
    
    /**
     * Configure reporter
     */
    fun configure(
        maxHistorySize: Int = 1000,
        enableDetailedLogging: Boolean = true
    ) {
        this.maxHistorySize = maxHistorySize
        this.enableDetailedLogging = enableDetailedLogging
    }
    
    /**
     * Export error data for analysis
     */
    fun exportErrorData(): ErrorExport {
        return ErrorExport(
            timestamp = System.currentTimeMillis(),
            statistics = getErrorStatistics(),
            recentErrors = getRecentErrors(100),
            errorPatterns = getErrorPatterns(),
            configuration = ErrorReporterConfig(
                maxHistorySize = maxHistorySize,
                enableDetailedLogging = enableDetailedLogging
            )
        )
    }
    
    // Private helper methods
    
    private fun enhanceReport(report: ErrorReport, context: ErrorContext): ErrorReport {
        return report.copy(
            context = context,
            technicalDetails = report.technicalDetails + mapOf(
                "threadName" to Thread.currentThread().name,
                "systemTime" to System.currentTimeMillis().toString()
            )
        )
    }
    
    private fun recordError(report: ErrorReport) {
        // Add to history
        errorHistory.offer(report)
        
        // Maintain size limit
        while (errorHistory.size > maxHistorySize) {
            errorHistory.poll()
        }
        
        // Update counters
        errorCounts.merge(report.category, 1) { old, new -> old + new }
        
        // Track by manifest if available
        report.context?.manifestId?.let { manifestId ->
            errorsByManifest.computeIfAbsent(manifestId) { mutableListOf() }.add(report)
        }
    }
    
    private fun emitErrorEvent(event: ErrorEvent) {
        errorEventFlow.tryEmit(event)
    }
    
    private fun logError(report: ErrorReport, context: ErrorContext) {
        // In a real implementation, this would use proper logging
        println("ERROR REPORT: ${report.formattedTimestamp}")
        println("  Category: ${report.category}")
        println("  Message: ${report.exception.message}")
        println("  User Message: ${report.userMessage}")
        println("  Context: ${context}")
        println("  Technical Details: ${report.technicalDetails}")
        println("  Suggested Actions: ${report.suggestedActions}")
        println("---")
    }
    
    private fun calculateErrorTrends(): ErrorTrends {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000L
        val oneDayAgo = now - 86400000L
        
        val lastHourErrors = errorHistory.count { it.timestamp >= oneHourAgo }
        val lastDayErrors = errorHistory.count { it.timestamp >= oneDayAgo }
        
        return ErrorTrends(
            lastHour = lastHourErrors,
            lastDay = lastDayErrors,
            trend = when {
                lastHourErrors == 0 -> TrendDirection.STABLE
                lastHourErrors > lastDayErrors / 24 * 2 -> TrendDirection.INCREASING
                lastHourErrors < lastDayErrors / 24 / 2 -> TrendDirection.DECREASING
                else -> TrendDirection.STABLE
            }
        )
    }
    
    private fun calculateAverageErrorsPerHour(): Double {
        if (errorHistory.isEmpty()) return 0.0
        
        val oldestTime = errorHistory.first().timestamp
        val newestTime = errorHistory.last().timestamp
        val timeSpanHours = (newestTime - oldestTime) / 3600000.0
        
        return if (timeSpanHours > 0) errorHistory.size / timeSpanHours else 0.0
    }
    
    private fun getCommonErrorMessages(): List<Pair<String, Int>> {
        return errorHistory
            .groupBy { it.exception.message ?: "Unknown" }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
    }
}

/**
 * Error context information
 */
data class ErrorContext(
    val manifestId: String? = null,
    val operation: String? = null,
    val url: String? = null,
    val userId: String? = null,
    val deviceInfo: String? = null,
    val additionalData: Map<String, String> = emptyMap()
)

/**
 * Enhanced error report with context
 */
data class ErrorReport(
    val timestamp: Long,
    val category: ErrorCategory,
    val exception: ManifestException,
    val userMessage: String,
    val suggestedActions: List<String>,
    val isRetryable: Boolean,
    val technicalDetails: Map<String, String>,
    val context: ErrorContext? = null
) {
    val formattedTimestamp: String
        get() = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
}

/**
 * Error events for reactive monitoring
 */
sealed class ErrorEvent {
    data class ErrorReported(val report: ErrorReport) : ErrorEvent()
    data class OperationSucceeded(val operation: String, val context: ErrorContext) : ErrorEvent()
    object HistoryCleared : ErrorEvent()
}

/**
 * Error statistics
 */
data class ErrorStatistics(
    val totalErrors: Int,
    val errorsByCategory: Map<ErrorCategory, Int>,
    val recentErrorsByCategory: Map<ErrorCategory, Int>,
    val errorsByManifest: Map<String, Int>,
    val errorTrends: ErrorTrends,
    val lastErrorTime: Long?,
    val averageErrorsPerHour: Double,
    val commonErrorMessages: List<Pair<String, Int>>
)

/**
 * Error trends analysis
 */
data class ErrorTrends(
    val lastHour: Int,
    val lastDay: Int,
    val trend: TrendDirection
)

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

/**
 * Error pattern detection
 */
data class ErrorPattern(
    val pattern: String,
    val occurrences: Int,
    val category: ErrorCategory,
    val firstOccurrence: Long,
    val lastOccurrence: Long,
    val affectedManifests: List<String>
)

/**
 * Error data export
 */
data class ErrorExport(
    val timestamp: Long,
    val statistics: ErrorStatistics,
    val recentErrors: List<ErrorReport>,
    val errorPatterns: List<ErrorPattern>,
    val configuration: ErrorReporterConfig
)

/**
 * Error reporter configuration
 */
data class ErrorReporterConfig(
    val maxHistorySize: Int,
    val enableDetailedLogging: Boolean
)