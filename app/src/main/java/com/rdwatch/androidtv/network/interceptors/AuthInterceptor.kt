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
        
        // Check for the @NoAuth annotation
        val noAuth = original.tag(retrofit2.Invocation::class.java)
            ?.method()
            ?.isAnnotationPresent(NoAuth::class.java) ?: false

        // If @NoAuth is present, proceed with original request
        if (noAuth) {
            return chain.proceed(original)
        }
        
        // Get authentication credentials (priority: OAuth token, then API key)
        val token = tokenProvider.getAccessToken()
        val apiKey = tokenProvider.getApiKey()
        
        val authToken = token ?: apiKey
        
        // If no authentication credentials, proceed with original request
        if (authToken.isNullOrBlank()) {
            return chain.proceed(original)
        }
        
        // Add authorization header
        val request = original.newBuilder()
            .header("Authorization", "Bearer $authToken")
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
    fun saveClientCredentials(clientId: String, clientSecret: String)
    fun getClientId(): String?
    fun getClientSecret(): String?
    fun saveApiKey(apiKey: String)
    fun getApiKey(): String?
    fun clearApiKey()
}