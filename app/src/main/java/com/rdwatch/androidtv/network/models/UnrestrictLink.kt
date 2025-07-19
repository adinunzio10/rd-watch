package com.rdwatch.androidtv.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UnrestrictLinkResponse(
    @Json(name = "id") val id: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "mimeType") val mimeType: String?,
    @Json(name = "filesize") val filesize: Long,
    @Json(name = "link") val link: String,
    @Json(name = "host") val host: String,
    @Json(name = "chunks") val chunks: Int,
    @Json(name = "crc") val crc: Int,
    @Json(name = "download") val download: String,
    @Json(name = "streamable") val streamable: Int,
)

@JsonClass(generateAdapter = true)
data class Host(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "image") val image: String,
)
