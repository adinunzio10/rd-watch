package com.rdwatch.androidtv.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    @Json(name = "error") val error: String?,
    @Json(name = "error_code") val errorCode: Int?,
    @Json(name = "error_details") val errorDetails: String?,
)
