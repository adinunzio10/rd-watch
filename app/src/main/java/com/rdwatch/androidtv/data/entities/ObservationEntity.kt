package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "observations",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["site_id"],
            childColumns = ["site_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ModelEntity::class,
            parentColumns = ["model_id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["site_id"]),
        Index(value = ["model_id"]),
        Index(value = ["observation_date"]),
        Index(value = ["created_at"])
    ]
)
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "observation_id")
    val observationId: Long = 0,
    
    @ColumnInfo(name = "site_id")
    val siteId: Long,
    
    @ColumnInfo(name = "model_id")
    val modelId: Long? = null,
    
    @ColumnInfo(name = "observation_date")
    val observationDate: Date,
    
    @ColumnInfo(name = "sensor_type")
    val sensorType: String,
    
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,
    
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null,
    
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float? = null,
    
    @ColumnInfo(name = "processing_status")
    val processingStatus: String = "pending",
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
    
    @ColumnInfo(name = "quality_flag")
    val qualityFlag: String? = null,
    
    @ColumnInfo(name = "cloud_cover_percentage")
    val cloudCoverPercentage: Float? = null
)