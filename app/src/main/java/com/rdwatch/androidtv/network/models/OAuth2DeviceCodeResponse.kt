package com.rdwatch.androidtv.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OAuth2DeviceCodeResponse(
    @Json(name = "device_code")
    val deviceCode: String,
    @Json(name = "user_code")
    val userCode: String,
    @Json(name = "verification_url")
    val verificationUri: String,
    @Json(name = "expires_in")
    val expiresIn: Int,
    @Json(name = "interval")
    val interval: Int,
)
