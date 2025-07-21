package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.LibraryDao
import com.rdwatch.androidtv.data.entities.LibraryEntity
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
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for LibraryRepositoryImpl
 * Tests CRUD operations, reactive flows, error handling, and data consistency
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: LibraryRepositoryImpl
    private lateinit var mockLibraryDao: LibraryDao

    private val testUserId = 1L
    private val testContentId = "test_content_123"
    private val testLibraryEntity =
        LibraryEntity(
            libraryId = 1L,
            userId = testUserId,
            contentId = testContentId,
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
        mockLibraryDao = mockk()
        repository = LibraryRepositoryImpl(mockLibraryDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `getLibraryByUser returns success with data`() =
        runTest {
            // Given
            val libraryItems = listOf(testLibraryEntity)
            every { mockLibraryDao.getLibraryByUser(testUserId) } returns flowOf(libraryItems)

            // When
            val result = repository.getLibraryByUser(testUserId).first()

            // Then
            assertTrue(result is Result.Success)
            assertEquals(libraryItems, result.data)
            verify { mockLibraryDao.getLibraryByUser(testUserId) }
        }

    @Test
    fun `getLibraryByUser handles database exception`() =
        runTest {
            // Given
            val exception = RuntimeException("Database error")
            every { mockLibraryDao.getLibraryByUser(testUserId) } throws exception

            // When
            val result = repository.getLibraryByUser(testUserId).first()

            // Then
            assertTrue(result is Result.Error)
            assertEquals(exception, result.exception)
        }

    @Test
    fun `getLibraryByUserAndType filters correctly`() =
        runTest {
            // Given
            val contentType = "TV_SHOW"
            val filteredItems = listOf(testLibraryEntity.copy(contentType = contentType))
            every { mockLibraryDao.getLibraryByUserAndType(testUserId, contentType) } returns flowOf(filteredItems)

            // When
            val result = repository.getLibraryByUserAndType(testUserId, contentType).first()

            // Then
            assertTrue(result is Result.Success)
            assertEquals(filteredItems, result.data)
            verify { mockLibraryDao.getLibraryByUserAndType(testUserId, contentType) }
        }

    @Test
    fun `getFavoritesByUser returns only favorites`() =
        runTest {
            // Given
            val favoriteItems = listOf(testLibraryEntity.copy(isFavorite = true))
            every { mockLibraryDao.getFavoritesByUser(testUserId) } returns flowOf(favoriteItems)

            // When
            val result = repository.getFavoritesByUser(testUserId).first()

            // Then
            assertTrue(result is Result.Success)
            assertEquals(favoriteItems, result.data)
            verify { mockLibraryDao.getFavoritesByUser(testUserId) }
        }

    @Test
    fun `getDownloadedContentByUser returns only downloaded items`() =
        runTest {
            // Given
            val downloadedItems = listOf(testLibraryEntity.copy(isDownloaded = true, filePath = "/path/to/file"))
            every { mockLibraryDao.getDownloadedContentByUser(testUserId) } returns flowOf(downloadedItems)

            // When
            val result = repository.getDownloadedContentByUser(testUserId).first()

            // Then
            assertTrue(result is Result.Success)
            assertEquals(downloadedItems, result.data)
            verify { mockLibraryDao.getDownloadedContentByUser(testUserId) }
        }

    @Test
    fun `getLibraryItem returns existing item`() =
        runTest {
            // Given
            coEvery { mockLibraryDao.getLibraryItem(testUserId, testContentId) } returns testLibraryEntity

            // When
            val result = repository.getLibraryItem(testUserId, testContentId)

            // Then
            assertTrue(result is Result.Success)
            assertEquals(testLibraryEntity, result.data)
            coVerify { mockLibraryDao.getLibraryItem(testUserId, testContentId) }
        }

    @Test
    fun `getLibraryItem returns null for non-existing item`() =
        runTest {
            // Given
            coEvery { mockLibraryDao.getLibraryItem(testUserId, testContentId) } returns null

            // When
            val result = repository.getLibraryItem(testUserId, testContentId)

            // Then
            assertTrue(result is Result.Success)
            assertEquals(null, result.data)
        }

    @Test
    fun `searchLibrary returns filtered results`() =
        runTest {
            // Given
            val query = "test"
            val searchResults = listOf(testLibraryEntity)
            every { mockLibraryDao.searchLibrary(testUserId, query) } returns flowOf(searchResults)

            // When
            val result = repository.searchLibrary(testUserId, query).first()

            // Then
            assertTrue(result is Result.Success)
            assertEquals(searchResults, result.data)
            verify { mockLibraryDao.searchLibrary(testUserId, query) }
        }

    @Test
    fun `getContentTypesByUser returns available types`() =
        runTest {
            // Given
            val contentTypes = listOf("MOVIE", "TV_SHOW")
            coEvery { mockLibraryDao.getContentTypesByUser(testUserId) } returns contentTypes

            // When
            val result = repository.getContentTypesByUser(testUserId)

            // Then
            assertTrue(result is Result.Success)
            assertEquals(contentTypes, result.data)
            coVerify { mockLibraryDao.getContentTypesByUser(testUserId) }
        }

    @Test
    fun `getLibraryCountByUser returns correct count`() =
        runTest {
            // Given
            val count = 5
            coEvery { mockLibraryDao.getLibraryCountByUser(testUserId) } returns count

            // When
            val result = repository.getLibraryCountByUser(testUserId)

            // Then
            assertTrue(result is Result.Success)
            assertEquals(count, result.data)
            coVerify { mockLibraryDao.getLibraryCountByUser(testUserId) }
        }

    @Test
    fun `getTotalDownloadedSizeByUser handles null result`() =
        runTest {
            // Given
            coEvery { mockLibraryDao.getTotalDownloadedSizeByUser(testUserId) } returns null

            // When
            val result = repository.getTotalDownloadedSizeByUser(testUserId)

            // Then
            assertTrue(result is Result.Success)
            assertEquals(0L, result.data)
        }

    @Test
    fun `getTotalDownloadedSizeByUser returns actual size`() =
        runTest {
            // Given
            val size = 1024L * 1024L * 100L // 100MB
            coEvery { mockLibraryDao.getTotalDownloadedSizeByUser(testUserId) } returns size

            // When
            val result = repository.getTotalDownloadedSizeByUser(testUserId)

            // Then
            assertTrue(result is Result.Success)
            assertEquals(size, result.data)
        }

    @Test
    fun `isInLibrary returns true when item exists`() =
        runTest {
            // Given
            coEvery { mockLibraryDao.isInLibrary(testUserId, testContentId) } returns 1

            // When
            val result = repository.isInLibrary(testUserId, testContentId)

            // Then
            assertTrue(result is Result.Success)
            assertTrue(result.data)
            coVerify { mockLibraryDao.isInLibrary(testUserId, testContentId) }
        }

    @Test
    fun `isInLibrary returns false when item does not exist`() =
        runTest {
            // Given
            coEvery { mockLibraryDao.isInLibrary(testUserId, testContentId) } returns 0

            // When
            val result = repository.isInLibrary(testUserId, testContentId)

            // Then
            assertTrue(result is Result.Success)
            assertFalse(result.data)
        }

    @Test
    fun `addToLibrary inserts item successfully`() =
        runTest {
            // Given
            coEvery { mockLibraryDao.insertLibraryItem(testLibraryEntity) } just Runs

            // When
            val result = repository.addToLibrary(testLibraryEntity)

            // Then
            assertTrue(result is Result.Success)
            coVerify { mockLibraryDao.insertLibraryItem(testLibraryEntity) }
        }

    @Test
    fun `addToLibrary handles insertion failure`() =
        runTest {
            // Given
            val exception = RuntimeException("Insert failed")
            coEvery { mockLibraryDao.insertLibraryItem(testLibraryEntity) } throws exception

            // When
            val result = repository.addToLibrary(testLibraryEntity)

            // Then
            assertTrue(result is Result.Error)
            assertEquals(exception, result.exception)
        }

    @Test
    fun `addToLibrary with list inserts multiple items`() =
        runTest {
            // Given
            val items = listOf(testLibraryEntity, testLibraryEntity.copy(contentId = "content_2"))
            coEvery { mockLibraryDao.insertLibraryItems(items) } just Runs

            // When
            val result = repository.addToLibrary(items)

            // Then
            assertTrue(result is Result.Success)
            coVerify { mockLibraryDao.insertLibraryItems(items) }
        }

    @Test
    fun `updateFavoriteStatus updates correctly`() =
        runTest {
            // Given
            val isFavorite = true
            coEvery { mockLibraryDao.updateFavoriteStatus(testUserId, testContentId, isFavorite) } just Runs

            // When
            val result = repository.updateFavoriteStatus(testUserId, testContentId, isFavorite)

            // Then
            assertTrue(result is Result.Success)
            coVerify { mockLibraryDao.updateFavoriteStatus(testUserId, testContentId, isFavorite) }
        }

    @Test
    fun `updateDownloadStatus updates correctly`() =
        runTest {
            // Given
            val isDownloaded = true
            val filePath = "/path/to/file"
            val fileSizeBytes = 1024L
            val updatedAt = Date()
            coEvery {
                mockLibraryDao.updateDownloadStatus(
                    testUserId,
                    testContentId,
                    isDownloaded,
                    filePath,
                    fileSizeBytes,
                    updatedAt,
                )
            } just Runs

            // When
            val result =
                repository.updateDownloadStatus(
                    testUserId,
                    testContentId,
                    isDownloaded,
                    filePath,
                    fileSizeBytes,
                    updatedAt,
                )

            // Then
            assertTrue(result is Result.Success)
            coVerify {
                mockLibraryDao.updateDownloadStatus(
                    testUserId,
                    testContentId,
                    isDownloaded,
                    filePath,
                    fileSizeBytes,
                    updatedAt,
                )
            }
        }

    @Test
    fun `removeFromLibrary deletes item successfully`() =
        runTest {
            // Given
            coEvery { mockLibraryDao.deleteLibraryItem(testUserId, testContentId) } just Runs

            // When
            val result = repository.removeFromLibrary(testUserId, testContentId)

            // Then
            assertTrue(result is Result.Success)
            coVerify { mockLibraryDao.deleteLibraryItem(testUserId, testContentId) }
        }

    @Test
    fun `clearLibraryForUser deletes all items for user`() =
        runTest {
            // Given
            coEvery { mockLibraryDao.deleteAllLibraryForUser(testUserId) } just Runs

            // When
            val result = repository.clearLibraryForUser(testUserId)

            // Then
            assertTrue(result is Result.Success)
            coVerify { mockLibraryDao.deleteAllLibraryForUser(testUserId) }
        }

    @Test
    fun `cleanupOrphanedDownloads cleans up successfully`() =
        runTest {
            // Given
            coEvery { mockLibraryDao.cleanupOrphanedDownloads() } just Runs

            // When
            val result = repository.cleanupOrphanedDownloads()

            // Then
            assertTrue(result is Result.Success)
            coVerify { mockLibraryDao.cleanupOrphanedDownloads() }
        }

    @Test
    fun `repository handles database exceptions gracefully`() =
        runTest {
            // Given
            val exception = RuntimeException("Database connection failed")
            coEvery { mockLibraryDao.getLibraryCountByUser(testUserId) } throws exception

            // When
            val result = repository.getLibraryCountByUser(testUserId)

            // Then
            assertTrue(result is Result.Error)
            assertEquals(exception, result.exception)
        }
}
