package com.rdwatch.androidtv.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create users table
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
            
            // Create sites table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `sites` (
                    `site_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `site_code` TEXT NOT NULL,
                    `site_name` TEXT NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `region_id` TEXT NOT NULL,
                    `country_code` TEXT NOT NULL,
                    `time_zone` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    `is_active` INTEGER NOT NULL DEFAULT 1,
                    `description` TEXT,
                    `elevation_meters` REAL
                )
            """.trimIndent())
            
            // Create indices for sites
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sites_site_code` ON `sites` (`site_code`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sites_region_id` ON `sites` (`region_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sites_country_code` ON `sites` (`country_code`)")
            
            // Create models table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `models` (
                    `model_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `model_name` TEXT NOT NULL,
                    `model_version` TEXT NOT NULL,
                    `model_type` TEXT NOT NULL,
                    `description` TEXT,
                    `configuration_json` TEXT,
                    `accuracy_metrics` TEXT,
                    `training_date` INTEGER,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    `is_active` INTEGER NOT NULL DEFAULT 1,
                    `author` TEXT,
                    `model_file_path` TEXT
                )
            """.trimIndent())
            
            // Create indices for models
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_models_model_name` ON `models` (`model_name`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_models_model_version` ON `models` (`model_version`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_models_created_at` ON `models` (`created_at`)")
            
            // Create observations table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `observations` (
                    `observation_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `site_id` INTEGER NOT NULL,
                    `model_id` INTEGER,
                    `observation_date` INTEGER NOT NULL,
                    `sensor_type` TEXT NOT NULL,
                    `image_url` TEXT,
                    `metadata_json` TEXT,
                    `confidence_score` REAL,
                    `processing_status` TEXT NOT NULL DEFAULT 'pending',
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    `quality_flag` TEXT,
                    `cloud_cover_percentage` REAL,
                    FOREIGN KEY(`site_id`) REFERENCES `sites`(`site_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`model_id`) REFERENCES `models`(`model_id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
            """.trimIndent())
            
            // Create indices for observations
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_observations_site_id` ON `observations` (`site_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_observations_model_id` ON `observations` (`model_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_observations_observation_date` ON `observations` (`observation_date`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_observations_created_at` ON `observations` (`created_at`)")
            
            // Create user_site_cross_ref table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `user_site_cross_ref` (
                    `user_id` INTEGER NOT NULL,
                    `site_id` INTEGER NOT NULL,
                    `role` TEXT NOT NULL DEFAULT 'viewer',
                    `created_at` INTEGER NOT NULL,
                    `is_active` INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY(`user_id`, `site_id`),
                    FOREIGN KEY(`user_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`site_id`) REFERENCES `sites`(`site_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Create indices for user_site_cross_ref
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_site_cross_ref_user_id` ON `user_site_cross_ref` (`user_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_site_cross_ref_site_id` ON `user_site_cross_ref` (`site_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_site_cross_ref_created_at` ON `user_site_cross_ref` (`created_at`)")
            
            // Create user_model_cross_ref table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `user_model_cross_ref` (
                    `user_id` INTEGER NOT NULL,
                    `model_id` INTEGER NOT NULL,
                    `permission_level` TEXT NOT NULL DEFAULT 'read',
                    `created_at` INTEGER NOT NULL,
                    `is_active` INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY(`user_id`, `model_id`),
                    FOREIGN KEY(`user_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`model_id`) REFERENCES `models`(`model_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Create indices for user_model_cross_ref
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_model_cross_ref_user_id` ON `user_model_cross_ref` (`user_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_model_cross_ref_model_id` ON `user_model_cross_ref` (`model_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_model_cross_ref_created_at` ON `user_model_cross_ref` (`created_at`)")
            
            // Create watch_progress table
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
            
            // Create library table
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
            
            // Create scraper_manifests table
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
            
            // Create search_history table
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
    
    // All migrations for this database
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2
    )
}