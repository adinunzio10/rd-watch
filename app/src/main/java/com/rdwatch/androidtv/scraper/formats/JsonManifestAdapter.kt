package com.rdwatch.androidtv.scraper.formats

import com.rdwatch.androidtv.scraper.models.ManifestParsingException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.StremioManifest
import com.rdwatch.androidtv.scraper.models.StremioManifestBehaviorHints
import com.rdwatch.androidtv.scraper.models.StremioManifestCatalog
import com.rdwatch.androidtv.scraper.models.StremioManifestExtra
import com.rdwatch.androidtv.scraper.models.StremioManifestResource
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced JSON adapter for Stremio manifests with error handling and validation
 */
@Singleton
class JsonManifestAdapter
    @Inject
    constructor() {
        private val moshi: Moshi =
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

        private val manifestAdapter: JsonAdapter<StremioManifest> =
            moshi.adapter(StremioManifest::class.java)

        private val rawJsonAdapter: JsonAdapter<Map<String, Any>> =
            moshi.adapter(Map::class.java) as JsonAdapter<Map<String, Any>>

        /**
         * Parse JSON with enhanced error handling and fallback mechanisms
         */
        fun parseJson(
            json: String,
            sourceUrl: String? = null,
        ): ManifestResult<StremioManifest> {
            return try {
                // First, try direct parsing
                val manifest = manifestAdapter.fromJson(json)
                if (manifest != null) {
                    return ManifestResult.Success(manifest)
                }

                // If direct parsing fails, try manual parsing with fallbacks
                val rawData = rawJsonAdapter.fromJson(json)
                if (rawData != null) {
                    return parseFromRawData(rawData, sourceUrl)
                }

                ManifestResult.Error(
                    ManifestParsingException(
                        "Failed to parse JSON: null result from both direct and manual parsing",
                        url = sourceUrl,
                        format = "JSON",
                    ),
                )
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
         * Serialize manifest to JSON with proper formatting
         */
        fun toJson(
            manifest: StremioManifest,
            pretty: Boolean = true,
        ): ManifestResult<String> {
            return try {
                val adapter =
                    if (pretty) {
                        manifestAdapter.indent("  ")
                    } else {
                        manifestAdapter
                    }

                val json = adapter.toJson(manifest)
                ManifestResult.Success(json)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestParsingException(
                        "JSON serialization failed: ${e.message}",
                        cause = e,
                        format = "JSON",
                    ),
                )
            }
        }

        /**
         * Parse from raw JSON data with type coercion and fallbacks
         */
        private fun parseFromRawData(
            data: Map<String, Any>,
            sourceUrl: String?,
        ): ManifestResult<StremioManifest> {
            return try {
                val manifest =
                    StremioManifest(
                        id = extractString(data, "id") ?: return errorResult("Missing required field: id", sourceUrl),
                        name = extractString(data, "name") ?: return errorResult("Missing required field: name", sourceUrl),
                        version = extractString(data, "version") ?: return errorResult("Missing required field: version", sourceUrl),
                        description = extractString(data, "description"),
                        logo = extractString(data, "logo"),
                        background = extractString(data, "background"),
                        contactEmail = extractString(data, "contactEmail"),
                        catalogs = extractCatalogs(data),
                        resources = extractResources(data),
                        types = extractStringList(data, "types"),
                        idPrefixes = extractStringList(data, "idPrefixes"),
                        behaviorHints = extractBehaviorHints(data),
                    )

                ManifestResult.Success(manifest)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestParsingException(
                        "Manual parsing failed: ${e.message}",
                        cause = e,
                        url = sourceUrl,
                        format = "JSON",
                    ),
                )
            }
        }

        private fun extractString(
            data: Map<String, Any>,
            key: String,
        ): String? {
            return when (val value = data[key]) {
                is String -> value.takeIf { it.isNotBlank() }
                is Number -> value.toString()
                else -> null
            }
        }

        private fun extractStringList(
            data: Map<String, Any>,
            key: String,
        ): List<String> {
            return when (val value = data[key]) {
                is List<*> ->
                    value.mapNotNull {
                        when (it) {
                            is String -> it
                            is Number -> it.toString()
                            else -> null
                        }
                    }
                is String -> if (value.isNotBlank()) listOf(value) else emptyList()
                else -> emptyList()
            }
        }

        private fun extractCatalogs(data: Map<String, Any>): List<StremioManifestCatalog> {
            val catalogsData = data["catalogs"] as? List<*> ?: return emptyList()

            return catalogsData.mapNotNull { catalogItem ->
                if (catalogItem is Map<*, *>) {
                    try {
                        val catalogMap = catalogItem as Map<String, Any>
                        StremioManifestCatalog(
                            type = extractString(catalogMap, "type") ?: return@mapNotNull null,
                            id = extractString(catalogMap, "id") ?: return@mapNotNull null,
                            name = extractString(catalogMap, "name") ?: return@mapNotNull null,
                            genres = extractStringList(catalogMap, "genres").takeIf { it.isNotEmpty() },
                            extra = extractExtras(catalogMap),
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
        }

        private fun extractResources(data: Map<String, Any>): List<StremioManifestResource> {
            val resourcesData = data["resources"] as? List<*> ?: return emptyList()

            return resourcesData.mapNotNull { resourceItem ->
                if (resourceItem is Map<*, *>) {
                    try {
                        val resourceMap = resourceItem as Map<String, Any>
                        StremioManifestResource(
                            name = extractString(resourceMap, "name") ?: return@mapNotNull null,
                            types =
                                extractStringList(resourceMap, "types").takeIf { it.isNotEmpty() }
                                    ?: return@mapNotNull null,
                            idPrefixes = extractStringList(resourceMap, "idPrefixes").takeIf { it.isNotEmpty() },
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
        }

        private fun extractExtras(data: Map<String, Any>): List<StremioManifestExtra>? {
            val extrasData = data["extra"] as? List<*> ?: return null

            val extras =
                extrasData.mapNotNull { extraItem ->
                    if (extraItem is Map<*, *>) {
                        try {
                            val extraMap = extraItem as Map<String, Any>
                            StremioManifestExtra(
                                name = extractString(extraMap, "name") ?: return@mapNotNull null,
                                isRequired = extractBoolean(extraMap, "isRequired"),
                                options = extractStringList(extraMap, "options").takeIf { it.isNotEmpty() },
                                optionsLimit = extractInt(extraMap, "optionsLimit"),
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }

            return if (extras.isNotEmpty()) extras else null
        }

        private fun extractBehaviorHints(data: Map<String, Any>): StremioManifestBehaviorHints? {
            val hintsData = data["behaviorHints"] as? Map<*, *> ?: return null

            return try {
                val hintsMap = hintsData as Map<String, Any>
                StremioManifestBehaviorHints(
                    adult = extractBoolean(hintsMap, "adult"),
                    p2p = extractBoolean(hintsMap, "p2p"),
                    configurable = extractBoolean(hintsMap, "configurable"),
                    configurationRequired = extractBoolean(hintsMap, "configurationRequired"),
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun extractBoolean(
            data: Map<String, Any>,
            key: String,
        ): Boolean? {
            return when (val value = data[key]) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                is Number -> value.toDouble() != 0.0
                else -> null
            }
        }

        private fun extractInt(
            data: Map<String, Any>,
            key: String,
        ): Int? {
            return when (val value = data[key]) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                else -> null
            }
        }

        private fun errorResult(
            message: String,
            sourceUrl: String?,
        ): ManifestResult<StremioManifest> {
            return ManifestResult.Error(
                ManifestParsingException(
                    message,
                    url = sourceUrl,
                    format = "JSON",
                ),
            )
        }

        /**
         * Validate JSON structure without full parsing
         */
        fun validateJsonStructure(json: String): ManifestResult<Boolean> {
            return try {
                val data = rawJsonAdapter.fromJson(json)
                if (data == null) {
                    ManifestResult.Error(
                        ManifestParsingException(
                            "Invalid JSON structure",
                            format = "JSON",
                        ),
                    )
                } else {
                    // Basic structure validation
                    val hasRequiredFields =
                        data.containsKey("id") &&
                            data.containsKey("name") &&
                            data.containsKey("version")

                    if (hasRequiredFields) {
                        ManifestResult.Success(true)
                    } else {
                        ManifestResult.Error(
                            ManifestParsingException(
                                "Missing required fields (id, name, version)",
                                format = "JSON",
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestParsingException(
                        "JSON validation failed: ${e.message}",
                        cause = e,
                        format = "JSON",
                    ),
                )
            }
        }

        /**
         * Extract manifest metadata without full parsing
         */
        fun extractMetadata(json: String): ManifestResult<Map<String, String>> {
            return try {
                val data = rawJsonAdapter.fromJson(json)
                if (data != null) {
                    val metadata = mutableMapOf<String, String>()

                    extractString(data, "id")?.let { metadata["id"] = it }
                    extractString(data, "name")?.let { metadata["name"] = it }
                    extractString(data, "version")?.let { metadata["version"] = it }
                    extractString(data, "description")?.let { metadata["description"] = it }
                    extractString(data, "contactEmail")?.let { metadata["contactEmail"] = it }

                    ManifestResult.Success(metadata)
                } else {
                    ManifestResult.Error(
                        ManifestParsingException(
                            "Failed to extract metadata: invalid JSON",
                            format = "JSON",
                        ),
                    )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestParsingException(
                        "Metadata extraction failed: ${e.message}",
                        cause = e,
                        format = "JSON",
                    ),
                )
            }
        }
    }
