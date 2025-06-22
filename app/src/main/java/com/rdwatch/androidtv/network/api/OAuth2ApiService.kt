package com.rdwatch.androidtv.network.api

import com.rdwatch.androidtv.network.models.OAuth2DeviceCodeResponse
import com.rdwatch.androidtv.network.models.OAuth2TokenResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OAuth2ApiService {
    
    @FormUrlEncoded
    @POST("oauth/device/code")
    suspend fun getDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String = ""
    ): Response<OAuth2DeviceCodeResponse>
    
    @FormUrlEncoded
    @POST("oauth/device/credentials")
    suspend fun getDeviceToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): Response<OAuth2TokenResponse>
    
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<OAuth2TokenResponse>
}