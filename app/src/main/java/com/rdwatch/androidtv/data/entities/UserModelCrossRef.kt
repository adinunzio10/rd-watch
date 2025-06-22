package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "user_model_cross_ref",
    primaryKeys = ["user_id", "model_id"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ModelEntity::class,
            parentColumns = ["model_id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["model_id"]),
        Index(value = ["created_at"])
    ]
)
data class UserModelCrossRef(
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    @ColumnInfo(name = "model_id")
    val modelId: Long,
    
    @ColumnInfo(name = "permission_level")
    val permissionLevel: String = "read",
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)