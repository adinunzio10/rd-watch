package com.rdwatch.androidtv.network.interceptors

import com.rdwatch.androidtv.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that adds TMDb API key to all requests
 * Follows the TMDb API specification requiring api_key query parameter
 */
@Singleton
class TMDbApiKeyInterceptor @Inject constructor() : Interceptor {
    
    companion object {
        private const val API_KEY_PARAM = "api_key"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Only add API key if it's not already present and we have a valid key
        if (BuildConfig.TMDB_API_KEY.isNotEmpty() && 
            originalRequest.url.queryParameter(API_KEY_PARAM) == null) {
            
            val urlWithApiKey = originalRequest.url.newBuilder()
                .addQueryParameter(API_KEY_PARAM, BuildConfig.TMDB_API_KEY)
                .build()
            
            val newRequest = originalRequest.newBuilder()
                .url(urlWithApiKey)
                .build()
            
            return chain.proceed(newRequest)
        }
        
        return chain.proceed(originalRequest)
    }
}