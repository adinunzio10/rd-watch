package com.rdwatch.androidtv.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenStorageImplTest {
    
    private lateinit var tokenStorage: TokenStorageImpl
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    
    @Before
    fun setup() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)
        
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit
        
        // Mock EncryptedSharedPreferences.create
        mockkStatic(EncryptedSharedPreferences::class)
        every { 
            EncryptedSharedPreferences.create(any(), any(), any(), any(), any()) 
        } returns sharedPreferences
        
        tokenStorage = TokenStorageImpl(context)
    }
    
    @Test
    fun `saveTokens stores tokens with expiry time`() = runTest {
        // Given
        val accessToken = "test_access_token"
        val refreshToken = "test_refresh_token"
        val expiresIn = 3600
        val currentTime = System.currentTimeMillis()
        val expiryTimeSlot = slot<Long>()
        
        every { editor.putLong("token_expiry", capture(expiryTimeSlot)) } returns editor
        
        // When
        tokenStorage.saveTokens(accessToken, refreshToken, expiresIn)
        
        // Then
        verify { editor.putString("access_token", accessToken) }
        verify { editor.putString("refresh_token", refreshToken) }
        verify { editor.putLong("token_expiry", any()) }
        verify { editor.apply() }
        
        // Verify expiry time is approximately correct (within 1 second)
        val expectedExpiryTime = currentTime + (expiresIn * 1000L)
        val actualExpiryTime = expiryTimeSlot.captured
        assertTrue(kotlin.math.abs(actualExpiryTime - expectedExpiryTime) < 1000L)
    }
    
    @Test
    fun `getAccessToken returns stored token`() = runTest {
        // Given
        val expectedToken = "test_access_token"
        every { sharedPreferences.getString("access_token", null) } returns expectedToken
        
        // When
        val result = tokenStorage.getAccessToken()
        
        // Then
        assertEquals(expectedToken, result)
    }
    
    @Test
    fun `getAccessToken returns null when no token stored`() = runTest {
        // Given
        every { sharedPreferences.getString("access_token", null) } returns null
        
        // When
        val result = tokenStorage.getAccessToken()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `getRefreshToken returns stored refresh token`() = runTest {
        // Given
        val expectedToken = "test_refresh_token"
        every { sharedPreferences.getString("refresh_token", null) } returns expectedToken
        
        // When
        val result = tokenStorage.getRefreshToken()
        
        // Then
        assertEquals(expectedToken, result)
    }
    
    @Test
    fun `clearTokens removes all stored tokens`() = runTest {
        // When
        tokenStorage.clearTokens()
        
        // Then
        verify { editor.remove("access_token") }
        verify { editor.remove("refresh_token") }
        verify { editor.remove("token_expiry") }
        verify { editor.apply() }
    }
    
    @Test
    fun `isTokenValid returns true when token exists and not expired`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val futureExpiryTime = currentTime + 120_000L // 2 minutes in future
        
        every { sharedPreferences.getString("access_token", null) } returns "test_token"
        every { sharedPreferences.getLong("token_expiry", 0L) } returns futureExpiryTime
        
        // When
        val result = tokenStorage.isTokenValid()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isTokenValid returns false when token is null`() = runTest {
        // Given
        every { sharedPreferences.getString("access_token", null) } returns null
        every { sharedPreferences.getLong("token_expiry", 0L) } returns System.currentTimeMillis() + 120_000L
        
        // When
        val result = tokenStorage.isTokenValid()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `isTokenValid returns false when token is expired`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val pastExpiryTime = currentTime - 60_000L // 1 minute ago
        
        every { sharedPreferences.getString("access_token", null) } returns "test_token"
        every { sharedPreferences.getLong("token_expiry", 0L) } returns pastExpiryTime
        
        // When
        val result = tokenStorage.isTokenValid()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `isTokenValid returns false when token expires soon (within 1 minute buffer)`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val soonExpiryTime = currentTime + 30_000L // 30 seconds in future (within 1 minute buffer)
        
        every { sharedPreferences.getString("access_token", null) } returns "test_token"
        every { sharedPreferences.getLong("token_expiry", 0L) } returns soonExpiryTime
        
        // When
        val result = tokenStorage.isTokenValid()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `hasRefreshToken returns true when refresh token exists`() = runTest {
        // Given
        every { sharedPreferences.getString("refresh_token", null) } returns "test_refresh_token"
        
        // When
        val result = tokenStorage.hasRefreshToken()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `hasRefreshToken returns false when refresh token is null`() = runTest {
        // Given
        every { sharedPreferences.getString("refresh_token", null) } returns null
        
        // When
        val result = tokenStorage.hasRefreshToken()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `hasRefreshToken returns false when refresh token is empty`() = runTest {
        // Given
        every { sharedPreferences.getString("refresh_token", null) } returns ""
        
        // When
        val result = tokenStorage.hasRefreshToken()
        
        // Then
        assertFalse(result)
    }
}