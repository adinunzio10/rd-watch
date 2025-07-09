package com.rdwatch.androidtv.data.mappers

import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.test.data.TMDbTestDataFactory
import com.rdwatch.androidtv.ui.details.models.ContentType
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for TMDbToContentDetailMapper
 * Tests mapping from TMDb DTOs to ContentDetail models
 */
class TMDbToContentDetailMapperTest {
    
    private lateinit var mapper: TMDbToContentDetailMapper
    
    @Before
    fun setUp() {
        mapper = TMDbToContentDetailMapper()
    }
    
    @Test
    fun `mapMovieToContentDetail maps basic movie properties correctly`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse)
        
        // Assert
        assertEquals("tmdb_movie_${movieResponse.id}", contentDetail.id)
        assertEquals(movieResponse.title, contentDetail.title)
        assertEquals(movieResponse.overview, contentDetail.description)
        assertEquals(ContentType.MOVIE, contentDetail.contentType)
        assertNull(contentDetail.videoUrl) // TMDb doesn't provide direct video URLs
    }
    
    @Test
    fun `mapMovieToContentDetail maps image URLs correctly`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val expectedBackdropUrl = "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}${movieResponse.backdropPath}"
        val expectedPosterUrl = "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}${movieResponse.posterPath}"
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse)
        
        // Assert
        assertEquals(expectedBackdropUrl, contentDetail.backgroundImageUrl)
        assertEquals(expectedPosterUrl, contentDetail.cardImageUrl)
    }
    
    @Test
    fun `mapMovieToContentDetail handles null image paths`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse().copy(
            backdropPath = null,
            posterPath = null
        )
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse)
        
        // Assert
        assertNull(contentDetail.backgroundImageUrl)
        assertNull(contentDetail.cardImageUrl)
    }
    
    @Test
    fun `mapMovieToContentDetail maps metadata correctly`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        assertEquals("1999", contentDetail.metadata.year)
        assertEquals("2h 19m", contentDetail.metadata.duration)
        assertEquals("8.4/10", contentDetail.metadata.rating)
        assertEquals("en", contentDetail.metadata.language)
        assertEquals(listOf("Drama", "Thriller"), contentDetail.metadata.genre)
        assertEquals("Regency Enterprises", contentDetail.metadata.studio)
    }
    
    @Test
    fun `mapMovieToContentDetail handles empty genres list`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse().copy(
            genres = emptyList()
        )
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        assertTrue(contentDetail.metadata.genre.isEmpty())
    }
    
    @Test
    fun `mapMovieToContentDetail handles empty production companies`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse().copy(
            productionCompanies = emptyList()
        )
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        assertNull(contentDetail.metadata.studio)
    }
    
    @Test
    fun `mapMovieToContentDetail creates correct custom metadata`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        assertEquals(movieResponse.id.toString(), contentDetail.metadata.customMetadata["tmdb_id"])
        assertEquals(movieResponse.imdbId, contentDetail.metadata.customMetadata["imdb_id"])
        assertEquals(movieResponse.voteCount.toString(), contentDetail.metadata.customMetadata["vote_count"])
        assertEquals(movieResponse.popularity.toString(), contentDetail.metadata.customMetadata["popularity"])
        assertEquals(movieResponse.tagline, contentDetail.metadata.customMetadata["tagline"])
        assertEquals(movieResponse.status, contentDetail.metadata.customMetadata["status"])
        assertEquals(movieResponse.adult.toString(), contentDetail.metadata.customMetadata["adult"])
        assertEquals(movieResponse.originalTitle, contentDetail.metadata.customMetadata["original_title"])
        assertEquals(movieResponse.originalLanguage, contentDetail.metadata.customMetadata["original_language"])
    }
    
    @Test
    fun `mapMovieToContentDetail handles null values in custom metadata`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse().copy(
            imdbId = null,
            tagline = null,
            homepage = null,
            belongsToCollection = null
        )
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        assertEquals("", contentDetail.metadata.customMetadata["imdb_id"])
        assertEquals("", contentDetail.metadata.customMetadata["tagline"])
        assertEquals("", contentDetail.metadata.customMetadata["homepage"])
        assertEquals("", contentDetail.metadata.customMetadata["belongs_to_collection"])
    }
    
    @Test
    fun `mapTVToContentDetail maps basic TV properties correctly`() {
        // Arrange
        val tvResponse = TMDbTestDataFactory.createTMDbTVResponse()
        
        // Act
        val contentDetail = mapper.mapTVToContentDetail(tvResponse)
        
        // Assert
        assertEquals("tmdb_tv_${tvResponse.id}", contentDetail.id)
        assertEquals(tvResponse.name, contentDetail.title)
        assertEquals(tvResponse.overview, contentDetail.description)
        assertEquals(ContentType.TV_SHOW, contentDetail.contentType)
        assertNull(contentDetail.videoUrl) // TMDb doesn't provide direct video URLs
    }
    
    @Test
    fun `mapTVToContentDetail maps TV image URLs correctly`() {
        // Arrange
        val tvResponse = TMDbTestDataFactory.createTMDbTVResponse()
        val expectedBackdropUrl = "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}${tvResponse.backdropPath}"
        val expectedPosterUrl = "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}${tvResponse.posterPath}"
        
        // Act
        val contentDetail = mapper.mapTVToContentDetail(tvResponse)
        
        // Assert
        assertEquals(expectedBackdropUrl, contentDetail.backgroundImageUrl)
        assertEquals(expectedPosterUrl, contentDetail.cardImageUrl)
    }
    
    @Test
    fun `mapTVToContentDetail maps TV metadata correctly`() {
        // Arrange
        val tvResponse = TMDbTestDataFactory.createTMDbTVResponse()
        
        // Act
        val contentDetail = mapper.mapTVToContentDetail(tvResponse) as TMDbTVContentDetail
        
        // Assert
        assertEquals("2011", contentDetail.metadata.year)
        assertEquals("8.5/10", contentDetail.metadata.rating)
        assertEquals("en", contentDetail.metadata.language)
        assertEquals(listOf("Drama", "Action & Adventure", "Sci-Fi & Fantasy"), contentDetail.metadata.genre)
        assertEquals("Revolution Sun Studios", contentDetail.metadata.studio)
    }
    
    @Test
    fun `mapTVToContentDetail handles TV-specific properties`() {
        // Arrange
        val tvResponse = TMDbTestDataFactory.createTMDbTVResponse()
        
        // Act
        val contentDetail = mapper.mapTVToContentDetail(tvResponse) as TMDbTVContentDetail
        
        // Assert
        assertEquals(tvResponse.numberOfSeasons, contentDetail.getTMDbTV().numberOfSeasons)
        assertEquals(tvResponse.numberOfEpisodes, contentDetail.getTMDbTV().numberOfEpisodes)
        assertEquals(tvResponse.status, contentDetail.getTMDbTV().status)
        assertEquals(tvResponse.type, contentDetail.getTMDbTV().type)
        assertEquals(tvResponse.inProduction, contentDetail.getTMDbTV().inProduction)
    }
    
    @Test
    fun `mapTVToContentDetail handles null TV image paths`() {
        // Arrange
        val tvResponse = TMDbTestDataFactory.createTMDbTVResponse().copy(
            backdropPath = null,
            posterPath = null
        )
        
        // Act
        val contentDetail = mapper.mapTVToContentDetail(tvResponse)
        
        // Assert
        assertNull(contentDetail.backgroundImageUrl)
        assertNull(contentDetail.cardImageUrl)
    }
    
    @Test
    fun `formatRuntime returns correct format for various durations`() {
        // Test formatRuntime through mapping
        val testCases = listOf(
            45 to "45m",
            60 to "1h",
            90 to "1h 30m",
            120 to "2h",
            139 to "2h 19m",
            180 to "3h",
            0 to "0m"
        )
        
        testCases.forEach { (runtime, expected) ->
            // Arrange
            val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse().copy(runtime = runtime)
            
            // Act
            val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
            
            // Assert
            assertEquals(expected, contentDetail.metadata.duration, "Runtime $runtime should format to $expected")
        }
    }
    
    @Test
    fun `formatRuntime handles null runtime`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse().copy(runtime = null)
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        assertNull(contentDetail.metadata.duration)
    }
    
    @Test
    fun `formatRating returns correct format for various ratings`() {
        val testCases = listOf(
            0.0 to null,
            5.5 to "5.5/10",
            8.433 to "8.4/10",
            10.0 to "10.0/10",
            1.0 to "1.0/10"
        )
        
        testCases.forEach { (rating, expected) ->
            // Arrange
            val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse().copy(voteAverage = rating)
            
            // Act
            val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
            
            // Assert
            assertEquals(expected, contentDetail.metadata.rating, "Rating $rating should format to $expected")
        }
    }
    
    @Test
    fun `formatYear extracts year from release date correctly`() {
        val testCases = listOf(
            "1999-10-15" to "1999",
            "2021-01-01" to "2021",
            "2000-12-31" to "2000",
            null to null,
            "" to null
        )
        
        testCases.forEach { (releaseDate, expected) ->
            // Arrange
            val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse().copy(releaseDate = releaseDate)
            
            // Act
            val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
            
            // Assert
            assertEquals(expected, contentDetail.metadata.year, "Release date $releaseDate should extract year $expected")
        }
    }
    
    @Test
    fun `formatYear handles TV first air date correctly`() {
        val testCases = listOf(
            "2011-04-17" to "2011",
            "2020-01-01" to "2020",
            null to null,
            "" to null
        )
        
        testCases.forEach { (firstAirDate, expected) ->
            // Arrange
            val tvResponse = TMDbTestDataFactory.createTMDbTVResponse().copy(firstAirDate = firstAirDate)
            
            // Act
            val contentDetail = mapper.mapTVToContentDetail(tvResponse) as TMDbTVContentDetail
            
            // Assert
            assertEquals(expected, contentDetail.metadata.year, "First air date $firstAirDate should extract year $expected")
        }
    }
    
    @Test
    fun `mapper handles complex production company names`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        val productionCompaniesString = contentDetail.metadata.customMetadata["production_companies"]
        assertEquals("Regency Enterprises", productionCompaniesString)
    }
    
    @Test
    fun `mapper handles complex country and language names`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        val countriesString = contentDetail.metadata.customMetadata["production_countries"]
        assertEquals("United States of America", countriesString)
        
        val languagesString = contentDetail.metadata.customMetadata["spoken_languages"]
        assertEquals("English", languagesString)
    }
    
    @Test
    fun `mapper creates content detail with default actions`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        assertNotNull(contentDetail.actions)
        assertTrue(contentDetail.actions.isNotEmpty())
    }
    
    @Test
    fun `mapper preserves all TMDb movie response data`() {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act
        val contentDetail = mapper.mapMovieToContentDetail(movieResponse) as TMDbMovieContentDetail
        
        // Assert
        assertEquals(movieResponse, contentDetail.getTMDbMovie())
    }
    
    @Test
    fun `mapper preserves all TMDb TV response data`() {
        // Arrange
        val tvResponse = TMDbTestDataFactory.createTMDbTVResponse()
        
        // Act
        val contentDetail = mapper.mapTVToContentDetail(tvResponse) as TMDbTVContentDetail
        
        // Assert
        assertEquals(tvResponse, contentDetail.getTMDbTV())
    }
}