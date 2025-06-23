package com.rdwatch.androidtv.core.reactive

import com.rdwatch.androidtv.core.error.AppException
import com.rdwatch.androidtv.core.error.ErrorHandler
import com.rdwatch.androidtv.core.error.toAppException
import com.rdwatch.androidtv.repository.base.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Custom throttle implementations
fun <T> Flow<T>.throttleFirst(windowDurationMillis: Long): Flow<T> = 
    debounce(windowDurationMillis)

fun <T> Flow<T>.throttleLatest(timeoutMs: Long): Flow<T> = 
    debounce(timeoutMs)

fun <T> Flow<T>.switchToIo(dispatcherProvider: DispatcherProvider): Flow<T> = 
    flowOn(dispatcherProvider.io)

fun <T> Flow<T>.switchToMain(dispatcherProvider: DispatcherProvider): Flow<T> = 
    flowOn(dispatcherProvider.main)

fun <T> Flow<T>.switchToDefault(dispatcherProvider: DispatcherProvider): Flow<T> = 
    flowOn(dispatcherProvider.default)

fun <T> Flow<T>.switchTo(dispatcher: CoroutineDispatcher): Flow<T> = 
    flowOn(dispatcher)

fun <T> Flow<T>.debounceSearch(timeoutMillis: Long = 300): Flow<T> = 
    debounce(timeoutMillis)

fun <T> Flow<T>.throttleClicks(windowDurationMillis: Long = 300): Flow<T> = 
    throttleFirst(windowDurationMillis)

fun <T> Flow<T>.throttleLatestWithTimeout(timeoutMs: Long = 1000): Flow<T> = 
    throttleLatest(timeoutMs)

fun <T> Flow<T>.timeoutWithDuration(duration: Duration = 30.seconds): Flow<T> = 
    timeout(duration)

fun <T> Flow<T>.distinctUntilChangedBy(keySelector: (T) -> Any?): Flow<T> = 
    distinctUntilChanged { old, new -> keySelector(old) == keySelector(new) }

fun <T> Flow<T>.filterNotNull(): Flow<T> = 
    filter { it != null }

fun <T> Flow<T>.onEachIndexed(action: suspend (index: Int, value: T) -> Unit): Flow<T> {
    var index = 0
    return onEach { value ->
        action(index++, value)
    }
}

fun <T> Flow<T>.retryWithBackoff(
    maxRetries: Int = 3,
    initialDelay: Duration = 1.seconds,
    maxDelay: Duration = 10.seconds,
    factor: Double = 2.0,
    shouldRetry: (Throwable) -> Boolean = { true }
): Flow<T> = retryWhen { cause, attempt ->
    if (attempt < maxRetries && shouldRetry(cause)) {
        val delay = (initialDelay * factor.pow(attempt.toInt())).coerceAtMost(maxDelay)
        kotlinx.coroutines.delay(delay)
        true
    } else {
        false
    }
}

fun <T> Flow<T>.retryOnNetworkError(maxRetries: Int = 3): Flow<T> = 
    retryWithBackoff(
        maxRetries = maxRetries,
        shouldRetry = { throwable ->
            throwable.toAppException() is AppException.NetworkException
        }
    )

fun <T> Flow<T>.handleErrors(
    errorHandler: ErrorHandler? = null,
    onError: (suspend FlowCollector<T>.(Throwable) -> Unit)? = null
): Flow<T> = catch { throwable ->
    if (onError != null) {
        onError(throwable)
    } else {
        val appException = throwable.toAppException()
        throw appException
    }
}

fun <T> Flow<Result<T>>.mapToData(): Flow<T?> = 
    map { result -> 
        when (result) {
            is Result.Success -> result.data
            else -> null
        }
    }

fun <T> Flow<Result<T>>.filterSuccess(): Flow<T> = 
    mapNotNull { result -> 
        when (result) {
            is Result.Success -> result.data
            else -> null
        }
    }

fun <T> Flow<Result<T>>.onFlowSuccess(action: suspend (T) -> Unit): Flow<Result<T>> = 
    onEach { result ->
        if (result is Result.Success) action(result.data)
    }

fun <T> Flow<Result<T>>.onFlowError(action: suspend (Throwable) -> Unit): Flow<Result<T>> = 
    onEach { result ->
        if (result is Result.Error) action(result.exception)
    }

fun <T> Flow<Result<T>>.onFlowLoading(action: suspend () -> Unit): Flow<Result<T>> = 
    onEach { result ->
        if (result is Result.Loading) action()
    }

fun <T> Flow<T>.asResult(): Flow<Result<T>> = 
    map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch { emit(Result.Error(it.toAppException())) }

private fun Double.pow(n: Int): Double {
    var result = 1.0
    repeat(n) { result *= this }
    return result
}