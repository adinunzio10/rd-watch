package com.rdwatch.androidtv.ui.filebrowser

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.test.HiltInstrumentedTestBase
import com.rdwatch.androidtv.test.fake.FakeFileBrowserRepository
import com.rdwatch.androidtv.ui.filebrowser.models.*
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * UI tests for bulk selection functionality in the Account File Browser.
 * Tests multi-select mode, selection interactions, and bulk operations.
 */
@HiltAndroidTest
@UninstallModules(/* Add actual modules to uninstall */)
@RunWith(AndroidJUnit4::class)
class BulkSelectionTest : HiltInstrumentedTestBase() {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Inject
    lateinit var repository: FakeFileBrowserRepository

    private lateinit var viewModel: AccountFileBrowserViewModel

    @Before
    override fun setUp() {
        super.setUp()
        repository = FakeFileBrowserRepository()
        viewModel = AccountFileBrowserViewModel(repository)
    }

    @Test
    fun testEnterMultiSelectModeWithToggle() {
        // Given content loaded
        repository.setAuthenticated(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When toggling multi-select mode
        composeTestRule
            .onNodeWithTag("multi_select_toggle")
            .performClick()
        
        // Then should enter multi-select mode
        composeTestRule
            .onNodeWithTag("multi_select_indicator")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("0 selected")
    }

    @Test
    fun testEnterMultiSelectModeWithLongPress() {
        // Given content loaded
        repository.setAuthenticated(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When long pressing an item
        composeTestRule
            .onNodeWithTag("file_item_0")
            .performTouchInput {
                longClick()
            }
        
        // Then should enter multi-select mode with item selected
        composeTestRule
            .onNodeWithTag("multi_select_indicator")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("1 selected")
        
        composeTestRule
            .onNodeWithTag("file_item_0_selected")
            .assertExists()
    }

    @Test
    fun testSelectMultipleItemsWithClick() {
        // Given multi-select mode enabled
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When clicking multiple items
        composeTestRule
            .onNodeWithTag("file_item_0")
            .performClick()
        
        composeTestRule
            .onNodeWithTag("file_item_1")
            .performClick()
        
        composeTestRule
            .onNodeWithTag("file_item_2")
            .performClick()
        
        // Then all items should be selected
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("3 selected")
        
        composeTestRule
            .onNodeWithTag("file_item_0_selected")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("file_item_1_selected")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("file_item_2_selected")
            .assertExists()
    }

    @Test
    fun testDeselectItemsWithClick() {
        // Given multi-select mode with selected items
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        viewModel.toggleItemSelection("file2")
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When clicking already selected item
        composeTestRule
            .onNodeWithTag("file_item_0")
            .performClick()
        
        // Then item should be deselected
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("1 selected")
        
        composeTestRule
            .onNodeWithTag("file_item_0_selected")
            .assertDoesNotExist()
    }

    @Test
    fun testSelectAllItems() {
        // Given multi-select mode enabled
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When clicking select all
        composeTestRule
            .onNodeWithTag("select_all_button")
            .performClick()
        
        // Then all visible items should be selected
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextContains("selected") // Should show count of all items
        
        // Check that individual items show selection
        composeTestRule
            .onNodeWithTag("file_item_0_selected")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("file_item_1_selected")
            .assertExists()
    }

    @Test
    fun testClearAllSelections() {
        // Given multi-select mode with selected items
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.selectAll()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When clicking clear all
        composeTestRule
            .onNodeWithTag("clear_selection_button")
            .performClick()
        
        // Then no items should be selected
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("0 selected")
        
        composeTestRule
            .onNodeWithTag("file_item_0_selected")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithTag("file_item_1_selected")
            .assertDoesNotExist()
    }

    @Test
    fun testExitMultiSelectMode() {
        // Given multi-select mode with selected items
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When toggling multi-select off
        composeTestRule
            .onNodeWithTag("multi_select_toggle")
            .performClick()
        
        // Then should exit multi-select mode and clear selections
        composeTestRule
            .onNodeWithTag("multi_select_indicator")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithTag("file_item_0_selected")
            .assertDoesNotExist()
    }

    @Test
    fun testBulkDownloadAction() {
        // Given multi-select mode with selected files
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        viewModel.toggleItemSelection("file2")
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When clicking download action
        composeTestRule
            .onNodeWithTag("action_download")
            .assertIsDisplayed()
            .performClick()
        
        // Then download should be initiated
        // Note: In real implementation, this would show progress or success message
        composeTestRule.waitForIdle()
    }

    @Test
    fun testBulkDeleteAction() {
        // Given multi-select mode with selected files
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        viewModel.toggleItemSelection("file2")
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When clicking delete action
        composeTestRule
            .onNodeWithTag("action_delete")
            .assertIsDisplayed()
            .performClick()
        
        // Then should show confirmation dialog
        composeTestRule
            .onNodeWithTag("delete_confirmation_dialog")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Delete Files")
            .assertExists()
    }

    @Test
    fun testBulkPlayAction() {
        // Given multi-select mode with playable files selected
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1") // video file
        viewModel.toggleItemSelection("audio1") // audio file
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When clicking play action
        composeTestRule
            .onNodeWithTag("action_play")
            .assertIsDisplayed()
            .performClick()
        
        // Then should play first playable file
        // Note: In real implementation, this would navigate to player
        composeTestRule.waitForIdle()
    }

    @Test
    fun testSelectionCounterUpdates() {
        // Given multi-select mode enabled
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When selecting items one by one
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("0 selected")
        
        composeTestRule
            .onNodeWithTag("file_item_0")
            .performClick()
        
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("1 selected")
        
        composeTestRule
            .onNodeWithTag("file_item_1")
            .performClick()
        
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("2 selected")
        
        composeTestRule
            .onNodeWithTag("file_item_2")
            .performClick()
        
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("3 selected")
    }

    @Test
    fun testActionButtonsAvailability() {
        // Given multi-select mode with no selections
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Then action buttons should be disabled
        composeTestRule
            .onNodeWithTag("action_download")
            .assertIsNotEnabled()
        
        composeTestRule
            .onNodeWithTag("action_delete")
            .assertIsNotEnabled()
        
        composeTestRule
            .onNodeWithTag("action_play")
            .assertIsNotEnabled()
        
        // When selecting an item
        composeTestRule
            .onNodeWithTag("file_item_0")
            .performClick()
        
        // Then action buttons should be enabled
        composeTestRule
            .onNodeWithTag("action_download")
            .assertIsEnabled()
        
        composeTestRule
            .onNodeWithTag("action_delete")
            .assertIsEnabled()
    }

    @Test
    fun testPlayActionOnlyEnabledForPlayableFiles() {
        // Given multi-select mode
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When selecting non-playable file (document)
        composeTestRule
            .onNodeWithTag("file_item_doc1") // Assuming doc1 is not playable
            .performClick()
        
        // Then play action should be disabled
        composeTestRule
            .onNodeWithTag("action_play")
            .assertIsNotEnabled()
        
        // When selecting playable file
        composeTestRule
            .onNodeWithTag("file_item_0") // Video file
            .performClick()
        
        // Then play action should be enabled
        composeTestRule
            .onNodeWithTag("action_play")
            .assertIsEnabled()
    }

    @Test
    fun testVisualSelectionIndicators() {
        // Given multi-select mode enabled
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When selecting an item
        composeTestRule
            .onNodeWithTag("file_item_0")
            .performClick()
        
        // Then should show visual selection indicators
        composeTestRule
            .onNodeWithTag("file_item_0_selected")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("file_item_0_checkbox")
            .assertExists()
        
        // When deselecting the item
        composeTestRule
            .onNodeWithTag("file_item_0")
            .performClick()
        
        // Then visual indicators should be removed
        composeTestRule
            .onNodeWithTag("file_item_0_selected")
            .assertDoesNotExist()
    }

    @Test
    fun testKeyboardSelectionInMultiSelectMode() {
        // Given multi-select mode enabled
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When using spacebar to select items
        composeTestRule
            .onNodeWithTag("file_item_0")
            .requestFocus()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.Spacebar)
            }
        
        // Then item should be selected
        composeTestRule
            .onNodeWithTag("file_item_0_selected")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("1 selected")
    }

    @Test
    fun testSelectionPersistenceAcrossFiltering() {
        // Given multi-select mode with selected items
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        viewModel.toggleItemSelection("audio1")
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When applying a filter
        composeTestRule
            .onNodeWithTag("filter_playable_only")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then selections should be maintained for visible items
        composeTestRule
            .onNodeWithTag("file_item_0_selected") // If video file is still visible
            .assertExists()
    }

    @Test
    fun testSelectionPersistenceAcrossSorting() {
        // Given multi-select mode with selected items
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        viewModel.toggleItemSelection("file2")
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When changing sort order
        composeTestRule
            .onNodeWithTag("sort_dropdown")
            .performClick()
        
        composeTestRule
            .onNodeWithTag("sort_by_size")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then selections should be maintained
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextEquals("2 selected")
    }

    @Test
    fun testBulkSelectionPerformance() {
        // Given multi-select mode with many items
        repository.setAuthenticated(true)
        
        // Add more test items for performance testing
        repeat(20) { index ->
            repository.addTestFile(
                FileItem.File(
                    id = "perf_file_$index",
                    name = "Performance Test File $index.mp4",
                    size = 1024L * index,
                    modifiedDate = System.currentTimeMillis(),
                    mimeType = "video/mp4",
                    downloadUrl = "url$index",
                    streamUrl = "stream$index",
                    isPlayable = true,
                    status = FileStatus.READY
                )
            )
        }
        
        viewModel.toggleMultiSelect()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When selecting all items
        val startTime = System.currentTimeMillis()
        
        composeTestRule
            .onNodeWithTag("select_all_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Then selection should complete quickly
        assert(duration < 1000) { "Bulk selection took too long: ${duration}ms" }
        
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertTextContains("selected")
    }

    @Test
    fun testSelectionLimits() {
        // Given multi-select mode
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When selecting all available items
        composeTestRule
            .onNodeWithTag("select_all_button")
            .performClick()
        
        // Then should handle large selections gracefully
        composeTestRule
            .onNodeWithTag("selection_count")
            .assertExists()
        
        // Action buttons should still be responsive
        composeTestRule
            .onNodeWithTag("action_download")
            .assertIsEnabled()
    }

    @Composable
    private fun TestFileBrowserScreen() {
        // This would be a simplified version of the actual AccountFileBrowserScreen
        // focusing on the bulk selection UI elements
        AccountFileBrowserScreen(
            modifier = Modifier
                .fillMaxSize()
                .testTag("file_browser_screen"),
            viewModel = viewModel,
            onNavigateToPlayer = { _, _ -> },
            onNavigateBack = { },
            onShowError = { },
            onShowSuccess = { }
        )
    }
}