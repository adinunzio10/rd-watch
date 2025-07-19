package com.rdwatch.androidtv.core.error

import android.content.Context
import com.rdwatch.androidtv.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorMessageProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun getUserFriendlyMessage(error: ErrorInfo): String {
            return when (error.type) {
                ErrorType.NETWORK ->
                    when {
                        error.message.contains("internet", ignoreCase = true) ->
                            getString(R.string.error_no_internet_description)
                        error.message.contains("timeout", ignoreCase = true) ->
                            getString(R.string.error_timeout_description)
                        else -> getString(R.string.error_network_description)
                    }
                ErrorType.API ->
                    when {
                        error.exception is AppException.ApiException -> {
                            when (error.exception.code) {
                                401 -> getString(R.string.error_unauthorized_description)
                                403 -> getString(R.string.error_forbidden_description)
                                404 -> getString(R.string.error_not_found_description)
                                429 -> getString(R.string.error_too_many_requests_description)
                                in 500..599 -> getString(R.string.error_server_description)
                                else -> error.message
                            }
                        }
                        else -> error.message
                    }
                ErrorType.DATABASE -> getString(R.string.error_database_description)
                ErrorType.VALIDATION -> error.message
                ErrorType.AUTHENTICATION -> getString(R.string.error_authentication_description)
                ErrorType.AUTHORIZATION -> getString(R.string.error_authorization_description)
                ErrorType.VIDEO_PLAYER -> getString(R.string.error_video_player_description)
                ErrorType.UNKNOWN -> getString(R.string.error_unknown_description)
            }
        }

        fun getRetryMessage(error: ErrorInfo): String? {
            return if (error.canRetry) {
                getString(R.string.error_retry_message)
            } else {
                null
            }
        }

        fun getActionButtonText(error: ErrorInfo): String {
            return when (error.type) {
                ErrorType.NETWORK -> getString(R.string.action_retry)
                ErrorType.API -> if (error.canRetry) getString(R.string.action_retry) else getString(R.string.action_ok)
                ErrorType.AUTHENTICATION -> getString(R.string.action_login)
                ErrorType.AUTHORIZATION -> getString(R.string.action_ok)
                else -> if (error.canRetry) getString(R.string.action_retry) else getString(R.string.action_ok)
            }
        }

        private fun getString(resId: Int): String {
            return try {
                context.getString(resId)
            } catch (e: Exception) {
                "Error"
            }
        }
    }
