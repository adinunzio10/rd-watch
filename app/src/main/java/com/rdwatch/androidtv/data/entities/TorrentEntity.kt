package com.rdwatch.androidtv.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing Real-Debrid torrent metadata
 */
@Entity(
    tableName = "torrents",
    indices = [
        Index(value = ["hash"]),
        Index(value = ["status"]),
        Index(value = ["added"]),
    ],
)
data class TorrentEntity(
    @PrimaryKey
    val id: String,
    val hash: String,
    val filename: String,
    val bytes: Long,
    val links: List<String>,
    val split: Int,
    val progress: Float,
    val status: String,
    val added: Date,
    val speed: Long? = null,
    val seeders: Int? = null,
    val created: Date? = null,
    val ended: Date? = null,
)
