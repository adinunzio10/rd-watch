package com.rdwatch.androidtv.ui.common

/**
 * Base sealed class for UI states across the application
 * Provides common patterns for loading, success, and error states
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    
    /**
     * Check if the state is loading
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Check if the state is successful
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if the state is error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Get data if successful, null otherwise
     */
    val dataOrNull: T?
        get() = (this as? Success<T>)?.data
    
    /**
     * Get error message if error, null otherwise
     */
    val errorMessageOrNull: String?
        get() = (this as? Error)?.message
}

/**
 * Extension function to map successful data
 */
inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> {
    return when (this) {
        is UiState.Loading -> UiState.Loading
        is UiState.Success -> UiState.Success(transform(data))
        is UiState.Error -> this
    }
}

/**
 * Extension function to handle state changes
 */
inline fun <T> UiState<T>.onSuccess(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) {
        action(data)
    }
    return this
}

inline fun <T> UiState<T>.onError(action: (String, Throwable?) -> Unit): UiState<T> {
    if (this is UiState.Error) {
        action(message, throwable)
    }
    return this
}

inline fun <T> UiState<T>.onLoading(action: () -> Unit): UiState<T> {
    if (this is UiState.Loading) {
        action()
    }
    return this
}