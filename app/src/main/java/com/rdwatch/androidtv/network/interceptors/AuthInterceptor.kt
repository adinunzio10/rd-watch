package com.rdwatch.androidtv.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        
        // Get the current token
        val token = tokenProvider.getAccessToken()
        
        // If no token, proceed with original request
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }
        
        // Add authorization header
        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        
        return chain.proceed(request)
    }
}

interface TokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    suspend fun refreshToken(): Boolean
    fun clearTokens()
    fun saveTokens(accessToken: String, refreshToken: String?)
}