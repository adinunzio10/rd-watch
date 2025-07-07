package com.rdwatch.androidtv.auth

import android.util.Log
import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.models.DeviceCodeInfo
import com.rdwatch.androidtv.network.api.OAuth2ApiService
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.network.interceptors.TokenProvider
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
    private val realDebridApiService: RealDebridApiService,
    private val tokenProvider: TokenProvider,
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

    suspend fun authenticateWithApiKey(apiKey: String): Result<Unit> {
        return try {
            _authState.value = AuthState.Initializing
            
            // Save the API key temporarily to test it
            val originalApiKey = tokenProvider.getApiKey()
            tokenProvider.saveApiKey(apiKey)
            
            // Test the API key by making a request to the user endpoint
            val response = realDebridApiService.getUserInfo()
            
            if (response.isSuccessful) {
                // API key is valid, clear any existing OAuth tokens and keep the API key
                tokenProvider.clearTokens()
                _authState.value = AuthState.Authenticated
                Result.Success(Unit)
            } else {
                // API key is invalid, restore original key (if any) and show error
                if (originalApiKey != null) {
                    tokenProvider.saveApiKey(originalApiKey)
                } else {
                    tokenProvider.clearApiKey()
                }
                val errorMessage = "Invalid API key: ${response.code()} ${response.message()}"
                _authState.value = AuthState.Error(errorMessage)
                Result.Error(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to validate API key: ${e.message}"
            _authState.value = AuthState.Error(errorMessage)
            Result.Error(e)
        }
    }
    
    fun startApiKeyAuthentication() {
        _authState.value = AuthState.ApiKeyEntry
    }
    
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
                    
                    // Save the credentials
                    tokenProvider.saveClientCredentials(clientId, clientSecret)
                    
                    break
                } else {
                    val errorBody = credentialsResponse.errorBody()?.string()
                    val error = parseError(errorBody)
                    Log.d(TAG, "Credentials polling error (expected until user authorizes): $error")
                    
                    when {
                        error.contains("slow_down", ignoreCase = true) -> {
                            // Increase polling interval by 5 seconds
                            currentInterval += 5000L
                            delay(currentInterval)
                        }
                        else -> {
                            // Continue polling - errors are expected until user authorizes
                            delay(currentInterval)
                        }
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 400 || e.code() == 403) {
                    // Parse the error response - 400/403 errors are expected until user authorizes
                    val errorBody = e.response()?.errorBody()?.string()
                    val error = parseError(errorBody)
                    Log.d(TAG, "HTTP ${e.code()} during credentials polling (expected): $error")
                    
                    when {
                        error.contains("slow_down", ignoreCase = true) -> {
                            currentInterval += 5000L
                            delay(currentInterval)
                        }
                        else -> {
                            // Continue polling - errors are expected until user authorizes
                            delay(currentInterval)
                        }
                    }
                } else {
                    // Only stop on serious network errors (5xx, etc.)
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
        
        // Save client credentials
        tokenProvider.saveClientCredentials(clientId, clientSecret)
        
        // Step 3: Exchange device code + credentials for access token
        return try {
            val tokenResponse = oauth2ApiService.getDeviceToken(clientId, clientSecret, deviceCode)
            
            if (tokenResponse.isSuccessful) {
                val tokens = tokenResponse.body()!!
                tokenProvider.saveTokens(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken
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
        val refreshToken = tokenProvider.getRefreshToken()
        val clientId = tokenProvider.getClientId()
        val clientSecret = tokenProvider.getClientSecret()

        if (refreshToken == null || clientId == null || clientSecret == null) {
            _authState.value = AuthState.Error("Missing credentials for refresh")
            return Result.Error(Exception("Missing credentials for refresh"))
        }
        
        return try {
            val response = oauth2ApiService.refreshToken(clientId, clientSecret, refreshToken)
            
            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                tokenProvider.saveTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken ?: refreshToken
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
        tokenProvider.clearTokens()
        tokenProvider.clearApiKey()
        _authState.value = AuthState.Unauthenticated
    }
    
    suspend fun checkAuthState() {
        Log.d(TAG, "checkAuthState() called")
        
        // Don't interfere with ongoing authentication flows
        val currentState = _authState.value
        if (currentState is AuthState.WaitingForUser) {
            Log.d(TAG, "Currently in device flow (WaitingForUser), not overriding state")
            return
        }
        
        try {
            Log.d(TAG, "Checking if token is valid...")
            val accessToken = tokenProvider.getAccessToken()
            val apiKey = tokenProvider.getApiKey()
            
            if (accessToken != null) {
                Log.d(TAG, "OAuth token is valid, setting state to Authenticated")
                _authState.value = AuthState.Authenticated
            } else if (apiKey != null) {
                Log.d(TAG, "API key found, validating...")
                // Validate API key by making a test request
                try {
                    val response = realDebridApiService.getUserInfo()
                    if (response.isSuccessful) {
                        Log.d(TAG, "API key is valid, setting state to Authenticated")
                        _authState.value = AuthState.Authenticated
                    } else {
                        Log.d(TAG, "API key is invalid, clearing and requiring authentication")
                        tokenProvider.clearApiKey()
                        _authState.value = AuthState.Unauthenticated
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error validating API key: ${e.message}")
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                Log.d(TAG, "No access token or API key, checking for refresh token...")
                val refreshToken = tokenProvider.getRefreshToken()
                
                if (refreshToken != null) {
                    Log.d(TAG, "Has refresh token, attempting refresh...")
                    refreshTokenIfNeeded()
                } else {
                    Log.d(TAG, "No credentials found, user needs to authenticate")
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