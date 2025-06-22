package com.rdwatch.androidtv.scraper.repository

import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for manifest operations
 * Combines local storage and remote fetching capabilities
 */
interface ManifestRepository {
    
    // Local storage operations
    suspend fun getManifest(id: String): ManifestResult<ScraperManifest>
    suspend fun getManifests(): ManifestResult<List<ScraperManifest>>
    suspend fun getEnabledManifests(): ManifestResult<List<ScraperManifest>>
    suspend fun saveManifest(manifest: ScraperManifest): ManifestResult<ScraperManifest>
    suspend fun updateManifest(manifest: ScraperManifest): ManifestResult<ScraperManifest>
    suspend fun deleteManifest(id: String): ManifestResult<Unit>
    suspend fun enableManifest(id: String, enabled: Boolean): ManifestResult<Unit>
    suspend fun updateManifestPriority(id: String, priority: Int): ManifestResult<Unit>
    
    // Remote operations
    suspend fun fetchManifestFromUrl(url: String): ManifestResult<ScraperManifest>
    suspend fun refreshManifest(id: String): ManifestResult<ScraperManifest>
    suspend fun refreshAllManifests(): ManifestResult<List<ScraperManifest>>
    
    // Search and query operations
    suspend fun searchManifests(query: String): ManifestResult<List<ScraperManifest>>
    suspend fun getManifestsByAuthor(author: String): ManifestResult<List<ScraperManifest>>
    suspend fun getManifestsByCapability(capability: com.rdwatch.androidtv.scraper.models.ManifestCapability): ManifestResult<List<ScraperManifest>>
    
    // Validation operations
    suspend fun validateManifest(id: String): ManifestResult<Boolean>
    suspend fun validateAllManifests(): ManifestResult<Map<String, Boolean>>
    
    // Reactive operations
    fun observeManifests(): Flow<ManifestResult<List<ScraperManifest>>>
    fun observeEnabledManifests(): Flow<ManifestResult<List<ScraperManifest>>>
    fun observeManifest(id: String): Flow<ManifestResult<ScraperManifest>>
    
    // Bulk operations
    suspend fun importManifests(urls: List<String>): ManifestResult<List<ScraperManifest>>
    suspend fun exportManifests(): ManifestResult<List<ScraperManifest>>
    
    // Statistics
    suspend fun getManifestCount(): ManifestResult<Int>
    suspend fun getEnabledManifestCount(): ManifestResult<Int>
    suspend fun getManifestStatistics(): ManifestResult<ManifestStatistics>
}

/**
 * Statistics about manifest repository
 */
data class ManifestStatistics(
    val totalManifests: Int,
    val enabledManifests: Int,
    val disabledManifests: Int,
    val manifestsByAuthor: Map<String, Int>,
    val manifestsByCapability: Map<com.rdwatch.androidtv.scraper.models.ManifestCapability, Int>,
    val lastUpdated: java.util.Date,
    val averageResponseTime: Long,
    val failureRate: Double
)