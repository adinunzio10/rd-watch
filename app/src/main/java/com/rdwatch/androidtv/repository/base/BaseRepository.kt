package com.rdwatch.androidtv.repository.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

interface BaseRepository<T, ID> {
    suspend fun getById(id: ID): Result<T>
    suspend fun getAll(): Result<List<T>>
    suspend fun insert(item: T): Result<T>
    suspend fun update(item: T): Result<T>
    suspend fun delete(id: ID): Result<Unit>
    suspend fun exists(id: ID): Result<Boolean>
    
    fun observeById(id: ID): Flow<Result<T>>
    fun observeAll(): Flow<Result<List<T>>>
}

abstract class BaseRepositoryImpl<T, ID> : BaseRepository<T, ID> {
    
    protected suspend fun <R> executeWithResult(block: suspend () -> R): Result<R> {
        return safeCall { block() }
    }
    
    protected fun <R> flowWithResult(block: suspend () -> R): Flow<Result<R>> = flow {
        emit(Result.Loading)
        emit(executeWithResult { block() })
    }
    
    protected fun <R> resultFlow(result: Result<R>): Flow<Result<R>> = flowOf(result)
    
    protected suspend fun handleDatabaseOperation(operation: suspend () -> Unit): Result<Unit> {
        return executeWithResult { operation() }
    }
    
    protected suspend fun <R> handleDatabaseQuery(query: suspend () -> R): Result<R> {
        return executeWithResult { query() }
    }
}