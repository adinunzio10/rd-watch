package com.rdwatch.androidtv.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Circuit breaker implementation to prevent cascading failures when external services are down.
 *
 * The circuit breaker has three states:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Service is considered down, requests fail fast
 * - HALF_OPEN: Testing if service is back up with limited requests
 *
 * Features:
 * - Configurable failure threshold
 * - Configurable recovery timeout
 * - Thread-safe state management
 * - Metrics for monitoring
 * - Fallback mechanism support
 */
class CircuitBreaker(
    private val serviceName: String,
    private val failureThreshold: Int = 5,
    private val recoveryTimeout: Duration = 60.seconds,
    private val fallbackFunction: (suspend () -> Any)? = null,
) {
    /**
     * Circuit breaker states
     */
    enum class State {
        CLOSED, // Normal operation
        OPEN, // Failing fast
        HALF_OPEN, // Testing recovery
    }

    /**
     * Result of circuit breaker execution
     */
    sealed class Result<out T> {
        data class Success<T>(val value: T) : Result<T>()

        data class Failure(val exception: Throwable) : Result<Nothing>()

        data class CircuitOpen(val message: String) : Result<Nothing>()

        data class FallbackUsed<T>(val value: T) : Result<T>()
    }

    /**
     * Circuit breaker metrics for monitoring
     */
    data class Metrics(
        val state: State,
        val failureCount: Int,
        val successCount: Int,
        val rejectedCount: Int,
        val lastFailureTime: Long?,
        val lastSuccessTime: Long?,
        val halfOpenAllowedRequests: Int,
        val halfOpenSuccessfulRequests: Int,
        val serviceName: String,
    )

    @Volatile
    private var state: State = State.CLOSED

    @Volatile
    private var failureCount: Int = 0

    @Volatile
    private var lastFailureTime: Long = 0

    @Volatile
    private var lastSuccessTime: Long = 0

    private val successCount = AtomicLong(0)
    private val rejectedCount = AtomicLong(0)

    // Half-open state management
    @Volatile
    private var halfOpenAllowedRequests: Int = 1

    @Volatile
    private var halfOpenSuccessfulRequests: Int = 0

    private val stateMutex = Mutex()

    companion object {
        private const val TAG = "CircuitBreaker"
        private const val HALF_OPEN_MAX_REQUESTS = 3
    }

    /**
     * Execute a function through the circuit breaker
     */
    suspend fun <T> execute(function: suspend () -> T): Result<T> {
        return stateMutex.withLock {
            when (state) {
                State.CLOSED -> executeClosed(function)
                State.OPEN -> executeOpen(function)
                State.HALF_OPEN -> executeHalfOpen(function)
            }
        }
    }

    /**
     * Execute in CLOSED state
     */
    private suspend fun <T> executeClosed(function: suspend () -> T): Result<T> {
        return try {
            val result = function()
            onSuccess()
            Result.Success(result)
        } catch (e: Exception) {
            onFailure(e)
            Result.Failure(e)
        }
    }

    /**
     * Execute in OPEN state - fail fast or use fallback
     */
    private suspend fun <T> executeOpen(function: suspend () -> T): Result<T> {
        // Check if we should transition to half-open
        if (shouldAttemptReset()) {
            android.util.Log.i(TAG, "[$serviceName] Transitioning from OPEN to HALF_OPEN")
            state = State.HALF_OPEN
            halfOpenSuccessfulRequests = 0
            return executeHalfOpen(function)
        }

        // Circuit is open, reject the request
        rejectedCount.incrementAndGet()
        android.util.Log.d(TAG, "[$serviceName] Circuit OPEN - rejecting request")

        // Try fallback if available
        return if (fallbackFunction != null) {
            try {
                @Suppress("UNCHECKED_CAST")
                val fallbackResult = fallbackFunction.invoke() as T
                android.util.Log.d(TAG, "[$serviceName] Using fallback result")
                Result.FallbackUsed(fallbackResult)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "[$serviceName] Fallback failed: ${e.message}")
                Result.CircuitOpen("Circuit breaker is OPEN for $serviceName and fallback failed")
            }
        } else {
            Result.CircuitOpen("Circuit breaker is OPEN for $serviceName")
        }
    }

    /**
     * Execute in HALF_OPEN state - limited test requests
     */
    private suspend fun <T> executeHalfOpen(function: suspend () -> T): Result<T> {
        return if (halfOpenSuccessfulRequests < halfOpenAllowedRequests) {
            try {
                val result = function()
                halfOpenSuccessfulRequests++
                android.util.Log.d(TAG, "[$serviceName] HALF_OPEN success ($halfOpenSuccessfulRequests/$halfOpenAllowedRequests)")

                // If we've had enough successful requests, close the circuit
                if (halfOpenSuccessfulRequests >= halfOpenAllowedRequests) {
                    android.util.Log.i(TAG, "[$serviceName] Transitioning from HALF_OPEN to CLOSED")
                    state = State.CLOSED
                    failureCount = 0
                    onSuccess()
                }

                Result.Success(result)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "[$serviceName] HALF_OPEN request failed, returning to OPEN")
                state = State.OPEN
                onFailure(e)
                Result.Failure(e)
            }
        } else {
            // Too many requests in half-open, reject
            rejectedCount.incrementAndGet()
            android.util.Log.d(TAG, "[$serviceName] HALF_OPEN limit reached - rejecting request")
            Result.CircuitOpen("Circuit breaker is HALF_OPEN for $serviceName - limit reached")
        }
    }

    /**
     * Handle successful execution
     */
    private fun onSuccess() {
        successCount.incrementAndGet()
        lastSuccessTime = System.currentTimeMillis()
        android.util.Log.d(TAG, "[$serviceName] Request successful")
    }

    /**
     * Handle failed execution
     */
    private fun onFailure(exception: Throwable) {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        android.util.Log.w(TAG, "[$serviceName] Request failed ($failureCount/$failureThreshold): ${exception.message}")

        if (failureCount >= failureThreshold && state == State.CLOSED) {
            android.util.Log.w(TAG, "[$serviceName] Failure threshold reached - transitioning to OPEN")
            state = State.OPEN
        }
    }

    /**
     * Check if we should attempt to reset the circuit (transition from OPEN to HALF_OPEN)
     */
    private fun shouldAttemptReset(): Boolean {
        val timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime
        return timeSinceLastFailure >= recoveryTimeout.inWholeMilliseconds
    }

    /**
     * Get current metrics
     */
    fun getMetrics(): Metrics {
        return Metrics(
            state = state,
            failureCount = failureCount,
            successCount = successCount.get().toInt(),
            rejectedCount = rejectedCount.get().toInt(),
            lastFailureTime = if (lastFailureTime > 0) lastFailureTime else null,
            lastSuccessTime = if (lastSuccessTime > 0) lastSuccessTime else null,
            halfOpenAllowedRequests = halfOpenAllowedRequests,
            halfOpenSuccessfulRequests = halfOpenSuccessfulRequests,
            serviceName = serviceName,
        )
    }

    /**
     * Force circuit to OPEN state (for testing or manual intervention)
     */
    suspend fun forceOpen() {
        stateMutex.withLock {
            android.util.Log.w(TAG, "[$serviceName] Manually forcing circuit to OPEN state")
            state = State.OPEN
            lastFailureTime = System.currentTimeMillis()
        }
    }

    /**
     * Force circuit to CLOSED state (for testing or manual intervention)
     */
    suspend fun forceClose() {
        stateMutex.withLock {
            android.util.Log.i(TAG, "[$serviceName] Manually forcing circuit to CLOSED state")
            state = State.CLOSED
            failureCount = 0
            halfOpenSuccessfulRequests = 0
        }
    }

    /**
     * Reset all metrics
     */
    suspend fun reset() {
        stateMutex.withLock {
            android.util.Log.i(TAG, "[$serviceName] Resetting circuit breaker metrics")
            state = State.CLOSED
            failureCount = 0
            lastFailureTime = 0
            lastSuccessTime = 0
            successCount.set(0)
            rejectedCount.set(0)
            halfOpenSuccessfulRequests = 0
        }
    }

    /**
     * Check if circuit is currently allowing requests
     */
    fun isRequestAllowed(): Boolean {
        return when (state) {
            State.CLOSED -> true
            State.OPEN -> shouldAttemptReset()
            State.HALF_OPEN -> halfOpenSuccessfulRequests < halfOpenAllowedRequests
        }
    }

    /**
     * Get current state
     */
    fun getCurrentState(): State = state

    /**
     * Get service name
     */
    fun getServiceName(): String = serviceName
}

/**
 * Factory for creating circuit breaker instances with common configurations
 */
object CircuitBreakerFactory {
    /**
     * Create a circuit breaker for scraper services
     */
    fun createScraperCircuitBreaker(serviceName: String): CircuitBreaker {
        return CircuitBreaker(
            serviceName = "scraper:$serviceName",
            failureThreshold = 5,
            recoveryTimeout = 60.seconds,
            fallbackFunction = {
                android.util.Log.d("CircuitBreaker", "Using fallback for scraper $serviceName")
                "[]" // Empty JSON array as fallback for scraper responses
            },
        )
    }

    /**
     * Create a circuit breaker for Real-Debrid API
     */
    fun createRealDebridCircuitBreaker(): CircuitBreaker {
        return CircuitBreaker(
            serviceName = "real-debrid-api",
            failureThreshold = 3, // More sensitive for paid service
            recoveryTimeout = 30.seconds, // Faster recovery for critical service
            fallbackFunction = null, // No fallback for Real-Debrid - return original URL
        )
    }

    /**
     * Create a circuit breaker for general external services
     */
    fun createGenericCircuitBreaker(serviceName: String): CircuitBreaker {
        return CircuitBreaker(
            serviceName = serviceName,
            failureThreshold = 5,
            recoveryTimeout = 60.seconds,
            fallbackFunction = null,
        )
    }
}
