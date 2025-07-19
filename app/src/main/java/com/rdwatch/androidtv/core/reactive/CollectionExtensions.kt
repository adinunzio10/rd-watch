package com.rdwatch.androidtv.core.reactive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(
    initialValue: T,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return collectAsState(
        initial = initialValue,
        context = Dispatchers.Main.immediate,
    )
}

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(minActiveState: Lifecycle.State = Lifecycle.State.STARTED): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return collectAsState(
        context = Dispatchers.Main.immediate,
    )
}

@Composable
fun <T> Flow<T>.CollectWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(this, lifecycleOwner, minActiveState) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            this@CollectWithLifecycle.collect(action)
        }
    }
}

@Composable
fun <T> StateFlow<T>.CollectAsEffect(action: suspend (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val value by collectAsState()

    LaunchedEffect(value, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            action(value)
        }
    }
}

@Composable
fun <T> Flow<T>.CollectAsEffect(
    vararg keys: Any?,
    action: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(this, lifecycleOwner, *keys) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            this@CollectAsEffect.collect(action)
        }
    }
}

suspend fun <T> Flow<T>.collectOnLifecycle(
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit,
) {
    lifecycle.repeatOnLifecycle(minActiveState) {
        collect(action)
    }
}

suspend fun <T> Flow<T>.collectOnLifecycleIO(
    lifecycle: Lifecycle,
    dispatcherProvider: DispatcherProvider,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit,
) {
    lifecycle.repeatOnLifecycle(minActiveState) {
        collect { value ->
            withContext(dispatcherProvider.io) {
                action(value)
            }
        }
    }
}

suspend fun <T> Flow<T>.collectOnLifecycleMain(
    lifecycle: Lifecycle,
    dispatcherProvider: DispatcherProvider,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit,
) {
    lifecycle.repeatOnLifecycle(minActiveState) {
        collect { value ->
            withContext(dispatcherProvider.main) {
                action(value)
            }
        }
    }
}
