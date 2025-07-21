package com.rdwatch.androidtv.ui.library

import com.rdwatch.androidtv.data.entities.LibraryEntity
import com.rdwatch.androidtv.data.repository.LibraryRepository
import com.rdwatch.androidtv.data.repository.UserRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.details.models.ContentType
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for LibraryViewModel
 * Tests state management, filtering, search, CRUD operations, and error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: LibraryViewModel
    private lateinit var mockLibraryRepository: LibraryRepository
    private lateinit var mockUserRepository: UserRepository

    private val testUserId = 1L
    private val testLibraryEntity =
        LibraryEntity(
            libraryId = 1L,
            userId = testUserId,
            contentId = "test_content_123",
            contentType = "MOVIE",
            title = "Test Movie",
            description = "Test Description",
            thumbnailUrl = "https://example.com/image.jpg",
            isFavorite = false,
            isDownloaded = false,
            filePath = null,
            fileSizeBytes = null,
            addedAt = Date(),
            updatedAt = Date(),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockLibraryRepository = mockk()
        mockUserRepository = mockk()

        // Mock user repository to return default user ID
        coEvery { mockUserRepository.getDefaultUserId() } returns testUserId

        // Mock empty library by default
        every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(emptyList()))

        // Mock statistics
        coEvery { mockLibraryRepository.getLibraryCountByUser(testUserId) } returns Result.Success(0)
        coEvery { mockLibraryRepository.getFavoritesCountByUser(testUserId) } returns Result.Success(0)
        coEvery { mockLibraryRepository.getTotalDownloadedSizeByUser(testUserId) } returns Result.Success(0L)
        coEvery { mockLibraryRepository.getContentTypesByUser(testUserId) } returns Result.Success(emptyList())

        viewModel = LibraryViewModel(mockLibraryRepository, mockUserRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `viewModel initializes with correct default state`() =
        runTest {
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(testUserId, uiState.userId)
            assertEquals(null, uiState.selectedContentType)
            assertFalse(uiState.showFavoritesOnly)
            assertFalse(uiState.showDownloadsOnly)
            assertEquals("", uiState.searchQuery)
            assertEquals(LibrarySortBy.RECENTLY_ADDED, uiState.sortBy)
        }

    @Test
    fun `loadLibraryContent emits loading then success`() =
        runTest {
            // Given
            val libraryItems = listOf(testLibraryEntity)
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(libraryItems))

            // When
            viewModel.loadLibraryContent()
            advanceUntilIdle()

            // Then
            val contentState = viewModel.libraryContent.value
            assertTrue(contentState is UiState.Success)
            assertEquals(libraryItems, contentState.data)
            assertEquals(1, viewModel.uiState.value.itemCount)
        }

    @Test
    fun `loadLibraryContent handles error`() =
        runTest {
            // Given
            val exception = RuntimeException("Network error")
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Error(exception))

            // When
            viewModel.loadLibraryContent()
            advanceUntilIdle()

            // Then
            val contentState = viewModel.libraryContent.value
            assertTrue(contentState is UiState.Error)
            assertEquals("Failed to load library content: Network error", contentState.message)
        }

    @Test
    fun `setContentTypeFilter updates state and reloads content`() =
        runTest {
            // Given
            val contentType = ContentType.TV_SHOW
            val filteredItems = listOf(testLibraryEntity.copy(contentType = "TV_SHOW"))
            every { mockLibraryRepository.getLibraryByUserAndType(testUserId, "TV_SHOW") } returns flowOf(Result.Success(filteredItems))

            // When
            viewModel.setContentTypeFilter(contentType)
            advanceUntilIdle()

            // Then
            val uiState = viewModel.uiState.value
            assertEquals(contentType, uiState.selectedContentType)

            val contentState = viewModel.libraryContent.value
            assertTrue(contentState is UiState.Success)
            assertEquals(filteredItems, contentState.data)
        }

    @Test
    fun `toggleFavoritesOnly updates state and filters content`() =
        runTest {
            // Given
            val favoriteItems = listOf(testLibraryEntity.copy(isFavorite = true))
            every { mockLibraryRepository.getFavoritesByUser(testUserId) } returns flowOf(Result.Success(favoriteItems))

            // When
            viewModel.toggleFavoritesOnly()
            advanceUntilIdle()

            // Then
            val uiState = viewModel.uiState.value
            assertTrue(uiState.showFavoritesOnly)

            val contentState = viewModel.libraryContent.value
            assertTrue(contentState is UiState.Success)
            assertEquals(favoriteItems, contentState.data)
        }

    @Test
    fun `toggleDownloadsOnly updates state and filters content`() =
        runTest {
            // Given
            val downloadedItems = listOf(testLibraryEntity.copy(isDownloaded = true, filePath = "/path"))
            every { mockLibraryRepository.getDownloadedContentByUser(testUserId) } returns flowOf(Result.Success(downloadedItems))

            // When
            viewModel.toggleDownloadsOnly()
            advanceUntilIdle()

            // Then
            val uiState = viewModel.uiState.value
            assertTrue(uiState.showDownloadsOnly)

            val contentState = viewModel.libraryContent.value
            assertTrue(contentState is UiState.Success)
            assertEquals(downloadedItems, contentState.data)
        }

    @Test
    fun `setSearchQuery updates state and searches content`() =
        runTest {
            // Given
            val query = "test"
            val searchResults = listOf(testLibraryEntity)
            every { mockLibraryRepository.searchLibrary(testUserId, query) } returns flowOf(Result.Success(searchResults))

            // When
            viewModel.setSearchQuery(query)
            advanceUntilIdle()

            // Then
            val uiState = viewModel.uiState.value
            assertEquals(query, uiState.searchQuery)

            val contentState = viewModel.libraryContent.value
            assertTrue(contentState is UiState.Success)
            assertEquals(searchResults, contentState.data)
        }

    @Test
    fun `setSearchQuery does not search for short queries`() =
        runTest {
            // Given
            val shortQuery = "a"

            // When
            viewModel.setSearchQuery(shortQuery)
            advanceUntilIdle()

            // Then
            val uiState = viewModel.uiState.value
            assertEquals(shortQuery, uiState.searchQuery)

            // Verify search was not called
            verify(exactly = 0) { mockLibraryRepository.searchLibrary(any(), any()) }
        }

    @Test
    fun `setSortBy updates state and sorts content`() =
        runTest {
            // Given
            val sortBy = LibrarySortBy.ALPHABETICAL
            val libraryItems = listOf(testLibraryEntity)
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(libraryItems))

            // When
            viewModel.setSortBy(sortBy)
            advanceUntilIdle()

            // Then
            val uiState = viewModel.uiState.value
            assertEquals(sortBy, uiState.sortBy)
        }

    @Test
    fun `clearFilters resets all filters and reloads content`() =
        runTest {
            // Given - Set some filters first
            viewModel.setContentTypeFilter(ContentType.MOVIE)
            viewModel.toggleFavoritesOnly()
            viewModel.setSearchQuery("test")
            advanceUntilIdle()

            val libraryItems = listOf(testLibraryEntity)
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(libraryItems))

            // When
            viewModel.clearFilters()
            advanceUntilIdle()

            // Then
            val uiState = viewModel.uiState.value
            assertEquals(null, uiState.selectedContentType)
            assertFalse(uiState.showFavoritesOnly)
            assertFalse(uiState.showDownloadsOnly)
            assertEquals("", uiState.searchQuery)
        }

    @Test
    fun `addToLibrary adds item successfully`() =
        runTest {
            // Given
            val contentId = "new_content_123"
            val contentType = ContentType.MOVIE
            val title = "New Movie"
            coEvery { mockLibraryRepository.addToLibrary(any<LibraryEntity>()) } returns Result.Success(Unit)
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(listOf(testLibraryEntity)))

            // When
            viewModel.addToLibrary(contentId, contentType, title)
            advanceUntilIdle()

            // Then
            coVerify { mockLibraryRepository.addToLibrary(any<LibraryEntity>()) }
            verify { mockLibraryRepository.getLibraryByUser(testUserId) }
        }

    @Test
    fun `addToLibrary handles error`() =
        runTest {
            // Given
            val contentId = "new_content_123"
            val contentType = ContentType.MOVIE
            val title = "New Movie"
            val exception = RuntimeException("Insert failed")
            coEvery { mockLibraryRepository.addToLibrary(any<LibraryEntity>()) } returns Result.Error(exception)

            // When
            viewModel.addToLibrary(contentId, contentType, title)
            advanceUntilIdle()

            // Then
            val uiState = viewModel.uiState.value
            assertEquals("Failed to add to library: Insert failed", uiState.error)
        }

    @Test
    fun `removeFromLibrary removes item successfully`() =
        runTest {
            // Given
            val contentId = "content_to_remove"
            coEvery { mockLibraryRepository.removeFromLibrary(testUserId, contentId) } returns Result.Success(Unit)
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(emptyList()))

            // When
            viewModel.removeFromLibrary(contentId)
            advanceUntilIdle()

            // Then
            coVerify { mockLibraryRepository.removeFromLibrary(testUserId, contentId) }
            verify { mockLibraryRepository.getLibraryByUser(testUserId) }
        }

    @Test
    fun `toggleFavorite updates favorite status successfully`() =
        runTest {
            // Given
            val contentId = "content_to_favorite"
            val libraryItem = testLibraryEntity.copy(contentId = contentId, isFavorite = false)
            coEvery { mockLibraryRepository.getLibraryItem(testUserId, contentId) } returns Result.Success(libraryItem)
            coEvery { mockLibraryRepository.updateFavoriteStatus(testUserId, contentId, true) } returns Result.Success(Unit)
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(listOf(libraryItem.copy(isFavorite = true))))

            // When
            viewModel.toggleFavorite(contentId)
            advanceUntilIdle()

            // Then
            coVerify { mockLibraryRepository.getLibraryItem(testUserId, contentId) }
            coVerify { mockLibraryRepository.updateFavoriteStatus(testUserId, contentId, true) }
        }

    @Test
    fun `updateDownloadStatus updates download info successfully`() =
        runTest {
            // Given
            val contentId = "content_to_download"
            val isDownloaded = true
            val filePath = "/path/to/file"
            val fileSizeBytes = 1024L
            coEvery {
                mockLibraryRepository.updateDownloadStatus(
                    testUserId,
                    contentId,
                    isDownloaded,
                    filePath,
                    fileSizeBytes,
                    any(),
                )
            } returns Result.Success(Unit)
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(emptyList()))

            // When
            viewModel.updateDownloadStatus(contentId, isDownloaded, filePath, fileSizeBytes)
            advanceUntilIdle()

            // Then
            coVerify {
                mockLibraryRepository.updateDownloadStatus(
                    testUserId,
                    contentId,
                    isDownloaded,
                    filePath,
                    fileSizeBytes,
                    any(),
                )
            }
        }

    @Test
    fun `loadLibraryStats loads statistics successfully`() =
        runTest {
            // Given
            val totalItems = 10
            val favoriteItems = 3
            val downloadedSize = 1024L * 1024L * 100L // 100MB
            val contentTypes = listOf("MOVIE", "TV_SHOW")

            coEvery { mockLibraryRepository.getLibraryCountByUser(testUserId) } returns Result.Success(totalItems)
            coEvery { mockLibraryRepository.getFavoritesCountByUser(testUserId) } returns Result.Success(favoriteItems)
            coEvery { mockLibraryRepository.getTotalDownloadedSizeByUser(testUserId) } returns Result.Success(downloadedSize)
            coEvery { mockLibraryRepository.getContentTypesByUser(testUserId) } returns Result.Success(contentTypes)

            // When
            viewModel.loadLibraryStats()
            advanceUntilIdle()

            // Then
            val statsState = viewModel.libraryStats.value
            assertTrue(statsState is UiState.Success)
            val stats = statsState.data
            assertEquals(totalItems, stats.totalItems)
            assertEquals(favoriteItems, stats.favoriteItems)
            assertEquals(downloadedSize, stats.downloadedSizeBytes)
            assertEquals(2, stats.availableContentTypes.size)
        }

    @Test
    fun `exportLibrary generates export data successfully`() =
        runTest {
            // Given
            val libraryItems = listOf(testLibraryEntity)
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(libraryItems))

            // When
            val exportResult = viewModel.exportLibrary().first()

            // Then
            assertTrue(exportResult is Result.Success)
            assertTrue(exportResult.data.contains("Export completed with 1 items"))
        }

    @Test
    fun `clearError clears error state`() =
        runTest {
            // Given - Set an error first
            viewModel.addToLibrary("invalid", ContentType.MOVIE, "")
            advanceUntilIdle()

            // When
            viewModel.clearError()

            // Then
            val uiState = viewModel.uiState.value
            assertEquals(null, uiState.error)
        }

    @Test
    fun `refresh reloads content and stats`() =
        runTest {
            // Given
            val libraryItems = listOf(testLibraryEntity)
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(libraryItems))

            // When
            viewModel.refresh()
            advanceUntilIdle()

            // Then
            verify(atLeast = 2) { mockLibraryRepository.getLibraryByUser(testUserId) }
            coVerify(atLeast = 2) { mockLibraryRepository.getLibraryCountByUser(testUserId) }
        }

    @Test
    fun `applyAdditionalFilters sorts by recently added`() =
        runTest {
            // Given
            val now = Date()
            val yesterday = Date(now.time - 24 * 60 * 60 * 1000)
            val items =
                listOf(
                    testLibraryEntity.copy(title = "Old Movie", addedAt = yesterday),
                    testLibraryEntity.copy(title = "New Movie", addedAt = now),
                )
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(items))

            // When
            viewModel.setSortBy(LibrarySortBy.RECENTLY_ADDED)
            viewModel.loadLibraryContent()
            advanceUntilIdle()

            // Then
            val contentState = viewModel.libraryContent.value
            assertTrue(contentState is UiState.Success)
            val sortedItems = contentState.data
            assertEquals("New Movie", sortedItems.first().title)
            assertEquals("Old Movie", sortedItems.last().title)
        }

    @Test
    fun `applyAdditionalFilters sorts alphabetically`() =
        runTest {
            // Given
            val items =
                listOf(
                    testLibraryEntity.copy(title = "Zebra Movie"),
                    testLibraryEntity.copy(title = "Alpha Movie"),
                )
            every { mockLibraryRepository.getLibraryByUser(testUserId) } returns flowOf(Result.Success(items))

            // When
            viewModel.setSortBy(LibrarySortBy.ALPHABETICAL)
            viewModel.loadLibraryContent()
            advanceUntilIdle()

            // Then
            val contentState = viewModel.libraryContent.value
            assertTrue(contentState is UiState.Success)
            val sortedItems = contentState.data
            assertEquals("Alpha Movie", sortedItems.first().title)
            assertEquals("Zebra Movie", sortedItems.last().title)
        }
}
