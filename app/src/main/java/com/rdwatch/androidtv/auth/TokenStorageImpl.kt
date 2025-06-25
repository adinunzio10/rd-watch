package com.rdwatch.androidtv.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenStorage {
    
    companion object {
        private const val PREFS_FILE_NAME = "auth_tokens"
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_EXPIRY_KEY = "token_expiry"
        private const val CLIENT_ID_KEY = "client_id"
        private const val CLIENT_SECRET_KEY = "client_secret"
    }
    
    private val sharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    override suspend fun saveTokens(accessToken: String, refreshToken: String?, expiresIn: Int) {
        withContext(Dispatchers.IO) {
            val expiryTime = System.currentTimeMillis() + (expiresIn * 1000L)
            
            sharedPreferences.edit()
                .putString(ACCESS_TOKEN_KEY, accessToken)
                .putString(REFRESH_TOKEN_KEY, refreshToken)
                .putLong(TOKEN_EXPIRY_KEY, expiryTime)
                .apply()
        }
    }
    
    override suspend fun getAccessToken(): String? {
        return withContext(Dispatchers.IO) {
            sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
        }
    }
    
    override suspend fun getRefreshToken(): String? {
        return withContext(Dispatchers.IO) {
            sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
        }
    }
    
    override suspend fun clearTokens() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .remove(ACCESS_TOKEN_KEY)
                .remove(REFRESH_TOKEN_KEY)
                .remove(TOKEN_EXPIRY_KEY)
                .remove(CLIENT_ID_KEY)
                .remove(CLIENT_SECRET_KEY)
                .apply()
        }
    }
    
    override suspend fun isTokenValid(): Boolean {
        return withContext(Dispatchers.IO) {
            val accessToken = sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
            val expiryTime = sharedPreferences.getLong(TOKEN_EXPIRY_KEY, 0L)
            val currentTime = System.currentTimeMillis()
            
            accessToken != null && expiryTime > currentTime + 60_000L // 1 minute buffer
        }
    }
    
    override suspend fun hasRefreshToken(): Boolean {
        return withContext(Dispatchers.IO) {
            !sharedPreferences.getString(REFRESH_TOKEN_KEY, null).isNullOrEmpty()
        }
    }
    
    override suspend fun saveClientCredentials(clientId: String, clientSecret: String) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .putString(CLIENT_ID_KEY, clientId)
                .putString(CLIENT_SECRET_KEY, clientSecret)
                .apply()
        }
    }
    
    override suspend fun getClientId(): String? {
        return withContext(Dispatchers.IO) {
            sharedPreferences.getString(CLIENT_ID_KEY, null)
        }
    }
    
    override suspend fun getClientSecret(): String? {
        return withContext(Dispatchers.IO) {
            sharedPreferences.getString(CLIENT_SECRET_KEY, null)
        }
    }
    
    override suspend fun clearClientCredentials() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .remove(CLIENT_ID_KEY)
                .remove(CLIENT_SECRET_KEY)
                .apply()
        }
    }
}