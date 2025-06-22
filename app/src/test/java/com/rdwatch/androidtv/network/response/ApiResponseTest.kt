package com.rdwatch.androidtv.network.response

import com.rdwatch.androidtv.network.models.UserInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ApiResponseTest {
    
    @Test
    fun `safeApiCall returns Success when API call succeeds`() = runBlocking {
        // Given
        val mockUserInfo = UserInfo(
            id = 123,
            username = "testuser",
            email = "test@example.com",
            points = 100,
            locale = "en",
            avatar = "avatar.jpg",
            type = "premium",
            premium = 1,
            expiration = "2024-12-31"
        )
        
        val apiCall: suspend () -> Response<UserInfo> = mockk()
        coEvery { apiCall() } returns Response.success(mockUserInfo)
        
        // When
        val result = safeApiCall(apiCall)
        
        // Then
        assertTrue(result is ApiResponse.Success)
        assertEquals(mockUserInfo, (result as ApiResponse.Success).data)
    }
    
    @Test
    fun `safeApiCall returns Error with AuthException on 401`() = runBlocking {
        // Given
        val apiCall: suspend () -> Response<UserInfo> = mockk()
        coEvery { apiCall() } returns Response.error(401, mockk(relaxed = true))
        
        // When
        val result = safeApiCall(apiCall)
        
        // Then
        assertTrue(result is ApiResponse.Error)
        val error = (result as ApiResponse.Error).exception
        assertTrue(error is ApiException.AuthException)
        assertEquals(401, (error as ApiException.AuthException).code)
    }
    
    @Test
    fun `safeApiCall returns Error with HttpException on 400 range errors`() = runBlocking {
        // Given
        val apiCall: suspend () -> Response<UserInfo> = mockk()
        coEvery { apiCall() } returns Response.error(404, mockk(relaxed = true))
        
        // When
        val result = safeApiCall(apiCall)
        
        // Then
        assertTrue(result is ApiResponse.Error)
        val error = (result as ApiResponse.Error).exception
        assertTrue(error is ApiException.HttpException)
        assertEquals(404, (error as ApiException.HttpException).code)
    }
    
    @Test
    fun `safeApiCall returns Error with HttpException on 500 range errors`() = runBlocking {
        // Given
        val apiCall: suspend () -> Response<UserInfo> = mockk()
        coEvery { apiCall() } returns Response.error(503, mockk(relaxed = true))
        
        // When
        val result = safeApiCall(apiCall)
        
        // Then
        assertTrue(result is ApiResponse.Error)
        val error = (result as ApiResponse.Error).exception
        assertTrue(error is ApiException.HttpException)
        assertEquals(503, (error as ApiException.HttpException).code)
        assertTrue(error.message.contains("Server error"))
    }
    
    @Test
    fun `safeApiCall returns Error with NetworkException on IOException`() = runBlocking {
        // Given
        val apiCall: suspend () -> Response<UserInfo> = mockk()
        coEvery { apiCall() } throws IOException("Network error")
        
        // When
        val result = safeApiCall(apiCall)
        
        // Then
        assertTrue(result is ApiResponse.Error)
        val error = (result as ApiResponse.Error).exception
        assertTrue(error is ApiException.NetworkException)
        assertEquals("Network error", error.message)
    }
    
    @Test
    fun `safeApiCall returns Error with ParseException when body is null`() = runBlocking {
        // Given
        val apiCall: suspend () -> Response<UserInfo?> = mockk()
        coEvery { apiCall() } returns Response.success(null)
        
        // When
        val result = safeApiCall(apiCall)
        
        // Then
        assertTrue(result is ApiResponse.Error)
        val error = (result as ApiResponse.Error).exception
        assertTrue(error is ApiException.ParseException)
        assertEquals("Response body is null", error.message)
    }
    
    @Test
    fun `safeApiCall returns Error with UnknownException on unexpected error`() = runBlocking {
        // Given
        val apiCall: suspend () -> Response<UserInfo> = mockk()
        coEvery { apiCall() } throws RuntimeException("Unexpected error")
        
        // When
        val result = safeApiCall(apiCall)
        
        // Then
        assertTrue(result is ApiResponse.Error)
        val error = (result as ApiResponse.Error).exception
        assertTrue(error is ApiException.UnknownException)
        assertEquals("Unexpected error", error.message)
    }
}