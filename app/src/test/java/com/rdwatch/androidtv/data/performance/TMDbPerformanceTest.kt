package com.rdwatch.androidtv.data.performance

import com.rdwatch.androidtv.data.mappers.TMDbToContentDetailMapper
import com.rdwatch.androidtv.data.mappers.TMDbMovieContentDetail
import com.rdwatch.androidtv.data.mappers.TMDbTVContentDetail
import com.rdwatch.androidtv.test.data.TMDbTestDataFactory
import com.rdwatch.androidtv.test.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

/**
 * Performance tests for TMDb data transformations
 * Tests memory usage, execution time, and efficiency of data operations
 */
class TMDbPerformanceTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var mapper: TMDbToContentDetailMapper
    
    @Before
    fun setUp() {
        mapper = TMDbToContentDetailMapper()
    }
    
    @Test
    fun `movie mapping performance - single item`() = runTest {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            val contentDetail = mapper.mapMovieToContentDetail(movieResponse)
            
            // Access all properties to ensure full mapping
            contentDetail.id
            contentDetail.title
            contentDetail.description
            contentDetail.backgroundImageUrl
            contentDetail.cardImageUrl
            contentDetail.metadata
            contentDetail.actions
        }
        
        // Assert - Should complete within reasonable time (< 10ms)
        assertTrue(executionTime < 10, "Single movie mapping took ${executionTime}ms, expected < 10ms")
    }
    
    @Test
    fun `movie mapping performance - batch processing`() = runTest {
        // Arrange
        val batchSize = 100
        val movieResponses = (1..batchSize).map { id ->
            TMDbTestDataFactory.createTMDbMovieResponse(id = id, title = "Movie $id")
        }
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            val contentDetails = movieResponses.map { movieResponse ->
                mapper.mapMovieToContentDetail(movieResponse)
            }
            
            // Access all properties to ensure full mapping
            contentDetails.forEach { detail ->
                detail.id
                detail.title
                detail.metadata
                detail.actions
            }
        }
        
        // Assert - Should complete within reasonable time (< 100ms for 100 items)
        assertTrue(executionTime < 100, "Batch movie mapping took ${executionTime}ms, expected < 100ms")
        
        // Performance per item should be reasonable
        val timePerItem = executionTime.toDouble() / batchSize
        assertTrue(timePerItem < 1.0, "Average time per item: ${timePerItem}ms, expected < 1ms")
    }
    
    @Test
    fun `tv mapping performance - single item`() = runTest {
        // Arrange
        val tvResponse = TMDbTestDataFactory.createTMDbTVResponse()
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            val contentDetail = mapper.mapTVToContentDetail(tvResponse)
            
            // Access all properties to ensure full mapping
            contentDetail.id
            contentDetail.title
            contentDetail.description
            contentDetail.backgroundImageUrl
            contentDetail.cardImageUrl
            contentDetail.metadata
            contentDetail.actions
        }
        
        // Assert - Should complete within reasonable time (< 10ms)
        assertTrue(executionTime < 10, "Single TV mapping took ${executionTime}ms, expected < 10ms")
    }
    
    @Test
    fun `tv mapping performance - batch processing`() = runTest {
        // Arrange
        val batchSize = 100
        val tvResponses = (1..batchSize).map { id ->
            TMDbTestDataFactory.createTMDbTVResponse(id = id, name = "TV Show $id")
        }
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            val contentDetails = tvResponses.map { tvResponse ->
                mapper.mapTVToContentDetail(tvResponse)
            }
            
            // Access all properties to ensure full mapping
            contentDetails.forEach { detail ->
                detail.id
                detail.title
                detail.metadata
                detail.actions
            }
        }
        
        // Assert - Should complete within reasonable time (< 100ms for 100 items)
        assertTrue(executionTime < 100, "Batch TV mapping took ${executionTime}ms, expected < 100ms")
        
        // Performance per item should be reasonable
        val timePerItem = executionTime.toDouble() / batchSize
        assertTrue(timePerItem < 1.0, "Average time per item: ${timePerItem}ms, expected < 1ms")
    }
    
    @Test
    fun `content detail creation performance - with credits`() = runTest {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val credits = TMDbTestDataFactory.createTMDbCreditsResponse()
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail(
                movieResponse = movieResponse,
                credits = credits
            )
            
            // Access credit-related properties
            contentDetail.metadata.cast
            contentDetail.metadata.director
            contentDetail.getCredits()
        }
        
        // Assert - Should complete within reasonable time (< 15ms)
        assertTrue(executionTime < 15, "Movie with credits mapping took ${executionTime}ms, expected < 15ms")
    }
    
    @Test
    fun `content detail state changes performance`() = runTest {
        // Arrange
        val originalDetail = TMDbTestDataFactory.createTMDbMovieContentDetail()
        val newProgress = TMDbTestDataFactory.createContentProgress(watchPercentage = 0.75f)
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            repeat(1000) {
                originalDetail.withProgress(newProgress)
                    .withWatchlistStatus(true)
                    .withLikeStatus(true)
                    .withDownloadStatus(true)
            }
        }
        
        // Assert - Should complete within reasonable time (< 50ms for 1000 operations)
        assertTrue(executionTime < 50, "State changes took ${executionTime}ms, expected < 50ms")
        
        // Performance per operation should be reasonable
        val timePerOperation = executionTime.toDouble() / 1000
        assertTrue(timePerOperation < 0.05, "Average time per operation: ${timePerOperation}ms, expected < 0.05ms")
    }
    
    @Test
    fun `metadata chip generation performance`() = runTest {
        // Arrange
        val contentDetail = TMDbTestDataFactory.createTMDbMovieContentDetail()
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            repeat(1000) {
                contentDetail.getMetadataChips()
            }
        }
        
        // Assert - Should complete within reasonable time (< 20ms for 1000 operations)
        assertTrue(executionTime < 20, "Metadata chips generation took ${executionTime}ms, expected < 20ms")
    }
    
    @Test
    fun `action creation performance`() = runTest {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            repeat(1000) {
                TMDbTestDataFactory.createTMDbMovieContentDetail(
                    movieResponse = movieResponse,
                    isInWatchlist = it % 2 == 0,
                    isLiked = it % 3 == 0,
                    isDownloaded = it % 4 == 0
                ).actions
            }
        }
        
        // Assert - Should complete within reasonable time (< 30ms for 1000 operations)
        assertTrue(executionTime < 30, "Action creation took ${executionTime}ms, expected < 30ms")
    }
    
    @Test
    fun `large dataset mapping performance`() = runTest {
        // Arrange
        val largeDatasetSize = 1000
        val movieResponses = (1..largeDatasetSize).map { id ->
            TMDbTestDataFactory.createTMDbMovieResponse(id = id, title = "Movie $id")
        }
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            val contentDetails = movieResponses.map { movieResponse ->
                mapper.mapMovieToContentDetail(movieResponse)
            }
            
            // Perform some operations on the mapped data
            contentDetails.forEach { detail ->
                detail.getDisplayTitle()
                detail.getDisplayDescription()
                detail.getPrimaryImageUrl()
                detail.isPlayable()
            }
        }
        
        // Assert - Should complete within reasonable time (< 500ms for 1000 items)
        assertTrue(executionTime < 500, "Large dataset mapping took ${executionTime}ms, expected < 500ms")
        
        // Performance per item should be reasonable
        val timePerItem = executionTime.toDouble() / largeDatasetSize
        assertTrue(timePerItem < 0.5, "Average time per item: ${timePerItem}ms, expected < 0.5ms")
    }
    
    @Test
    fun `memory efficiency - object creation`() = runTest {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Act
        val contentDetails = (1..10000).map {
            mapper.mapMovieToContentDetail(movieResponse)
        }
        
        // Force garbage collection and measure
        System.gc()
        Thread.sleep(100) // Give GC time to run
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Calculate memory usage
        val memoryUsed = finalMemory - initialMemory
        val memoryPerObject = memoryUsed / contentDetails.size
        
        // Assert - Memory usage should be reasonable (< 10KB per object)
        assertTrue(memoryPerObject < 10 * 1024, "Memory per object: ${memoryPerObject} bytes, expected < 10KB")
        
        // Ensure objects are actually created
        assertTrue(contentDetails.size == 10000)
        assertTrue(contentDetails.all { it.id.isNotEmpty() })
    }
    
    @Test
    fun `concurrent mapping performance`() = runTest {
        // Arrange
        val batchSize = 100
        val movieResponses = (1..batchSize).map { id ->
            TMDbTestDataFactory.createTMDbMovieResponse(id = id, title = "Movie $id")
        }
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            // Simulate concurrent mapping (though in test it's sequential)
            val contentDetails = movieResponses.map { movieResponse ->
                mapper.mapMovieToContentDetail(movieResponse)
            }
            
            // Access all properties concurrently
            contentDetails.forEach { detail ->
                detail.id
                detail.title
                detail.metadata
                detail.actions
                detail.getDisplayTitle()
                detail.getDisplayDescription()
                detail.getPrimaryImageUrl()
                detail.isPlayable()
                detail.getMetadataChips()
            }
        }
        
        // Assert - Should complete within reasonable time
        assertTrue(executionTime < 100, "Concurrent mapping took ${executionTime}ms, expected < 100ms")
    }
    
    @Test
    fun `string formatting performance`() = runTest {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            repeat(10000) {
                // Test runtime formatting
                val runtime = movieResponse.runtime
                val formattedRuntime = runtime?.let { minutes ->
                    val hours = minutes / 60
                    val mins = minutes % 60
                    if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                }
                
                // Test rating formatting
                val rating = movieResponse.voteAverage
                val formattedRating = if (rating > 0) "${"%.1f".format(rating)}/10" else null
                
                // Test year formatting
                val year = movieResponse.releaseDate?.take(4)
                
                // Use formatted values
                formattedRuntime?.length
                formattedRating?.length
                year?.length
            }
        }
        
        // Assert - Should complete within reasonable time (< 100ms for 10000 operations)
        assertTrue(executionTime < 100, "String formatting took ${executionTime}ms, expected < 100ms")
    }
    
    @Test
    fun `collection operations performance`() = runTest {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            repeat(1000) {
                // Test genre extraction
                val genres = movieResponse.genres.map { it.name }
                
                // Test production company extraction
                val companies = movieResponse.productionCompanies.map { it.name }
                
                // Test country extraction
                val countries = movieResponse.productionCountries.map { it.name }
                
                // Test language extraction
                val languages = movieResponse.spokenLanguages.map { it.name }
                
                // Use extracted values
                genres.size
                companies.size
                countries.size
                languages.size
            }
        }
        
        // Assert - Should complete within reasonable time (< 50ms for 1000 operations)
        assertTrue(executionTime < 50, "Collection operations took ${executionTime}ms, expected < 50ms")
    }
    
    @Test
    fun `image url construction performance`() = runTest {
        // Arrange
        val movieResponse = TMDbTestDataFactory.createTMDbMovieResponse()
        
        // Act & Measure
        val executionTime = measureTimeMillis {
            repeat(10000) {
                // Test backdrop URL construction
                val backdropUrl = movieResponse.backdropPath?.let { 
                    "https://image.tmdb.org/t/p/w1280$it" 
                }
                
                // Test poster URL construction
                val posterUrl = movieResponse.posterPath?.let { 
                    "https://image.tmdb.org/t/p/w500$it" 
                }
                
                // Use constructed URLs
                backdropUrl?.length
                posterUrl?.length
            }
        }
        
        // Assert - Should complete within reasonable time (< 50ms for 10000 operations)
        assertTrue(executionTime < 50, "Image URL construction took ${executionTime}ms, expected < 50ms")
    }
}