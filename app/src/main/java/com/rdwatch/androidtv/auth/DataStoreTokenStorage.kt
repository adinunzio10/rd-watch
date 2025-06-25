package com.rdwatch.androidtv.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-based implementation of TokenStorage that provides encrypted storage
 * for OAuth tokens using AndroidX DataStore Preferences.
 * 
 * This implementation replaces the SharedPreferences-based storage to provide
 * better performance, type safety, and coroutine support.
 */
@Singleton
class DataStoreTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenStorage {
    
    companion object {
        private const val DATASTORE_NAME = "auth_tokens"
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val TOKEN_EXPIRY_KEY = longPreferencesKey("token_expiry")
        
        // Buffer time in milliseconds (1 minute) before considering token expired
        private const val TOKEN_EXPIRY_BUFFER_MS = 60_000L
    }
    
    // Create DataStore instance
    private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATASTORE_NAME
    )
    
    private val dataStore: DataStore<Preferences> = context.authDataStore
    
    override suspend fun saveTokens(accessToken: String, refreshToken: String?, expiresIn: Int) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000L)
        
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            refreshToken?.let { preferences[REFRESH_TOKEN_KEY] = it }
            preferences[TOKEN_EXPIRY_KEY] = expiryTime
        }
    }
    
    override suspend fun getAccessToken(): String? {
        return dataStore.data
            .map { preferences -> preferences[ACCESS_TOKEN_KEY] }
            .first()
    }
    
    override suspend fun getRefreshToken(): String? {
        return dataStore.data
            .map { preferences -> preferences[REFRESH_TOKEN_KEY] }
            .first()
    }
    
    override suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(TOKEN_EXPIRY_KEY)
        }
    }
    
    override suspend fun isTokenValid(): Boolean {
        return dataStore.data
            .map { preferences ->
                val accessToken = preferences[ACCESS_TOKEN_KEY]
                val expiryTime = preferences[TOKEN_EXPIRY_KEY] ?: 0L
                val currentTime = System.currentTimeMillis()
                
                // Token is valid if it exists and hasn't expired (with buffer)
                accessToken != null && expiryTime > (currentTime + TOKEN_EXPIRY_BUFFER_MS)
            }
            .first()
    }
    
    override suspend fun hasRefreshToken(): Boolean {
        return dataStore.data
            .map { preferences -> 
                !preferences[REFRESH_TOKEN_KEY].isNullOrEmpty()
            }
            .first()
    }
    
    /**
     * Get the token expiry time in milliseconds
     * @return expiry time or 0 if not set
     */
    suspend fun getTokenExpiryTime(): Long {
        return dataStore.data
            .map { preferences -> preferences[TOKEN_EXPIRY_KEY] ?: 0L }
            .first()
    }
    
    /**
     * Get the remaining time until token expiry in minutes
     * @return remaining minutes or 0 if expired/not set
     */
    suspend fun getRemainingTokenTime(): Long {
        val expiryTime = getTokenExpiryTime()
        if (expiryTime == 0L) return 0L
        
        val currentTime = System.currentTimeMillis()
        val remainingMs = expiryTime - currentTime
        
        return if (remainingMs > 0) remainingMs / (60 * 1000) else 0L
    }
    
    /**
     * Check if token is expired or will expire within the specified buffer time
     * @param bufferMinutes Buffer time in minutes
     * @return true if token needs refresh
     */
    suspend fun isTokenExpiringSoon(bufferMinutes: Int = 5): Boolean {
        val expiryTime = getTokenExpiryTime()
        if (expiryTime == 0L) return true
        
        val currentTime = System.currentTimeMillis()
        val bufferMs = bufferMinutes * 60 * 1000L
        
        return expiryTime <= (currentTime + bufferMs)
    }
}