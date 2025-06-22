package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.ScraperManifestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScraperManifestDao {
    
    @Query("SELECT * FROM scraper_manifests WHERE is_enabled = 1 ORDER BY priority_order ASC, scraper_name ASC")
    fun getEnabledScrapers(): Flow<List<ScraperManifestEntity>>
    
    @Query("SELECT * FROM scraper_manifests ORDER BY scraper_name ASC")
    fun getAllScrapers(): Flow<List<ScraperManifestEntity>>
    
    @Query("SELECT * FROM scraper_manifests WHERE manifest_id = :manifestId")
    suspend fun getScraperById(manifestId: Long): ScraperManifestEntity?
    
    @Query("SELECT * FROM scraper_manifests WHERE scraper_name = :scraperName")
    suspend fun getScraperByName(scraperName: String): ScraperManifestEntity?
    
    @Query("SELECT * FROM scraper_manifests WHERE scraper_name = :scraperName")
    fun getScraperByNameFlow(scraperName: String): Flow<ScraperManifestEntity?>
    
    @Query("SELECT * FROM scraper_manifests WHERE author = :author ORDER BY scraper_name ASC")
    fun getScrapersByAuthor(author: String): Flow<List<ScraperManifestEntity>>
    
    @Query("""
        SELECT * FROM scraper_manifests 
        WHERE scraper_name LIKE '%' || :query || '%' 
        OR display_name LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        ORDER BY scraper_name ASC
    """)
    fun searchScrapers(query: String): Flow<List<ScraperManifestEntity>>
    
    @Query("SELECT DISTINCT author FROM scraper_manifests WHERE author IS NOT NULL ORDER BY author")
    suspend fun getAllAuthors(): List<String>
    
    @Query("SELECT COUNT(*) FROM scraper_manifests WHERE scraper_name = :scraperName")
    suspend fun isScraperNameExists(scraperName: String): Int
    
    @Query("SELECT COUNT(*) FROM scraper_manifests WHERE is_enabled = 1")
    suspend fun getEnabledScrapersCount(): Int
    
    @Query("SELECT MAX(priority_order) FROM scraper_manifests")
    suspend fun getMaxPriorityOrder(): Int?
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScraper(scraper: ScraperManifestEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScrapers(scrapers: List<ScraperManifestEntity>)
    
    @Update
    suspend fun updateScraper(scraper: ScraperManifestEntity)
    
    @Query("UPDATE scraper_manifests SET is_enabled = :isEnabled WHERE manifest_id = :manifestId")
    suspend fun updateScraperEnabledStatus(manifestId: Long, isEnabled: Boolean)
    
    @Query("UPDATE scraper_manifests SET priority_order = :priorityOrder WHERE manifest_id = :manifestId")
    suspend fun updateScraperPriority(manifestId: Long, priorityOrder: Int)
    
    @Query("UPDATE scraper_manifests SET config_json = :configJson, updated_at = :updatedAt WHERE manifest_id = :manifestId")
    suspend fun updateScraperConfig(manifestId: Long, configJson: String, updatedAt: java.util.Date)
    
    @Query("""
        UPDATE scraper_manifests 
        SET rate_limit_ms = :rateLimitMs, timeout_seconds = :timeoutSeconds 
        WHERE manifest_id = :manifestId
    """)
    suspend fun updateScraperLimits(manifestId: Long, rateLimitMs: Long, timeoutSeconds: Int)
    
    @Delete
    suspend fun deleteScraper(scraper: ScraperManifestEntity)
    
    @Query("DELETE FROM scraper_manifests WHERE manifest_id = :manifestId")
    suspend fun deleteScraperById(manifestId: Long)
    
    @Query("DELETE FROM scraper_manifests WHERE is_enabled = 0")
    suspend fun deleteDisabledScrapers()
    
    @Query("UPDATE scraper_manifests SET priority_order = priority_order - 1 WHERE priority_order > :deletedPriority")
    suspend fun adjustPrioritiesAfterDeletion(deletedPriority: Int)
}