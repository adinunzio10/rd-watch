package com.rdwatch.androidtv.core.error

import kotlinx.coroutines.delay
import kotlin.random.Random

class RetryHandler {
    companion object {
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_INITIAL_DELAY = 1000L
        const val DEFAULT_MAX_DELAY = 10000L
        const val DEFAULT_BACKOFF_MULTIPLIER = 2.0
    }

    suspend fun <T> executeWithRetry(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        initialDelay: Long = DEFAULT_INITIAL_DELAY,
        maxDelay: Long = DEFAULT_MAX_DELAY,
        backoffMultiplier: Double = DEFAULT_BACKOFF_MULTIPLIER,
        shouldRetry: (Throwable) -> Boolean = ::defaultShouldRetry,
        action: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        var lastException: Throwable? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return action()
            } catch (e: Throwable) {
                lastException = e

                if (attempt == maxRetries || !shouldRetry(e)) {
                    throw e
                }

                // Add jitter to prevent thundering herd
                val jitteredDelay = currentDelay + Random.nextLong(0, currentDelay / 2)
                delay(jitteredDelay)

                currentDelay = (currentDelay * backoffMultiplier).toLong().coerceAtMost(maxDelay)
            }
        }

        throw lastException ?: RuntimeException("Unknown error in retry handler")
    }

    private fun defaultShouldRetry(throwable: Throwable): Boolean {
        return when (throwable.toAppException()) {
            is AppException.NetworkException -> true
            is AppException.ApiException ->
                throwable.cause?.let {
                    (it as? retrofit2.HttpException)?.code() in 500..599
                } ?: false
            is AppException.VideoPlayerException -> true
            else -> false
        }
    }

    fun shouldRetryError(error: ErrorInfo): Boolean {
        return error.canRetry &&
            when (error.type) {
                ErrorType.NETWORK -> true
                ErrorType.API ->
                    error.exception is AppException.ApiException &&
                        error.exception.code in 500..599
                ErrorType.VIDEO_PLAYER -> true
                else -> false
            }
    }
}

suspend inline fun <T> retryOnFailure(
    maxRetries: Int = RetryHandler.DEFAULT_MAX_RETRIES,
    initialDelay: Long = RetryHandler.DEFAULT_INITIAL_DELAY,
    noinline shouldRetry: (Throwable) -> Boolean = { true },
    noinline action: suspend () -> T,
): T {
    return RetryHandler().executeWithRetry(
        maxRetries = maxRetries,
        initialDelay = initialDelay,
        shouldRetry = shouldRetry,
        action = action,
    )
}
