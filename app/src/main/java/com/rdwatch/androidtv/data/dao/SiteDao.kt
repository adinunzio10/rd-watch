package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    
    @Query("SELECT * FROM sites WHERE is_active = 1")
    fun getAllActiveSites(): Flow<List<SiteEntity>>
    
    @Query("SELECT * FROM sites")
    fun getAllSites(): Flow<List<SiteEntity>>
    
    @Query("SELECT * FROM sites WHERE site_id = :siteId")
    suspend fun getSiteById(siteId: Long): SiteEntity?
    
    @Query("SELECT * FROM sites WHERE site_code = :siteCode")
    suspend fun getSiteBySiteCode(siteCode: String): SiteEntity?
    
    @Query("SELECT * FROM sites WHERE region_id = :regionId")
    fun getSitesByRegion(regionId: String): Flow<List<SiteEntity>>
    
    @Query("SELECT * FROM sites WHERE country_code = :countryCode")
    fun getSitesByCountry(countryCode: String): Flow<List<SiteEntity>>
    
    @Query("""
        SELECT * FROM sites 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLon AND :maxLon
        AND is_active = 1
    """)
    fun getSitesInBounds(
        minLat: Double, 
        maxLat: Double, 
        minLon: Double, 
        maxLon: Double
    ): Flow<List<SiteEntity>>
    
    @Query("SELECT * FROM sites WHERE site_name LIKE '%' || :query || '%' OR site_code LIKE '%' || :query || '%'")
    fun searchSites(query: String): Flow<List<SiteEntity>>
    
    @Query("SELECT COUNT(*) FROM sites WHERE site_code = :siteCode")
    suspend fun isSiteCodeExists(siteCode: String): Int
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSite(site: SiteEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)
    
    @Update
    suspend fun updateSite(site: SiteEntity)
    
    @Query("UPDATE sites SET is_active = :isActive WHERE site_id = :siteId")
    suspend fun updateSiteActiveStatus(siteId: Long, isActive: Boolean)
    
    @Delete
    suspend fun deleteSite(site: SiteEntity)
    
    @Query("DELETE FROM sites WHERE site_id = :siteId")
    suspend fun deleteSiteById(siteId: Long)
    
    @Query("DELETE FROM sites WHERE is_active = 0")
    suspend fun deleteInactiveSites()
}