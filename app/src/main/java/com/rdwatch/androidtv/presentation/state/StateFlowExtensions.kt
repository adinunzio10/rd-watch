package com.rdwatch.androidtv.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(minActiveState: Lifecycle.State = Lifecycle.State.STARTED): State<T> {
    return collectAsState()
}

@Composable
fun <T> Flow<T>.collectWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            this@collectWithLifecycle.collect(action)
        }
    }
}

fun <T, R> StateFlow<T>.mapState(transform: (T) -> R): Flow<R> {
    return this.map(transform).distinctUntilChanged()
}

fun <T> StateFlow<ViewState<T>>.mapToUiState(): Flow<UiState<T>> {
    return this.mapState { it.data }
}

fun <T> StateFlow<ViewState<T>>.mapToData(): Flow<T?> {
    return this.mapState { it.data.getDataOrNull() }
}

fun <T> StateFlow<ViewState<T>>.mapToLoading(): Flow<Boolean> {
    return this.mapState { it.isLoading }
}

fun <T> StateFlow<ViewState<T>>.mapToError(): Flow<Throwable?> {
    return this.mapState { it.data.getErrorOrNull() }
}
