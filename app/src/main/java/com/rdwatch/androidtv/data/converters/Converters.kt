package com.rdwatch.androidtv.data.converters

import androidx.room.TypeConverter
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