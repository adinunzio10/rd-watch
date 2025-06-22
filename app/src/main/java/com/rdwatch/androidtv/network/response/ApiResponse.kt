package com.rdwatch.androidtv.network.response

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val exception: ApiException) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()
}

sealed class ApiException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    data class NetworkException(
        override val message: String = "Network connection error",
        override val cause: Throwable? = null
    ) : ApiException(message, cause)
    
    data class AuthException(
        override val message: String = "Authentication failed",
        val code: Int = 401
    ) : ApiException(message)
    
    data class HttpException(
        val code: Int,
        override val message: String,
        val body: String? = null
    ) : ApiException(message)
    
    data class ParseException(
        override val message: String = "Failed to parse response",
        override val cause: Throwable? = null
    ) : ApiException(message, cause)
    
    data class UnknownException(
        override val message: String = "An unknown error occurred",
        override val cause: Throwable? = null
    ) : ApiException(message, cause)
}

suspend fun <T> safeApiCall(
    apiCall: suspend () -> retrofit2.Response<T>
): ApiResponse<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            response.body()?.let {
                ApiResponse.Success(it)
            } ?: ApiResponse.Error(
                ApiException.ParseException("Response body is null")
            )
        } else {
            ApiResponse.Error(
                when (response.code()) {
                    401, 403 -> ApiException.AuthException(
                        message = response.message(),
                        code = response.code()
                    )
                    in 400..499 -> ApiException.HttpException(
                        code = response.code(),
                        message = response.message(),
                        body = response.errorBody()?.string()
                    )
                    in 500..599 -> ApiException.HttpException(
                        code = response.code(),
                        message = "Server error: ${response.message()}",
                        body = response.errorBody()?.string()
                    )
                    else -> ApiException.UnknownException(
                        message = "Unexpected response: ${response.code()} ${response.message()}"
                    )
                }
            )
        }
    } catch (e: Exception) {
        ApiResponse.Error(
            when (e) {
                is java.io.IOException -> ApiException.NetworkException(
                    message = e.message ?: "Network error occurred",
                    cause = e
                )
                is com.squareup.moshi.JsonDataException,
                is com.squareup.moshi.JsonEncodingException -> ApiException.ParseException(
                    message = e.message ?: "JSON parsing error",
                    cause = e
                )
                else -> ApiException.UnknownException(
                    message = e.message ?: "Unknown error occurred",
                    cause = e
                )
            }
        )
    }
}