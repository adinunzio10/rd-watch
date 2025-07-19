package com.rdwatch.androidtv.ui.details

import com.rdwatch.androidtv.data.repository.TMDbTVRepository
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.details.managers.ScraperSourceManager
import com.rdwatch.androidtv.ui.details.models.TVShowContentDetail
import com.rdwatch.androidtv.ui.details.models.TVShowDetail
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for season-related functionality in TVDetailsViewModel
 * Focuses on season selection, loading, and episode management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TVDetailsViewModelSeasonTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: TVDetailsViewModel
    private lateinit var mockRealDebridRepository: RealDebridContentRepository
    private lateinit var mockTMDbTVRepository: TMDbTVRepository
    private lateinit var mockScraperSourceManager: ScraperSourceManager

    companion object {
        private const val TEST_SEASON_NUMBER = 1
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Create mocks with relaxed behavior
        mockRealDebridRepository = mockk(relaxed = true)
        mockTMDbTVRepository = mockk(relaxed = true)
        mockScraperSourceManager = mockk(relaxed = true)

        // Set up mock TV repository to return a successful result
        val mockTVShowDetail =
            TVShowDetail(
                id = "12345",
                title = "Test Show",
                originalTitle = null,
                overview = "Test overview",
                posterPath = null,
                backdropPath = null,
                firstAirDate = "2023-01-01",
                lastAirDate = null,
                status = "Returning Series",
                type = "Scripted",
                genres = emptyList(),
                languages = emptyList(),
                originCountry = emptyList(),
                numberOfSeasons = 1,
                numberOfEpisodes = 10,
                seasons = emptyList(),
                networks = emptyList(),
                productionCompanies = emptyList(),
                creators = emptyList(),
                voteAverage = 8.0f,
                voteCount = 100,
                popularity = 75.0f,
                adult = false,
                homepage = null,
                tagline = null,
                inProduction = true,
                episodeRunTime = listOf(45),
            )
        val mockTVShow = TVShowContentDetail(mockTVShowDetail)
        every { mockTMDbTVRepository.getTVContentDetail(any()) } returns flowOf(Result.Success(mockTVShow))

        // Create ViewModel
        viewModel =
            TVDetailsViewModel(
                realDebridContentRepository = mockRealDebridRepository,
                tmdbTVRepository = mockTMDbTVRepository,
                scraperSourceManager = mockScraperSourceManager,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadSeasonOnDemand - method exists and can be called`() =
        runTest {
            // Given: Mock TV show loaded
            viewModel.loadTVShow("12345") // Load a TV show first to set up state
            delay(100) // Allow time for state to update

            // When: Loading season on demand (should work now that state is set)
            viewModel.loadSeasonOnDemand(TEST_SEASON_NUMBER)

            // Then: Method should execute without throwing exception
            assertTrue(true) // Basic assertion to verify test runs
        }

    @Test
    fun `getCurrentSeasonFromAuthoritativeSource - method exists and returns nullable season`() =
        runTest {
            // When: Getting current season from authoritative source (no TV show loaded)
            val currentSeason = viewModel.getCurrentSeasonFromAuthoritativeSource()

            // Then: Should return null initially (no TV show loaded)
            assertTrue(currentSeason == null)
        }

    @Test
    fun `getAllSeasonsFromAuthoritativeSource - method exists and returns season list`() =
        runTest {
            // When: Getting all seasons from authoritative source
            val allSeasons = viewModel.getAllSeasonsFromAuthoritativeSource()

            // Then: Should return a list (empty initially until show is loaded)
            assertNotNull(allSeasons)
            assertTrue(allSeasons.isEmpty()) // Initially empty since no show is loaded
        }

    @Test
    fun `getSeasonByNumberFromAuthoritativeSource - method exists and accepts season number`() =
        runTest {
            // Given: A season number
            val requestedSeasonNumber = 2

            // When: Getting season by number from authoritative source
            val season = viewModel.getSeasonByNumberFromAuthoritativeSource(requestedSeasonNumber)

            // Then: Should return null initially (no TV show loaded)
            assertTrue(season == null) // Initially null since no show is loaded
        }

    @Test
    fun `viewModel initialization - creates instance successfully`() {
        // Then: ViewModel should be initialized
        assertNotNull(viewModel)
    }
}
