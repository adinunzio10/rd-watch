package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.AutoPlaySettingsEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AutoPlaySettingsDao {
    @Query("SELECT * FROM auto_play_settings WHERE user_id = :userId")
    suspend fun getAutoPlaySettings(userId: Long): AutoPlaySettingsEntity?

    @Query("SELECT * FROM auto_play_settings WHERE user_id = :userId")
    fun getAutoPlaySettingsFlow(userId: Long): Flow<AutoPlaySettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutoPlaySettings(settings: AutoPlaySettingsEntity)

    @Update
    suspend fun updateAutoPlaySettings(settings: AutoPlaySettingsEntity)

    @Query("UPDATE auto_play_settings SET enabled = :enabled, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateAutoPlayEnabled(
        userId: Long,
        enabled: Boolean,
        updatedAt: Date,
    )

    @Query("UPDATE auto_play_settings SET countdown_seconds = :countdownSeconds, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateCountdownSeconds(
        userId: Long,
        countdownSeconds: Int,
        updatedAt: Date,
    )

    @Query("UPDATE auto_play_settings SET skip_intro_enabled = :skipIntro, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateSkipIntroEnabled(
        userId: Long,
        skipIntro: Boolean,
        updatedAt: Date,
    )

    @Query("UPDATE auto_play_settings SET skip_outro_enabled = :skipOutro, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateSkipOutroEnabled(
        userId: Long,
        skipOutro: Boolean,
        updatedAt: Date,
    )

    @Query("UPDATE auto_play_settings SET binge_mode_enabled = :bingeMode, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateBingeModeEnabled(
        userId: Long,
        bingeMode: Boolean,
        updatedAt: Date,
    )

    @Query("UPDATE auto_play_settings SET notification_enabled = :notifications, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateNotificationEnabled(
        userId: Long,
        notifications: Boolean,
        updatedAt: Date,
    )

    @Query("UPDATE auto_play_settings SET auto_mark_watched_threshold = :threshold, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateAutoMarkWatchedThreshold(
        userId: Long,
        threshold: Float,
        updatedAt: Date,
    )

    @Query("DELETE FROM auto_play_settings WHERE user_id = :userId")
    suspend fun deleteAutoPlaySettings(userId: Long)

    @Query("SELECT COUNT(*) FROM auto_play_settings WHERE enabled = 1")
    suspend fun getEnabledAutoPlayUsersCount(): Int
}
