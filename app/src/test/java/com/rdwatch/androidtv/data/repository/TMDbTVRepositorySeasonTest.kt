package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.TMDbTVDao
import com.rdwatch.androidtv.data.dao.TMDbSearchDao
import com.rdwatch.androidtv.data.entities.TMDbTVEntity
import com.rdwatch.androidtv.data.mappers.toEntity
import com.rdwatch.androidtv.data.mappers.TMDbToContentDetailMapper
import com.rdwatch.androidtv.network.api.TMDbTVService
import com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVResponse
import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.repository.base.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Comprehensive tests for season loading functionality in TMDbTVRepositoryImpl
 * Tests caching strategy, data validation, error handling, and state synchronization
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TMDbTVRepositorySeasonTest {
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var repository: TMDbTVRepositoryImpl
    private lateinit var mockTmdbTVService: TMDbTVService
    private lateinit var mockTmdbTVDao: TMDbTVDao
    private lateinit var mockTmdbSearchDao: TMDbSearchDao
    private lateinit var mockContentDetailMapper: TMDbToContentDetailMapper
    
    private lateinit var mockSeasonCall: Call<ApiResponse<TMDbSeasonResponse>>
    
    companion object {
        private const val TEST_TV_ID = 12345
        private const val TEST_SEASON_NUMBER = 1
        private const val TEST_LANGUAGE = "en-US"
    }
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Create mocks
        mockTmdbTVService = mockk()
        mockTmdbTVDao = mockk()
        mockTmdbSearchDao = mockk()
        mockContentDetailMapper = mockk()
        mockSeasonCall = mockk()
        
        // Setup common mock behaviors
        every { mockSeasonCall.cancel() } just Runs
        
        // Setup repository
        repository = TMDbTVRepositoryImpl(
            tmdbTVService = mockTmdbTVService,
            tmdbTVDao = mockTmdbTVDao,
            tmdbSearchDao = mockTmdbSearchDao,
            contentDetailMapper = mockContentDetailMapper
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Test
    fun `getSeasonDetails - cache hit with valid data returns cached season`() = runTest {
        // Given: Valid cached season data
        val cachedSeason = createValidSeasonResponse()
        val existingTV = createTVEntityWithSeason(cachedSeason)
        
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returns flowOf(existingTV)
        
        // When: Getting season details
        val result = withTimeoutOrNull(5000) {
            repository.getSeasonDetails(
                tvId = TEST_TV_ID,
                seasonNumber = TEST_SEASON_NUMBER,
                forceRefresh = false,
                language = TEST_LANGUAGE
            ).first { it is Result.Success } // Wait for success result
        } ?: throw RuntimeException("Flow collection timed out")
        
        // Then: Returns cached data without API call
        assertTrue(result is Result.Success)
        assertEquals(cachedSeason.id, (result as Result.Success).data.id)
        assertEquals(cachedSeason.episodes.size, result.data.episodes.size)
        
        // Verify no API call was made
        verify(exactly = 0) { mockTmdbTVService.getSeasonDetails(any(), any(), any()) }
    }
    
    @Test
    fun `getSeasonDetails - cache miss triggers API call and saves result`() = runTest {
        // Given: No cached data initially, updated data after API call
        val apiSeason = createValidSeasonResponse()
        val apiResponse = createSuccessApiResponse(apiSeason)
        val updatedTV = createTVEntityWithSeason(apiSeason)
        
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returnsMany listOf(
            flowOf(null),       // First call returns no data
            flowOf(updatedTV)   // After API call, returns updated data
        )
        
        every { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) } returns mockSeasonCall
        every { mockSeasonCall.enqueue(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val callback = it.invocation.args[0] as retrofit2.Callback<ApiResponse<TMDbSeasonResponse>>
            callback.onResponse(mockSeasonCall, Response.success(apiResponse))
        }
        
        coEvery { mockTmdbTVDao.getTVShowByIdSuspend(TEST_TV_ID) } returns null
        coEvery { mockTmdbTVDao.insertTVShow(any()) } just Runs
        
        // When: Getting season details
        val results = repository.getSeasonDetails(
            tvId = TEST_TV_ID,
            seasonNumber = TEST_SEASON_NUMBER,
            forceRefresh = false,
            language = TEST_LANGUAGE
        ).take(3).toList() // Collect first 3 emissions (Loading, Loading, Success)
        
        // Then: Makes API call and returns data
        val successResult = results.last()
        assertTrue(successResult is Result.Success)
        assertEquals(apiSeason.id, (successResult as Result.Success).data!!.id)
        
        // Verify API call was made
        verify(exactly = 1) { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) }
    }
    
    @Test
    fun `getSeasonDetails - invalid cached data triggers refresh`() = runTest {
        // Given: Invalid cached season (id = 0, no episodes)
        val invalidSeason = TMDbSeasonResponse(
            id = 0,
            seasonNumber = TEST_SEASON_NUMBER,
            name = "Season 1",
            overview = "",
            posterPath = null,
            airDate = null,
            episodeCount = 10,
            episodes = emptyList(),
            voteAverage = 0.0
        )
        val existingTV = createTVEntityWithSeason(invalidSeason)
        
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returns flowOf(existingTV)
        
        val validApiSeason = createValidSeasonResponse()
        val apiResponse = createSuccessApiResponse(validApiSeason)
        
        every { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) } returns mockSeasonCall
        every { mockSeasonCall.enqueue(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val callback = it.invocation.args[0] as retrofit2.Callback<ApiResponse<TMDbSeasonResponse>>
            callback.onResponse(mockSeasonCall, Response.success(apiResponse))
        }
        
        coEvery { mockTmdbTVDao.getTVShowByIdSuspend(TEST_TV_ID) } returns existingTV
        coEvery { mockTmdbTVDao.insertTVShow(any()) } just Runs
        
        // Mock the DAO to return updated data after API call
        val updatedTV = createTVEntityWithSeason(validApiSeason)
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returnsMany listOf(
            flowOf(existingTV), // First call returns invalid data
            flowOf(updatedTV)   // After API call, returns updated data
        )
        
        // When: Getting season details
        val results = repository.getSeasonDetails(
            tvId = TEST_TV_ID,
            seasonNumber = TEST_SEASON_NUMBER,
            forceRefresh = false,
            language = TEST_LANGUAGE
        ).take(3).toList() // Collect first 3 emissions
        
        // Then: API call is made despite having cached data
        val successResult = results.last()
        assertTrue(successResult is Result.Success)
        assertEquals(validApiSeason.id, (successResult as Result.Success).data!!.id)
        
        verify(exactly = 1) { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) }
    }
    
    @Test
    fun `getSeasonDetails - stale cached data triggers refresh`() = runTest {
        // Given: Stale cached season (claims episodes but has none)
        val staleSeason = TMDbSeasonResponse(
            id = 123,
            seasonNumber = TEST_SEASON_NUMBER,
            name = "Season 1",
            overview = "",
            posterPath = null,
            airDate = null,
            episodeCount = 10,
            episodes = emptyList(), // No episodes but claims 10
            voteAverage = 8.0
        )
        val existingTV = createTVEntityWithSeason(staleSeason)
        
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returns flowOf(existingTV)
        
        val freshApiSeason = createValidSeasonResponse()
        val apiResponse = createSuccessApiResponse(freshApiSeason)
        
        every { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) } returns mockSeasonCall
        every { mockSeasonCall.enqueue(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val callback = it.invocation.args[0] as retrofit2.Callback<ApiResponse<TMDbSeasonResponse>>
            callback.onResponse(mockSeasonCall, Response.success(apiResponse))
        }
        
        coEvery { mockTmdbTVDao.getTVShowByIdSuspend(TEST_TV_ID) } returns existingTV
        coEvery { mockTmdbTVDao.insertTVShow(any()) } just Runs
        
        // Mock the DAO to return updated data after API call
        val updatedTV = createTVEntityWithSeason(freshApiSeason)
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returnsMany listOf(
            flowOf(existingTV), // First call returns stale data
            flowOf(updatedTV)   // After API call, returns updated data
        )
        
        // When: Getting season details
        val results = repository.getSeasonDetails(
            tvId = TEST_TV_ID,
            seasonNumber = TEST_SEASON_NUMBER,
            forceRefresh = false,
            language = TEST_LANGUAGE
        ).take(3).toList() // Collect first 3 emissions
        
        // Then: API call is made to refresh stale data
        val successResult = results.last()
        assertTrue(successResult is Result.Success)
        assertEquals(freshApiSeason.episodes.size, (successResult as Result.Success).data!!.episodes.size)
        
        verify(exactly = 1) { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) }
    }
    
    @Test
    fun `getSeasonDetails - force refresh bypasses cache`() = runTest {
        // Given: Valid cached data
        val cachedSeason = createValidSeasonResponse()
        val existingTV = createTVEntityWithSeason(cachedSeason)
        
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returns flowOf(existingTV)
        
        val apiSeason = createValidSeasonResponse().copy(id = 999) // Different ID
        val apiResponse = createSuccessApiResponse(apiSeason)
        
        every { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) } returns mockSeasonCall
        every { mockSeasonCall.enqueue(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val callback = it.invocation.args[0] as retrofit2.Callback<ApiResponse<TMDbSeasonResponse>>
            callback.onResponse(mockSeasonCall, Response.success(apiResponse))
        }
        
        coEvery { mockTmdbTVDao.getTVShowByIdSuspend(TEST_TV_ID) } returns existingTV
        coEvery { mockTmdbTVDao.insertTVShow(any()) } just Runs
        
        // Mock the DAO to return updated data after API call
        val updatedTV = createTVEntityWithSeason(apiSeason)
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returnsMany listOf(
            flowOf(existingTV), // First call returns cached data
            flowOf(updatedTV)   // After API call, returns updated data
        )
        
        // When: Force refreshing season details
        val results = repository.getSeasonDetails(
            tvId = TEST_TV_ID,
            seasonNumber = TEST_SEASON_NUMBER,
            forceRefresh = true, // Force refresh
            language = TEST_LANGUAGE
        ).take(3).toList() // Collect first 3 emissions
        
        // Then: API call is made even with valid cache
        val successResult = results.last()
        assertTrue(successResult is Result.Success)
        assertEquals(999, (successResult as Result.Success).data!!.id) // Should return API data, not cached
        
        verify(exactly = 1) { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) }
    }
    
    @Test
    fun `getSeasonDetails - API error returns cached data when available`() = runTest {
        // Given: Valid cached data but API failure
        val cachedSeason = createValidSeasonResponse()
        val existingTV = createTVEntityWithSeason(cachedSeason)
        
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returns flowOf(existingTV)
        every { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) } returns mockSeasonCall
        every { mockSeasonCall.enqueue(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val callback = it.invocation.args[0] as retrofit2.Callback<ApiResponse<TMDbSeasonResponse>>
            callback.onFailure(mockSeasonCall, RuntimeException("Network error"))
        }
        
        // When: Getting season details with API failure
        val result = withTimeoutOrNull(5000) {
            repository.getSeasonDetails(
                tvId = TEST_TV_ID,
                seasonNumber = TEST_SEASON_NUMBER,
                forceRefresh = true, // Force refresh to trigger API call
                language = TEST_LANGUAGE
            ).first { it is Result.Success } // Wait for success result (fallback to cache)
        } ?: throw RuntimeException("Flow collection timed out")
        
        // Then: Returns cached data as fallback
        assertTrue(result is Result.Success)
        assertEquals(cachedSeason.id, (result as Result.Success).data.id)
    }
    
    @Test
    fun `updateSeasonInDatabase - updates existing season correctly`() = runTest {
        // Given: Existing TV show with season data
        val existingSeason = createValidSeasonResponse()
        val existingTV = createTVEntityWithSeason(existingSeason)
        
        val updatedSeason = existingSeason.copy(
            episodeCount = 12,
            episodes = createEpisodeList(12)
        )
        
        coEvery { mockTmdbTVDao.getTVShowByIdSuspend(TEST_TV_ID) } returns existingTV
        coEvery { mockTmdbTVDao.insertTVShow(any()) } just Runs
        
        // When: Updating season in database (testing the private method via public interface)
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returns flowOf(null)
        every { mockTmdbTVService.getSeasonDetails(TEST_TV_ID, TEST_SEASON_NUMBER, TEST_LANGUAGE) } returns mockSeasonCall
        every { mockSeasonCall.enqueue(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val callback = it.invocation.args[0] as retrofit2.Callback<ApiResponse<TMDbSeasonResponse>>
            callback.onResponse(mockSeasonCall, Response.success(createSuccessApiResponse(updatedSeason)))
        }
        
        // Mock the DAO to return updated data after API call
        val updatedTV = createTVEntityWithSeason(updatedSeason)
        every { mockTmdbTVDao.getTVShowById(TEST_TV_ID) } returnsMany listOf(
            flowOf(null),       // First call returns no data
            flowOf(updatedTV)   // After API call, returns updated data
        )
        
        val results = repository.getSeasonDetails(
            tvId = TEST_TV_ID,
            seasonNumber = TEST_SEASON_NUMBER,
            forceRefresh = false,
            language = TEST_LANGUAGE
        ).take(3).toList() // Collect first 3 emissions
        
        // Then: Database is updated with new season data
        val successResult = results.last()
        assertTrue(successResult is Result.Success)
        assertEquals(12, (successResult as Result.Success).data!!.episodeCount)
        assertEquals(12, successResult.data!!.episodes.size)
        
        coVerify(exactly = 1) { mockTmdbTVDao.insertTVShow(any()) }
    }
    
    @Test
    fun `cache invalidation - validateAndInvalidateAllSeasons clears invalid seasons`() = runTest {
        // Given: TV show with both valid and invalid seasons
        val validSeason = createValidSeasonResponse()
        val invalidSeason = TMDbSeasonResponse(
            id = 0, // Invalid ID
            seasonNumber = 2,
            name = "Season 2",
            overview = "",
            posterPath = null,
            airDate = null,
            episodeCount = 8,
            episodes = emptyList(),
            voteAverage = 0.0
        )
        
        val tvResponse = TMDbTVResponse(
            id = TEST_TV_ID,
            name = "Test Show",
            overview = "",
            posterPath = null,
            backdropPath = null,
            firstAirDate = "2023-01-01",
            lastAirDate = null,
            status = "Returning Series",
            type = "Scripted",
            genres = emptyList(),
            spokenLanguages = emptyList(),
            originCountry = emptyList(),
            numberOfSeasons = 2,
            numberOfEpisodes = 18,
            seasons = listOf(validSeason, invalidSeason),
            networks = emptyList(),
            productionCompanies = emptyList(),
            createdBy = emptyList(),
            voteAverage = 8.0,
            voteCount = 100,
            popularity = 75.0,
            adult = false,
            homepage = null,
            tagline = null,
            inProduction = true,
            episodeRunTime = listOf(45),
            lastEpisodeToAir = null,
            nextEpisodeToAir = null
        )
        
        val existingTV = tvResponse.toEntity()
        
        coEvery { mockTmdbTVDao.getTVShowByIdSuspend(TEST_TV_ID) } returns existingTV
        coEvery { mockTmdbTVDao.insertTVShow(any()) } just Runs
        
        // When: Validating and invalidating seasons
        repository.validateAndInvalidateAllSeasons(TEST_TV_ID)
        
        // Then: Invalid season should be cleared (season 2 should have empty episodes)
        coVerify(exactly = 1) { mockTmdbTVDao.insertTVShow(any()) }
    }
    
    // Helper methods for creating test data
    
    private fun createValidSeasonResponse(episodeCount: Int = 10): TMDbSeasonResponse {
        return TMDbSeasonResponse(
            id = 123,
            seasonNumber = TEST_SEASON_NUMBER,
            name = "Season 1",
            overview = "First season of the show",
            posterPath = "/season1.jpg",
            airDate = "2023-01-01",
            episodeCount = episodeCount,
            episodes = createEpisodeList(episodeCount),
            voteAverage = 8.5
        )
    }
    
    private fun createEpisodeList(count: Int): List<TMDbEpisodeResponse> {
        return (1..count).map { episodeNumber ->
            TMDbEpisodeResponse(
                id = episodeNumber * 100,
                seasonNumber = TEST_SEASON_NUMBER,
                episodeNumber = episodeNumber,
                name = "Episode $episodeNumber",
                overview = "Episode $episodeNumber description",
                stillPath = "/episode$episodeNumber.jpg",
                airDate = "2023-01-${episodeNumber.toString().padStart(2, '0')}",
                runtime = 45,
                voteAverage = 8.0,
                voteCount = 50
            )
        }
    }
    
    private fun createTVEntityWithSeason(season: TMDbSeasonResponse): TMDbTVEntity {
        val tvResponse = TMDbTVResponse(
            id = TEST_TV_ID,
            name = "Test Show",
            overview = "Test show description",
            posterPath = "/poster.jpg",
            backdropPath = "/backdrop.jpg",
            firstAirDate = "2023-01-01",
            lastAirDate = null,
            status = "Returning Series",
            type = "Scripted",
            genres = emptyList(),
            spokenLanguages = emptyList(),
            originCountry = emptyList(),
            numberOfSeasons = 1,
            numberOfEpisodes = season.episodeCount,
            seasons = listOf(season),
            networks = emptyList(),
            productionCompanies = emptyList(),
            createdBy = emptyList(),
            voteAverage = 8.0,
            voteCount = 100,
            popularity = 75.0,
            adult = false,
            homepage = null,
            tagline = null,
            inProduction = true,
            episodeRunTime = listOf(45),
            lastEpisodeToAir = null,
            nextEpisodeToAir = null
        )
        
        return tvResponse.toEntity()
    }
    
    private fun createSuccessApiResponse(season: TMDbSeasonResponse): ApiResponse<TMDbSeasonResponse> {
        return ApiResponse.Success(season)
    }
}