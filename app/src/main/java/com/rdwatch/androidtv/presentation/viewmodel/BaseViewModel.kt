package com.rdwatch.androidtv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<UiState> : ViewModel() {
    
    protected abstract fun createInitialState(): UiState
    
    private val _uiState: MutableStateFlow<UiState> by lazy {
        MutableStateFlow(createInitialState())
    }
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    protected fun updateState(newState: UiState) {
        _uiState.value = newState
    }
    
    protected fun updateState(reducer: UiState.() -> UiState) {
        _uiState.value = _uiState.value.reducer()
    }
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }
    
    protected val safeViewModelScope: CoroutineScope
        get() = CoroutineScope(viewModelScope.coroutineContext + exceptionHandler)
    
    protected fun launchSafely(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val errorHandler = if (onError != null) {
            CoroutineExceptionHandler { _, exception ->
                onError(exception)
            }
        } else {
            exceptionHandler
        }
        
        viewModelScope.launch(errorHandler) {
            block()
        }
    }
    
    protected open fun handleError(exception: Throwable) {
        // Default error handling - can be overridden in subclasses
    }
}