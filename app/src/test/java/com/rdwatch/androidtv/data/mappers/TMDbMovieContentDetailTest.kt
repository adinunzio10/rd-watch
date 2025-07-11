package com.rdwatch.androidtv.data.mappers

import com.rdwatch.androidtv.test.data.TMDbTestDataFactory
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.ContentAction
import com.rdwatch.androidtv.ui.details.models.ContentProgress
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for TMDbMovieContentDetail
 * Tests ContentDetail implementation for TMDb movies
 */
class TMDbMovieContentDetailTest {
    
    @Test
    fun `TMDbMovieContentDetail implements ContentDetail interface correctly`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Assert
        assertEquals(movieResponse.id.toString(), contentDetail.id)
        assertEquals(movieResponse.title, contentDetail.title)
        assertEquals(movieResponse.overview, contentDetail.description)
        assertEquals(ContentType.MOVIE, contentDetail.contentType)
        assertNotNull(contentDetail.backgroundImageUrl)
        assertNotNull(contentDetail.cardImageUrl)
        assertNull(contentDetail.videoUrl) // TMDb doesn't provide video URLs
        assertNotNull(contentDetail.metadata)
        assertNotNull(contentDetail.actions)
    }
    
    @Test
    fun `getDisplayTitle returns movie title`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val displayTitle = contentDetail.getDisplayTitle()
        
        // Assert
        assertEquals(movieResponse.title, displayTitle)
    }
    
    @Test
    fun `getDisplayDescription returns overview or fallback`() {
        // Test with overview
        val movieWithOverview = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetailWithOverview = TMDbTestDataFactory.createTMDbMovieContentDetail(movieWithOverview)
        
        assertEquals(movieWithOverview.overview, contentDetailWithOverview.getDisplayDescription())
        
        // Test without overview
        val movieWithoutOverview = TMDbTestDataFactory.createTMDbMovieResponse().copy(overview = null)
        val contentDetailWithoutOverview = TMDbTestDataFactory.createTMDbMovieContentDetail(movieWithoutOverview)
        
        assertEquals("No description available", contentDetailWithoutOverview.getDisplayDescription())
    }
    
    @Test
    fun `getPrimaryImageUrl returns backdrop or poster`() {
        // Test with backdrop
        val movieWithBackdrop = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetailWithBackdrop = TMDbTestDataFactory.createTMDbMovieContentDetail(movieWithBackdrop)
        
        assertEquals(contentDetailWithBackdrop.backgroundImageUrl, contentDetailWithBackdrop.getPrimaryImageUrl())
        
        // Test without backdrop but with poster
        val movieWithoutBackdrop = TMDbTestDataFactory.createTMDbMovieResponse().copy(backdropPath = null)
        val contentDetailWithoutBackdrop = TMDbTestDataFactory.createTMDbMovieContentDetail(movieWithoutBackdrop)
        
        assertEquals(contentDetailWithoutBackdrop.cardImageUrl, contentDetailWithoutBackdrop.getPrimaryImageUrl())
        
        // Test without both
        val movieWithoutImages = TMDbTestDataFactory.createTMDbMovieResponse().copy(
            backdropPath = null,
            posterPath = null
        )
        val contentDetailWithoutImages = TMDbTestDataFactory.createTMDbMovieContentDetail(movieWithoutImages)
        
        assertNull(contentDetailWithoutImages.getPrimaryImageUrl())
    }
    
    @Test
    fun `isPlayable returns false for TMDb movies`() {
        // Arrange
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail()
        
        // Act & Assert
        assertFalse(contentDetail.isPlayable())
    }
    
    @Test
    fun `metadata contains correct movie information`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val metadata = contentDetail.metadata
        
        // Assert
        assertEquals("1999", metadata.year)
        assertEquals("2h 19m", metadata.duration)
        assertEquals("8.4", metadata.rating)
        assertEquals("en", metadata.language)
        assertEquals(listOf("Drama", "Thriller"), metadata.genre)
        assertEquals("Regency Enterprises", metadata.studio)
        assertEquals(listOf("Edward Norton", "Brad Pitt"), metadata.cast)
        assertEquals("David Fincher", metadata.director)
    }
    
    @Test
    fun `actions contain expected movie actions`() {
        // Arrange
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail()
        
        // Act
        val actions = contentDetail.actions
        
        // Assert
        assertTrue(actions.isNotEmpty())
        assertTrue(actions.any { it is ContentAction.Play })
        assertTrue(actions.any { it is ContentAction.AddToWatchlist })
        assertTrue(actions.any { it is ContentAction.Like })
        assertTrue(actions.any { it is ContentAction.Share })
        assertTrue(actions.any { it is ContentAction.Download })
    }
    
    @Test
    fun `actions reflect current state`() {
        // Test with different states
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(
            progress = TMDbTestDataFactory.createContentProgress(watchPercentage = 0.5f),
            isInWatchlist = true,
            isLiked = true,
            isDownloaded = true
        )
        
        val actions = contentDetail.actions
        
        // Check Play action shows as Resume
        val playAction = actions.find { it is ContentAction.Play } as ContentAction.Play
        assertEquals("Resume", playAction.title)
        
        // Check Watchlist action shows as Remove
        val watchlistAction = actions.find { it is ContentAction.AddToWatchlist } as ContentAction.AddToWatchlist
        assertEquals("Remove from Watchlist", watchlistAction.title)
        
        // Check Like action shows as Unlike
        val likeAction = actions.find { it is ContentAction.Like } as ContentAction.Like
        assertEquals("Unlike", likeAction.title)
        
        // Check Download action shows as Downloaded
        val downloadAction = actions.find { it is ContentAction.Download } as ContentAction.Download
        assertEquals("Downloaded", downloadAction.title)
    }
    
    @Test
    fun `getTMDbMovie returns original movie response`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val tmdbMovie = contentDetail.getTMDbMovie()
        
        // Assert
        assertEquals(movieResponse, tmdbMovie)
    }
    
    @Test
    fun `getCredits returns credits information`() {
        // Arrange
        val credits = TMDbTestDataFactory.createTMDbCreditsResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(credits = credits)
        
        // Act
        val retrievedCredits = contentDetail.getCredits()
        
        // Assert
        assertEquals(credits, retrievedCredits)
    }
    
    @Test
    fun `getProgress returns progress information`() {
        // Arrange
        val progress = TMDbTestDataFactory.createContentProgress()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(progress = progress)
        
        // Act
        val retrievedProgress = contentDetail.getProgress()
        
        // Assert
        assertEquals(progress, retrievedProgress)
    }
    
    @Test
    fun `getTMDbId returns TMDb movie ID`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val tmdbId = contentDetail.getTMDbId()
        
        // Assert
        assertEquals(movieResponse.id, tmdbId)
    }
    
    @Test
    fun `getImdbId returns IMDb ID`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val imdbId = contentDetail.getImdbId()
        
        // Assert
        assertEquals(movieResponse.imdbId, imdbId)
    }
    
    @Test
    fun `getFormattedReleaseDate returns release date`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val releaseDate = contentDetail.getFormattedReleaseDate()
        
        // Assert
        assertEquals(movieResponse.releaseDate, releaseDate)
    }
    
    @Test
    fun `getFormattedVoteAverage returns formatted rating`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val formattedRating = contentDetail.getFormattedVoteAverage()
        
        // Assert
        assertEquals("8.4", formattedRating)
    }
    
    @Test
    fun `getVoteCount returns vote count`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val voteCount = contentDetail.getVoteCount()
        
        // Assert
        assertEquals(movieResponse.voteCount, voteCount)
    }
    
    @Test
    fun `getPopularity returns popularity score`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val popularity = contentDetail.getPopularity()
        
        // Assert
        assertEquals(movieResponse.popularity, popularity)
    }
    
    @Test
    fun `getBudget returns budget`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val budget = contentDetail.getBudget()
        
        // Assert
        assertEquals(movieResponse.budget, budget)
    }
    
    @Test
    fun `getRevenue returns revenue`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val revenue = contentDetail.getRevenue()
        
        // Assert
        assertEquals(movieResponse.revenue, revenue)
    }
    
    @Test
    fun `getStatus returns status`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val status = contentDetail.getStatus()
        
        // Assert
        assertEquals(movieResponse.status, status)
    }
    
    @Test
    fun `getTagline returns tagline`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val tagline = contentDetail.getTagline()
        
        // Assert
        assertEquals(movieResponse.tagline, tagline)
    }
    
    @Test
    fun `getHomepage returns homepage URL`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val homepage = contentDetail.getHomepage()
        
        // Assert
        assertEquals(movieResponse.homepage, homepage)
    }
    
    @Test
    fun `isAdultContent returns adult flag`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val isAdult = contentDetail.isAdultContent()
        
        // Assert
        assertEquals(movieResponse.adult, isAdult)
    }
    
    @Test
    fun `getOriginalTitle returns original title`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val originalTitle = contentDetail.getOriginalTitle()
        
        // Assert
        assertEquals(movieResponse.originalTitle, originalTitle)
    }
    
    @Test
    fun `getOriginalLanguage returns original language`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val originalLanguage = contentDetail.getOriginalLanguage()
        
        // Assert
        assertEquals(movieResponse.originalLanguage, originalLanguage)
    }
    
    @Test
    fun `hasVideoContent returns video flag`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(movieResponse)
        
        // Act
        val hasVideo = contentDetail.hasVideoContent()
        
        // Assert
        assertEquals(movieResponse.video, hasVideo)
    }
    
    @Test
    fun `withProgress creates new instance with updated progress`() {
        // Arrange
        val originalProgress = TMDbTestDataFactory.createContentProgress(watchPercentage = 0.3f)
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(progress = originalProgress)
        val newProgress = TMDbTestDataFactory.createContentProgress(watchPercentage = 0.7f)
        
        // Act
        val updatedContentDetail = contentDetail.withProgress(newProgress)
        
        // Assert
        assertEquals(newProgress, updatedContentDetail.getProgress())
        assertEquals(originalProgress, contentDetail.getProgress()) // Original unchanged
    }
    
    @Test
    fun `withWatchlistStatus creates new instance with updated watchlist status`() {
        // Arrange
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(isInWatchlist = false)
        
        // Act
        val updatedContentDetail = contentDetail.withWatchlistStatus(true)
        
        // Assert
        val watchlistAction = updatedContentDetail.actions.find { it is ContentAction.AddToWatchlist } as ContentAction.AddToWatchlist
        assertEquals("Remove from Watchlist", watchlistAction.title)
        
        // Original should be unchanged
        val originalWatchlistAction = contentDetail.actions.find { it is ContentAction.AddToWatchlist } as ContentAction.AddToWatchlist
        assertEquals("Add to Watchlist", originalWatchlistAction.title)
    }
    
    @Test
    fun `withLikeStatus creates new instance with updated like status`() {
        // Arrange
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(isLiked = false)
        
        // Act
        val updatedContentDetail = contentDetail.withLikeStatus(true)
        
        // Assert
        val likeAction = updatedContentDetail.actions.find { it is ContentAction.Like } as ContentAction.Like
        assertEquals("Unlike", likeAction.title)
        
        // Original should be unchanged
        val originalLikeAction = contentDetail.actions.find { it is ContentAction.Like } as ContentAction.Like
        assertEquals("Like", originalLikeAction.title)
    }
    
    @Test
    fun `withDownloadStatus creates new instance with updated download status`() {
        // Arrange
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(isDownloaded = false, isDownloading = false)
        
        // Act
        val updatedContentDetail = contentDetail.withDownloadStatus(downloaded = true, downloading = false)
        
        // Assert
        val downloadAction = updatedContentDetail.actions.find { it is ContentAction.Download } as ContentAction.Download
        assertEquals("Downloaded", downloadAction.title)
        
        // Original should be unchanged
        val originalDownloadAction = contentDetail.actions.find { it is ContentAction.Download } as ContentAction.Download
        assertEquals("Download", originalDownloadAction.title)
    }
    
    @Test
    fun `withCredits creates new instance with updated credits`() {
        // Arrange
        val originalCredits = TMDbTestDataFactory.createTMDbCreditsResponse()
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(credits = originalCredits)
        val newCredits = TMDbTestDataFactory.createTMDbCreditsResponse().copy(id = 999)
        
        // Act
        val updatedContentDetail = contentDetail.withCredits(newCredits)
        
        // Assert
        assertEquals(newCredits, updatedContentDetail.getCredits())
        assertEquals(originalCredits, contentDetail.getCredits()) // Original unchanged
    }
}