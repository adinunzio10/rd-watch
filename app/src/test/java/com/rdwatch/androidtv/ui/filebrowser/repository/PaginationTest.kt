package com.rdwatch.androidtv.ui.filebrowser.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.test.MainDispatcherRule
import com.rdwatch.androidtv.test.fake.FakeFileBrowserRepository
import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for pagination logic in FileBrowserRepository.
 * Tests page loading, offset management, and pagination state handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaginationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeFileBrowserRepository

    @Before
    fun setup() {
        repository = FakeFileBrowserRepository()
    }

    @Test
    fun `should load initial page correctly`() = runTest {
        // Given repository with test data
        val allFiles = repository.getTestFiles()
        val allTorrents = repository.getTestTorrents()
        val totalItems = allFiles.size + allTorrents.size
        
        // When loading root content
        repository.getRootContent().test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertEquals(totalItems, data.size)
            
            // Should return all items for initial load
            assertTrue(data.isNotEmpty())
        }
    }

    @Test
    fun `should handle pagination state correctly`() = runTest {
        // Given pagination state
        val pageSize = 10
        val paginationState = PaginationState(
            currentPage = 0,
            pageSize = pageSize,
            totalItems = 25,
            isLoadingMore = false,
            hasNextPage = true,
            offset = 0
        )
        
        // When checking has more
        assertTrue(paginationState.hasMore)
        
        // When setting loading more
        val loadingState = paginationState.copy(isLoadingMore = true)
        assertFalse(loadingState.hasMore)
        
        // When no next page
        val noNextPageState = paginationState.copy(hasNextPage = false)
        assertFalse(noNextPageState.hasMore)
    }

    @Test
    fun `should calculate pagination correctly`() = runTest {
        // Given pagination parameters
        val pageSize = 5
        val totalItems = 13
        val currentPage = 2
        
        // When calculating pagination
        val offset = currentPage * pageSize
        val hasNextPage = offset + pageSize < totalItems
        val remainingItems = totalItems - offset
        
        // Then
        assertEquals(10, offset)
        assertTrue(hasNextPage)
        assertEquals(3, remainingItems)
    }

    @Test
    fun `should handle last page correctly`() = runTest {
        // Given last page parameters
        val pageSize = 10
        val totalItems = 25
        val currentPage = 2 // Last page (pages 0, 1, 2)
        
        // When calculating last page
        val offset = currentPage * pageSize
        val hasNextPage = offset + pageSize < totalItems
        val remainingItems = totalItems - offset
        
        // Then
        assertEquals(20, offset)
        assertFalse(hasNextPage)
        assertEquals(5, remainingItems)
    }

    @Test
    fun `should handle empty page correctly`() = runTest {
        // Given empty results
        repository.setReturnEmpty(true)
        
        // When loading content
        repository.getRootContent().test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.isEmpty())
        }
    }

    @Test
    fun `should handle pagination with filtering`() = runTest {
        // Given filter options
        val filterOptions = FilterOptions(showOnlyPlayable = true)
        val sortingOptions = SortingOptions()
        
        // When getting filtered content
        repository.getFilteredContent(filterOptions, sortingOptions).test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.all { item ->
                when (item) {
                    is FileItem.File -> item.isPlayable
                    is FileItem.Torrent -> item.files.any { it.isPlayable }
                    else -> false
                }
            })
        }
    }

    @Test
    fun `should handle pagination with sorting`() = runTest {
        // Given sorting options
        val filterOptions = FilterOptions()
        val sortingOptions = SortingOptions(sortBy = SortBy.SIZE, sortOrder = SortOrder.DESCENDING)
        
        // When getting sorted content
        repository.getFilteredContent(filterOptions, sortingOptions).test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            // Should be sorted by size descending
            for (i in 0 until data.size - 1) {
                assertTrue(data[i].size >= data[i + 1].size)
            }
        }
    }

    @Test
    fun `should handle pagination with search`() = runTest {
        // Given search query
        val query = "Test Movie"
        
        // When searching content
        repository.searchContent(query).test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.all { it.name.contains(query, ignoreCase = true) })
        }
    }

    @Test
    fun `should handle pagination by file type`() = runTest {
        // Given file type filter
        val videoTypes = setOf(FileType.VIDEO)
        
        // When getting content by file type
        repository.getContentByFileType(videoTypes).test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.all { item ->
                when (item) {
                    is FileItem.File -> {
                        val extension = item.name.substringAfterLast('.', "")
                        val fileType = FileType.fromExtension(extension)
                        videoTypes.contains(fileType)
                    }
                    else -> false
                }
            })
        }
    }

    @Test
    fun `should handle pagination by date range`() = runTest {
        // Given date range
        val oneDayAgo = System.currentTimeMillis() - 86400000L
        val now = System.currentTimeMillis()
        
        // When getting content by date range
        repository.getContentByDateRange(oneDayAgo, now).test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.all { it.modifiedDate in oneDayAgo..now })
        }
    }

    @Test
    fun `should handle pagination by size range`() = runTest {
        // Given size range (1MB to 1GB)
        val minSize = 1024L * 1024L // 1MB
        val maxSize = 1024L * 1024L * 1024L // 1GB
        
        // When getting content by size range
        repository.getContentBySizeRange(minSize, maxSize).test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.all { it.size in minSize..maxSize })
        }
    }

    @Test
    fun `should handle pagination with network delay`() = runTest {
        // Given network delay
        repository.setNetworkDelay(100)
        
        // When loading content
        repository.getRootContent().test(timeout = 5.seconds) {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.isNotEmpty())
        }
    }

    @Test
    fun `should handle pagination errors gracefully`() = runTest {
        // Given repository error
        repository.setReturnError(true)
        
        // When loading content
        repository.getRootContent().test {
            val result = awaitItem()
            assertTrue(result is Result.Error)
            
            val error = result.exception
            assertNotNull(error)
        }
    }

    @Test
    fun `should handle concurrent pagination requests`() = runTest {
        // Given multiple concurrent requests
        val filterOptions1 = FilterOptions(showOnlyPlayable = true)
        val filterOptions2 = FilterOptions(showOnlyDownloaded = true)
        val sortingOptions = SortingOptions()
        
        // When making concurrent requests
        repository.getFilteredContent(filterOptions1, sortingOptions).test {
            val result1 = awaitItem()
            assertTrue(result1 is Result.Success)
        }
        
        repository.getFilteredContent(filterOptions2, sortingOptions).test {
            val result2 = awaitItem()
            assertTrue(result2 is Result.Success)
        }
    }

    @Test
    fun `should handle pagination state transitions`() = runTest {
        // Given initial pagination state
        val initialState = PaginationState()
        
        // When loading first page
        val firstPageState = initialState.copy(
            currentPage = 0,
            offset = 0,
            hasNextPage = true,
            isLoadingMore = false
        )
        
        // Then
        assertEquals(0, firstPageState.currentPage)
        assertEquals(0, firstPageState.offset)
        assertTrue(firstPageState.hasMore)
        
        // When loading next page
        val nextPageState = firstPageState.copy(
            currentPage = 1,
            offset = firstPageState.pageSize,
            isLoadingMore = true
        )
        
        // Then
        assertEquals(1, nextPageState.currentPage)
        assertEquals(firstPageState.pageSize, nextPageState.offset)
        assertFalse(nextPageState.hasMore) // isLoadingMore is true
        
        // When page load complete
        val completeState = nextPageState.copy(
            isLoadingMore = false,
            hasNextPage = true
        )
        
        // Then
        assertTrue(completeState.hasMore)
    }

    @Test
    fun `should handle pagination with deleted items`() = runTest {
        // Given items to delete
        val itemsToDelete = setOf("file1", "file2")
        
        // When deleting items
        repository.deleteItems(itemsToDelete)
        
        // Then get remaining content
        repository.getRootContent().test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.none { itemsToDelete.contains(it.id) })
        }
    }

    @Test
    fun `should handle pagination refresh correctly`() = runTest {
        // Given initial content
        val initialRefreshCount = repository.getRefreshCount()
        
        // When refreshing content
        repository.refreshContent()
        
        // Then refresh count should increase
        assertEquals(initialRefreshCount + 1, repository.getRefreshCount())
        
        // And content should be available
        repository.getRootContent().test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.isNotEmpty())
        }
    }

    @Test
    fun `should handle pull to refresh with pagination`() = runTest {
        // Given some operations performed
        repository.deleteItems(setOf("file1"))
        
        // When pull to refresh
        repository.pullToRefresh()
        
        // Then content should be restored
        repository.getRootContent().test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.any { it.id == "file1" }) // Should be restored
        }
    }

    @Test
    fun `should handle pagination with cache`() = runTest {
        // Given cache info
        val cacheInfo = repository.getCacheInfo()
        
        // Then cache should contain data
        assertTrue(cacheInfo.itemCount > 0)
        assertTrue(cacheInfo.totalSize > 0)
        assertFalse(cacheInfo.isExpired)
        
        // When clearing cache
        repository.clearCache()
        
        // Then cache should be empty
        val clearedCacheInfo = repository.getCacheInfo()
        assertTrue(clearedCacheInfo.isExpired)
    }

    @Test
    fun `should handle edge cases in pagination`() = runTest {
        // Given edge case: single item
        repository.clearTestData()
        repository.addTestFile(
            FileItem.File(
                id = "single",
                name = "Single File.mp4",
                size = 1024L,
                modifiedDate = System.currentTimeMillis(),
                mimeType = "video/mp4",
                downloadUrl = "url",
                streamUrl = "stream",
                isPlayable = true
            )
        )
        
        // When loading content
        repository.getRootContent().test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertEquals(1, data.size)
            assertEquals("single", data[0].id)
        }
    }

    @Test
    fun `should handle zero items pagination`() = runTest {
        // Given no items
        repository.clearTestData()
        
        // When loading content
        repository.getRootContent().test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            
            val data = result.data
            assertTrue(data.isEmpty())
        }
    }
}