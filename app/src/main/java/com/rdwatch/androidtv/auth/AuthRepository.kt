package com.rdwatch.androidtv.auth

import android.util.Log
import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.models.DeviceCodeInfo
import com.rdwatch.androidtv.network.api.OAuth2ApiService
import com.rdwatch.androidtv.network.models.OAuth2ErrorResponse
import com.rdwatch.androidtv.repository.base.Result
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val oauth2ApiService: OAuth2ApiService,
    private val tokenStorage: TokenStorage,
    private val moshi: Moshi
) {
    
    companion object {
        private const val TAG = "AuthRepository"
        private const val CLIENT_ID = "X245A4XAIBGVM"  // Real Debrid client ID for open source apps
        private const val POLLING_TIMEOUT_MS = 600_000L  // 10 minutes
    }
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    /**
     * Get current authentication state value
     */
    fun getCurrentAuthState(): AuthState = _authState.value
    
    suspend fun startDeviceFlow(): Result<DeviceCodeInfo> {
        return try {
            _authState.value = AuthState.Initializing
            
            val response = oauth2ApiService.getDeviceCode(CLIENT_ID)
            
            if (response.isSuccessful) {
                val deviceCodeResponse = response.body()!!
                val deviceCodeInfo = DeviceCodeInfo(
                    deviceCode = deviceCodeResponse.deviceCode,
                    userCode = deviceCodeResponse.userCode,
                    verificationUri = deviceCodeResponse.verificationUri,
                    expiresIn = deviceCodeResponse.expiresIn,
                    interval = deviceCodeResponse.interval
                )
                
                _authState.value = AuthState.WaitingForUser(deviceCodeInfo)
                Result.Success(deviceCodeInfo)
            } else {
                val errorBody = response.errorBody()?.string()
                val error = parseError(errorBody)
                _authState.value = AuthState.Error(error)
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to start device flow: ${e.message}"
            _authState.value = AuthState.Error(errorMessage)
            Result.Error(e)
        }
    }
    
    suspend fun pollForToken(deviceCode: String, interval: Int): Result<Unit> {
        val startTime = System.currentTimeMillis()
        var currentInterval = interval * 1000L  // Convert to milliseconds
        
        // Step 2: Poll for credentials (client_id and client_secret)
        var clientId: String? = null
        var clientSecret: String? = null
        
        while (System.currentTimeMillis() - startTime < POLLING_TIMEOUT_MS) {
            try {
                val credentialsResponse = oauth2ApiService.getDeviceCredentials(CLIENT_ID, deviceCode)
                
                if (credentialsResponse.isSuccessful) {
                    val credentials = credentialsResponse.body()!!
                    clientId = credentials.clientId
                    clientSecret = credentials.clientSecret
                    Log.d(TAG, "Received credentials: client_id=$clientId")
                    break
                } else {
                    val errorBody = credentialsResponse.errorBody()?.string()
                    val error = parseError(errorBody)
                    
                    when {
                        error.contains("authorization_pending", ignoreCase = true) -> {
                            // Continue polling
                            delay(currentInterval)
                        }
                        error.contains("slow_down", ignoreCase = true) -> {
                            // Increase polling interval by 5 seconds
                            currentInterval += 5000L
                            delay(currentInterval)
                        }
                        else -> {
                            _authState.value = AuthState.Error(error)
                            return Result.Error(Exception(error))
                        }
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 400) {
                    // Parse the error response
                    val errorBody = e.response()?.errorBody()?.string()
                    val error = parseError(errorBody)
                    
                    when {
                        error.contains("authorization_pending", ignoreCase = true) -> {
                            delay(currentInterval)
                        }
                        error.contains("slow_down", ignoreCase = true) -> {
                            currentInterval += 5000L
                            delay(currentInterval)
                        }
                        else -> {
                            _authState.value = AuthState.Error(error)
                            return Result.Error(e)
                        }
                    }
                } else {
                    _authState.value = AuthState.Error("Network error: ${e.message}")
                    return Result.Error(e)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Unexpected error: ${e.message}")
                return Result.Error(e)
            }
        }
        
        // Check if we got credentials
        if (clientId == null || clientSecret == null) {
            _authState.value = AuthState.Error("Authentication timeout - no credentials received")
            return Result.Error(Exception("Authentication timeout"))
        }
        
        // Step 3: Exchange device code + credentials for access token
        return try {
            val tokenResponse = oauth2ApiService.getDeviceToken(clientId, clientSecret, deviceCode)
            
            if (tokenResponse.isSuccessful) {
                val tokens = tokenResponse.body()!!
                // Store client credentials for future token refresh
                tokenStorage.saveClientCredentials(clientId, clientSecret)
                tokenStorage.saveTokens(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresIn = tokens.expiresIn
                )
                _authState.value = AuthState.Authenticated
                Result.Success(Unit)
            } else {
                val errorBody = tokenResponse.errorBody()?.string()
                val error = parseError(errorBody)
                _authState.value = AuthState.Error(error)
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to exchange device code for tokens: ${e.message}"
            _authState.value = AuthState.Error(errorMessage)
            Result.Error(e)
        }
    }
    
    suspend fun refreshTokenIfNeeded(): Result<Unit> {
        val refreshToken = tokenStorage.getRefreshToken()
        val clientId = tokenStorage.getClientId()
        
        if (refreshToken == null) {
            _authState.value = AuthState.Error("No refresh token available")
            return Result.Error(Exception("No refresh token available"))
        }
        
        if (clientId == null) {
            _authState.value = AuthState.Error("No client credentials available")
            return Result.Error(Exception("No client credentials available"))
        }
        
        return try {
            val response = oauth2ApiService.refreshToken(clientId, refreshToken)
            
            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                tokenStorage.saveTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken ?: refreshToken,
                    expiresIn = tokenResponse.expiresIn
                )
                _authState.value = AuthState.Authenticated
                Result.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val error = parseError(errorBody)
                _authState.value = AuthState.Error(error)
                Result.Error(Exception(error))
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to refresh token: ${e.message}"
            _authState.value = AuthState.Error(errorMessage)
            Result.Error(e)
        }
    }
    
    suspend fun logout() {
        tokenStorage.clearTokens()
        _authState.value = AuthState.Unauthenticated
    }
    
    suspend fun checkAuthState() {
        Log.d(TAG, "checkAuthState() called")
        try {
            Log.d(TAG, "Checking if token is valid...")
            val isTokenValid = tokenStorage.isTokenValid()
            Log.d(TAG, "Token valid: $isTokenValid")
            
            if (isTokenValid) {
                Log.d(TAG, "Token is valid, setting state to Authenticated")
                _authState.value = AuthState.Authenticated
            } else {
                Log.d(TAG, "Token not valid, checking for refresh token...")
                val hasRefreshToken = tokenStorage.hasRefreshToken()
                Log.d(TAG, "Has refresh token: $hasRefreshToken")
                
                if (hasRefreshToken) {
                    Log.d(TAG, "Has refresh token, attempting refresh...")
                    refreshTokenIfNeeded()
                } else {
                    Log.d(TAG, "No refresh token, user needs to authenticate")
                    _authState.value = AuthState.Unauthenticated
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during checkAuthState(): ${e.message}", e)
            _authState.value = AuthState.Error("Failed to check authentication state: ${e.message}")
        }
        Log.d(TAG, "checkAuthState() completed, current state: ${_authState.value}")
    }
    
    private fun parseError(errorBody: String?): String {
        return try {
            if (errorBody != null) {
                val adapter = moshi.adapter(OAuth2ErrorResponse::class.java)
                val errorResponse = adapter.fromJson(errorBody)
                errorResponse?.errorDescription ?: errorResponse?.error ?: "Unknown error"
            } else {
                "Unknown error"
            }
        } catch (e: Exception) {
            errorBody ?: "Unknown error"
        }
    }
}