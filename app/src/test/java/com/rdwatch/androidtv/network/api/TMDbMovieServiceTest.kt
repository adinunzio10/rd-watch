package com.rdwatch.androidtv.network.api

import com.rdwatch.androidtv.network.models.tmdb.*
import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.test.data.TMDbTestDataFactory
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for TMDbMovieService
 * Tests API service methods with mocked responses
 */
class TMDbMovieServiceTest {
    
    private lateinit var mockService: TMDbMovieService
    private lateinit var mockCall: Call<ApiResponse<TMDbMovieResponse>>
    private lateinit var mockCreditsCall: Call<ApiResponse<TMDbCreditsResponse>>
    private lateinit var mockRecommendationsCall: Call<ApiResponse<TMDbRecommendationsResponse>>
    private lateinit var mockImagesCall: Call<ApiResponse<TMDbMovieImagesResponse>>
    private lateinit var mockVideosCall: Call<ApiResponse<TMDbMovieVideosResponse>>
    
    @Before
    fun setUp() {
        mockService = mockk()
        mockCall = mockk()
        mockCreditsCall = mockk()
        mockRecommendationsCall = mockk()
        mockImagesCall = mockk()
        mockVideosCall = mockk()
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `getMovieDetails returns success response`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val expectedMovie = TMDbTestDataFactory.createTMDbMovieResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedMovie)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getMovieDetails(movieId, null, "en-US") } returns mockCall
        every { mockCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getMovieDetails(movieId)
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedMovie.id, (apiResponse as ApiResponse.Success).data.id)
        assertEquals(expectedMovie.title, apiResponse.data.title)
    }
    
    @Test
    fun `getMovieDetails with appendToResponse parameter`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val appendToResponse = "credits,videos,images"
        val expectedMovie = TMDbTestDataFactory.createTMDbMovieResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedMovie)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getMovieDetails(movieId, appendToResponse, "en-US") } returns mockCall
        every { mockCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getMovieDetails(movieId, appendToResponse)
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        verify { mockService.getMovieDetails(movieId, appendToResponse, "en-US") }
    }
    
    @Test
    fun `getMovieDetails with custom language`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val language = "es-ES"
        val expectedMovie = TMDbTestDataFactory.createTMDbMovieResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedMovie)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getMovieDetails(movieId, null, language) } returns mockCall
        every { mockCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getMovieDetails(movieId, language = language)
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        verify { mockService.getMovieDetails(movieId, null, language) }
    }
    
    @Test
    fun `getMovieDetails returns error response`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val errorResponse = Response.error<ApiResponse<TMDbMovieResponse>>(
            404, 
            okhttp3.ResponseBody.create(null, "Not Found")
        )
        
        every { mockService.getMovieDetails(movieId, null, "en-US") } returns mockCall
        every { mockCall.execute() } returns errorResponse
        
        // Act
        val call = mockService.getMovieDetails(movieId)
        val response = call.execute()
        
        // Assert
        assertTrue(!response.isSuccessful)
        assertEquals(404, response.code())
    }
    
    @Test
    fun `getMovieCredits returns success response`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val expectedCredits = TMDbTestDataFactory.createTMDbCreditsResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedCredits)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getMovieCredits(movieId, "en-US") } returns mockCreditsCall
        every { mockCreditsCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getMovieCredits(movieId)
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedCredits.id, (apiResponse as ApiResponse.Success).data.id)
        assertEquals(2, apiResponse.data.cast.size)
        assertEquals(2, apiResponse.data.crew.size)
    }
    
    @Test
    fun `getMovieRecommendations returns success response`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val page = 1
        val expectedRecommendations = TMDbTestDataFactory.createTMDbRecommendationsResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedRecommendations)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getMovieRecommendations(movieId, "en-US", page) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getMovieRecommendations(movieId)
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedRecommendations.page, (apiResponse as ApiResponse.Success).data.page)
        assertEquals(1, apiResponse.data.results.size)
    }
    
    @Test
    fun `getSimilarMovies returns success response`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val page = 1
        val expectedSimilar = TMDbTestDataFactory.createTMDbRecommendationsResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedSimilar)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getSimilarMovies(movieId, "en-US", page) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getSimilarMovies(movieId)
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedSimilar.page, (apiResponse as ApiResponse.Success).data.page)
    }
    
    @Test
    fun `getMovieImages returns success response`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val expectedImages = TMDbTestDataFactory.createTMDbMovieImagesResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedImages)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getMovieImages(movieId, null) } returns mockImagesCall
        every { mockImagesCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getMovieImages(movieId)
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedImages.id, (apiResponse as ApiResponse.Success).data.id)
        assertEquals(1, apiResponse.data.backdrops.size)
        assertEquals(1, apiResponse.data.posters.size)
        assertEquals(1, apiResponse.data.logos.size)
    }
    
    @Test
    fun `getMovieVideos returns success response`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val expectedVideos = TMDbTestDataFactory.createTMDbMovieVideosResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedVideos)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getMovieVideos(movieId, "en-US") } returns mockVideosCall
        every { mockVideosCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getMovieVideos(movieId)
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedVideos.id, (apiResponse as ApiResponse.Success).data.id)
        assertEquals(1, apiResponse.data.results.size)
        assertEquals("YouTube", apiResponse.data.results.first().site)
    }
    
    @Test
    fun `getPopularMovies returns success response`() = runTest {
        // Arrange
        val page = 1
        val expectedPopular = TMDbTestDataFactory.createTMDbRecommendationsResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedPopular)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getPopularMovies("en-US", page, null) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getPopularMovies()
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedPopular.page, (apiResponse as ApiResponse.Success).data.page)
    }
    
    @Test
    fun `getTopRatedMovies returns success response`() = runTest {
        // Arrange
        val page = 1
        val expectedTopRated = TMDbTestDataFactory.createTMDbRecommendationsResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedTopRated)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getTopRatedMovies("en-US", page, null) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getTopRatedMovies()
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedTopRated.page, (apiResponse as ApiResponse.Success).data.page)
    }
    
    @Test
    fun `getNowPlayingMovies returns success response`() = runTest {
        // Arrange
        val page = 1
        val expectedNowPlaying = TMDbTestDataFactory.createTMDbRecommendationsResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedNowPlaying)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getNowPlayingMovies("en-US", page, null) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getNowPlayingMovies()
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedNowPlaying.page, (apiResponse as ApiResponse.Success).data.page)
    }
    
    @Test
    fun `getUpcomingMovies returns success response`() = runTest {
        // Arrange
        val page = 1
        val expectedUpcoming = TMDbTestDataFactory.createTMDbRecommendationsResponse()
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedUpcoming)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getUpcomingMovies("en-US", page, null) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns expectedRetrofitResponse
        
        // Act
        val call = mockService.getUpcomingMovies()
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        val apiResponse = response.body()
        assertTrue(apiResponse is ApiResponse.Success)
        assertEquals(expectedUpcoming.page, (apiResponse as ApiResponse.Success).data.page)
    }
    
    @Test
    fun `service constants are correct`() {
        // Assert
        assertEquals("https://api.themoviedb.org/3/", TMDbMovieService.BASE_URL)
        assertEquals("https://image.tmdb.org/t/p/", TMDbMovieService.IMAGE_BASE_URL)
        assertEquals("w1280", TMDbMovieService.BACKDROP_SIZE)
        assertEquals("w500", TMDbMovieService.POSTER_SIZE)
        assertEquals("w185", TMDbMovieService.PROFILE_SIZE)
    }
    
    @Test
    fun `service calls with pagination parameters`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val page = 2
        val language = "es-ES"
        val region = "ES"
        val expectedRecommendations = TMDbTestDataFactory.createTMDbRecommendationsResponse(page = page)
        val expectedApiResponse = TMDbTestDataFactory.createSuccessApiResponse(expectedRecommendations)
        val expectedRetrofitResponse = Response.success(expectedApiResponse)
        
        every { mockService.getMovieRecommendations(movieId, language, page) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns expectedRetrofitResponse
        
        every { mockService.getPopularMovies(language, page, region) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns expectedRetrofitResponse
        
        // Act & Assert - Recommendations
        val recommendationsCall = mockService.getMovieRecommendations(movieId, language, page)
        val recommendationsResponse = recommendationsCall.execute()
        
        assertTrue(recommendationsResponse.isSuccessful)
        verify { mockService.getMovieRecommendations(movieId, language, page) }
        
        // Act & Assert - Popular with region
        val popularCall = mockService.getPopularMovies(language, page, region)
        val popularResponse = popularCall.execute()
        
        assertTrue(popularResponse.isSuccessful)
        verify { mockService.getPopularMovies(language, page, region) }
    }
    
    @Test
    fun `service handles null responses gracefully`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val nullResponse = Response.success<ApiResponse<TMDbMovieResponse>>(null)
        
        every { mockService.getMovieDetails(movieId, null, "en-US") } returns mockCall
        every { mockCall.execute() } returns nullResponse
        
        // Act
        val call = mockService.getMovieDetails(movieId)
        val response = call.execute()
        
        // Assert
        assertTrue(response.isSuccessful)
        assertEquals(null, response.body())
    }
}