package com.rdwatch.androidtv.network.interceptors

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenProvider: TokenProvider
    private lateinit var tokenAuthenticator: TokenAuthenticator
    private lateinit var okHttpClient: OkHttpClient
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        tokenProvider = mockk()
        tokenAuthenticator = TokenAuthenticator(tokenProvider)
        
        okHttpClient = OkHttpClient.Builder()
            .authenticator(tokenAuthenticator)
            .build()
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `refreshes token on 401 response`() = runBlocking {
        // Given
        val oldToken = "old_token"
        val newToken = "new_token"
        
        every { tokenProvider.getAccessToken() } returns oldToken andThen newToken
        coEvery { tokenProvider.refreshToken() } returns true
        
        // First response is 401, second should succeed
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .header("Authorization", "Bearer $oldToken")
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        // Then
        assertEquals(200, response.code)
        
        val firstRequest = mockWebServer.takeRequest()
        assertEquals("Bearer $oldToken", firstRequest.getHeader("Authorization"))
        
        val secondRequest = mockWebServer.takeRequest()
        assertEquals("Bearer $newToken", secondRequest.getHeader("Authorization"))
        
        verify { tokenProvider.refreshToken() }
    }
    
    @Test
    fun `clears tokens when refresh fails`() = runBlocking {
        // Given
        every { tokenProvider.getAccessToken() } returns "old_token"
        coEvery { tokenProvider.refreshToken() } returns false
        every { tokenProvider.clearTokens() } returns Unit
        
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .header("Authorization", "Bearer old_token")
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        // Then
        assertEquals(401, response.code)
        verify { tokenProvider.refreshToken() }
        verify { tokenProvider.clearTokens() }
    }
    
    @Test
    fun `stops retrying after max attempts`() = runBlocking {
        // Given
        every { tokenProvider.getAccessToken() } returns "new_token"
        coEvery { tokenProvider.refreshToken() } returns true
        
        // Enqueue multiple 401 responses
        repeat(5) {
            mockWebServer.enqueue(MockResponse().setResponseCode(401))
        }
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        // Then
        assertEquals(401, response.code)
        
        // Should only make 4 requests (original + 3 retries)
        assertEquals(4, mockWebServer.requestCount)
    }
}