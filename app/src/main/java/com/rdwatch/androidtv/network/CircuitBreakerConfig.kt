package com.rdwatch.androidtv.network

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for circuit breakers across the application
 */
data class CircuitBreakerConfig(
    val failureThreshold: Int,
    val recoveryTimeout: Duration,
    val enabled: Boolean = true,
    val fallbackEnabled: Boolean = true,
) {
    companion object {
        /**
         * Default configuration for scraper services
         */
        val SCRAPER_DEFAULT =
            CircuitBreakerConfig(
                failureThreshold = 5,
                recoveryTimeout = 60.seconds,
                enabled = true,
                fallbackEnabled = true,
            )

        /**
         * Default configuration for Real-Debrid API
         */
        val REAL_DEBRID_DEFAULT =
            CircuitBreakerConfig(
                failureThreshold = 3, // More sensitive for paid service
                recoveryTimeout = 30.seconds, // Faster recovery for critical service
                enabled = true,
                fallbackEnabled = false, // No fallback for Real-Debrid
            )

        /**
         * Configuration for external metadata services (TMDb, etc.)
         */
        val METADATA_DEFAULT =
            CircuitBreakerConfig(
                failureThreshold = 5,
                recoveryTimeout = 2.minutes,
                enabled = true,
                fallbackEnabled = true,
            )

        /**
         * Configuration for development/testing (more lenient)
         */
        val DEVELOPMENT =
            CircuitBreakerConfig(
                failureThreshold = 10,
                recoveryTimeout = 15.seconds,
                enabled = true,
                fallbackEnabled = true,
            )

        /**
         * Disabled circuit breaker (bypass for testing)
         */
        val DISABLED =
            CircuitBreakerConfig(
                failureThreshold = Int.MAX_VALUE,
                recoveryTimeout = 1.seconds,
                enabled = false,
                fallbackEnabled = false,
            )
    }

    /**
     * Validate configuration parameters
     */
    fun validate(): CircuitBreakerConfig {
        require(failureThreshold > 0) { "Failure threshold must be positive" }
        require(recoveryTimeout.isPositive()) { "Recovery timeout must be positive" }
        return this
    }

    /**
     * Create a copy with modified failure threshold
     */
    fun withFailureThreshold(threshold: Int): CircuitBreakerConfig {
        return copy(failureThreshold = threshold).validate()
    }

    /**
     * Create a copy with modified recovery timeout
     */
    fun withRecoveryTimeout(timeout: Duration): CircuitBreakerConfig {
        return copy(recoveryTimeout = timeout).validate()
    }

    /**
     * Create a copy with circuit breaker enabled/disabled
     */
    fun withEnabled(enabled: Boolean): CircuitBreakerConfig {
        return copy(enabled = enabled)
    }

    /**
     * Create a copy with fallback enabled/disabled
     */
    fun withFallbackEnabled(enabled: Boolean): CircuitBreakerConfig {
        return copy(fallbackEnabled = enabled)
    }
}

/**
 * Circuit breaker registry for managing multiple circuit breakers
 */
class CircuitBreakerRegistry {
    private val circuitBreakers = mutableMapOf<String, CircuitBreaker>()
    private val configs = mutableMapOf<String, CircuitBreakerConfig>()

    /**
     * Register a circuit breaker with configuration
     */
    fun register(
        name: String,
        config: CircuitBreakerConfig,
        fallbackFunction: (suspend () -> String)? = null,
    ): CircuitBreaker {
        val circuitBreaker =
            CircuitBreaker(
                serviceName = name,
                failureThreshold = config.failureThreshold,
                recoveryTimeout = config.recoveryTimeout,
                fallbackFunction = if (config.fallbackEnabled) fallbackFunction else null,
            )

        circuitBreakers[name] = circuitBreaker
        configs[name] = config

        return circuitBreaker
    }

    /**
     * Get a circuit breaker by name
     */
    fun get(name: String): CircuitBreaker? {
        return circuitBreakers[name]
    }

    /**
     * Get configuration for a circuit breaker
     */
    fun getConfig(name: String): CircuitBreakerConfig? {
        return configs[name]
    }

    /**
     * Get all registered circuit breakers
     */
    fun getAll(): Map<String, CircuitBreaker> {
        return circuitBreakers.toMap()
    }

    /**
     * Get metrics for all circuit breakers
     */
    fun getAllMetrics(): Map<String, CircuitBreaker.Metrics> {
        return circuitBreakers.mapValues { (_, breaker) -> breaker.getMetrics() }
    }

    /**
     * Reset all circuit breakers
     */
    suspend fun resetAll() {
        circuitBreakers.values.forEach { it.reset() }
    }

    /**
     * Reset a specific circuit breaker
     */
    suspend fun reset(name: String) {
        circuitBreakers[name]?.reset()
    }

    /**
     * Remove a circuit breaker
     */
    fun remove(name: String) {
        circuitBreakers.remove(name)
        configs.remove(name)
    }

    /**
     * Check if any circuit breakers are open
     */
    fun hasOpenCircuits(): Boolean {
        return circuitBreakers.values.any {
            it.getCurrentState() == CircuitBreaker.State.OPEN
        }
    }

    /**
     * Get count of circuit breakers by state
     */
    fun getStateCount(): Map<CircuitBreaker.State, Int> {
        val counts = mutableMapOf<CircuitBreaker.State, Int>()
        CircuitBreaker.State.values().forEach { state ->
            counts[state] = circuitBreakers.values.count { it.getCurrentState() == state }
        }
        return counts
    }

    /**
     * Get summary of circuit breaker health
     */
    fun getHealthSummary(): CircuitBreakerHealthSummary {
        val metrics = getAllMetrics()
        val states = getStateCount()

        return CircuitBreakerHealthSummary(
            totalCircuitBreakers = circuitBreakers.size,
            openCircuits = states[CircuitBreaker.State.OPEN] ?: 0,
            halfOpenCircuits = states[CircuitBreaker.State.HALF_OPEN] ?: 0,
            closedCircuits = states[CircuitBreaker.State.CLOSED] ?: 0,
            totalSuccesses = metrics.values.sumOf { it.successCount.toLong() },
            totalFailures = metrics.values.sumOf { it.failureCount.toLong() },
            totalRejections = metrics.values.sumOf { it.rejectedCount.toLong() },
            unhealthyServices =
                metrics.filter { (_, metric) ->
                    metric.state == CircuitBreaker.State.OPEN ||
                        (metric.failureCount > 0 && metric.successCount == 0)
                }.keys.toList(),
        )
    }
}

/**
 * Health summary for circuit breakers
 */
data class CircuitBreakerHealthSummary(
    val totalCircuitBreakers: Int,
    val openCircuits: Int,
    val halfOpenCircuits: Int,
    val closedCircuits: Int,
    val totalSuccesses: Long,
    val totalFailures: Long,
    val totalRejections: Long,
    val unhealthyServices: List<String>,
) {
    val isHealthy: Boolean
        get() = openCircuits == 0 && unhealthyServices.isEmpty()

    val healthPercentage: Double
        get() =
            if (totalCircuitBreakers == 0) {
                100.0
            } else {
                ((closedCircuits.toDouble() / totalCircuitBreakers) * 100.0)
            }
}
