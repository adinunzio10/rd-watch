package com.rdwatch.androidtv.scraper.parser

import com.rdwatch.androidtv.data.entities.ScraperManifestEntity
import com.rdwatch.androidtv.scraper.models.ManifestEntry
import com.rdwatch.androidtv.scraper.models.ManifestMetadata
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.ValidationStatus
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converter between different manifest representations
 */
@Singleton
class ManifestConverter
    @Inject
    constructor() {
        private val moshi: Moshi =
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

        private val configAdapter: JsonAdapter<Map<String, Any>> =
            moshi.adapter(Map::class.java) as JsonAdapter<Map<String, Any>>

        /**
         * Convert ScraperManifest to ScraperManifestEntity for database storage
         */
        fun toEntity(manifest: ScraperManifest): ScraperManifestEntity {
            val configJson =
                try {
                    val configMap =
                        mapOf(
                            "providers" to manifest.configuration.providers,
                            "qualityFilters" to manifest.configuration.qualityFilters,
                            "sortingOptions" to manifest.configuration.sortingOptions,
                            "rateLimitMs" to manifest.configuration.rateLimitMs,
                            "timeoutSeconds" to manifest.configuration.timeoutSeconds,
                            "retryAttempts" to manifest.configuration.retryAttempts,
                            "cacheTtlMinutes" to manifest.configuration.cacheTtlMinutes,
                            "supportedTypes" to manifest.configuration.supportedTypes,
                            "requiresAuth" to manifest.configuration.requiresAuth,
                            "additionalParams" to manifest.configuration.additionalParams,
                            "stremioManifest" to moshi.adapter(com.rdwatch.androidtv.scraper.models.StremioManifest::class.java).toJson(manifest.stremioManifest),
                            "capabilities" to manifest.metadata.capabilities.map { it.name },
                        )
                    configAdapter.toJson(configMap)
                } catch (e: Exception) {
                    "{}"
                }

            return ScraperManifestEntity(
                scraperName = manifest.name,
                displayName = manifest.displayName,
                version = manifest.version,
                baseUrl = manifest.baseUrl,
                configJson = configJson,
                isEnabled = manifest.isEnabled,
                priorityOrder = manifest.priorityOrder,
                rateLimitMs = manifest.configuration.rateLimitMs,
                timeoutSeconds = manifest.configuration.timeoutSeconds,
                description = manifest.description,
                author = manifest.author,
                createdAt = manifest.metadata.createdAt,
                updatedAt = manifest.metadata.updatedAt,
            )
        }

        /**
         * Convert ScraperManifestEntity to ScraperManifest
         */
        fun fromEntity(entity: ScraperManifestEntity): ScraperManifest? {
            return try {
                val configMap = configAdapter.fromJson(entity.configJson) ?: emptyMap()

                val stremioManifestJson = configMap["stremioManifest"] as? String
                val stremioManifest =
                    stremioManifestJson?.let { json ->
                        moshi.adapter(com.rdwatch.androidtv.scraper.models.StremioManifest::class.java).fromJson(json)
                    } ?: return null

                val capabilities =
                    (configMap["capabilities"] as? List<*>)?.mapNotNull { capability ->
                        try {
                            com.rdwatch.androidtv.scraper.models.ManifestCapability.valueOf(capability.toString())
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()

                ScraperManifest(
                    id = entity.scraperName,
                    name = entity.scraperName,
                    displayName = entity.displayName,
                    version = entity.version,
                    description = entity.description,
                    author = entity.author,
                    baseUrl = entity.baseUrl,
                    sourceUrl = "", // Not stored in entity, would need separate field
                    stremioManifest = stremioManifest,
                    configuration =
                        com.rdwatch.androidtv.scraper.models.ManifestConfiguration(
                            providers = getListFromConfig(configMap, "providers"),
                            qualityFilters = getListFromConfig(configMap, "qualityFilters"),
                            sortingOptions = getListFromConfig(configMap, "sortingOptions"),
                            rateLimitMs = entity.rateLimitMs,
                            timeoutSeconds = entity.timeoutSeconds,
                            retryAttempts = getLongFromConfig(configMap, "retryAttempts", 3).toInt(),
                            cacheTtlMinutes = getLongFromConfig(configMap, "cacheTtlMinutes", 60).toInt(),
                            supportedTypes = getListFromConfig(configMap, "supportedTypes"),
                            requiresAuth = getBooleanFromConfig(configMap, "requiresAuth", false),
                            additionalParams = getMapFromConfig(configMap, "additionalParams"),
                        ),
                    metadata =
                        ManifestMetadata(
                            sourceUrl = "", // Not stored in entity
                            createdAt = entity.createdAt,
                            updatedAt = entity.updatedAt,
                            capabilities = capabilities,
                        ),
                    isEnabled = entity.isEnabled,
                    priorityOrder = entity.priorityOrder,
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Convert ScraperManifest to ManifestEntry
         */
        fun toManifestEntry(
            manifest: ScraperManifest,
            manifestId: Long = 0,
        ): ManifestEntry {
            val configJson =
                try {
                    val configMap =
                        mapOf(
                            "providers" to manifest.configuration.providers,
                            "qualityFilters" to manifest.configuration.qualityFilters,
                            "sortingOptions" to manifest.configuration.sortingOptions,
                            "rateLimitMs" to manifest.configuration.rateLimitMs,
                            "timeoutSeconds" to manifest.configuration.timeoutSeconds,
                            "retryAttempts" to manifest.configuration.retryAttempts,
                            "cacheTtlMinutes" to manifest.configuration.cacheTtlMinutes,
                            "supportedTypes" to manifest.configuration.supportedTypes,
                            "requiresAuth" to manifest.configuration.requiresAuth,
                            "additionalParams" to manifest.configuration.additionalParams,
                        )
                    configAdapter.toJson(configMap)
                } catch (e: Exception) {
                    "{}"
                }

            return ManifestEntry(
                manifestId = manifestId,
                scraperName = manifest.name,
                displayName = manifest.displayName,
                version = manifest.version,
                baseUrl = manifest.baseUrl,
                sourceUrl = manifest.sourceUrl,
                configJson = configJson,
                isEnabled = manifest.isEnabled,
                priorityOrder = manifest.priorityOrder,
                rateLimitMs = manifest.configuration.rateLimitMs,
                timeoutSeconds = manifest.configuration.timeoutSeconds,
                description = manifest.description,
                author = manifest.author,
                etag = manifest.metadata.etag,
                lastModified = manifest.metadata.lastModified,
                createdAt = manifest.metadata.createdAt,
                updatedAt = manifest.metadata.updatedAt,
                lastFetchedAt = manifest.metadata.lastFetchedAt,
                fetchAttempts = manifest.metadata.fetchAttempts,
                lastError = manifest.metadata.lastError,
                validationStatus = manifest.metadata.validationStatus.name,
            )
        }

        /**
         * Convert ManifestEntry to ScraperManifest
         */
        fun fromManifestEntry(entry: ManifestEntry): ScraperManifest? {
            return try {
                val configMap = configAdapter.fromJson(entry.configJson) ?: emptyMap()

                // Create a minimal StremioManifest since we don't have the full data
                val stremioManifest =
                    com.rdwatch.androidtv.scraper.models.StremioManifest(
                        id = entry.scraperName,
                        name = entry.displayName,
                        version = entry.version,
                        description = entry.description,
                    )

                ScraperManifest(
                    id = entry.scraperName,
                    name = entry.scraperName,
                    displayName = entry.displayName,
                    version = entry.version,
                    description = entry.description,
                    author = entry.author,
                    baseUrl = entry.baseUrl,
                    sourceUrl = entry.sourceUrl,
                    stremioManifest = stremioManifest,
                    configuration =
                        com.rdwatch.androidtv.scraper.models.ManifestConfiguration(
                            providers = getListFromConfig(configMap, "providers"),
                            qualityFilters = getListFromConfig(configMap, "qualityFilters"),
                            sortingOptions = getListFromConfig(configMap, "sortingOptions"),
                            rateLimitMs = entry.rateLimitMs,
                            timeoutSeconds = entry.timeoutSeconds,
                            retryAttempts = getLongFromConfig(configMap, "retryAttempts", 3).toInt(),
                            cacheTtlMinutes = getLongFromConfig(configMap, "cacheTtlMinutes", 60).toInt(),
                            supportedTypes = getListFromConfig(configMap, "supportedTypes"),
                            requiresAuth = getBooleanFromConfig(configMap, "requiresAuth", false),
                            additionalParams = getMapFromConfig(configMap, "additionalParams"),
                        ),
                    metadata =
                        ManifestMetadata(
                            sourceUrl = entry.sourceUrl,
                            etag = entry.etag,
                            lastModified = entry.lastModified,
                            createdAt = entry.createdAt,
                            updatedAt = entry.updatedAt,
                            lastFetchedAt = entry.lastFetchedAt,
                            fetchAttempts = entry.fetchAttempts,
                            lastError = entry.lastError,
                            validationStatus =
                                try {
                                    ValidationStatus.valueOf(entry.validationStatus)
                                } catch (e: Exception) {
                                    ValidationStatus.PENDING
                                },
                        ),
                    isEnabled = entry.isEnabled,
                    priorityOrder = entry.priorityOrder,
                )
            } catch (e: Exception) {
                null
            }
        }

        // Helper methods for safe config extraction
        private fun getListFromConfig(
            config: Map<String, Any>,
            key: String,
        ): List<String> {
            return (config[key] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        }

        private fun getMapFromConfig(
            config: Map<String, Any>,
            key: String,
        ): Map<String, String> {
            return (config[key] as? Map<*, *>)?.mapNotNull { (k, v) ->
                k?.toString()?.let { keyStr -> v?.toString()?.let { valueStr -> keyStr to valueStr } }
            }?.toMap() ?: emptyMap()
        }

        private fun getBooleanFromConfig(
            config: Map<String, Any>,
            key: String,
            default: Boolean,
        ): Boolean {
            return (config[key] as? Boolean) ?: default
        }

        private fun getLongFromConfig(
            config: Map<String, Any>,
            key: String,
            default: Long,
        ): Long {
            return when (val value = config[key]) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: default
                else -> default
            }
        }
    }
