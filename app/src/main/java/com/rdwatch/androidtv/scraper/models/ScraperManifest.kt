package com.rdwatch.androidtv.scraper.models

import java.util.Date

/**
 * Internal representation of a scraper manifest
 * Combines Stremio manifest data with our app-specific metadata
 */
data class ScraperManifest(
    val id: String,
    val name: String,
    val displayName: String,
    val version: String,
    val description: String? = null,
    val author: String? = null,
    val logo: String? = null,
    val background: String? = null,
    val contactEmail: String? = null,
    val baseUrl: String,
    val sourceUrl: String,
    val stremioManifest: StremioManifest,
    val configuration: ManifestConfiguration = ManifestConfiguration(),
    val metadata: ManifestMetadata,
    val isEnabled: Boolean = true,
    val priorityOrder: Int = 0,
)

/**
 * Configuration parameters extracted from the manifest
 */
data class ManifestConfiguration(
    val providers: List<String> = emptyList(),
    val qualityFilters: List<String> = emptyList(),
    val sortingOptions: List<String> = emptyList(),
    val rateLimitMs: Long = 1000,
    val timeoutSeconds: Int = 30,
    val retryAttempts: Int = 3,
    val cacheTtlMinutes: Int = 60,
    val supportedTypes: List<String> = emptyList(),
    val requiresAuth: Boolean = false,
    val additionalParams: Map<String, String> = emptyMap(),
)

/**
 * Metadata about the manifest entry
 */
data class ManifestMetadata(
    val sourceUrl: String,
    val etag: String? = null,
    val lastModified: String? = null,
    val createdAt: Date,
    val updatedAt: Date,
    val lastFetchedAt: Date? = null,
    val fetchAttempts: Int = 0,
    val lastError: String? = null,
    val validationStatus: ValidationStatus = ValidationStatus.PENDING,
    val capabilities: List<ManifestCapability> = emptyList(),
)

/**
 * Validation status of the manifest
 */
enum class ValidationStatus {
    PENDING,
    VALID,
    INVALID,
    ERROR,
    OUTDATED,
}

/**
 * Capabilities supported by the manifest
 */
enum class ManifestCapability {
    CATALOG,
    STREAM,
    META,
    SUBTITLES,
    ADDON_CATALOG,
    P2P,
    CONFIGURABLE,
}

/**
 * Manifest entry for storage and listing
 */
data class ManifestEntry(
    val manifestId: Long = 0,
    val scraperName: String,
    val displayName: String,
    val version: String,
    val baseUrl: String,
    val sourceUrl: String,
    val configJson: String,
    val isEnabled: Boolean = true,
    val priorityOrder: Int = 0,
    val rateLimitMs: Long = 1000,
    val timeoutSeconds: Int = 30,
    val description: String? = null,
    val author: String? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val createdAt: Date,
    val updatedAt: Date,
    val lastFetchedAt: Date? = null,
    val fetchAttempts: Int = 0,
    val lastError: String? = null,
    val validationStatus: String = ValidationStatus.PENDING.name,
)
