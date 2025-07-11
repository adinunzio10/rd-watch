package com.rdwatch.androidtv.repository.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

abstract class NetworkBoundResource<ResultType, RequestType> {
    
    fun asFlow(): Flow<Result<ResultType>> = flow {
        emit(Result.Loading)
        
        val dbValue = loadFromDb().first()
        
        if (shouldFetch(dbValue)) {
            emit(Result.Loading)
            
            when (val apiResponse = safeCall { createCall() }) {
                is Result.Success -> {
                    android.util.Log.d("NetworkBoundResource", "API call success, saving data")
                    saveCallResult(apiResponse.data)
                    emitAll(loadFromDb().map { Result.Success(it) })
                }
                is Result.Error -> {
                    android.util.Log.e("NetworkBoundResource", "API call failed", apiResponse.exception)
                    onFetchFailed(apiResponse.exception)
                    emitAll(loadFromDb().map { Result.Success(it) })
                }
                is Result.Loading -> {
                    android.util.Log.d("NetworkBoundResource", "API call still loading")
                    emitAll(loadFromDb().map { Result.Success(it) })
                }
            }
        } else {
            emitAll(loadFromDb().map { Result.Success(it) })
        }
    }
    
    protected open fun onFetchFailed(throwable: Throwable) {}
    
    protected abstract fun loadFromDb(): Flow<ResultType>
    
    protected abstract fun shouldFetch(data: ResultType?): Boolean
    
    protected abstract suspend fun createCall(): RequestType
    
    protected abstract suspend fun saveCallResult(data: RequestType)
}

inline fun <ResultType, RequestType> networkBoundResource(
    crossinline loadFromDb: () -> Flow<ResultType>,
    crossinline shouldFetch: (ResultType?) -> Boolean,
    crossinline createCall: suspend () -> RequestType,
    crossinline saveCallResult: suspend (RequestType) -> Unit,
    crossinline onFetchFailed: (Throwable) -> Unit = {}
): Flow<Result<ResultType>> {
    return object : NetworkBoundResource<ResultType, RequestType>() {
        override fun loadFromDb(): Flow<ResultType> = loadFromDb()
        override fun shouldFetch(data: ResultType?): Boolean = shouldFetch(data)
        override suspend fun createCall(): RequestType = createCall()
        override suspend fun saveCallResult(data: RequestType) = saveCallResult(data)
        override fun onFetchFailed(throwable: Throwable) = onFetchFailed(throwable)
    }.asFlow()
}