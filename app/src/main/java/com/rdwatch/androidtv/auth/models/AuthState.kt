package com.rdwatch.androidtv.auth.models

sealed class AuthState {
    data object Initializing : AuthState()
    data class WaitingForUser(val deviceCodeInfo: DeviceCodeInfo) : AuthState()
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}