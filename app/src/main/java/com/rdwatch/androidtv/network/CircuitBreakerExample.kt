package com.rdwatch.androidtv.network

/**
 * Example usage of the circuit breaker pattern implementation
 * This demonstrates how to use circuit breakers for external service calls
 */
object CircuitBreakerExample {
    /**
     * Example of using circuit breaker with scraper services
     */
    suspend fun exampleScraperUsage() {
        // Create a circuit breaker for a scraper service
        val scraperCircuitBreaker = CircuitBreakerFactory.createScraperCircuitBreaker("torrentio")

        // Example scraper API call with circuit breaker protection
        when (
            val result =
                scraperCircuitBreaker.execute {
                    // Simulate scraper API call that might fail
                    performScraperCall("https://torrentio.strem.fun/stream/...")
                }
        ) {
            is CircuitBreaker.Result.Success -> {
                android.util.Log.d("CircuitBreakerExample", "Scraper call successful: ${result.value}")
            }
            is CircuitBreaker.Result.Failure -> {
                android.util.Log.w("CircuitBreakerExample", "Scraper call failed: ${result.exception.message}")
            }
            is CircuitBreaker.Result.CircuitOpen -> {
                android.util.Log.w("CircuitBreakerExample", "Scraper circuit is OPEN: ${result.message}")
                // Use cached data or skip this scraper
            }
            is CircuitBreaker.Result.FallbackUsed -> {
                android.util.Log.i("CircuitBreakerExample", "Using fallback for scraper")
            }
        }
    }

    /**
     * Example of using circuit breaker with Real-Debrid API
     */
    suspend fun exampleRealDebridUsage() {
        // Create a circuit breaker for Real-Debrid API
        val realDebridCircuitBreaker = CircuitBreakerFactory.createRealDebridCircuitBreaker()

        // Example Real-Debrid API call with circuit breaker protection
        when (
            val result =
                realDebridCircuitBreaker.execute {
                    // Simulate Real-Debrid API call that might fail
                    performRealDebridCall("https://api.real-debrid.com/rest/1.0/unrestrict/link")
                }
        ) {
            is CircuitBreaker.Result.Success -> {
                android.util.Log.d("CircuitBreakerExample", "Real-Debrid call successful: ${result.value}")
            }
            is CircuitBreaker.Result.Failure -> {
                android.util.Log.w("CircuitBreakerExample", "Real-Debrid call failed: ${result.exception.message}")
                // Return original URL as fallback
            }
            is CircuitBreaker.Result.CircuitOpen -> {
                android.util.Log.w("CircuitBreakerExample", "Real-Debrid circuit is OPEN: ${result.message}")
                // Return original URL as fallback
            }
            is CircuitBreaker.Result.FallbackUsed -> {
                // Real-Debrid circuit breaker has no fallback by design
                android.util.Log.i("CircuitBreakerExample", "This shouldn't happen for Real-Debrid")
            }
        }
    }

    /**
     * Example of monitoring circuit breaker health
     */
    fun exampleMonitoring() {
        // Create circuit breaker monitor
        val monitor = CircuitBreakerMonitor()

        // Register circuit breakers for monitoring
        val scraperBreaker = CircuitBreakerFactory.createScraperCircuitBreaker("torrentio")
        val realDebridBreaker = CircuitBreakerFactory.createRealDebridCircuitBreaker()

        monitor.registerCircuitBreaker("torrentio", scraperBreaker)
        monitor.registerCircuitBreaker("real-debrid", realDebridBreaker)

        // Start monitoring
        monitor.startMonitoring()

        // Get health summary
        val healthSummary = monitor.getHealthSummary()
        android.util.Log.i(
            "CircuitBreakerExample",
            "Circuit Breaker Health: ${healthSummary.healthPercentage}% " +
                "(${healthSummary.closedCircuits}/${healthSummary.totalCircuitBreakers} healthy)",
        )

        // Get metrics for specific circuit breaker
        val scraperMetrics = monitor.getMetrics("torrentio")
        scraperMetrics?.let { metrics ->
            android.util.Log.d(
                "CircuitBreakerExample",
                "Torrentio Metrics - State: ${metrics.state}, " +
                    "Successes: ${metrics.successCount}, " +
                    "Failures: ${metrics.failureCount}",
            )
        }

        // Log status
        monitor.logStatus()
    }

    /**
     * Simulate a scraper API call that might fail
     */
    private suspend fun performScraperCall(url: String): String {
        // Simulate network call
        kotlinx.coroutines.delay(100)

        // Simulate failure scenario (for demonstration)
        if (url.contains("fail")) {
            throw Exception("Scraper service unavailable")
        }

        return """
            {
                "streams": [
                    {
                        "title": "Sample Stream 1080p",
                        "url": "magnet:?xt=urn:btih:example"
                    }
                ]
            }
            """.trimIndent()
    }

    /**
     * Simulate a Real-Debrid API call that might fail
     */
    private suspend fun performRealDebridCall(url: String): String {
        // Simulate network call
        kotlinx.coroutines.delay(50)

        // Simulate failure scenario (for demonstration)
        if (url.contains("fail")) {
            throw Exception("Real-Debrid API error: Rate limit exceeded")
        }

        return "https://download.real-debrid.com/d/ABC123/filename.mkv"
    }
}

/**
 * Integration points summary:
 *
 * 1. ScraperApiClient: Already integrated with per-service circuit breakers
 *    - Automatically creates circuit breakers for each scraper domain
 *    - Provides fallback empty JSON responses when circuit is open
 *    - Handles failures gracefully with exponential backoff
 *
 * 2. MediaUrlResolver: Integrated with Real-Debrid circuit breaker
 *    - Protects Real-Debrid API calls from cascading failures
 *    - Returns original URL as fallback when circuit is open
 *    - More sensitive failure threshold for paid service
 *
 * 3. Circuit Breaker States:
 *    - CLOSED: Normal operation, requests pass through
 *    - OPEN: Service down, failing fast with fallbacks
 *    - HALF_OPEN: Testing recovery with limited requests
 *
 * 4. Configuration:
 *    - Scraper services: 5 failure threshold, 60s recovery
 *    - Real-Debrid API: 3 failure threshold, 30s recovery
 *    - TMDb API: 5 failure threshold, 2min recovery
 *
 * 5. Monitoring:
 *    - Real-time health monitoring via CircuitBreakerMonitor
 *    - Metrics collection and alerting
 *    - Health summary and status reporting
 *    - Event flows for reactive monitoring
 */
