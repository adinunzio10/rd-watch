package com.rdwatch.androidtv.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create users table (simplified for TV/Real-Debrid auth)
            database.execSQL("""
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
            """.trimIndent())
            
            // Create indices for users
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")
            
            // Create watch_progress table for tracking user viewing progress
            database.execSQL("""
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
            """.trimIndent())
            
            // Create indices for watch_progress
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_progress_user_id` ON `watch_progress` (`user_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_progress_content_id` ON `watch_progress` (`content_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_progress_updated_at` ON `watch_progress` (`updated_at`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_watch_progress_user_id_content_id` ON `watch_progress` (`user_id`, `content_id`)")
            
            // Create library table for user's saved content
            database.execSQL("""
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
            """.trimIndent())
            
            // Create indices for library
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_library_user_id` ON `library` (`user_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_library_content_type` ON `library` (`content_type`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_library_added_at` ON `library` (`added_at`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_library_is_favorite` ON `library` (`is_favorite`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_library_user_id_content_id` ON `library` (`user_id`, `content_id`)")
            
            // Create scraper_manifests table for content scraper configurations
            database.execSQL("""
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
            """.trimIndent())
            
            // Create indices for scraper_manifests
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scraper_manifests_scraper_name` ON `scraper_manifests` (`scraper_name`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_scraper_manifests_is_enabled` ON `scraper_manifests` (`is_enabled`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_scraper_manifests_updated_at` ON `scraper_manifests` (`updated_at`)")
            
            // Create search_history table for user search queries
            database.execSQL("""
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
            """.trimIndent())
            
            // Create indices for search_history
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_user_id` ON `search_history` (`user_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_search_query` ON `search_history` (`search_query`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_search_date` ON `search_history` (`search_date`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_user_id_search_query` ON `search_history` (`user_id`, `search_query`)")
        }
    }
    
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create content table
            database.execSQL("""
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
            """.trimIndent())
            
            // Create indices for content
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_source` ON `content` (`source`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_title` ON `content` (`title`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_addedDate` ON `content` (`addedDate`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_isFavorite` ON `content` (`isFavorite`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_content_isWatched` ON `content` (`isWatched`)")
            
            // Create torrents table
            database.execSQL("""
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
            """.trimIndent())
            
            // Create indices for torrents
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_torrents_hash` ON `torrents` (`hash`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_torrents_status` ON `torrents` (`status`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_torrents_added` ON `torrents` (`added`)")
            
            // Create downloads table
            database.execSQL("""
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
            """.trimIndent())
            
            // Create indices for downloads
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_filename` ON `downloads` (`filename`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_mimeType` ON `downloads` (`mimeType`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_streamable` ON `downloads` (`streamable`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_generated` ON `downloads` (`generated`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_contentId` ON `downloads` (`contentId`)")
        }
    }
    
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create file_hashes table for caching OpenSubtitles hash calculations
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `file_hashes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `file_path` TEXT NOT NULL,
                    `hash_value` TEXT NOT NULL,
                    `file_size` INTEGER NOT NULL,
                    `last_modified` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create indices for file_hashes
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_file_hashes_file_path` ON `file_hashes` (`file_path`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_file_hashes_hash_value` ON `file_hashes` (`hash_value`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_file_hashes_last_modified_file_size` ON `file_hashes` (`last_modified`, `file_size`)")
        }
    }
    
    // All migrations for this database
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4
    )
}