package com.rdwatch.androidtv.data.converters

import androidx.room.TypeConverter
import com.rdwatch.androidtv.data.entities.ContentSource
import com.rdwatch.androidtv.data.entities.TMDbSearchItemEntity
import com.rdwatch.androidtv.data.entities.TMDbCastMemberEntity
import com.rdwatch.androidtv.data.entities.TMDbCrewMemberEntity
import com.rdwatch.androidtv.data.entities.TMDbImageEntity
import com.rdwatch.androidtv.data.entities.TMDbVideoEntity
import com.rdwatch.androidtv.data.entities.TMDbTrendingItem
import com.rdwatch.androidtv.data.entities.TMDbDiscoveryFilters
import com.rdwatch.androidtv.data.entities.TMDbGenreItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.Date

class Converters {
    
    private val moshi = Moshi.Builder().build()
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter: JsonAdapter<List<String>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromListString(list: List<String>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter: JsonAdapter<List<String>> = moshi.adapter(type)
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            null
        }
    }
    
    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String>? {
        if (value == null) return null
        val type = Types.newParameterizedType(
            Map::class.java, 
            String::class.java, 
            String::class.java
        )
        val adapter: JsonAdapter<Map<String, String>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    @TypeConverter
    fun fromMapString(map: Map<String, String>?): String? {
        if (map == null) return null
        val type = Types.newParameterizedType(
            Map::class.java, 
            String::class.java, 
            String::class.java
        )
        val adapter: JsonAdapter<Map<String, String>> = moshi.adapter(type)
        return try {
            adapter.toJson(map)
        } catch (e: Exception) {
            null
        }
    }
    
    @TypeConverter
    fun fromStringIntMap(value: String?): Map<String, Int>? {
        if (value == null) return null
        val type = Types.newParameterizedType(
            Map::class.java, 
            String::class.java, 
            Int::class.javaObjectType
        )
        val adapter: JsonAdapter<Map<String, Int>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    @TypeConverter
    fun fromMapStringInt(map: Map<String, Int>?): String? {
        if (map == null) return null
        val type = Types.newParameterizedType(
            Map::class.java, 
            String::class.java, 
            Int::class.javaObjectType
        )
        val adapter: JsonAdapter<Map<String, Int>> = moshi.adapter(type)
        return try {
            adapter.toJson(map)
        } catch (e: Exception) {
            null
        }
    }
    
    @TypeConverter
    fun fromStringFloatMap(value: String?): Map<String, Float>? {
        if (value == null) return null
        val type = Types.newParameterizedType(
            Map::class.java, 
            String::class.java, 
            Float::class.javaObjectType
        )
        val adapter: JsonAdapter<Map<String, Float>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    @TypeConverter
    fun fromMapStringFloat(map: Map<String, Float>?): String? {
        if (map == null) return null
        val type = Types.newParameterizedType(
            Map::class.java, 
            String::class.java, 
            Float::class.javaObjectType
        )
        val adapter: JsonAdapter<Map<String, Float>> = moshi.adapter(type)
        return try {
            adapter.toJson(map)
        } catch (e: Exception) {
            null
        }
    }
    
    @TypeConverter
    fun fromCoordinates(value: String?): Coordinates? {
        if (value == null) return null
        val adapter: JsonAdapter<Coordinates> = moshi.adapter(Coordinates::class.java)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            null
        }
    }
    
    @TypeConverter
    fun coordinatesToString(coordinates: Coordinates?): String? {
        if (coordinates == null) return null
        val adapter: JsonAdapter<Coordinates> = moshi.adapter(Coordinates::class.java)
        return try {
            adapter.toJson(coordinates)
        } catch (e: Exception) {
            null
        }
    }
    
    @TypeConverter
    fun fromBoundingBox(value: String?): BoundingBox? {
        if (value == null) return null
        val adapter: JsonAdapter<BoundingBox> = moshi.adapter(BoundingBox::class.java)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            null
        }
    }
    
    @TypeConverter
    fun boundingBoxToString(boundingBox: BoundingBox?): String? {
        if (boundingBox == null) return null
        val adapter: JsonAdapter<BoundingBox> = moshi.adapter(BoundingBox::class.java)
        return try {
            adapter.toJson(boundingBox)
        } catch (e: Exception) {
            null
        }
    }
    
    @TypeConverter
    fun fromContentSource(value: String?): ContentSource? {
        return value?.let {
            try {
                ContentSource.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
    
    @TypeConverter
    fun contentSourceToString(source: ContentSource?): String? {
        return source?.name
    }
    
    // List<Int> converters for TMDb entities
    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
        val adapter: JsonAdapter<List<Int>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromListInt(list: List<Int>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
        val adapter: JsonAdapter<List<Int>> = moshi.adapter(type)
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            null
        }
    }
    
    // The following converters were removed as they reference non-existent classes:
    // - TMDbCastMember, TMDbCrewMember, TMDbImage, TMDbVideo (use Entity versions)
    // - TMDbTrendingItem, TMDbDiscoveryFilters, TMDbGenreItem (not in TMDbEntities.kt)
    
    // TMDb Search Item converters
    @TypeConverter
    fun fromTMDbSearchItemEntityList(value: String?): List<TMDbSearchItemEntity>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbSearchItemEntity::class.java)
        val adapter: JsonAdapter<List<TMDbSearchItemEntity>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromListTMDbSearchItemEntity(list: List<TMDbSearchItemEntity>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbSearchItemEntity::class.java)
        val adapter: JsonAdapter<List<TMDbSearchItemEntity>> = moshi.adapter(type)
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            null
        }
    }
    
    // TMDb Cast Member Entity converters
    @TypeConverter
    fun fromTMDbCastMemberEntityList(value: String?): List<TMDbCastMemberEntity>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbCastMemberEntity::class.java)
        val adapter: JsonAdapter<List<TMDbCastMemberEntity>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromListTMDbCastMemberEntity(list: List<TMDbCastMemberEntity>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbCastMemberEntity::class.java)
        val adapter: JsonAdapter<List<TMDbCastMemberEntity>> = moshi.adapter(type)
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            null
        }
    }
    
    // TMDb Crew Member Entity converters
    @TypeConverter
    fun fromTMDbCrewMemberEntityList(value: String?): List<TMDbCrewMemberEntity>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbCrewMemberEntity::class.java)
        val adapter: JsonAdapter<List<TMDbCrewMemberEntity>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromListTMDbCrewMemberEntity(list: List<TMDbCrewMemberEntity>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbCrewMemberEntity::class.java)
        val adapter: JsonAdapter<List<TMDbCrewMemberEntity>> = moshi.adapter(type)
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            null
        }
    }
    
    // TMDb Image Entity converters
    @TypeConverter
    fun fromTMDbImageEntityList(value: String?): List<TMDbImageEntity>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbImageEntity::class.java)
        val adapter: JsonAdapter<List<TMDbImageEntity>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromListTMDbImageEntity(list: List<TMDbImageEntity>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbImageEntity::class.java)
        val adapter: JsonAdapter<List<TMDbImageEntity>> = moshi.adapter(type)
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            null
        }
    }
    
    // TMDb Video Entity converters
    @TypeConverter
    fun fromTMDbVideoEntityList(value: String?): List<TMDbVideoEntity>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbVideoEntity::class.java)
        val adapter: JsonAdapter<List<TMDbVideoEntity>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromListTMDbVideoEntity(list: List<TMDbVideoEntity>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbVideoEntity::class.java)
        val adapter: JsonAdapter<List<TMDbVideoEntity>> = moshi.adapter(type)
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            null
        }
    }
    
    // TMDb Trending Item converters
    @TypeConverter
    fun fromTMDbTrendingItemList(value: String?): List<TMDbTrendingItem>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbTrendingItem::class.java)
        val adapter: JsonAdapter<List<TMDbTrendingItem>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromListTMDbTrendingItem(list: List<TMDbTrendingItem>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbTrendingItem::class.java)
        val adapter: JsonAdapter<List<TMDbTrendingItem>> = moshi.adapter(type)
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            null
        }
    }
    
    // TMDb Discovery Filters converters
    @TypeConverter
    fun fromTMDbDiscoveryFilters(value: String?): TMDbDiscoveryFilters? {
        if (value == null) return null
        val adapter: JsonAdapter<TMDbDiscoveryFilters> = moshi.adapter(TMDbDiscoveryFilters::class.java)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            null
        }
    }
    
    @TypeConverter
    fun fromTMDbDiscoveryFiltersToString(filters: TMDbDiscoveryFilters?): String? {
        if (filters == null) return null
        val adapter: JsonAdapter<TMDbDiscoveryFilters> = moshi.adapter(TMDbDiscoveryFilters::class.java)
        return try {
            adapter.toJson(filters)
        } catch (e: Exception) {
            null
        }
    }
    
    // TMDb Genre Item converters
    @TypeConverter
    fun fromTMDbGenreItemList(value: String?): List<TMDbGenreItem>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbGenreItem::class.java)
        val adapter: JsonAdapter<List<TMDbGenreItem>> = moshi.adapter(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromListTMDbGenreItem(list: List<TMDbGenreItem>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, TMDbGenreItem::class.java)
        val adapter: JsonAdapter<List<TMDbGenreItem>> = moshi.adapter(type)
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            null
        }
    }
}

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null
)

data class BoundingBox(
    val northEast: Coordinates,
    val southWest: Coordinates
)