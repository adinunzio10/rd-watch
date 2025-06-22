package com.rdwatch.androidtv.scraper.error

import com.rdwatch.androidtv.scraper.models.ManifestException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Circuit breaker implementation for manifest operations
 * Prevents cascading failures and provides fast failure when services are down
 */
@Singleton
class ManifestCircuitBreaker @Inject constructor() {
    
    private val circuitBreakers = mutableMapOf<String, CircuitBreakerInstance>()
    private val mutex = Mutex()
    
    /**
     * Execute operation with circuit breaker protection
     */
    suspend fun <T> execute(
        key: String,
        config: CircuitBreakerConfig = CircuitBreakerConfig(),
        operation: suspend () -> ManifestResult<T>
    ): ManifestResult<T> {
        val breaker = getOrCreateBreaker(key, config)
        return breaker.execute(operation)
    }
    
    /**
     * Get circuit breaker state for monitoring
     */
    suspend fun getState(key: String): CircuitBreakerState? {
        return mutex.withLock {
            circuitBreakers[key]?.state
        }
    }
    
    /**
     * Get all circuit breaker states
     */
    suspend fun getAllStates(): Map<String, CircuitBreakerState> {
        return mutex.withLock {
            circuitBreakers.mapValues { it.value.state }
        }
    }
    
    /**
     * Reset circuit breaker (force to closed state)
     */
    suspend fun reset(key: String) {
        mutex.withLock {
            circuitBreakers[key]?.reset()
        }
    }
    
    /**
     * Reset all circuit breakers
     */
    suspend fun resetAll() {
        mutex.withLock {
            circuitBreakers.values.forEach { it.reset() }
        }
    }
    
    private suspend fun getOrCreateBreaker(
        key: String,
        config: CircuitBreakerConfig
    ): CircuitBreakerInstance {
        return mutex.withLock {
            circuitBreakers.getOrPut(key) {
                CircuitBreakerInstance(key, config)
            }
        }
    }
    
    /**
     * Individual circuit breaker instance
     */
    private class CircuitBreakerInstance(
        private val key: String,
        private val config: CircuitBreakerConfig
    ) {
        @Volatile
        private var circuitState = CircuitState.CLOSED
        
        @Volatile
        private var lastFailureTime = 0L
        
        private val failureCount = AtomicInteger(0)
        private val successCount = AtomicInteger(0)
        private val requestCount = AtomicInteger(0)
        private val instanceMutex = Mutex()
        
        val state: CircuitBreakerState
            get() = CircuitBreakerState(
                circuitState = circuitState,
                failureCount = failureCount.get(),
                successCount = successCount.get(),
                requestCount = requestCount.get(),
                lastFailureTime = lastFailureTime,
                failureRate = calculateFailureRate(),
                nextRetryTime = if (circuitState == CircuitState.OPEN) {
                    lastFailureTime + config.openTimeoutMs
                } else null
            )
        
        suspend fun <T> execute(operation: suspend () -> ManifestResult<T>): ManifestResult<T> {
            return instanceMutex.withLock {
                when (circuitState) {
                    CircuitState.OPEN -> {
                        if (shouldAttemptReset()) {
                            circuitState = CircuitState.HALF_OPEN
                            executeInHalfOpen(operation)
                        } else {
                            ManifestResult.Error(
                                ManifestException(
                                    "Circuit breaker is OPEN for '$key'. " +
                                    "Next retry available at ${java.util.Date(lastFailureTime + config.openTimeoutMs)}"
                                )
                            )
                        }
                    }
                    CircuitState.HALF_OPEN -> executeInHalfOpen(operation)
                    CircuitState.CLOSED -> executeInClosed(operation)
                }
            }
        }
        
        private suspend fun <T> executeInClosed(operation: suspend () -> ManifestResult<T>): ManifestResult<T> {
            requestCount.incrementAndGet()
            
            return try {
                val result = operation()
                
                when (result) {
                    is ManifestResult.Success -> {
                        onSuccess()
                        result
                    }
                    is ManifestResult.Error -> {
                        onFailure(result.exception)
                        result
                    }
                }
            } catch (e: Exception) {
                val manifestException = if (e is ManifestException) e else ManifestException("Unexpected error", e)
                onFailure(manifestException)
                ManifestResult.Error(manifestException)
            }
        }
        
        private suspend fun <T> executeInHalfOpen(operation: suspend () -> ManifestResult<T>): ManifestResult<T> {
            return try {
                val result = operation()
                
                when (result) {
                    is ManifestResult.Success -> {
                        // Success in half-open state closes the circuit
                        circuitState = CircuitState.CLOSED
                        reset()
                        result
                    }
                    is ManifestResult.Error -> {
                        // Failure in half-open state opens the circuit again
                        circuitState = CircuitState.OPEN
                        lastFailureTime = System.currentTimeMillis()
                        result
                    }
                }
            } catch (e: Exception) {
                val manifestException = if (e is ManifestException) e else ManifestException("Unexpected error", e)
                circuitState = CircuitState.OPEN
                lastFailureTime = System.currentTimeMillis()
                ManifestResult.Error(manifestException)
            }
        }
        
        private fun onSuccess() {
            successCount.incrementAndGet()
            
            // Reset failure count on successful operation
            if (config.resetFailureCountOnSuccess) {
                failureCount.set(0)
            }
        }
        
        private fun onFailure(exception: ManifestException) {
            failureCount.incrementAndGet()
            lastFailureTime = System.currentTimeMillis()
            
            // Check if we should open the circuit
            if (shouldOpenCircuit()) {
                circuitState = CircuitState.OPEN
            }
        }
        
        private fun shouldOpenCircuit(): Boolean {
            val currentFailureCount = failureCount.get()
            val currentRequestCount = requestCount.get()
            
            // Need minimum number of requests to consider opening
            if (currentRequestCount < config.minimumRequests) {
                return false
            }
            
            // Check failure threshold
            if (currentFailureCount >= config.failureThreshold) {
                return true
            }
            
            // Check failure rate threshold
            val failureRate = calculateFailureRate()
            if (failureRate >= config.failureRateThreshold) {
                return true
            }
            
            return false
        }
        
        private fun shouldAttemptReset(): Boolean {
            return System.currentTimeMillis() >= lastFailureTime + config.openTimeoutMs
        }
        
        private fun calculateFailureRate(): Double {
            val total = requestCount.get()
            return if (total > 0) {
                failureCount.get().toDouble() / total.toDouble()
            } else {
                0.0
            }
        }
        
        fun reset() {
            circuitState = CircuitState.CLOSED
            failureCount.set(0)
            successCount.set(0)
            requestCount.set(0)
            lastFailureTime = 0L
        }
    }
}

/**
 * Circuit breaker configuration
 */
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,
    val failureRateThreshold: Double = 0.5, // 50%
    val minimumRequests: Int = 10,
    val openTimeoutMs: Long = 60000L, // 1 minute
    val resetFailureCountOnSuccess: Boolean = true
) {
    companion object {
        fun forNetworkOperations() = CircuitBreakerConfig(
            failureThreshold = 3,
            failureRateThreshold = 0.6,
            minimumRequests = 5,
            openTimeoutMs = 30000L
        )
        
        fun forStorageOperations() = CircuitBreakerConfig(
            failureThreshold = 5,
            failureRateThreshold = 0.8,
            minimumRequests = 10,
            openTimeoutMs = 10000L
        )
        
        fun strict() = CircuitBreakerConfig(
            failureThreshold = 2,
            failureRateThreshold = 0.3,
            minimumRequests = 3,
            openTimeoutMs = 120000L
        )
        
        fun lenient() = CircuitBreakerConfig(
            failureThreshold = 10,
            failureRateThreshold = 0.8,
            minimumRequests = 20,
            openTimeoutMs = 30000L
        )
    }
}

/**
 * Circuit breaker states
 */
enum class CircuitState {
    /**
     * Normal operation - requests are allowed through
     */
    CLOSED,
    
    /**
     * Circuit is open - requests fail fast
     */
    OPEN,
    
    /**
     * Testing if service has recovered - limited requests allowed
     */
    HALF_OPEN
}

/**
 * Circuit breaker state information
 */
data class CircuitBreakerState(
    val circuitState: CircuitState,
    val failureCount: Int,
    val successCount: Int,
    val requestCount: Int,
    val lastFailureTime: Long,
    val failureRate: Double,
    val nextRetryTime: Long?
) {
    val isOpen: Boolean get() = circuitState == CircuitState.OPEN
    val isClosed: Boolean get() = circuitState == CircuitState.CLOSED
    val isHalfOpen: Boolean get() = circuitState == CircuitState.HALF_OPEN
    
    val formattedLastFailureTime: String?
        get() = if (lastFailureTime > 0) {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(lastFailureTime))
        } else null
    
    val formattedNextRetryTime: String?
        get() = nextRetryTime?.let { time ->
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(time))
        }
}