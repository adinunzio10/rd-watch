package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress WHERE user_id = :userId ORDER BY updated_at DESC")
    fun getWatchProgressByUser(userId: Long): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE user_id = :userId AND content_id = :contentId")
    suspend fun getWatchProgress(
        userId: Long,
        contentId: String,
    ): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE user_id = :userId AND content_id = :contentId")
    fun getWatchProgressFlow(
        userId: Long,
        contentId: String,
    ): Flow<WatchProgressEntity?>

    @Query("SELECT * FROM watch_progress WHERE user_id = :userId AND is_completed = 0 ORDER BY updated_at DESC")
    fun getInProgressWatchHistory(userId: Long): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE user_id = :userId AND is_completed = 1 ORDER BY updated_at DESC")
    fun getCompletedWatchHistory(userId: Long): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE user_id = :userId AND watch_percentage >= :minPercentage ORDER BY updated_at DESC")
    fun getWatchProgressWithMinPercentage(
        userId: Long,
        minPercentage: Float,
    ): Flow<List<WatchProgressEntity>>

    @Query("SELECT DISTINCT content_id FROM watch_progress WHERE user_id = :userId ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getRecentlyWatchedContentIds(
        userId: Long,
        limit: Int = 50,
    ): List<String>

    @Query("SELECT COUNT(*) FROM watch_progress WHERE user_id = :userId AND is_completed = 1")
    suspend fun getCompletedWatchCount(userId: Long): Int

    @Query("SELECT AVG(watch_percentage) FROM watch_progress WHERE user_id = :userId")
    suspend fun getAverageWatchPercentage(userId: Long): Float?

    @Query("SELECT SUM(progress_seconds) FROM watch_progress WHERE user_id = :userId")
    suspend fun getTotalWatchTimeSeconds(userId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchProgress(watchProgress: WatchProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchProgressBatch(watchProgresses: List<WatchProgressEntity>)

    @Update
    suspend fun updateWatchProgress(watchProgress: WatchProgressEntity)

    @Query(
        """
        UPDATE watch_progress 
        SET progress_seconds = :progressSeconds, 
            watch_percentage = :watchPercentage,
            is_completed = :isCompleted,
            updated_at = :updatedAt,
            device_info = :deviceInfo
        WHERE user_id = :userId AND content_id = :contentId
    """,
    )
    suspend fun updateProgress(
        userId: Long,
        contentId: String,
        progressSeconds: Long,
        watchPercentage: Float,
        isCompleted: Boolean,
        updatedAt: java.util.Date,
        deviceInfo: String? = null,
    )

    @Query("UPDATE watch_progress SET is_completed = 1 WHERE user_id = :userId AND content_id = :contentId")
    suspend fun markAsCompleted(
        userId: Long,
        contentId: String,
    )

    @Delete
    suspend fun deleteWatchProgress(watchProgress: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE user_id = :userId AND content_id = :contentId")
    suspend fun deleteWatchProgress(
        userId: Long,
        contentId: String,
    )

    @Query("DELETE FROM watch_progress WHERE user_id = :userId")
    suspend fun deleteAllWatchProgressForUser(userId: Long)

    @Query("DELETE FROM watch_progress WHERE watch_percentage < 0.05")
    suspend fun cleanupMinimalProgress()
}
