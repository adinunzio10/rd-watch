package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing content that can be from local or Real-Debrid sources
 */
@Entity(
    tableName = "content",
    indices = [
        Index(value = ["source"]),
        Index(value = ["title"]),
        Index(value = ["addedDate"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isWatched"])
    ]
)
data class ContentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val year: Int? = null,
    val quality: String? = null,
    val source: ContentSource,
    val realDebridId: String? = null, // Real-Debrid torrent/download ID for API operations
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val description: String? = null,
    val duration: Int? = null, // in minutes
    val rating: Float? = null,
    val genres: List<String>? = null,
    val cast: List<String>? = null,
    val director: String? = null,
    val imdbId: String? = null,
    val tmdbId: Int? = null,
    val addedDate: Date = Date(),
    val lastPlayedDate: Date? = null,
    @ColumnInfo(defaultValue = "0")
    val playCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isWatched: Boolean = false
)

enum class ContentSource {
    LOCAL,
    REAL_DEBRID
}