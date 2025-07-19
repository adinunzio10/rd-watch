package com.rdwatch.androidtv.scraper.config

import com.rdwatch.androidtv.scraper.models.ManifestCapability
import com.rdwatch.androidtv.scraper.models.ManifestConfiguration
import com.rdwatch.androidtv.scraper.models.ManifestMetadata
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.StremioManifest
import com.rdwatch.androidtv.scraper.models.StremioManifestCatalog
import com.rdwatch.androidtv.scraper.models.StremioManifestResource
import com.rdwatch.androidtv.scraper.models.ValidationStatus
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for default scrapers
 * Includes built-in Stremio-compatible scrapers like Torrentio and Knightcrawler
 */
@Singleton
class DefaultScrapersConfig
    @Inject
    constructor() {
        /**
         * Get all default scraper manifests
         */
        fun getDefaultScrapers(): List<ScraperManifest> {
            return listOf(
                createTorrentioManifest(),
                createKnightcrawlerManifest(),
                createCinemetaManifest(),
                createOpenSubtitlesManifest(),
                createKitsuManifest(),
            )
        }

        /**
         * Get default scrapers by priority
         */
        fun getDefaultScrapersByPriority(): List<ScraperManifest> {
            return getDefaultScrapers().sortedBy { it.priorityOrder }
        }

        /**
         * Get default scrapers by capability
         */
        fun getDefaultScrapersByCapability(capability: ManifestCapability): List<ScraperManifest> {
            return getDefaultScrapers().filter { manifest ->
                manifest.metadata.capabilities.contains(capability)
            }
        }

        private fun createTorrentioManifest(): ScraperManifest {
            val now = Date()

            val stremioManifest =
                StremioManifest(
                    id = "com.torrentio.stremio",
                    name = "Torrentio",
                    version = "1.0.0",
                    description = "Stremio addon providing torrent streaming from various trackers",
                    logo = "https://torrentio.strem.fun/logo.png",
                    contactEmail = "torrentio@strem.fun",
                    types = listOf("movie", "series"),
                    resources =
                        listOf(
                            StremioManifestResource(
                                name = "stream",
                                types = listOf("movie", "series"),
                                idPrefixes = null,
                            ),
                        ),
                    catalogs = emptyList(), // Torrentio is primarily a stream provider
                    behaviorHints =
                        com.rdwatch.androidtv.scraper.models.StremioManifestBehaviorHints(
                            p2p = true,
                            configurable = true,
                        ),
                )

            return ScraperManifest(
                id = "torrentio",
                name = "torrentio",
                displayName = "Torrentio",
                version = "1.0.0",
                description = "Popular torrent streaming addon for movies and TV series",
                author = "torrentio",
                baseUrl = "https://torrentio.strem.fun",
                sourceUrl = "https://torrentio.strem.fun/manifest.json",
                stremioManifest = stremioManifest,
                configuration =
                    ManifestConfiguration(
                        providers = listOf("yts", "eztv", "rarbg", "1337x", "thepiratebay"),
                        qualityFilters = listOf("720p", "1080p", "4k"),
                        supportedTypes = listOf("movie", "series"),
                        rateLimitMs = 1000,
                        timeoutSeconds = 30,
                        retryAttempts = 3,
                    ),
                metadata =
                    ManifestMetadata(
                        sourceUrl = "https://torrentio.strem.fun/manifest.json",
                        createdAt = now,
                        updatedAt = now,
                        validationStatus = ValidationStatus.VALID,
                        capabilities = listOf(ManifestCapability.STREAM, ManifestCapability.P2P, ManifestCapability.CONFIGURABLE),
                    ),
                isEnabled = true,
                priorityOrder = 1,
            )
        }

        private fun createKnightcrawlerManifest(): ScraperManifest {
            val now = Date()

            val stremioManifest =
                StremioManifest(
                    id = "com.knightcrawler.stremio",
                    name = "KnightCrawler",
                    version = "1.0.0",
                    description = "Torrent indexer addon for Stremio",
                    logo = "https://knightcrawler.elfhosted.com/logo.png",
                    types = listOf("movie", "series"),
                    resources =
                        listOf(
                            StremioManifestResource(
                                name = "stream",
                                types = listOf("movie", "series"),
                            ),
                        ),
                    catalogs = emptyList(),
                    behaviorHints =
                        com.rdwatch.androidtv.scraper.models.StremioManifestBehaviorHints(
                            p2p = true,
                            configurable = true,
                        ),
                )

            return ScraperManifest(
                id = "knightcrawler",
                name = "knightcrawler",
                displayName = "KnightCrawler",
                version = "1.0.0",
                description = "Advanced torrent indexer with multiple tracker support",
                author = "elfhosted",
                baseUrl = "https://knightcrawler.elfhosted.com",
                sourceUrl = "https://knightcrawler.elfhosted.com/manifest.json",
                stremioManifest = stremioManifest,
                configuration =
                    ManifestConfiguration(
                        providers = listOf("knightcrawler", "torrentgalaxy", "torrentleech"),
                        qualityFilters = listOf("720p", "1080p", "2160p"),
                        supportedTypes = listOf("movie", "series"),
                        rateLimitMs = 1500,
                        timeoutSeconds = 45,
                        retryAttempts = 2,
                    ),
                metadata =
                    ManifestMetadata(
                        sourceUrl = "https://knightcrawler.elfhosted.com/manifest.json",
                        createdAt = now,
                        updatedAt = now,
                        validationStatus = ValidationStatus.VALID,
                        capabilities = listOf(ManifestCapability.STREAM, ManifestCapability.P2P, ManifestCapability.CONFIGURABLE),
                    ),
                isEnabled = true,
                priorityOrder = 2,
            )
        }

        private fun createCinemetaManifest(): ScraperManifest {
            val now = Date()

            val stremioManifest =
                StremioManifest(
                    id = "com.linvo.cinemeta",
                    name = "Cinemeta",
                    version = "3.0.0",
                    description = "Official Stremio addon providing metadata for movies and series",
                    logo = "https://cinemeta.strem.io/logo.png",
                    types = listOf("movie", "series"),
                    resources =
                        listOf(
                            StremioManifestResource(
                                name = "meta",
                                types = listOf("movie", "series"),
                            ),
                        ),
                    catalogs =
                        listOf(
                            StremioManifestCatalog(
                                type = "movie",
                                id = "top",
                                name = "Top Movies",
                            ),
                            StremioManifestCatalog(
                                type = "series",
                                id = "top",
                                name = "Top Series",
                            ),
                        ),
                )

            return ScraperManifest(
                id = "cinemeta",
                name = "cinemeta",
                displayName = "Cinemeta",
                version = "3.0.0",
                description = "Official Stremio metadata provider",
                author = "stremio",
                baseUrl = "https://cinemeta.strem.io",
                sourceUrl = "https://cinemeta.strem.io/manifest.json",
                stremioManifest = stremioManifest,
                configuration =
                    ManifestConfiguration(
                        supportedTypes = listOf("movie", "series"),
                        rateLimitMs = 500,
                        timeoutSeconds = 15,
                        retryAttempts = 3,
                    ),
                metadata =
                    ManifestMetadata(
                        sourceUrl = "https://cinemeta.strem.io/manifest.json",
                        createdAt = now,
                        updatedAt = now,
                        validationStatus = ValidationStatus.VALID,
                        capabilities = listOf(ManifestCapability.META, ManifestCapability.CATALOG),
                    ),
                isEnabled = true,
                priorityOrder = 3,
            )
        }

        private fun createOpenSubtitlesManifest(): ScraperManifest {
            val now = Date()

            val stremioManifest =
                StremioManifest(
                    id = "com.linvo.opensubtitles",
                    name = "OpenSubtitles",
                    version = "1.0.0",
                    description = "Official OpenSubtitles addon for Stremio",
                    logo = "https://opensubtitles.strem.io/logo.png",
                    types = listOf("movie", "series"),
                    resources =
                        listOf(
                            StremioManifestResource(
                                name = "subtitles",
                                types = listOf("movie", "series"),
                            ),
                        ),
                    catalogs = emptyList(),
                )

            return ScraperManifest(
                id = "opensubtitles",
                name = "opensubtitles",
                displayName = "OpenSubtitles",
                version = "1.0.0",
                description = "Subtitle provider with multi-language support",
                author = "stremio",
                baseUrl = "https://opensubtitles.strem.io",
                sourceUrl = "https://opensubtitles.strem.io/manifest.json",
                stremioManifest = stremioManifest,
                configuration =
                    ManifestConfiguration(
                        supportedTypes = listOf("movie", "series"),
                        rateLimitMs = 2000,
                        timeoutSeconds = 20,
                        retryAttempts = 2,
                    ),
                metadata =
                    ManifestMetadata(
                        sourceUrl = "https://opensubtitles.strem.io/manifest.json",
                        createdAt = now,
                        updatedAt = now,
                        validationStatus = ValidationStatus.VALID,
                        capabilities = listOf(ManifestCapability.SUBTITLES),
                    ),
                isEnabled = true,
                priorityOrder = 4,
            )
        }

        private fun createKitsuManifest(): ScraperManifest {
            val now = Date()

            val stremioManifest =
                StremioManifest(
                    id = "com.stremio.kitsu",
                    name = "Kitsu",
                    version = "1.0.0",
                    description = "Anime catalog from Kitsu.io",
                    logo = "https://kitsu.strem.fun/logo.png",
                    types = listOf("series"),
                    resources =
                        listOf(
                            StremioManifestResource(
                                name = "meta",
                                types = listOf("series"),
                            ),
                        ),
                    catalogs =
                        listOf(
                            StremioManifestCatalog(
                                type = "series",
                                id = "kitsu-anime",
                                name = "Anime",
                                genres = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Romance", "Thriller"),
                            ),
                        ),
                )

            return ScraperManifest(
                id = "kitsu",
                name = "kitsu",
                displayName = "Kitsu Anime",
                version = "1.0.0",
                description = "Anime metadata and catalog provider",
                author = "stremio",
                baseUrl = "https://kitsu.strem.fun",
                sourceUrl = "https://kitsu.strem.fun/manifest.json",
                stremioManifest = stremioManifest,
                configuration =
                    ManifestConfiguration(
                        supportedTypes = listOf("series"),
                        rateLimitMs = 1000,
                        timeoutSeconds = 25,
                        retryAttempts = 3,
                    ),
                metadata =
                    ManifestMetadata(
                        sourceUrl = "https://kitsu.strem.fun/manifest.json",
                        createdAt = now,
                        updatedAt = now,
                        validationStatus = ValidationStatus.VALID,
                        capabilities = listOf(ManifestCapability.META, ManifestCapability.CATALOG),
                    ),
                isEnabled = true,
                priorityOrder = 5,
            )
        }

        /**
         * Get default configuration for manifest manager
         */
        fun getDefaultManagerConfig(): DefaultManagerConfig {
            return DefaultManagerConfig(
                autoLoadDefaults = true,
                enabledByDefault = true,
                defaultCacheTtlMinutes = 60,
                defaultRetryAttempts = 3,
                defaultTimeoutSeconds = 30,
                priorityOrderStart = 1,
                validateOnLoad = true,
            )
        }

        /**
         * Get configuration templates for different types of scrapers
         */
        fun getConfigurationTemplates(): Map<String, ManifestConfiguration> {
            return mapOf(
                "torrent_stream" to
                    ManifestConfiguration(
                        rateLimitMs = 1000,
                        timeoutSeconds = 30,
                        retryAttempts = 3,
                        cacheTtlMinutes = 30,
                        supportedTypes = listOf("movie", "series"),
                        requiresAuth = false,
                    ),
                "metadata" to
                    ManifestConfiguration(
                        rateLimitMs = 500,
                        timeoutSeconds = 15,
                        retryAttempts = 3,
                        cacheTtlMinutes = 120,
                        supportedTypes = listOf("movie", "series"),
                        requiresAuth = false,
                    ),
                "subtitles" to
                    ManifestConfiguration(
                        rateLimitMs = 2000,
                        timeoutSeconds = 20,
                        retryAttempts = 2,
                        cacheTtlMinutes = 60,
                        supportedTypes = listOf("movie", "series"),
                        requiresAuth = false,
                    ),
                "catalog" to
                    ManifestConfiguration(
                        rateLimitMs = 1000,
                        timeoutSeconds = 25,
                        retryAttempts = 3,
                        cacheTtlMinutes = 240,
                        supportedTypes = listOf("movie", "series", "channel"),
                        requiresAuth = false,
                    ),
            )
        }
    }

/**
 * Default manager configuration
 */
data class DefaultManagerConfig(
    val autoLoadDefaults: Boolean,
    val enabledByDefault: Boolean,
    val defaultCacheTtlMinutes: Long,
    val defaultRetryAttempts: Int,
    val defaultTimeoutSeconds: Int,
    val priorityOrderStart: Int,
    val validateOnLoad: Boolean,
)

/**
 * Scraper installation status
 */
enum class ScraperInstallStatus {
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    FAILED,
    OUTDATED,
}

/**
 * Default scraper metadata
 */
data class DefaultScraperInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val category: String,
    val capabilities: List<ManifestCapability>,
    val isRecommended: Boolean,
    val minimumVersion: String,
    val installStatus: ScraperInstallStatus = ScraperInstallStatus.NOT_INSTALLED,
)
