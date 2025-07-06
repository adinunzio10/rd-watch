package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.FileBrowserPreferencesEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for file browser user preferences
 * 
 * This DAO manages user-specific settings for the file browser,
 * including sorting preferences, view modes, and filter settings.
 */
@Dao
interface FileBrowserPreferencesDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM file_browser_preferences WHERE userId = :userId")
    suspend fun getPreferencesForUser(userId: String): FileBrowserPreferencesEntity?
    
    @Query("SELECT * FROM file_browser_preferences WHERE userId = :userId")
    fun getPreferencesForUserFlow(userId: String): Flow<FileBrowserPreferencesEntity?>
    
    @Query("SELECT * FROM file_browser_preferences ORDER BY lastUpdated DESC")
    fun getAllPreferences(): Flow<List<FileBrowserPreferencesEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: FileBrowserPreferencesEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferencesList: List<FileBrowserPreferencesEntity>)
    
    @Update
    suspend fun updatePreferences(preferences: FileBrowserPreferencesEntity)
    
    @Upsert
    suspend fun upsertPreferences(preferences: FileBrowserPreferencesEntity)
    
    @Delete
    suspend fun deletePreferences(preferences: FileBrowserPreferencesEntity)
    
    @Query("DELETE FROM file_browser_preferences WHERE userId = :userId")
    suspend fun deletePreferencesForUser(userId: String)
    
    // Specific preference queries
    @Query("SELECT defaultSortOption FROM file_browser_preferences WHERE userId = :userId")
    suspend fun getDefaultSortOption(userId: String): String?
    
    @Query("SELECT defaultViewMode FROM file_browser_preferences WHERE userId = :userId")
    suspend fun getDefaultViewMode(userId: String): String?
    
    @Query("SELECT showHiddenFiles FROM file_browser_preferences WHERE userId = :userId")
    suspend fun getShowHiddenFiles(userId: String): Boolean?
    
    @Query("SELECT autoRefreshInterval FROM file_browser_preferences WHERE userId = :userId")
    suspend fun getAutoRefreshInterval(userId: String): Int?
    
    @Query("SELECT defaultFilterTypes FROM file_browser_preferences WHERE userId = :userId")
    suspend fun getDefaultFilterTypes(userId: String): List<String>?
    
    @Query("SELECT favoriteFileIds FROM file_browser_preferences WHERE userId = :userId")
    suspend fun getFavoriteFileIds(userId: String): List<String>?
    
    // Update specific preferences
    @Query("UPDATE file_browser_preferences SET defaultSortOption = :sortOption, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateDefaultSortOption(userId: String, sortOption: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_browser_preferences SET defaultViewMode = :viewMode, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateDefaultViewMode(userId: String, viewMode: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_browser_preferences SET showHiddenFiles = :showHidden, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateShowHiddenFiles(userId: String, showHidden: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_browser_preferences SET autoRefreshInterval = :intervalSeconds, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateAutoRefreshInterval(userId: String, intervalSeconds: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_browser_preferences SET defaultFilterTypes = :filterTypes, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateDefaultFilterTypes(userId: String, filterTypes: List<String>, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_browser_preferences SET favoriteFileIds = :favoriteIds, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateFavoriteFileIds(userId: String, favoriteIds: List<String>, timestamp: Long = System.currentTimeMillis())
    
    // Favorite file operations
    @Query("""
        UPDATE file_browser_preferences 
        SET favoriteFileIds = favoriteFileIds || :fileId,
            lastUpdated = :timestamp
        WHERE userId = :userId 
        AND NOT EXISTS (
            SELECT 1 FROM json_each(favoriteFileIds) 
            WHERE value = :fileId
        )
    """)
    suspend fun addFavoriteFileId(userId: String, fileId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE file_browser_preferences 
        SET favoriteFileIds = (
            SELECT json_group_array(value) 
            FROM json_each(favoriteFileIds) 
            WHERE value != :fileId
        ),
        lastUpdated = :timestamp
        WHERE userId = :userId
    """)
    suspend fun removeFavoriteFileId(userId: String, fileId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM file_browser_preferences 
            WHERE userId = :userId 
            AND EXISTS (
                SELECT 1 FROM json_each(favoriteFileIds) 
                WHERE value = :fileId
            )
        )
    """)
    suspend fun isFavoriteFileId(userId: String, fileId: String): Boolean
    
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT value FROM file_browser_preferences, json_each(favoriteFileIds) 
            WHERE userId = :userId
        )
    """)
    suspend fun getFavoriteFileCount(userId: String): Int
    
    // Bulk favorite operations
    @Query("UPDATE file_browser_preferences SET favoriteFileIds = :favoriteIds, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun setAllFavoriteFileIds(userId: String, favoriteIds: List<String>, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_browser_preferences SET favoriteFileIds = '[]', lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun clearAllFavorites(userId: String, timestamp: Long = System.currentTimeMillis())
    
    // Cache management
    @Query("SELECT * FROM file_browser_preferences WHERE lastUpdated < :expirationTime")
    suspend fun getStalePreferences(expirationTime: Long): List<FileBrowserPreferencesEntity>
    
    @Query("DELETE FROM file_browser_preferences WHERE lastUpdated < :expirationTime")
    suspend fun deleteStalePreferences(expirationTime: Long)
    
    @Query("UPDATE file_browser_preferences SET lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateTimestamp(userId: String, timestamp: Long = System.currentTimeMillis())
    
    // Utility operations
    @Query("SELECT EXISTS(SELECT 1 FROM file_browser_preferences WHERE userId = :userId)")
    suspend fun hasPreferencesForUser(userId: String): Boolean
    
    @Query("SELECT COUNT(*) FROM file_browser_preferences")
    suspend fun getPreferencesCount(): Int
    
    @Query("SELECT userId FROM file_browser_preferences ORDER BY lastUpdated DESC")
    suspend fun getAllUserIds(): List<String>
    
    @Query("SELECT userId FROM file_browser_preferences WHERE lastUpdated >= :since ORDER BY lastUpdated DESC")
    suspend fun getActiveUserIds(since: Long): List<String>
    
    @Query("DELETE FROM file_browser_preferences")
    suspend fun deleteAllPreferences()
    
    @Query("SELECT lastUpdated FROM file_browser_preferences WHERE userId = :userId")
    suspend fun getLastUpdatedTime(userId: String): Long?
    
    // Default value operations
    @Query("""
        UPDATE file_browser_preferences SET 
        defaultSortOption = COALESCE(defaultSortOption, 'DATE_DESC'),
        defaultViewMode = COALESCE(defaultViewMode, 'GRID'),
        showHiddenFiles = COALESCE(showHiddenFiles, 0),
        autoRefreshInterval = COALESCE(autoRefreshInterval, 300),
        defaultFilterTypes = COALESCE(defaultFilterTypes, '[]'),
        favoriteFileIds = COALESCE(favoriteFileIds, '[]'),
        lastUpdated = :timestamp
        WHERE userId = :userId
    """)
    suspend fun ensureDefaultValues(userId: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Gets preferences for user, creating default ones if they don't exist
     */
    suspend fun getOrCreatePreferencesForUser(userId: String): FileBrowserPreferencesEntity {
        return getPreferencesForUser(userId) ?: run {
            val defaultPrefs = FileBrowserPreferencesEntity.defaultForUser(userId)
            insertPreferences(defaultPrefs)
            defaultPrefs
        }
    }
    
    /**
     * Updates a specific preference, creating default preferences if they don't exist
     */
    suspend fun updateOrCreateSortOption(userId: String, sortOption: String) {
        if (hasPreferencesForUser(userId)) {
            updateDefaultSortOption(userId, sortOption)
        } else {
            val defaultPrefs = FileBrowserPreferencesEntity.defaultForUser(userId)
                .withSortOption(
                    com.rdwatch.androidtv.ui.filebrowser.models.FileEnhancedSortOption.valueOf(sortOption)
                )
            insertPreferences(defaultPrefs)
        }
    }
    
    /**
     * Updates view mode, creating default preferences if they don't exist
     */
    suspend fun updateOrCreateViewMode(userId: String, viewMode: String) {
        if (hasPreferencesForUser(userId)) {
            updateDefaultViewMode(userId, viewMode)
        } else {
            val defaultPrefs = FileBrowserPreferencesEntity.defaultForUser(userId)
                .withViewMode(viewMode)
            insertPreferences(defaultPrefs)
        }
    }
    
    /**
     * Toggles show hidden files setting
     */
    suspend fun toggleShowHiddenFiles(userId: String) {
        val currentValue = getShowHiddenFiles(userId) ?: false
        updateOrCreateShowHiddenFiles(userId, !currentValue)
    }
    
    /**
     * Updates show hidden files, creating default preferences if they don't exist
     */
    suspend fun updateOrCreateShowHiddenFiles(userId: String, showHidden: Boolean) {
        if (hasPreferencesForUser(userId)) {
            updateShowHiddenFiles(userId, showHidden)
        } else {
            val defaultPrefs = FileBrowserPreferencesEntity.defaultForUser(userId)
                .withShowHiddenFiles(showHidden)
            insertPreferences(defaultPrefs)
        }
    }
}