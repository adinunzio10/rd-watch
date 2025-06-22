package com.rdwatch.androidtv.core.error

import android.content.Context
import com.rdwatch.androidtv.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun handleError(throwable: Throwable): ErrorInfo {
        return when (throwable) {
            is CancellationException -> throw throwable // Don't handle cancellation
            is AppException -> mapAppException(throwable)
            is HttpException -> mapHttpException(throwable)
            is IOException -> mapNetworkException(throwable)
            else -> ErrorInfo(
                type = ErrorType.UNKNOWN,
                message = throwable.message ?: getStringResource(R.string.error_unknown),
                canRetry = false,
                exception = throwable.toAppException()
            )
        }
    }
    
    private fun mapAppException(exception: AppException): ErrorInfo {
        return when (exception) {
            is AppException.NetworkException -> ErrorInfo(
                type = ErrorType.NETWORK,
                message = exception.message,
                canRetry = true,
                exception = exception
            )
            is AppException.ApiException -> ErrorInfo(
                type = ErrorType.API,
                message = exception.message,
                canRetry = exception.code in 500..599,
                exception = exception
            )
            is AppException.DatabaseException -> ErrorInfo(
                type = ErrorType.DATABASE,
                message = exception.message,
                canRetry = false,
                exception = exception
            )
            is AppException.ValidationException -> ErrorInfo(
                type = ErrorType.VALIDATION,
                message = exception.message,
                canRetry = false,
                exception = exception
            )
            is AppException.AuthenticationException -> ErrorInfo(
                type = ErrorType.AUTHENTICATION,
                message = exception.message,
                canRetry = false,
                exception = exception
            )
            is AppException.AuthorizationException -> ErrorInfo(
                type = ErrorType.AUTHORIZATION,
                message = exception.message,
                canRetry = false,
                exception = exception
            )
            is AppException.VideoPlayerException -> ErrorInfo(
                type = ErrorType.VIDEO_PLAYER,
                message = exception.message,
                canRetry = true,
                exception = exception
            )
            is AppException.UnknownException -> ErrorInfo(
                type = ErrorType.UNKNOWN,
                message = exception.message,
                canRetry = false,
                exception = exception
            )
        }
    }
    
    private fun mapHttpException(exception: HttpException): ErrorInfo {
        val message = when (exception.code()) {
            400 -> getStringResource(R.string.error_bad_request)
            401 -> getStringResource(R.string.error_unauthorized)
            403 -> getStringResource(R.string.error_forbidden)
            404 -> getStringResource(R.string.error_not_found)
            408 -> getStringResource(R.string.error_timeout)
            429 -> getStringResource(R.string.error_too_many_requests)
            in 500..599 -> getStringResource(R.string.error_server)
            else -> getStringResource(R.string.error_unknown)
        }
        
        return ErrorInfo(
            type = ErrorType.API,
            message = message,
            canRetry = exception.code() in 500..599 || exception.code() == 408,
            exception = AppException.ApiException(
                code = exception.code(),
                message = message,
                cause = exception
            )
        )
    }
    
    private fun mapNetworkException(exception: IOException): ErrorInfo {
        val message = when (exception) {
            is UnknownHostException -> getStringResource(R.string.error_no_internet)
            is SocketTimeoutException -> getStringResource(R.string.error_timeout)
            else -> getStringResource(R.string.error_network)
        }
        
        return ErrorInfo(
            type = ErrorType.NETWORK,
            message = message,
            canRetry = true,
            exception = AppException.NetworkException(
                message = message,
                cause = exception
            )
        )
    }
    
    private fun getStringResource(resId: Int): String {
        return try {
            context.getString(resId)
        } catch (e: Exception) {
            "An error occurred"
        }
    }
}

data class ErrorInfo(
    val type: ErrorType,
    val message: String,
    val canRetry: Boolean,
    val exception: AppException
)

enum class ErrorType {
    NETWORK,
    API,
    DATABASE,
    VALIDATION,
    AUTHENTICATION,
    AUTHORIZATION,
    VIDEO_PLAYER,
    UNKNOWN
}