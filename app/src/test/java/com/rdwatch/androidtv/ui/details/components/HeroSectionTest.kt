package com.rdwatch.androidtv.ui.details.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.ui.details.models.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeroSectionTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun heroSection_displaysContentTitle() {
        val testContent = createTestMovieContent()
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Test Movie Title").assertIsDisplayed()
    }
    
    @Test
    fun heroSection_displaysBackButton() {
        val testContent = createTestMovieContent()
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }
    
    @Test
    fun heroSection_displaysMetadataChips() {
        val testContent = createTestMovieContent()
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        // Check for rating chip
        composeTestRule.onNodeWithText("8.5/10").assertIsDisplayed()
        
        // Check for quality chip
        composeTestRule.onNodeWithText("4K").assertIsDisplayed()
        
        // Check for year chip
        composeTestRule.onNodeWithText("2023").assertIsDisplayed()
    }
    
    @Test
    fun heroSection_displaysProgressWhenPartiallyWatched() {
        val testContent = createTestMovieContent()
        val progress = ContentProgress(
            watchPercentage = 0.6f,
            isCompleted = false,
            resumePosition = 3600000L,
            totalDuration = 6000000L
        )
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                progress = progress,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("60% watched").assertIsDisplayed()
    }
    
    @Test
    fun heroSection_displaysResumeButtonWhenPartiallyWatched() {
        val testContent = createTestMovieContent()
        val progress = ContentProgress(
            watchPercentage = 0.35f,
            isCompleted = false,
            resumePosition = 2100000L,
            totalDuration = 6000000L
        )
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                progress = progress,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Resume Playing").assertIsDisplayed()
    }
    
    @Test
    fun heroSection_displaysPlayButtonWhenNotWatched() {
        val testContent = createTestMovieContent()
        val progress = ContentProgress() // No progress
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                progress = progress,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Play").assertIsDisplayed()
    }
    
    @Test
    fun heroSection_movieContentDisplaysMovieSpecificMetadata() {
        val testContent = createTestMovieContent()
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        // Check for movie-specific metadata
        composeTestRule.onNodeWithText("2h 30m").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Studio").assertIsDisplayed()
    }
    
    @Test
    fun heroSection_tvShowContentDisplaysTvSpecificMetadata() {
        val testContent = createTestTvShowContent()
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        // Check for TV show-specific metadata
        composeTestRule.onNodeWithText("3 Seasons").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 Episodes").assertIsDisplayed()
    }
    
    @Test
    fun heroSection_episodeContentDisplaysEpisodeSpecificMetadata() {
        val testContent = createTestEpisodeContent()
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        // Check for episode-specific metadata
        composeTestRule.onNodeWithText("S2E5").assertIsDisplayed()
        composeTestRule.onNodeWithText("45m").assertIsDisplayed()
    }
    
    @Test
    fun heroSection_handlesBackButtonClick() {
        val testContent = createTestMovieContent()
        var backClicked = false
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                onActionClick = {},
                onBackPressed = { backClicked = true }
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(backClicked)
    }
    
    @Test
    fun heroSection_handlesActionButtonClick() {
        val testContent = createTestMovieContent()
        var actionClicked = false
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                onActionClick = { actionClicked = true },
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Play").performClick()
        assert(actionClicked)
    }
    
    @Test
    fun heroSection_displaysWatchedIndicatorWhenCompleted() {
        val testContent = createTestMovieContent()
        val progress = ContentProgress(
            watchPercentage = 1.0f,
            isCompleted = true,
            resumePosition = 6000000L,
            totalDuration = 6000000L
        )
        
        composeTestRule.setContent {
            HeroSection(
                content = testContent,
                progress = progress,
                onActionClick = {},
                onBackPressed = {}
            )
        }
        
        composeTestRule.onNodeWithText("Watched").assertIsDisplayed()
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
    
    private fun createTestEpisodeContent(): ContentDetail {
        return object : ContentDetail {
            override val id: String = "test-episode-1"
            override val title: String = "Test Episode"
            override val description: String = "This is a test episode description."
            override val backgroundImageUrl: String = "https://example.com/episode-backdrop.jpg"
            override val cardImageUrl: String = "https://example.com/episode-poster.jpg"
            override val contentType: ContentType = ContentType.TV_EPISODE
            override val videoUrl: String = "https://example.com/episode.mp4"
            override val metadata: ContentMetadata = ContentMetadata(
                year = "2022",
                duration = "45m",
                rating = "8.2/10",
                season = 2,
                episode = 5,
                quality = "HD",
                is4K = false,
                isHDR = true
            )
            override val actions: List<ContentAction> = listOf(
                ContentAction.Play()
            )
        }
    }
}