package com.rdwatch.androidtv.network.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class BooleanAdapter {
    @ToJson
    fun toJson(value: Boolean): Int {
        return if (value) 1 else 0
    }
    
    @FromJson
    fun fromJson(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> false
        }
    }
}