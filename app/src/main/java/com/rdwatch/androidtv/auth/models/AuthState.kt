package com.rdwatch.androidtv.auth.models

sealed class AuthState {
    data object Initializing : AuthState()
    data object Unauthenticated : AuthState() // No tokens, needs authentication
    data class WaitingForUser(val deviceCodeInfo: DeviceCodeInfo) : AuthState()
    data object ApiKeyEntry : AuthState() // User entering API key manually
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}