package com.rdwatch.androidtv.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TorrentInfo(
    @Json(name = "id") val id: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "hash") val hash: String,
    @Json(name = "bytes") val bytes: Long,
    @Json(name = "original_filename") val originalFilename: String?,
    @Json(name = "host") val host: String,
    @Json(name = "split") val split: Int,
    @Json(name = "progress") val progress: Float,
    @Json(name = "status") val status: String,
    @Json(name = "added") val added: String,
    @Json(name = "ended") val ended: String?,
    @Json(name = "speed") val speed: Long?,
    @Json(name = "seeders") val seeders: Int?,
    @Json(name = "links") val links: List<String>
)

@JsonClass(generateAdapter = true)
data class TorrentFile(
    @Json(name = "id") val id: Int,
    @Json(name = "path") val path: String,
    @Json(name = "bytes") val bytes: Long,
    @Json(name = "selected") val selected: Int
)

@JsonClass(generateAdapter = true)
data class AddTorrentResponse(
    @Json(name = "id") val id: String,
    @Json(name = "uri") val uri: String
)