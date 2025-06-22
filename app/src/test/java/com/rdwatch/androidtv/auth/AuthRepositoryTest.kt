package com.rdwatch.androidtv.auth

import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.models.DeviceCodeInfo
import com.rdwatch.androidtv.network.api.OAuth2ApiService
import com.rdwatch.androidtv.network.models.OAuth2DeviceCodeResponse
import com.rdwatch.androidtv.network.models.OAuth2TokenResponse
import com.rdwatch.androidtv.repository.base.Result
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRepositoryTest {
    
    private lateinit var authRepository: AuthRepository
    private lateinit var oauth2ApiService: OAuth2ApiService
    private lateinit var tokenStorage: TokenStorage
    private lateinit var moshi: Moshi
    
    @Before
    fun setup() {
        oauth2ApiService = mockk()
        tokenStorage = mockk(relaxed = true)
        moshi = Moshi.Builder().build()
        
        authRepository = AuthRepository(oauth2ApiService, tokenStorage, moshi)
    }
    
    @Test
    fun `startDeviceFlow returns success when API call succeeds`() = runTest {
        // Given
        val deviceCodeResponse = OAuth2DeviceCodeResponse(
            deviceCode = "test_device_code",
            userCode = "TEST123",
            verificationUri = "https://real-debrid.com/device",
            expiresIn = 600,
            interval = 5
        )
        
        coEvery { oauth2ApiService.getDeviceCode(any(), any()) } returns 
            Response.success(deviceCodeResponse)
        
        // When
        val result = authRepository.startDeviceFlow()
        
        // Then
        assertTrue(result is Result.Success)
        val deviceCodeInfo = result.data
        assertEquals("test_device_code", deviceCodeInfo.deviceCode)
        assertEquals("TEST123", deviceCodeInfo.userCode)
        assertEquals("https://real-debrid.com/device", deviceCodeInfo.verificationUri)
        assertEquals(600, deviceCodeInfo.expiresIn)
        assertEquals(5, deviceCodeInfo.interval)
        
        // Verify auth state is updated
        val authState = authRepository.authState.first()
        assertTrue(authState is AuthState.WaitingForUser)
        assertEquals(deviceCodeInfo, (authState as AuthState.WaitingForUser).deviceCodeInfo)
    }
    
    @Test
    fun `startDeviceFlow returns error when API call fails`() = runTest {
        // Given
        val errorResponse = """{"error": "invalid_client", "error_description": "Invalid client"}"""
        
        coEvery { oauth2ApiService.getDeviceCode(any(), any()) } returns 
            Response.error(400, errorResponse.toResponseBody())
        
        // When
        val result = authRepository.startDeviceFlow()
        
        // Then
        assertTrue(result is Result.Error)
        
        // Verify auth state shows error
        val authState = authRepository.authState.first()
        assertTrue(authState is AuthState.Error)
    }
    
    @Test
    fun `pollForToken returns success when token is obtained`() = runTest {
        // Given
        val tokenResponse = OAuth2TokenResponse(
            accessToken = "test_access_token",
            tokenType = "Bearer",
            expiresIn = 3600,
            refreshToken = "test_refresh_token",
            scope = null
        )
        
        coEvery { oauth2ApiService.getDeviceToken(any(), any()) } returns 
            Response.success(tokenResponse)
        
        coEvery { tokenStorage.saveTokens(any(), any(), any()) } returns Unit
        
        // When
        val result = authRepository.pollForToken("test_device_code", 5)
        
        // Then
        assertTrue(result is Result.Success)
        
        // Verify tokens are saved
        coVerify { tokenStorage.saveTokens("test_access_token", "test_refresh_token", 3600) }
        
        // Verify auth state is updated
        val authState = authRepository.authState.first()
        assertTrue(authState is AuthState.Authenticated)
    }
    
    @Test
    fun `pollForToken handles authorization_pending error`() = runTest {
        // Given
        val pendingResponse = """{"error": "authorization_pending", "error_description": "User has not authorized yet"}"""
        val tokenResponse = OAuth2TokenResponse(
            accessToken = "test_access_token",
            tokenType = "Bearer",
            expiresIn = 3600,
            refreshToken = "test_refresh_token",
            scope = null
        )
        
        coEvery { oauth2ApiService.getDeviceToken(any(), any()) } returnsMany listOf(
            Response.error(400, pendingResponse.toResponseBody()),
            Response.success(tokenResponse)
        )
        
        coEvery { tokenStorage.saveTokens(any(), any(), any()) } returns Unit
        
        // When
        val result = authRepository.pollForToken("test_device_code", 1) // Short interval for test
        
        // Then
        assertTrue(result is Result.Success)
        
        // Verify multiple API calls were made
        coVerify(atLeast = 2) { oauth2ApiService.getDeviceToken(any(), any()) }
    }
    
    @Test
    fun `refreshTokenIfNeeded returns success when token refresh succeeds`() = runTest {
        // Given
        val refreshToken = "test_refresh_token"
        val newTokenResponse = OAuth2TokenResponse(
            accessToken = "new_access_token",
            tokenType = "Bearer",
            expiresIn = 3600,
            refreshToken = "new_refresh_token",
            scope = null
        )
        
        coEvery { tokenStorage.getRefreshToken() } returns refreshToken
        coEvery { oauth2ApiService.refreshToken(any(), any()) } returns 
            Response.success(newTokenResponse)
        coEvery { tokenStorage.saveTokens(any(), any(), any()) } returns Unit
        
        // When
        val result = authRepository.refreshTokenIfNeeded()
        
        // Then
        assertTrue(result is Result.Success)
        
        // Verify new tokens are saved
        coVerify { tokenStorage.saveTokens("new_access_token", "new_refresh_token", 3600) }
        
        // Verify auth state is updated
        val authState = authRepository.authState.first()
        assertTrue(authState is AuthState.Authenticated)
    }
    
    @Test
    fun `refreshTokenIfNeeded returns error when no refresh token available`() = runTest {
        // Given
        coEvery { tokenStorage.getRefreshToken() } returns null
        
        // When
        val result = authRepository.refreshTokenIfNeeded()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals("No refresh token available", result.exception.message)
        
        // Verify auth state shows error
        val authState = authRepository.authState.first()
        assertTrue(authState is AuthState.Error)
    }
    
    @Test
    fun `logout clears tokens and resets auth state`() = runTest {
        // Given
        coEvery { tokenStorage.clearTokens() } returns Unit
        
        // When
        authRepository.logout()
        
        // Then
        coVerify { tokenStorage.clearTokens() }
        
        // Verify auth state is reset
        val authState = authRepository.authState.first()
        assertTrue(authState is AuthState.Initializing)
    }
    
    @Test
    fun `checkAuthState sets authenticated when token is valid`() = runTest {
        // Given
        coEvery { tokenStorage.isTokenValid() } returns true
        
        // When
        authRepository.checkAuthState()
        
        // Then
        val authState = authRepository.authState.first()
        assertTrue(authState is AuthState.Authenticated)
    }
    
    @Test
    fun `checkAuthState attempts refresh when token invalid but refresh token available`() = runTest {
        // Given
        val refreshToken = "test_refresh_token"
        val newTokenResponse = OAuth2TokenResponse(
            accessToken = "new_access_token",
            tokenType = "Bearer",
            expiresIn = 3600,
            refreshToken = "new_refresh_token",
            scope = null
        )
        
        coEvery { tokenStorage.isTokenValid() } returns false
        coEvery { tokenStorage.hasRefreshToken() } returns true
        coEvery { tokenStorage.getRefreshToken() } returns refreshToken
        coEvery { oauth2ApiService.refreshToken(any(), any()) } returns 
            Response.success(newTokenResponse)
        coEvery { tokenStorage.saveTokens(any(), any(), any()) } returns Unit
        
        // When
        authRepository.checkAuthState()
        
        // Then
        coVerify { oauth2ApiService.refreshToken(any(), refreshToken) }
        
        val authState = authRepository.authState.first()
        assertTrue(authState is AuthState.Authenticated)
    }
}