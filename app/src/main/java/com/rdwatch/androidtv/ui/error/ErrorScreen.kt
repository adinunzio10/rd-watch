package com.rdwatch.androidtv.ui.error

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorScreen(
    message: String,
    canRetry: Boolean = true,
    onRetry: (() -> Unit)? = null,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ErrorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize error state
    LaunchedEffect(message, canRetry) {
        viewModel.setError(message, canRetry)
    }

    // Handle back navigation
    BackHandler(enabled = true) {
        onBackPressed()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Error icon
        Icon(
            imageVector =
                when (uiState.errorType) {
                    ErrorType.NETWORK -> Icons.Default.CloudOff
                    ErrorType.PLAYBACK -> Icons.Default.ErrorOutline
                    ErrorType.AUTHENTICATION -> Icons.Default.Lock
                    ErrorType.NOT_FOUND -> Icons.Default.SearchOff
                    ErrorType.UNKNOWN -> Icons.Default.Error
                },
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error title
        Text(
            text = getErrorTitle(uiState.errorType),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        Text(
            text = uiState.message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        uiState.details?.let { details ->
            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back button
            OutlinedButton(
                onClick = onBackPressed,
                modifier = Modifier.widthIn(min = 120.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go Back")
            }

            // Retry button (if applicable and retry action is provided)
            if (uiState.canRetry && onRetry != null) {
                Button(
                    onClick = {
                        viewModel.incrementRetryCount()
                        onRetry()
                    },
                    enabled = !uiState.isRetrying,
                    modifier = Modifier.widthIn(min = 120.dp),
                ) {
                    if (uiState.isRetrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.retryCount > 0) "Retry (${uiState.retryCount + 1})" else "Retry")
                }
            }
        }

        // Additional help or suggestions
        if (uiState.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Suggestions:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getErrorTitle(errorType: ErrorType): String {
    return when (errorType) {
        ErrorType.NETWORK -> "Network Error"
        ErrorType.PLAYBACK -> "Playback Error"
        ErrorType.AUTHENTICATION -> "Authentication Error"
        ErrorType.NOT_FOUND -> "Content Not Found"
        ErrorType.UNKNOWN -> "Something Went Wrong"
    }
}

@HiltViewModel
class ErrorViewModel
    @Inject
    constructor() : BaseViewModel<ErrorUiState>() {
        override fun createInitialState(): ErrorUiState {
            return ErrorUiState()
        }

        fun setError(
            message: String,
            canRetry: Boolean,
        ) {
            val errorType = categorizeError(message)
            val suggestions = generateSuggestions(errorType, message)
            val details = extractErrorDetails(message)

            updateState {
                copy(
                    message = message,
                    canRetry = canRetry,
                    errorType = errorType,
                    suggestions = suggestions,
                    details = details,
                )
            }
        }

        fun incrementRetryCount() {
            updateState {
                copy(
                    retryCount = retryCount + 1,
                    isRetrying = true,
                )
            }

            // Reset retry state after a short delay
            launchSafely {
                kotlinx.coroutines.delay(1000)
                updateState { copy(isRetrying = false) }
            }
        }

        private fun categorizeError(message: String): ErrorType {
            val lowerMessage = message.lowercase()

            return when {
                lowerMessage.contains("network") ||
                    lowerMessage.contains("connection") ||
                    lowerMessage.contains("timeout") -> ErrorType.NETWORK

                lowerMessage.contains("playback") ||
                    lowerMessage.contains("video") ||
                    lowerMessage.contains("audio") ||
                    lowerMessage.contains("codec") -> ErrorType.PLAYBACK

                lowerMessage.contains("auth") ||
                    lowerMessage.contains("login") ||
                    lowerMessage.contains("permission") -> ErrorType.AUTHENTICATION

                lowerMessage.contains("not found") ||
                    lowerMessage.contains("404") ||
                    lowerMessage.contains("missing") -> ErrorType.NOT_FOUND

                else -> ErrorType.UNKNOWN
            }
        }

        private fun generateSuggestions(
            errorType: ErrorType,
            message: String,
        ): List<String> {
            return when (errorType) {
                ErrorType.NETWORK ->
                    listOf(
                        "Check your internet connection",
                        "Try again in a few moments",
                        "Restart your router if the problem persists",
                    )

                ErrorType.PLAYBACK ->
                    listOf(
                        "Try a different video quality",
                        "Check if the content format is supported",
                        "Restart the app if the issue continues",
                    )

                ErrorType.AUTHENTICATION ->
                    listOf(
                        "Check your login credentials",
                        "Try signing out and signing back in",
                        "Contact support if you continue having issues",
                    )

                ErrorType.NOT_FOUND ->
                    listOf(
                        "The content might have been removed",
                        "Try searching for similar content",
                        "Check back later as content is updated regularly",
                    )

                ErrorType.UNKNOWN ->
                    listOf(
                        "Try refreshing the page",
                        "Restart the application",
                        "Contact support if the problem persists",
                    )
            }
        }

        private fun extractErrorDetails(message: String): String? {
            // Extract technical details from error message if present
            // This is a simplified implementation
            return if (message.contains("Exception:") || message.contains("Error:")) {
                message.substringAfter(":", "").trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
        }
    }

enum class ErrorType {
    NETWORK,
    PLAYBACK,
    AUTHENTICATION,
    NOT_FOUND,
    UNKNOWN,
}

data class ErrorUiState(
    val message: String = "",
    val canRetry: Boolean = true,
    val errorType: ErrorType = ErrorType.UNKNOWN,
    val suggestions: List<String> = emptyList(),
    val details: String? = null,
    val retryCount: Int = 0,
    val isRetrying: Boolean = false,
)
