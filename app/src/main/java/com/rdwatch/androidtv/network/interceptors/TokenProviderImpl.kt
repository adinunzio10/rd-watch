package com.rdwatch.androidtv.network.interceptors

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenProvider {
    
    companion object {
        private const val PREFS_NAME = "secure_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_CLIENT_SECRET = "client_secret"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    override fun getAccessToken(): String? {
        return securePrefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    override fun getRefreshToken(): String? {
        return securePrefs.getString(KEY_REFRESH_TOKEN, null)
    }
    
    override suspend fun refreshToken(): Boolean {
        // TODO: Implement actual token refresh logic with Real-Debrid API
        // This is a placeholder implementation
        val refreshToken = getRefreshToken() ?: return false
        
        // In a real implementation, you would:
        // 1. Call the Real-Debrid token refresh endpoint
        // 2. Parse the response
        // 3. Save the new tokens
        // 4. Return true if successful, false otherwise
        
        return false
    }
    
    override fun clearTokens() {
        securePrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }
    
    override fun saveTokens(accessToken: String, refreshToken: String?) {
        val editor = securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
        
        if (!refreshToken.isNullOrBlank()) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        
        editor.apply()
    }

    override fun saveClientCredentials(clientId: String, clientSecret: String) {
        securePrefs.edit()
            .putString(KEY_CLIENT_ID, clientId)
            .putString(KEY_CLIENT_SECRET, clientSecret)
            .apply()
    }

    override fun getClientId(): String? {
        return securePrefs.getString(KEY_CLIENT_ID, null)
    }

    override fun getClientSecret(): String? {
        return securePrefs.getString(KEY_CLIENT_SECRET, null)
    }
}