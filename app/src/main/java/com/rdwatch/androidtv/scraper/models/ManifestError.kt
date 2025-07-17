package com.rdwatch.androidtv.scraper.models

/**
 * Exception hierarchy for manifest-related errors
 */
sealed class ManifestException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class ManifestParsingException(
    message: String,
    cause: Throwable? = null,
    val url: String? = null,
    val format: String? = null,
) : ManifestException(message, cause)

class ManifestValidationException(
    message: String,
    cause: Throwable? = null,
    val validationErrors: List<ValidationError> = emptyList(),
) : ManifestException(message, cause)

class ManifestNetworkException(
    message: String,
    cause: Throwable? = null,
    val url: String? = null,
    val statusCode: Int? = null,
) : ManifestException(message, cause)

class ManifestStorageException(
    message: String,
    cause: Throwable? = null,
    val operation: String? = null,
) : ManifestException(message, cause)

class ManifestCacheException(
    message: String,
    cause: Throwable? = null,
    val cacheKey: String? = null,
) : ManifestException(message, cause)

/**
 * Detailed validation error information
 */
data class ValidationError(
    val field: String,
    val message: String,
    val value: Any? = null,
    val rule: String? = null,
    val severity: ValidationSeverity = ValidationSeverity.ERROR,
)

enum class ValidationSeverity {
    ERROR,
    WARNING,
    INFO,
}

/**
 * Result wrapper for manifest operations
 */
sealed class ManifestResult<out T> {
    data class Success<T>(val data: T) : ManifestResult<T>()

    data class Error(val exception: ManifestException) : ManifestResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): ManifestResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }

    inline fun <R> flatMap(transform: (T) -> ManifestResult<R>): ManifestResult<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> this
        }
    }

    inline fun onSuccess(action: (T) -> Unit): ManifestResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (ManifestException) -> Unit): ManifestResult<T> {
        if (this is Error) action(exception)
        return this
    }

    fun getOrNull(): T? = if (this is Success) data else null

    fun getOrThrow(): T =
        when (this) {
            is Success -> data
            is Error -> throw exception
        }
}
