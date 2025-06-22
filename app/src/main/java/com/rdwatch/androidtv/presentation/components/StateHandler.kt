package com.rdwatch.androidtv.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rdwatch.androidtv.core.error.ErrorInfo
import com.rdwatch.androidtv.presentation.state.UiState

@Composable
fun <T> StateHandler(
    uiState: UiState<T>,
    onRetry: (() -> Unit)? = null,
    onErrorDismiss: (() -> Unit)? = null,
    loadingMessage: String? = null,
    emptyTitle: String = "No data available",
    emptyDescription: String? = null,
    emptyActionText: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable (data: T) -> Unit
) {
    when (uiState) {
        is UiState.Loading -> {
            LoadingState(
                message = loadingMessage,
                modifier = modifier
            )
        }
        
        is UiState.Success -> {
            content(uiState.data)
        }
        
        is UiState.Error -> {
            ErrorState(
                errorInfo = ErrorInfo(
                    type = com.rdwatch.androidtv.core.error.ErrorType.UNKNOWN,
                    message = uiState.message ?: uiState.exception.message ?: "An error occurred",
                    canRetry = onRetry != null,
                    exception = com.rdwatch.androidtv.core.error.toAppException(uiState.exception)
                ),
                onRetry = onRetry,
                onDismiss = onErrorDismiss,
                modifier = modifier
            )
        }
        
        is UiState.Empty -> {
            EmptyState(
                title = emptyTitle,
                description = emptyDescription,
                actionText = emptyActionText,
                onAction = onEmptyAction,
                modifier = modifier
            )
        }
    }
}

@Composable
fun <T> StateHandler(
    uiState: UiState<T>,
    errorInfo: ErrorInfo? = null,
    onRetry: (() -> Unit)? = null,
    onErrorDismiss: (() -> Unit)? = null,
    loadingMessage: String? = null,
    emptyTitle: String = "No data available",
    emptyDescription: String? = null,
    emptyActionText: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable (data: T) -> Unit
) {
    when (uiState) {
        is UiState.Loading -> {
            LoadingState(
                message = loadingMessage,
                modifier = modifier
            )
        }
        
        is UiState.Success -> {
            content(uiState.data)
        }
        
        is UiState.Error -> {
            ErrorState(
                errorInfo = errorInfo ?: ErrorInfo(
                    type = com.rdwatch.androidtv.core.error.ErrorType.UNKNOWN,
                    message = uiState.message ?: uiState.exception.message ?: "An error occurred",
                    canRetry = onRetry != null,
                    exception = com.rdwatch.androidtv.core.error.toAppException(uiState.exception)
                ),
                onRetry = onRetry,
                onDismiss = onErrorDismiss,
                modifier = modifier
            )
        }
        
        is UiState.Empty -> {
            EmptyState(
                title = emptyTitle,
                description = emptyDescription,
                actionText = emptyActionText,
                onAction = onEmptyAction,
                modifier = modifier
            )
        }
    }
}