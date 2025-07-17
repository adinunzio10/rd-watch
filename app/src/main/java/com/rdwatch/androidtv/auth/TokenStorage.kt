package com.rdwatch.androidtv.auth

interface TokenStorage {
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        expiresIn: Int,
    )

    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun clearTokens()

    suspend fun isTokenValid(): Boolean

    suspend fun hasRefreshToken(): Boolean

    // Client credentials storage for Real-Debrid OAuth flow
    suspend fun saveClientCredentials(
        clientId: String,
        clientSecret: String,
    )

    suspend fun getClientId(): String?

    suspend fun getClientSecret(): String?

    suspend fun clearClientCredentials()
}
