package com.rdwatch.androidtv.network.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.text.SimpleDateFormat
import java.util.*

class DateAdapter {
    private val dateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    // Alternative formats Real-Debrid might use
    private val alternativeDateFormats =
        listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
        ).onEach { it.timeZone = TimeZone.getTimeZone("UTC") }

    @ToJson
    fun toJson(date: Date): String {
        return dateFormat.format(date)
    }

    @FromJson
    fun fromJson(dateString: String): Date? {
        if (dateString.isEmpty()) return null

        // Try primary format first
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            // Try alternative formats
            alternativeDateFormats.forEach { format ->
                try {
                    return format.parse(dateString)
                } catch (ignored: Exception) {
                    // Continue to next format
                }
            }

            // If all formats fail, try Unix timestamp
            try {
                Date(dateString.toLong() * 1000) // Convert seconds to milliseconds
            } catch (e: Exception) {
                null
            }
        }
    }
}
