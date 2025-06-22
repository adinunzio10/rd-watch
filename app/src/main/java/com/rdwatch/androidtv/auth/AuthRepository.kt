package com.rdwatch.androidtv.auth

import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.models.DeviceCodeInfo
import com.rdwatch.androidtv.network.api.OAuth2ApiService
import com.rdwatch.androidtv.network.models.OAuth2ErrorResponse
import com.rdwatch.androidtv.repository.base.Result
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        private const val CLIENT_ID = "X245A4XAIBGVM"  // Real Debrid client ID
        private const val SCOPE = ""
        private const val POLLING_TIMEOUT_MS = 600_000L  // 10 minutes
    }
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: Flow<AuthState> = _authState.asStateFlow()
    
    suspend fun startDeviceFlow(): Result<DeviceCodeInfo> {
        return try {
            _authState.value = AuthState.Initializing
            
            val response = oauth2ApiService.getDeviceCode(CLIENT_ID, SCOPE)
            
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
        
        while (System.currentTimeMillis() - startTime < POLLING_TIMEOUT_MS) {
            try {
                val response = oauth2ApiService.getDeviceToken(CLIENT_ID, deviceCode)
                
                if (response.isSuccessful) {
                    val tokenResponse = response.body()!!
                    tokenStorage.saveTokens(
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                        expiresIn = tokenResponse.expiresIn
                    )
                    _authState.value = AuthState.Authenticated
                    return Result.Success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
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
        
        _authState.value = AuthState.Error("Authentication timeout")
        return Result.Error(Exception("Authentication timeout"))
    }
    
    suspend fun refreshTokenIfNeeded(): Result<Unit> {
        val refreshToken = tokenStorage.getRefreshToken()
        
        if (refreshToken == null) {
            _authState.value = AuthState.Error("No refresh token available")
            return Result.Error(Exception("No refresh token available"))
        }
        
        return try {
            val response = oauth2ApiService.refreshToken(CLIENT_ID, refreshToken)
            
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
        _authState.value = AuthState.Initializing
    }
    
    suspend fun checkAuthState() {
        if (tokenStorage.isTokenValid()) {
            _authState.value = AuthState.Authenticated
        } else if (tokenStorage.hasRefreshToken()) {
            refreshTokenIfNeeded()
        } else {
            _authState.value = AuthState.Initializing
        }
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