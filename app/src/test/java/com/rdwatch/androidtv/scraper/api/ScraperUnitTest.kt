package com.rdwatch.androidtv.scraper.api

import com.rdwatch.androidtv.scraper.api.models.TorrentInfo
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.ManifestConfiguration
import com.rdwatch.androidtv.scraper.models.ManifestMetadata
import com.rdwatch.androidtv.scraper.models.ManifestCapability
import com.rdwatch.androidtv.scraper.models.ValidationStatus
import com.rdwatch.androidtv.scraper.models.StremioManifest
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for scraper components (no dependencies)
 */
class ScraperUnitTest {
    
    @Test
    fun `test Torrentio URL generation`() {
        val queryBuilder = ScraperQueryBuilder()
        val torrentioManifest = createTorrentioManifest()
        
        val url = queryBuilder.buildContentQueryUrl(
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
        assertEquals("Should generate correct Torrentio URL", 
            "https://torrentio.strem.fun/defaults/stream/movie/tt0111161.json", url)
    }
    
    @Test
    fun `test KnightCrawler URL generation`() {
        val queryBuilder = ScraperQueryBuilder()
        val knightCrawlerManifest = createKnightCrawlerManifest()
        
        val url = queryBuilder.buildContentQueryUrl(
            manifest = knightCrawlerManifest,
            contentType = "movie",
            contentId = "tt0111161",
            imdbId = "tt0111161"
        )
        
        println("Generated KnightCrawler URL: $url")
        assertTrue("URL should contain knightcrawler domain", url.contains("knightcrawler"))
        assertTrue("URL should contain movie type", url.contains("/movie/"))
        assertTrue("URL should contain IMDB ID", url.contains("tt0111161"))
        assertEquals("Should generate correct KnightCrawler URL",
            "https://knightcrawler.elfhosted.com/stream/movie/tt0111161.json", url)
    }
    
    @Test
    fun `test TV show URL generation with season and episode`() {
        val queryBuilder = ScraperQueryBuilder()
        val torrentioManifest = createTorrentioManifest()
        
        val url = queryBuilder.buildContentQueryUrl(
            manifest = torrentioManifest,
            contentType = "tv",
            contentId = "tt0903747", // Breaking Bad
            imdbId = "tt0903747",
            seasonNumber = 1,
            episodeNumber = 1
        )
        
        println("Generated TV URL: $url")
        assertTrue("URL should contain series type", url.contains("/series/"))
        assertTrue("URL should contain season:episode format", url.contains("tt0903747:1:1"))
        assertEquals("Should generate correct TV URL",
            "https://torrentio.strem.fun/defaults/stream/series/tt0903747:1:1.json", url)
    }
    
    @Test
    fun `test quality detection from torrent titles`() {
        val testCases = listOf(
            "The.Movie.2023.2160p.UHD.BluRay.x265-GROUP" to "2160p",
            "The.Movie.2023.1080p.BluRay.x264-GROUP" to "1080p", 
            "The.Movie.2023.720p.WEB-DL.x264-GROUP" to "720p",
            "The.Movie.2023.480p.DVDRip.x264-GROUP" to "480p",
            "The.Movie.2023.4K.HDR.BluRay.HEVC-GROUP" to "2160p" // 4K should map to 2160p
        )
        
        println("Testing quality detection:")
        testCases.forEach { (title, expectedQuality) ->
            val torrentInfo = TorrentInfo.fromTitle(title)
            println("Title: $title")
            println("Detected: ${torrentInfo.quality}, Expected: $expectedQuality")
            
            assertEquals("Quality should be detected correctly for: $title", 
                expectedQuality, torrentInfo.quality)
        }
    }
    
    @Test
    fun `test codec detection from torrent titles`() {
        val testCases = listOf(
            "Movie.2023.1080p.BluRay.x264-GROUP" to "H264",
            "Movie.2023.1080p.BluRay.x265-GROUP" to "HEVC",
            "Movie.2023.1080p.BluRay.HEVC-GROUP" to "HEVC",
            "Movie.2023.1080p.BluRay.H264-GROUP" to "H264",
            "Movie.2023.1080p.BluRay.AV1-GROUP" to "AV1"
        )
        
        println("Testing codec detection:")
        testCases.forEach { (title, expectedCodec) ->
            val torrentInfo = TorrentInfo.fromTitle(title)
            println("Title: $title")
            println("Detected: ${torrentInfo.codec}, Expected: $expectedCodec")
            
            assertEquals("Codec should be detected correctly for: $title",
                expectedCodec, torrentInfo.codec)
        }
    }
    
    @Test
    fun `test seeders and leechers extraction`() {
        val title = "Movie.2023.1080p.BluRay.x264-GROUP 👥 150/10"
        val torrentInfo = TorrentInfo.fromTitle(title)
        
        println("Title: $title")
        println("Seeders: ${torrentInfo.seeders}, Leechers: ${torrentInfo.leechers}")
        
        assertEquals("Should extract seeders correctly", 150, torrentInfo.seeders)
        assertEquals("Should extract leechers correctly", 10, torrentInfo.leechers)
    }
    
    @Test
    fun `test size extraction`() {
        val testCases = listOf(
            "Movie.2023.1080p.BluRay.x264-GROUP 15.2GB" to "15.2GB",
            "Movie.2023.720p.WEB-DL.x264-GROUP 2.5 GB" to "2.5 GB",
            "Movie.2023.480p.DVDRip.x264-GROUP 750MB" to "750MB",
            "Movie.2023.4K.BluRay.x265-GROUP 25.8 GB" to "25.8 GB"
        )
        
        println("Testing size extraction:")
        testCases.forEach { (title, expectedSize) ->
            val torrentInfo = TorrentInfo.fromTitle(title)
            println("Title: $title")
            println("Detected: ${torrentInfo.size}, Expected: $expectedSize")
            
            assertEquals("Size should be extracted correctly for: $title",
                expectedSize, torrentInfo.size)
        }
    }
    
    @Test
    fun `test HDR detection`() {
        val hdrTitles = listOf(
            "Movie.2023.2160p.UHD.BluRay.x265.HDR-GROUP",
            "Movie.2023.2160p.UHD.BluRay.x265.HDR10-GROUP", 
            "Movie.2023.2160p.UHD.BluRay.x265.DOLBY.VISION-GROUP",
            "Movie.2023.2160p.UHD.BluRay.x265.DV-GROUP"
        )
        
        val nonHdrTitles = listOf(
            "Movie.2023.2160p.UHD.BluRay.x265-GROUP",
            "Movie.2023.1080p.BluRay.x264-GROUP"
        )
        
        println("Testing HDR detection:")
        hdrTitles.forEach { title ->
            val torrentInfo = TorrentInfo.fromTitle(title)
            println("HDR Title: $title -> ${torrentInfo.hdr}")
            assertTrue("Should detect HDR for: $title", torrentInfo.hdr)
        }
        
        nonHdrTitles.forEach { title ->
            val torrentInfo = TorrentInfo.fromTitle(title)
            println("Non-HDR Title: $title -> ${torrentInfo.hdr}")
            assertFalse("Should not detect HDR for: $title", torrentInfo.hdr)
        }
    }
    
    @Test
    fun `test source type detection`() {
        val testCases = listOf(
            "Movie.2023.1080p.BluRay.x264-GROUP" to "BluRay",
            "Movie.2023.1080p.WEB-DL.x264-GROUP" to "WEB-DL",
            "Movie.2023.1080p.WebRip.x264-GROUP" to "WebRip", 
            "Movie.2023.1080p.HDTV.x264-GROUP" to "HDTV",
            "Movie.2023.1080p.DVDRip.x264-GROUP" to "DVDRip"
        )
        
        println("Testing source type detection:")
        testCases.forEach { (title, expectedSource) ->
            val torrentInfo = TorrentInfo.fromTitle(title)
            println("Title: $title")
            println("Detected: ${torrentInfo.source}, Expected: $expectedSource")
            
            assertEquals("Source should be detected correctly for: $title",
                expectedSource, torrentInfo.source)
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