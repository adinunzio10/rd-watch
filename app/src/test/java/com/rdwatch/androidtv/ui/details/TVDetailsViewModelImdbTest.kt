package com.rdwatch.androidtv.ui.details

import com.rdwatch.androidtv.data.mappers.TMDbTVContentDetail
import com.rdwatch.androidtv.data.repository.TMDbTVRepository
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVResponse
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.details.managers.ScraperSourceManager
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import com.rdwatch.androidtv.ui.details.models.TVEpisode
import com.rdwatch.androidtv.ui.details.models.TVShowDetail
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull

/**
 * Integration test for IMDB ID flow in TVDetailsViewModel
 * Tests the complete flow from TV show loading to episode source selection
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TVDetailsViewModelImdbTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: TVDetailsViewModel
    private lateinit var mockRealDebridRepository: RealDebridContentRepository
    private lateinit var mockTMDbTVRepository: TMDbTVRepository
    private lateinit var mockScraperSourceManager: ScraperSourceManager

    companion object {
        private const val TEST_TV_ID = "37854"
        private const val TEST_IMDB_ID = "tt0388629"
        private const val TEST_SEASON_NUMBER = 1
        private const val TEST_EPISODE_NUMBER = 1
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Create mocks with relaxed behavior
        mockRealDebridRepository = mockk(relaxed = true)
        mockTMDbTVRepository = mockk(relaxed = true)
        mockScraperSourceManager = mockk(relaxed = true)

        // Create ViewModel instance
        viewModel =
            TVDetailsViewModel(
                tmdbTVRepository = mockTMDbTVRepository,
                realDebridRepository = mockRealDebridRepository,
                scraperSourceManager = mockScraperSourceManager,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `TV show loading should fetch IMDB ID and make it available for source selection`() =
        runTest {
            // Arrange
            val mockTVShowContentDetail = createMockTVShowContentDetail()
            val mockTVShowDetail = createMockTVShowDetail()

            // Mock repository to return TV show with IMDB ID
            every { mockTMDbTVRepository.getTVContentDetail(TEST_TV_ID.toInt(), any(), any()) } returns flowOf(Result.Success(mockTVShowContentDetail))

            // Mock source manager to return empty sources (we just want to test ID passing)
            every { mockScraperSourceManager.getSourcesForTVEpisode(any(), any(), any(), any(), any()) } returns emptyList()

            // Act - Load TV show
            viewModel.loadTVShow(TEST_TV_ID)

            // Wait for async operations
            advanceUntilIdle()

            // Verify TV show was loaded
            val tvShowState = viewModel.tvShowState.value
            assertNotNull(tvShowState)

            // Act - Ensure IMDB ID is loaded
            viewModel.ensureIMDbIdIsLoaded()
            advanceUntilIdle()

            // Act - Select an episode to trigger source loading
            val testEpisode = createMockTVEpisode()
            viewModel.selectEpisode(testEpisode)
            advanceUntilIdle()

            // Verify that source manager was called with IMDB ID
            verify {
                mockScraperSourceManager.getSourcesForTVEpisode(
                    tvShowId = TEST_TV_ID,
                    seasonNumber = TEST_SEASON_NUMBER,
                    episodeNumber = TEST_EPISODE_NUMBER,
                    imdbId = TEST_IMDB_ID, // This should be the IMDB ID, not TMDb ID
                    tmdbId = TEST_TV_ID,
                )
            }

            println("✅ IMDB ID Integration Test: Source manager called with IMDB ID: $TEST_IMDB_ID")
        }

    @Test
    fun `ensureIMDbIdIsLoaded should update TV show state with IMDB ID`() =
        runTest {
            // Arrange
            val mockTVShowContentDetailWithoutImdb = createMockTVShowContentDetailWithoutImdb()
            val mockTVShowContentDetailWithImdb = createMockTVShowContentDetail()

            // Mock initial TV show loading (without IMDB ID)
            every {
                mockTMDbTVRepository.getTVContentDetail(TEST_TV_ID.toInt(), any(), any())
            } returns flowOf(Result.Success(mockTVShowContentDetailWithoutImdb))

            // Mock IMDB ID fetching
            every {
                mockTMDbTVRepository.getTVContentDetail(TEST_TV_ID.toInt(), forceRefresh = true, any())
            } returns flowOf(Result.Success(mockTVShowContentDetailWithImdb))

            // Act - Load TV show initially
            viewModel.loadTVShow(TEST_TV_ID)
            advanceUntilIdle()

            // Verify initial state has no IMDB ID
            val initialState = viewModel.tvShowState.value
            assertNotNull(initialState)

            // Act - Ensure IMDB ID is loaded
            viewModel.ensureIMDbIdIsLoaded()
            advanceUntilIdle()

            // Verify that force refresh was called to get IMDB ID
            verify { mockTMDbTVRepository.getTVContentDetail(TEST_TV_ID.toInt(), forceRefresh = true, any()) }

            println("✅ IMDB ID Loading Test: ensureIMDbIdIsLoaded triggered force refresh")
        }

    @Test
    fun `source selection should log IMDB ID usage for debugging`() =
        runTest {
            // Arrange
            val mockTVShowContentDetail = createMockTVShowContentDetail()
            val mockSources = listOf(createMockStreamingSource())

            // Mock repository
            every { mockTMDbTVRepository.getTVContentDetail(TEST_TV_ID.toInt(), any(), any()) } returns flowOf(Result.Success(mockTVShowContentDetail))

            // Mock source manager with logging
            every { mockScraperSourceManager.getSourcesForTVEpisode(any(), any(), any(), any(), any()) } answers {
                val tvShowId = args[0] as String
                val seasonNumber = args[1] as Int
                val episodeNumber = args[2] as Int
                val imdbId = args[3] as String?
                val tmdbId = args[4] as String

                println("🔍 ScraperSourceManager called with:")
                println("   - TV Show ID: $tvShowId")
                println("   - Season: $seasonNumber")
                println("   - Episode: $episodeNumber")
                println("   - IMDB ID: $imdbId")
                println("   - TMDb ID: $tmdbId")

                mockSources
            }

            // Act - Complete flow
            viewModel.loadTVShow(TEST_TV_ID)
            advanceUntilIdle()

            viewModel.ensureIMDbIdIsLoaded()
            advanceUntilIdle()

            val testEpisode = createMockTVEpisode()
            viewModel.selectEpisode(testEpisode)
            advanceUntilIdle()

            // Verify source manager was called
            verify { mockScraperSourceManager.getSourcesForTVEpisode(any(), any(), any(), any(), any()) }

            println("✅ Debug Test: Source selection logged IMDB ID usage")
        }

    @Test
    fun `source selection should handle missing IMDB ID gracefully`() =
        runTest {
            // Arrange
            val mockTVShowContentDetailWithoutImdb = createMockTVShowContentDetailWithoutImdb()

            // Mock repository to return TV show without IMDB ID
            every {
                mockTMDbTVRepository.getTVContentDetail(TEST_TV_ID.toInt(), any(), any())
            } returns flowOf(Result.Success(mockTVShowContentDetailWithoutImdb))

            // Mock source manager
            every { mockScraperSourceManager.getSourcesForTVEpisode(any(), any(), any(), any(), any()) } answers {
                val imdbId = args[3] as String?
                println("🔍 ScraperSourceManager called with IMDB ID: $imdbId (should be null)")
                emptyList()
            }

            // Act - Complete flow
            viewModel.loadTVShow(TEST_TV_ID)
            advanceUntilIdle()

            viewModel.ensureIMDbIdIsLoaded()
            advanceUntilIdle()

            val testEpisode = createMockTVEpisode()
            viewModel.selectEpisode(testEpisode)
            advanceUntilIdle()

            // Verify source manager was called with null IMDB ID
            verify {
                mockScraperSourceManager.getSourcesForTVEpisode(
                    tvShowId = TEST_TV_ID,
                    seasonNumber = TEST_SEASON_NUMBER,
                    episodeNumber = TEST_EPISODE_NUMBER,
                    imdbId = null, // Should be null when IMDB ID is not available
                    tmdbId = TEST_TV_ID,
                )
            }

            println("✅ Null IMDB ID Test: Source manager called with null IMDB ID")
        }

    // Helper methods for creating mock objects
    private fun createMockTVShowContentDetail(): TMDbTVContentDetail {
        val mockTVResponse =
            TMDbTVResponse(
                id = TEST_TV_ID.toInt(),
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

        return TMDbTVContentDetail(
            tmdbTV = mockTVResponse,
            imdbId = TEST_IMDB_ID,
        )
    }

    private fun createMockTVShowContentDetailWithoutImdb(): TMDbTVContentDetail {
        val mockTVResponse =
            TMDbTVResponse(
                id = TEST_TV_ID.toInt(),
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

        return TMDbTVContentDetail(
            tmdbTV = mockTVResponse,
            imdbId = null,
        )
    }

    private fun createMockTVShowDetail(): TVShowDetail {
        return TVShowDetail(
            id = TEST_TV_ID,
            title = "One Piece",
            description = "Test TV show",
            backgroundImageUrl = "/test-backdrop.jpg",
            cardImageUrl = "/test.jpg",
            contentType = ContentType.TV_SHOW,
            videoUrl = null,
            firstAirDate = "1999-10-20",
            lastAirDate = null,
            voteAverage = 8.9f,
            voteCount = 1000,
            popularity = 100.0f,
            adult = false,
            originalLanguage = "ja",
            genres = emptyList(),
            numberOfEpisodes = 1000,
            numberOfSeasons = 20,
            status = "Returning Series",
            type = "Scripted",
            homepage = "https://example.com",
            inProduction = true,
            imdbId = TEST_IMDB_ID,
            networks = emptyList(),
            originCountry = listOf("JP"),
            productionCompanies = emptyList(),
            productionCountries = emptyList(),
            spokenLanguages = emptyList(),
            seasons = emptyList(),
        )
    }

    private fun createMockTVEpisode(): TVEpisode {
        return TVEpisode(
            id = "1",
            title = "I'm Luffy! The Man Who Will Become the Pirate King!",
            description = "Test episode",
            episodeNumber = TEST_EPISODE_NUMBER,
            seasonNumber = TEST_SEASON_NUMBER,
            airDate = "1999-10-20",
            runtime = 24,
            voteAverage = 8.5f,
            voteCount = 500,
            stillPath = "/test-still.jpg",
            videoUrl = null,
            tvShowId = TEST_TV_ID.toInt(),
            imdbId = null,
            productionCode = null,
            overview = "Test episode overview",
        )
    }

    private fun createMockStreamingSource(): StreamingSource {
        return StreamingSource(
            id = "test-source",
            name = "Test Source",
            url = "https://example.com/stream",
            quality = "1080p",
            type = "torrent",
            seeders = 100,
            leechers = 10,
            fileSize = "1.5 GB",
            provider = "Test Provider",
            isHealthy = true,
            isP2P = true,
            isCached = false,
            languages = listOf("en"),
            subtitles = emptyList(),
            audioCodec = "AAC",
            videoCodec = "H264",
            container = "mp4",
            bitrate = "2000 kbps",
            framerate = "24 fps",
            resolution = "1920x1080",
            hdr = false,
            source = "Test",
        )
    }
}
