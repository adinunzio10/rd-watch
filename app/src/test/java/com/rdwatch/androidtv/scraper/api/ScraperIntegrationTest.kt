package com.rdwatch.androidtv.scraper.api

import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.ManifestConfiguration
import com.rdwatch.androidtv.scraper.models.ManifestMetadata
import com.rdwatch.androidtv.scraper.models.ManifestCapability
import com.rdwatch.androidtv.scraper.models.ValidationStatus
import com.rdwatch.androidtv.scraper.models.StremioManifest
import com.rdwatch.androidtv.ui.details.adapters.ScraperSourceAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for the scraper system
 * Tests real API calls to verify the implementation works
 */
class ScraperIntegrationTest {
    
    private lateinit var scraperApiClient: ScraperApiClient
    private lateinit var scraperQueryBuilder: ScraperQueryBuilder
    private lateinit var scraperResponseMapper: ScraperResponseMapper
    private lateinit var scraperSourceAdapter: ScraperSourceAdapter
    private lateinit var moshi: Moshi
    
    @Before
    fun setup() {
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            
        scraperApiClient = ScraperApiClient()
        scraperQueryBuilder = ScraperQueryBuilder()
        scraperSourceAdapter = ScraperSourceAdapter()
        scraperResponseMapper = ScraperResponseMapper(scraperSourceAdapter, moshi)
    }
    
    @Test
    fun `test Torrentio URL generation`() {
        val torrentioManifest = createTorrentioManifest()
        
        val url = scraperQueryBuilder.buildContentQueryUrl(
            manifest = torrentioManifest,
            contentType = "movie",
            contentId = "tt0111161", // The Shawshank Redemption
            imdbId = "tt0111161"
        )
        
        println("Generated Torrentio URL: $url")
        assertTrue("URL should contain torrentio domain", url.contains("torrentio.strem.fun"))
        assertTrue("URL should contain movie type", url.contains("/movie/"))
        assertTrue("URL should contain IMDB ID", url.contains("tt0111161"))
        assertTrue("URL should end with .json", url.endsWith(".json"))
    }
    
    @Test
    fun `test KnightCrawler URL generation`() {
        val knightCrawlerManifest = createKnightCrawlerManifest()
        
        val url = scraperQueryBuilder.buildContentQueryUrl(
            manifest = knightCrawlerManifest,
            contentType = "movie",
            contentId = "tt0111161",
            imdbId = "tt0111161"
        )
        
        println("Generated KnightCrawler URL: $url")
        assertTrue("URL should contain knightcrawler domain", url.contains("knightcrawler"))
        assertTrue("URL should contain movie type", url.contains("/movie/"))
        assertTrue("URL should contain IMDB ID", url.contains("tt0111161"))
    }
    
    @Test
    fun `test real Torrentio API call`() = runBlocking {
        val torrentioManifest = createTorrentioManifest()
        
        val url = scraperQueryBuilder.buildContentQueryUrl(
            manifest = torrentioManifest,
            contentType = "movie",
            contentId = "tt0111161", // The Shawshank Redemption
            imdbId = "tt0111161"
        )
        
        println("Testing real API call to: $url")
        
        val response = scraperApiClient.makeScraperRequest(url)
        
        when (response) {
            is ScraperApiResponse.Success -> {
                println("✅ API call successful!")
                println("Response length: ${response.data.length}")
                println("Status code: ${response.statusCode}")
                
                // Test response parsing
                val sources = scraperResponseMapper.parseScraperResponse(
                    manifest = torrentioManifest,
                    responseBody = response.data,
                    contentId = "tt0111161",
                    contentType = "movie"
                )
                
                println("✅ Parsed ${sources.size} sources")
                sources.take(3).forEach { source ->
                    println("Source: ${source.title} - ${source.quality.displayName} - ${source.url}")
                }
                
                assertTrue("Should have at least one source", sources.isNotEmpty())
                sources.forEach { source ->
                    assertNotNull("Source should have a title", source.title)
                    assertNotNull("Source should have a URL", source.url)
                    assertNotNull("Source should have a quality", source.quality)
                }
            }
            is ScraperApiResponse.Error -> {
                println("❌ API call failed: ${response.message}")
                if (response.statusCode == 0) {
                    println("Network timeout or connection error - this is expected in some environments")
                    // Don't fail the test for network issues
                } else {
                    fail("API call failed with status ${response.statusCode}: ${response.message}")
                }
            }
        }
    }
    
    @Test
    fun `test response parsing with sample Torrentio data`() {
        val torrentioManifest = createTorrentioManifest()
        
        // Sample Torrentio response format
        val sampleResponse = """
        {
            "streams": [
                {
                    "name": "The.Shawshank.Redemption.1994.2160p.BluRay.x265-SURCODE",
                    "title": "🎬 The Shawshank Redemption (1994)\n📺 2160p BluRay x265\n💾 15.2 GB\n👥 45/5",
                    "infoHash": "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c",
                    "sources": [
                        "tracker:udp://open.demonii.com:1337/announce"
                    ]
                },
                {
                    "name": "The.Shawshank.Redemption.1994.1080p.BluRay.x264-AMIABLE",
                    "title": "🎬 The Shawshank Redemption (1994)\n📺 1080p BluRay x264\n💾 8.7 GB\n👥 123/8",
                    "infoHash": "1e8b1c4e8b1c4e8b1c4e8b1c4e8b1c4e8b1c4e8b",
                    "sources": [
                        "tracker:udp://tracker.openbittorrent.com:80/announce"
                    ]
                }
            ]
        }
        """.trimIndent()
        
        val sources = scraperResponseMapper.parseScraperResponse(
            manifest = torrentioManifest,
            responseBody = sampleResponse,
            contentId = "tt0111161",
            contentType = "movie"
        )
        
        println("✅ Parsed ${sources.size} sources from sample data")
        
        assertEquals("Should parse 2 sources", 2, sources.size)
        
        val source4k = sources.find { it.quality.displayName.contains("4K") || it.quality.displayName.contains("2160p") }
        assertNotNull("Should have 4K source", source4k)
        
        val source1080p = sources.find { it.quality.displayName.contains("1080p") }
        assertNotNull("Should have 1080p source", source1080p)
        
        sources.forEach { source ->
            println("Parsed source: ${source.title}")
            println("  Quality: ${source.quality.displayName}")
            println("  URL: ${source.url}")
            println("  Available: ${source.isAvailable}")
            println("  Seeders: ${source.features.seeders}")
            println("  Leechers: ${source.features.leechers}")
            println()
            
            assertTrue("URL should be a magnet link", source.url.startsWith("magnet:"))
            assertTrue("Source should be available", source.isAvailable)
        }
    }
    
    @Test
    fun `test quality detection from torrent titles`() {
        val testCases = listOf(
            "The.Movie.2023.2160p.UHD.BluRay.x265-GROUP" to "4K",
            "The.Movie.2023.1080p.BluRay.x264-GROUP" to "1080p",
            "The.Movie.2023.720p.WEB-DL.x264-GROUP" to "720p",
            "The.Movie.2023.480p.DVDRip.x264-GROUP" to "480p",
            "The.Movie.2023.4K.HDR.BluRay.HEVC-GROUP" to "4K"
        )
        
        testCases.forEach { (title, expectedQuality) ->
            val torrentInfo = com.rdwatch.androidtv.scraper.api.models.TorrentInfo.fromTitle(title)
            println("Title: $title")
            println("Detected quality: ${torrentInfo.quality}")
            println("Expected: $expectedQuality")
            println()
            
            assertTrue(
                "Quality should be detected correctly for: $title",
                torrentInfo.quality?.contains(expectedQuality, ignoreCase = true) == true
            )
        }
    }
    
    private fun createTorrentioManifest(): ScraperManifest {
        return ScraperManifest(
            id = "torrentio",
            name = "torrentio",
            displayName = "Torrentio",
            version = "1.0.0",
            baseUrl = "https://torrentio.strem.fun",
            sourceUrl = "https://torrentio.strem.fun/manifest.json",
            stremioManifest = StremioManifest(
                id = "torrentio",
                name = "Torrentio",
                version = "1.0.0",
                description = "Torrentio addon",
                resources = listOf("stream"),
                types = listOf("movie", "series"),
                catalogs = emptyList()
            ),
            configuration = ManifestConfiguration(),
            metadata = ManifestMetadata(
                validationStatus = ValidationStatus.VALID,
                capabilities = listOf(ManifestCapability.STREAM, ManifestCapability.P2P),
                addedTime = System.currentTimeMillis(),
                lastChecked = System.currentTimeMillis()
            ),
            isEnabled = true,
            priorityOrder = 1
        )
    }
    
    private fun createKnightCrawlerManifest(): ScraperManifest {
        return ScraperManifest(
            id = "knightcrawler",
            name = "knightcrawler",
            displayName = "KnightCrawler",
            version = "1.0.0",
            baseUrl = "https://knightcrawler.elfhosted.com",
            sourceUrl = "https://knightcrawler.elfhosted.com/manifest.json",
            stremioManifest = StremioManifest(
                id = "knightcrawler",
                name = "KnightCrawler",
                version = "1.0.0",
                description = "KnightCrawler addon",
                resources = listOf("stream"),
                types = listOf("movie", "series"),
                catalogs = emptyList()
            ),
            configuration = ManifestConfiguration(),
            metadata = ManifestMetadata(
                validationStatus = ValidationStatus.VALID,
                capabilities = listOf(ManifestCapability.STREAM, ManifestCapability.P2P),
                addedTime = System.currentTimeMillis(),
                lastChecked = System.currentTimeMillis()
            ),
            isEnabled = true,
            priorityOrder = 2
        )
    }
}