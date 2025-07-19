package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "scraper_manifests",
    indices = [
        Index(value = ["scraper_name"], unique = true),
        Index(value = ["is_enabled"]),
        Index(value = ["updated_at"]),
    ],
)
data class ScraperManifestEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "manifest_id")
    val manifestId: Long = 0,
    @ColumnInfo(name = "scraper_name")
    val scraperName: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "version")
    val version: String,
    @ColumnInfo(name = "base_url")
    val baseUrl: String,
    @ColumnInfo(name = "config_json")
    val configJson: String,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    @ColumnInfo(name = "priority_order")
    val priorityOrder: Int = 0,
    @ColumnInfo(name = "rate_limit_ms")
    val rateLimitMs: Long = 1000,
    @ColumnInfo(name = "timeout_seconds")
    val timeoutSeconds: Int = 30,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "author")
    val author: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
)
