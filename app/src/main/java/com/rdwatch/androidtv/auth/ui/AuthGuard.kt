package com.rdwatch.androidtv.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.ui.AuthViewModel

/**
 * AuthGuard component that ensures the user is authenticated before showing protected content.
 * If the user is not authenticated, it will automatically redirect to the authentication screen.
 * Includes edge case handling for token refresh and network issues.
 */
@Composable
fun AuthGuard(
    onAuthenticationRequired: () -> Unit,
    content: @Composable () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    showLoadingOnInitializing: Boolean = false
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    var hasTriggeredRedirect by remember { mutableStateOf(false) }
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Initializing -> {
                // Reset redirect flag when initializing
                hasTriggeredRedirect = false
                // Check auth state - this is handled automatically in AuthViewModel init
            }
            is AuthState.Unauthenticated,
            is AuthState.WaitingForUser,
            is AuthState.ApiKeyEntry,
            is AuthState.Error -> {
                // Only trigger redirect once to prevent infinite loops
                if (!hasTriggeredRedirect) {
                    hasTriggeredRedirect = true
                    onAuthenticationRequired()
                }
            }
            is AuthState.Authenticated -> {
                // Reset redirect flag when authenticated
                hasTriggeredRedirect = false
            }
        }
    }
    
    // Only show content if authenticated
    when (authState) {
        is AuthState.Authenticated -> {
            content()
        }
        is AuthState.Initializing -> {
            if (showLoadingOnInitializing) {
                // Optional loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Otherwise show nothing while checking auth
        }
        else -> {
            // Show nothing while redirecting to authentication
        }
    }
}