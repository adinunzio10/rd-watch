package com.rdwatch.androidtv.player.error

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class PlayerErrorHandler
    @Inject
    constructor() {
        private val _currentError = MutableStateFlow<PlayerError?>(null)
        val currentError: StateFlow<PlayerError?> = _currentError.asStateFlow()

        private val _errorHistory = MutableStateFlow<List<PlayerError>>(emptyList())
        val errorHistory: StateFlow<List<PlayerError>> = _errorHistory.asStateFlow()

        private var retryCount = 0
        private var lastRetryTime = 0L

        fun handleError(exception: PlaybackException): PlayerError {
            val error =
                when (exception.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    -> {
                        PlayerError.NetworkError(
                            message = "Network connection failed. Please check your internet connection.",
                            originalException = exception,
                            retryable = true,
                        )
                    }

                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                        val httpException = exception.cause as? HttpDataSource.HttpDataSourceException
                        val responseCode =
                            httpException?.let {
                                // Media3 HttpDataSourceException doesn't expose responseCode directly
                                // Parse from message or use a default
                                if (it.message?.contains("400") == true) {
                                    400
                                } else if (it.message?.contains("404") == true) {
                                    404
                                } else if (it.message?.contains("500") == true) {
                                    500
                                } else {
                                    0
                                }
                            } ?: 0
                        PlayerError.HttpError(
                            message = "Server returned error: ${httpException?.message ?: "Unknown"}",
                            responseCode = responseCode,
                            originalException = exception,
                            retryable = isRetryableHttpError(responseCode),
                        )
                    }

                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
                    -> {
                        PlayerError.FormatError(
                            message = "Video format is not supported or corrupted.",
                            originalException = exception,
                            retryable = false,
                        )
                    }

                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
                    -> {
                        PlayerError.DecoderError(
                            message = "Device cannot decode this video format.",
                            originalException = exception,
                            retryable = false,
                        )
                    }

                    PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED,
                    PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
                    -> {
                        PlayerError.DrmError(
                            message = "Content protection error. This content may not be available.",
                            originalException = exception,
                            retryable = false,
                        )
                    }

                    else -> {
                        PlayerError.UnknownError(
                            message = "An unexpected error occurred: ${exception.message}",
                            originalException = exception,
                            retryable = true,
                        )
                    }
                }

            addToErrorHistory(error)
            _currentError.value = error

            return error
        }

        private fun isRetryableHttpError(responseCode: Int): Boolean {
            return when (responseCode) {
                408, 429, 500, 502, 503, 504 -> true // Timeout, rate limit, server errors
                else -> false
            }
        }

        private fun addToErrorHistory(error: PlayerError) {
            val current = _errorHistory.value
            _errorHistory.value = listOf(error) + current.take(9) // Keep last 10 errors
        }

        fun shouldRetry(error: PlayerError): Boolean {
            val currentTime = System.currentTimeMillis()

            // Don't retry non-retryable errors
            if (!error.retryable) return false

            // Don't retry too frequently (minimum 5 seconds between retries)
            if (currentTime - lastRetryTime < 5000) return false

            // Don't retry more than 3 times for the same error type
            val recentSameErrors =
                _errorHistory.value
                    .take(5)
                    .count { it::class == error::class }

            return recentSameErrors < 3
        }

        fun performRetry(): Boolean {
            val currentTime = System.currentTimeMillis()
            lastRetryTime = currentTime
            retryCount++

            return true
        }

        fun clearError() {
            _currentError.value = null
            retryCount = 0
        }

        fun getRetryDelay(attempt: Int): Long {
            // Exponential backoff: 2^attempt seconds, max 30 seconds
            return minOf(2000L * (1 shl attempt), 30000L)
        }

        fun getUserFriendlyMessage(error: PlayerError): String {
            return when (error) {
                is PlayerError.NetworkError -> "Please check your internet connection and try again."
                is PlayerError.HttpError ->
                    when (error.responseCode) {
                        403 -> "Access denied. This content may not be available in your region."
                        404 -> "Content not found. This video may have been removed."
                        429 -> "Too many requests. Please wait a moment and try again."
                        else -> "Server error. Please try again later."
                    }
                is PlayerError.FormatError -> "This video format is not supported on your device."
                is PlayerError.DecoderError -> "Your device cannot play this video quality. Try a lower quality."
                is PlayerError.DrmError -> "This content is protected and cannot be played."
                is PlayerError.UnknownError -> "Something went wrong. Please try again."
            }
        }

        fun getRecommendedAction(error: PlayerError): String {
            return when (error) {
                is PlayerError.NetworkError -> "Check connection"
                is PlayerError.HttpError -> if (error.retryable) "Retry" else "Go back"
                is PlayerError.FormatError -> "Choose different format"
                is PlayerError.DecoderError -> "Try lower quality"
                is PlayerError.DrmError -> "Contact support"
                is PlayerError.UnknownError -> "Retry"
            }
        }
    }

sealed class PlayerError(
    open val message: String,
    open val originalException: Exception?,
    open val retryable: Boolean,
    open val timestamp: Long = System.currentTimeMillis(),
) {
    data class NetworkError(
        override val message: String,
        override val originalException: Exception?,
        override val retryable: Boolean = true,
    ) : PlayerError(message, originalException, retryable)

    data class HttpError(
        override val message: String,
        val responseCode: Int,
        override val originalException: Exception?,
        override val retryable: Boolean,
    ) : PlayerError(message, originalException, retryable)

    data class FormatError(
        override val message: String,
        override val originalException: Exception?,
        override val retryable: Boolean = false,
    ) : PlayerError(message, originalException, retryable)

    data class DecoderError(
        override val message: String,
        override val originalException: Exception?,
        override val retryable: Boolean = false,
    ) : PlayerError(message, originalException, retryable)

    data class DrmError(
        override val message: String,
        override val originalException: Exception?,
        override val retryable: Boolean = false,
    ) : PlayerError(message, originalException, retryable)

    data class UnknownError(
        override val message: String,
        override val originalException: Exception?,
        override val retryable: Boolean = true,
    ) : PlayerError(message, originalException, retryable)
}
