package com.rdwatch.androidtv.auth

import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.models.DeviceCodeInfo
import com.rdwatch.androidtv.repository.base.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central authentication manager that provides a unified interface for handling
 * OAuth authentication state and operations across the application.
 *
 * This class acts as a facade over the AuthRepository, providing centralized
 * state management and automatic token refresh capabilities.
 */
@Singleton
class AuthManager
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokenStorage: TokenStorage,
    ) {
        // Create a supervisor scope for background operations
        private val managerScope = CoroutineScope(SupervisorJob())

        // Current authentication state
        private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
        val authState: StateFlow<AuthState> = _authState.asStateFlow()

        // Indicates if the manager is currently refreshing tokens
        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

        init {
            // Initialize auth state on startup
            managerScope.launch {
                initializeAuthState()
            }

            // Observe auth repository state changes
            managerScope.launch {
                authRepository.authState.collect { repositoryState ->
                    _authState.value = repositoryState
                }
            }
        }

        /**
         * Initialize the authentication state by checking stored tokens
         */
        private suspend fun initializeAuthState() {
            try {
                _authState.value = AuthState.Initializing
                authRepository.checkAuthState()
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to initialize auth state: ${e.message}")
            }
        }

        /**
         * Start the OAuth device flow for authentication
         * @return Result containing device code information or error
         */
        suspend fun startAuthentication(): Result<DeviceCodeInfo> {
            return authRepository.startDeviceFlow()
        }

        /**
         * Poll for authentication token using device code
         * @param deviceCode The device code from the initial authentication request
         * @param interval Polling interval in seconds
         * @return Result indicating success or failure
         */
        suspend fun pollForAuthentication(
            deviceCode: String,
            interval: Int,
        ): Result<Unit> {
            return authRepository.pollForToken(deviceCode, interval)
        }

        /**
         * Check if the user is currently authenticated
         * @return true if authenticated, false otherwise
         */
        suspend fun isAuthenticated(): Boolean {
            return tokenStorage.isTokenValid()
        }

        /**
         * Get the current access token if available
         * @return access token or null if not available/expired
         */
        suspend fun getAccessToken(): String? {
            return if (tokenStorage.isTokenValid()) {
                tokenStorage.getAccessToken()
            } else {
                // Try to refresh token if we have a refresh token
                if (tokenStorage.hasRefreshToken()) {
                    val refreshResult = refreshTokensIfNeeded()
                    if (refreshResult is Result.Success) {
                        tokenStorage.getAccessToken()
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }

        /**
         * Refresh authentication tokens if needed
         * @return Result indicating success or failure
         */
        suspend fun refreshTokensIfNeeded(): Result<Unit> {
            if (_isRefreshing.value) {
                return Result.Error(Exception("Token refresh already in progress"))
            }

            return try {
                _isRefreshing.value = true
                authRepository.refreshTokenIfNeeded()
            } finally {
                _isRefreshing.value = false
            }
        }

        /**
         * Force refresh of authentication tokens
         * @return Result indicating success or failure
         */
        suspend fun forceRefreshTokens(): Result<Unit> {
            if (_isRefreshing.value) {
                return Result.Error(Exception("Token refresh already in progress"))
            }

            if (!tokenStorage.hasRefreshToken()) {
                return Result.Error(Exception("No refresh token available"))
            }

            return try {
                _isRefreshing.value = true
                authRepository.refreshTokenIfNeeded()
            } finally {
                _isRefreshing.value = false
            }
        }

        /**
         * Check if tokens need to be refreshed (approaching expiry)
         * @param bufferMinutes Buffer time in minutes before actual expiry
         * @return true if tokens should be refreshed
         */
        suspend fun shouldRefreshTokens(bufferMinutes: Int = 5): Boolean {
            return !tokenStorage.isTokenValid() && tokenStorage.hasRefreshToken()
        }

        /**
         * Logout the user and clear all authentication data
         */
        suspend fun logout() {
            authRepository.logout()
        }

        /**
         * Get the current authentication state as a flow for reactive UI updates
         * @return Flow of AuthState
         */
        fun getAuthStateFlow(): Flow<AuthState> = authState

        /**
         * Check if the authentication manager is currently performing any operations
         * @return true if busy, false otherwise
         */
        fun isBusy(): Boolean {
            return _isRefreshing.value || _authState.value == AuthState.Initializing
        }

        /**
         * Get detailed authentication status information
         * @return AuthStatus containing detailed information
         */
        suspend fun getAuthStatus(): AuthStatus {
            val hasAccessToken = tokenStorage.getAccessToken() != null
            val hasRefreshToken = tokenStorage.hasRefreshToken()
            val isTokenValid = tokenStorage.isTokenValid()

            return AuthStatus(
                isAuthenticated = isTokenValid,
                hasAccessToken = hasAccessToken,
                hasRefreshToken = hasRefreshToken,
                needsRefresh = !isTokenValid && hasRefreshToken,
                currentState = _authState.value,
            )
        }
    }

/**
 * Data class containing detailed authentication status information
 */
data class AuthStatus(
    val isAuthenticated: Boolean,
    val hasAccessToken: Boolean,
    val hasRefreshToken: Boolean,
    val needsRefresh: Boolean,
    val currentState: AuthState,
)
