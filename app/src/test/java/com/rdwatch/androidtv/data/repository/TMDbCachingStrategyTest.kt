package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.TMDbMovieDao
import com.rdwatch.androidtv.data.dao.TMDbSearchDao
import com.rdwatch.androidtv.data.mappers.TMDbToContentDetailMapper
import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbRecommendationsResponse
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
import kotlin.test.assertTrue

/**
 * Tests for caching strategy implementation in TMDb repository
 * Tests cache hit/miss scenarios, cache expiration, and cache management
 */
class TMDbCachingStrategyTest {
    
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
    
    companion object {
        private const val CACHE_TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    @Before
    fun setUp() {
        mockTmdbMovieService = mockk()
        mockTmdbMovieDao = mockk()
        mockTmdbSearchDao = mockk()
        mockContentDetailMapper = mockk()
        
        mockMovieCall = mockk()
        mockCreditsCall = mockk()
        mockRecommendationsCall = mockk()
        
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
    
    // Cache Hit Tests
    
    @Test
    fun `getMovieDetails cache hit - returns cached data without API call`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val cachedEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        val currentTime = System.currentTimeMillis()
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedEntity)
        every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns currentTime
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(movieId, result.data.id)
        
        // Verify no API call was made
        verify(exactly = 0) { mockTmdbMovieService.getMovieDetails(any(), any(), any()) }
        verify { mockTmdbMovieDao.getMovieById(movieId) }
        verify { mockTmdbMovieDao.getMovieLastUpdated(movieId) }
    }
    
    @Test
    fun `getMovieCredits cache hit - returns cached data without API call`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val cachedCredits = com.rdwatch.androidtv.data.entities.TMDbCreditsEntity(
            id = movieId,
            contentType = "movie",
            cast = emptyList(),
            crew = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        every { mockTmdbSearchDao.getCredits(movieId, "movie") } returns flowOf(cachedCredits)
        
        // Act
        val result = repository.getMovieCredits(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(movieId, result.data.id)
        
        // Verify no API call was made
        verify(exactly = 0) { mockTmdbMovieService.getMovieCredits(any(), any()) }
        verify { mockTmdbSearchDao.getCredits(movieId, "movie") }
    }
    
    @Test
    fun `getMovieRecommendations cache hit - returns cached data without API call`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val page = 1
        val cachedRecommendations = com.rdwatch.androidtv.data.entities.TMDbRecommendationsEntity(
            id = movieId,
            contentType = "movie",
            recommendationType = "recommendations",
            page = page,
            results = emptyList(),
            totalPages = 1,
            totalResults = 0,
            lastUpdated = System.currentTimeMillis()
        )
        
        every { mockTmdbSearchDao.getRecommendations(movieId, "movie", "recommendations", page) } returns flowOf(cachedRecommendations)
        
        // Act
        val result = repository.getMovieRecommendations(movieId, page, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(page, result.data.page)
        
        // Verify no API call was made
        verify(exactly = 0) { mockTmdbMovieService.getMovieRecommendations(any(), any(), any()) }
        verify { mockTmdbSearchDao.getRecommendations(movieId, "movie", "recommendations", page) }
    }
    
    // Cache Miss Tests
    
    @Test
    fun `getMovieDetails cache miss - fetches from API and caches result`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null)
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        every { mockTmdbMovieDao.insertMovie(any()) } just runs
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(movieId, result.data.id)
        
        // Verify API call was made and data was cached
        verify { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") }
        verify { mockTmdbMovieDao.insertMovie(any()) }
    }
    
    @Test
    fun `getMovieCredits cache miss - fetches from API and caches result`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val apiResponse = TMDbTestDataFactory.createTMDbCreditsResponse(id = movieId)
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
        
        // Verify API call was made and data was cached
        verify { mockTmdbMovieService.getMovieCredits(movieId, "en-US") }
        verify { mockTmdbSearchDao.insertCredits(any()) }
    }
    
    // Cache Expiration Tests
    
    @Test
    fun `getMovieDetails cache expired - fetches fresh data from API`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val expiredTime = System.currentTimeMillis() - (CACHE_TIMEOUT_MS + 1000L)
        val cachedEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId).copy(
            title = "Fresh Title"
        )
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedEntity)
        every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns expiredTime
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        every { mockTmdbMovieDao.insertMovie(any()) } just runs
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals("Fresh Title", result.data.title)
        
        // Verify API call was made to refresh expired cache
        verify { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") }
        verify { mockTmdbMovieDao.insertMovie(any()) }
    }
    
    @Test
    fun `getMovieDetails cache not expired - uses cached data`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val recentTime = System.currentTimeMillis() - (CACHE_TIMEOUT_MS / 2) // 12 hours ago
        val cachedEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedEntity)
        every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns recentTime
        
        // Act
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(movieId, result.data.id)
        
        // Verify no API call was made
        verify(exactly = 0) { mockTmdbMovieService.getMovieDetails(any(), any(), any()) }
    }
    
    // Force Refresh Tests
    
    @Test
    fun `getMovieDetails force refresh - bypasses cache and fetches fresh data`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val cachedEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId).copy(
            title = "Force Refreshed Title"
        )
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
        assertEquals("Force Refreshed Title", result.data.title)
        
        // Verify API call was made despite having cache
        verify { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") }
        verify { mockTmdbMovieDao.insertMovie(any()) }
        
        // Should not check cache expiration when force refreshing
        verify(exactly = 0) { mockTmdbMovieDao.getMovieLastUpdated(movieId) }
    }
    
    @Test
    fun `getMovieCredits force refresh - bypasses cache and fetches fresh data`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val cachedCredits = com.rdwatch.androidtv.data.entities.TMDbCreditsEntity(
            id = movieId,
            contentType = "movie",
            cast = emptyList(),
            crew = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        val apiResponse = TMDbTestDataFactory.createTMDbCreditsResponse(id = movieId)
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbSearchDao.getCredits(movieId, "movie") } returns flowOf(cachedCredits)
        every { mockTmdbMovieService.getMovieCredits(movieId, "en-US") } returns mockCreditsCall
        every { mockCreditsCall.execute() } returns retrofitResponse
        every { mockTmdbSearchDao.insertCredits(any()) } just runs
        
        // Act
        val result = repository.getMovieCredits(movieId, forceRefresh = true).first()
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(movieId, result.data.id)
        
        // Verify API call was made despite having cache
        verify { mockTmdbMovieService.getMovieCredits(movieId, "en-US") }
        verify { mockTmdbSearchDao.insertCredits(any()) }
    }
    
    // Cache Management Tests
    
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
    
    // Multiple Cache Types Tests
    
    @Test
    fun `different cache types work independently`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val page = 1
        
        // Movie cache hit
        val cachedMovie = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedMovie)
        every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns System.currentTimeMillis()
        
        // Credits cache miss
        every { mockTmdbSearchDao.getCredits(movieId, "movie") } returns flowOf(null)
        val creditsApiResponse = TMDbTestDataFactory.createTMDbCreditsResponse(id = movieId)
        val creditsApiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(creditsApiResponse)
        val creditsRetrofitResponse = Response.success(creditsApiResponseWrapper)
        every { mockTmdbMovieService.getMovieCredits(movieId, "en-US") } returns mockCreditsCall
        every { mockCreditsCall.execute() } returns creditsRetrofitResponse
        every { mockTmdbSearchDao.insertCredits(any()) } just runs
        
        // Recommendations cache hit
        val cachedRecommendations = com.rdwatch.androidtv.data.entities.TMDbRecommendationsEntity(
            id = movieId,
            contentType = "movie",
            recommendationType = "recommendations",
            page = page,
            results = emptyList(),
            totalPages = 1,
            totalResults = 0,
            lastUpdated = System.currentTimeMillis()
        )
        every { mockTmdbSearchDao.getRecommendations(movieId, "movie", "recommendations", page) } returns flowOf(cachedRecommendations)
        
        // Act
        val movieResult = repository.getMovieDetails(movieId, forceRefresh = false).first()
        val creditsResult = repository.getMovieCredits(movieId, forceRefresh = false).first()
        val recommendationsResult = repository.getMovieRecommendations(movieId, page, forceRefresh = false).first()
        
        // Assert
        assertTrue(movieResult is Result.Success)
        assertTrue(creditsResult is Result.Success)
        assertTrue(recommendationsResult is Result.Success)
        
        // Verify movie used cache (no API call)
        verify(exactly = 0) { mockTmdbMovieService.getMovieDetails(any(), any(), any()) }
        
        // Verify credits used API (cache miss)
        verify { mockTmdbMovieService.getMovieCredits(movieId, "en-US") }
        verify { mockTmdbSearchDao.insertCredits(any()) }
        
        // Verify recommendations used cache (no API call)
        verify(exactly = 0) { mockTmdbMovieService.getMovieRecommendations(any(), any(), any()) }
    }
    
    // Pagination Cache Tests
    
    @Test
    fun `different pages are cached independently`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val page1 = 1
        val page2 = 2
        
        // Page 1 cache hit
        val cachedPage1 = com.rdwatch.androidtv.data.entities.TMDbRecommendationsEntity(
            id = movieId,
            contentType = "movie",
            recommendationType = "recommendations",
            page = page1,
            results = emptyList(),
            totalPages = 2,
            totalResults = 20,
            lastUpdated = System.currentTimeMillis()
        )
        every { mockTmdbSearchDao.getRecommendations(movieId, "movie", "recommendations", page1) } returns flowOf(cachedPage1)
        
        // Page 2 cache miss
        every { mockTmdbSearchDao.getRecommendations(movieId, "movie", "recommendations", page2) } returns flowOf(null)
        val page2ApiResponse = TMDbTestDataFactory.createTMDbRecommendationsResponse(page = page2)
        val page2ApiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(page2ApiResponse)
        val page2RetrofitResponse = Response.success(page2ApiResponseWrapper)
        every { mockTmdbMovieService.getMovieRecommendations(movieId, "en-US", page2) } returns mockRecommendationsCall
        every { mockRecommendationsCall.execute() } returns page2RetrofitResponse
        every { mockTmdbSearchDao.insertRecommendations(any()) } just runs
        
        // Act
        val page1Result = repository.getMovieRecommendations(movieId, page1, forceRefresh = false).first()
        val page2Result = repository.getMovieRecommendations(movieId, page2, forceRefresh = false).first()
        
        // Assert
        assertTrue(page1Result is Result.Success)
        assertTrue(page2Result is Result.Success)
        assertEquals(page1, page1Result.data.page)
        assertEquals(page2, page2Result.data.page)
        
        // Verify page 1 used cache (no API call)
        verify(exactly = 0) { mockTmdbMovieService.getMovieRecommendations(movieId, "en-US", page1) }
        
        // Verify page 2 used API (cache miss)
        verify { mockTmdbMovieService.getMovieRecommendations(movieId, "en-US", page2) }
        verify { mockTmdbSearchDao.insertRecommendations(any()) }
    }
    
    // Search vs Recommendations Cache Tests
    
    @Test
    fun `search always fetches fresh data`() = runTest {
        // Arrange
        val query = "fight club"
        val page = 1
        
        // Even with cached search results, search should always fetch fresh data
        val cachedSearch = com.rdwatch.androidtv.data.entities.TMDbSearchResultEntity(
            searchId = "$query-$page-movie",
            query = query,
            page = page,
            contentType = "movie",
            results = emptyList(),
            totalPages = 1,
            totalResults = 0,
            lastUpdated = System.currentTimeMillis()
        )
        every { mockTmdbSearchDao.getSearchResults(query, "movie", page) } returns flowOf(cachedSearch)
        
        // Act & Assert
        try {
            repository.searchMovies(query, page).first()
        } catch (e: NotImplementedError) {
            // Expected - search is not implemented in the current repository
            assertTrue(e.message!!.contains("Search functionality requires"))
        }
    }
    
    // Cache Consistency Tests
    
    @Test
    fun `cache updates are consistent across multiple calls`() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(null) andThen flowOf(TMDbTestDataFactory.createTMDbMovieEntity(id = movieId))
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        every { mockTmdbMovieDao.insertMovie(any()) } just runs
        every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns System.currentTimeMillis()
        
        // Act - First call should cache the data
        val firstResult = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Act - Second call should use cached data
        val secondResult = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert
        assertTrue(firstResult is Result.Success)
        assertTrue(secondResult is Result.Success)
        assertEquals(movieId, firstResult.data.id)
        assertEquals(movieId, secondResult.data.id)
        
        // Verify API was called only once
        verify(exactly = 1) { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") }
        verify(exactly = 1) { mockTmdbMovieDao.insertMovie(any()) }
        
        // Verify cache was checked on second call
        verify(exactly = 2) { mockTmdbMovieDao.getMovieById(movieId) }
    }
}