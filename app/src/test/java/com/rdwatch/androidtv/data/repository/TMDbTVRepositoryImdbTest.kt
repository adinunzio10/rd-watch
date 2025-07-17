package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.TMDbSearchDao
import com.rdwatch.androidtv.data.dao.TMDbTVDao
import com.rdwatch.androidtv.data.mappers.TMDbTVContentDetail
import com.rdwatch.androidtv.data.mappers.TMDbToContentDetailMapper
import com.rdwatch.androidtv.network.api.TMDbTVService
import com.rdwatch.androidtv.network.models.tmdb.TMDbExternalIdsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVResponse
import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.repository.base.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for IMDB ID integration in TMDbTVRepositoryImpl
 * Verifies that TV shows properly fetch and integrate IMDB IDs for source scraping
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TMDbTVRepositoryImdbTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: TMDbTVRepositoryImpl
    private lateinit var mockTVService: TMDbTVService
    private lateinit var mockTVDao: TMDbTVDao
    private lateinit var mockSearchDao: TMDbSearchDao
    private lateinit var mockContentDetailMapper: TMDbToContentDetailMapper

    companion object {
        private const val TEST_TV_ID = 37854
        private const val TEST_IMDB_ID = "tt0388629"
        private const val TEST_TMDB_ID = "tmdb_tv_37854"
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Create mocks
        mockTVService = mockk(relaxed = true)
        mockTVDao = mockk(relaxed = true)
        mockSearchDao = mockk(relaxed = true)
        mockContentDetailMapper = mockk(relaxed = true)

        // Create repository instance
        repository =
            TMDbTVRepositoryImpl(
                tmdbTVService = mockTVService,
                tmdbTVDao = mockTVDao,
                tmdbSearchDao = mockSearchDao,
                contentDetailMapper = mockContentDetailMapper,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `getTVContentDetail should fetch and include IMDB ID`() =
        runTest {
            // Arrange
            val mockTVResponse = createMockTVResponse()
            val mockExternalIds = createMockExternalIdsResponse()
            val mockContentDetail = createMockContentDetail()

            // Mock TV details call
            val mockTVCall = mockk<Call<ApiResponse<TMDbTVResponse>>>()
            every { mockTVService.getTVDetails(TEST_TV_ID, any(), any()) } returns mockTVCall
            every { mockTVCall.execute() } returns Response.success(ApiResponse.Success(mockTVResponse))

            // Mock external IDs call
            val mockExternalIdsCall = mockk<Call<ApiResponse<TMDbExternalIdsResponse>>>()
            every { mockTVService.getTVExternalIds(TEST_TV_ID) } returns mockExternalIdsCall
            every { mockExternalIdsCall.execute() } returns Response.success(ApiResponse.Success(mockExternalIds))

            // Mock database operations
            every { mockTVDao.getTVShowById(TEST_TV_ID) } returns flowOf(null)
            every { mockTVDao.insertTVShow(any()) } just Runs

            // Mock content detail mapper
            every { mockContentDetailMapper.mapTVToContentDetail(mockTVResponse, mockExternalIds) } returns mockContentDetail

            // Act
            val result = repository.getTVContentDetail(TEST_TV_ID, forceRefresh = true, language = "en-US").first()

            // Assert
            assertTrue(result is Result.Success)
            assertNotNull(result.data)

            // Verify that external IDs were fetched
            verify { mockTVService.getTVExternalIds(TEST_TV_ID) }

            // Verify that content detail mapper was called with external IDs
            verify { mockContentDetailMapper.mapTVToContentDetail(mockTVResponse, mockExternalIds) }

            // Verify that the returned content detail has IMDB ID
            val contentDetail = result.data as TMDbTVContentDetail
            assertEquals(TEST_IMDB_ID, contentDetail.getImdbId())

            println("✅ IMDB ID Test: Successfully fetched IMDB ID: ${contentDetail.getImdbId()}")
        }

    @Test
    fun `getTVContentDetail should handle external IDs API failure gracefully`() =
        runTest {
            // Arrange
            val mockTVResponse = createMockTVResponse()
            val mockContentDetailWithoutImdb = createMockContentDetailWithoutImdb()

            // Mock TV details call (success)
            val mockTVCall = mockk<Call<ApiResponse<TMDbTVResponse>>>()
            every { mockTVService.getTVDetails(TEST_TV_ID, any(), any()) } returns mockTVCall
            every { mockTVCall.execute() } returns Response.success(ApiResponse.Success(mockTVResponse))

            // Mock external IDs call (failure)
            val mockExternalIdsCall = mockk<Call<ApiResponse<TMDbExternalIdsResponse>>>()
            every { mockTVService.getTVExternalIds(TEST_TV_ID) } returns mockExternalIdsCall
            every { mockExternalIdsCall.execute() } throws RuntimeException("API failure")

            // Mock database operations
            every { mockTVDao.getTVShowById(TEST_TV_ID) } returns flowOf(null)
            every { mockTVDao.insertTVShow(any()) } just Runs

            // Mock content detail mapper for fallback
            every { mockContentDetailMapper.mapTVToContentDetail(mockTVResponse, null) } returns mockContentDetailWithoutImdb

            // Act
            val result = repository.getTVContentDetail(TEST_TV_ID, forceRefresh = true, language = "en-US").first()

            // Assert
            assertTrue(result is Result.Success)
            assertNotNull(result.data)

            // Verify that external IDs API was attempted
            verify { mockTVService.getTVExternalIds(TEST_TV_ID) }

            // Verify that fallback was used (mapper called with null external IDs)
            verify { mockContentDetailMapper.mapTVToContentDetail(mockTVResponse, null) }

            println("✅ Fallback Test: Successfully handled external IDs API failure")
        }

    // Helper methods for creating mock objects
    private fun createMockTVResponse(): TMDbTVResponse {
        return TMDbTVResponse(
            id = TEST_TV_ID,
            name = "One Piece",
            originalName = "One Piece",
            overview = "Test TV show",
            firstAirDate = "1999-10-20",
            lastAirDate = null,
            posterPath = "/test.jpg",
            backdropPath = "/test-backdrop.jpg",
            voteAverage = 8.9,
            voteCount = 1000,
            popularity = 100.0,
            originalLanguage = "ja",
            adult = false,
            numberOfEpisodes = 1000,
            numberOfSeasons = 20,
            status = "Returning Series",
            type = "Scripted",
            homepage = "https://example.com",
            inProduction = true,
            genres = emptyList(),
            networks = emptyList(),
            productionCompanies = emptyList(),
            productionCountries = emptyList(),
            spokenLanguages = emptyList(),
            originCountry = listOf("JP"),
            languages = listOf("ja"),
            createdBy = emptyList(),
            episodeRunTime = listOf(24),
            seasons = emptyList(),
            lastEpisodeToAir = null,
            nextEpisodeToAir = null,
            tagline = null,
        )
    }

    private fun createMockExternalIdsResponse(): TMDbExternalIdsResponse {
        return TMDbExternalIdsResponse(
            id = TEST_TV_ID,
            imdbId = TEST_IMDB_ID,
            freebaseId = null,
            freebaseMid = null,
            tvdbId = null,
            tvrageId = null,
            wikidataId = null,
            facebookId = null,
            instagramId = null,
            twitterId = null,
        )
    }

    private fun createMockContentDetail(): TMDbTVContentDetail {
        return TMDbTVContentDetail(
            tmdbTV = createMockTVResponse(),
            imdbId = TEST_IMDB_ID,
        )
    }

    private fun createMockContentDetailWithoutImdb(): TMDbTVContentDetail {
        return TMDbTVContentDetail(
            tmdbTV = createMockTVResponse(),
            imdbId = null,
        )
    }
}
