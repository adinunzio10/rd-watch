package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "models",
    indices = [
        Index(value = ["model_name"], unique = true),
        Index(value = ["model_version"]),
        Index(value = ["created_at"])
    ]
)
data class ModelEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "model_id")
    val modelId: Long = 0,
    
    @ColumnInfo(name = "model_name")
    val modelName: String,
    
    @ColumnInfo(name = "model_version")
    val modelVersion: String,
    
    @ColumnInfo(name = "model_type")
    val modelType: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "configuration_json")
    val configurationJson: String? = null,
    
    @ColumnInfo(name = "accuracy_metrics")
    val accuracyMetrics: String? = null,
    
    @ColumnInfo(name = "training_date")
    val trainingDate: Date? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "author")
    val author: String? = null,
    
    @ColumnInfo(name = "model_file_path")
    val modelFilePath: String? = null
)