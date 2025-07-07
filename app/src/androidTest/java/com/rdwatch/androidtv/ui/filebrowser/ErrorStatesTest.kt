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
 * UI tests for error state handling in the Account File Browser.
 * Tests various error scenarios and recovery mechanisms.
 */
@HiltAndroidTest
@UninstallModules(/* Add actual modules to uninstall */)
@RunWith(AndroidJUnit4::class)
class ErrorStatesTest : HiltInstrumentedTestBase() {

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
    fun testAuthenticationErrorState() {
        // Given unauthenticated user
        repository.setAuthenticated(false)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Then should show authentication error
        composeTestRule
            .onNodeWithTag("error_state")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Authentication required")
            .assertExists()
        
        // Should show sign in option
        composeTestRule
            .onNodeWithTag("sign_in_button")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testNetworkErrorState() {
        // Given network error
        repository.setAuthenticated(true)
        repository.setReturnError(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Then should show network error
        composeTestRule
            .onNodeWithTag("error_state")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Network error")
            .assertExists()
        
        // Should show retry button
        composeTestRule
            .onNodeWithTag("retry_button")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testEmptyStateDisplay() {
        // Given empty content
        repository.setAuthenticated(true)
        repository.setReturnEmpty(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Then should show empty state
        composeTestRule
            .onNodeWithTag("empty_state")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("No files found")
            .assertExists()
        
        // Should show refresh option
        composeTestRule
            .onNodeWithTag("refresh_button")
            .assertExists()
    }

    @Test
    fun testRetryAfterNetworkError() {
        // Given network error initially
        repository.setAuthenticated(true)
        repository.setReturnError(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When error is shown
        composeTestRule
            .onNodeWithTag("error_state")
            .assertExists()
        
        // And network is restored
        repository.setReturnError(false)
        
        // When clicking retry
        composeTestRule
            .onNodeWithTag("retry_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then should load content successfully
        composeTestRule
            .onNodeWithTag("error_state")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithTag("file_item_0")
            .assertExists()
    }

    @Test
    fun testSearchErrorHandling() {
        // Given successful initial load
        repository.setAuthenticated(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When search fails
        repository.setReturnError(true)
        
        composeTestRule
            .onNodeWithTag("search_field")
            .performTextInput("test query")
        
        composeTestRule.waitForIdle()
        
        // Then should show search error
        composeTestRule
            .onNodeWithTag("search_error")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Search failed")
            .assertExists()
        
        // Should still show previous content
        composeTestRule
            .onNodeWithTag("file_item_0")
            .assertExists()
    }

    @Test
    fun testFilePlaybackError() {
        // Given content loaded
        repository.setAuthenticated(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When playback URL fetch fails
        repository.setReturnError(true)
        
        composeTestRule
            .onNodeWithTag("file_item_0") // Video file
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then should show playback error
        composeTestRule
            .onNodeWithTag("error_snackbar")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Failed to get playback URL")
            .assertExists()
    }

    @Test
    fun testDownloadErrorHandling() {
        // Given content loaded and multi-select mode
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When download fails
        repository.setReturnError(true)
        
        composeTestRule
            .onNodeWithTag("action_download")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then should show download error
        composeTestRule
            .onNodeWithTag("error_snackbar")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Download failed")
            .assertExists()
    }

    @Test
    fun testDeleteErrorHandling() {
        // Given content loaded and multi-select mode
        repository.setAuthenticated(true)
        viewModel.toggleMultiSelect()
        viewModel.toggleItemSelection("file1")
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When delete fails
        repository.setReturnError(true)
        
        composeTestRule
            .onNodeWithTag("action_delete")
            .performClick()
        
        // Confirm deletion
        composeTestRule
            .onNodeWithTag("confirm_delete_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then should show delete error
        composeTestRule
            .onNodeWithTag("error_snackbar")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Delete failed")
            .assertExists()
    }

    @Test
    fun testLoadingStateHandling() {
        // Given network delay
        repository.setAuthenticated(true)
        repository.setNetworkDelay(2000) // 2 second delay
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        // When content is loading
        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Loading...")
            .assertExists()
        
        // Should not show error state during loading
        composeTestRule
            .onNodeWithTag("error_state")
            .assertDoesNotExist()
        
        // Should eventually load content
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithTag("file_item_0")
            .assertExists()
    }

    @Test
    fun testTimeoutErrorHandling() {
        // Given very long network delay (simulating timeout)
        repository.setAuthenticated(true)
        repository.setNetworkDelay(10000) // 10 second delay
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        // Should show loading initially
        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertExists()
        
        // After some time, should show timeout option
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("timeout_message")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        
        composeTestRule
            .onNodeWithTag("timeout_message")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("cancel_loading_button")
            .assertExists()
    }

    @Test
    fun testErrorMessageDisplay() {
        // Given various error types
        val errorScenarios = listOf(
            "Network error",
            "Authentication required",
            "Server error",
            "File not found",
            "Permission denied"
        )
        
        errorScenarios.forEach { errorMessage ->
            // Reset repository
            repository.resetTestState()
            repository.setAuthenticated(true)
            repository.setReturnError(true)
            
            composeTestRule.setContent {
                TestFileBrowserScreen()
            }
            
            composeTestRule.waitForIdle()
            
            // Should show error message
            composeTestRule
                .onNodeWithTag("error_state")
                .assertExists()
            
            // Should have accessible error text
            composeTestRule
                .onNodeWithTag("error_message")
                .assertExists()
        }
    }

    @Test
    fun testErrorStateAccessibility() {
        // Given error state
        repository.setAuthenticated(true)
        repository.setReturnError(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Then error state should be accessible
        composeTestRule
            .onNodeWithTag("error_state")
            .assert(hasContentDescription())
        
        composeTestRule
            .onNodeWithTag("retry_button")
            .assert(hasContentDescription())
        
        // Should announce error to screen readers
        composeTestRule
            .onNodeWithTag("error_message")
            .assert(hasSetTextAction())
    }

    @Test
    fun testPartialLoadingErrorRecovery() {
        // Given successful initial load
        repository.setAuthenticated(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // When pagination fails
        repository.setReturnError(true)
        
        composeTestRule
            .onNodeWithTag("load_more_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then should show pagination error but keep existing content
        composeTestRule
            .onNodeWithTag("file_item_0")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("pagination_error")
            .assertExists()
        
        composeTestRule
            .onNodeWithTag("retry_pagination_button")
            .assertExists()
    }

    @Test
    fun testErrorStateFocusManagement() {
        // Given error state
        repository.setAuthenticated(true)
        repository.setReturnError(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Then retry button should receive focus
        composeTestRule
            .onNodeWithTag("retry_button")
            .assertIsFocused()
        
        // Should be able to navigate to other available controls
        composeTestRule
            .onNodeWithTag("retry_button")
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionUp)
            }
        
        composeTestRule
            .onNodeWithTag("search_field")
            .assertIsFocused()
    }

    @Test
    fun testMultipleErrorTypesHandling() {
        // Given authentication error first
        repository.setAuthenticated(false)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Should show authentication error
        composeTestRule
            .onNodeWithText("Authentication required")
            .assertExists()
        
        // When authenticated but network fails
        repository.setAuthenticated(true)
        repository.setReturnError(true)
        
        // Trigger retry
        composeTestRule
            .onNodeWithTag("retry_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Should show network error
        composeTestRule
            .onNodeWithText("Network error")
            .assertExists()
        
        // Should not show authentication error anymore
        composeTestRule
            .onNodeWithText("Authentication required")
            .assertDoesNotExist()
    }

    @Test
    fun testErrorStateWithOfflineMode() {
        // Given offline scenario
        repository.setAuthenticated(true)
        repository.setReturnError(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Should show offline-friendly error message
        composeTestRule
            .onNodeWithTag("error_state")
            .assertExists()
        
        // Should offer offline options if available
        composeTestRule
            .onNodeWithTag("offline_mode_button")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Use cached content")
            .assertExists()
    }

    @Test
    fun testErrorRecoveryAfterSuccessfulRetry() {
        // Given initial error
        repository.setAuthenticated(true)
        repository.setReturnError(true)
        
        composeTestRule.setContent {
            TestFileBrowserScreen()
        }
        
        composeTestRule.waitForIdle()
        
        // Verify error state
        composeTestRule
            .onNodeWithTag("error_state")
            .assertExists()
        
        // When network is restored
        repository.setReturnError(false)
        
        // And retry is clicked
        composeTestRule
            .onNodeWithTag("retry_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then should fully recover
        composeTestRule
            .onNodeWithTag("error_state")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithTag("file_item_0")
            .assertExists()
        
        // All functionality should work normally
        composeTestRule
            .onNodeWithTag("search_field")
            .assertIsEnabled()
        
        composeTestRule
            .onNodeWithTag("sort_dropdown")
            .assertIsEnabled()
    }

    @Composable
    private fun TestFileBrowserScreen() {
        // This would be a simplified version of the actual AccountFileBrowserScreen
        // focusing on error state handling
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