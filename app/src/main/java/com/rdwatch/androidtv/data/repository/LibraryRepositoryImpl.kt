package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.LibraryDao
import com.rdwatch.androidtv.data.entities.LibraryEntity
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.repository.base.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl
    @Inject
    constructor(
        private val libraryDao: LibraryDao,
    ) : LibraryRepository {
        override fun getLibraryByUser(userId: Long): Flow<Result<List<LibraryEntity>>> =
            libraryDao.getLibraryByUser(userId)
                .map { Result.Success(it) as Result<List<LibraryEntity>> }
                .catch { emit(Result.Error(it)) }

        override fun getLibraryByUserAndType(
            userId: Long,
            contentType: String,
        ): Flow<Result<List<LibraryEntity>>> =
            libraryDao.getLibraryByUserAndType(userId, contentType)
                .map { Result.Success(it) as Result<List<LibraryEntity>> }
                .catch { emit(Result.Error(it)) }

        override fun getFavoritesByUser(userId: Long): Flow<Result<List<LibraryEntity>>> =
            libraryDao.getFavoritesByUser(userId)
                .map { Result.Success(it) as Result<List<LibraryEntity>> }
                .catch { emit(Result.Error(it)) }

        override fun getDownloadedContentByUser(userId: Long): Flow<Result<List<LibraryEntity>>> =
            libraryDao.getDownloadedContentByUser(userId)
                .map { Result.Success(it) as Result<List<LibraryEntity>> }
                .catch { emit(Result.Error(it)) }

        override suspend fun getLibraryItem(
            userId: Long,
            contentId: String,
        ): Result<LibraryEntity?> =
            safeCall {
                libraryDao.getLibraryItem(userId, contentId)
            }

        override fun getLibraryItemFlow(
            userId: Long,
            contentId: String,
        ): Flow<Result<LibraryEntity?>> =
            libraryDao.getLibraryItemFlow(userId, contentId)
                .map { Result.Success(it) as Result<LibraryEntity?> }
                .catch { emit(Result.Error(it)) }

        override fun searchLibrary(
            userId: Long,
            query: String,
        ): Flow<Result<List<LibraryEntity>>> =
            libraryDao.searchLibrary(userId, query)
                .map { Result.Success(it) as Result<List<LibraryEntity>> }
                .catch { emit(Result.Error(it)) }

        override suspend fun getContentTypesByUser(userId: Long): Result<List<String>> =
            safeCall {
                libraryDao.getContentTypesByUser(userId)
            }

        override suspend fun getLibraryCountByUser(userId: Long): Result<Int> =
            safeCall {
                libraryDao.getLibraryCountByUser(userId)
            }

        override suspend fun getLibraryCountByUserAndType(
            userId: Long,
            contentType: String,
        ): Result<Int> =
            safeCall {
                libraryDao.getLibraryCountByUserAndType(userId, contentType)
            }

        override suspend fun getFavoritesCountByUser(userId: Long): Result<Int> =
            safeCall {
                libraryDao.getFavoritesCountByUser(userId)
            }

        override suspend fun getTotalDownloadedSizeByUser(userId: Long): Result<Long> =
            safeCall {
                libraryDao.getTotalDownloadedSizeByUser(userId) ?: 0L
            }

        override suspend fun isInLibrary(
            userId: Long,
            contentId: String,
        ): Result<Boolean> =
            safeCall {
                libraryDao.isInLibrary(userId, contentId) > 0
            }

        override suspend fun addToLibrary(libraryItem: LibraryEntity): Result<Unit> =
            safeCall {
                libraryDao.insertLibraryItem(libraryItem)
            }

        override suspend fun addToLibrary(libraryItems: List<LibraryEntity>): Result<Unit> =
            safeCall {
                libraryDao.insertLibraryItems(libraryItems)
            }

        override suspend fun updateLibraryItem(libraryItem: LibraryEntity): Result<Unit> =
            safeCall {
                libraryDao.updateLibraryItem(libraryItem)
            }

        override suspend fun updateFavoriteStatus(
            userId: Long,
            contentId: String,
            isFavorite: Boolean,
        ): Result<Unit> =
            safeCall {
                libraryDao.updateFavoriteStatus(userId, contentId, isFavorite)
            }

        override suspend fun updateDownloadStatus(
            userId: Long,
            contentId: String,
            isDownloaded: Boolean,
            filePath: String?,
            fileSizeBytes: Long?,
            updatedAt: Date,
        ): Result<Unit> =
            safeCall {
                libraryDao.updateDownloadStatus(
                    userId = userId,
                    contentId = contentId,
                    isDownloaded = isDownloaded,
                    filePath = filePath,
                    fileSizeBytes = fileSizeBytes,
                    updatedAt = updatedAt,
                )
            }

        override suspend fun removeFromLibrary(
            userId: Long,
            contentId: String,
        ): Result<Unit> =
            safeCall {
                libraryDao.deleteLibraryItem(userId, contentId)
            }

        override suspend fun clearLibraryForUser(userId: Long): Result<Unit> =
            safeCall {
                libraryDao.deleteAllLibraryForUser(userId)
            }

        override suspend fun cleanupOrphanedDownloads(): Result<Unit> =
            safeCall {
                libraryDao.cleanupOrphanedDownloads()
            }
    }
