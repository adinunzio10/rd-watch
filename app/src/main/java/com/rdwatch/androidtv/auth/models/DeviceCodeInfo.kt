package com.rdwatch.androidtv.auth.models

import android.util.Log
import java.net.URL

data class DeviceCodeInfo(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int,
) {
    val verificationUriComplete: String
        get() {
            val uri = "$verificationUri?user_code=$userCode"

            // Validate the URL
            try {
                URL(uri)
                Log.d("DeviceCodeInfo", "Valid verification URI: $uri")
            } catch (e: Exception) {
                Log.e("DeviceCodeInfo", "Invalid verification URI: $uri", e)
            }

            return uri
        }
}
