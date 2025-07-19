package com.rdwatch.androidtv.core.error

import java.io.IOException

sealed class AppException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data class NetworkException(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class ApiException(
        val code: Int,
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class DatabaseException(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class ValidationException(
        val field: String,
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class AuthenticationException(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class AuthorizationException(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class VideoPlayerException(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class UnknownException(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppException(message, cause)
}

fun Throwable.toAppException(): AppException {
    return when (this) {
        is AppException -> this
        is IOException ->
            AppException.NetworkException(
                message = message ?: "Network error occurred",
                cause = this,
            )
        is SecurityException ->
            AppException.AuthorizationException(
                message = message ?: "Access denied",
                cause = this,
            )
        is IllegalArgumentException ->
            AppException.ValidationException(
                field = "unknown",
                message = message ?: "Invalid input",
                cause = this,
            )
        else ->
            AppException.UnknownException(
                message = message ?: "An unexpected error occurred",
                cause = this,
            )
    }
}
