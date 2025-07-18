package com.rdwatch.androidtv.auth.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.auth.AuthRepository
import com.rdwatch.androidtv.auth.QRCodeGenerator
import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.models.DeviceCodeInfo
import com.rdwatch.androidtv.repository.base.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val qrCodeGenerator: QRCodeGenerator,
    ) : ViewModel() {
        companion object {
            private const val TAG = "AuthViewModel"
        }

        private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
        val authState: StateFlow<AuthState> = _authState.asStateFlow()

        private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
        val qrCodeBitmap: StateFlow<Bitmap?> = _qrCodeBitmap.asStateFlow()

        init {
            // Observe auth state from repository
            viewModelScope.launch {
                authRepository.authState.collect { state ->
                    Log.d(TAG, "Repository auth state changed to: $state")
                    _authState.value = state
                    Log.d(TAG, "ViewModel auth state updated to: ${_authState.value}")

                    // Generate QR code when waiting for user
                    if (state is AuthState.WaitingForUser) {
                        generateQRCode(state.deviceCodeInfo)
                        startPolling(state.deviceCodeInfo)
                    }
                }
            }

            // Check initial auth state
            viewModelScope.launch {
                authRepository.checkAuthState()
            }
        }

        fun startAuthentication() {
            viewModelScope.launch {
                // Reset state and retry count
                _authState.value = AuthState.Initializing
                retryAuthenticationInternal()
            }
        }

        private suspend fun retryAuthenticationInternal(retryCount: Int = 0) {
            val maxRetries = 3

            when (val result = authRepository.startDeviceFlow()) {
                is Result.Success -> {
                    Log.d(TAG, "Device flow started successfully")
                    // State will be updated through the repository flow
                }
                is Result.Error -> {
                    val errorMessage = result.exception.message ?: "Unknown error"
                    Log.e(TAG, "Device flow error (attempt ${retryCount + 1}/$maxRetries): $errorMessage")

                    // Check if we should retry
                    if (retryCount < maxRetries - 1 && result.exception is Exception && shouldRetry(result.exception)) {
                        Log.d(TAG, "Retrying device flow after delay...")
                        delay(2000) // Wait 2 seconds before retry
                        retryAuthenticationInternal(retryCount + 1)
                    } else {
                        Log.e(TAG, "Max retries reached or non-retryable error")
                        _authState.value = AuthState.Error("Failed to start authentication: $errorMessage")
                    }
                }
                is Result.Loading -> {
                    _authState.value = AuthState.Initializing
                }
            }
        }

        private fun shouldRetry(exception: Exception): Boolean {
            // Retry on network errors but not on client errors
            return when (exception) {
                is java.net.UnknownHostException,
                is java.net.SocketTimeoutException,
                is kotlinx.coroutines.TimeoutCancellationException,
                -> true
                else ->
                    exception.message?.contains("timeout", ignoreCase = true) == true ||
                        exception.message?.contains("network", ignoreCase = true) == true
            }
        }

        private fun generateQRCode(deviceCodeInfo: DeviceCodeInfo) {
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Generating QR code for: ${deviceCodeInfo.verificationUriComplete}")
                    val bitmap =
                        qrCodeGenerator.generateTVOptimizedQRCode(
                            text = deviceCodeInfo.verificationUriComplete,
                        )
                    if (bitmap != null) {
                        Log.d(TAG, "QR code generated successfully")
                        _qrCodeBitmap.value = bitmap
                    } else {
                        Log.e(TAG, "QR code generation failed - bitmap is null")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating QR code", e)
                }
            }
        }

        private fun startPolling(deviceCodeInfo: DeviceCodeInfo) {
            viewModelScope.launch {
                when (
                    val result =
                        authRepository.pollForToken(
                            deviceCode = deviceCodeInfo.deviceCode,
                            interval = deviceCodeInfo.interval,
                        )
                ) {
                    is Result.Success -> {
                        // State will be updated through the repository flow
                    }
                    is Result.Error -> {
                        _authState.value = AuthState.Error("Authentication failed: ${result.exception.message}")
                    }
                    is Result.Loading -> {
                        // Polling in progress, state managed by repository
                    }
                }
            }
        }

        fun logout() {
            viewModelScope.launch {
                authRepository.logout()
                _qrCodeBitmap.value = null
            }
        }

        fun refreshToken() {
            viewModelScope.launch {
                when (val result = authRepository.refreshTokenIfNeeded()) {
                    is Result.Success -> {
                        // State will be updated through the repository flow
                    }
                    is Result.Error -> {
                        _authState.value = AuthState.Error("Token refresh failed: ${result.exception.message}")
                    }
                    is Result.Loading -> {
                        _authState.value = AuthState.Initializing
                    }
                }
            }
        }

        fun checkAuthenticationState() {
            viewModelScope.launch {
                authRepository.checkAuthState()
            }
        }

        fun startApiKeyAuthentication() {
            authRepository.startApiKeyAuthentication()
        }

        fun authenticateWithApiKey(apiKey: String) {
            Log.d(TAG, "authenticateWithApiKey() called")
            viewModelScope.launch {
                Log.d(TAG, "Calling repository.authenticateWithApiKey()")
                val result = authRepository.authenticateWithApiKey(apiKey)
                Log.d(TAG, "Repository authenticateWithApiKey() returned: $result")
                // Don't override the state here - let the repository's authState flow handle it
                // The repository will set the appropriate state (Authenticated, Error, etc.)
            }
        }
    }
