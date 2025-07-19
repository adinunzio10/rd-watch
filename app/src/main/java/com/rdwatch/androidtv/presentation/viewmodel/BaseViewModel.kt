package com.rdwatch.androidtv.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Enhanced BaseViewModel with state persistence, debugging, and error handling
 */
abstract class BaseViewModel<UiState> : ViewModel() {
    protected abstract fun createInitialState(): UiState

    // Optional SavedStateHandle for state persistence
    protected open val savedStateHandle: SavedStateHandle? = null

    // Unique identifier for this ViewModel instance
    protected val viewModelId: String = UUID.randomUUID().toString()

    // State management
    private val _uiState: MutableStateFlow<UiState> by lazy {
        MutableStateFlow(restoreState() ?: createInitialState())
    }
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Debug mode tracking
    private val _debugInfo = MutableStateFlow(DebugInfo())
    val debugInfo: StateFlow<DebugInfo> = _debugInfo.asStateFlow()

    // State history for debugging (only in debug builds)
    private val stateHistory = mutableListOf<StateHistoryEntry<UiState>>()
    private val maxHistorySize = 50

    // Error handling
    private val exceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            logError(exception)
            handleError(exception)
        }

    protected val safeViewModelScope: CoroutineScope
        get() = CoroutineScope(viewModelScope.coroutineContext + exceptionHandler)

    init {
        logDebug("ViewModel initialized: ${this::class.simpleName}")
        updateDebugInfo { copy(initializationTime = System.currentTimeMillis()) }
    }

    /**
     * Update state with automatic persistence and debugging
     */
    protected fun updateState(newState: UiState) {
        val oldState = _uiState.value
        _uiState.value = newState

        // Save state for persistence
        saveState(newState)

        // Update debug information
        updateDebugInfo {
            copy(
                lastStateUpdate = System.currentTimeMillis(),
                stateUpdateCount = stateUpdateCount + 1,
            )
        }

        // Add to history in debug mode
        addToStateHistory(oldState, newState)

        logDebug("State updated in ${this::class.simpleName}")
    }

    /**
     * Update state using a reducer function
     */
    protected fun updateState(reducer: UiState.() -> UiState) {
        updateState(_uiState.value.reducer())
    }

    /**
     * Safe coroutine launching with error handling
     */
    protected fun launchSafely(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        val errorHandler =
            if (onError != null) {
                CoroutineExceptionHandler { _, exception ->
                    logError(exception)
                    onError(exception)
                }
            } else {
                exceptionHandler
            }

        viewModelScope.launch(errorHandler) {
            updateDebugInfo { copy(activeCoroutines = activeCoroutines + 1) }

            try {
                block()
            } finally {
                updateDebugInfo { copy(activeCoroutines = maxOf(0, activeCoroutines - 1)) }
            }
        }
    }

    /**
     * Handle errors - can be overridden in subclasses
     */
    protected open fun handleError(exception: Throwable) {
        updateDebugInfo {
            copy(
                lastError = exception.message,
                errorCount = errorCount + 1,
            )
        }
        logError(exception)
    }

    /**
     * Save state to SavedStateHandle for persistence
     */
    protected open fun saveState(state: UiState) {
        savedStateHandle?.let { handle ->
            try {
                // Only save if the state is serializable
                if (isStateSerializable()) {
                    val stateKey = "${this::class.simpleName}_state"
                    handle[stateKey] = serializeState(state)
                }
            } catch (e: Exception) {
                logError(e, "Failed to save state")
            }
        }
    }

    /**
     * Restore state from SavedStateHandle
     */
    protected open fun restoreState(): UiState? {
        return savedStateHandle?.let { handle ->
            try {
                val stateKey = "${this::class.simpleName}_state"
                val serializedState = handle.get<String>(stateKey)
                serializedState?.let { deserializeState(it) }
            } catch (e: Exception) {
                logError(e, "Failed to restore state")
                null
            }
        }
    }

    /**
     * Check if the state is serializable for persistence
     */
    protected open fun isStateSerializable(): Boolean = false

    /**
     * Serialize state to string - override for custom serialization
     */
    protected open fun serializeState(state: UiState): String {
        throw UnsupportedOperationException("State serialization not implemented")
    }

    /**
     * Deserialize state from string - override for custom deserialization
     */
    protected open fun deserializeState(serializedState: String): UiState {
        throw UnsupportedOperationException("State deserialization not implemented")
    }

    /**
     * Clear saved state
     */
    protected fun clearSavedState() {
        savedStateHandle?.let { handle ->
            val stateKey = "${this::class.simpleName}_state"
            handle.remove<String>(stateKey)
        }
    }

    /**
     * Get current state value
     */
    protected fun getCurrentState(): UiState = _uiState.value

    /**
     * Debug methods
     */
    protected fun logDebug(message: String) {
        if (isDebugMode()) {
            println("DEBUG [${this::class.simpleName}]: $message")
        }
    }

    protected fun logError(
        exception: Throwable,
        message: String? = null,
    ) {
        val errorMessage = message ?: "Error in ${this::class.simpleName}"
        println("ERROR [$errorMessage]: ${exception.message}")
        exception.printStackTrace()
    }

    protected fun isDebugMode(): Boolean {
        // In a real app, this would check BuildConfig.DEBUG or a debug setting
        return true // For now, always debug
    }

    private fun updateDebugInfo(updater: DebugInfo.() -> DebugInfo) {
        _debugInfo.value = _debugInfo.value.updater()
    }

    private fun addToStateHistory(
        oldState: UiState,
        newState: UiState,
    ) {
        if (isDebugMode()) {
            stateHistory.add(
                StateHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    fromState = oldState,
                    toState = newState,
                ),
            )

            // Limit history size
            if (stateHistory.size > maxHistorySize) {
                stateHistory.removeAt(0)
            }
        }
    }

    /**
     * Get state history for debugging
     */
    fun getStateHistory(): List<StateHistoryEntry<UiState>> {
        return if (isDebugMode()) stateHistory.toList() else emptyList()
    }

    /**
     * Reset state to initial value
     */
    protected fun resetState() {
        updateState(createInitialState())
        clearSavedState()
        logDebug("State reset to initial value")
    }

    /**
     * Validate current state - override for custom validation
     */
    protected open fun validateState(state: UiState): ValidationResult {
        return ValidationResult.Valid
    }

    /**
     * Get validation result for current state
     */
    fun getCurrentStateValidation(): ValidationResult {
        return validateState(getCurrentState())
    }

    override fun onCleared() {
        super.onCleared()
        updateDebugInfo { copy(clearedTime = System.currentTimeMillis()) }
        logDebug("ViewModel cleared")
    }
}

/**
 * Debug information for ViewModels
 */
@Serializable
data class DebugInfo(
    val initializationTime: Long = 0L,
    val lastStateUpdate: Long = 0L,
    val stateUpdateCount: Int = 0,
    val activeCoroutines: Int = 0,
    val errorCount: Int = 0,
    val lastError: String? = null,
    val clearedTime: Long = 0L,
)

/**
 * State history entry for debugging
 */
data class StateHistoryEntry<T>(
    val timestamp: Long,
    val fromState: T,
    val toState: T,
)

/**
 * State validation result
 */
sealed class ValidationResult {
    object Valid : ValidationResult()

    data class Invalid(val errorMessages: List<String>) : ValidationResult()

    fun isValid(): Boolean = this is Valid

    fun getErrors(): List<String> =
        when (this) {
            is Invalid -> errorMessages
            is Valid -> emptyList()
        }
}
