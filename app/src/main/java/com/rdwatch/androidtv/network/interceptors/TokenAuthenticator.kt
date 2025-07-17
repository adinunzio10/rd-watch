package com.rdwatch.androidtv.network.interceptors

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator
    @Inject
    constructor(
        private val tokenProvider: TokenProvider,
    ) : Authenticator {
        companion object {
            private const val MAX_RETRY_COUNT = 3
            private const val HEADER_RETRY_COUNT = "Retry-Count"
        }

        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            // Get the retry count from the request
            val retryCount = response.request.header(HEADER_RETRY_COUNT)?.toIntOrNull() ?: 0

            // If we've already retried too many times, give up
            if (retryCount >= MAX_RETRY_COUNT) {
                return null
            }

            // If the response is 401, try to refresh the token
            if (response.code == 401) {
                // Synchronously refresh the token
                val refreshSuccessful =
                    runBlocking {
                        tokenProvider.refreshToken()
                    }

                if (refreshSuccessful) {
                    // Get the new access token
                    val newToken = tokenProvider.getAccessToken()

                    if (!newToken.isNullOrBlank()) {
                        // Build a new request with the new token
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .header(HEADER_RETRY_COUNT, (retryCount + 1).toString())
                            .build()
                    }
                }

                // If refresh failed, clear tokens and return null
                tokenProvider.clearTokens()
            }

            return null
        }
    }
