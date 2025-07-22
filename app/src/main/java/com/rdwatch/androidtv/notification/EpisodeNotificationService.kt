package com.rdwatch.androidtv.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rdwatch.androidtv.MainActivity
import com.rdwatch.androidtv.R
import com.rdwatch.androidtv.data.repository.NextEpisodeResult
import com.rdwatch.androidtv.util.DebugLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing episode completion and next episode notifications
 * Handles Android TV-specific notification display and user interactions
 */
@Singleton
class EpisodeNotificationService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            // Notification channels
            private const val CHANNEL_EPISODE_PROGRESS = "episode_progress"
            private const val CHANNEL_EPISODE_COMPLETION = "episode_completion"
            private const val CHANNEL_SEASON_PROGRESS = "season_progress"
            private const val CHANNEL_SERIES_COMPLETION = "series_completion"

            // Notification IDs
            private const val NOTIFICATION_ID_EPISODE_COMPLETED = 1001
            private const val NOTIFICATION_ID_NEXT_EPISODE_READY = 1002
            private const val NOTIFICATION_ID_SEASON_FINISHED = 1003
            private const val NOTIFICATION_ID_SERIES_COMPLETED = 1004

            // Intent action constants
            const val ACTION_PLAY_NEXT_EPISODE = "com.rdwatch.androidtv.PLAY_NEXT_EPISODE"
            const val ACTION_VIEW_SHOW = "com.rdwatch.androidtv.VIEW_SHOW"
            const val ACTION_DISMISS_NOTIFICATION = "com.rdwatch.androidtv.DISMISS_NOTIFICATION"

            // Intent extras
            const val EXTRA_TMDB_SHOW_ID = "tmdb_show_id"
            const val EXTRA_SEASON_NUMBER = "season_number"
            const val EXTRA_EPISODE_NUMBER = "episode_number"
            const val EXTRA_NOTIFICATION_ID = "notification_id"
        }

        private val notificationManager: NotificationManagerCompat by lazy {
            NotificationManagerCompat.from(context)
        }

        init {
            createNotificationChannels()
        }

        /**
         * Create notification channels for different types of episode events
         */
        private fun createNotificationChannels() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channels =
                    listOf(
                        NotificationChannel(
                            CHANNEL_EPISODE_PROGRESS,
                            "Episode Progress",
                            NotificationManager.IMPORTANCE_LOW,
                        ).apply {
                            description = "Notifications for episode completion and progress updates"
                            setShowBadge(false) // Don't show badge on Android TV launcher
                        },
                        NotificationChannel(
                            CHANNEL_EPISODE_COMPLETION,
                            "Episode Completion",
                            NotificationManager.IMPORTANCE_DEFAULT,
                        ).apply {
                            description = "Notifications when episodes are completed"
                            setShowBadge(true)
                        },
                        NotificationChannel(
                            CHANNEL_SEASON_PROGRESS,
                            "Season Progress",
                            NotificationManager.IMPORTANCE_DEFAULT,
                        ).apply {
                            description = "Notifications for season completion and new season availability"
                            setShowBadge(true)
                        },
                        NotificationChannel(
                            CHANNEL_SERIES_COMPLETION,
                            "Series Completion",
                            NotificationManager.IMPORTANCE_HIGH,
                        ).apply {
                            description = "Notifications when entire series are completed"
                            setShowBadge(true)
                        },
                    )

                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                channels.forEach { channel ->
                    systemNotificationManager.createNotificationChannel(channel)
                }

                DebugLogger.d("EpisodeNotificationService", "Created ${channels.size} notification channels")
            }
        }

        /**
         * Show notification when an episode is completed
         */
        fun notifyEpisodeCompleted(
            showTitle: String,
            seasonNumber: Int,
            episodeNumber: Int,
            episodeTitle: String?,
        ) {
            DebugLogger.d("EpisodeNotificationService", "Notifying episode completed: $showTitle S${seasonNumber}E$episodeNumber")

            val episodeId = "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
            val title = "Episode Completed"
            val content =
                if (episodeTitle != null) {
                    "$showTitle • $episodeId • $episodeTitle"
                } else {
                    "$showTitle • $episodeId"
                }

            val notification =
                createBaseNotificationBuilder(CHANNEL_EPISODE_COMPLETION)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .setAutoCancel(true)
                    .build()

            try {
                notificationManager.notify(NOTIFICATION_ID_EPISODE_COMPLETED, notification)
            } catch (e: SecurityException) {
                DebugLogger.w("EpisodeNotificationService", "No notification permission for episode completed")
            }
        }

        /**
         * Show notification when next episode is ready to watch
         */
        fun notifyNextEpisodeReady(
            showTitle: String,
            tmdbShowId: Int,
            nextEpisode: NextEpisodeResult,
        ) {
            DebugLogger.d("EpisodeNotificationService", "Notifying next episode ready: $showTitle ${nextEpisode.getFormattedEpisodeId()}")

            val title = "Next Episode Ready"
            val content =
                if (nextEpisode.episodeTitle != null) {
                    "$showTitle • ${nextEpisode.getFormattedEpisodeId()} • ${nextEpisode.episodeTitle}"
                } else {
                    "$showTitle • ${nextEpisode.getFormattedEpisodeId()}"
                }

            val playNextAction = createPlayNextAction(tmdbShowId, nextEpisode)
            val viewShowAction = createViewShowAction(tmdbShowId)

            val notification =
                createBaseNotificationBuilder(CHANNEL_EPISODE_COMPLETION)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .addAction(playNextAction)
                    .addAction(viewShowAction)
                    .setAutoCancel(true)
                    .build()

            try {
                notificationManager.notify(NOTIFICATION_ID_NEXT_EPISODE_READY, notification)
            } catch (e: SecurityException) {
                DebugLogger.w("EpisodeNotificationService", "No notification permission for next episode ready")
            }
        }

        /**
         * Show notification when a season is finished
         */
        fun notifySeasonFinished(
            showTitle: String,
            tmdbShowId: Int,
            seasonNumber: Int,
            nextEpisode: NextEpisodeResult?,
        ) {
            DebugLogger.d("EpisodeNotificationService", "Notifying season finished: $showTitle Season $seasonNumber")

            val title = "Season Complete!"
            val content =
                if (nextEpisode != null && nextEpisode.isNewSeason) {
                    "$showTitle • Season $seasonNumber complete • Season ${nextEpisode.seasonNumber} now available"
                } else {
                    "$showTitle • Season $seasonNumber complete"
                }

            val notificationBuilder =
                createBaseNotificationBuilder(CHANNEL_SEASON_PROGRESS)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .addAction(createViewShowAction(tmdbShowId))

            // Add play next action if next season is available
            if (nextEpisode != null && nextEpisode.isNewSeason) {
                notificationBuilder.addAction(createPlayNextAction(tmdbShowId, nextEpisode))
            }

            val notification =
                notificationBuilder
                    .setAutoCancel(true)
                    .build()

            try {
                notificationManager.notify(NOTIFICATION_ID_SEASON_FINISHED, notification)
            } catch (e: SecurityException) {
                DebugLogger.w("EpisodeNotificationService", "No notification permission for season finished")
            }
        }

        /**
         * Show notification when entire series is completed
         */
        fun notifySeriesCompleted(
            showTitle: String,
            tmdbShowId: Int,
            totalSeasonsWatched: Int,
            totalEpisodesWatched: Int,
        ) {
            DebugLogger.d("EpisodeNotificationService", "Notifying series completed: $showTitle")

            val title = "Series Complete! 🎉"
            val content = "$showTitle • $totalSeasonsWatched seasons • $totalEpisodesWatched episodes"

            val notification =
                createBaseNotificationBuilder(CHANNEL_SERIES_COMPLETION)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .addAction(createViewShowAction(tmdbShowId))
                    .setAutoCancel(true)
                    .build()

            try {
                notificationManager.notify(NOTIFICATION_ID_SERIES_COMPLETED, notification)
            } catch (e: SecurityException) {
                DebugLogger.w("EpisodeNotificationService", "No notification permission for series completed")
            }
        }

        /**
         * Create base notification builder with common Android TV settings
         */
        private fun createBaseNotificationBuilder(channelId: String): NotificationCompat.Builder {
            return NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(createMainActivityPendingIntent())
        }

        /**
         * Create "Play Next" action for notifications
         */
        private fun createPlayNextAction(
            tmdbShowId: Int,
            nextEpisode: NextEpisodeResult,
        ): NotificationCompat.Action {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    action = ACTION_PLAY_NEXT_EPISODE
                    putExtra(EXTRA_TMDB_SHOW_ID, tmdbShowId)
                    putExtra(EXTRA_SEASON_NUMBER, nextEpisode.seasonNumber)
                    putExtra(EXTRA_EPISODE_NUMBER, nextEpisode.episodeNumber)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    tmdbShowId * 1000 + nextEpisode.seasonNumber * 100 + nextEpisode.episodeNumber, // Unique request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            return NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Play Next",
                pendingIntent,
            ).build()
        }

        /**
         * Create "View Show" action for notifications
         */
        private fun createViewShowAction(tmdbShowId: Int): NotificationCompat.Action {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    action = ACTION_VIEW_SHOW
                    putExtra(EXTRA_TMDB_SHOW_ID, tmdbShowId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    tmdbShowId, // Unique request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            return NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_view,
                "View Show",
                pendingIntent,
            ).build()
        }

        /**
         * Create pending intent for main activity (default notification tap action)
         */
        private fun createMainActivityPendingIntent(): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Clear all episode-related notifications
         */
        fun clearAllNotifications() {
            try {
                notificationManager.cancel(NOTIFICATION_ID_EPISODE_COMPLETED)
                notificationManager.cancel(NOTIFICATION_ID_NEXT_EPISODE_READY)
                notificationManager.cancel(NOTIFICATION_ID_SEASON_FINISHED)
                notificationManager.cancel(NOTIFICATION_ID_SERIES_COMPLETED)
                DebugLogger.d("EpisodeNotificationService", "Cleared all episode notifications")
            } catch (e: Exception) {
                DebugLogger.e("EpisodeNotificationService", "Error clearing notifications", e)
            }
        }

        /**
         * Check if notifications are enabled for this app
         */
        fun areNotificationsEnabled(): Boolean {
            return notificationManager.areNotificationsEnabled()
        }

        /**
         * Get notification permission status message for debugging
         */
        fun getNotificationStatus(): String {
            return if (areNotificationsEnabled()) {
                "Notifications enabled"
            } else {
                "Notifications disabled - check app permissions"
            }
        }
    }
