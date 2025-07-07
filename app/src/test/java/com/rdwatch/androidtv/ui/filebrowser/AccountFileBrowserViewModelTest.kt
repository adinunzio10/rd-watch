package com.rdwatch.androidtv.ui.filebrowser

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.rdwatch.androidtv.test.MainDispatcherRule
import com.rdwatch.androidtv.test.fake.FakeFileBrowserRepository
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for AccountFileBrowserViewModel.
 * Tests core functionality including state management, sorting, filtering, and navigation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountFileBrowserViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeFileBrowserRepository
    private lateinit var viewModel: AccountFileBrowserViewModel

    @Before
    fun setup() {
        repository = FakeFileBrowserRepository()
        viewModel = AccountFileBrowserViewModel(repository)
    }

    @Test
    fun `initial state should be loading`() = runTest {
        // Given fresh viewModel
        val initialState = viewModel.uiState.value
        
        // Then
        assertTrue(initialState.contentState is UiState.Loading)
        assertEquals("/", initialState.currentPath)
        assertEquals(listOf("/"), initialState.navigationHistory)
        assertFalse(initialState.isMultiSelectMode)
        assertEquals(emptySet<String>(), initialState.selectedItems)
    }

    @Test
    fun `should load root content successfully when authenticated`() = runTest {
        // Given authenticated user
        repository.setAuthenticated(true)
        
        // When
        viewModel.uiState.test {
            // Skip initial loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.contentState is UiState.Loading)
            
            // Wait for content to load
            val contentState = awaitItem()
            assertTrue(contentState.contentState is UiState.Success)
            
            val data = contentState.contentState.dataOrNull
            assertNotNull(data)
            assertTrue(data!!.isNotEmpty())
        }
    }

    @Test
    fun `should show error when not authenticated`() = runTest {
        // Given unauthenticated user
        repository.setAuthenticated(false)
        
        // When creating new viewModel
        val unauthenticatedViewModel = AccountFileBrowserViewModel(repository)
        
        // Then
        unauthenticatedViewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.contentState is UiState.Error)
            assertTrue(state.contentState.errorOrNull?.contains("Authentication required") == true)
        }
    }

    @Test
    fun `should handle network error gracefully`() = runTest {
        // Given network error
        repository.setReturnError(true)
        
        // When
        viewModel.uiState.test {
            // Skip initial loading state
            awaitItem()
            
            // Wait for error state
            val errorState = awaitItem()
            assertTrue(errorState.contentState is UiState.Error)
            
            val error = errorState.contentState.errorOrNull
            assertNotNull(error)
            assertTrue(error!!.contains("Network error"))
        }
    }

    @Test
    fun `should toggle multi-select mode`() = runTest {
        // Given viewModel in normal mode
        assertFalse(viewModel.uiState.value.isMultiSelectMode)
        
        // When toggling multi-select
        viewModel.toggleMultiSelect()
        
        // Then
        assertTrue(viewModel.uiState.value.isMultiSelectMode)
        
        // When toggling again
        viewModel.toggleMultiSelect()
        
        // Then
        assertFalse(viewModel.uiState.value.isMultiSelectMode)
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedItems)
    }

    @Test
    fun `should enter bulk selection mode with long press`() = runTest {
        // Given viewModel in normal mode
        assertFalse(viewModel.uiState.value.isMultiSelectMode)
        
        // When entering bulk selection mode
        viewModel.enterBulkSelectionMode("file1")
        
        // Then
        assertTrue(viewModel.uiState.value.isMultiSelectMode)
        assertEquals(setOf("file1"), viewModel.uiState.value.selectedItems)
    }

    @Test
    fun `should toggle item selection`() = runTest {
        // Given multi-select mode
        viewModel.toggleMultiSelect()
        
        // When selecting item
        viewModel.toggleItemSelection("file1")
        
        // Then
        assertTrue(viewModel.uiState.value.selectedItems.contains("file1"))
        
        // When deselecting item
        viewModel.toggleItemSelection("file1")
        
        // Then
        assertFalse(viewModel.uiState.value.selectedItems.contains("file1"))
    }

    @Test
    fun `should clear all selections`() = runTest {
        // Given items selected
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        viewModel.toggleItemSelection("file2")
        
        // When clearing selection
        viewModel.clearSelection()
        
        // Then
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedItems)
    }

    @Test
    fun `should select all visible items`() = runTest {
        // Given content loaded
        viewModel.uiState.test {
            // Skip initial states
            awaitItem()
            val contentState = awaitItem()
            assertTrue(contentState.contentState is UiState.Success)
            
            val data = contentState.contentState.dataOrNull!!
            
            // When selecting all
            viewModel.selectAll()
            
            // Then
            val expectedIds = data.map { it.id }.toSet()
            assertEquals(expectedIds, viewModel.uiState.value.selectedItems)
        }
    }

    @Test
    fun `should update sorting options`() = runTest {
        // Given initial sorting
        assertEquals(SortBy.NAME, viewModel.uiState.value.sortingOptions.sortBy)
        assertEquals(SortOrder.ASCENDING, viewModel.uiState.value.sortingOptions.sortOrder)
        
        // When updating sorting
        val newSorting = SortingOptions(SortBy.SIZE, SortOrder.DESCENDING)
        viewModel.updateSorting(newSorting)
        
        // Then
        assertEquals(SortBy.SIZE, viewModel.uiState.value.sortingOptions.sortBy)
        assertEquals(SortOrder.DESCENDING, viewModel.uiState.value.sortingOptions.sortOrder)
    }

    @Test
    fun `should update filter options`() = runTest {
        // Given initial filter
        assertFalse(viewModel.uiState.value.filterOptions.showOnlyPlayable)
        
        // When updating filter
        val newFilter = FilterOptions(showOnlyPlayable = true)
        viewModel.updateFilter(newFilter)
        
        // Then
        assertTrue(viewModel.uiState.value.filterOptions.showOnlyPlayable)
    }

    @Test
    fun `should search content`() = runTest {
        // Given loaded content
        viewModel.uiState.test {
            // Skip initial states
            awaitItem()
            awaitItem()
            
            // When searching
            viewModel.searchContent("Test Movie")
            
            // Then
            val searchState = awaitItem()
            assertTrue(searchState.contentState is UiState.Success)
            
            val searchResults = searchState.contentState.dataOrNull!!
            assertTrue(searchResults.all { it.name.contains("Test Movie", ignoreCase = true) })
        }
    }

    @Test
    fun `should clear search when query is blank`() = runTest {
        // Given search applied
        viewModel.searchContent("Test")
        
        // When clearing search
        viewModel.searchContent("")
        
        // Then
        assertEquals("", viewModel.uiState.value.filterOptions.searchQuery)
    }

    @Test
    fun `should navigate to torrent content`() = runTest {
        // Given torrent item
        val torrent = FileItem.Torrent(
            id = "torrent1",
            name = "Test Torrent",
            size = 1024L,
            modifiedDate = System.currentTimeMillis(),
            hash = "abc123",
            progress = 1.0f,
            status = TorrentStatus.DOWNLOADED,
            seeders = 5,
            speed = 0L
        )
        
        // When navigating to torrent
        viewModel.navigateToPath("/torrent1", torrent)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("/torrent1", state.currentPath)
        assertEquals(listOf("/", "/torrent1"), state.navigationHistory)
    }

    @Test
    fun `should navigate back in history`() = runTest {
        // Given navigation history
        viewModel.navigateToPath("/folder1")
        viewModel.navigateToPath("/folder1/subfolder")
        
        // When navigating back
        viewModel.navigateBack()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("/folder1", state.currentPath)
        assertEquals(listOf("/", "/folder1"), state.navigationHistory)
    }

    @Test
    fun `should emit back navigation event at root`() = runTest {
        // Given at root
        assertEquals("/", viewModel.uiState.value.currentPath)
        
        // When navigating back
        viewModel.events.test {
            viewModel.navigateBack()
            
            // Then
            val event = awaitItem()
            assertEquals(FileBrowserEvent.NavigateBack, event)
        }
    }

    @Test
    fun `should handle file selection for playback`() = runTest {
        // Given playable file
        val file = FileItem.File(
            id = "file1",
            name = "Test Movie.mp4",
            size = 1024L,
            modifiedDate = System.currentTimeMillis(),
            mimeType = "video/mp4",
            downloadUrl = "url",
            streamUrl = "stream",
            isPlayable = true
        )
        
        // When selecting file
        viewModel.events.test {
            viewModel.selectFile(file)
            
            // Then
            val event = awaitItem()
            assertTrue(event is FileBrowserEvent.NavigateToPlayer)
        }
    }

    @Test
    fun `should show error for non-playable file`() = runTest {
        // Given non-playable file
        val file = FileItem.File(
            id = "file1",
            name = "Test Document.pdf",
            size = 1024L,
            modifiedDate = System.currentTimeMillis(),
            mimeType = "application/pdf",
            downloadUrl = "url",
            streamUrl = null,
            isPlayable = false
        )
        
        // When selecting file
        viewModel.events.test {
            viewModel.selectFile(file)
            
            // Then
            val event = awaitItem()
            assertTrue(event is FileBrowserEvent.ShowError)
            assertTrue((event as FileBrowserEvent.ShowError).message.contains("cannot be played"))
        }
    }

    @Test
    fun `should download selected files`() = runTest {
        // Given selected files
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        viewModel.toggleItemSelection("file2")
        
        // When downloading
        viewModel.events.test {
            viewModel.downloadSelectedFiles()
            
            // Then
            val event = awaitItem()
            assertTrue(event is FileBrowserEvent.ShowSuccess)
        }
    }

    @Test
    fun `should show confirm dialog for deletion`() = runTest {
        // Given selected files
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        
        // When deleting
        viewModel.events.test {
            viewModel.deleteSelectedFiles()
            
            // Then
            val event = awaitItem()
            assertTrue(event is FileBrowserEvent.ShowConfirmDialog)
        }
    }

    @Test
    fun `should refresh content`() = runTest {
        // Given initial content
        val initialRefreshCount = repository.getRefreshCount()
        
        // When refreshing
        viewModel.refresh()
        
        // Then
        val newRefreshCount = repository.getRefreshCount()
        assertEquals(initialRefreshCount + 1, newRefreshCount)
    }

    @Test
    fun `should load more content for pagination`() = runTest {
        // Given pagination state
        val initialState = viewModel.uiState.value
        val pageSize = initialState.paginationState.pageSize
        
        // When loading more content
        viewModel.loadMoreContent()
        
        // Then
        val newState = viewModel.uiState.value
        assertEquals(1, newState.paginationState.currentPage)
        assertEquals(pageSize, newState.paginationState.offset)
    }

    @Test
    fun `should not load more when no more content available`() = runTest {
        // Given no more content
        repository.setReturnEmpty(true)
        
        // When loading more content
        viewModel.loadMoreContent()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.paginationState.hasMore)
        assertFalse(state.paginationState.isLoadingMore)
    }

    @Test
    fun `should show file details`() = runTest {
        // Given file item
        val file = FileItem.File(
            id = "file1",
            name = "Test Movie.mp4",
            size = 1024L,
            modifiedDate = System.currentTimeMillis(),
            mimeType = "video/mp4",
            downloadUrl = "url",
            streamUrl = "stream",
            isPlayable = true
        )
        
        // When showing file details
        viewModel.events.test {
            viewModel.showFileDetails(file)
            
            // Then
            val event = awaitItem()
            assertTrue(event is FileBrowserEvent.ShowFileDetails)
            assertEquals(file, (event as FileBrowserEvent.ShowFileDetails).item)
        }
    }

    @Test
    fun `should play first playable selected file`() = runTest {
        // Given selected files with one playable
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1") // playable
        viewModel.toggleItemSelection("doc1") // not playable
        
        // When playing selected files
        viewModel.events.test {
            viewModel.playSelectedFiles()
            
            // Then
            val event = awaitItem()
            assertTrue(event is FileBrowserEvent.NavigateToPlayer)
        }
    }

    @Test
    fun `should show error when no playable files selected`() = runTest {
        // Given selected non-playable files
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("doc1") // not playable
        
        // When playing selected files
        viewModel.events.test {
            viewModel.playSelectedFiles()
            
            // Then
            val event = awaitItem()
            assertTrue(event is FileBrowserEvent.ShowError)
            assertTrue((event as FileBrowserEvent.ShowError).message.contains("No playable files"))
        }
    }

    @Test
    fun `should get correct selection state`() = runTest {
        // Given content loaded and items selected
        viewModel.uiState.test {
            awaitItem()
            awaitItem()
            
            // When selecting items
            viewModel.toggleMultiSelect()
            viewModel.toggleItemSelection("file1") // playable
            viewModel.toggleItemSelection("doc1") // not playable
            
            // Then
            val selectionState = viewModel.getSelectionState()
            assertEquals(2, selectionState.selectedCount)
            assertTrue(selectionState.canDownload)
            assertTrue(selectionState.canDelete)
            assertTrue(selectionState.canPlay)
        }
    }

    @Test
    fun `should handle repository errors gracefully`() = runTest {
        // Given repository error
        repository.setReturnError(true)
        
        // When error occurs
        viewModel.uiState.test {
            // Skip initial loading state
            awaitItem()
            
            // Wait for error state
            val errorState = awaitItem()
            assertTrue(errorState.contentState is UiState.Error)
            
            val error = errorState.contentState.errorOrNull
            assertNotNull(error)
        }
    }

    @Test
    fun `should apply sorting and filtering correctly`() = runTest {
        // Given content loaded
        viewModel.uiState.test {
            awaitItem()
            awaitItem()
            
            // When applying filters
            val filterOptions = FilterOptions(showOnlyPlayable = true)
            viewModel.updateFilter(filterOptions)
            
            // Then
            val filteredState = awaitItem()
            assertTrue(filteredState.contentState is UiState.Success)
            
            val filteredData = filteredState.contentState.dataOrNull!!
            assertTrue(filteredData.all { item ->
                when (item) {
                    is FileItem.File -> item.isPlayable
                    is FileItem.Torrent -> item.files.any { it.isPlayable }
                    else -> true
                }
            })
        }
    }

    @Test
    fun `should handle search with network delay`() = runTest {
        // Given network delay
        repository.setNetworkDelay(100)
        
        // When searching
        viewModel.searchContent("Test")
        
        // Then should handle delay gracefully
        viewModel.uiState.test(timeout = 5.seconds) {
            val state = awaitItem()
            assertTrue(state.contentState is UiState.Success || state.contentState is UiState.Loading)
        }
    }

    @Test
    fun `should handle empty content gracefully`() = runTest {
        // Given empty content
        repository.setReturnEmpty(true)
        
        // When loading content
        val emptyViewModel = AccountFileBrowserViewModel(repository)
        
        // Then
        emptyViewModel.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertTrue(state.contentState is UiState.Success)
            assertTrue(state.contentState.dataOrNull?.isEmpty() == true)
        }
    }

    @Test
    fun `should maintain state across configuration changes`() = runTest {
        // Given viewModel with state
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        viewModel.updateSorting(SortingOptions(SortBy.SIZE, SortOrder.DESCENDING))
        
        // When simulating configuration change (state should persist)
        val currentState = viewModel.uiState.value
        
        // Then
        assertTrue(currentState.isMultiSelectMode)
        assertTrue(currentState.selectedItems.contains("file1"))
        assertEquals(SortBy.SIZE, currentState.sortingOptions.sortBy)
        assertEquals(SortOrder.DESCENDING, currentState.sortingOptions.sortOrder)
    }
}