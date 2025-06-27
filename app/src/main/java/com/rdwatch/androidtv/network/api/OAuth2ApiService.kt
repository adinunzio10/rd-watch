package com.rdwatch.androidtv.network.api

import com.rdwatch.androidtv.network.models.OAuth2CredentialsResponse
import com.rdwatch.androidtv.network.models.OAuth2DeviceCodeResponse
import com.rdwatch.androidtv.network.models.OAuth2TokenResponse
import com.rdwatch.androidtv.network.interceptors.NoAuth
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OAuth2ApiService {
    
    companion object {
        const val OAUTH_BASE_URL = "https://api.real-debrid.com/"
    }
    
    @GET("oauth/v2/device/code")
    suspend fun getDeviceCode(
        @Query("client_id") clientId: String,
        @Query("new_credentials") newCredentials: String = "yes"
    ): Response<OAuth2DeviceCodeResponse>
    
    @GET("oauth/v2/device/credentials")
    suspend fun getDeviceCredentials(
        @Query("client_id") clientId: String,
        @Query("code") deviceCode: String
    ): Response<OAuth2CredentialsResponse>
    
    @FormUrlEncoded
    @POST("oauth/v2/token")
    suspend fun getDeviceToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") deviceCode: String,
        @Field("grant_type") grantType: String = "http://oauth.net/grant_type/device/1.0"
    ): Response<OAuth2TokenResponse>
    
    @NoAuth
    @FormUrlEncoded
    @POST("oauth/v2/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<OAuth2TokenResponse>
}