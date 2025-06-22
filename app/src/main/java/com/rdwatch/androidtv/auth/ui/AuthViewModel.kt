package com.rdwatch.androidtv.auth.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.auth.AuthRepository
import com.rdwatch.androidtv.auth.QRCodeGenerator
import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.models.DeviceCodeInfo
import com.rdwatch.androidtv.repository.base.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val qrCodeGenerator: QRCodeGenerator
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrCodeBitmap: StateFlow<Bitmap?> = _qrCodeBitmap.asStateFlow()
    
    init {
        // Observe auth state from repository
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                _authState.value = state
                
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
            when (val result = authRepository.startDeviceFlow()) {
                is Result.Success -> {
                    // State will be updated through the repository flow
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error("Failed to start authentication: ${result.exception.message}")
                }
            }
        }
    }
    
    private fun generateQRCode(deviceCodeInfo: DeviceCodeInfo) {
        viewModelScope.launch {
            val bitmap = qrCodeGenerator.generateTVOptimizedQRCode(
                text = deviceCodeInfo.verificationUriComplete
            )
            _qrCodeBitmap.value = bitmap
        }
    }
    
    private fun startPolling(deviceCodeInfo: DeviceCodeInfo) {
        viewModelScope.launch {
            when (val result = authRepository.pollForToken(
                deviceCode = deviceCodeInfo.deviceCode,
                interval = deviceCodeInfo.interval
            )) {
                is Result.Success -> {
                    // State will be updated through the repository flow
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error("Authentication failed: ${result.exception.message}")
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
            }
        }
    }
}