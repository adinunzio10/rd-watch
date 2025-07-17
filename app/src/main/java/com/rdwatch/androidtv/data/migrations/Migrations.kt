package com.rdwatch.androidtv.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create users table (simplified for TV/Real-Debrid auth)
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `user_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `username` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `password_hash` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `display_name` TEXT,
                        `profile_image_url` TEXT
                    )
                    """.trimIndent(),
                )

                // Create indices for users
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")

                // Create watch_progress table for tracking user viewing progress
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `watch_progress` (
                        `progress_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id` INTEGER NOT NULL,
                        `content_id` TEXT NOT NULL,
                        `progress_seconds` INTEGER NOT NULL,
                        `duration_seconds` INTEGER NOT NULL,
                        `watch_percentage` REAL NOT NULL,
                        `is_completed` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `device_info` TEXT,
                        FOREIGN KEY(`user_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                // Create indices for watch_progress
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_progress_user_id` ON `watch_progress` (`user_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_progress_content_id` ON `watch_progress` (`content_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_progress_updated_at` ON `watch_progress` (`updated_at`)")
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_watch_progress_user_id_content_id` ON `watch_progress` (`user_id`, `content_id`)",
                )

                // Create library table for user's saved content
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `library` (
                        `library_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id` INTEGER NOT NULL,
                        `content_id` TEXT NOT NULL,
                        `content_type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `thumbnail_url` TEXT,
                        `is_favorite` INTEGER NOT NULL DEFAULT 0,
                        `is_downloaded` INTEGER NOT NULL DEFAULT 0,
                        `file_path` TEXT,
                        `file_size_bytes` INTEGER,
                        `added_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`user_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                // Create indices for library
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_library_user_id` ON `library` (`user_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_library_content_type` ON `library` (`content_type`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_library_added_at` ON `library` (`added_at`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_library_is_favorite` ON `library` (`is_favorite`)")
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_library_user_id_content_id` ON `library` (`user_id`, `content_id`)",
                )

                // Create scraper_manifests table for content scraper configurations
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scraper_manifests` (
                        `manifest_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `scraper_name` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `base_url` TEXT NOT NULL,
                        `config_json` TEXT NOT NULL,
                        `is_enabled` INTEGER NOT NULL DEFAULT 1,
                        `priority_order` INTEGER NOT NULL DEFAULT 0,
                        `rate_limit_ms` INTEGER NOT NULL DEFAULT 1000,
                        `timeout_seconds` INTEGER NOT NULL DEFAULT 30,
                        `description` TEXT,
                        `author` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for scraper_manifests
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_scraper_manifests_scraper_name` ON `scraper_manifests` (`scraper_name`)",
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_scraper_manifests_is_enabled` ON `scraper_manifests` (`is_enabled`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_scraper_manifests_updated_at` ON `scraper_manifests` (`updated_at`)")

                // Create search_history table for user search queries
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `search_history` (
                        `search_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id` INTEGER NOT NULL,
                        `search_query` TEXT NOT NULL,
                        `search_type` TEXT NOT NULL DEFAULT 'general',
                        `results_count` INTEGER NOT NULL DEFAULT 0,
                        `search_date` INTEGER NOT NULL,
                        `filters_json` TEXT,
                        `response_time_ms` INTEGER,
                        FOREIGN KEY(`user_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                // Create indices for search_history
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_user_id` ON `search_history` (`user_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_search_query` ON `search_history` (`search_query`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_search_date` ON `search_history` (`search_date`)")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_search_history_user_id_search_query` ON `search_history` (`user_id`, `search_query`)",
                )
            }
        }

    val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create content table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `content` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `year` INTEGER,
                        `quality` TEXT,
                        `source` TEXT NOT NULL,
                        `realDebridId` TEXT,
                        `posterUrl` TEXT,
                        `backdropUrl` TEXT,
                        `description` TEXT,
                        `duration` INTEGER,
                        `rating` REAL,
                        `genres` TEXT,
                        `cast` TEXT,
                        `director` TEXT,
                        `imdbId` TEXT,
                        `tmdbId` INTEGER,
                        `addedDate` INTEGER NOT NULL,
                        `lastPlayedDate` INTEGER,
                        `playCount` INTEGER NOT NULL DEFAULT 0,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `isWatched` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )

                // Create indices for content
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_source` ON `content` (`source`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_title` ON `content` (`title`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_addedDate` ON `content` (`addedDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_isFavorite` ON `content` (`isFavorite`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_isWatched` ON `content` (`isWatched`)")

                // Create torrents table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `torrents` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `hash` TEXT NOT NULL,
                        `filename` TEXT NOT NULL,
                        `bytes` INTEGER NOT NULL,
                        `links` TEXT NOT NULL,
                        `split` INTEGER NOT NULL,
                        `progress` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `added` INTEGER NOT NULL,
                        `speed` INTEGER,
                        `seeders` INTEGER,
                        `created` INTEGER,
                        `ended` INTEGER
                    )
                    """.trimIndent(),
                )

                // Create indices for torrents
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_torrents_hash` ON `torrents` (`hash`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_torrents_status` ON `torrents` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_torrents_added` ON `torrents` (`added`)")

                // Create downloads table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `downloads` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `filename` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `filesize` INTEGER NOT NULL,
                        `link` TEXT NOT NULL,
                        `host` TEXT NOT NULL,
                        `chunks` INTEGER NOT NULL,
                        `download` TEXT NOT NULL,
                        `streamable` INTEGER NOT NULL,
                        `generated` INTEGER NOT NULL,
                        `type` TEXT,
                        `alternative` TEXT,
                        `contentId` INTEGER,
                        FOREIGN KEY(`contentId`) REFERENCES `content`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for downloads
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_filename` ON `downloads` (`filename`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_mimeType` ON `downloads` (`mimeType`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_streamable` ON `downloads` (`streamable`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_generated` ON `downloads` (`generated`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_contentId` ON `downloads` (`contentId`)")
            }
        }

    val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create file_hashes table for caching OpenSubtitles hash calculations
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `file_hashes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `file_path` TEXT NOT NULL,
                        `hash_value` TEXT NOT NULL,
                        `file_size` INTEGER NOT NULL,
                        `last_modified` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for file_hashes
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_file_hashes_file_path` ON `file_hashes` (`file_path`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_file_hashes_hash_value` ON `file_hashes` (`hash_value`)")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_file_hashes_last_modified_file_size` ON `file_hashes` (`last_modified`, `file_size`)",
                )
            }
        }

    val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create subtitle-related tables
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subtitle_cache` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `content_hash` TEXT NOT NULL,
                        `language_code` TEXT NOT NULL,
                        `provider_name` TEXT NOT NULL,
                        `subtitle_data` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `expires_at` INTEGER NOT NULL,
                        `file_size` INTEGER NOT NULL,
                        `encoding` TEXT NOT NULL DEFAULT 'UTF-8'
                    )
                    """.trimIndent(),
                )

                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_subtitle_cache_content_hash_language_provider` ON `subtitle_cache` (`content_hash`, `language_code`, `provider_name`)",
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_subtitle_cache_expires_at` ON `subtitle_cache` (`expires_at`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subtitle_results` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `content_hash` TEXT NOT NULL,
                        `query_params` TEXT NOT NULL,
                        `results_json` TEXT NOT NULL,
                        `provider_name` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `expires_at` INTEGER NOT NULL,
                        `result_count` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )

                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_subtitle_results_content_hash_provider` ON `subtitle_results` (`content_hash`, `provider_name`)",
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_subtitle_results_expires_at` ON `subtitle_results` (`expires_at`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subtitle_files` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subtitle_id` TEXT NOT NULL,
                        `file_path` TEXT NOT NULL,
                        `language_code` TEXT NOT NULL,
                        `provider_name` TEXT NOT NULL,
                        `content_hash` TEXT NOT NULL,
                        `downloaded_at` INTEGER NOT NULL,
                        `file_size` INTEGER NOT NULL,
                        `encoding` TEXT NOT NULL DEFAULT 'UTF-8',
                        `is_valid` INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent(),
                )

                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_subtitle_files_file_path` ON `subtitle_files` (`file_path`)")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_subtitle_files_content_hash_language` ON `subtitle_files` (`content_hash`, `language_code`)",
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subtitle_provider_stats` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `provider_name` TEXT NOT NULL,
                        `total_requests` INTEGER NOT NULL DEFAULT 0,
                        `successful_requests` INTEGER NOT NULL DEFAULT 0,
                        `failed_requests` INTEGER NOT NULL DEFAULT 0,
                        `avg_response_time_ms` INTEGER NOT NULL DEFAULT 0,
                        `last_request_at` INTEGER,
                        `last_success_at` INTEGER,
                        `consecutive_failures` INTEGER NOT NULL DEFAULT 0,
                        `is_enabled` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_subtitle_provider_stats_provider_name` ON `subtitle_provider_stats` (`provider_name`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_subtitle_provider_stats_is_enabled` ON `subtitle_provider_stats` (`is_enabled`)",
                )
            }
        }

    val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create TMDb movies table - schema must match TMDbMovieEntity exactly
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tmdb_movies` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `title` TEXT NOT NULL,
                        `originalTitle` TEXT NOT NULL,
                        `overview` TEXT,
                        `releaseDate` TEXT,
                        `posterPath` TEXT,
                        `backdropPath` TEXT,
                        `voteAverage` REAL NOT NULL,
                        `voteCount` INTEGER NOT NULL,
                        `popularity` REAL NOT NULL,
                        `adult` INTEGER NOT NULL,
                        `video` INTEGER NOT NULL,
                        `originalLanguage` TEXT NOT NULL,
                        `genreIds` TEXT NOT NULL,
                        `runtime` INTEGER,
                        `budget` INTEGER,
                        `revenue` INTEGER,
                        `status` TEXT,
                        `tagline` TEXT,
                        `homepage` TEXT,
                        `imdbId` TEXT,
                        `spokenLanguages` TEXT,
                        `productionCompanies` TEXT,
                        `productionCountries` TEXT,
                        `genres` TEXT,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for tmdb_movies
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_movies_title` ON `tmdb_movies` (`title`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_movies_popularity` ON `tmdb_movies` (`popularity`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_movies_voteAverage` ON `tmdb_movies` (`voteAverage`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_movies_releaseDate` ON `tmdb_movies` (`releaseDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_movies_lastUpdated` ON `tmdb_movies` (`lastUpdated`)")

                // Create TMDb TV shows table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tmdb_tv_shows` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `originalName` TEXT NOT NULL,
                        `overview` TEXT,
                        `firstAirDate` TEXT,
                        `lastAirDate` TEXT,
                        `posterPath` TEXT,
                        `backdropPath` TEXT,
                        `voteAverage` REAL NOT NULL,
                        `voteCount` INTEGER NOT NULL,
                        `popularity` REAL NOT NULL,
                        `adult` INTEGER NOT NULL,
                        `originalLanguage` TEXT NOT NULL,
                        `genreIds` TEXT NOT NULL,
                        `numberOfEpisodes` INTEGER,
                        `numberOfSeasons` INTEGER,
                        `status` TEXT,
                        `type` TEXT,
                        `homepage` TEXT,
                        `inProduction` INTEGER,
                        `languages` TEXT,
                        `lastEpisodeToAir` TEXT,
                        `nextEpisodeToAir` TEXT,
                        `networks` TEXT,
                        `originCountry` TEXT,
                        `productionCompanies` TEXT,
                        `productionCountries` TEXT,
                        `spokenLanguages` TEXT,
                        `seasons` TEXT,
                        `genres` TEXT,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for tmdb_tv_shows
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_tv_shows_name` ON `tmdb_tv_shows` (`name`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_tv_shows_popularity` ON `tmdb_tv_shows` (`popularity`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_tv_shows_voteAverage` ON `tmdb_tv_shows` (`voteAverage`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_tv_shows_firstAirDate` ON `tmdb_tv_shows` (`firstAirDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_tv_shows_lastUpdated` ON `tmdb_tv_shows` (`lastUpdated`)")

                // Create TMDb search results table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tmdb_search_results` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `query` TEXT NOT NULL,
                        `page` INTEGER NOT NULL,
                        `totalPages` INTEGER NOT NULL,
                        `totalResults` INTEGER NOT NULL,
                        `searchType` TEXT NOT NULL,
                        `results` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for tmdb_search_results
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_search_results_query` ON `tmdb_search_results` (`query`)")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tmdb_search_results_searchType` ON `tmdb_search_results` (`searchType`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tmdb_search_results_lastUpdated` ON `tmdb_search_results` (`lastUpdated`)",
                )

                // Create TMDb credits table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tmdb_credits` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `contentId` INTEGER NOT NULL,
                        `contentType` TEXT NOT NULL,
                        `cast` TEXT NOT NULL,
                        `crew` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for tmdb_credits
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_credits_contentId` ON `tmdb_credits` (`contentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_credits_contentType` ON `tmdb_credits` (`contentType`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_credits_lastUpdated` ON `tmdb_credits` (`lastUpdated`)")

                // Create TMDb recommendations table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tmdb_recommendations` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `contentId` INTEGER NOT NULL,
                        `contentType` TEXT NOT NULL,
                        `recommendationType` TEXT NOT NULL,
                        `page` INTEGER NOT NULL,
                        `totalPages` INTEGER NOT NULL,
                        `totalResults` INTEGER NOT NULL,
                        `results` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for tmdb_recommendations
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tmdb_recommendations_contentId` ON `tmdb_recommendations` (`contentId`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tmdb_recommendations_contentType` ON `tmdb_recommendations` (`contentType`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tmdb_recommendations_recommendationType` ON `tmdb_recommendations` (`recommendationType`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tmdb_recommendations_lastUpdated` ON `tmdb_recommendations` (`lastUpdated`)",
                )

                // Create TMDb images table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tmdb_images` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `contentId` INTEGER NOT NULL,
                        `contentType` TEXT NOT NULL,
                        `backdrops` TEXT NOT NULL,
                        `posters` TEXT NOT NULL,
                        `logos` TEXT,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for tmdb_images
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_images_contentId` ON `tmdb_images` (`contentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_images_contentType` ON `tmdb_images` (`contentType`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_images_lastUpdated` ON `tmdb_images` (`lastUpdated`)")

                // Create TMDb videos table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tmdb_videos` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `contentId` INTEGER NOT NULL,
                        `contentType` TEXT NOT NULL,
                        `results` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for tmdb_videos
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_videos_contentId` ON `tmdb_videos` (`contentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_videos_contentType` ON `tmdb_videos` (`contentType`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_videos_lastUpdated` ON `tmdb_videos` (`lastUpdated`)")

                // Create TMDb genres table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tmdb_genres` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for tmdb_genres
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_genres_name` ON `tmdb_genres` (`name`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_genres_mediaType` ON `tmdb_genres` (`mediaType`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_genres_lastUpdated` ON `tmdb_genres` (`lastUpdated`)")

                // Create TMDb config table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tmdb_config` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `imageBaseUrl` TEXT NOT NULL,
                        `secureBaseUrl` TEXT NOT NULL,
                        `backdropSizes` TEXT NOT NULL,
                        `logoSizes` TEXT NOT NULL,
                        `posterSizes` TEXT NOT NULL,
                        `profileSizes` TEXT NOT NULL,
                        `stillSizes` TEXT NOT NULL,
                        `changeKeys` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create indices for tmdb_config
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_config_lastUpdated` ON `tmdb_config` (`lastUpdated`)")
            }
        }

    val MIGRATION_6_7 =
        object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes needed - just version bump to ensure consistency
                // All TMDb tables were already created in migration 5_6
            }
        }

    // All migrations for this database
    val ALL_MIGRATIONS =
        arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
        )
}
