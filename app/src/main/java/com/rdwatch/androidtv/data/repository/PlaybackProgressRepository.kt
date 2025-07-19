package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.WatchProgressDao
import com.rdwatch.androidtv.data.entities.WatchProgressEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackProgressRepository
    @Inject
    constructor(
        private val watchProgressDao: WatchProgressDao,
    ) {
        suspend fun savePlaybackProgress(
            userId: Long,
            contentId: String,
            progressSeconds: Long,
            durationSeconds: Long,
            deviceInfo: String? = null,
        ) {
            val watchPercentage =
                if (durationSeconds > 0) {
                    (progressSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

            val isCompleted = watchPercentage >= 0.9f
            val now = Date()

            // Check if record exists
            val existing = watchProgressDao.getWatchProgress(userId, contentId)

            if (existing != null) {
                // Update existing record
                watchProgressDao.updateProgress(
                    userId = userId,
                    contentId = contentId,
                    progressSeconds = progressSeconds,
                    watchPercentage = watchPercentage,
                    isCompleted = isCompleted,
                    updatedAt = now,
                    deviceInfo = deviceInfo,
                )
            } else {
                // Insert new record
                val entity =
                    WatchProgressEntity(
                        userId = userId,
                        contentId = contentId,
                        progressSeconds = progressSeconds,
                        durationSeconds = durationSeconds,
                        watchPercentage = watchPercentage,
                        isCompleted = isCompleted,
                        createdAt = now,
                        updatedAt = now,
                        deviceInfo = deviceInfo,
                    )
                watchProgressDao.insertWatchProgress(entity)
            }
        }

        suspend fun getPlaybackProgress(
            userId: Long,
            contentId: String,
        ): WatchProgressEntity? {
            return watchProgressDao.getWatchProgress(userId, contentId)
        }

        fun getPlaybackProgressFlow(
            userId: Long,
            contentId: String,
        ): Flow<WatchProgressEntity?> {
            return watchProgressDao.getWatchProgressFlow(userId, contentId)
        }

        fun getInProgressContent(userId: Long): Flow<List<WatchProgressEntity>> {
            return watchProgressDao.getInProgressWatchHistory(userId)
        }

        fun getCompletedContent(userId: Long): Flow<List<WatchProgressEntity>> {
            return watchProgressDao.getCompletedWatchHistory(userId)
        }

        suspend fun markAsCompleted(
            userId: Long,
            contentId: String,
        ) {
            watchProgressDao.markAsCompleted(userId, contentId)
        }

        suspend fun removeProgress(
            userId: Long,
            contentId: String,
        ) {
            watchProgressDao.deleteWatchProgress(userId, contentId)
        }

        suspend fun getRecentlyWatchedContentIds(
            userId: Long,
            limit: Int = 50,
        ): List<String> {
            return watchProgressDao.getRecentlyWatchedContentIds(userId, limit)
        }

        suspend fun cleanupMinimalProgress() {
            // Remove entries with less than 5% progress
            watchProgressDao.cleanupMinimalProgress()
        }

        fun getUserWatchProgress(userId: Long): Flow<List<WatchProgressEntity>> {
            return watchProgressDao.getWatchProgressByUser(userId)
        }

        suspend fun getTotalWatchTimeSeconds(userId: Long): Long {
            return watchProgressDao.getTotalWatchTimeSeconds(userId) ?: 0L
        }

        suspend fun getCompletedWatchCount(userId: Long): Int {
            return watchProgressDao.getCompletedWatchCount(userId)
        }

        suspend fun getAverageWatchPercentage(userId: Long): Float {
            return watchProgressDao.getAverageWatchPercentage(userId) ?: 0f
        }
    }
