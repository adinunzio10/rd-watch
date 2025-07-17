package com.rdwatch.androidtv.scraper.api

import com.rdwatch.androidtv.scraper.api.models.TorrentInfo
import com.rdwatch.androidtv.scraper.models.ManifestCapability
import com.rdwatch.androidtv.scraper.models.ManifestConfiguration
import com.rdwatch.androidtv.scraper.models.ManifestMetadata
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.StremioManifest
import com.rdwatch.androidtv.scraper.models.ValidationStatus
import com.rdwatch.androidtv.ui.details.adapters.ScraperSourceAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking

/**
 * Manual test runner to verify scraper functionality
 * Can be called from anywhere in the app for testing
 */
object ScraperTestRunner {
    fun runAllTests(): String {
        val results = mutableListOf<String>()

        results.add("🧪 Running Scraper Integration Tests")
        results.add("=" * 50)

        // Test URL generation
        results.addAll(testUrlGeneration())
        results.add("")

        // Test quality detection
        results.addAll(testQualityDetection())
        results.add("")

        // Test response parsing
        results.addAll(testResponseParsing())
        results.add("")

        // Test real API call (if network available)
        results.addAll(testRealApiCall())

        return results.joinToString("\n")
    }

    private fun testUrlGeneration(): List<String> {
        val results = mutableListOf<String>()
        val queryBuilder = ScraperQueryBuilder()

        results.add("📡 Testing URL Generation:")

        try {
            // Test Torrentio
            val torrentioManifest = createTorrentioManifest()
            val torrentioUrl =
                queryBuilder.buildContentQueryUrl(
                    manifest = torrentioManifest,
                    contentType = "movie",
                    contentId = "tt0111161",
                    imdbId = "tt0111161",
                )

            val expectedTorrentio = "https://torrentio.strem.fun/defaults/stream/movie/tt0111161.json"
            if (torrentioUrl == expectedTorrentio) {
                results.add("✅ Torrentio URL: PASSED")
            } else {
                results.add("❌ Torrentio URL: FAILED")
                results.add("   Expected: $expectedTorrentio")
                results.add("   Got:      $torrentioUrl")
            }

            // Test KnightCrawler
            val knightCrawlerManifest = createKnightCrawlerManifest()
            val knightCrawlerUrl =
                queryBuilder.buildContentQueryUrl(
                    manifest = knightCrawlerManifest,
                    contentType = "movie",
                    contentId = "tt0111161",
                    imdbId = "tt0111161",
                )

            val expectedKnightCrawler = "https://knightcrawler.elfhosted.com/stream/movie/tt0111161.json"
            if (knightCrawlerUrl == expectedKnightCrawler) {
                results.add("✅ KnightCrawler URL: PASSED")
            } else {
                results.add("❌ KnightCrawler URL: FAILED")
                results.add("   Expected: $expectedKnightCrawler")
                results.add("   Got:      $knightCrawlerUrl")
            }

            // Test TV show format
            val tvUrl =
                queryBuilder.buildContentQueryUrl(
                    manifest = torrentioManifest,
                    contentType = "tv",
                    contentId = "tt0903747",
                    imdbId = "tt0903747",
                    seasonNumber = 1,
                    episodeNumber = 1,
                )

            val expectedTv = "https://torrentio.strem.fun/defaults/stream/series/tt0903747:1:1.json"
            if (tvUrl == expectedTv) {
                results.add("✅ TV Show URL: PASSED")
            } else {
                results.add("❌ TV Show URL: FAILED")
                results.add("   Expected: $expectedTv")
                results.add("   Got:      $tvUrl")
            }
        } catch (e: Exception) {
            results.add("❌ URL Generation: EXCEPTION - ${e.message}")
        }

        return results
    }

    private fun testQualityDetection(): List<String> {
        val results = mutableListOf<String>()
        results.add("🎬 Testing Quality Detection:")

        val testCases =
            listOf(
                "The.Movie.2023.2160p.UHD.BluRay.x265-GROUP" to "2160p",
                "The.Movie.2023.1080p.BluRay.x264-GROUP" to "1080p",
                "The.Movie.2023.720p.WEB-DL.x264-GROUP" to "720p",
                "The.Movie.2023.480p.DVDRip.x264-GROUP" to "480p",
                "Movie.2023.1080p.BluRay.x264-GROUP 👥 150/10" to "1080p",
            )

        var passed = 0
        var total = testCases.size

        testCases.forEach { (title, expectedQuality) ->
            try {
                val torrentInfo = TorrentInfo.fromTitle(title)
                if (torrentInfo.quality == expectedQuality) {
                    results.add("✅ Quality detection: $expectedQuality")
                    passed++
                } else {
                    results.add("❌ Quality detection failed for: $title")
                    results.add("   Expected: $expectedQuality, Got: ${torrentInfo.quality}")
                }

                // Also test seeders/leechers for the last case
                if (title.contains("👥")) {
                    if (torrentInfo.seeders == 150 && torrentInfo.leechers == 10) {
                        results.add("✅ Seeders/Leechers extraction: PASSED")
                    } else {
                        results.add("❌ Seeders/Leechers extraction: FAILED")
                        results.add("   Expected: 150/10, Got: ${torrentInfo.seeders}/${torrentInfo.leechers}")
                    }
                }
            } catch (e: Exception) {
                results.add("❌ Quality detection: EXCEPTION - ${e.message}")
            }
        }

        results.add("📊 Quality Detection: $passed/$total tests passed")
        return results
    }

    private fun testResponseParsing(): List<String> {
        val results = mutableListOf<String>()
        results.add("📋 Testing Response Parsing:")

        try {
            val moshi =
                Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

            val scraperSourceAdapter = ScraperSourceAdapter()
            val responseMapper = ScraperResponseMapper(scraperSourceAdapter, moshi)
            val torrentioManifest = createTorrentioManifest()

            // Sample Torrentio response
            val sampleResponse =
                """
                {
                    "streams": [
                        {
                            "name": "The.Shawshank.Redemption.1994.2160p.BluRay.x265-SURCODE",
                            "title": "🎬 The Shawshank Redemption (1994)\n📺 2160p BluRay x265\n💾 15.2 GB\n👥 45/5",
                            "infoHash": "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c"
                        },
                        {
                            "name": "The.Shawshank.Redemption.1994.1080p.BluRay.x264-AMIABLE",
                            "title": "🎬 The Shawshank Redemption (1994)\n📺 1080p BluRay x264\n💾 8.7 GB\n👥 123/8",
                            "infoHash": "1e8b1c4e8b1c4e8b1c4e8b1c4e8b1c4e8b1c4e8b"
                        }
                    ]
                }
                """.trimIndent()

            val sources =
                responseMapper.parseScraperResponse(
                    manifest = torrentioManifest,
                    responseBody = sampleResponse,
                    contentId = "tt0111161",
                    contentType = "movie",
                )

            if (sources.size == 2) {
                results.add("✅ Response parsing: Found ${sources.size} sources")

                val has4K = sources.any { it.quality.displayName.contains("4K") || it.quality.displayName.contains("2160p") }
                val has1080p = sources.any { it.quality.displayName.contains("1080p") }
                val allHaveMagnetUrls = sources.all { it.url.startsWith("magnet:") }
                val allAvailable = sources.all { it.isAvailable }

                if (has4K) {
                    results.add("✅ 4K source detected")
                } else {
                    results.add("❌ 4K source not detected")
                }

                if (has1080p) {
                    results.add("✅ 1080p source detected")
                } else {
                    results.add("❌ 1080p source not detected")
                }

                if (allHaveMagnetUrls) {
                    results.add("✅ All sources have magnet URLs")
                } else {
                    results.add("❌ Some sources missing magnet URLs")
                }

                if (allAvailable) {
                    results.add("✅ All sources marked as available")
                } else {
                    results.add("❌ Some sources not available")
                }
            } else {
                results.add("❌ Response parsing: Expected 2 sources, got ${sources.size}")
            }
        } catch (e: Exception) {
            results.add("❌ Response parsing: EXCEPTION - ${e.message}")
            e.printStackTrace()
        }

        return results
    }

    private fun testRealApiCall(): List<String> {
        val results = mutableListOf<String>()
        results.add("🌐 Testing Real API Call:")

        return runBlocking {
            try {
                val apiClient = ScraperApiClient()
                val queryBuilder = ScraperQueryBuilder()
                val torrentioManifest = createTorrentioManifest()

                val url =
                    queryBuilder.buildContentQueryUrl(
                        manifest = torrentioManifest,
                        contentType = "movie",
                        contentId = "tt0111161", // The Shawshank Redemption
                        imdbId = "tt0111161",
                    )

                results.add("🔗 Testing URL: $url")

                val response = apiClient.makeScraperRequest(url)

                when (response) {
                    is ScraperApiResponse.Success -> {
                        results.add("✅ Real API call: SUCCESS")
                        results.add("📊 Status: ${response.statusCode}")
                        results.add("📏 Response length: ${response.data.length} characters")

                        // Try to parse the response
                        try {
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                            val scraperSourceAdapter = ScraperSourceAdapter()
                            val responseMapper = ScraperResponseMapper(scraperSourceAdapter, moshi)

                            val sources =
                                responseMapper.parseScraperResponse(
                                    manifest = torrentioManifest,
                                    responseBody = response.data,
                                    contentId = "tt0111161",
                                    contentType = "movie",
                                )

                            results.add("✅ Parsing successful: ${sources.size} sources found")

                            if (sources.isNotEmpty()) {
                                results.add("📄 Sample sources:")
                                sources.take(3).forEach { source ->
                                    results.add("   • ${source.quality.displayName} - ${source.title?.take(50) ?: "No title"}")
                                }
                            }
                        } catch (e: Exception) {
                            results.add("❌ Response parsing failed: ${e.message}")
                        }
                    }
                    is ScraperApiResponse.Error -> {
                        results.add("❌ Real API call: FAILED")
                        results.add("📊 Status: ${response.statusCode}")
                        results.add("💬 Error: ${response.message}")

                        if (response.statusCode == 0) {
                            results.add("ℹ️  Network timeout/connection error - expected in some environments")
                        }
                    }
                }
            } catch (e: Exception) {
                results.add("❌ Real API call: EXCEPTION - ${e.message}")
            }

            results
        }
    }

    private fun createTorrentioManifest(): ScraperManifest {
        val now = java.util.Date()
        return ScraperManifest(
            id = "torrentio",
            name = "torrentio",
            displayName = "Torrentio",
            version = "1.0.0",
            baseUrl = "https://torrentio.strem.fun",
            sourceUrl = "https://torrentio.strem.fun/manifest.json",
            stremioManifest =
                StremioManifest(
                    id = "torrentio",
                    name = "Torrentio",
                    version = "1.0.0",
                    description = "Torrentio addon",
                    resources =
                        listOf(
                            com.rdwatch.androidtv.scraper.models.StremioManifestResource(
                                name = "stream",
                                types = listOf("movie", "series"),
                            ),
                        ),
                    types = listOf("movie", "series"),
                    catalogs = emptyList(),
                ),
            configuration = ManifestConfiguration(),
            metadata =
                ManifestMetadata(
                    sourceUrl = "https://torrentio.strem.fun/manifest.json",
                    createdAt = now,
                    updatedAt = now,
                    validationStatus = ValidationStatus.VALID,
                    capabilities = listOf(ManifestCapability.STREAM, ManifestCapability.P2P),
                ),
            isEnabled = true,
            priorityOrder = 1,
        )
    }

    private fun createKnightCrawlerManifest(): ScraperManifest {
        val now = java.util.Date()
        return ScraperManifest(
            id = "knightcrawler",
            name = "knightcrawler",
            displayName = "KnightCrawler",
            version = "1.0.0",
            baseUrl = "https://knightcrawler.elfhosted.com",
            sourceUrl = "https://knightcrawler.elfhosted.com/manifest.json",
            stremioManifest =
                StremioManifest(
                    id = "knightcrawler",
                    name = "KnightCrawler",
                    version = "1.0.0",
                    description = "KnightCrawler addon",
                    resources =
                        listOf(
                            com.rdwatch.androidtv.scraper.models.StremioManifestResource(
                                name = "stream",
                                types = listOf("movie", "series"),
                            ),
                        ),
                    types = listOf("movie", "series"),
                    catalogs = emptyList(),
                ),
            configuration = ManifestConfiguration(),
            metadata =
                ManifestMetadata(
                    sourceUrl = "https://knightcrawler.elfhosted.com/manifest.json",
                    createdAt = now,
                    updatedAt = now,
                    validationStatus = ValidationStatus.VALID,
                    capabilities = listOf(ManifestCapability.STREAM, ManifestCapability.P2P),
                ),
            isEnabled = true,
            priorityOrder = 2,
        )
    }

    private operator fun String.times(n: Int): String = this.repeat(n)
}
