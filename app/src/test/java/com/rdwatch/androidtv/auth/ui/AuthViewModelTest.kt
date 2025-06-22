package com.rdwatch.androidtv.auth.ui

import android.graphics.Bitmap
import com.rdwatch.androidtv.auth.AuthRepository
import com.rdwatch.androidtv.auth.QRCodeGenerator
import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.models.DeviceCodeInfo
import com.rdwatch.androidtv.repository.base.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthViewModelTest {
    
    private lateinit var authViewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var qrCodeGenerator: QRCodeGenerator
    private lateinit var authStateFlow: MutableStateFlow<AuthState>
    
    @Before
    fun setup() {
        authRepository = mockk()
        qrCodeGenerator = mockk()
        authStateFlow = MutableStateFlow(AuthState.Initializing)
        
        every { authRepository.authState } returns authStateFlow
        coEvery { authRepository.checkAuthState() } returns Unit
        
        authViewModel = AuthViewModel(authRepository, qrCodeGenerator)
    }
    
    @Test
    fun `init checks auth state on startup`() = runTest {
        // Then
        coVerify { authRepository.checkAuthState() }
    }
    
    @Test
    fun `authState reflects repository state`() = runTest {
        // Given
        val waitingState = AuthState.WaitingForUser(
            DeviceCodeInfo(
                deviceCode = "test_device_code",
                userCode = "TEST123",
                verificationUri = "https://real-debrid.com/device",
                expiresIn = 600,
                interval = 5
            )
        )
        
        // When
        authStateFlow.value = waitingState
        
        // Then
        val viewModelState = authViewModel.authState.first()
        assertEquals(waitingState, viewModelState)
    }
    
    @Test
    fun `startAuthentication calls repository startDeviceFlow`() = runTest {
        // Given
        val deviceCodeInfo = DeviceCodeInfo(
            deviceCode = "test_device_code",
            userCode = "TEST123",
            verificationUri = "https://real-debrid.com/device",
            expiresIn = 600,
            interval = 5
        )
        
        coEvery { authRepository.startDeviceFlow() } returns Result.Success(deviceCodeInfo)
        
        // When
        authViewModel.startAuthentication()
        
        // Then
        coVerify { authRepository.startDeviceFlow() }
    }
    
    @Test
    fun `startAuthentication handles repository error`() = runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { authRepository.startDeviceFlow() } returns Result.Error(exception)
        
        // When
        authViewModel.startAuthentication()
        
        // Then
        coVerify { authRepository.startDeviceFlow() }
        
        // Verify error state is set
        val authState = authViewModel.authState.first()
        assertTrue(authState is AuthState.Error)
        assertTrue((authState as AuthState.Error).message.contains("Failed to start authentication"))
    }
    
    @Test
    fun `generateQRCode is called when state becomes WaitingForUser`() = runTest {
        // Given
        val deviceCodeInfo = DeviceCodeInfo(
            deviceCode = "test_device_code",
            userCode = "TEST123",
            verificationUri = "https://real-debrid.com/device",
            expiresIn = 600,
            interval = 5
        )
        val mockBitmap = mockk<Bitmap>()
        
        every { qrCodeGenerator.generateTVOptimizedQRCode(any()) } returns mockBitmap
        coEvery { authRepository.pollForToken(any(), any()) } returns Result.Success(Unit)
        
        // When
        authStateFlow.value = AuthState.WaitingForUser(deviceCodeInfo)
        
        // Then
        verify { qrCodeGenerator.generateTVOptimizedQRCode(deviceCodeInfo.verificationUriComplete) }
        
        // Verify QR code bitmap is set
        val qrCodeBitmap = authViewModel.qrCodeBitmap.first()
        assertEquals(mockBitmap, qrCodeBitmap)
    }
    
    @Test
    fun `polling is started when state becomes WaitingForUser`() = runTest {
        // Given
        val deviceCodeInfo = DeviceCodeInfo(
            deviceCode = "test_device_code",
            userCode = "TEST123",
            verificationUri = "https://real-debrid.com/device",
            expiresIn = 600,
            interval = 5
        )
        val mockBitmap = mockk<Bitmap>()
        
        every { qrCodeGenerator.generateTVOptimizedQRCode(any()) } returns mockBitmap
        coEvery { authRepository.pollForToken(any(), any()) } returns Result.Success(Unit)
        
        // When
        authStateFlow.value = AuthState.WaitingForUser(deviceCodeInfo)
        
        // Then
        coVerify { authRepository.pollForToken(deviceCodeInfo.deviceCode, deviceCodeInfo.interval) }
    }
    
    @Test
    fun `polling failure sets error state`() = runTest {
        // Given
        val deviceCodeInfo = DeviceCodeInfo(
            deviceCode = "test_device_code",
            userCode = "TEST123",
            verificationUri = "https://real-debrid.com/device",
            expiresIn = 600,
            interval = 5
        )
        val mockBitmap = mockk<Bitmap>()
        val exception = Exception("Polling failed")
        
        every { qrCodeGenerator.generateTVOptimizedQRCode(any()) } returns mockBitmap
        coEvery { authRepository.pollForToken(any(), any()) } returns Result.Error(exception)
        
        // When
        authStateFlow.value = AuthState.WaitingForUser(deviceCodeInfo)
        
        // Allow some time for the coroutine to complete
        kotlinx.coroutines.delay(100)
        
        // Then
        val authState = authViewModel.authState.first()
        assertTrue(authState is AuthState.Error)
        assertTrue((authState as AuthState.Error).message.contains("Authentication failed"))
    }
    
    @Test
    fun `logout calls repository logout and clears QR code`() = runTest {
        // Given
        coEvery { authRepository.logout() } returns Unit
        
        // When
        authViewModel.logout()
        
        // Then
        coVerify { authRepository.logout() }
        
        // Verify QR code bitmap is cleared
        val qrCodeBitmap = authViewModel.qrCodeBitmap.first()
        assertNull(qrCodeBitmap)
    }
    
    @Test
    fun `refreshToken calls repository refreshTokenIfNeeded`() = runTest {
        // Given
        coEvery { authRepository.refreshTokenIfNeeded() } returns Result.Success(Unit)
        
        // When
        authViewModel.refreshToken()
        
        // Then
        coVerify { authRepository.refreshTokenIfNeeded() }
    }
    
    @Test
    fun `refreshToken handles repository error`() = runTest {
        // Given
        val exception = Exception("Refresh failed")
        coEvery { authRepository.refreshTokenIfNeeded() } returns Result.Error(exception)
        
        // When
        authViewModel.refreshToken()
        
        // Then
        coVerify { authRepository.refreshTokenIfNeeded() }
        
        // Verify error state is set
        val authState = authViewModel.authState.first()
        assertTrue(authState is AuthState.Error)
        assertTrue((authState as AuthState.Error).message.contains("Token refresh failed"))
    }
    
    @Test
    fun `QR code generation failure is handled gracefully`() = runTest {
        // Given
        val deviceCodeInfo = DeviceCodeInfo(
            deviceCode = "test_device_code",
            userCode = "TEST123",
            verificationUri = "https://real-debrid.com/device",
            expiresIn = 600,
            interval = 5
        )
        
        every { qrCodeGenerator.generateTVOptimizedQRCode(any()) } returns null
        coEvery { authRepository.pollForToken(any(), any()) } returns Result.Success(Unit)
        
        // When
        authStateFlow.value = AuthState.WaitingForUser(deviceCodeInfo)
        
        // Then
        verify { qrCodeGenerator.generateTVOptimizedQRCode(any()) }
        
        // Verify QR code bitmap is null but app doesn't crash
        val qrCodeBitmap = authViewModel.qrCodeBitmap.first()
        assertNull(qrCodeBitmap)
    }
}