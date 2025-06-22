package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "sites",
    indices = [
        Index(value = ["site_code"], unique = true),
        Index(value = ["region_id"]),
        Index(value = ["country_code"])
    ]
)
data class SiteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "site_id")
    val siteId: Long = 0,
    
    @ColumnInfo(name = "site_code")
    val siteCode: String,
    
    @ColumnInfo(name = "site_name")
    val siteName: String,
    
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    
    @ColumnInfo(name = "region_id")
    val regionId: String,
    
    @ColumnInfo(name = "country_code")
    val countryCode: String,
    
    @ColumnInfo(name = "time_zone")
    val timeZone: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "elevation_meters")
    val elevationMeters: Double? = null
)