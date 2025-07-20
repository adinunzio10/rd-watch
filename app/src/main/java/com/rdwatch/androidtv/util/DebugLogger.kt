package com.rdwatch.androidtv.util

import android.util.Log
import com.rdwatch.androidtv.BuildConfig

/**
 * Debug utility class that gates logging based on BuildConfig.DEBUG
 * This helps reduce log spam in production builds and improves performance
 */
object DebugLogger {
    /**
     * Debug level logging - only shown in debug builds
     */
    fun d(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    /**
     * Debug level logging with throwable - only shown in debug builds
     */
    fun d(
        tag: String,
        message: String,
        throwable: Throwable,
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message, throwable)
        }
    }

    /**
     * Info level logging - only shown in debug builds
     */
    fun i(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    /**
     * Warning level logging - always shown but can be gated for specific cases
     */
    fun w(
        tag: String,
        message: String,
    ) {
        Log.w(tag, message)
    }

    /**
     * Warning level logging with throwable - always shown
     */
    fun w(
        tag: String,
        message: String,
        throwable: Throwable,
    ) {
        Log.w(tag, message, throwable)
    }

    /**
     * Error level logging - always shown
     */
    fun e(
        tag: String,
        message: String,
    ) {
        Log.e(tag, message)
    }

    /**
     * Error level logging with throwable - always shown
     */
    fun e(
        tag: String,
        message: String,
        throwable: Throwable,
    ) {
        Log.e(tag, message, throwable)
    }

    /**
     * Verbose level logging - only shown in debug builds
     */
    fun v(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }
}
