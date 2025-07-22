package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.AutoPlaySettingsDao
import com.rdwatch.androidtv.data.dao.EpisodeProgressDao
import com.rdwatch.androidtv.data.dao.ShowProgressDao
import com.rdwatch.androidtv.data.dao.TMDbTVDao
import com.rdwatch.androidtv.data.entities.AutoPlaySettingsEntity
import com.rdwatch.androidtv.data.entities.EpisodeProgressEntity
import com.rdwatch.androidtv.data.entities.ShowProgressEntity
import com.rdwatch.androidtv.data.entities.WatchOrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing episode progression and next episode logic.
 * Implements smart episode tracking with season/episode boundaries, special episodes,
 * and different watch order preferences.
 */
@Singleton
class NextEpisodeRepository
    @Inject
    constructor(
        private val episodeProgressDao: EpisodeProgressDao,
        private val showProgressDao: ShowProgressDao,
        private val autoPlaySettingsDao: AutoPlaySettingsDao,
        private val tmdbTVDao: TMDbTVDao,
    ) {
        /**
         * Get the next episode to watch for a specific show
         */
        suspend fun getNextEpisode(
            userId: Long,
            tmdbShowId: Int,
        ): NextEpisodeResult? {
            val showProgress = showProgressDao.getShowProgress(userId, tmdbShowId)
            val episodeProgress = episodeProgressDao.getEpisodeProgressByShow(userId, tmdbShowId).first()

            // If we have show progress with next episode, return that
            if (showProgress?.hasNextEpisode() == true) {
                return NextEpisodeResult(
                    tmdbShowId = tmdbShowId,
                    seasonNumber = showProgress.nextSeasonNumber!!,
                    episodeNumber = showProgress.nextEpisodeNumber!!,
                    episodeTitle = showProgress.nextEpisodeTitle,
                    isNewSeason = isNewSeasonStart(showProgress, episodeProgress),
                    canAutoPlay = canAutoPlay(userId, showProgress),
                )
            }

            // Calculate next episode based on watch progress
            return calculateNextEpisode(userId, tmdbShowId, episodeProgress, showProgress)
        }

        /**
         * Calculate next episode to watch based on current progress
         */
        private suspend fun calculateNextEpisode(
            userId: Long,
            tmdbShowId: Int,
            episodeProgress: List<EpisodeProgressEntity>,
            showProgress: ShowProgressEntity?,
        ): NextEpisodeResult? {
            if (episodeProgress.isEmpty()) {
                // No progress yet, start with S01E01 (or first available episode)
                return NextEpisodeResult(
                    tmdbShowId = tmdbShowId,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    episodeTitle = null,
                    isNewSeason = true,
                    canAutoPlay = canAutoPlay(userId, showProgress),
                )
            }

            val watchOrder = showProgress?.watchOrderPreference ?: WatchOrderType.CHRONOLOGICAL
            val skipSpecials = showProgress?.skipSpecialEpisodes ?: false

            // Get the latest completed episode
            val lastCompleted =
                episodeProgress.filter { it.isCompleted }
                    .maxByOrNull { it.seasonNumber * 1000 + it.episodeNumber }

            if (lastCompleted == null) {
                // No completed episodes, find first partially watched or start from beginning
                val firstPartial =
                    episodeProgress.filter { it.isPartiallyWatched() }
                        .minByOrNull { it.seasonNumber * 1000 + it.episodeNumber }

                return if (firstPartial != null) {
                    NextEpisodeResult(
                        tmdbShowId = tmdbShowId,
                        seasonNumber = firstPartial.seasonNumber,
                        episodeNumber = firstPartial.episodeNumber,
                        episodeTitle = firstPartial.episodeTitle,
                        isNewSeason = false,
                        canAutoPlay = canAutoPlay(userId, showProgress),
                    )
                } else {
                    NextEpisodeResult(
                        tmdbShowId = tmdbShowId,
                        seasonNumber = 1,
                        episodeNumber = 1,
                        episodeTitle = null,
                        isNewSeason = true,
                        canAutoPlay = canAutoPlay(userId, showProgress),
                    )
                }
            }

            // Calculate next episode after the last completed one
            return when (watchOrder) {
                WatchOrderType.CHRONOLOGICAL ->
                    calculateNextChronological(
                        lastCompleted,
                        episodeProgress,
                        skipSpecials,
                        userId,
                        showProgress,
                    )
                WatchOrderType.RELEASE_DATE ->
                    calculateNextByReleaseDate(
                        lastCompleted,
                        episodeProgress,
                        skipSpecials,
                        userId,
                        showProgress,
                    )
                WatchOrderType.CUSTOM ->
                    calculateNextCustomOrder(
                        lastCompleted,
                        episodeProgress,
                        skipSpecials,
                        userId,
                        showProgress,
                    )
                WatchOrderType.SKIP_SPECIALS ->
                    calculateNextChronological(
                        lastCompleted,
                        episodeProgress,
                        true,
                        userId,
                        showProgress,
                    )
            }
        }

        /**
         * Calculate next episode in chronological order (default behavior)
         */
        private suspend fun calculateNextChronological(
            lastCompleted: EpisodeProgressEntity,
            episodeProgress: List<EpisodeProgressEntity>,
            skipSpecials: Boolean,
            userId: Long,
            showProgress: ShowProgressEntity?,
        ): NextEpisodeResult? {
            val currentSeason = lastCompleted.seasonNumber
            val currentEpisode = lastCompleted.episodeNumber

            // Check if there's a next episode in the same season
            val nextInSeason =
                episodeProgress.find { progress ->
                    progress.seasonNumber == currentSeason &&
                        progress.episodeNumber == currentEpisode + 1 &&
                        (!skipSpecials || progress.seasonNumber > 0)
                }

            if (nextInSeason != null && !nextInSeason.isCompleted) {
                return NextEpisodeResult(
                    tmdbShowId = lastCompleted.tmdbShowId,
                    seasonNumber = nextInSeason.seasonNumber,
                    episodeNumber = nextInSeason.episodeNumber,
                    episodeTitle = nextInSeason.episodeTitle,
                    isNewSeason = false,
                    canAutoPlay = canAutoPlay(userId, showProgress),
                )
            }

            // Move to next season
            val nextSeason =
                if (skipSpecials && currentSeason == 0) {
                    1 // Skip from specials to season 1
                } else {
                    currentSeason + 1
                }

            // Check if next season has episodes
            val nextSeasonFirstEpisode =
                episodeProgress.find { progress ->
                    progress.seasonNumber == nextSeason &&
                        progress.episodeNumber == 1 &&
                        (!skipSpecials || progress.seasonNumber > 0)
                }

            return if (nextSeasonFirstEpisode != null && !nextSeasonFirstEpisode.isCompleted) {
                NextEpisodeResult(
                    tmdbShowId = lastCompleted.tmdbShowId,
                    seasonNumber = nextSeasonFirstEpisode.seasonNumber,
                    episodeNumber = nextSeasonFirstEpisode.episodeNumber,
                    episodeTitle = nextSeasonFirstEpisode.episodeTitle,
                    isNewSeason = true,
                    canAutoPlay = canAutoPlay(userId, showProgress),
                )
            } else {
                // Try to find next available episode in any future season
                episodeProgress.filter { progress ->
                    (
                        progress.seasonNumber > currentSeason ||
                            (progress.seasonNumber == currentSeason && progress.episodeNumber > currentEpisode)
                    ) &&
                        !progress.isCompleted &&
                        (!skipSpecials || progress.seasonNumber > 0)
                }.minByOrNull { it.seasonNumber * 1000 + it.episodeNumber }?.let { nextAvailable ->
                    NextEpisodeResult(
                        tmdbShowId = lastCompleted.tmdbShowId,
                        seasonNumber = nextAvailable.seasonNumber,
                        episodeNumber = nextAvailable.episodeNumber,
                        episodeTitle = nextAvailable.episodeTitle,
                        isNewSeason = nextAvailable.seasonNumber > currentSeason,
                        canAutoPlay = canAutoPlay(userId, showProgress),
                    )
                }
            }
        }

        /**
         * Calculate next episode by release date order
         */
        private suspend fun calculateNextByReleaseDate(
            lastCompleted: EpisodeProgressEntity,
            episodeProgress: List<EpisodeProgressEntity>,
            skipSpecials: Boolean,
            userId: Long,
            showProgress: ShowProgressEntity?,
        ): NextEpisodeResult? {
            // For now, fall back to chronological order
            // In the future, this could use air date information from TMDb
            return calculateNextChronological(lastCompleted, episodeProgress, skipSpecials, userId, showProgress)
        }

        /**
         * Calculate next episode using custom order (for anthology shows)
         */
        private suspend fun calculateNextCustomOrder(
            lastCompleted: EpisodeProgressEntity,
            episodeProgress: List<EpisodeProgressEntity>,
            skipSpecials: Boolean,
            userId: Long,
            showProgress: ShowProgressEntity?,
        ): NextEpisodeResult? {
            // For now, fall back to chronological order
            // In the future, this could support custom watch orders
            return calculateNextChronological(lastCompleted, episodeProgress, skipSpecials, userId, showProgress)
        }

        /**
         * Check if this is the start of a new season
         */
        private fun isNewSeasonStart(
            showProgress: ShowProgressEntity,
            episodeProgress: List<EpisodeProgressEntity>,
        ): Boolean {
            val lastWatchedSeason = showProgress.lastWatchedSeason ?: return true
            val nextSeason = showProgress.nextSeasonNumber ?: return false
            return nextSeason > lastWatchedSeason
        }

        /**
         * Check if auto-play is enabled for this user and show
         */
        private suspend fun canAutoPlay(
            userId: Long,
            showProgress: ShowProgressEntity?,
        ): Boolean {
            val autoPlaySettings =
                autoPlaySettingsDao.getAutoPlaySettings(userId)
                    ?: createDefaultAutoPlaySettings(userId)
            val globalAutoPlay = autoPlaySettings.isAutoPlayActive()
            val showAutoPlay = showProgress?.autoPlayEnabled ?: true
            return globalAutoPlay && showAutoPlay
        }

        /**
         * Create default auto-play settings for a user
         */
        private suspend fun createDefaultAutoPlaySettings(userId: Long): AutoPlaySettingsEntity {
            val defaultSettings =
                AutoPlaySettingsEntity(
                    userId = userId,
                    enabled = true,
                    countdownSeconds = 10,
                    skipIntroEnabled = false,
                    skipOutroEnabled = false,
                    bingeModeEnabled = false,
                    notificationEnabled = true,
                    autoMarkWatchedThreshold = 0.9f,
                    updatedAt = Date(),
                )
            autoPlaySettingsDao.insertAutoPlaySettings(defaultSettings)
            return defaultSettings
        }

        /**
         * Update episode progress and recalculate next episode
         */
        suspend fun updateEpisodeProgress(
            userId: Long,
            tmdbShowId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
            progressSeconds: Long,
            durationSeconds: Long,
            episodeTitle: String? = null,
            deviceInfo: String? = null,
        ) {
            val watchPercentage =
                if (durationSeconds > 0) {
                    (progressSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

            val autoPlaySettings = autoPlaySettingsDao.getAutoPlaySettings(userId)
            val threshold = autoPlaySettings?.autoMarkWatchedThreshold ?: 0.9f
            val isCompleted = watchPercentage >= threshold

            val now = Date()

            // Update or insert episode progress
            val existing = episodeProgressDao.getEpisodeProgress(userId, tmdbShowId, seasonNumber, episodeNumber)
            if (existing != null) {
                episodeProgressDao.updateProgress(
                    userId = userId,
                    tmdbShowId = tmdbShowId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    progressSeconds = progressSeconds,
                    watchPercentage = watchPercentage,
                    isCompleted = isCompleted,
                    updatedAt = now,
                    deviceInfo = deviceInfo,
                )
            } else {
                val newProgress =
                    EpisodeProgressEntity(
                        userId = userId,
                        tmdbShowId = tmdbShowId,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        episodeTitle = episodeTitle,
                        progressSeconds = progressSeconds,
                        durationSeconds = durationSeconds,
                        watchPercentage = watchPercentage,
                        isCompleted = isCompleted,
                        createdAt = now,
                        updatedAt = now,
                        deviceInfo = deviceInfo,
                    )
                episodeProgressDao.insertEpisodeProgress(newProgress)
            }

            // Recalculate show progress if episode was completed
            if (isCompleted) {
                recalculateShowProgress(userId, tmdbShowId)
            }
        }

        /**
         * Recalculate overall show progress and next episode
         */
        private suspend fun recalculateShowProgress(
            userId: Long,
            tmdbShowId: Int,
        ) {
            val episodeProgress = episodeProgressDao.getEpisodeProgressByShow(userId, tmdbShowId).first()
            val completedEpisodes = episodeProgress.filter { it.isCompleted }

            if (completedEpisodes.isEmpty()) return

            // Get the latest completed episode
            val latestCompleted =
                completedEpisodes.maxByOrNull { it.seasonNumber * 1000 + it.episodeNumber }
                    ?: return

            // Calculate next episode
            val nextResult = calculateNextEpisode(userId, tmdbShowId, episodeProgress, null)

            // Get or create show progress entity
            val existingShowProgress = showProgressDao.getShowProgress(userId, tmdbShowId)
            val now = Date()

            val totalWatchTime = episodeProgress.sumOf { it.progressSeconds }
            val totalEpisodesWatched = completedEpisodes.size

            // Calculate completion percentage (this would ideally use total episodes from TMDb)
            val completionPercentage =
                if (episodeProgress.isNotEmpty()) {
                    (completedEpisodes.size.toFloat() / episodeProgress.size.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

            val showProgressEntity =
                if (existingShowProgress != null) {
                    existingShowProgress.copy(
                        nextSeasonNumber = nextResult?.seasonNumber,
                        nextEpisodeNumber = nextResult?.episodeNumber,
                        nextEpisodeTitle = nextResult?.episodeTitle,
                        lastWatchedSeason = latestCompleted.seasonNumber,
                        lastWatchedEpisode = latestCompleted.episodeNumber,
                        totalEpisodesWatched = totalEpisodesWatched,
                        totalRuntimeSeconds = totalWatchTime,
                        showCompletionPercentage = completionPercentage,
                        isShowCompleted = nextResult == null,
                        updatedAt = now,
                    )
                } else {
                    // Create new show progress entry - need show title from TMDb
                    val showEntity = tmdbTVDao.getTVShowByIdSuspend(tmdbShowId)
                    val showTitle = showEntity?.name ?: "Unknown Show"

                    ShowProgressEntity(
                        userId = userId,
                        tmdbShowId = tmdbShowId,
                        showTitle = showTitle,
                        nextSeasonNumber = nextResult?.seasonNumber,
                        nextEpisodeNumber = nextResult?.episodeNumber,
                        nextEpisodeTitle = nextResult?.episodeTitle,
                        lastWatchedSeason = latestCompleted.seasonNumber,
                        lastWatchedEpisode = latestCompleted.episodeNumber,
                        totalEpisodesWatched = totalEpisodesWatched,
                        totalRuntimeSeconds = totalWatchTime,
                        showCompletionPercentage = completionPercentage,
                        isShowCompleted = nextResult == null,
                        createdAt = now,
                        updatedAt = now,
                    )
                }

            showProgressDao.insertShowProgress(showProgressEntity)
        }

        /**
         * Get all shows that have next episodes available
         */
        fun getShowsWithNextEpisodes(userId: Long): Flow<List<ShowProgressEntity>> {
            return showProgressDao.getShowsWithNextEpisode(userId)
        }

        /**
         * Get all actively watched shows
         */
        fun getActiveShows(userId: Long): Flow<List<ShowProgressEntity>> {
            return showProgressDao.getActiveShows(userId)
        }

        /**
         * Get episode progress for a specific show
         */
        fun getEpisodeProgressForShow(
            userId: Long,
            tmdbShowId: Int,
        ): Flow<List<EpisodeProgressEntity>> {
            return episodeProgressDao.getEpisodeProgressByShow(userId, tmdbShowId)
        }

        /**
         * Get auto-play settings for user
         */
        suspend fun getAutoPlaySettings(userId: Long): AutoPlaySettingsEntity? {
            return autoPlaySettingsDao.getAutoPlaySettings(userId)
        }

        /**
         * Update auto-play settings
         */
        suspend fun updateAutoPlaySettings(settings: AutoPlaySettingsEntity) {
            autoPlaySettingsDao.insertAutoPlaySettings(settings)
        }

        /**
         * Get shows eligible for auto-play (have next episode and auto-play enabled)
         */
        suspend fun getShowsEligibleForAutoPlay(userId: Long): List<ShowProgressEntity> {
            return showProgressDao.getShowsEligibleForAutoPlay(userId)
        }

        /**
         * Mark an episode as completed manually
         */
        suspend fun markEpisodeCompleted(
            userId: Long,
            tmdbShowId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
        ) {
            episodeProgressDao.markEpisodeAsCompleted(userId, tmdbShowId, seasonNumber, episodeNumber)
            recalculateShowProgress(userId, tmdbShowId)
        }

        /**
         * Remove episode from continue watching (reset progress)
         */
        suspend fun removeEpisodeProgress(
            userId: Long,
            tmdbShowId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
        ) {
            episodeProgressDao.deleteEpisodeProgress(userId, tmdbShowId, seasonNumber, episodeNumber)
            recalculateShowProgress(userId, tmdbShowId)
        }

        /**
         * Clean up minimal progress entries
         */
        suspend fun cleanupMinimalProgress() {
            episodeProgressDao.cleanupMinimalProgress()
        }
    }

/**
 * Result of next episode calculation
 */
data class NextEpisodeResult(
    val tmdbShowId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeTitle: String?,
    val isNewSeason: Boolean,
    val canAutoPlay: Boolean,
) {
    /**
     * Get formatted episode identifier (S01E05)
     */
    fun getFormattedEpisodeId(): String {
        return "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
    }

    /**
     * Get display text for UI
     */
    fun getDisplayText(): String {
        return if (episodeTitle != null) {
            "${getFormattedEpisodeId()} • $episodeTitle"
        } else {
            getFormattedEpisodeId()
        }
    }
}
