package com.rdwatch.androidtv.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserInfo(
    @Json(name = "id") val id: Long,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "points") val points: Int,
    @Json(name = "locale") val locale: String,
    @Json(name = "avatar") val avatar: String,
    @Json(name = "type") val type: String,
    @Json(name = "premium") val premium: Int,
    @Json(name = "expiration") val expiration: String?
)