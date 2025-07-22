package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.EpisodeProgressEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface EpisodeProgressDao {
    @Query("SELECT * FROM episode_progress WHERE user_id = :userId ORDER BY updated_at DESC")
    fun getEpisodeProgressByUser(userId: Long): Flow<List<EpisodeProgressEntity>>

    @Query("SELECT * FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId ORDER BY season_number, episode_number")
    fun getEpisodeProgressByShow(
        userId: Long,
        tmdbShowId: Int,
    ): Flow<List<EpisodeProgressEntity>>

    @Query("SELECT * FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number = :seasonNumber ORDER BY episode_number")
    fun getEpisodeProgressBySeason(
        userId: Long,
        tmdbShowId: Int,
        seasonNumber: Int,
    ): Flow<List<EpisodeProgressEntity>>

    @Query(
        "SELECT * FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number = :seasonNumber AND episode_number = :episodeNumber",
    )
    suspend fun getEpisodeProgress(
        userId: Long,
        tmdbShowId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): EpisodeProgressEntity?

    @Query(
        "SELECT * FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number = :seasonNumber AND episode_number = :episodeNumber",
    )
    fun getEpisodeProgressFlow(
        userId: Long,
        tmdbShowId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Flow<EpisodeProgressEntity?>

    @Query("SELECT * FROM episode_progress WHERE user_id = :userId AND is_completed = 0 AND watch_percentage > 0.05 ORDER BY updated_at DESC")
    fun getInProgressEpisodes(userId: Long): Flow<List<EpisodeProgressEntity>>

    @Query("SELECT * FROM episode_progress WHERE user_id = :userId AND is_completed = 1 ORDER BY updated_at DESC")
    fun getCompletedEpisodes(userId: Long): Flow<List<EpisodeProgressEntity>>

    @Query(
        "SELECT * FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND is_completed = 1 ORDER BY season_number DESC, episode_number DESC LIMIT 1",
    )
    suspend fun getLastCompletedEpisode(
        userId: Long,
        tmdbShowId: Int,
    ): EpisodeProgressEntity?

    @Query(
        "SELECT * FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND is_completed = 0 AND watch_percentage > 0 ORDER BY season_number, episode_number LIMIT 1",
    )
    suspend fun getFirstInProgressEpisode(
        userId: Long,
        tmdbShowId: Int,
    ): EpisodeProgressEntity?

    @Query(
        "SELECT DISTINCT tmdb_show_id FROM episode_progress WHERE user_id = :userId AND (is_completed = 1 OR watch_percentage > 0.05) ORDER BY updated_at DESC",
    )
    suspend fun getActiveShowIds(userId: Long): List<Int>

    @Query("SELECT COUNT(*) FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND is_completed = 1")
    suspend fun getCompletedEpisodeCount(
        userId: Long,
        tmdbShowId: Int,
    ): Int

    @Query(
        "SELECT COUNT(*) FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number = :seasonNumber AND is_completed = 1",
    )
    suspend fun getCompletedEpisodeCountBySeason(
        userId: Long,
        tmdbShowId: Int,
        seasonNumber: Int,
    ): Int

    @Query("SELECT SUM(progress_seconds) FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun getTotalWatchTimeForShow(
        userId: Long,
        tmdbShowId: Int,
    ): Long?

    @Query("SELECT AVG(watch_percentage) FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun getAverageWatchPercentageForShow(
        userId: Long,
        tmdbShowId: Int,
    ): Float?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodeProgress(episodeProgress: EpisodeProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodeProgressBatch(episodeProgresses: List<EpisodeProgressEntity>)

    @Update
    suspend fun updateEpisodeProgress(episodeProgress: EpisodeProgressEntity)

    @Query(
        """
        UPDATE episode_progress 
        SET progress_seconds = :progressSeconds, 
            watch_percentage = :watchPercentage,
            is_completed = :isCompleted,
            updated_at = :updatedAt,
            device_info = :deviceInfo
        WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number = :seasonNumber AND episode_number = :episodeNumber
    """,
    )
    suspend fun updateProgress(
        userId: Long,
        tmdbShowId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        progressSeconds: Long,
        watchPercentage: Float,
        isCompleted: Boolean,
        updatedAt: Date,
        deviceInfo: String? = null,
    )

    @Query(
        "UPDATE episode_progress SET is_completed = 1 WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number = :seasonNumber AND episode_number = :episodeNumber",
    )
    suspend fun markEpisodeAsCompleted(
        userId: Long,
        tmdbShowId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    )

    @Query("UPDATE episode_progress SET is_completed = 1 WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number = :seasonNumber")
    suspend fun markSeasonAsCompleted(
        userId: Long,
        tmdbShowId: Int,
        seasonNumber: Int,
    )

    @Delete
    suspend fun deleteEpisodeProgress(episodeProgress: EpisodeProgressEntity)

    @Query(
        "DELETE FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number = :seasonNumber AND episode_number = :episodeNumber",
    )
    suspend fun deleteEpisodeProgress(
        userId: Long,
        tmdbShowId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    )

    @Query("DELETE FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId")
    suspend fun deleteAllEpisodeProgressForShow(
        userId: Long,
        tmdbShowId: Int,
    )

    @Query("DELETE FROM episode_progress WHERE user_id = :userId")
    suspend fun deleteAllEpisodeProgressForUser(userId: Long)

    @Query("DELETE FROM episode_progress WHERE watch_percentage < 0.05 AND is_completed = 0")
    suspend fun cleanupMinimalProgress()

    @Query(
        "SELECT * FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number > 0 ORDER BY season_number DESC, episode_number DESC LIMIT 1",
    )
    suspend fun getLatestWatchedEpisode(
        userId: Long,
        tmdbShowId: Int,
    ): EpisodeProgressEntity?

    @Query("SELECT MAX(season_number) FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND is_completed = 1")
    suspend fun getHighestCompletedSeason(
        userId: Long,
        tmdbShowId: Int,
    ): Int?

    @Query(
        "SELECT MAX(episode_number) FROM episode_progress WHERE user_id = :userId AND tmdb_show_id = :tmdbShowId AND season_number = :seasonNumber AND is_completed = 1",
    )
    suspend fun getHighestCompletedEpisodeInSeason(
        userId: Long,
        tmdbShowId: Int,
        seasonNumber: Int,
    ): Int?
}
