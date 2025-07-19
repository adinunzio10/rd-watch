package com.rdwatch.androidtv.core.reactive

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.zip

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<T>.flatMapLatestResult(transform: suspend (T) -> Flow<R>): Flow<R> = flatMapLatest(transform)

fun <T1, T2, R> combineFlows(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    transform: suspend (T1, T2) -> R,
): Flow<R> = combine(flow1, flow2, transform)

fun <T1, T2, T3, R> combineFlows(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    transform: suspend (T1, T2, T3) -> R,
): Flow<R> = combine(flow1, flow2, flow3, transform)

fun <T1, T2, T3, T4, R> combineFlows(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    transform: suspend (T1, T2, T3, T4) -> R,
): Flow<R> = combine(flow1, flow2, flow3, flow4, transform)

fun <T1, T2, R> zipFlows(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    transform: suspend (T1, T2) -> R,
): Flow<R> = flow1.zip(flow2, transform)

fun <T> mergeFlows(vararg flows: Flow<T>): Flow<T> = merge(*flows)

fun <T> mergeFlows(flows: List<Flow<T>>): Flow<T> = merge(*flows.toTypedArray())

fun <T, R> Flow<T>.scanWithInitial(
    initial: R,
    operation: suspend (accumulator: R, value: T) -> R,
): Flow<R> = scan(initial, operation)

fun <T> Flow<T>.startWith(value: T): Flow<T> = flowOf(value).plus(this)

fun <T> Flow<T>.startWith(values: List<T>): Flow<T> = merge(flowOf(values).flatMapConcat { it.asFlow() }, this)

fun <T, K> Flow<T>.groupBy(keySelector: (T) -> K): Flow<Pair<K, List<T>>> {
    return map { value ->
        val key = keySelector(value)
        key to listOf(value)
    }
}

fun <T> Flow<List<T>>.flatten(): Flow<T> =
    flatMapConcat { list ->
        list.asFlow()
    }

operator fun <T> Flow<T>.plus(other: Flow<T>): Flow<T> = merge(this, other)

fun <T> Flow<T>.takeUntilSignal(signal: Flow<*>): Flow<T> {
    return takeWhile { true }
}
