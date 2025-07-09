package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.TMDbMovieDao
import com.rdwatch.androidtv.data.dao.TMDbSearchDao
import com.rdwatch.androidtv.data.entities.TMDbMovieEntity
import com.rdwatch.androidtv.data.mappers.TMDbToContentDetailMapper
import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbRecommendationsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieImagesResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieVideosResponse
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for TMDbMovieRepositoryImpl
 * Tests repository implementation with mocked dependencies
 */
class TMDbMovieRepositoryImplTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var repository: TMDbMovieRepositoryImpl
    private lateinit var mockTmdbMovieService: TMDbMovieService
    private lateinit var mockTmdbMovieDao: TMDbMovieDao
    private lateinit var mockTmdbSearchDao: TMDbSearchDao
    private lateinit var mockContentDetailMapper: TMDbToContentDetailMapper
    
    private lateinit var mockMovieCall: Call<ApiResponse<TMDbMovieResponse>>
    private lateinit var mockCreditsCall: Call<ApiResponse<TMDbCreditsResponse>>
    private lateinit var mockRecommendationsCall: Call<ApiResponse<TMDbRecommendationsResponse>>
    private lateinit var mockImagesCall: Call<ApiResponse<TMDbMovieImagesResponse>>
    private lateinit var mockVideosCall: Call<ApiResponse<TMDbMovieVideosResponse>>
    
    @Before
    fun setUp() {
        mockTmdbMovieService = mockk()
        mockTmdbMovieDao = mockk()
        mockTmdbSearchDao = mockk()
        mockContentDetailMapper = mockk()
        
        mockMovieCall = mockk()
        mockCreditsCall = mockk()
        mockRecommendationsCall = mockk()
        mockImagesCall = mockk()
        mockVideosCall = mockk()
        
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
    
    @Test
    fun `getMovieDetails returns cached data when available and not expired`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val cachedEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        val expectedMovieResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedEntity)
        every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns System.currentTimeMillis()
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(movieId, result.data.id)
        assertEquals(expectedMovieResponse.title, result.data.title)
        verify { mockTmdbMovieDao.getMovieById(movieId) }
        verify(exactly = 0) { mockTmdbMovieService.getMovieDetails(any(), any(), any()) }
    }
    
    @Test
    fun `getMovieDetails fetches from API when cache is expired`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val cachedEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        // Cache is expired (older than 24 hours)
        val expiredTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedEntity)
        every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns expiredTime
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        every { mockTmdbMovieDao.insertMovie(any()) } just runs
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(movieId, result.data.id)
        verify { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") }
        verify { mockTmdbMovieDao.insertMovie(any()) }
    }
    
    @Test
    fun `getMovieDetails force refresh bypasses cache`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val cachedEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedEntity)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        every { mockTmdbMovieDao.insertMovie(any()) } just runs
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = true).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(movieId, result.data.id)
        verify { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") }
        verify { mockTmdbMovieDao.insertMovie(any()) }
    }
    
    @Test
    fun `getMovieDetails returns error when API call fails`() = runTest {
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
    fun `getMovieContentDetail maps movie response to ContentDetail`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val cachedEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        val expectedContentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail()
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedEntity)
        every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns System.currentTimeMillis()
        every { mockContentDetailMapper.mapMovieToContentDetail(any()) } returns expectedContentDetail
        
        // Act
        val result = repository.getMovieContentDetail(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(expectedContentDetail.id, result.data.id)
        assertEquals(expectedContentDetail.title, result.data.title)
        verify { mockContentDetailMapper.mapMovieToContentDetail(any()) }
    }
    
    @Test
    fun `getMovieCredits returns cached credits when available`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val expectedCredits = TMDbTestDataFactory.createTMDbCreditsResponse()
        
        every { mockTmdbSearchDao.getCredits(movieId, "movie") } returns flowOf(
            com.rdwatch.androidtv.data.entities.TMDbCreditsEntity(
                id = movieId,
                contentType = "movie",
                cast = emptyList(),
                crew = emptyList(),
                lastUpdated = System.currentTimeMillis()
            )
        )
        
        // Act
        val result = repository.getMovieCredits(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        verify { mockTmdbSearchDao.getCredits(movieId, "movie") }
        verify(exactly = 0) { mockTmdbMovieService.getMovieCredits(any(), any()) }
    }
    
    @Test
    fun `getMovieCredits fetches from API when cache is empty`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val apiResponse = TMDbTestDataFactory.createTMDbCreditsResponse()
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbSearchDao.getCredits(movieId, "movie") } returns flowOf(null)
        every { mockTmdbMovieService.getMovieCredits(movieId, "en-US") } returns mockCreditsCall
        every { mockCreditsCall.execute() } returns retrofitResponse
        every { mockTmdbSearchDao.insertCredits(any()) } just runs
        
        // Act
        val result = repository.getMovieCredits(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(movieId, result.data.id)
        verify { mockTmdbMovieService.getMovieCredits(movieId, "en-US") }
        verify { mockTmdbSearchDao.insertCredits(any()) }
    }
    
    @Test
    fun `getMovieRecommendations returns cached recommendations when available`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val page = 1
        val expectedRecommendations = TMDbTestDataFactory.createTMDbRecommendationsResponse()
        
        every { mockTmdbSearchDao.getRecommendations(movieId, "movie", "recommendations", page) } returns flowOf(
            com.rdwatch.androidtv.data.entities.TMDbRecommendationsEntity(
                id = movieId,
                contentType = "movie",
                recommendationType = "recommendations",
                page = page,
                results = emptyList(),
                totalPages = 1,
                totalResults = 0,
                lastUpdated = System.currentTimeMillis()
            )
        )
        
        // Act
        val result = repository.getMovieRecommendations(movieId, page, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        verify { mockTmdbSearchDao.getRecommendations(movieId, "movie", "recommendations", page) }
        verify(exactly = 0) { mockTmdbMovieService.getMovieRecommendations(any(), any(), any()) }
    }
    
    @Test
    fun `getSimilarMovies returns cached similar movies when available`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val page = 1
        val expectedSimilar = TMDbTestDataFactory.createTMDbRecommendationsResponse()
        
        every { mockTmdbSearchDao.getRecommendations(movieId, "movie", "similar", page) } returns flowOf(
            com.rdwatch.androidtv.data.entities.TMDbRecommendationsEntity(
                id = movieId,
                contentType = "movie",
                recommendationType = "similar",
                page = page,
                results = emptyList(),
                totalPages = 1,
                totalResults = 0,
                lastUpdated = System.currentTimeMillis()
            )
        )
        
        // Act
        val result = repository.getSimilarMovies(movieId, page, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        verify { mockTmdbSearchDao.getRecommendations(movieId, "movie", "similar", page) }
        verify(exactly = 0) { mockTmdbMovieService.getSimilarMovies(any(), any(), any()) }
    }
    
    @Test
    fun `getMovieImages returns cached images when available`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val expectedImages = TMDbTestDataFactory.createTMDbMovieImagesResponse()
        
        every { mockTmdbSearchDao.getImages(movieId, "movie") } returns flowOf(
            com.rdwatch.androidtv.data.entities.TMDbImagesEntity(
                id = movieId,
                contentType = "movie",
                backdrops = emptyList(),
                logos = emptyList(),
                posters = emptyList(),
                lastUpdated = System.currentTimeMillis()
            )
        )
        
        // Act
        val result = repository.getMovieImages(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        verify { mockTmdbSearchDao.getImages(movieId, "movie") }
        verify(exactly = 0) { mockTmdbMovieService.getMovieImages(any(), any()) }
    }
    
    @Test
    fun `getMovieVideos returns cached videos when available`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val expectedVideos = TMDbTestDataFactory.createTMDbMovieVideosResponse()
        
        every { mockTmdbSearchDao.getVideos(movieId, "movie") } returns flowOf(
            com.rdwatch.androidtv.data.entities.TMDbVideosEntity(
                id = movieId,
                contentType = "movie",
                results = emptyList(),
                lastUpdated = System.currentTimeMillis()
            )
        )
        
        // Act
        val result = repository.getMovieVideos(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        verify { mockTmdbSearchDao.getVideos(movieId, "movie") }
        verify(exactly = 0) { mockTmdbMovieService.getMovieVideos(any(), any()) }
    }
    
    @Test
    fun `getPopularMovies fetches from API when cache is empty`() = runTest {
        // Arrange
        val page = 1
        val apiResponse = TMDbTestDataFactory.createTMDbRecommendationsResponse()
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbSearchDao.getRecommendations(0, "movie", "popular", page) } returns flowOf(null)
        every { mockTmdbMovieService.getPopularMovies("en-US", page, null) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns retrofitResponse
        every { mockTmdbSearchDao.insertRecommendations(any()) } just runs
        
        // Act
        val result = repository.getPopularMovies(page, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        verify { mockTmdbMovieService.getPopularMovies("en-US", page, null) }
        verify { mockTmdbSearchDao.insertRecommendations(any()) }
    }
    
    @Test
    fun `clearCache clears all cached data`() = runTest {
        // Arrange
        every { mockTmdbMovieDao.deleteAllMovies() } just runs
        every { mockTmdbSearchDao.deleteAllSearchResults() } just runs
        every { mockTmdbSearchDao.deleteAllCredits() } just runs
        every { mockTmdbSearchDao.deleteAllRecommendations() } just runs
        every { mockTmdbSearchDao.deleteAllImages() } just runs
        every { mockTmdbSearchDao.deleteAllVideos() } just runs
        
        // Act
        repository.clearCache()
        
        // Assert
        verify { mockTmdbMovieDao.deleteAllMovies() }
        verify { mockTmdbSearchDao.deleteAllSearchResults() }
        verify { mockTmdbSearchDao.deleteAllCredits() }
        verify { mockTmdbSearchDao.deleteAllRecommendations() }
        verify { mockTmdbSearchDao.deleteAllImages() }
        verify { mockTmdbSearchDao.deleteAllVideos() }
    }
    
    @Test
    fun `clearMovieCache clears movie-specific cached data`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        
        every { mockTmdbMovieDao.deleteMovieById(movieId) } just runs
        every { mockTmdbSearchDao.deleteCredits(movieId, "movie") } just runs
        every { mockTmdbSearchDao.deleteRecommendations(movieId, "movie", "recommendations") } just runs
        every { mockTmdbSearchDao.deleteRecommendations(movieId, "movie", "similar") } just runs
        every { mockTmdbSearchDao.deleteImages(movieId, "movie") } just runs
        every { mockTmdbSearchDao.deleteVideos(movieId, "movie") } just runs
        
        // Act
        repository.clearMovieCache(movieId)
        
        // Assert
        verify { mockTmdbMovieDao.deleteMovieById(movieId) }
        verify { mockTmdbSearchDao.deleteCredits(movieId, "movie") }
        verify { mockTmdbSearchDao.deleteRecommendations(movieId, "movie", "recommendations") }
        verify { mockTmdbSearchDao.deleteRecommendations(movieId, "movie", "similar") }
        verify { mockTmdbSearchDao.deleteImages(movieId, "movie") }
        verify { mockTmdbSearchDao.deleteVideos(movieId, "movie") }
    }
    
    @Test
    fun `searchMovies throws NotImplementedError`() = runTest {
        // Act & Assert
        assertFailsWith<NotImplementedError> {
            repository.searchMovies("test query").first()
        }
    }
    
    @Test
    fun `discoverMovies throws NotImplementedError`() = runTest {
        // Act & Assert
        assertFailsWith<NotImplementedError> {
            repository.discoverMovies().first()
        }
    }
    
    @Test
    fun `getTrendingMovies throws NotImplementedError`() = runTest {
        // Act & Assert
        assertFailsWith<NotImplementedError> {
            repository.getTrendingMovies("day").first()
        }
    }
    
    @Test
    fun `repository handles API response errors properly`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val errorApiResponse = TMDbTestDataFactory.createErrorApiResponse<TMDbMovieResponse>("Movie not found", 404)
        val retrofitResponse = Response.success(errorApiResponse)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message!!.contains("Movie not found"))
    }
    
    @Test
    fun `repository handles network exceptions`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val networkException = TMDbTestDataFactory.ErrorScenarios.networkError()
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } throws networkException
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals(networkException.message, result.exception.message)
    }
}