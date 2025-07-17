package com.rdwatch.androidtv.scraper.parser

import com.rdwatch.androidtv.scraper.models.ManifestException
import com.rdwatch.androidtv.scraper.models.ManifestParsingException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.StremioManifest
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manifest parser supporting multiple formats (JSON, YAML, XML)
 * Currently focused on JSON (Stremio format) with extensibility for other formats
 */
@Singleton
class ManifestParser
    @Inject
    constructor() {
        private val moshi: Moshi =
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

        private val stremioManifestAdapter: JsonAdapter<StremioManifest> =
            moshi.adapter(StremioManifest::class.java)

        /**
         * Parse manifest from string content
         */
        fun parseManifest(
            content: String,
            sourceUrl: String,
            format: ManifestFormat = ManifestFormat.AUTO_DETECT,
        ): ManifestResult<StremioManifest> {
            return try {
                val detectedFormat =
                    if (format == ManifestFormat.AUTO_DETECT) {
                        detectFormat(content, sourceUrl)
                    } else {
                        format
                    }

                when (detectedFormat) {
                    ManifestFormat.JSON -> parseJsonManifest(content, sourceUrl)
                    ManifestFormat.YAML -> parseYamlManifest(content, sourceUrl)
                    ManifestFormat.XML -> parseXmlManifest(content, sourceUrl)
                    ManifestFormat.AUTO_DETECT -> {
                        ManifestResult.Error(
                            ManifestParsingException(
                                "Unable to auto-detect manifest format",
                                url = sourceUrl,
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestParsingException(
                        "Failed to parse manifest: ${e.message}",
                        cause = e,
                        url = sourceUrl,
                        format = format.name,
                    ),
                )
            }
        }

        /**
         * Parse JSON manifest (Stremio format)
         */
        private fun parseJsonManifest(
            content: String,
            sourceUrl: String,
        ): ManifestResult<StremioManifest> {
            return try {
                val manifest =
                    stremioManifestAdapter.fromJson(content)
                        ?: return ManifestResult.Error(
                            ManifestParsingException(
                                "Failed to parse JSON manifest: null result",
                                url = sourceUrl,
                                format = "JSON",
                            ),
                        )

                validateBasicStructure(manifest, sourceUrl)?.let { error ->
                    return ManifestResult.Error(error)
                }

                ManifestResult.Success(manifest)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestParsingException(
                        "JSON parsing failed: ${e.message}",
                        cause = e,
                        url = sourceUrl,
                        format = "JSON",
                    ),
                )
            }
        }

        /**
         * Parse YAML manifest (future implementation)
         */
        private fun parseYamlManifest(
            content: String,
            sourceUrl: String,
        ): ManifestResult<StremioManifest> {
            // TODO: Implement YAML parsing using SnakeYAML library
            return ManifestResult.Error(
                ManifestParsingException(
                    "YAML parsing not yet implemented",
                    url = sourceUrl,
                    format = "YAML",
                ),
            )
        }

        /**
         * Parse XML manifest (future implementation)
         */
        private fun parseXmlManifest(
            content: String,
            sourceUrl: String,
        ): ManifestResult<StremioManifest> {
            // TODO: Implement XML parsing
            return ManifestResult.Error(
                ManifestParsingException(
                    "XML parsing not yet implemented",
                    url = sourceUrl,
                    format = "XML",
                ),
            )
        }

        /**
         * Auto-detect manifest format based on content and URL
         */
        private fun detectFormat(
            content: String,
            sourceUrl: String,
        ): ManifestFormat {
            val trimmedContent = content.trim()

            // Check URL extension first
            when {
                sourceUrl.endsWith(".json", ignoreCase = true) -> return ManifestFormat.JSON
                sourceUrl.endsWith(".yml", ignoreCase = true) ||
                    sourceUrl.endsWith(".yaml", ignoreCase = true) -> return ManifestFormat.YAML
                sourceUrl.endsWith(".xml", ignoreCase = true) -> return ManifestFormat.XML
            }

            // Check content structure
            when {
                trimmedContent.startsWith("{") && trimmedContent.endsWith("}") -> return ManifestFormat.JSON
                trimmedContent.startsWith("---") || trimmedContent.contains("id:") -> return ManifestFormat.YAML
                trimmedContent.startsWith("<?xml") || trimmedContent.startsWith("<manifest") -> return ManifestFormat.XML
            }

            // Default to JSON for Stremio compatibility
            return ManifestFormat.JSON
        }

        /**
         * Validate basic manifest structure
         */
        private fun validateBasicStructure(
            manifest: StremioManifest,
            sourceUrl: String,
        ): ManifestException? {
            return when {
                manifest.id.isBlank() ->
                    ManifestParsingException(
                        "Manifest ID cannot be empty",
                        url = sourceUrl,
                    )
                manifest.name.isBlank() ->
                    ManifestParsingException(
                        "Manifest name cannot be empty",
                        url = sourceUrl,
                    )
                manifest.version.isBlank() ->
                    ManifestParsingException(
                        "Manifest version cannot be empty",
                        url = sourceUrl,
                    )
                manifest.resources.isEmpty() && manifest.catalogs.isEmpty() ->
                    ManifestParsingException(
                        "Manifest must have at least one resource or catalog",
                        url = sourceUrl,
                    )
                else -> null
            }
        }

        /**
         * Convert content to normalized manifest format
         */
        fun convertToScraperManifest(
            stremioManifest: StremioManifest,
            sourceUrl: String,
            baseUrl: String = extractBaseUrl(sourceUrl),
        ): ScraperManifest {
            val now = Date()

            return ScraperManifest(
                id = stremioManifest.id,
                name = stremioManifest.id,
                displayName = stremioManifest.name,
                version = stremioManifest.version,
                description = stremioManifest.description,
                author = extractAuthorFromEmail(stremioManifest.contactEmail),
                logo = stremioManifest.logo,
                background = stremioManifest.background,
                contactEmail = stremioManifest.contactEmail,
                baseUrl = baseUrl,
                sourceUrl = sourceUrl,
                stremioManifest = stremioManifest,
                configuration = extractConfiguration(stremioManifest),
                metadata =
                    com.rdwatch.androidtv.scraper.models.ManifestMetadata(
                        sourceUrl = sourceUrl,
                        createdAt = now,
                        updatedAt = now,
                        capabilities = extractCapabilities(stremioManifest),
                    ),
            )
        }

        /**
         * Extract base URL from source URL
         */
        private fun extractBaseUrl(sourceUrl: String): String {
            return try {
                val url = java.net.URL(sourceUrl)
                "${url.protocol}://${url.host}${if (url.port != -1) ":${url.port}" else ""}"
            } catch (e: Exception) {
                sourceUrl.substringBeforeLast("/")
            }
        }

        /**
         * Extract author from contact email
         */
        private fun extractAuthorFromEmail(email: String?): String? {
            return email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        }

        /**
         * Extract configuration from Stremio manifest
         */
        private fun extractConfiguration(manifest: StremioManifest): com.rdwatch.androidtv.scraper.models.ManifestConfiguration {
            return com.rdwatch.androidtv.scraper.models.ManifestConfiguration(
                supportedTypes = manifest.types,
                requiresAuth = manifest.behaviorHints?.configurationRequired == true,
            )
        }

        /**
         * Extract capabilities from Stremio manifest
         */
        private fun extractCapabilities(manifest: StremioManifest): List<com.rdwatch.androidtv.scraper.models.ManifestCapability> {
            val capabilities = mutableListOf<com.rdwatch.androidtv.scraper.models.ManifestCapability>()

            if (manifest.catalogs.isNotEmpty()) {
                capabilities.add(com.rdwatch.androidtv.scraper.models.ManifestCapability.CATALOG)
            }

            manifest.resources.forEach { resource ->
                when (resource.name.lowercase()) {
                    "stream" -> capabilities.add(com.rdwatch.androidtv.scraper.models.ManifestCapability.STREAM)
                    "meta" -> capabilities.add(com.rdwatch.androidtv.scraper.models.ManifestCapability.META)
                    "subtitles" -> capabilities.add(com.rdwatch.androidtv.scraper.models.ManifestCapability.SUBTITLES)
                    "addon_catalog" -> capabilities.add(com.rdwatch.androidtv.scraper.models.ManifestCapability.ADDON_CATALOG)
                }
            }

            manifest.behaviorHints?.let { hints ->
                if (hints.p2p == true) {
                    capabilities.add(com.rdwatch.androidtv.scraper.models.ManifestCapability.P2P)
                }
                if (hints.configurable == true) {
                    capabilities.add(com.rdwatch.androidtv.scraper.models.ManifestCapability.CONFIGURABLE)
                }
            }

            return capabilities.distinct()
        }
    }

/**
 * Supported manifest formats
 */
enum class ManifestFormat {
    AUTO_DETECT,
    JSON,
    YAML,
    XML,
}
