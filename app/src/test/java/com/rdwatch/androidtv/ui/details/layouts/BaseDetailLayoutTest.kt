package com.rdwatch.androidtv.ui.details.layouts

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.ui.details.models.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaseDetailLayoutTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun baseDetailLayout_displaysLoadingState() {
        val loadingState = DetailUiState(
            content = null,
            isLoading = true,
            isLoaded = false
        )
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = loadingState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Loading content details...").assertIsDisplayed()
    }
    
    @Test
    fun baseDetailLayout_displaysErrorState() {
        val errorState = DetailUiState(
            content = null,
            isLoading = false,
            error = "Network error"
        )
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = errorState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Failed to load content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
    }
    
    @Test
    fun baseDetailLayout_displaysContentNotFoundState() {
        val notFoundState = DetailUiState(
            content = null,
            isLoading = false,
            isLoaded = true
        )
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = notFoundState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Content not found").assertIsDisplayed()
    }
    
    @Test
    fun baseDetailLayout_displaysContentWhenLoaded() {
        val content = createTestMovieContent()
        val loadedState = DetailUiState(
            content = content,
            isLoading = false,
            isLoaded = true
        )
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = loadedState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Test Movie Title").assertIsDisplayed()
    }
    
    @Test
    fun baseDetailLayout_displaysHeroSectionForMovie() {
        val content = createTestMovieContent()
        val loadedState = DetailUiState(
            content = content,
            isLoading = false,
            isLoaded = true
        )
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = loadedState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        // Hero section should be displayed
        composeTestRule.onNodeWithText("Test Movie Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Play").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }
    
    @Test
    fun baseDetailLayout_displaysInfoSectionForMovie() {
        val content = createTestMovieContent()
        val loadedState = DetailUiState(
            content = content,
            isLoading = false,
            isLoaded = true
        )
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = loadedState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        // Info section should be displayed
        composeTestRule.onNodeWithText("Info Section").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a test movie description for testing purposes.").assertIsDisplayed()
    }
    
    @Test
    fun baseDetailLayout_displaysActionsSectionForMovie() {
        val content = createTestMovieContent()
        val loadedState = DetailUiState(
            content = content,
            isLoading = false,
            isLoaded = true
        )
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = loadedState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        // Actions section should be displayed
        composeTestRule.onNodeWithText("Actions: 3 available").assertIsDisplayed()
    }
    
    @Test
    fun baseDetailLayout_displaysProgressInHeroSection() {
        val content = createTestMovieContent()
        val progress = ContentProgress(
            watchPercentage = 0.4f,
            isCompleted = false,
            resumePosition = 2400000L,
            totalDuration = 6000000L
        )
        val loadedState = DetailUiState(
            content = content,
            progress = progress,
            isLoading = false,
            isLoaded = true
        )
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = loadedState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        // Progress should be displayed in hero section
        composeTestRule.onNodeWithText("40% watched").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resume Playing").assertIsDisplayed()
    }
    
    @Test
    fun baseDetailLayout_handlesBackButtonClick() {
        val content = createTestMovieContent()
        val loadedState = DetailUiState(
            content = content,
            isLoading = false,
            isLoaded = true
        )
        
        var backClicked = false
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = loadedState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = { backClicked = true }
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(backClicked)
    }
    
    @Test
    fun baseDetailLayout_handlesActionClick() {
        val content = createTestMovieContent()
        val loadedState = DetailUiState(
            content = content,
            isLoading = false,
            isLoaded = true
        )
        
        var actionClicked = false
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = loadedState,
                onActionClick = { actionClicked = true },
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Play").performClick()
        assert(actionClicked)
    }
    
    @Test
    fun baseDetailLayout_displaysCorrectSectionsForTVShow() {
        val content = createTestTvShowContent()
        val loadedState = DetailUiState(
            content = content,
            isLoading = false,
            isLoaded = true
        )
        
        composeTestRule.setContent {
            BaseDetailLayout(
                uiState = loadedState,
                onActionClick = {},
                onRelatedContentClick = {},
                onBackPressed = {}
            )
        }
        
        // Hero section should be displayed
        composeTestRule.onNodeWithText("Test TV Show").assertIsDisplayed()
        
        // Should show seasons info
        composeTestRule.onNodeWithText("3 Seasons").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 Episodes").assertIsDisplayed()
    }
    
    // Helper functions to create test content
    private fun createTestMovieContent(): ContentDetail {
        return object : ContentDetail {
            override val id: String = "test-movie-1"
            override val title: String = "Test Movie Title"
            override val description: String = "This is a test movie description for testing purposes."
            override val backgroundImageUrl: String = "https://example.com/backdrop.jpg"
            override val cardImageUrl: String = "https://example.com/poster.jpg"
            override val contentType: ContentType = ContentType.MOVIE
            override val videoUrl: String = "https://example.com/movie.mp4"
            override val metadata: ContentMetadata = ContentMetadata(
                year = "2023",
                duration = "2h 30m",
                rating = "8.5/10",
                studio = "Test Studio",
                quality = "HD",
                is4K = true,
                isHDR = true
            )
            override val actions: List<ContentAction> = listOf(
                ContentAction.Play(),
                ContentAction.AddToWatchlist(),
                ContentAction.Like()
            )
        }
    }
    
    private fun createTestTvShowContent(): ContentDetail {
        return object : ContentDetail {
            override val id: String = "test-tv-1"
            override val title: String = "Test TV Show"
            override val description: String = "This is a test TV show description."
            override val backgroundImageUrl: String = "https://example.com/tv-backdrop.jpg"
            override val cardImageUrl: String = "https://example.com/tv-poster.jpg"
            override val contentType: ContentType = ContentType.TV_SHOW
            override val videoUrl: String = "https://example.com/tv-show.mp4"
            override val metadata: ContentMetadata = ContentMetadata(
                year = "2022",
                rating = "7.8/10",
                studio = "Test Network",
                quality = "4K",
                is4K = true,
                isHDR = false,
                customMetadata = mapOf(
                    "seasons" to "3",
                    "episodes" to "30"
                )
            )
            override val actions: List<ContentAction> = listOf(
                ContentAction.Play(),
                ContentAction.AddToWatchlist()
            )
        }
    }
}