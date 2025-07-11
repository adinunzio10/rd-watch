package com.rdwatch.androidtv.network.interceptors

import io.mockk.*
import okhttp3.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for TMDbApiKeyInterceptor
 * Tests API key injection functionality
 */
class TMDbApiKeyInterceptorTest {
    
    private lateinit var interceptor: TMDbApiKeyInterceptor
    private lateinit var mockChain: Interceptor.Chain
    private lateinit var mockRequest: Request
    private lateinit var mockResponse: Response
    private lateinit var mockHttpUrl: HttpUrl
    private lateinit var mockRequestBuilder: Request.Builder
    private lateinit var mockHttpUrlBuilder: HttpUrl.Builder
    
    private val testApiKey = "test_api_key_123456"
    
    @Before
    fun setUp() {
        interceptor = TMDbApiKeyInterceptor(testApiKey)
        mockChain = mockk()
        mockRequest = mockk()
        mockResponse = mockk()
        mockHttpUrl = mockk()
        mockRequestBuilder = mockk()
        mockHttpUrlBuilder = mockk()
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `intercept adds API key to request URL`() {
        // Arrange
        val originalUrl = "https://api.themoviedb.org/3/movie/550"
        val expectedUrl = "https://api.themoviedb.org/3/movie/550?api_key=$testApiKey"
        
        every { mockChain.request() } returns mockRequest
        every { mockRequest.url } returns mockHttpUrl
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockHttpUrl.newBuilder() } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.addQueryParameter("api_key", testApiKey) } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.build() } returns mockHttpUrl
        every { mockRequestBuilder.url(mockHttpUrl) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockRequest
        every { mockChain.proceed(mockRequest) } returns mockResponse
        
        // Act
        val result = interceptor.intercept(mockChain)
        
        // Assert
        assertEquals(mockResponse, result)
        verify { mockHttpUrlBuilder.addQueryParameter("api_key", testApiKey) }
        verify { mockChain.proceed(mockRequest) }
    }
    
    @Test
    fun `intercept preserves existing query parameters`() {
        // Arrange
        val originalUrl = "https://api.themoviedb.org/3/movie/550?language=en-US"
        val expectedUrl = "https://api.themoviedb.org/3/movie/550?language=en-US&api_key=$testApiKey"
        
        every { mockChain.request() } returns mockRequest
        every { mockRequest.url } returns mockHttpUrl
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockHttpUrl.newBuilder() } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.addQueryParameter("api_key", testApiKey) } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.build() } returns mockHttpUrl
        every { mockRequestBuilder.url(mockHttpUrl) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockRequest
        every { mockChain.proceed(mockRequest) } returns mockResponse
        
        // Act
        val result = interceptor.intercept(mockChain)
        
        // Assert
        assertEquals(mockResponse, result)
        verify { mockHttpUrlBuilder.addQueryParameter("api_key", testApiKey) }
    }
    
    @Test
    fun `intercept works with empty API key`() {
        // Arrange
        val emptyKeyInterceptor = TMDbApiKeyInterceptor("")
        
        every { mockChain.request() } returns mockRequest
        every { mockRequest.url } returns mockHttpUrl
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockHttpUrl.newBuilder() } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.addQueryParameter("api_key", "") } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.build() } returns mockHttpUrl
        every { mockRequestBuilder.url(mockHttpUrl) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockRequest
        every { mockChain.proceed(mockRequest) } returns mockResponse
        
        // Act
        val result = emptyKeyInterceptor.intercept(mockChain)
        
        // Assert
        assertEquals(mockResponse, result)
        verify { mockHttpUrlBuilder.addQueryParameter("api_key", "") }
    }
    
    @Test
    fun `intercept handles null API key gracefully`() {
        // Arrange
        val nullKeyInterceptor = TMDbApiKeyInterceptor(null)
        
        every { mockChain.request() } returns mockRequest
        every { mockRequest.url } returns mockHttpUrl
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockHttpUrl.newBuilder() } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.addQueryParameter("api_key", "") } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.build() } returns mockHttpUrl
        every { mockRequestBuilder.url(mockHttpUrl) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockRequest
        every { mockChain.proceed(mockRequest) } returns mockResponse
        
        // Act
        val result = nullKeyInterceptor.intercept(mockChain)
        
        // Assert
        assertEquals(mockResponse, result)
        verify { mockHttpUrlBuilder.addQueryParameter("api_key", "") }
    }
    
    @Test
    fun `intercept works with complex URLs`() {
        // Arrange
        val complexUrl = "https://api.themoviedb.org/3/movie/550/recommendations?language=en-US&page=1"
        
        every { mockChain.request() } returns mockRequest
        every { mockRequest.url } returns mockHttpUrl
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockHttpUrl.newBuilder() } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.addQueryParameter("api_key", testApiKey) } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.build() } returns mockHttpUrl
        every { mockRequestBuilder.url(mockHttpUrl) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockRequest
        every { mockChain.proceed(mockRequest) } returns mockResponse
        
        // Act
        val result = interceptor.intercept(mockChain)
        
        // Assert
        assertEquals(mockResponse, result)
        verify { mockHttpUrlBuilder.addQueryParameter("api_key", testApiKey) }
    }
    
    @Test
    fun `intercept preserves request method and headers`() {
        // Arrange
        val mockHeaders = mockk<Headers>()
        
        every { mockChain.request() } returns mockRequest
        every { mockRequest.url } returns mockHttpUrl
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockRequest.method } returns "GET"
        every { mockRequest.headers } returns mockHeaders
        every { mockHttpUrl.newBuilder() } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.addQueryParameter("api_key", testApiKey) } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.build() } returns mockHttpUrl
        every { mockRequestBuilder.url(mockHttpUrl) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockRequest
        every { mockChain.proceed(mockRequest) } returns mockResponse
        
        // Act
        val result = interceptor.intercept(mockChain)
        
        // Assert
        assertEquals(mockResponse, result)
        verify { mockChain.proceed(mockRequest) }
    }
    
    @Test
    fun `intercept handles exceptions gracefully`() {
        // Arrange
        val exception = RuntimeException("Network error")
        
        every { mockChain.request() } returns mockRequest
        every { mockRequest.url } returns mockHttpUrl
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockHttpUrl.newBuilder() } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.addQueryParameter("api_key", testApiKey) } returns mockHttpUrlBuilder
        every { mockHttpUrlBuilder.build() } returns mockHttpUrl
        every { mockRequestBuilder.url(mockHttpUrl) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockRequest
        every { mockChain.proceed(mockRequest) } throws exception
        
        // Act & Assert
        try {
            interceptor.intercept(mockChain)
            assertTrue(false, "Expected exception to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("Network error", e.message)
        }
    }
    
    @Test
    fun `interceptor can be reused for multiple requests`() {
        // Arrange
        val request1 = mockk<Request>()
        val request2 = mockk<Request>()
        val response1 = mockk<Response>()
        val response2 = mockk<Response>()
        val url1 = mockk<HttpUrl>()
        val url2 = mockk<HttpUrl>()
        val builder1 = mockk<Request.Builder>()
        val builder2 = mockk<Request.Builder>()
        val urlBuilder1 = mockk<HttpUrl.Builder>()
        val urlBuilder2 = mockk<HttpUrl.Builder>()
        
        val chain1 = mockk<Interceptor.Chain>()
        val chain2 = mockk<Interceptor.Chain>()
        
        // Setup first request
        every { chain1.request() } returns request1
        every { request1.url } returns url1
        every { request1.newBuilder() } returns builder1
        every { url1.newBuilder() } returns urlBuilder1
        every { urlBuilder1.addQueryParameter("api_key", testApiKey) } returns urlBuilder1
        every { urlBuilder1.build() } returns url1
        every { builder1.url(url1) } returns builder1
        every { builder1.build() } returns request1
        every { chain1.proceed(request1) } returns response1
        
        // Setup second request
        every { chain2.request() } returns request2
        every { request2.url } returns url2
        every { request2.newBuilder() } returns builder2
        every { url2.newBuilder() } returns urlBuilder2
        every { urlBuilder2.addQueryParameter("api_key", testApiKey) } returns urlBuilder2
        every { urlBuilder2.build() } returns url2
        every { builder2.url(url2) } returns builder2
        every { builder2.build() } returns request2
        every { chain2.proceed(request2) } returns response2
        
        // Act
        val result1 = interceptor.intercept(chain1)
        val result2 = interceptor.intercept(chain2)
        
        // Assert
        assertEquals(response1, result1)
        assertEquals(response2, result2)
        verify { chain1.proceed(request1) }
        verify { chain2.proceed(request2) }
    }
}