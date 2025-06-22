package com.rdwatch.androidtv.presentation.state

data class ViewState<T>(
    val data: UiState<T> = UiState.Loading,
    val isRefreshing: Boolean = false,
    val userMessage: String? = null,
    val showError: Boolean = false
) {
    val isLoading: Boolean
        get() = data is UiState.Loading && !isRefreshing
    
    val isRefreshingOrLoading: Boolean
        get() = isLoading || isRefreshing
    
    fun copyWithData(newData: UiState<T>): ViewState<T> = copy(data = newData)
    
    fun copyWithRefreshing(refreshing: Boolean): ViewState<T> = copy(isRefreshing = refreshing)
    
    fun copyWithUserMessage(message: String?): ViewState<T> = copy(userMessage = message)
    
    fun copyWithError(show: Boolean): ViewState<T> = copy(showError = show)
    
    companion object {
        fun <T> loading(): ViewState<T> = ViewState(data = UiState.Loading)
        
        fun <T> success(data: T): ViewState<T> = ViewState(data = UiState.Success(data))
        
        fun <T> error(
            exception: Throwable,
            message: String? = null,
            showError: Boolean = true
        ): ViewState<T> = ViewState(
            data = UiState.Error(exception, message),
            showError = showError
        )
        
        fun <T> empty(): ViewState<T> = ViewState(data = UiState.Empty)
    }
}