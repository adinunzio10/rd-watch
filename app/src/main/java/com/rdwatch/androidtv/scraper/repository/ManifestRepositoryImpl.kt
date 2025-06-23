package com.rdwatch.androidtv.scraper.repository

import com.rdwatch.androidtv.data.dao.ScraperManifestDao
import com.rdwatch.androidtv.scraper.models.ManifestCapability
import com.rdwatch.androidtv.scraper.models.ManifestException
import com.rdwatch.androidtv.scraper.models.ManifestNetworkException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ManifestStorageException
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.ValidationStatus
import com.rdwatch.androidtv.scraper.parser.ManifestConverter
import com.rdwatch.androidtv.scraper.parser.ManifestParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestRepositoryImpl @Inject constructor(
    private val dao: ScraperManifestDao,
    private val parser: ManifestParser,
    private val converter: ManifestConverter,
    private val httpClient: OkHttpClient
) : ManifestRepository {
    
    override suspend fun getManifest(id: String): ManifestResult<ScraperManifest> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = dao.getScraperByName(id)
                if (entity != null) {
                    val manifest = converter.fromEntity(entity)
                    if (manifest != null) {
                        ManifestResult.Success(manifest)
                    } else {
                        ManifestResult.Error(
                            ManifestStorageException(
                                "Failed to convert entity to manifest",
                                operation = "getManifest"
                            )
                        )
                    }
                } else {
                    ManifestResult.Error(
                        ManifestStorageException(
                            "Manifest not found with id: $id",
                            operation = "getManifest"
                        )
                    )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to get manifest: ${e.message}",
                        cause = e,
                        operation = "getManifest"
                    )
                )
            }
        }
    }
    
    override suspend fun getManifests(): ManifestResult<List<ScraperManifest>> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = dao.getAllScrapers()
                val manifests = entities.first().mapNotNull { entity ->
                    converter.fromEntity(entity)
                }
                ManifestResult.Success(manifests)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to get manifests: ${e.message}",
                        cause = e,
                        operation = "getManifests"
                    )
                )
            }
        }
    }
    
    override suspend fun getEnabledManifests(): ManifestResult<List<ScraperManifest>> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = dao.getEnabledScrapers()
                val manifests = entities.first().mapNotNull { entity ->
                    converter.fromEntity(entity)
                }
                ManifestResult.Success(manifests)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to get enabled manifests: ${e.message}",
                        cause = e,
                        operation = "getEnabledManifests"
                    )
                )
            }
        }
    }
    
    override suspend fun saveManifest(manifest: ScraperManifest): ManifestResult<ScraperManifest> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = converter.toEntity(manifest)
                val insertedId = dao.insertScraper(entity)
                
                val savedEntity = dao.getScraperById(insertedId)
                if (savedEntity != null) {
                    val savedManifest = converter.fromEntity(savedEntity)
                    if (savedManifest != null) {
                        ManifestResult.Success(savedManifest)
                    } else {
                        ManifestResult.Error(
                            ManifestStorageException(
                                "Failed to convert saved entity to manifest",
                                operation = "saveManifest"
                            )
                        )
                    }
                } else {
                    ManifestResult.Error(
                        ManifestStorageException(
                            "Failed to retrieve saved manifest",
                            operation = "saveManifest"
                        )
                    )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to save manifest: ${e.message}",
                        cause = e,
                        operation = "saveManifest"
                    )
                )
            }
        }
    }
    
    override suspend fun updateManifest(manifest: ScraperManifest): ManifestResult<ScraperManifest> {
        return withContext(Dispatchers.IO) {
            try {
                val existingEntity = dao.getScraperByName(manifest.id)
                if (existingEntity != null) {
                    val updatedEntity = converter.toEntity(manifest).copy(
                        manifestId = existingEntity.manifestId
                    )
                    dao.updateScraper(updatedEntity)
                    
                    val savedEntity = dao.getScraperById(existingEntity.manifestId)
                    if (savedEntity != null) {
                        val updatedManifest = converter.fromEntity(savedEntity)
                        if (updatedManifest != null) {
                            ManifestResult.Success(updatedManifest)
                        } else {
                            ManifestResult.Error(
                                ManifestStorageException(
                                    "Failed to convert updated entity to manifest",
                                    operation = "updateManifest"
                                )
                            )
                        }
                    } else {
                        ManifestResult.Error(
                            ManifestStorageException(
                                "Failed to retrieve updated manifest",
                                operation = "updateManifest"
                            )
                        )
                    }
                } else {
                    ManifestResult.Error(
                        ManifestStorageException(
                            "Manifest not found for update: ${manifest.id}",
                            operation = "updateManifest"
                        )
                    )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to update manifest: ${e.message}",
                        cause = e,
                        operation = "updateManifest"
                    )
                )
            }
        }
    }
    
    override suspend fun deleteManifest(id: String): ManifestResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = dao.getScraperByName(id)
                if (entity != null) {
                    dao.deleteScraperById(entity.manifestId)
                    ManifestResult.Success(Unit)
                } else {
                    ManifestResult.Error(
                        ManifestStorageException(
                            "Manifest not found for deletion: $id",
                            operation = "deleteManifest"
                        )
                    )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to delete manifest: ${e.message}",
                        cause = e,
                        operation = "deleteManifest"
                    )
                )
            }
        }
    }
    
    override suspend fun enableManifest(id: String, enabled: Boolean): ManifestResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = dao.getScraperByName(id)
                if (entity != null) {
                    dao.updateScraperEnabledStatus(entity.manifestId, enabled)
                    ManifestResult.Success(Unit)
                } else {
                    ManifestResult.Error(
                        ManifestStorageException(
                            "Manifest not found for enable operation: $id",
                            operation = "enableManifest"
                        )
                    )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to enable/disable manifest: ${e.message}",
                        cause = e,
                        operation = "enableManifest"
                    )
                )
            }
        }
    }
    
    override suspend fun updateManifestPriority(id: String, priority: Int): ManifestResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = dao.getScraperByName(id)
                if (entity != null) {
                    dao.updateScraperPriority(entity.manifestId, priority)
                    ManifestResult.Success(Unit)
                } else {
                    ManifestResult.Error(
                        ManifestStorageException(
                            "Manifest not found for priority update: $id",
                            operation = "updateManifestPriority"
                        )
                    )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to update manifest priority: ${e.message}",
                        cause = e,
                        operation = "updateManifestPriority"
                    )
                )
            }
        }
    }
    
    override suspend fun fetchManifestFromUrl(url: String): ManifestResult<ScraperManifest> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext ManifestResult.Error(
                        ManifestNetworkException(
                            "HTTP ${response.code}: ${response.message}",
                            url = url,
                            statusCode = response.code
                        )
                    )
                }
                
                val content = response.body?.string()
                if (content.isNullOrBlank()) {
                    return@withContext ManifestResult.Error(
                        ManifestNetworkException(
                            "Empty response from server",
                            url = url,
                            statusCode = response.code
                        )
                    )
                }
                
                when (val parseResult = parser.parseManifest(content, url)) {
                    is ManifestResult.Success -> {
                        val manifest = parser.convertToScraperManifest(parseResult.data, url)
                        
                        // Update metadata with fetch information
                        val updatedManifest = manifest.copy(
                            metadata = manifest.metadata.copy(
                                etag = response.header("ETag"),
                                lastModified = response.header("Last-Modified"),
                                lastFetchedAt = Date(),
                                fetchAttempts = 0,
                                lastError = null,
                                validationStatus = ValidationStatus.VALID
                            )
                        )
                        
                        ManifestResult.Success(updatedManifest)
                    }
                    is ManifestResult.Error -> parseResult
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestNetworkException(
                        "Failed to fetch manifest: ${e.message}",
                        cause = e,
                        url = url
                    )
                )
            }
        }
    }
    
    override suspend fun refreshManifest(id: String): ManifestResult<ScraperManifest> {
        return withContext(Dispatchers.IO) {
            when (val manifestResult = getManifest(id)) {
                is ManifestResult.Success -> {
                    val manifest = manifestResult.data
                    when (val fetchResult = fetchManifestFromUrl(manifest.sourceUrl)) {
                        is ManifestResult.Success -> {
                            val refreshedManifest = fetchResult.data.copy(
                                id = manifest.id,
                                isEnabled = manifest.isEnabled,
                                priorityOrder = manifest.priorityOrder
                            )
                            updateManifest(refreshedManifest)
                        }
                        is ManifestResult.Error -> {
                            // Update error information but keep existing manifest
                            val errorManifest = manifest.copy(
                                metadata = manifest.metadata.copy(
                                    lastFetchedAt = Date(),
                                    fetchAttempts = manifest.metadata.fetchAttempts + 1,
                                    lastError = fetchResult.exception.message,
                                    validationStatus = ValidationStatus.ERROR
                                )
                            )
                            updateManifest(errorManifest)
                            fetchResult
                        }
                    }
                }
                is ManifestResult.Error -> manifestResult
            }
        }
    }
    
    override suspend fun refreshAllManifests(): ManifestResult<List<ScraperManifest>> {
        return withContext(Dispatchers.IO) {
            when (val manifestsResult = getManifests()) {
                is ManifestResult.Success -> {
                    val refreshedManifests = mutableListOf<ScraperManifest>()
                    val errors = mutableListOf<ManifestException>()
                    
                    manifestsResult.data.forEach { manifest ->
                        when (val refreshResult = refreshManifest(manifest.id)) {
                            is ManifestResult.Success -> refreshedManifests.add(refreshResult.data)
                            is ManifestResult.Error -> errors.add(refreshResult.exception)
                        }
                    }
                    
                    if (errors.isEmpty()) {
                        ManifestResult.Success(refreshedManifests)
                    } else {
                        ManifestResult.Error(
                            ManifestStorageException(
                                "Some manifests failed to refresh: ${errors.size} errors",
                                operation = "refreshAllManifests"
                            )
                        )
                    }
                }
                is ManifestResult.Error -> manifestsResult
            }
        }
    }
    
    override suspend fun searchManifests(query: String): ManifestResult<List<ScraperManifest>> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = dao.searchScrapers(query)
                val manifests = entities.first().mapNotNull { entity ->
                    converter.fromEntity(entity)
                }
                ManifestResult.Success(manifests)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to search manifests: ${e.message}",
                        cause = e,
                        operation = "searchManifests"
                    )
                )
            }
        }
    }
    
    override suspend fun getManifestsByAuthor(author: String): ManifestResult<List<ScraperManifest>> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = dao.getScrapersByAuthor(author)
                val manifests = entities.first().mapNotNull { entity ->
                    converter.fromEntity(entity)
                }
                ManifestResult.Success(manifests)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to get manifests by author: ${e.message}",
                        cause = e,
                        operation = "getManifestsByAuthor"
                    )
                )
            }
        }
    }
    
    override suspend fun getManifestsByCapability(capability: ManifestCapability): ManifestResult<List<ScraperManifest>> {
        return withContext(Dispatchers.IO) {
            when (val manifestsResult = getManifests()) {
                is ManifestResult.Success -> {
                    val filteredManifests = manifestsResult.data.filter { manifest ->
                        manifest.metadata.capabilities.contains(capability)
                    }
                    ManifestResult.Success(filteredManifests)
                }
                is ManifestResult.Error -> manifestsResult
            }
        }
    }
    
    override suspend fun validateManifest(id: String): ManifestResult<Boolean> {
        return withContext(Dispatchers.IO) {
            when (val manifestResult = getManifest(id)) {
                is ManifestResult.Success -> {
                    val manifest = manifestResult.data
                    val isValid = manifest.metadata.validationStatus == ValidationStatus.VALID
                    ManifestResult.Success(isValid)
                }
                is ManifestResult.Error -> ManifestResult.Success(false)
            }
        }
    }
    
    override suspend fun validateAllManifests(): ManifestResult<Map<String, Boolean>> {
        return withContext(Dispatchers.IO) {
            when (val manifestsResult = getManifests()) {
                is ManifestResult.Success -> {
                    val validationResults = manifestsResult.data.associate { manifest ->
                        manifest.id to (manifest.metadata.validationStatus == ValidationStatus.VALID)
                    }
                    ManifestResult.Success(validationResults)
                }
                is ManifestResult.Error -> manifestsResult.map { emptyMap() }
            }
        }
    }
    
    override fun observeManifests(): Flow<ManifestResult<List<ScraperManifest>>> = 
        dao.getAllScrapers()
            .map<List<com.rdwatch.androidtv.data.entities.ScraperManifestEntity>, ManifestResult<List<ScraperManifest>>> { entities ->
                val manifests = entities.mapNotNull { entity -> converter.fromEntity(entity) }
                ManifestResult.Success(manifests)
            }
            .catch { e ->
                emit(ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to observe manifests: ${e.message}",
                        cause = e,
                        operation = "observeManifests"
                    )
                ))
            }
            .flowOn(Dispatchers.IO)
    
    override fun observeEnabledManifests(): Flow<ManifestResult<List<ScraperManifest>>> = 
        dao.getEnabledScrapers()
            .map<List<com.rdwatch.androidtv.data.entities.ScraperManifestEntity>, ManifestResult<List<ScraperManifest>>> { entities ->
                val manifests = entities.mapNotNull { entity -> converter.fromEntity(entity) }
                ManifestResult.Success(manifests)
            }
            .catch { e ->
                emit(ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to observe enabled manifests: ${e.message}",
                        cause = e,
                        operation = "observeEnabledManifests"
                    )
                ))
            }
            .flowOn(Dispatchers.IO)
    
    override fun observeManifest(id: String): Flow<ManifestResult<ScraperManifest>> = flow {
        dao.getScraperByNameFlow(id)
            .collect { entity ->
                if (entity != null) {
                    val manifest = converter.fromEntity(entity)
                    if (manifest != null) {
                        emit(ManifestResult.Success(manifest))
                    } else {
                        emit(ManifestResult.Error(
                            ManifestStorageException(
                                "Failed to convert entity to manifest",
                                operation = "observeManifest"
                            )
                        ))
                    }
                } else {
                    emit(ManifestResult.Error(
                        ManifestStorageException(
                            "Manifest not found: $id",
                            operation = "observeManifest"
                        )
                    ))
                }
            }
    }.catch { e ->
        emit(ManifestResult.Error(
            ManifestStorageException(
                "Failed to observe manifest: ${e.message}",
                cause = e,
                operation = "observeManifest"
            )
        ))
    }.flowOn(Dispatchers.IO)
    
    override suspend fun importManifests(urls: List<String>): ManifestResult<List<ScraperManifest>> {
        return withContext(Dispatchers.IO) {
            val importedManifests = mutableListOf<ScraperManifest>()
            val errors = mutableListOf<ManifestException>()
            
            urls.forEach { url ->
                when (val fetchResult = fetchManifestFromUrl(url)) {
                    is ManifestResult.Success -> {
                        when (val saveResult = saveManifest(fetchResult.data)) {
                            is ManifestResult.Success -> importedManifests.add(saveResult.data)
                            is ManifestResult.Error -> errors.add(saveResult.exception)
                        }
                    }
                    is ManifestResult.Error -> errors.add(fetchResult.exception)
                }
            }
            
            if (errors.isEmpty()) {
                ManifestResult.Success(importedManifests)
            } else {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Some manifests failed to import: ${errors.size} errors",
                        operation = "importManifests"
                    )
                )
            }
        }
    }
    
    override suspend fun exportManifests(): ManifestResult<List<ScraperManifest>> {
        return getManifests()
    }
    
    override suspend fun getManifestCount(): ManifestResult<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = dao.getAllScrapers()
                val count = entities.first().size
                ManifestResult.Success(count)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to get manifest count: ${e.message}",
                        cause = e,
                        operation = "getManifestCount"
                    )
                )
            }
        }
    }
    
    override suspend fun getEnabledManifestCount(): ManifestResult<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val count = dao.getEnabledScrapersCount()
                ManifestResult.Success(count)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to get enabled manifest count: ${e.message}",
                        cause = e,
                        operation = "getEnabledManifestCount"
                    )
                )
            }
        }
    }
    
    override suspend fun getManifestStatistics(): ManifestResult<ManifestStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                when (val manifestsResult = getManifests()) {
                    is ManifestResult.Success -> {
                        val manifests = manifestsResult.data
                        val totalCount = manifests.size
                        val enabledCount = manifests.count { it.isEnabled }
                        val disabledCount = totalCount - enabledCount
                        
                        val byAuthor = manifests.groupBy { it.author ?: "Unknown" }
                            .mapValues { it.value.size }
                        
                        val byCapability = mutableMapOf<ManifestCapability, Int>()
                        manifests.forEach { manifest ->
                            manifest.metadata.capabilities.forEach { capability ->
                                byCapability[capability] = (byCapability[capability] ?: 0) + 1
                            }
                        }
                        
                        val statistics = ManifestStatistics(
                            totalManifests = totalCount,
                            enabledManifests = enabledCount,
                            disabledManifests = disabledCount,
                            manifestsByAuthor = byAuthor,
                            manifestsByCapability = byCapability,
                            lastUpdated = Date(),
                            averageResponseTime = 0L, // TODO: Implement response time tracking
                            failureRate = 0.0 // TODO: Implement failure rate tracking
                        )
                        
                        ManifestResult.Success(statistics)
                    }
                    is ManifestResult.Error -> manifestsResult.map { 
                        ManifestStatistics(0, 0, 0, emptyMap(), emptyMap(), Date(), 0L, 0.0)
                    }
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestStorageException(
                        "Failed to get manifest statistics: ${e.message}",
                        cause = e,
                        operation = "getManifestStatistics"
                    )
                )
            }
        }
    }
}