package com.rdwatch.androidtv.scraper.api

import com.rdwatch.androidtv.scraper.models.ScraperManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP client for making API calls to scraper services
 * Handles timeouts, retries, and error handling
 */
@Singleton
class ScraperApiClient @Inject constructor() {
    
    companion object {
        private const val DEFAULT_TIMEOUT_SECONDS = 15L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    /**
     * Make an API call to a scraper endpoint
     */
    suspend fun makeScraperRequest(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ScraperApiResponse = withContext(Dispatchers.IO) {
        println("DEBUG [ScraperApiClient]: Making request to: $url")
        
        var lastException: Exception? = null
        
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                val response = withTimeout(DEFAULT_TIMEOUT_SECONDS * 1000) {
                    executeRequest(url, headers)
                }
                
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    println("DEBUG [ScraperApiClient]: Request successful, response length: ${body.length}")
                    return@withContext ScraperApiResponse.Success(
                        data = body,
                        statusCode = response.code,
                        headers = response.headers.toMultimap()
                    )
                } else {
                    println("DEBUG [ScraperApiClient]: Request failed with status: ${response.code}")
                    return@withContext ScraperApiResponse.Error(
                        message = "HTTP ${response.code}: ${response.message}",
                        statusCode = response.code,
                        throwable = null
                    )
                }
            } catch (e: Exception) {
                lastException = e
                println("DEBUG [ScraperApiClient]: Request attempt ${attempt + 1} failed: ${e.message}")
                
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        
        println("DEBUG [ScraperApiClient]: All retry attempts failed")
        ScraperApiResponse.Error(
            message = "Request failed after $MAX_RETRY_ATTEMPTS attempts: ${lastException?.message}",
            statusCode = 0,
            throwable = lastException
        )
    }
    
    /**
     * Execute HTTP request
     */
    private fun executeRequest(url: String, headers: Map<String, String>): Response {
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
        queryParams: Map<String, String> = emptyMap()
    ): String {
        val baseUrl = manifest.baseUrl.trimEnd('/')
        val cleanEndpoint = endpoint.trimStart('/')
        
        val url = StringBuilder("$baseUrl/$cleanEndpoint")
        
        if (queryParams.isNotEmpty()) {
            url.append("?")
            url.append(queryParams.entries.joinToString("&") { (key, value) ->
                "$key=${java.net.URLEncoder.encode(value, "UTF-8")}"
            })
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
}

/**
 * Response wrapper for scraper API calls
 */
sealed class ScraperApiResponse {
    data class Success(
        val data: String,
        val statusCode: Int,
        val headers: Map<String, List<String>>
    ) : ScraperApiResponse()
    
    data class Error(
        val message: String,
        val statusCode: Int,
        val throwable: Throwable?
    ) : ScraperApiResponse()
}