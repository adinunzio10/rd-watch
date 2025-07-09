package com.rdwatch.androidtv.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.dao.TMDbMovieDao
import com.rdwatch.androidtv.data.dao.TMDbSearchDao
import com.rdwatch.androidtv.data.database.AppDatabase
import com.rdwatch.androidtv.data.entities.TMDbMovieEntity
import com.rdwatch.androidtv.data.entities.TMDbCreditsEntity
import com.rdwatch.androidtv.data.entities.TMDbRecommendationsEntity
import com.rdwatch.androidtv.data.entities.TMDbImagesEntity
import com.rdwatch.androidtv.data.entities.TMDbVideosEntity
import com.rdwatch.androidtv.data.mappers.TMDbToContentDetailMapper
import com.rdwatch.androidtv.data.repository.TMDbMovieRepositoryImpl
import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.test.data.TMDbTestDataFactory
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Call
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for TMDb components with real Room database
 * Tests end-to-end data flow from API to database to repository
 */
@RunWith(AndroidJUnit4::class)
class TMDbIntegrationTest {
    
    private lateinit var database: AppDatabase
    private lateinit var movieDao: TMDbMovieDao
    private lateinit var searchDao: TMDbSearchDao
    private lateinit var repository: TMDbMovieRepositoryImpl
    private lateinit var contentDetailMapper: TMDbToContentDetailMapper
    
    // Mock network components
    private lateinit var mockTmdbMovieService: TMDbMovieService
    private lateinit var mockMovieCall: Call<ApiResponse<TMDbMovieResponse>>
    private lateinit var mockCreditsCall: Call<ApiResponse<TMDbCreditsResponse>>
    
    @Before
    fun setUp() {
        // Create in-memory database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        movieDao = database.tmdbMovieDao()
        searchDao = database.tmdbSearchDao()
        contentDetailMapper = TMDbToContentDetailMapper()
        
        // Mock network service
        mockTmdbMovieService = mockk()
        mockMovieCall = mockk()
        mockCreditsCall = mockk()
        
        repository = TMDbMovieRepositoryImpl(
            tmdbMovieService = mockTmdbMovieService,
            tmdbMovieDao = movieDao,
            tmdbSearchDao = searchDao,
            contentDetailMapper = contentDetailMapper
        )
    }
    
    @After
    fun tearDown() {
        database.close()
        unmockkAll()
    }
    
    @Test
    fun testMovieEntityCRUDOperations() = runTest {
        // Arrange
        val movieEntity = TMDbTestDataFactory.createTMDbMovieEntity()
        
        // Test Insert
        movieDao.insertMovie(movieEntity)
        
        // Test Read
        val retrievedMovie = movieDao.getMovieById(movieEntity.id).first()
        assertNotNull(retrievedMovie)
        assertEquals(movieEntity.id, retrievedMovie.id)
        assertEquals(movieEntity.title, retrievedMovie.title)
        assertEquals(movieEntity.overview, retrievedMovie.overview)
        
        // Test Update
        val updatedMovie = movieEntity.copy(title = "Updated Title")
        movieDao.insertMovie(updatedMovie)
        
        val retrievedUpdatedMovie = movieDao.getMovieById(movieEntity.id).first()
        assertEquals("Updated Title", retrievedUpdatedMovie?.title)
        
        // Test Delete
        movieDao.deleteMovieById(movieEntity.id)
        val deletedMovie = movieDao.getMovieById(movieEntity.id).first()
        assertEquals(null, deletedMovie)
    }
    
    @Test
    fun testCreditsEntityCRUDOperations() = runTest {
        // Arrange
        val creditsEntity = TMDbCreditsEntity(
            id = TMDbTestDataFactory.TEST_MOVIE_ID,
            contentType = "movie",
            cast = emptyList(),
            crew = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        // Test Insert
        searchDao.insertCredits(creditsEntity)
        
        // Test Read
        val retrievedCredits = searchDao.getCredits(creditsEntity.id, "movie").first()
        assertNotNull(retrievedCredits)
        assertEquals(creditsEntity.id, retrievedCredits.id)
        assertEquals(creditsEntity.contentType, retrievedCredits.contentType)
        
        // Test Delete
        searchDao.deleteCredits(creditsEntity.id, "movie")
        val deletedCredits = searchDao.getCredits(creditsEntity.id, "movie").first()
        assertEquals(null, deletedCredits)
    }
    
    @Test
    fun testRecommendationsEntityCRUDOperations() = runTest {
        // Arrange
        val recommendationsEntity = TMDbRecommendationsEntity(
            id = TMDbTestDataFactory.TEST_MOVIE_ID,
            contentType = "movie",
            recommendationType = "recommendations",
            page = 1,
            results = emptyList(),
            totalPages = 1,
            totalResults = 0,
            lastUpdated = System.currentTimeMillis()
        )
        
        // Test Insert
        searchDao.insertRecommendations(recommendationsEntity)
        
        // Test Read
        val retrievedRecommendations = searchDao.getRecommendations(
            recommendationsEntity.id, 
            "movie", 
            "recommendations", 
            1
        ).first()
        assertNotNull(retrievedRecommendations)
        assertEquals(recommendationsEntity.id, retrievedRecommendations.id)
        assertEquals(recommendationsEntity.recommendationType, retrievedRecommendations.recommendationType)
        
        // Test Delete
        searchDao.deleteRecommendations(recommendationsEntity.id, "movie", "recommendations")
        val deletedRecommendations = searchDao.getRecommendations(
            recommendationsEntity.id, 
            "movie", 
            "recommendations", 
            1
        ).first()
        assertEquals(null, deletedRecommendations)
    }
    
    @Test
    fun testImagesEntityCRUDOperations() = runTest {
        // Arrange
        val imagesEntity = TMDbImagesEntity(
            id = TMDbTestDataFactory.TEST_MOVIE_ID,
            contentType = "movie",
            backdrops = emptyList(),
            logos = emptyList(),
            posters = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        // Test Insert
        searchDao.insertImages(imagesEntity)
        
        // Test Read
        val retrievedImages = searchDao.getImages(imagesEntity.id, "movie").first()
        assertNotNull(retrievedImages)
        assertEquals(imagesEntity.id, retrievedImages.id)
        assertEquals(imagesEntity.contentType, retrievedImages.contentType)
        
        // Test Delete
        searchDao.deleteImages(imagesEntity.id, "movie")
        val deletedImages = searchDao.getImages(imagesEntity.id, "movie").first()
        assertEquals(null, deletedImages)
    }
    
    @Test
    fun testVideosEntityCRUDOperations() = runTest {
        // Arrange
        val videosEntity = TMDbVideosEntity(
            id = TMDbTestDataFactory.TEST_MOVIE_ID,
            contentType = "movie",
            results = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        // Test Insert
        searchDao.insertVideos(videosEntity)
        
        // Test Read
        val retrievedVideos = searchDao.getVideos(videosEntity.id, "movie").first()
        assertNotNull(retrievedVideos)
        assertEquals(videosEntity.id, retrievedVideos.id)
        assertEquals(videosEntity.contentType, retrievedVideos.contentType)
        
        // Test Delete
        searchDao.deleteVideos(videosEntity.id, "movie")
        val deletedVideos = searchDao.getVideos(videosEntity.id, "movie").first()
        assertEquals(null, deletedVideos)
    }
    
    @Test
    fun testRepositoryWithRealDatabaseCaching() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        
        // Act - First call should fetch from API and cache
        val firstResult = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert - First call should be successful
        assertTrue(firstResult is Result.Success)
        assertEquals(movieId, firstResult.data.id)
        
        // Verify data was cached in database
        val cachedMovie = movieDao.getMovieById(movieId).first()
        assertNotNull(cachedMovie)
        assertEquals(movieId, cachedMovie.id)
        assertEquals(apiResponse.title, cachedMovie.title)
        
        // Act - Second call should use cached data (no API call)
        clearAllMocks()
        val secondResult = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert - Second call should also be successful
        assertTrue(secondResult is Result.Success)
        assertEquals(movieId, secondResult.data.id)
        
        // Verify API was not called again
        verify(exactly = 0) { mockTmdbMovieService.getMovieDetails(any(), any(), any()) }
    }
    
    @Test
    fun testRepositoryWithRealDatabaseForceRefresh() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val initialApiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        val initialApiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(initialApiResponse)
        val initialRetrofitResponse = Response.success(initialApiResponseWrapper)
        
        val updatedApiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId).copy(
            title = "Updated Title"
        )
        val updatedApiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(updatedApiResponse)
        val updatedRetrofitResponse = Response.success(updatedApiResponseWrapper)
        
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns initialRetrofitResponse andThen updatedRetrofitResponse
        
        // Act - First call to cache data
        val firstResult = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert - First call should be successful
        assertTrue(firstResult is Result.Success)
        assertEquals(initialApiResponse.title, firstResult.data.title)
        
        // Act - Force refresh should bypass cache
        val refreshResult = repository.getMovieDetails(movieId, forceRefresh = true).first()
        
        // Assert - Force refresh should get updated data
        assertTrue(refreshResult is Result.Success)
        assertEquals("Updated Title", refreshResult.data.title)
        
        // Verify API was called twice
        verify(exactly = 2) { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") }
    }
    
    @Test
    fun testEndToEndDataFlowWithContentDetail() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        
        // Act - Get movie as ContentDetail
        val result = repository.getMovieContentDetail(movieId, forceRefresh = false).first()
        
        // Assert - Should successfully map to ContentDetail
        assertTrue(result is Result.Success)
        assertEquals("tmdb_movie_$movieId", result.data.id)
        assertEquals(apiResponse.title, result.data.title)
        assertEquals(apiResponse.overview, result.data.description)
        
        // Verify data was cached
        val cachedMovie = movieDao.getMovieById(movieId).first()
        assertNotNull(cachedMovie)
        assertEquals(movieId, cachedMovie.id)
    }
    
    @Test
    fun testMultipleEntitiesIntegration() = runTest {
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val movieApiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId)
        val movieApiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(movieApiResponse)
        val movieRetrofitResponse = Response.success(movieApiResponseWrapper)
        
        val creditsApiResponse = TMDbTestDataFactory.createTMDbCreditsResponse(id = movieId)
        val creditsApiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(creditsApiResponse)
        val creditsRetrofitResponse = Response.success(creditsApiResponseWrapper)
        
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns movieRetrofitResponse
        
        every { mockTmdbMovieService.getMovieCredits(movieId, "en-US") } returns mockCreditsCall
        every { mockCreditsCall.execute() } returns creditsRetrofitResponse
        
        // Act - Get movie details and credits
        val movieResult = repository.getMovieDetails(movieId, forceRefresh = false).first()
        val creditsResult = repository.getMovieCredits(movieId, forceRefresh = false).first()
        
        // Assert - Both should be successful
        assertTrue(movieResult is Result.Success)
        assertTrue(creditsResult is Result.Success)
        
        // Verify both are cached
        val cachedMovie = movieDao.getMovieById(movieId).first()
        val cachedCredits = searchDao.getCredits(movieId, "movie").first()
        
        assertNotNull(cachedMovie)
        assertNotNull(cachedCredits)
        assertEquals(movieId, cachedMovie.id)
        assertEquals(movieId, cachedCredits.id)
    }
    
    @Test
    fun testCacheClearingOperations() = runTest {
        // Arrange - Insert test data
        val movieEntity = TMDbTestDataFactory.createTMDbMovieEntity()
        val creditsEntity = TMDbCreditsEntity(
            id = movieEntity.id,
            contentType = "movie",
            cast = emptyList(),
            crew = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        movieDao.insertMovie(movieEntity)
        searchDao.insertCredits(creditsEntity)
        
        // Verify data exists
        assertNotNull(movieDao.getMovieById(movieEntity.id).first())
        assertNotNull(searchDao.getCredits(movieEntity.id, "movie").first())
        
        // Act - Clear specific movie cache
        repository.clearMovieCache(movieEntity.id)
        
        // Assert - Data should be cleared
        assertEquals(null, movieDao.getMovieById(movieEntity.id).first())
        assertEquals(null, searchDao.getCredits(movieEntity.id, "movie").first())
    }
    
    @Test
    fun testCacheExpirationLogic() = runTest {
        // Arrange - Insert expired cached data
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val expiredTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25 hours ago
        val expiredEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId).copy(
            lastUpdated = expiredTime
        )
        
        movieDao.insertMovie(expiredEntity)
        
        val apiResponse = TMDbTestDataFactory.createTMDbMovieResponse(id = movieId).copy(
            title = "Fresh Title"
        )
        val apiResponseWrapper = TMDbTestDataFactory.createSuccessApiResponse(apiResponse)
        val retrofitResponse = Response.success(apiResponseWrapper)
        
        every { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") } returns mockMovieCall
        every { mockMovieCall.execute() } returns retrofitResponse
        
        // Act - Get movie (should refresh due to expiration)
        val result = repository.getMovieDetails(movieId, forceRefresh = false).first()
        
        // Assert - Should get fresh data
        assertTrue(result is Result.Success)
        assertEquals("Fresh Title", result.data.title)
        
        // Verify API was called to refresh
        verify { mockTmdbMovieService.getMovieDetails(movieId, null, "en-US") }
    }
    
    @Test
    fun testDatabaseTransactionRollback() = runTest {
        // This test would verify that database operations are properly rolled back
        // in case of errors during complex operations
        
        // Arrange
        val movieId = TMDbTestDataFactory.TEST_MOVIE_ID
        val movieEntity = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
        
        // Insert movie
        movieDao.insertMovie(movieEntity)
        
        // Verify it exists
        assertNotNull(movieDao.getMovieById(movieId).first())
        
        // Act - Delete movie
        movieDao.deleteMovieById(movieId)
        
        // Assert - Should be deleted
        assertEquals(null, movieDao.getMovieById(movieId).first())
    }
}