package com.rdwatch.androidtv.scraper.config

import com.rdwatch.androidtv.scraper.ScraperManifestManager
import com.rdwatch.androidtv.scraper.error.ManifestErrorReporter
import com.rdwatch.androidtv.scraper.models.ManifestException
import com.rdwatch.androidtv.scraper.models.ManifestStorageException
import com.rdwatch.androidtv.scraper.models.ManifestNetworkException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for installing and managing default scrapers
 */
@Singleton
class ScraperInstaller @Inject constructor(
    private val manifestManager: ScraperManifestManager,
    private val defaultConfig: DefaultScrapersConfig,
    private val errorReporter: ManifestErrorReporter
) {
    
    private val installerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Installation status tracking
    private val _installationStatus = MutableStateFlow<InstallationStatus>(InstallationStatus.Idle)
    private val _scraperStatuses = MutableStateFlow<Map<String, ScraperInstallStatus>>(emptyMap())
    
    val installationStatus: Flow<InstallationStatus> = _installationStatus.asStateFlow()
    val scraperStatuses: Flow<Map<String, ScraperInstallStatus>> = _scraperStatuses.asStateFlow()
    
    /**
     * Install all default scrapers
     */
    suspend fun installDefaultScrapers(): ManifestResult<InstallationSummary> {
        return try {
            _installationStatus.value = InstallationStatus.Installing
            
            val defaultScrapers = defaultConfig.getDefaultScrapers()
            val results = mutableListOf<ScraperInstallationResult>()
            
            defaultScrapers.forEach { scraper ->
                updateScraperStatus(scraper.id, ScraperInstallStatus.INSTALLING)
                
                val result = installScraper(scraper)
                results.add(result)
                
                val status = if (result.success) ScraperInstallStatus.INSTALLED else ScraperInstallStatus.FAILED
                updateScraperStatus(scraper.id, status)
            }
            
            val summary = InstallationSummary(
                totalScrapers = defaultScrapers.size,
                successfulInstalls = results.count { it.success },
                failedInstalls = results.count { !it.success },
                results = results
            )
            
            _installationStatus.value = InstallationStatus.Completed(summary)
            ManifestResult.Success(summary)
            
        } catch (e: Exception) {
            _installationStatus.value = InstallationStatus.Failed(e.message ?: "Installation failed")
            ManifestResult.Error(
                ManifestStorageException("Failed to install default scrapers: ${e.message}", e, operation = "installDefaults")
            )
        }
    }
    
    /**
     * Install specific scraper
     */
    suspend fun installScraper(scraper: ScraperManifest): ScraperInstallationResult {
        return try {
            updateScraperStatus(scraper.id, ScraperInstallStatus.INSTALLING)
            
            when (val result = manifestManager.updateManifest(scraper)) {
                is ManifestResult.Success -> {
                    updateScraperStatus(scraper.id, ScraperInstallStatus.INSTALLED)
                    ScraperInstallationResult(
                        scraperId = scraper.id,
                        scraperName = scraper.displayName,
                        success = true,
                        manifest = result.data,
                        error = null
                    )
                }
                is ManifestResult.Error -> {
                    updateScraperStatus(scraper.id, ScraperInstallStatus.FAILED)
                    errorReporter.reportError(result.exception)
                    ScraperInstallationResult(
                        scraperId = scraper.id,
                        scraperName = scraper.displayName,
                        success = false,
                        manifest = null,
                        error = result.exception.message
                    )
                }
            }
        } catch (e: Exception) {
            updateScraperStatus(scraper.id, ScraperInstallStatus.FAILED)
            val error = "Failed to install ${scraper.displayName}: ${e.message}"
            errorReporter.reportError(ManifestStorageException(error, e, operation = "install"))
            
            ScraperInstallationResult(
                scraperId = scraper.id,
                scraperName = scraper.displayName,
                success = false,
                manifest = null,
                error = error
            )
        }
    }
    
    /**
     * Install scraper from URL
     */
    suspend fun installScraperFromUrl(url: String): ScraperInstallationResult {
        return try {
            when (val result = manifestManager.addManifestFromUrl(url)) {
                is ManifestResult.Success -> {
                    val manifest = result.data
                    updateScraperStatus(manifest.id, ScraperInstallStatus.INSTALLED)
                    
                    ScraperInstallationResult(
                        scraperId = manifest.id,
                        scraperName = manifest.displayName,
                        success = true,
                        manifest = manifest,
                        error = null
                    )
                }
                is ManifestResult.Error -> {
                    errorReporter.reportError(result.exception)
                    ScraperInstallationResult(
                        scraperId = "unknown",
                        scraperName = "Unknown (from $url)",
                        success = false,
                        manifest = null,
                        error = result.exception.message
                    )
                }
            }
        } catch (e: Exception) {
            val error = "Failed to install from URL $url: ${e.message}"
            errorReporter.reportError(ManifestNetworkException(error, e, url = url))
            
            ScraperInstallationResult(
                scraperId = "unknown",
                scraperName = "Unknown (from $url)",
                success = false,
                manifest = null,
                error = error
            )
        }
    }
    
    /**
     * Uninstall scraper
     */
    suspend fun uninstallScraper(scraperId: String): ManifestResult<Unit> {
        return try {
            updateScraperStatus(scraperId, ScraperInstallStatus.NOT_INSTALLED)
            manifestManager.removeManifest(scraperId)
        } catch (e: Exception) {
            ManifestResult.Error(
                ManifestStorageException("Failed to uninstall scraper: ${e.message}", e, operation = "uninstall")
            )
        }
    }
    
    /**
     * Check which default scrapers are installed
     */
    suspend fun checkInstalledScrapers(): ManifestResult<Map<String, Boolean>> {
        return try {
            val defaultScrapers = defaultConfig.getDefaultScrapers()
            val installedStates = mutableMapOf<String, Boolean>()
            
            defaultScrapers.forEach { scraper ->
                when (val result = manifestManager.getManifest(scraper.id)) {
                    is ManifestResult.Success -> {
                        installedStates[scraper.id] = true
                        updateScraperStatus(scraper.id, ScraperInstallStatus.INSTALLED)
                    }
                    is ManifestResult.Error -> {
                        installedStates[scraper.id] = false
                        updateScraperStatus(scraper.id, ScraperInstallStatus.NOT_INSTALLED)
                    }
                }
            }
            
            ManifestResult.Success(installedStates)
        } catch (e: Exception) {
            ManifestResult.Error(
                ManifestStorageException("Failed to check installed scrapers: ${e.message}", e, operation = "checkInstalled")
            )
        }
    }
    
    /**
     * Update all scrapers to latest versions
     */
    suspend fun updateAllScrapers(): ManifestResult<UpdateSummary> {
        return try {
            _installationStatus.value = InstallationStatus.Updating
            
            when (val refreshResult = manifestManager.refreshAllManifests()) {
                is ManifestResult.Success -> {
                    val refreshSummary = refreshResult.data
                    
                    val updateSummary = UpdateSummary(
                        totalScrapers = refreshSummary.totalProcessed,
                        successfulUpdates = refreshSummary.successCount,
                        failedUpdates = refreshSummary.failureCount,
                        updateResults = refreshSummary.results.map { refreshResult ->
                            ScraperUpdateResult(
                                scraperId = refreshResult.id,
                                success = refreshResult.success,
                                newVersion = refreshResult.updatedManifest?.version,
                                error = refreshResult.error
                            )
                        }
                    )
                    
                    _installationStatus.value = InstallationStatus.UpdateCompleted(updateSummary)
                    ManifestResult.Success(updateSummary)
                }
                is ManifestResult.Error -> {
                    _installationStatus.value = InstallationStatus.Failed("Update failed: ${refreshResult.exception.message}")
                    refreshResult.map { UpdateSummary(0, 0, 0, emptyList()) }
                }
            }
        } catch (e: Exception) {
            _installationStatus.value = InstallationStatus.Failed("Update failed: ${e.message}")
            ManifestResult.Error(
                ManifestStorageException("Failed to update scrapers: ${e.message}", e, operation = "updateAll")
            )
        }
    }
    
    /**
     * Get installation recommendations
     */
    suspend fun getInstallationRecommendations(): ManifestResult<List<ScraperRecommendation>> {
        return try {
            val checkResult = checkInstalledScrapers()
            when (checkResult) {
                is ManifestResult.Success -> {
                    val installedStates = checkResult.data
                    val recommendations = mutableListOf<ScraperRecommendation>()
                    
                    defaultConfig.getDefaultScrapers().forEach { scraper ->
                        val isInstalled = installedStates[scraper.id] == true
                        
                        if (!isInstalled) {
                            recommendations.add(
                                ScraperRecommendation(
                                    scraper = scraper,
                                    reason = getRecommendationReason(scraper),
                                    priority = getRecommendationPriority(scraper),
                                    estimatedBenefit = getEstimatedBenefit(scraper)
                                )
                            )
                        }
                    }
                    
                    ManifestResult.Success(recommendations.sortedByDescending { it.priority })
                }
                is ManifestResult.Error -> checkResult.map { emptyList() }
            }
        } catch (e: Exception) {
            ManifestResult.Error(
                ManifestStorageException("Failed to get recommendations: ${e.message}", e, operation = "getRecommendations")
            )
        }
    }
    
    /**
     * Initialize installer (run on app startup)
     */
    suspend fun initialize(): ManifestResult<Unit> {
        return try {
            // Check current installation status
            checkInstalledScrapers()
            
            // Auto-install defaults if configured
            val config = defaultConfig.getDefaultManagerConfig()
            if (config.autoLoadDefaults) {
                val installedResult = checkInstalledScrapers()
                when (installedResult) {
                    is ManifestResult.Success -> {
                        val hasAnyInstalled = installedResult.data.values.any { it }
                        if (!hasAnyInstalled) {
                            // No scrapers installed, install defaults
                            installDefaultScrapers()
                        }
                    }
                    is ManifestResult.Error -> { /* Continue without auto-install */ }
                }
            }
            
            ManifestResult.Success(Unit)
        } catch (e: Exception) {
            ManifestResult.Error(
                ManifestStorageException("Failed to initialize installer: ${e.message}", e, operation = "initialize")
            )
        }
    }
    
    // Private helper methods
    
    private fun updateScraperStatus(scraperId: String, status: ScraperInstallStatus) {
        val currentStatuses = _scraperStatuses.value.toMutableMap()
        currentStatuses[scraperId] = status
        _scraperStatuses.value = currentStatuses
    }
    
    private fun getRecommendationReason(scraper: ScraperManifest): String {
        return when (scraper.id) {
            "torrentio" -> "Essential for torrent streaming with wide tracker support"
            "cinemeta" -> "Official metadata provider with comprehensive movie/series info"
            "opensubtitles" -> "Multi-language subtitle support for better accessibility"
            "knightcrawler" -> "Advanced torrent indexer with premium quality sources"
            "kitsu" -> "Specialized anime metadata and catalog provider"
            else -> "Provides additional content and functionality"
        }
    }
    
    private fun getRecommendationPriority(scraper: ScraperManifest): Int {
        return when (scraper.id) {
            "torrentio" -> 100
            "cinemeta" -> 90
            "opensubtitles" -> 80
            "knightcrawler" -> 70
            "kitsu" -> 60
            else -> 50
        }
    }
    
    private fun getEstimatedBenefit(scraper: ScraperManifest): String {
        return when {
            scraper.metadata.capabilities.contains(com.rdwatch.androidtv.scraper.models.ManifestCapability.STREAM) -> "High - Provides streaming sources"
            scraper.metadata.capabilities.contains(com.rdwatch.androidtv.scraper.models.ManifestCapability.META) -> "Medium - Enhances content information"
            scraper.metadata.capabilities.contains(com.rdwatch.androidtv.scraper.models.ManifestCapability.SUBTITLES) -> "Medium - Improves accessibility"
            else -> "Low - Additional features"
        }
    }
}

// Data classes for installation results

sealed class InstallationStatus {
    object Idle : InstallationStatus()
    object Installing : InstallationStatus()
    object Updating : InstallationStatus()
    data class Completed(val summary: InstallationSummary) : InstallationStatus()
    data class UpdateCompleted(val summary: UpdateSummary) : InstallationStatus()
    data class Failed(val error: String) : InstallationStatus()
}

data class InstallationSummary(
    val totalScrapers: Int,
    val successfulInstalls: Int,
    val failedInstalls: Int,
    val results: List<ScraperInstallationResult>
)

data class ScraperInstallationResult(
    val scraperId: String,
    val scraperName: String,
    val success: Boolean,
    val manifest: ScraperManifest?,
    val error: String?
)

data class UpdateSummary(
    val totalScrapers: Int,
    val successfulUpdates: Int,
    val failedUpdates: Int,
    val updateResults: List<ScraperUpdateResult>
)

data class ScraperUpdateResult(
    val scraperId: String,
    val success: Boolean,
    val newVersion: String?,
    val error: String?
)

data class ScraperRecommendation(
    val scraper: ScraperManifest,
    val reason: String,
    val priority: Int,
    val estimatedBenefit: String
)