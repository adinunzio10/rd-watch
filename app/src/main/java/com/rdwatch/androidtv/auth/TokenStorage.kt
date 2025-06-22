package com.rdwatch.androidtv.auth

interface TokenStorage {
    suspend fun saveTokens(accessToken: String, refreshToken: String?, expiresIn: Int)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()
    suspend fun isTokenValid(): Boolean
    suspend fun hasRefreshToken(): Boolean
}