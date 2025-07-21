package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.entities.LibraryEntity
import com.rdwatch.androidtv.repository.base.Result
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface LibraryRepository {
    /**
     * Get all library items for a user
     */
    fun getLibraryByUser(userId: Long): Flow<Result<List<LibraryEntity>>>

    /**
     * Get library items by user and content type
     */
    fun getLibraryByUserAndType(
        userId: Long,
        contentType: String,
    ): Flow<Result<List<LibraryEntity>>>

    /**
     * Get favorite items for a user
     */
    fun getFavoritesByUser(userId: Long): Flow<Result<List<LibraryEntity>>>

    /**
     * Get downloaded content for a user
     */
    fun getDownloadedContentByUser(userId: Long): Flow<Result<List<LibraryEntity>>>

    /**
     * Get specific library item
     */
    suspend fun getLibraryItem(
        userId: Long,
        contentId: String,
    ): Result<LibraryEntity?>

    /**
     * Observe specific library item
     */
    fun getLibraryItemFlow(
        userId: Long,
        contentId: String,
    ): Flow<Result<LibraryEntity?>>

    /**
     * Search library items
     */
    fun searchLibrary(
        userId: Long,
        query: String,
    ): Flow<Result<List<LibraryEntity>>>

    /**
     * Get available content types for user
     */
    suspend fun getContentTypesByUser(userId: Long): Result<List<String>>

    /**
     * Get library statistics
     */
    suspend fun getLibraryCountByUser(userId: Long): Result<Int>

    suspend fun getLibraryCountByUserAndType(
        userId: Long,
        contentType: String,
    ): Result<Int>

    suspend fun getFavoritesCountByUser(userId: Long): Result<Int>

    suspend fun getTotalDownloadedSizeByUser(userId: Long): Result<Long>

    /**
     * Check if content is in library
     */
    suspend fun isInLibrary(
        userId: Long,
        contentId: String,
    ): Result<Boolean>

    /**
     * Add item to library
     */
    suspend fun addToLibrary(libraryItem: LibraryEntity): Result<Unit>

    /**
     * Add multiple items to library
     */
    suspend fun addToLibrary(libraryItems: List<LibraryEntity>): Result<Unit>

    /**
     * Update library item
     */
    suspend fun updateLibraryItem(libraryItem: LibraryEntity): Result<Unit>

    /**
     * Update favorite status
     */
    suspend fun updateFavoriteStatus(
        userId: Long,
        contentId: String,
        isFavorite: Boolean,
    ): Result<Unit>

    /**
     * Update download status
     */
    suspend fun updateDownloadStatus(
        userId: Long,
        contentId: String,
        isDownloaded: Boolean,
        filePath: String? = null,
        fileSizeBytes: Long? = null,
        updatedAt: Date = Date(),
    ): Result<Unit>

    /**
     * Remove item from library
     */
    suspend fun removeFromLibrary(
        userId: Long,
        contentId: String,
    ): Result<Unit>

    /**
     * Remove all library items for user
     */
    suspend fun clearLibraryForUser(userId: Long): Result<Unit>

    /**
     * Cleanup orphaned downloads
     */
    suspend fun cleanupOrphanedDownloads(): Result<Unit>
}
