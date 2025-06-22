package com.rdwatch.androidtv.network.interceptors

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenProvider: TokenProvider
    private lateinit var authInterceptor: AuthInterceptor
    private lateinit var okHttpClient: OkHttpClient
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        tokenProvider = mockk()
        authInterceptor = AuthInterceptor(tokenProvider)
        
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `adds authorization header when token is available`() {
        // Given
        val testToken = "test_access_token_123"
        every { tokenProvider.getAccessToken() } returns testToken
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()
        
        okHttpClient.newCall(request).execute()
        
        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer $testToken", recordedRequest.getHeader("Authorization"))
        verify { tokenProvider.getAccessToken() }
    }
    
    @Test
    fun `does not add authorization header when token is null`() {
        // Given
        every { tokenProvider.getAccessToken() } returns null
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()
        
        okHttpClient.newCall(request).execute()
        
        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
        verify { tokenProvider.getAccessToken() }
    }
    
    @Test
    fun `does not add authorization header when token is blank`() {
        // Given
        every { tokenProvider.getAccessToken() } returns ""
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        
        // When
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()
        
        okHttpClient.newCall(request).execute()
        
        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
        verify { tokenProvider.getAccessToken() }
    }
}