package com.rdwatch.androidtv.network.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class NullToEmptyString

class NullToEmptyStringAdapter {
    @ToJson
    fun toJson(
        @NullToEmptyString value: String?,
    ): String? {
        return value
    }

    @FromJson
    @NullToEmptyString
    fun fromJson(value: String?): String {
        return value ?: ""
    }
}
