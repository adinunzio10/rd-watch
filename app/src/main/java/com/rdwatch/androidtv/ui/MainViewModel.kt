package com.rdwatch.androidtv.ui

import android.util.Log
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
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val _isInitialized = MutableStateFlow(false)
    private val _startDestination = MutableStateFlow<Screen>(Screen.Home)
    private val _initializationError = MutableStateFlow<String?>(null)
    
    val startDestination: StateFlow<Screen> = _startDestination.asStateFlow()
    val initializationError: StateFlow<String?> = _initializationError.asStateFlow()
    
    val isReady = combine(
        _isInitialized,
        authRepository.authState
    ) { initialized, authState ->
        Log.d(TAG, "isReady combine: initialized=$initialized, authState=$authState")
        
        when (authState) {
            is AuthState.Initializing -> {
                Log.d(TAG, "AuthState is Initializing, waiting...")
                false
            }
            is AuthState.Unauthenticated -> {
                Log.d(TAG, "AuthState is Unauthenticated, navigating to Authentication")
                _startDestination.value = Screen.Authentication
                _initializationError.value = null
                // Mark as initialized since we have a stable auth state
                if (!initialized) {
                    Log.d(TAG, "Setting initialized=true due to stable auth state")
                    _isInitialized.value = true
                }
                true
            }
            is AuthState.WaitingForUser -> {
                Log.d(TAG, "AuthState is WaitingForUser, navigating to Authentication")
                _startDestination.value = Screen.Authentication
                _initializationError.value = null
                // Mark as initialized since we have a stable auth state
                if (!initialized) {
                    Log.d(TAG, "Setting initialized=true due to stable auth state")
                    _isInitialized.value = true
                }
                true
            }
            is AuthState.ApiKeyEntry -> {
                Log.d(TAG, "AuthState is ApiKeyEntry, navigating to Authentication")
                _startDestination.value = Screen.Authentication
                _initializationError.value = null
                // Mark as initialized since we have a stable auth state
                if (!initialized) {
                    Log.d(TAG, "Setting initialized=true due to stable auth state")
                    _isInitialized.value = true
                }
                true
            }
            is AuthState.Authenticated -> {
                Log.d(TAG, "AuthState is Authenticated, navigating to Home")
                _startDestination.value = Screen.Home
                _initializationError.value = null
                // Mark as initialized since we have a stable auth state
                if (!initialized) {
                    Log.d(TAG, "Setting initialized=true due to stable auth state")
                    _isInitialized.value = true
                }
                true
            }
            is AuthState.Error -> {
                Log.d(TAG, "AuthState is Error: ${authState.message}, navigating to Authentication")
                _startDestination.value = Screen.Authentication
                _initializationError.value = null
                // Mark as initialized since we have a stable auth state
                if (!initialized) {
                    Log.d(TAG, "Setting initialized=true due to stable auth state")
                    _isInitialized.value = true
                }
                true
            }
        }
    }
    
    init {
        Log.d(TAG, "MainViewModel init starting")
        initializeApp()
    }
    
    private fun initializeApp() {
        Log.d(TAG, "initializeApp() starting")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting app initialization in coroutine")
                // Simple timeout approach - don't wait forever
                val initTimeoutMs = 5_000L // 5 seconds max
                val startTime = System.currentTimeMillis()
                
                // Try to check authentication state, but with error handling
                try {
                    Log.d(TAG, "Calling authRepository.checkAuthState()")
                    authRepository.checkAuthState()
                    Log.d(TAG, "authRepository.checkAuthState() completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during authRepository.checkAuthState(): ${e.message}", e)
                    // If auth check fails, assume unauthenticated and continue
                    _startDestination.value = Screen.Authentication
                    _initializationError.value = null
                    _isInitialized.value = true
                    Log.d(TAG, "Set to authentication screen due to auth check error")
                    return@launch
                }
                
                // Wait briefly for auth state to stabilize
                var attempts = 0
                val maxAttempts = 10 // 5 seconds with 500ms intervals
                
                Log.d(TAG, "Starting auth state stabilization loop")
                while (attempts < maxAttempts) {
                    delay(500)
                    attempts++
                    
                    // Check if we have a stable auth state
                    val currentAuthState = authRepository.getCurrentAuthState()
                    Log.d(TAG, "Attempt $attempts/$maxAttempts: Current auth state = $currentAuthState")
                    
                    if (currentAuthState !is AuthState.Initializing) {
                        Log.d(TAG, "Auth state is stable, breaking from loop")
                        break
                    }
                    
                    // Timeout protection
                    if (System.currentTimeMillis() - startTime > initTimeoutMs) {
                        Log.w(TAG, "Initialization timeout reached, defaulting to authentication screen")
                        _startDestination.value = Screen.Authentication
                        _initializationError.value = null
                        break
                    }
                }
                
                Log.d(TAG, "Auth state stabilization loop completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during initialization: ${e.message}", e)
                // Any error during initialization - default to authentication screen
                _startDestination.value = Screen.Authentication
                _initializationError.value = null
            } finally {
                Log.d(TAG, "Setting _isInitialized to true")
                _isInitialized.value = true
                Log.d(TAG, "initializeApp() completed")
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