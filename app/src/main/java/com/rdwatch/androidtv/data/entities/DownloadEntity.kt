package com.rdwatch.androidtv.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing Real-Debrid download information
 */
@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["filename"]),
        Index(value = ["mimeType"]),
        Index(value = ["streamable"]),
        Index(value = ["generated"]),
        Index(value = ["contentId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ContentEntity::class,
            parentColumns = ["id"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val filename: String,
    val mimeType: String,
    val filesize: Long,
    val link: String,
    val host: String,
    val chunks: Int,
    val download: String,
    val streamable: Boolean,
    val generated: Date,
    val type: String? = null,
    val alternative: List<String>? = null,
    val contentId: Long? = null, // Foreign key to ContentEntity
)
