package com.rdwatch.androidtv.player.subtitle

import com.rdwatch.androidtv.player.subtitle.api.RateLimitStatus
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Manages rate limiting across all subtitle API providers.
 * Prevents hitting API limits and implements exponential backoff for failed requests.
 *
 * This is critical for maintaining good relationships with external APIs and
 * ensuring the app doesn't get blocked due to excessive requests.
 */
@Singleton
class SubtitleRateLimiter
    @Inject
    constructor() {
        private val mutex = Mutex()
        private val providerLimits = ConcurrentHashMap<SubtitleApiProvider, ProviderRateLimit>()

        init {
            // Initialize rate limits for each provider based on their documented limits
            initializeProviderLimits()
        }

        /**
         * Check if a request can be made to the specified provider.
         *
         * @param provider The subtitle API provider
         * @return true if request is allowed, false if rate limited
         */
        suspend fun canMakeRequest(provider: SubtitleApiProvider): Boolean =
            mutex.withLock {
                val limit = providerLimits[provider] ?: return false
                val now = System.currentTimeMillis()

                // Reset window if expired
                if (now >= limit.windowResetTime) {
                    resetWindow(provider, now)
                }

                // Check if within limits
                return limit.requestsInWindow < limit.maxRequestsPerWindow
            }

        /**
         * Record a request to the specified provider.
         * Updates rate limiting counters and handles failures.
         *
         * @param provider The subtitle API provider
         * @param success Whether the request was successful
         */
        suspend fun recordRequest(
            provider: SubtitleApiProvider,
            success: Boolean = true,
        ) = mutex.withLock {
            val limit = providerLimits[provider] ?: return

            limit.requestsInWindow++
            limit.totalRequests++

            if (!success) {
                limit.failureCount++
                // Implement exponential backoff for failures
                val backoffMs = calculateBackoffDelay(limit.failureCount)
                limit.backoffUntil = System.currentTimeMillis() + backoffMs
            } else {
                // Reset failure count on success
                limit.failureCount = 0
                limit.backoffUntil = 0
            }
        }

        /**
         * Get current rate limit status for a provider.
         *
         * @param provider The subtitle API provider
         * @return Current rate limit status
         */
        fun getStatus(provider: SubtitleApiProvider): RateLimitStatus {
            val limit = providerLimits[provider] ?: return RateLimitStatus(0, 0, true)
            val now = System.currentTimeMillis()

            return RateLimitStatus(
                requestsRemaining = (limit.maxRequestsPerWindow - limit.requestsInWindow).coerceAtLeast(0),
                resetTimeMs = limit.windowResetTime,
                isLimited = limit.requestsInWindow >= limit.maxRequestsPerWindow || now < limit.backoffUntil,
            )
        }

        /**
         * Reset rate limits for all providers.
         * Used for testing or manual reset.
         */
        suspend fun resetAllLimits() =
            mutex.withLock {
                val now = System.currentTimeMillis()
                providerLimits.keys.forEach { provider ->
                    resetWindow(provider, now)
                }
            }

        /**
         * Get comprehensive rate limiting statistics.
         * Used for monitoring and debugging.
         */
        fun getStatistics(): Map<SubtitleApiProvider, RateLimitStatistics> {
            return providerLimits.mapValues { (_, limit) ->
                RateLimitStatistics(
                    totalRequests = limit.totalRequests,
                    requestsInCurrentWindow = limit.requestsInWindow,
                    maxRequestsPerWindow = limit.maxRequestsPerWindow,
                    windowDurationMs = limit.windowDurationMs,
                    failureCount = limit.failureCount,
                    isInBackoff = System.currentTimeMillis() < limit.backoffUntil,
                    backoffEndsAt = limit.backoffUntil,
                )
            }
        }

        private fun initializeProviderLimits() {
            // Configure limits based on each provider's documented API limits
            providerLimits[SubtitleApiProvider.SUBDL] =
                ProviderRateLimit(
                    maxRequestsPerWindow = 100, // Generous limit for free API
                    windowDurationMs = 60 * 60 * 1000, // 1 hour window
                )

            providerLimits[SubtitleApiProvider.SUBDB] =
                ProviderRateLimit(
                    maxRequestsPerWindow = 200, // Higher limit, simple hash-based API
                    windowDurationMs = 60 * 60 * 1000, // 1 hour window
                )

            providerLimits[SubtitleApiProvider.PODNAPISI] =
                ProviderRateLimit(
                    maxRequestsPerWindow = 50, // Conservative limit for web scraping
                    windowDurationMs = 60 * 60 * 1000, // 1 hour window
                )

            providerLimits[SubtitleApiProvider.ADDIC7ED_ALT] =
                ProviderRateLimit(
                    maxRequestsPerWindow = 30, // Very conservative for TV shows
                    windowDurationMs = 60 * 60 * 1000, // 1 hour window
                )

            providerLimits[SubtitleApiProvider.LOCAL_FILES] =
                ProviderRateLimit(
                    maxRequestsPerWindow = Int.MAX_VALUE, // No limit for local files
                    windowDurationMs = 1000, // Minimal window
                )
        }

        private fun resetWindow(
            provider: SubtitleApiProvider,
            now: Long,
        ) {
            val limit = providerLimits[provider] ?: return
            limit.requestsInWindow = 0
            limit.windowResetTime = now + limit.windowDurationMs
        }

        private fun calculateBackoffDelay(failureCount: Int): Long {
            // Exponential backoff: 1s, 2s, 4s, 8s, max 60s
            val baseDelayMs = 1000L
            val maxDelayMs = 60000L
            val exponentialDelay = baseDelayMs * (1L shl min(failureCount - 1, 6))
            return min(exponentialDelay, maxDelayMs)
        }

        private data class ProviderRateLimit(
            val maxRequestsPerWindow: Int,
            val windowDurationMs: Long,
            var requestsInWindow: Int = 0,
            var windowResetTime: Long = System.currentTimeMillis() + windowDurationMs,
            var totalRequests: Long = 0,
            var failureCount: Int = 0,
            var backoffUntil: Long = 0,
        )
    }

/**
 * Statistics for rate limiting monitoring.
 */
data class RateLimitStatistics(
    val totalRequests: Long,
    val requestsInCurrentWindow: Int,
    val maxRequestsPerWindow: Int,
    val windowDurationMs: Long,
    val failureCount: Int,
    val isInBackoff: Boolean,
    val backoffEndsAt: Long,
)
