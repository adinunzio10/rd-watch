package com.rdwatch.androidtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.rdwatch.androidtv.data.converters.Converters
import com.rdwatch.androidtv.ui.filebrowser.models.FileTypeCategory

/**
 * Entity representing user preferences for the file browser
 * 
 * This table stores user-specific settings for file browser behavior,
 * including default sorting, view modes, and filter preferences.
 */
@Entity(
    tableName = "file_browser_preferences",
    indices = [
        Index(value = ["userId"])
    ]
)
@TypeConverters(Converters::class)
data class FileBrowserPreferencesEntity(
    @PrimaryKey
    val userId: String,
    
    val defaultSortOption: String = "DATE_DESC", // FileEnhancedSortOption.name
    val defaultViewMode: String = "GRID", // ViewMode.name
    val showHiddenFiles: Boolean = false,
    val autoRefreshInterval: Int = 300, // seconds
    val defaultFilterTypes: List<String> = emptyList(), // FileTypeCategory.name list
    val favoriteFileIds: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    
    /**
     * Gets the default sort option as enum
     */
    val defaultSortOptionEnum: com.rdwatch.androidtv.ui.filebrowser.models.FileEnhancedSortOption?
        get() = try {
            com.rdwatch.androidtv.ui.filebrowser.models.FileEnhancedSortOption.valueOf(defaultSortOption)
        } catch (e: IllegalArgumentException) {
            com.rdwatch.androidtv.ui.filebrowser.models.FileEnhancedSortOption.DATE_DESC
        }
    
    /**
     * Gets the default filter types as enum set
     */
    val defaultFilterTypesEnum: Set<FileTypeCategory>
        get() = defaultFilterTypes.mapNotNull { typeName ->
            try {
                FileTypeCategory.valueOf(typeName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()
    
    /**
     * Checks if a file ID is marked as favorite
     */
    fun isFileIdFavorite(fileId: String): Boolean {
        return favoriteFileIds.contains(fileId)
    }
    
    /**
     * Adds a file ID to favorites
     */
    fun addFavoriteFileId(fileId: String): FileBrowserPreferencesEntity {
        return if (favoriteFileIds.contains(fileId)) {
            this
        } else {
            copy(
                favoriteFileIds = favoriteFileIds + fileId,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Removes a file ID from favorites
     */
    fun removeFavoriteFileId(fileId: String): FileBrowserPreferencesEntity {
        return copy(
            favoriteFileIds = favoriteFileIds - fileId,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Updates the default sort option
     */
    fun withSortOption(sortOption: com.rdwatch.androidtv.ui.filebrowser.models.FileEnhancedSortOption): FileBrowserPreferencesEntity {
        return copy(
            defaultSortOption = sortOption.name,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Updates the default filter types
     */
    fun withFilterTypes(filterTypes: Set<FileTypeCategory>): FileBrowserPreferencesEntity {
        return copy(
            defaultFilterTypes = filterTypes.map { it.name },
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Updates the view mode
     */
    fun withViewMode(viewMode: String): FileBrowserPreferencesEntity {
        return copy(
            defaultViewMode = viewMode,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Updates the auto-refresh interval
     */
    fun withAutoRefreshInterval(intervalSeconds: Int): FileBrowserPreferencesEntity {
        return copy(
            autoRefreshInterval = intervalSeconds,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Toggles the show hidden files setting
     */
    fun withShowHiddenFiles(showHidden: Boolean): FileBrowserPreferencesEntity {
        return copy(
            showHiddenFiles = showHidden,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    companion object {
        /**
         * Creates default preferences for a user
         */
        fun defaultForUser(userId: String): FileBrowserPreferencesEntity {
            return FileBrowserPreferencesEntity(
                userId = userId,
                defaultSortOption = "DATE_DESC",
                defaultViewMode = "GRID",
                showHiddenFiles = false,
                autoRefreshInterval = 300,
                defaultFilterTypes = emptyList(),
                favoriteFileIds = emptyList()
            )
        }
        
        /**
         * Creates preferences with custom defaults
         */
        fun withDefaults(
            userId: String,
            sortOption: com.rdwatch.androidtv.ui.filebrowser.models.FileEnhancedSortOption = com.rdwatch.androidtv.ui.filebrowser.models.FileEnhancedSortOption.DATE_DESC,
            viewMode: String = "GRID",
            showHiddenFiles: Boolean = false,
            autoRefreshInterval: Int = 300,
            filterTypes: Set<FileTypeCategory> = emptySet()
        ): FileBrowserPreferencesEntity {
            return FileBrowserPreferencesEntity(
                userId = userId,
                defaultSortOption = sortOption.name,
                defaultViewMode = viewMode,
                showHiddenFiles = showHiddenFiles,
                autoRefreshInterval = autoRefreshInterval,
                defaultFilterTypes = filterTypes.map { it.name },
                favoriteFileIds = emptyList()
            )
        }
    }
}