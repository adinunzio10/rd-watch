package com.rdwatch.androidtv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.auth.AuthRepository
import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _isInitialized = MutableStateFlow(false)
    private val _startDestination = MutableStateFlow<Screen>(Screen.Home)
    private val _initializationError = MutableStateFlow<String?>(null)
    
    val startDestination: StateFlow<Screen> = _startDestination.asStateFlow()
    val initializationError: StateFlow<String?> = _initializationError.asStateFlow()
    
    val isReady = combine(
        _isInitialized,
        authRepository.authState
    ) { initialized, authState ->
        if (!initialized) return@combine false
        
        when (authState) {
            is AuthState.Initializing -> {
                // Give some time for auth check, but don't wait indefinitely
                false
            }
            is AuthState.WaitingForUser -> {
                _startDestination.value = Screen.Authentication
                _initializationError.value = null
                true
            }
            is AuthState.Authenticated -> {
                _startDestination.value = Screen.Home
                _initializationError.value = null
                true
            }
            is AuthState.Error -> {
                _startDestination.value = Screen.Authentication
                _initializationError.value = null
                true
            }
        }
    }
    
    init {
        initializeApp()
    }
    
    private fun initializeApp() {
        viewModelScope.launch {
            try {
                // Add a timeout for initialization to prevent infinite loading
                val initTimeoutMs = 10_000L // 10 seconds
                val startTime = System.currentTimeMillis()
                
                // Check current authentication state
                authRepository.checkAuthState()
                
                // Wait for auth state to stabilize, but with timeout
                var attempts = 0
                val maxAttempts = 20 // 10 seconds with 500ms intervals
                
                while (!_isInitialized.value && attempts < maxAttempts) {
                    delay(500)
                    attempts++
                    
                    // If we've been waiting too long, assume unauthenticated
                    if (System.currentTimeMillis() - startTime > initTimeoutMs) {
                        _startDestination.value = Screen.Authentication
                        _initializationError.value = "Authentication check timed out"
                        break
                    }
                }
                
            } catch (e: Exception) {
                // If there's an error checking auth state, start with authentication screen
                _startDestination.value = Screen.Authentication
                _initializationError.value = "Failed to check authentication: ${e.message}"
            } finally {
                _isInitialized.value = true
            }
        }
    }
    
    fun onAuthenticationSuccess() {
        _startDestination.value = Screen.Home
        _initializationError.value = null
    }
    
    fun retryInitialization() {
        _isInitialized.value = false
        _initializationError.value = null
        initializeApp()
    }
    
    /**
     * Handle app resume - recheck auth state in case tokens expired
     */
    fun onAppResume() {
        viewModelScope.launch {
            try {
                authRepository.checkAuthState()
            } catch (e: Exception) {
                // If auth check fails on resume, don't force re-authentication immediately
                // Let the user continue and handle it gracefully through AuthGuards
            }
        }
    }
    
    /**
     * Handle network connectivity restored
     */
    fun onNetworkRestored() {
        if (_initializationError.value != null) {
            retryInitialization()
        }
    }
}