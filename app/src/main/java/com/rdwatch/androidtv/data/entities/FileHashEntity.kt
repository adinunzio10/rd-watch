package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for caching file hashes to avoid recalculating for the same files.
 * Uses OpenSubtitles hash algorithm (first 64KB + last 64KB + file size).
 */
@Entity(
    tableName = "file_hashes",
    indices = [
        Index(value = ["file_path"], unique = true),
        Index(value = ["hash_value"]),
        Index(value = ["last_modified", "file_size"])
    ]
)
data class FileHashEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "hash_value")
    val hashValue: String,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    @ColumnInfo(name = "last_modified")
    val lastModified: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)