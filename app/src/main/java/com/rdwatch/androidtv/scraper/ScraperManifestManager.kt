package com.rdwatch.androidtv.scraper

import com.rdwatch.androidtv.scraper.cache.CacheConfig
import com.rdwatch.androidtv.scraper.cache.ManifestCache
import com.rdwatch.androidtv.scraper.cache.SmartCacheManager
import com.rdwatch.androidtv.scraper.models.ManifestNetworkException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ManifestStorageException
import com.rdwatch.androidtv.scraper.models.ManifestValidationException
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.ValidationStatus
import com.rdwatch.androidtv.scraper.parser.ManifestParser
import com.rdwatch.androidtv.scraper.repository.ManifestRepository
import com.rdwatch.androidtv.scraper.validation.ManifestValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central facade for all manifest operations
 * Coordinates parser, repository, cache, and validation components
 */
@Singleton
class ScraperManifestManager
    @Inject
    constructor(
        private val repository: ManifestRepository,
        private val parser: ManifestParser,
        private val validator: ManifestValidator,
        private val cache: ManifestCache,
        private val cacheConfig: CacheConfig = CacheConfig(),
    ) {
        private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val smartCache = SmartCacheManager(cache, cacheConfig)

        init {
            // Initialize background tasks
            initializeBackgroundTasks()
        }

        // === Core Operations ===

        /**
         * Get manifest by ID with caching
         */
        suspend fun getManifest(id: String): ManifestResult<ScraperManifest> {
            return try {
                // Try cache first
                when (val cacheResult = smartCache.get(id)) {
                    is ManifestResult.Success -> {
                        cacheResult.data?.let {
                            return ManifestResult.Success(it)
                        }
                    }
                    is ManifestResult.Error -> { /* Continue to repository */ }
                }

                // Fallback to repository
                when (val repoResult = repository.getManifest(id)) {
                    is ManifestResult.Success -> {
                        val manifest = repoResult.data

                        // Cache the result
                        smartCache.put(id, manifest)

                        ManifestResult.Success(manifest)
                    }
                    is ManifestResult.Error -> repoResult
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Failed to get manifest: ${e.message}", e, operation = "getManifest"),
                )
            }
        }

        /**
         * Get all manifests with caching
         */
        suspend fun getAllManifests(): ManifestResult<List<ScraperManifest>> {
            return repository.getManifests()
        }

        /**
         * Get enabled manifests with caching
         */
        suspend fun getEnabledManifests(): ManifestResult<List<ScraperManifest>> {
            return repository.getEnabledManifests()
        }

        /**
         * Add manifest from URL with full processing pipeline
         */
        suspend fun addManifestFromUrl(url: String): ManifestResult<ScraperManifest> {
            return try {
                // 1. Fetch and parse
                val fetchResult = repository.fetchManifestFromUrl(url)
                when (fetchResult) {
                    is ManifestResult.Success -> {
                        val manifest = fetchResult.data

                        // 2. Validate
                        when (val validationResult = validator.validateScraperManifest(manifest)) {
                            is ManifestResult.Success -> {
                                // 3. Save to repository
                                when (val saveResult = repository.saveManifest(manifest)) {
                                    is ManifestResult.Success -> {
                                        val savedManifest = saveResult.data

                                        // 4. Update cache
                                        smartCache.put(savedManifest.id, savedManifest)

                                        ManifestResult.Success(savedManifest)
                                    }
                                    is ManifestResult.Error -> saveResult
                                }
                            }
                            is ManifestResult.Error -> {
                                // Save with validation error status
                                val invalidManifest =
                                    manifest.copy(
                                        metadata =
                                            manifest.metadata.copy(
                                                validationStatus = ValidationStatus.INVALID,
                                                lastError = validationResult.exception.message,
                                            ),
                                    )
                                repository.saveManifest(invalidManifest)
                                validationResult
                            }
                        }
                    }
                    is ManifestResult.Error -> fetchResult
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestNetworkException("Failed to add manifest from URL: ${e.message}", e, url = url),
                )
            }
        }

        /**
         * Update existing manifest
         */
        suspend fun updateManifest(manifest: ScraperManifest): ManifestResult<ScraperManifest> {
            return try {
                // 1. Validate
                when (val validationResult = validator.validateScraperManifest(manifest)) {
                    is ManifestResult.Success -> {
                        // 2. Update repository
                        when (val updateResult = repository.updateManifest(manifest)) {
                            is ManifestResult.Success -> {
                                val updatedManifest = updateResult.data

                                // 3. Update cache
                                smartCache.update(updatedManifest.id, updatedManifest)

                                ManifestResult.Success(updatedManifest)
                            }
                            is ManifestResult.Error -> updateResult
                        }
                    }
                    is ManifestResult.Error -> validationResult
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Failed to update manifest: ${e.message}", e, operation = "updateManifest"),
                )
            }
        }

        /**
         * Remove manifest
         */
        suspend fun removeManifest(id: String): ManifestResult<Unit> {
            return try {
                // 1. Remove from repository
                when (val removeResult = repository.deleteManifest(id)) {
                    is ManifestResult.Success -> {
                        // 2. Remove from cache
                        cache.remove(id)

                        ManifestResult.Success(Unit)
                    }
                    is ManifestResult.Error -> removeResult
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Failed to remove manifest: ${e.message}", e, operation = "removeManifest"),
                )
            }
        }

        // === Bulk Operations ===

        /**
         * Import multiple manifests from URLs
         */
        suspend fun importManifests(urls: List<String>): ManifestResult<ImportResult> {
            return try {
                val results = mutableListOf<ManifestImportResult>()

                urls.forEach { url ->
                    val result =
                        when (val addResult = addManifestFromUrl(url)) {
                            is ManifestResult.Success ->
                                ManifestImportResult(
                                    url = url,
                                    success = true,
                                    manifest = addResult.data,
                                    error = null,
                                )
                            is ManifestResult.Error ->
                                ManifestImportResult(
                                    url = url,
                                    success = false,
                                    manifest = null,
                                    error = addResult.exception.message,
                                )
                        }
                    results.add(result)
                }

                val successCount = results.count { it.success }
                val failureCount = results.size - successCount

                ManifestResult.Success(
                    ImportResult(
                        totalProcessed = results.size,
                        successCount = successCount,
                        failureCount = failureCount,
                        results = results,
                    ),
                )
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Bulk import failed: ${e.message}", e, operation = "bulkImport"),
                )
            }
        }

        /**
         * Refresh all manifests from their source URLs
         */
        suspend fun refreshAllManifests(): ManifestResult<RefreshResult> {
            return try {
                when (val manifestsResult = repository.getManifests()) {
                    is ManifestResult.Success -> {
                        val manifests = manifestsResult.data
                        val results = mutableListOf<ManifestRefreshResult>()

                        manifests.forEach { manifest ->
                            val result =
                                when (val refreshResult = refreshManifest(manifest.id)) {
                                    is ManifestResult.Success ->
                                        ManifestRefreshResult(
                                            id = manifest.id,
                                            success = true,
                                            updatedManifest = refreshResult.data,
                                            error = null,
                                        )
                                    is ManifestResult.Error ->
                                        ManifestRefreshResult(
                                            id = manifest.id,
                                            success = false,
                                            updatedManifest = null,
                                            error = refreshResult.exception.message,
                                        )
                                }
                            results.add(result)
                        }

                        val successCount = results.count { it.success }
                        val failureCount = results.size - successCount

                        ManifestResult.Success(
                            RefreshResult(
                                totalProcessed = results.size,
                                successCount = successCount,
                                failureCount = failureCount,
                                results = results,
                            ),
                        )
                    }
                    is ManifestResult.Error ->
                        ManifestResult.Error(
                            ManifestStorageException(
                                "Failed to get manifests for refresh: ${manifestsResult.exception.message}",
                                manifestsResult.exception,
                                operation = "refreshAll",
                            ),
                        )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Refresh all failed: ${e.message}", e, operation = "refreshAll"),
                )
            }
        }

        /**
         * Refresh single manifest
         */
        suspend fun refreshManifest(id: String): ManifestResult<ScraperManifest> {
            return try {
                when (val refreshResult = repository.refreshManifest(id)) {
                    is ManifestResult.Success -> {
                        val refreshedManifest = refreshResult.data

                        // Update cache
                        smartCache.update(refreshedManifest.id, refreshedManifest)

                        ManifestResult.Success(refreshedManifest)
                    }
                    is ManifestResult.Error -> refreshResult
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestNetworkException("Failed to refresh manifest: ${e.message}", e),
                )
            }
        }

        // === Validation Operations ===

        /**
         * Validate all manifests
         */
        suspend fun validateAllManifests(): ManifestResult<ValidationSummary> {
            return try {
                when (val manifestsResult = repository.getManifests()) {
                    is ManifestResult.Success -> {
                        val manifests = manifestsResult.data
                        val results = mutableMapOf<String, Boolean>()
                        val errors = mutableMapOf<String, String>()

                        manifests.forEach { manifest ->
                            when (val validationResult = validator.validateScraperManifest(manifest)) {
                                is ManifestResult.Success -> {
                                    results[manifest.id] = validationResult.data
                                }
                                is ManifestResult.Error -> {
                                    results[manifest.id] = false
                                    errors[manifest.id] = validationResult.exception.message ?: "Unknown error"
                                }
                            }
                        }

                        val validCount = results.values.count { it }
                        val invalidCount = results.size - validCount

                        ManifestResult.Success(
                            ValidationSummary(
                                totalManifests = results.size,
                                validManifests = validCount,
                                invalidManifests = invalidCount,
                                validationResults = results,
                                errors = errors,
                            ),
                        )
                    }
                    is ManifestResult.Error ->
                        ManifestResult.Error(
                            ManifestValidationException(
                                "Failed to get manifests for validation: ${manifestsResult.exception.message}",
                                manifestsResult.exception,
                            ),
                        )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestValidationException("Validation failed: ${e.message}", e),
                )
            }
        }

        // === Search and Query Operations ===

        /**
         * Search manifests
         */
        suspend fun searchManifests(query: String): ManifestResult<List<ScraperManifest>> {
            return repository.searchManifests(query)
        }

        /**
         * Get manifests by author
         */
        suspend fun getManifestsByAuthor(author: String): ManifestResult<List<ScraperManifest>> {
            return repository.getManifestsByAuthor(author)
        }

        /**
         * Get manifests by capability
         */
        suspend fun getManifestsByCapability(
            capability: com.rdwatch.androidtv.scraper.models.ManifestCapability,
        ): ManifestResult<List<ScraperManifest>> {
            return repository.getManifestsByCapability(capability)
        }

        // === Status Management ===

        /**
         * Enable/disable manifest
         */
        suspend fun setManifestEnabled(
            id: String,
            enabled: Boolean,
        ): ManifestResult<Unit> {
            return try {
                when (val result = repository.enableManifest(id, enabled)) {
                    is ManifestResult.Success -> {
                        // Update cache if manifest exists
                        when (val manifestResult = getManifest(id)) {
                            is ManifestResult.Success -> {
                                val updatedManifest = manifestResult.data.copy(isEnabled = enabled)
                                smartCache.update(id, updatedManifest)
                            }
                            is ManifestResult.Error -> { /* Manifest not in cache */ }
                        }
                        result
                    }
                    is ManifestResult.Error -> result
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Failed to set manifest enabled status: ${e.message}", e, operation = "setEnabled"),
                )
            }
        }

        /**
         * Update manifest priority
         */
        suspend fun updateManifestPriority(
            id: String,
            priority: Int,
        ): ManifestResult<Unit> {
            return try {
                when (val result = repository.updateManifestPriority(id, priority)) {
                    is ManifestResult.Success -> {
                        // Update cache if manifest exists
                        when (val manifestResult = getManifest(id)) {
                            is ManifestResult.Success -> {
                                val updatedManifest = manifestResult.data.copy(priorityOrder = priority)
                                smartCache.update(id, updatedManifest)
                            }
                            is ManifestResult.Error -> { /* Manifest not in cache */ }
                        }
                        result
                    }
                    is ManifestResult.Error -> result
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Failed to update manifest priority: ${e.message}", e, operation = "updatePriority"),
                )
            }
        }

        // === Statistics and Monitoring ===

        /**
         * Get comprehensive statistics
         */
        suspend fun getStatistics(): ManifestResult<ManagerStatistics> {
            return try {
                val repoStats = repository.getManifestStatistics()
                val cacheStats = cache.getStatistics()
                val smartCacheStats = smartCache.getUsageStatistics()

                when (repoStats) {
                    is ManifestResult.Success -> {
                        ManifestResult.Success(
                            ManagerStatistics(
                                repositoryStats = repoStats.data,
                                cacheStats = cacheStats,
                                usageStats = smartCacheStats,
                                lastRefreshTime = Date(), // TODO: Track actual refresh times
                            ),
                        )
                    }
                    is ManifestResult.Error ->
                        repoStats.map {
                            ManagerStatistics(
                                repositoryStats = null,
                                cacheStats = cacheStats,
                                usageStats = smartCacheStats,
                                lastRefreshTime = null,
                            )
                        }
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Failed to get statistics: ${e.message}", e, operation = "getStatistics"),
                )
            }
        }

        // === Reactive Operations ===

        /**
         * Observe all manifests
         */
        fun observeManifests(): Flow<ManifestResult<List<ScraperManifest>>> {
            return repository.observeManifests()
        }

        /**
         * Observe enabled manifests
         */
        fun observeEnabledManifests(): Flow<ManifestResult<List<ScraperManifest>>> {
            return repository.observeEnabledManifests()
        }

        /**
         * Observe specific manifest
         */
        fun observeManifest(id: String): Flow<ManifestResult<ScraperManifest>> {
            return repository.observeManifest(id)
        }

        // === Lifecycle Management ===

        /**
         * Initialize manager with default manifests
         */
        suspend fun initialize(): ManifestResult<Unit> {
            return try {
                // Preload cache if configured
                if (cacheConfig.preloadOnStartup) {
                    cache.preloadFromRepository()
                }

                // Clean up expired cache entries
                cache.evictExpired()

                ManifestResult.Success(Unit)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Failed to initialize manager: ${e.message}", e, operation = "initialize"),
                )
            }
        }

        /**
         * Shutdown manager and cleanup resources
         */
        suspend fun shutdown(): ManifestResult<Unit> {
            return try {
                // Cancel background tasks
                managerScope.launch { }.cancel()

                // Clear cache
                cache.clear()

                ManifestResult.Success(Unit)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException("Failed to shutdown manager: ${e.message}", e, operation = "shutdown"),
                )
            }
        }

        // === Private Helper Methods ===

        private fun initializeBackgroundTasks() {
            // Start background cache cleanup
            if (cacheConfig.backgroundCleanupIntervalMinutes > 0) {
                managerScope.launch {
                    startBackgroundCleanup()
                }
            }
        }

        private suspend fun startBackgroundCleanup() {
            // Implementation for periodic cache cleanup
            // This would run every X minutes to clean expired entries
        }
    }

// === Data Classes for Results ===

data class ImportResult(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<ManifestImportResult>,
)

data class ManifestImportResult(
    val url: String,
    val success: Boolean,
    val manifest: ScraperManifest?,
    val error: String?,
)

data class RefreshResult(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<ManifestRefreshResult>,
)

data class ManifestRefreshResult(
    val id: String,
    val success: Boolean,
    val updatedManifest: ScraperManifest?,
    val error: String?,
)

data class ValidationSummary(
    val totalManifests: Int,
    val validManifests: Int,
    val invalidManifests: Int,
    val validationResults: Map<String, Boolean>,
    val errors: Map<String, String>,
)

data class ManagerStatistics(
    val repositoryStats: com.rdwatch.androidtv.scraper.repository.ManifestStatistics?,
    val cacheStats: com.rdwatch.androidtv.scraper.cache.CacheStatistics,
    val usageStats: com.rdwatch.androidtv.scraper.cache.UsageStatistics,
    val lastRefreshTime: Date?,
)
