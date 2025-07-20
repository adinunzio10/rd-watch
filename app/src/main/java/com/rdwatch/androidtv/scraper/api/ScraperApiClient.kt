package com.rdwatch.androidtv.scraper.api

import com.rdwatch.androidtv.network.CircuitBreaker
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * HTTP client for making API calls to scraper services
 * Handles timeouts, retries, error handling, and circuit breaker pattern
 */
@Singleton
class ScraperApiClient
    @Inject
    constructor() {
        companion object {
            private const val DEFAULT_TIMEOUT_SECONDS = 15L
            private const val MAX_RETRY_ATTEMPTS = 3
            private const val RETRY_DELAY_MS = 1000L
        }

        private val httpClient =
            OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()

        // Circuit breakers per scraper service
        private val circuitBreakers = ConcurrentHashMap<String, CircuitBreaker>()

        /**
         * Make an API call to a scraper endpoint with circuit breaker protection
         */
        suspend fun makeScraperRequest(
            url: String,
            headers: Map<String, String> = emptyMap(),
        ): ScraperApiResponse =
            withContext(Dispatchers.IO) {
                val serviceName = extractServiceName(url)
                val circuitBreaker = getOrCreateCircuitBreaker(serviceName)

                println("DEBUG [ScraperApiClient]: Making request to: $url (service: $serviceName)")

                when (val result = circuitBreaker.execute { performRequest(url, headers) }) {
                    is CircuitBreaker.Result.Success -> {
                        println("DEBUG [ScraperApiClient]: Request successful via circuit breaker")
                        result.value
                    }
                    is CircuitBreaker.Result.Failure -> {
                        println("DEBUG [ScraperApiClient]: Request failed: ${result.exception.message}")
                        ScraperApiResponse.Error(
                            message = "Request failed: ${result.exception.message}",
                            statusCode = 0,
                            throwable = result.exception,
                        )
                    }
                    is CircuitBreaker.Result.CircuitOpen -> {
                        println("DEBUG [ScraperApiClient]: Circuit breaker is OPEN for $serviceName")
                        ScraperApiResponse.Error(
                            message = result.message,
                            statusCode = 503, // Service Unavailable
                            throwable = null,
                        )
                    }
                    is CircuitBreaker.Result.FallbackUsed -> {
                        println("DEBUG [ScraperApiClient]: Using fallback response for $serviceName")
                        result.value as ScraperApiResponse
                    }
                }
            }

        /**
         * Perform the actual HTTP request with retries
         */
        private suspend fun performRequest(
            url: String,
            headers: Map<String, String>,
        ): ScraperApiResponse {
            var lastException: Exception? = null

            repeat(MAX_RETRY_ATTEMPTS) { attempt ->
                try {
                    val response =
                        withTimeout(DEFAULT_TIMEOUT_SECONDS * 1000) {
                            executeRequest(url, headers)
                        }

                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        println("DEBUG [ScraperApiClient]: Request successful, response length: ${body.length}")
                        return ScraperApiResponse.Success(
                            data = body,
                            statusCode = response.code,
                            headers = response.headers.toMultimap(),
                        )
                    } else {
                        println("DEBUG [ScraperApiClient]: Request failed with status: ${response.code}")
                        // For HTTP errors, don't retry - circuit breaker will handle
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }
                } catch (e: Exception) {
                    lastException = e
                    println("DEBUG [ScraperApiClient]: Request attempt ${attempt + 1} failed: ${e.message}")

                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                    }
                }
            }

            throw lastException ?: Exception("Request failed after $MAX_RETRY_ATTEMPTS attempts")
        }

        /**
         * Extract service name from URL for circuit breaker identification
         */
        private fun extractServiceName(url: String): String {
            return try {
                val host = java.net.URL(url).host
                host.lowercase().replace(".", "-")
            } catch (e: Exception) {
                "unknown-scraper"
            }
        }

        /**
         * Get or create a circuit breaker for a specific service
         */
        private fun getOrCreateCircuitBreaker(serviceName: String): CircuitBreaker {
            return circuitBreakers.computeIfAbsent(serviceName) {
                CircuitBreaker(
                    serviceName = "scraper:$serviceName",
                    failureThreshold = 5,
                    recoveryTimeout = 60.seconds,
                    fallbackFunction = {
                        android.util.Log.d("CircuitBreaker", "Using fallback for scraper $serviceName")
                        ScraperApiResponse.Success(
                            data = "[]", // Empty JSON array as fallback
                            statusCode = 200,
                            headers = emptyMap(),
                        )
                    },
                )
            }
        }

        /**
         * Execute HTTP request
         */
        private fun executeRequest(
            url: String,
            headers: Map<String, String>,
        ): Response {
            val requestBuilder = Request.Builder().url(url)

            // Add custom headers
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            // Add default headers
            requestBuilder.addHeader("User-Agent", "RDWatch-AndroidTV/1.0")
            requestBuilder.addHeader("Accept", "application/json")

            val request = requestBuilder.build()
            return httpClient.newCall(request).execute()
        }

        /**
         * Build request URL for a scraper manifest
         */
        fun buildScraperUrl(
            manifest: ScraperManifest,
            endpoint: String,
            queryParams: Map<String, String> = emptyMap(),
        ): String {
            val baseUrl = manifest.baseUrl.trimEnd('/')
            val cleanEndpoint = endpoint.trimStart('/')

            val url = StringBuilder("$baseUrl/$cleanEndpoint")

            if (queryParams.isNotEmpty()) {
                url.append("?")
                url.append(
                    queryParams.entries.joinToString("&") { (key, value) ->
                        "$key=${java.net.URLEncoder.encode(value, "UTF-8")}"
                    },
                )
            }

            return url.toString()
        }

        /**
         * Parse JSON response safely
         */
        fun parseJsonResponse(responseBody: String): JSONObject? {
            return try {
                JSONObject(responseBody)
            } catch (e: Exception) {
                println("DEBUG [ScraperApiClient]: Failed to parse JSON response: ${e.message}")
                null
            }
        }

        /**
         * Get circuit breaker metrics for monitoring
         */
        fun getCircuitBreakerMetrics(): Map<String, CircuitBreaker.Metrics> {
            return circuitBreakers.mapValues { (_, breaker) -> breaker.getMetrics() }
        }

        /**
         * Reset circuit breaker for a specific service
         */
        suspend fun resetCircuitBreaker(serviceName: String) {
            circuitBreakers[serviceName]?.reset()
        }

        /**
         * Reset all circuit breakers
         */
        suspend fun resetAllCircuitBreakers() {
            circuitBreakers.values.forEach { it.reset() }
        }
    }

/**
 * Response wrapper for scraper API calls
 */
sealed class ScraperApiResponse {
    data class Success(
        val data: String,
        val statusCode: Int,
        val headers: Map<String, List<String>>,
    ) : ScraperApiResponse()

    data class Error(
        val message: String,
        val statusCode: Int,
        val throwable: Throwable?,
    ) : ScraperApiResponse()
}
