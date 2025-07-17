package com.rdwatch.androidtv.network.api

import com.rdwatch.androidtv.network.models.*
import retrofit2.Response
import retrofit2.http.*

interface RealDebridApiService {
    companion object {
        const val BASE_URL = "https://api.real-debrid.com/rest/1.0/"
    }

    // User endpoints
    @GET("user")
    suspend fun getUserInfo(): Response<UserInfo>

    // Unrestrict endpoints
    @POST("unrestrict/link")
    @FormUrlEncoded
    suspend fun unrestrictLink(
        @Field("link") link: String,
        @Field("password") password: String? = null,
        @Field("remote") remote: Int? = null,
    ): Response<UnrestrictLinkResponse>

    // Traffic endpoints
    @GET("traffic")
    suspend fun getTrafficInfo(): Response<Map<String, Any>>

    @GET("traffic/details")
    suspend fun getTrafficDetails(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
    ): Response<Map<String, Any>>

    // Streaming endpoints
    @GET("streaming/transcode/{id}")
    suspend fun getStreamingTranscode(
        @Path("id") id: String,
    ): Response<Map<String, Any>>

    @GET("streaming/mediaInfos/{id}")
    suspend fun getMediaInfo(
        @Path("id") id: String,
    ): Response<Map<String, Any>>

    // Downloads endpoints
    @GET("downloads")
    suspend fun getDownloads(
        @Query("offset") offset: Int? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): Response<List<Map<String, Any>>>

    @DELETE("downloads/delete/{id}")
    suspend fun deleteDownload(
        @Path("id") id: String,
    ): Response<Unit>

    @DELETE("downloads/delete")
    @FormUrlEncoded
    suspend fun deleteDownloads(
        @Field("ids") ids: String,
    ): Response<Unit>

    // Torrents endpoints
    @GET("torrents")
    suspend fun getTorrents(
        @Query("offset") offset: Int? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("filter") filter: String? = null,
    ): Response<List<TorrentInfo>>

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(
        @Path("id") id: String,
    ): Response<TorrentInfo>

    @GET("torrents/instantAvailability/{hash}")
    suspend fun checkInstantAvailability(
        @Path("hash") hash: String,
    ): Response<Map<String, Any>>

    @GET("torrents/activeCount")
    suspend fun getActiveCount(): Response<Map<String, Int>>

    @GET("torrents/availableHosts")
    suspend fun getAvailableHosts(): Response<List<Host>>

    @PUT("torrents/addTorrent")
    suspend fun addTorrent(
        @Body file: okhttp3.RequestBody,
    ): Response<AddTorrentResponse>

    @POST("torrents/addMagnet")
    @FormUrlEncoded
    suspend fun addMagnet(
        @Field("magnet") magnet: String,
        @Field("host") host: String? = null,
    ): Response<AddTorrentResponse>

    @POST("torrents/selectFiles/{id}")
    @FormUrlEncoded
    suspend fun selectFiles(
        @Path("id") id: String,
        @Field("files") files: String,
    ): Response<Unit>

    @DELETE("torrents/delete/{id}")
    suspend fun deleteTorrent(
        @Path("id") id: String,
    ): Response<Unit>

    @DELETE("torrents/delete")
    @FormUrlEncoded
    suspend fun deleteTorrents(
        @Field("hashes") hashes: String,
    ): Response<Unit>

    // Hosts endpoints
    @GET("hosts")
    suspend fun getHosts(): Response<Map<String, Host>>

    @GET("hosts/status")
    suspend fun getHostsStatus(): Response<Map<String, Any>>

    @GET("hosts/regex")
    suspend fun getHostsRegex(): Response<List<String>>

    @GET("hosts/domains")
    suspend fun getHostsDomains(): Response<List<String>>

    // Settings endpoints
    @GET("settings")
    suspend fun getSettings(): Response<Map<String, Any>>

    @POST("settings/update")
    @FormUrlEncoded
    suspend fun updateSettings(
        @Field("setting_name") settingName: String,
        @Field("setting_value") settingValue: String,
    ): Response<Unit>

    @POST("settings/convertPoints")
    suspend fun convertPoints(): Response<Unit>

    @POST("settings/changePassword")
    @FormUrlEncoded
    suspend fun changePassword(
        @Field("password") password: String,
    ): Response<Unit>

    @POST("settings/avatarFile")
    suspend fun uploadAvatar(
        @Body file: okhttp3.RequestBody,
    ): Response<Unit>

    @DELETE("settings/avatarDelete")
    suspend fun deleteAvatar(): Response<Unit>

    // Disable access token endpoint
    @GET("disable_access_token")
    suspend fun disableAccessToken(): Response<Unit>
}
