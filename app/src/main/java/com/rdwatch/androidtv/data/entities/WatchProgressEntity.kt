package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "watch_progress",
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
        Index(value = ["content_id"]),
        Index(value = ["updated_at"]),
        Index(value = ["user_id", "content_id"], unique = true),
    ],
)
data class WatchProgressEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "progress_id")
    val progressId: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "content_id")
    val contentId: String,
    @ColumnInfo(name = "progress_seconds")
    val progressSeconds: Long,
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long,
    @ColumnInfo(name = "watch_percentage")
    val watchPercentage: Float,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
    @ColumnInfo(name = "device_info")
    val deviceInfo: String? = null,
)
