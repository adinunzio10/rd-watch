package com.rdwatch.androidtv.ui.search

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rate limiter for search requests to prevent overwhelming scrapers
 * Implements per-scraper rate limiting with configurable limits
 */
@Singleton
class SearchRateLimiter
    @Inject
    constructor() {
        private val scraperLimits = ConcurrentHashMap<String, ScraperRateLimit>()
        private val globalSemaphore = Semaphore(permits = 10) // Global concurrent search limit

        // Default rate limiting configuration
        private val defaultConfig =
            RateLimitConfig(
                requestsPerMinute = 30,
                requestsPerHour = 500,
                burstSize = 5,
                cooldownDelayMs = 1000L,
            )

        /**
         * Acquire permission to perform a search for a specific scraper
         */
        suspend fun acquirePermit(
            scraperId: String,
            config: RateLimitConfig = defaultConfig,
        ) {
            // Acquire global permit first
            globalSemaphore.acquire()

            try {
                val scraperLimit =
                    scraperLimits.getOrPut(scraperId) {
                        ScraperRateLimit(scraperId, config)
                    }

                scraperLimit.acquirePermit()
            } catch (e: Exception) {
                // Release global permit if scraper-specific acquisition fails
                globalSemaphore.release()
                throw e
            }
        }

        /**
         * Release permission after search completion
         */
        fun releasePermit(scraperId: String) {
            scraperLimits[scraperId]?.releasePermit()
            globalSemaphore.release()
        }

        /**
         * Check if a scraper is currently rate limited
         */
        fun isRateLimited(scraperId: String): Boolean {
            return scraperLimits[scraperId]?.isRateLimited() ?: false
        }

        /**
         * Get rate limit status for a scraper
         */
        fun getRateLimitStatus(scraperId: String): RateLimitStatus {
            val scraperLimit =
                scraperLimits[scraperId] ?: return RateLimitStatus(
                    scraperId = scraperId,
                    isRateLimited = false,
                    remainingRequests = defaultConfig.requestsPerMinute,
                    resetTimeMs = 0L,
                    currentBurst = 0,
                )

            return scraperLimit.getStatus()
        }

        /**
         * Get rate limit status for all scrapers
         */
        fun getAllRateLimitStatus(): Map<String, RateLimitStatus> {
            return scraperLimits.mapValues { (_, limit) -> limit.getStatus() }
        }

        /**
         * Reset rate limits for a specific scraper
         */
        fun resetScraper(scraperId: String) {
            scraperLimits.remove(scraperId)
        }

        /**
         * Reset all rate limits
         */
        fun resetAll() {
            scraperLimits.clear()
        }

        /**
         * Update configuration for a specific scraper
         */
        fun updateScraperConfig(
            scraperId: String,
            config: RateLimitConfig,
        ) {
            scraperLimits[scraperId]?.updateConfig(config)
        }

        /**
         * Get global search capacity (available permits)
         */
        fun getGlobalCapacity(): Int {
            return globalSemaphore.availablePermits
        }
    }

/**
 * Per-scraper rate limiting implementation
 */
private class ScraperRateLimit(
    private val scraperId: String,
    private var config: RateLimitConfig,
) {
    private val minuteWindow = RateLimitWindow(60_000L) // 1 minute
    private val hourWindow = RateLimitWindow(3_600_000L) // 1 hour
    private val burstSemaphore = Semaphore(config.burstSize)
    private val lastRequestTime = AtomicLong(0L)

    suspend fun acquirePermit() {
        val currentTime = System.currentTimeMillis()

        // Check hour limit
        if (hourWindow.getRequestCount(currentTime) >= config.requestsPerHour) {
            val waitTime = hourWindow.getResetTime(currentTime) - currentTime
            delay(waitTime)
        }

        // Check minute limit
        if (minuteWindow.getRequestCount(currentTime) >= config.requestsPerMinute) {
            val waitTime = minuteWindow.getResetTime(currentTime) - currentTime
            delay(waitTime)
        }

        // Acquire burst permit
        burstSemaphore.acquire()

        // Apply cooldown delay if needed
        val timeSinceLastRequest = currentTime - lastRequestTime.get()
        if (timeSinceLastRequest < config.cooldownDelayMs) {
            delay(config.cooldownDelayMs - timeSinceLastRequest)
        }

        // Record request
        val requestTime = System.currentTimeMillis()
        minuteWindow.addRequest(requestTime)
        hourWindow.addRequest(requestTime)
        lastRequestTime.set(requestTime)
    }

    fun releasePermit() {
        burstSemaphore.release()
    }

    fun isRateLimited(): Boolean {
        val currentTime = System.currentTimeMillis()
        return minuteWindow.getRequestCount(currentTime) >= config.requestsPerMinute ||
            hourWindow.getRequestCount(currentTime) >= config.requestsPerHour ||
            burstSemaphore.availablePermits == 0
    }

    fun getStatus(): RateLimitStatus {
        val currentTime = System.currentTimeMillis()
        val minuteRequests = minuteWindow.getRequestCount(currentTime)
        val hourRequests = hourWindow.getRequestCount(currentTime)

        return RateLimitStatus(
            scraperId = scraperId,
            isRateLimited = isRateLimited(),
            remainingRequests =
                minOf(
                    config.requestsPerMinute - minuteRequests,
                    config.requestsPerHour - hourRequests,
                ),
            resetTimeMs =
                maxOf(
                    minuteWindow.getResetTime(currentTime),
                    hourWindow.getResetTime(currentTime),
                ),
            currentBurst = config.burstSize - burstSemaphore.availablePermits,
        )
    }

    fun updateConfig(newConfig: RateLimitConfig) {
        this.config = newConfig
        // Update burst semaphore if burst size changed
        if (newConfig.burstSize != config.burstSize) {
            // Note: In a real implementation, you might want to handle this more gracefully
        }
    }
}

/**
 * Sliding window for rate limit tracking
 */
private class RateLimitWindow(private val windowSizeMs: Long) {
    private val requests = mutableListOf<Long>()

    @Synchronized
    fun addRequest(timestamp: Long) {
        requests.add(timestamp)
        cleanupOldRequests(timestamp)
    }

    @Synchronized
    fun getRequestCount(currentTime: Long): Int {
        cleanupOldRequests(currentTime)
        return requests.size
    }

    @Synchronized
    fun getResetTime(currentTime: Long): Long {
        cleanupOldRequests(currentTime)
        return if (requests.isEmpty()) {
            currentTime
        } else {
            requests.first() + windowSizeMs
        }
    }

    private fun cleanupOldRequests(currentTime: Long) {
        val cutoffTime = currentTime - windowSizeMs
        requests.removeAll { it < cutoffTime }
    }
}

/**
 * Rate limiting configuration
 */
data class RateLimitConfig(
    val requestsPerMinute: Int = 30,
    val requestsPerHour: Int = 500,
    val burstSize: Int = 5,
    val cooldownDelayMs: Long = 1000L,
) {
    companion object {
        // Predefined configurations for different scraper types
        fun conservative() =
            RateLimitConfig(
                requestsPerMinute = 10,
                requestsPerHour = 200,
                burstSize = 2,
                cooldownDelayMs = 2000L,
            )

        fun standard() =
            RateLimitConfig(
                requestsPerMinute = 30,
                requestsPerHour = 500,
                burstSize = 5,
                cooldownDelayMs = 1000L,
            )

        fun aggressive() =
            RateLimitConfig(
                requestsPerMinute = 60,
                requestsPerHour = 1000,
                burstSize = 10,
                cooldownDelayMs = 500L,
            )

        fun forScraper(scraperType: String): RateLimitConfig {
            return when (scraperType.lowercase()) {
                "public", "free" -> conservative()
                "premium", "paid" -> aggressive()
                else -> standard()
            }
        }
    }
}

/**
 * Current rate limit status for a scraper
 */
data class RateLimitStatus(
    val scraperId: String,
    val isRateLimited: Boolean,
    val remainingRequests: Int,
    val resetTimeMs: Long,
    val currentBurst: Int,
) {
    fun getRemainingTimeMs(): Long {
        return maxOf(0L, resetTimeMs - System.currentTimeMillis())
    }

    fun getRemainingTimeSeconds(): Int {
        return (getRemainingTimeMs() / 1000).toInt()
    }
}
