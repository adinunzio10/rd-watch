package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "library",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["content_type"]),
        Index(value = ["added_at"]),
        Index(value = ["is_favorite"]),
        Index(value = ["user_id", "content_id"], unique = true),
    ],
)
data class LibraryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "library_id")
    val libraryId: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "content_id")
    val contentId: String,
    @ColumnInfo(name = "content_type")
    val contentType: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_downloaded")
    val isDownloaded: Boolean = false,
    @ColumnInfo(name = "file_path")
    val filePath: String? = null,
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long? = null,
    @ColumnInfo(name = "added_at")
    val addedAt: Date,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
)
