package com.rdwatch.androidtv.auth.models

data class DeviceCodeInfo(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int
) {
    val verificationUriComplete: String
        get() = "$verificationUri?user_code=$userCode"
}