package com.rdwatch.androidtv.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OAuth2ErrorResponse(
    @Json(name = "error")
    val error: String,
    @Json(name = "error_description")
    val errorDescription: String?
)