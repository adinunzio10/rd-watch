package com.rdwatch.androidtv.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Monitoring service for circuit breakers across the application
 * Provides real-time monitoring, alerting, and health reporting
 */
@Singleton
class CircuitBreakerMonitor
    @Inject
    constructor() {
        private val monitoringScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // Event flows for monitoring
        private val _healthEvents = MutableSharedFlow<CircuitBreakerHealthEvent>()
        val healthEvents: Flow<CircuitBreakerHealthEvent> = _healthEvents.asSharedFlow()

        private val _metricEvents = MutableSharedFlow<CircuitBreakerMetricEvent>()
        val metricEvents: Flow<CircuitBreakerMetricEvent> = _metricEvents.asSharedFlow()

        // Registry for tracking circuit breakers
        private val registry = CircuitBreakerRegistry()

        // Monitoring configuration
        private var monitoringInterval: Duration = 30.seconds
        private var isMonitoring = false

        companion object {
            private const val TAG = "CircuitBreakerMonitor"
        }

        /**
         * Start monitoring circuit breakers
         */
        fun startMonitoring(interval: Duration = 30.seconds) {
            if (isMonitoring) return

            monitoringInterval = interval
            isMonitoring = true

            android.util.Log.i(TAG, "Starting circuit breaker monitoring with interval: $interval")

            monitoringScope.launch {
                while (isActive && isMonitoring) {
                    try {
                        performHealthCheck()
                        delay(monitoringInterval)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error during health check", e)
                        delay(monitoringInterval)
                    }
                }
            }
        }

        /**
         * Stop monitoring
         */
        fun stopMonitoring() {
            isMonitoring = false
            android.util.Log.i(TAG, "Stopped circuit breaker monitoring")
        }

        /**
         * Register a circuit breaker for monitoring
         */
        fun registerCircuitBreaker(
            name: String,
            circuitBreaker: CircuitBreaker,
            config: CircuitBreakerConfig? = null,
        ) {
            if (config != null) {
                registry.register(name, config)
            }
            android.util.Log.d(TAG, "Registered circuit breaker for monitoring: $name")
        }

        /**
         * Unregister a circuit breaker
         */
        fun unregisterCircuitBreaker(name: String) {
            registry.remove(name)
            android.util.Log.d(TAG, "Unregistered circuit breaker: $name")
        }

        /**
         * Get current health summary
         */
        fun getHealthSummary(): CircuitBreakerHealthSummary {
            return registry.getHealthSummary()
        }

        /**
         * Get metrics for all circuit breakers
         */
        fun getAllMetrics(): Map<String, CircuitBreaker.Metrics> {
            return registry.getAllMetrics()
        }

        /**
         * Get metrics for a specific circuit breaker
         */
        fun getMetrics(name: String): CircuitBreaker.Metrics? {
            return registry.get(name)?.getMetrics()
        }

        /**
         * Reset all circuit breakers
         */
        suspend fun resetAllCircuitBreakers() {
            registry.resetAll()
            android.util.Log.i(TAG, "Reset all circuit breakers")

            _healthEvents.emit(
                CircuitBreakerHealthEvent.SystemReset(
                    timestamp = System.currentTimeMillis(),
                ),
            )
        }

        /**
         * Reset a specific circuit breaker
         */
        suspend fun resetCircuitBreaker(name: String) {
            registry.reset(name)
            android.util.Log.i(TAG, "Reset circuit breaker: $name")

            _healthEvents.emit(
                CircuitBreakerHealthEvent.CircuitReset(
                    serviceName = name,
                    timestamp = System.currentTimeMillis(),
                ),
            )
        }

        /**
         * Perform health check and emit events
         */
        private suspend fun performHealthCheck() {
            val summary = registry.getHealthSummary()
            val metrics = registry.getAllMetrics()

            android.util.Log.d(
                TAG,
                "Health check - Healthy: ${summary.isHealthy}, " +
                    "Open circuits: ${summary.openCircuits}, " +
                    "Total breakers: ${summary.totalCircuitBreakers}",
            )

            // Emit health events for state changes
            metrics.forEach { (name, metric) ->
                when (metric.state) {
                    CircuitBreaker.State.OPEN -> {
                        _healthEvents.emit(
                            CircuitBreakerHealthEvent.CircuitOpened(
                                serviceName = name,
                                failureCount = metric.failureCount,
                                lastFailureTime = metric.lastFailureTime,
                                timestamp = System.currentTimeMillis(),
                            ),
                        )
                    }
                    CircuitBreaker.State.HALF_OPEN -> {
                        _healthEvents.emit(
                            CircuitBreakerHealthEvent.CircuitHalfOpen(
                                serviceName = name,
                                timestamp = System.currentTimeMillis(),
                            ),
                        )
                    }
                    CircuitBreaker.State.CLOSED -> {
                        // Only emit if recently recovered (has previous failures)
                        if (metric.failureCount > 0 && metric.lastSuccessTime != null) {
                            val timeSinceSuccess = System.currentTimeMillis() - metric.lastSuccessTime
                            if (timeSinceSuccess < monitoringInterval.inWholeMilliseconds * 2) {
                                _healthEvents.emit(
                                    CircuitBreakerHealthEvent.CircuitClosed(
                                        serviceName = name,
                                        timestamp = System.currentTimeMillis(),
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // Emit metric events
            _metricEvents.emit(
                CircuitBreakerMetricEvent.HealthSummary(
                    summary = summary,
                    timestamp = System.currentTimeMillis(),
                ),
            )

            // Check for system-wide health issues
            if (!summary.isHealthy && summary.openCircuits > summary.totalCircuitBreakers / 2) {
                _healthEvents.emit(
                    CircuitBreakerHealthEvent.SystemDegraded(
                        openCircuits = summary.openCircuits,
                        totalCircuits = summary.totalCircuitBreakers,
                        unhealthyServices = summary.unhealthyServices,
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            }
        }

        /**
         * Log detailed circuit breaker status
         */
        fun logStatus() {
            val summary = getHealthSummary()
            val metrics = getAllMetrics()

            android.util.Log.i(TAG, "=== Circuit Breaker Status ===")
            android.util.Log.i(
                TAG,
                "Total: ${summary.totalCircuitBreakers}, " +
                    "Open: ${summary.openCircuits}, " +
                    "Half-Open: ${summary.halfOpenCircuits}, " +
                    "Closed: ${summary.closedCircuits}",
            )
            android.util.Log.i(
                TAG,
                "Health: ${summary.healthPercentage}%, " +
                    "Successes: ${summary.totalSuccesses}, " +
                    "Failures: ${summary.totalFailures}, " +
                    "Rejections: ${summary.totalRejections}",
            )

            if (summary.unhealthyServices.isNotEmpty()) {
                android.util.Log.w(TAG, "Unhealthy services: ${summary.unhealthyServices.joinToString()}")
            }

            metrics.forEach { (name, metric) ->
                android.util.Log.d(
                    TAG,
                    "[$name] State: ${metric.state}, " +
                        "Failures: ${metric.failureCount}, " +
                        "Successes: ${metric.successCount}, " +
                        "Rejections: ${metric.rejectedCount}",
                )
            }
        }

        /**
         * Check if monitoring is active
         */
        fun isMonitoring(): Boolean = isMonitoring

        /**
         * Get monitoring interval
         */
        fun getMonitoringInterval(): Duration = monitoringInterval
    }

/**
 * Events related to circuit breaker health
 */
sealed class CircuitBreakerHealthEvent {
    abstract val timestamp: Long

    data class CircuitOpened(
        val serviceName: String,
        val failureCount: Int,
        val lastFailureTime: Long?,
        override val timestamp: Long,
    ) : CircuitBreakerHealthEvent()

    data class CircuitClosed(
        val serviceName: String,
        override val timestamp: Long,
    ) : CircuitBreakerHealthEvent()

    data class CircuitHalfOpen(
        val serviceName: String,
        override val timestamp: Long,
    ) : CircuitBreakerHealthEvent()

    data class CircuitReset(
        val serviceName: String,
        override val timestamp: Long,
    ) : CircuitBreakerHealthEvent()

    data class SystemReset(
        override val timestamp: Long,
    ) : CircuitBreakerHealthEvent()

    data class SystemDegraded(
        val openCircuits: Int,
        val totalCircuits: Int,
        val unhealthyServices: List<String>,
        override val timestamp: Long,
    ) : CircuitBreakerHealthEvent()
}

/**
 * Events related to circuit breaker metrics
 */
sealed class CircuitBreakerMetricEvent {
    abstract val timestamp: Long

    data class HealthSummary(
        val summary: CircuitBreakerHealthSummary,
        override val timestamp: Long,
    ) : CircuitBreakerMetricEvent()

    data class ServiceMetrics(
        val serviceName: String,
        val metrics: CircuitBreaker.Metrics,
        override val timestamp: Long,
    ) : CircuitBreakerMetricEvent()
}
