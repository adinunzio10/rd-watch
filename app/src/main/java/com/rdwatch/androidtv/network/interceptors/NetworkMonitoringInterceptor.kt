package com.rdwatch.androidtv.network.interceptors

import android.util.Log
import com.rdwatch.androidtv.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitoringInterceptor @Inject constructor() : Interceptor {
    
    companion object {
        private const val TAG = "NetworkMonitor"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        val requestLog = buildString {
            appendLine("⬆️ REQUEST: ${request.method} ${request.url}")
            appendLine("Headers: ${request.headers.size}")
            request.body?.let {
                appendLine("Body: ${it.contentType()}, ${it.contentLength()} bytes")
            }
        }
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, requestLog)
        }
        
        return try {
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - startTime
            
            val responseLog = buildString {
                appendLine("⬇️ RESPONSE: ${response.code} ${response.message} (${duration}ms)")
                appendLine("From: ${request.url}")
                appendLine("Headers: ${response.headers.size}")
                response.body?.let {
                    appendLine("Body: ${it.contentType()}, ${it.contentLength()} bytes")
                }
            }
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, responseLog)
            }
            
            // Track performance metrics (could be sent to analytics)
            trackPerformance(request.url.toString(), response.code, duration)
            
            response
        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime
            
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "❌ FAILED: ${request.url} after ${duration}ms", e)
            }
            
            // Track failure metrics
            trackFailure(request.url.toString(), e, duration)
            
            throw e
        }
    }
    
    private fun trackPerformance(url: String, responseCode: Int, durationMs: Long) {
        // TODO: Implement analytics tracking
        // This could send metrics to Firebase Analytics, Crashlytics, or custom analytics
        
        // Log slow requests
        if (durationMs > 3000) {
            Log.w(TAG, "Slow request detected: $url took ${durationMs}ms")
        }
        
        // Log errors
        if (responseCode >= 400) {
            Log.e(TAG, "Error response: $responseCode for $url")
        }
    }
    
    private fun trackFailure(url: String, error: IOException, durationMs: Long) {
        // TODO: Implement error tracking
        // This could send errors to Crashlytics or custom error tracking service
        
        Log.e(TAG, "Network failure for $url after ${durationMs}ms: ${error.message}")
    }
}