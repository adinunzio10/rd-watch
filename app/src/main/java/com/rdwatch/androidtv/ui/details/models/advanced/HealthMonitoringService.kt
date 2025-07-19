package com.rdwatch.androidtv.ui.details.models.advanced

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Background service for real-time health monitoring
 * Provides continuous health updates, degradation alerts, and performance optimization
 */
class HealthMonitoringService : Service() {
    private val binder = HealthMonitoringBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Core components
    private val healthMonitor = HealthMonitor()
    private val seasonPackDetector = SeasonPackDetector()

    // Monitoring state
    private val monitoredSources = ConcurrentHashMap<String, MonitoredSource>()
    private val _healthAlerts = MutableSharedFlow<HealthAlert>()
    val healthAlerts: SharedFlow<HealthAlert> = _healthAlerts.asSharedFlow()

    // Configuration
    private val monitoringIntervalMs = 60_000L // 1 minute
    private val alertThresholdScore = 30 // Alert if health drops below 30
    private val maxMonitoredSources = 100 // Limit to prevent memory issues

    // Performance tracking
    private val performanceMetrics = PerformanceMetrics()

    override fun onCreate() {
        super.onCreate()
        startMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        healthMonitor.cleanup()
    }

    /**
     * Binder for service communication
     */
    inner class HealthMonitoringBinder : Binder() {
        fun getService(): HealthMonitoringService = this@HealthMonitoringService
    }

    /**
     * Start monitoring a source
     */
    fun startMonitoringSource(sourceMetadata: SourceMetadata) {
        val sourceId = sourceMetadata.id

        // Limit number of monitored sources
        if (monitoredSources.size >= maxMonitoredSources) {
            removeOldestMonitoredSource()
        }

        val monitoredSource =
            MonitoredSource(
                metadata = sourceMetadata,
                startTime = Date(),
                lastHealthCheck = null,
                healthHistory = mutableListOf(),
                alertCount = 0,
            )

        monitoredSources[sourceId] = monitoredSource

        // Immediate health check
        scope.launch {
            performHealthCheck(sourceId)
        }
    }

    /**
     * Stop monitoring a source
     */
    fun stopMonitoringSource(sourceId: String) {
        monitoredSources.remove(sourceId)
    }

    /**
     * Get current health data for a source
     */
    fun getHealthData(sourceId: String): HealthData? {
        return healthMonitor.getCachedHealthData(sourceId)
    }

    /**
     * Get season pack information for a source
     */
    fun getSeasonPackInfo(sourceMetadata: SourceMetadata): SeasonPackInfo {
        val filename = sourceMetadata.file.name ?: ""
        return seasonPackDetector.analyzeSeasonPack(filename, sourceMetadata.file.sizeInBytes)
    }

    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): PerformanceMetrics = performanceMetrics

    /**
     * Get all monitored sources
     */
    fun getMonitoredSources(): Map<String, MonitoredSource> = monitoredSources.toMap()

    /**
     * Start background monitoring
     */
    private fun startMonitoring() {
        // Health monitoring loop
        scope.launch {
            while (true) {
                try {
                    performScheduledHealthChecks()
                    delay(monitoringIntervalMs)
                } catch (e: Exception) {
                    // Log error but continue monitoring
                    delay(monitoringIntervalMs)
                }
            }
        }

        // Performance optimization loop
        scope.launch {
            while (true) {
                try {
                    optimizePerformance()
                    delay(5 * 60_000L) // Every 5 minutes
                } catch (e: Exception) {
                    delay(5 * 60_000L)
                }
            }
        }

        // Cleanup loop
        scope.launch {
            while (true) {
                try {
                    cleanupStaleData()
                    delay(15 * 60_000L) // Every 15 minutes
                } catch (e: Exception) {
                    delay(15 * 60_000L)
                }
            }
        }
    }

    /**
     * Perform scheduled health checks for all monitored sources
     */
    private suspend fun performScheduledHealthChecks() {
        val startTime = System.currentTimeMillis()

        coroutineScope {
            val checkTasks = mutableListOf<Deferred<Unit>>()

            for (sourceId in monitoredSources.keys) {
                val task =
                    async {
                        performHealthCheck(sourceId)
                    }
                checkTasks.add(task)
            }

            // Wait for all checks to complete
            checkTasks.awaitAll()
        }

        val duration = System.currentTimeMillis() - startTime
        performanceMetrics.recordHealthCheckBatch(monitoredSources.size, duration)
    }

    /**
     * Perform health check for a specific source
     */
    private suspend fun performHealthCheck(sourceId: String) {
        val monitoredSource = monitoredSources[sourceId] ?: return
        val startTime = System.currentTimeMillis()

        try {
            // Calculate current health
            val healthData =
                healthMonitor.calculateHealthScore(
                    monitoredSource.metadata.health,
                    monitoredSource.metadata.provider,
                )

            // Cache the health data
            healthMonitor.cacheHealthData(sourceId, healthData)

            // Update monitored source
            val updatedSource =
                monitoredSource.copy(
                    lastHealthCheck = Date(),
                    healthHistory = (monitoredSource.healthHistory + healthData).takeLast(50).toMutableList(),
                )
            monitoredSources[sourceId] = updatedSource

            // Check for health degradation
            checkHealthDegradation(sourceId, healthData, updatedSource.healthHistory)

            val duration = System.currentTimeMillis() - startTime
            performanceMetrics.recordSuccessfulHealthCheck(duration)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            performanceMetrics.recordFailedHealthCheck(duration)

            // Emit error alert
            _healthAlerts.emit(
                HealthAlert(
                    sourceId = sourceId,
                    type = HealthAlertType.MONITORING_ERROR,
                    severity = AlertSeverity.MEDIUM,
                    message = "Health check failed: ${e.message}",
                    timestamp = Date(),
                ),
            )
        }
    }

    /**
     * Check for health degradation and emit alerts
     */
    private suspend fun checkHealthDegradation(
        sourceId: String,
        currentHealth: HealthData,
        healthHistory: List<HealthData>,
    ) {
        // Check immediate health threshold
        if (currentHealth.overallScore < alertThresholdScore) {
            _healthAlerts.emit(
                HealthAlert(
                    sourceId = sourceId,
                    type = HealthAlertType.LOW_HEALTH,
                    severity = AlertSeverity.HIGH,
                    message = "Source health critically low: ${currentHealth.overallScore}%",
                    timestamp = Date(),
                    healthData = currentHealth,
                ),
            )
        }

        // Check for degradation trend
        if (healthHistory.size >= 3) {
            val recentScores = healthHistory.takeLast(3).map { it.overallScore }
            val isDecreasing = recentScores.zipWithNext().all { (prev, curr) -> curr < prev }
            val totalDrop = recentScores.first() - recentScores.last()

            if (isDecreasing && totalDrop >= 20) {
                _healthAlerts.emit(
                    HealthAlert(
                        sourceId = sourceId,
                        type = HealthAlertType.DEGRADING_HEALTH,
                        severity = AlertSeverity.MEDIUM,
                        message = "Source health degrading: dropped $totalDrop points",
                        timestamp = Date(),
                        healthData = currentHealth,
                    ),
                )
            }
        }

        // Check P2P specific alerts
        if (currentHealth.p2pHealth.seeders == 0) {
            _healthAlerts.emit(
                HealthAlert(
                    sourceId = sourceId,
                    type = HealthAlertType.DEAD_TORRENT,
                    severity = AlertSeverity.HIGH,
                    message = "Torrent has no seeders - may be dead",
                    timestamp = Date(),
                    healthData = currentHealth,
                ),
            )
        }

        // Check stale data alert
        if (currentHealth.isStale) {
            _healthAlerts.emit(
                HealthAlert(
                    sourceId = sourceId,
                    type = HealthAlertType.STALE_DATA,
                    severity = AlertSeverity.LOW,
                    message = "Health data is stale and needs refresh",
                    timestamp = Date(),
                    healthData = currentHealth,
                ),
            )
        }
    }

    /**
     * Optimize performance by managing resources
     */
    private suspend fun optimizePerformance() {
        val currentTime = System.currentTimeMillis()

        // Remove sources that haven't been accessed recently
        val inactiveSources =
            monitoredSources.filter { (_, source) ->
                val lastCheck = source.lastHealthCheck?.time ?: source.startTime.time
                currentTime - lastCheck > 30 * 60_000L // 30 minutes
            }

        inactiveSources.keys.forEach { sourceId ->
            monitoredSources.remove(sourceId)
        }

        // Adjust monitoring frequency based on load
        val currentLoad = performanceMetrics.getAverageHealthCheckDuration()
        if (currentLoad > 5000L) { // If checks take > 5 seconds on average
            // Consider reducing check frequency or optimizing
            performanceMetrics.recordPerformanceOptimization("Reduced monitoring frequency due to high load")
        }

        performanceMetrics.recordOptimizationCycle(inactiveSources.size)
    }

    /**
     * Clean up stale data to prevent memory leaks
     */
    private suspend fun cleanupStaleData() {
        val currentTime = System.currentTimeMillis()
        val staleThreshold = 60 * 60_000L // 1 hour

        // Clean up old health history
        monitoredSources.forEach { (_, source) ->
            val cutoffTime = Date(currentTime - staleThreshold)
            source.healthHistory.removeAll { it.lastUpdated.before(cutoffTime) }
        }

        // Limit performance metrics history
        performanceMetrics.cleanupOldMetrics()

        performanceMetrics.recordCleanupCycle()
    }

    /**
     * Remove oldest monitored source when limit is reached
     */
    private fun removeOldestMonitoredSource() {
        val oldestEntry = monitoredSources.minByOrNull { it.value.startTime.time }
        oldestEntry?.key?.let { sourceId ->
            monitoredSources.remove(sourceId)
        }
    }
}

/**
 * Monitored source information
 */
data class MonitoredSource(
    val metadata: SourceMetadata,
    val startTime: Date,
    var lastHealthCheck: Date?,
    val healthHistory: MutableList<HealthData>,
    var alertCount: Int,
)

/**
 * Health alert information
 */
data class HealthAlert(
    val sourceId: String,
    val type: HealthAlertType,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: Date,
    val healthData: HealthData? = null,
)

/**
 * Health alert types
 */
enum class HealthAlertType {
    LOW_HEALTH,
    DEGRADING_HEALTH,
    DEAD_TORRENT,
    STALE_DATA,
    MONITORING_ERROR,
    PROVIDER_UNRELIABLE,
}

/**
 * Alert severity levels
 */
enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/**
 * Performance metrics tracking
 */
class PerformanceMetrics {
    private val healthCheckDurations = mutableListOf<Long>()
    private val batchCheckMetrics = mutableListOf<BatchCheckMetric>()
    private var successfulChecks = 0
    private var failedChecks = 0
    private var optimizationEvents = mutableListOf<String>()
    private var cleanupCycles = 0

    fun recordSuccessfulHealthCheck(durationMs: Long) {
        synchronized(this) {
            healthCheckDurations.add(durationMs)
            if (healthCheckDurations.size > 1000) {
                healthCheckDurations.removeAt(0)
            }
            successfulChecks++
        }
    }

    fun recordFailedHealthCheck(durationMs: Long) {
        synchronized(this) {
            healthCheckDurations.add(durationMs)
            if (healthCheckDurations.size > 1000) {
                healthCheckDurations.removeAt(0)
            }
            failedChecks++
        }
    }

    fun recordHealthCheckBatch(
        sourceCount: Int,
        totalDurationMs: Long,
    ) {
        synchronized(this) {
            batchCheckMetrics.add(BatchCheckMetric(sourceCount, totalDurationMs, Date()))
            if (batchCheckMetrics.size > 100) {
                batchCheckMetrics.removeAt(0)
            }
        }
    }

    fun recordOptimizationCycle(removedSources: Int) {
        synchronized(this) {
            optimizationEvents.add("Removed $removedSources inactive sources at ${Date()}")
            if (optimizationEvents.size > 50) {
                optimizationEvents.removeAt(0)
            }
        }
    }

    fun recordPerformanceOptimization(event: String) {
        synchronized(this) {
            optimizationEvents.add("$event at ${Date()}")
            if (optimizationEvents.size > 50) {
                optimizationEvents.removeAt(0)
            }
        }
    }

    fun recordCleanupCycle() {
        synchronized(this) {
            cleanupCycles++
        }
    }

    fun getAverageHealthCheckDuration(): Long {
        return synchronized(this) {
            if (healthCheckDurations.isEmpty()) {
                0L
            } else {
                healthCheckDurations.average().toLong()
            }
        }
    }

    fun getSuccessRate(): Float {
        return synchronized(this) {
            val total = successfulChecks + failedChecks
            if (total == 0) 1.0f else successfulChecks.toFloat() / total
        }
    }

    fun getMetricsSummary(): MetricsSummary {
        return synchronized(this) {
            MetricsSummary(
                totalChecks = successfulChecks + failedChecks,
                successfulChecks = successfulChecks,
                failedChecks = failedChecks,
                averageDurationMs = getAverageHealthCheckDuration(),
                successRate = getSuccessRate(),
                cleanupCycles = cleanupCycles,
                recentOptimizations = optimizationEvents.takeLast(10),
            )
        }
    }

    fun cleanupOldMetrics() {
        synchronized(this) {
            val cutoffTime = Date(System.currentTimeMillis() - 24 * 60 * 60_000L) // 24 hours
            batchCheckMetrics.removeAll { it.timestamp.before(cutoffTime) }
        }
    }
}

/**
 * Batch check metric
 */
private data class BatchCheckMetric(
    val sourceCount: Int,
    val totalDurationMs: Long,
    val timestamp: Date,
)

/**
 * Performance metrics summary
 */
data class MetricsSummary(
    val totalChecks: Int,
    val successfulChecks: Int,
    val failedChecks: Int,
    val averageDurationMs: Long,
    val successRate: Float,
    val cleanupCycles: Int,
    val recentOptimizations: List<String>,
)
