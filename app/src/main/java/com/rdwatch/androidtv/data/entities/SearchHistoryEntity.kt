package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "search_history",
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
        Index(value = ["search_query"]),
        Index(value = ["search_date"]),
        Index(value = ["user_id", "search_query"]),
    ],
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "search_id")
    val searchId: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "search_query")
    val searchQuery: String,
    @ColumnInfo(name = "search_type")
    val searchType: String = "general",
    @ColumnInfo(name = "results_count")
    val resultsCount: Int = 0,
    @ColumnInfo(name = "search_date")
    val searchDate: Date,
    @ColumnInfo(name = "filters_json")
    val filtersJson: String? = null,
    @ColumnInfo(name = "response_time_ms")
    val responseTimeMs: Long? = null,
)
