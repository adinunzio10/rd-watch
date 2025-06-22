package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "user_site_cross_ref",
    primaryKeys = ["user_id", "site_id"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["site_id"],
            childColumns = ["site_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["site_id"]),
        Index(value = ["created_at"])
    ]
)
data class UserSiteCrossRef(
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    @ColumnInfo(name = "site_id")
    val siteId: Long,
    
    @ColumnInfo(name = "role")
    val role: String = "viewer",
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)