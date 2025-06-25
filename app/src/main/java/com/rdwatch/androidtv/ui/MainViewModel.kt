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
                // Simple timeout approach - don't wait forever
                val initTimeoutMs = 5_000L // 5 seconds max
                val startTime = System.currentTimeMillis()
                
                // Try to check authentication state, but with error handling
                try {
                    authRepository.checkAuthState()
                } catch (e: Exception) {
                    // If auth check fails, assume unauthenticated and continue
                    _startDestination.value = Screen.Authentication
                    _initializationError.value = null
                    _isInitialized.value = true
                    return@launch
                }
                
                // Wait briefly for auth state to stabilize
                var attempts = 0
                val maxAttempts = 10 // 5 seconds with 500ms intervals
                
                while (attempts < maxAttempts) {
                    delay(500)
                    attempts++
                    
                    // Check if we have a stable auth state
                    val currentAuthState = authRepository.getCurrentAuthState()
                    if (currentAuthState !is AuthState.Initializing) {
                        // We have a stable state, proceed
                        break
                    }
                    
                    // Timeout protection
                    if (System.currentTimeMillis() - startTime > initTimeoutMs) {
                        _startDestination.value = Screen.Authentication
                        _initializationError.value = null
                        break
                    }
                }
                
            } catch (e: Exception) {
                // Any error during initialization - default to authentication screen
                _startDestination.value = Screen.Authentication
                _initializationError.value = null
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