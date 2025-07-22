package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.ShowProgressEntity
import com.rdwatch.androidtv.data.entities.WatchOrderType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ShowProgressDao {
    @Query("SELECT * FROM show_progress WHERE user_id = :userId ORDER BY updated_at DESC")
    fun getShowProgressByUser(userId: Long): Flow<List<ShowProgressEntity>>

    @Query("SELECT * FROM show_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun getShowProgress(
        userId: Long,
        tmdbShowId: Int,
    ): ShowProgressEntity?

    @Query("SELECT * FROM show_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    fun getShowProgressFlow(
        userId: Long,
        tmdbShowId: Int,
    ): Flow<ShowProgressEntity?>

    @Query(
        "SELECT * FROM show_progress WHERE user_id = :userId AND next_season_number IS NOT NULL AND next_episode_number IS NOT NULL ORDER BY updated_at DESC",
    )
    fun getShowsWithNextEpisode(userId: Long): Flow<List<ShowProgressEntity>>

    @Query("SELECT * FROM show_progress WHERE user_id = :userId AND is_show_completed = 0 AND total_episodes_watched > 0 ORDER BY updated_at DESC")
    fun getActiveShows(userId: Long): Flow<List<ShowProgressEntity>>

    @Query("SELECT * FROM show_progress WHERE user_id = :userId AND is_show_completed = 1 ORDER BY updated_at DESC")
    fun getCompletedShows(userId: Long): Flow<List<ShowProgressEntity>>

    @Query(
        "SELECT * FROM show_progress WHERE user_id = :userId AND auto_play_enabled = 1 AND next_season_number IS NOT NULL AND next_episode_number IS NOT NULL",
    )
    suspend fun getShowsEligibleForAutoPlay(userId: Long): List<ShowProgressEntity>

    @Query("SELECT COUNT(*) FROM show_progress WHERE user_id = :userId AND is_show_completed = 1")
    suspend fun getCompletedShowCount(userId: Long): Int

    @Query("SELECT SUM(total_runtime_seconds) FROM show_progress WHERE user_id = :userId")
    suspend fun getTotalWatchTimeAllShows(userId: Long): Long?

    @Query("SELECT AVG(show_completion_percentage) FROM show_progress WHERE user_id = :userId AND total_episodes_watched > 0")
    suspend fun getAverageShowCompletionPercentage(userId: Long): Float?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowProgress(showProgress: ShowProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowProgressBatch(showProgresses: List<ShowProgressEntity>)

    @Update
    suspend fun updateShowProgress(showProgress: ShowProgressEntity)

    @Query(
        """
        UPDATE show_progress 
        SET next_season_number = :nextSeasonNumber,
            next_episode_number = :nextEpisodeNumber,
            next_episode_title = :nextEpisodeTitle,
            next_episode_air_date = :nextEpisodeAirDate,
            last_watched_season = :lastWatchedSeason,
            last_watched_episode = :lastWatchedEpisode,
            total_episodes_watched = :totalEpisodesWatched,
            total_runtime_seconds = :totalRuntimeSeconds,
            show_completion_percentage = :showCompletionPercentage,
            is_show_completed = :isShowCompleted,
            updated_at = :updatedAt
        WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId
    """,
    )
    suspend fun updateShowProgressDetails(
        userId: Long,
        tmdbShowId: Int,
        nextSeasonNumber: Int?,
        nextEpisodeNumber: Int?,
        nextEpisodeTitle: String?,
        nextEpisodeAirDate: Date?,
        lastWatchedSeason: Int?,
        lastWatchedEpisode: Int?,
        totalEpisodesWatched: Int,
        totalRuntimeSeconds: Long,
        showCompletionPercentage: Float,
        isShowCompleted: Boolean,
        updatedAt: Date,
    )

    @Query("UPDATE show_progress SET auto_play_enabled = :enabled WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun updateAutoPlaySetting(
        userId: Long,
        tmdbShowId: Int,
        enabled: Boolean,
    )

    @Query("UPDATE show_progress SET watch_order_preference = :watchOrder WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun updateWatchOrderPreference(
        userId: Long,
        tmdbShowId: Int,
        watchOrder: WatchOrderType,
    )

    @Query("UPDATE show_progress SET skip_special_episodes = :skipSpecials WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun updateSkipSpecialEpisodes(
        userId: Long,
        tmdbShowId: Int,
        skipSpecials: Boolean,
    )

    @Query("UPDATE show_progress SET is_show_completed = 1 WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun markShowAsCompleted(
        userId: Long,
        tmdbShowId: Int,
    )

    @Delete
    suspend fun deleteShowProgress(showProgress: ShowProgressEntity)

    @Query("DELETE FROM show_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun deleteShowProgress(
        userId: Long,
        tmdbShowId: Int,
    )

    @Query("DELETE FROM show_progress WHERE user_id = :userId")
    suspend fun deleteAllShowProgressForUser(userId: Long)

    @Query(
        "SELECT * FROM show_progress WHERE user_id = :userId AND next_episode_air_date <= :currentDate AND next_episode_air_date IS NOT NULL ORDER BY next_episode_air_date",
    )
    suspend fun getShowsWithNewEpisodesAvailable(
        userId: Long,
        currentDate: Date,
    ): List<ShowProgressEntity>

    @Query("UPDATE show_progress SET updated_at = :updatedAt WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun touchShowProgress(
        userId: Long,
        tmdbShowId: Int,
        updatedAt: Date,
    )
}
