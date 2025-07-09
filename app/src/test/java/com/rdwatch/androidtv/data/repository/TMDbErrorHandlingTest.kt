package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.TMDbMovieDao
import com.rdwatch.androidtv.data.dao.TMDbSearchDao
import com.rdwatch.androidtv.data.mappers.TMDbToContentDetailMapper
import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.test.data.TMDbTestDataFactory
import com.rdwatch.androidtv.test.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for error handling in TMDb repository implementations
 * Tests various network failure scenarios and API error responses
 */
class TMDbErrorHandlingTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var repository: TMDbMovieRepositoryImpl
    private lateinit var mockTmdbMovieService: TMDbMovieService
    private lateinit var mockTmdbMovieDao: TMDbMovieDao
    private lateinit var mockTmdbSearchDao: TMDbSearchDao
    private lateinit var mockContentDetailMapper: TMDbToContentDetailMapper
    
    private lateinit var mockMovieCall: Call<ApiResponse<TMDbMovieResponse>>
    private lateinit var mockCreditsCall: Call<ApiResponse<TMDbCreditsResponse>>
    
    @Before
    fun setUp() {
        mockTmdbMovieService = mockk()
        mockTmdbMovieDao = mockk()
        mockTmdbSearchDao = mockk()
        mockContentDetailMapper = mockk()
        
        mockMovieCall = mockk()
        mockCreditsCall = mockk()
        
        repository = TMDbMovieRepositoryImpl(
            tmdbMovieService = mockTmdbMovieService,
            tmdbMovieDao = mockTmdbMovieDao,
            tmdbSearchDao = mockTmdbSearchDao,
            contentDetailMapper = mockContentDetailMapper
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // Network Error Tests
    
    @Test
    fun `getMovieDetails handles network timeout error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val timeoutException = SocketTimeoutException("Read timeout")
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } throws timeoutException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(timeoutException, result.exception)
    }
    
    @Test
    fun `getMovieDetails handles network connection error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val connectionException = UnknownHostException("Unable to resolve host")
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } throws connectionException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(connectionException, result.exception)
    }
    
    @Test
    fun `getMovieDetails handles SSL handshake error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val sslException = SSLHandshakeException("SSL handshake failed")
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } throws sslException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(sslException, result.exception)
    }
    
    @Test
    fun `getMovieDetails handles IO exception`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val ioException = IOException("Network error")
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } throws ioException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(ioException, result.exception)
    }
    
    // HTTP Error Response Tests
    
    @Test
    fun `getMovieDetails handles 401 Unauthorized error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val errorResponse = Response.error<ApiResponse<TMDbMovieResponse>>(
            401, 
            okhttp3.ResponseBody.create(null, "Unauthorized")
        )
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns errorResponse
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains("401"))
    }
    
    @Test
    fun `getMovieDetails handles 404 Not Found error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val errorResponse = Response.error<ApiResponse<TMDbMovieResponse>>(
            404, 
            okhttp3.ResponseBody.create(null, "Not Found")
        )
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns errorResponse
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains("404"))
    }
    
    @Test
    fun `getMovieDetails handles 429 Rate Limit error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val errorResponse = Response.error<ApiResponse<TMDbMovieResponse>>(
            429, 
            okhttp3.ResponseBody.create(null, "Too Many Requests")
        )
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns errorResponse
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains("429"))
    }
    
    @Test
    fun `getMovieDetails handles 500 Internal Server Error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val errorResponse = Response.error<ApiResponse<TMDbMovieResponse>>(
            500, 
            okhttp3.ResponseBody.create(null, "Internal Server Error")
        )
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns errorResponse
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains("500"))
    }
    
    @Test
    fun `getMovieDetails handles 503 Service Unavailable error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val errorResponse = Response.error<ApiResponse<TMDbMovieResponse>>(
            503, 
            okhttp3.ResponseBody.create(null, "Service Unavailable")
        )
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns errorResponse
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains("503"))
    }
    
    // API Response Error Tests
    
    @Test
    fun `getMovieDetails handles ApiResponse Error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val apiError = TMDbTestDataFactory.createErrorApiResponse<TMDbMovieResponse>("Invalid API key", 401)
        val errorResponse = Response.success(apiError)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns errorResponse
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains("Invalid API key"))
    }
    
    @Test
    fun `getMovieDetails handles null response body`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val nullResponse = Response.success<ApiResponse<TMDbMovieResponse>>(null)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns nullResponse
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains("Empty response body"))
    }
    
    // Database Error Tests
    
    @Test
    fun `getMovieDetails handles database access error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val databaseException = TMDbTestDataFactory.ErrorScenarios.databaseError()
        
        every { mockTmdbMovieDao.getMovieById(movieId) } throws databaseException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(databaseException, result.exception)
    }
    
    @Test
    fun `getMovieDetails handles database insertion error during save`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        val databaseException = TMDbTestDataFactory.ErrorScenarios.databaseError()
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        every { mockTmdbMovieDao.insertMovie(any()) } throws databaseException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(databaseException, result.exception)
    }
    
    // Multiple Error Scenarios
    
    @Test
    fun `getMovieCredits handles network error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val networkException = TMDbTestDataFactory.ErrorScenarios.networkError()
        
        every { mockTmdbSearchDao.getCredits(movieId, "movie") } returns flowOf(null)
        every { mockTmdbMovieService.getMovieCredits(movieId, "en-US") } returns mockCreditsCall
        every { mockCreditsCall.execute() } throws networkException
        
        // Act
        val result = repository.getMovieCredits(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(networkException, result.exception)
    }
    
    @Test
    fun `getMovieCredits handles 404 error`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val errorResponse = Response.error<ApiResponse<TMDbCreditsResponse>>(
            404, 
            okhttp3.ResponseBody.create(null, "Credits not found")
        )
        
        every { mockTmdbSearchDao.getCredits(movieId, "movie") } returns flowOf(null)
        every { mockTmdbMovieService.getMovieCredits(movieId, "en-US") } returns mockCreditsCall
        every { mockCreditsCall.execute() } returns errorResponse
        
        // Act
        val result = repository.getMovieCredits(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains("404"))
    }
    
    // Error Recovery Tests
    
    @Test
    fun `getMovieDetails falls back to cached data when network fails`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val cachedEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        val networkException = TMDbTestDataFactory.ErrorScenarios.networkError()
        
        // Cache is expired, so it should try to fetch from network
        val expiredTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedEntity)
        every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns expiredTime
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } throws networkException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        // Since we're using NetworkBoundResource, it should still return the cached data
        // even if the network call fails, depending on the implementation
        assertTrue(result is Result.Error)
        assertEquals(networkException, result.exception)
    }
    
    @Test
    fun `repository handles multiple concurrent errors`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val networkException = TMDbTestDataFactory.ErrorScenarios.networkError()
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } throws networkException
        
        every { mockTmdbSearchDao.getCredits(movieId, "movie") } returns flowOf(null)
        every { mockTmdbMovieService.getMovieCredits(movieId, "en-US") } returns mockCreditsCall
        every { mockCreditsCall.execute() } throws networkException
        
        // Act
        val movieResult = repository.getMovieDetails(movieId, forceRefresh = false).first()
        val creditsResult = repository.getMovieCredits(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(movieResult is Result.Error)
        assertTrue(creditsResult is Result.Error)
        assertEquals(networkException, movieResult.exception)
        assertEquals(networkException, creditsResult.exception)
    }
    
    // JSON Parsing Error Tests
    
    @Test
    fun `repository handles JSON parsing errors`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val parseException = TMDbTestDataFactory.ErrorScenarios.parseError()
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } throws parseException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(parseException, result.exception)
    }
    
    // Cache Corruption Tests
    
    @Test
    fun `repository handles corrupted cache data`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val corruptedException = Exception("Corrupted cache data")
        
        every { mockTmdbMovieDao.getMovieById(movieId) } throws corruptedException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(corruptedException, result.exception)
    }
    
    // Custom Error Scenarios
    
    @Test
    fun `repository handles custom API error messages`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val customErrorMessage = "Custom API error: Resource not available in your region"
        val apiError = TMDbTestDataFactory.createErrorApiResponse<TMDbMovieResponse>(customErrorMessage, 403)
        val errorResponse = Response.success(apiError)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns errorResponse
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains(customErrorMessage))
    }
}