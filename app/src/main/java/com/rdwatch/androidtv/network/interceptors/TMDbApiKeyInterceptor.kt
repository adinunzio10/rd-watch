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
class TMDbApiKeyInterceptor
    @Inject
    constructor() : Interceptor {
        companion object {
            private const val API_KEY_PARAM = "api_key"
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()

            // Log API key status
            android.util.Log.d("TMDbApiKeyInterceptor", "=== API Key Debug Info ===")
            android.util.Log.d("TMDbApiKeyInterceptor", "Request URL: ${originalRequest.url}")
            android.util.Log.d("TMDbApiKeyInterceptor", "BuildConfig.TMDB_API_KEY.isEmpty(): ${BuildConfig.TMDB_API_KEY.isEmpty()}")
            android.util.Log.d("TMDbApiKeyInterceptor", "BuildConfig.TMDB_API_KEY.length: ${BuildConfig.TMDB_API_KEY.length}")
            android.util.Log.d(
                "TMDbApiKeyInterceptor",
                "API key starts with: ${if (BuildConfig.TMDB_API_KEY.isNotEmpty()) BuildConfig.TMDB_API_KEY.take(8) + "..." else "EMPTY"}",
            )
            android.util.Log.d(
                "TMDbApiKeyInterceptor",
                "Request already has api_key param: ${originalRequest.url.queryParameter(API_KEY_PARAM) != null}",
            )

            // Only add API key if it's not already present and we have a valid key
            if (BuildConfig.TMDB_API_KEY.isNotEmpty() &&
                originalRequest.url.queryParameter(API_KEY_PARAM) == null
            ) {
                android.util.Log.d("TMDbApiKeyInterceptor", "Adding API key to request")

                val urlWithApiKey =
                    originalRequest.url.newBuilder()
                        .addQueryParameter(API_KEY_PARAM, BuildConfig.TMDB_API_KEY)
                        .build()

                val newRequest =
                    originalRequest.newBuilder()
                        .url(urlWithApiKey)
                        .build()

                android.util.Log.d("TMDbApiKeyInterceptor", "Final URL with API key: $urlWithApiKey")

                return chain.proceed(newRequest)
            } else {
                android.util.Log.w(
                    "TMDbApiKeyInterceptor",
                    "NOT adding API key - isEmpty: ${BuildConfig.TMDB_API_KEY.isEmpty()}, hasParam: ${originalRequest.url.queryParameter(API_KEY_PARAM) != null}",
                )
            }

            android.util.Log.d("TMDbApiKeyInterceptor", "Proceeding with original request (no API key added)")
            return chain.proceed(originalRequest)
        }
    }
