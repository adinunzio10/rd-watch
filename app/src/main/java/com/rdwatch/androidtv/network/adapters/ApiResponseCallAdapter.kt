package com.rdwatch.androidtv.network.adapters

import com.rdwatch.androidtv.network.response.ApiException
import com.rdwatch.androidtv.network.response.ApiResponse
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class ApiResponseCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        // Check if the return type is Call
        if (getRawType(returnType) != Call::class.java) {
            return null
        }

        // Check if it's a parameterized type
        check(returnType is ParameterizedType) {
            "Call return type must be parameterized as Call<ApiResponse<T>>"
        }

        // Get the response type inside Call
        val responseType = getParameterUpperBound(0, returnType)

        // Check if the response type is ApiResponse
        if (getRawType(responseType) != ApiResponse::class.java) {
            return null
        }

        // Check if ApiResponse is parameterized
        check(responseType is ParameterizedType) {
            "ApiResponse must be parameterized as ApiResponse<T>"
        }

        // Get the actual data type inside ApiResponse
        val dataType = getParameterUpperBound(0, responseType)

        return ApiResponseCallAdapter<Any>(dataType)
    }
}

class ApiResponseCallAdapter<T>(
    private val responseType: Type,
) : CallAdapter<T, Call<ApiResponse<T>>> {
    override fun responseType(): Type = responseType

    override fun adapt(call: Call<T>): Call<ApiResponse<T>> {
        return ApiResponseCall(call)
    }
}

class ApiResponseCall<T>(
    private val delegate: Call<T>,
) : Call<ApiResponse<T>> {
    override fun enqueue(callback: retrofit2.Callback<ApiResponse<T>>) {
        delegate.enqueue(
            object : retrofit2.Callback<T> {
                override fun onResponse(
                    call: Call<T>,
                    response: retrofit2.Response<T>,
                ) {
                    val apiResponse =
                        if (response.isSuccessful) {
                            response.body()?.let {
                                ApiResponse.Success(it)
                            } ?: ApiResponse.Error(
                                ApiException.ParseException("Response body is null"),
                            )
                        } else {
                            ApiResponse.Error(
                                when (response.code()) {
                                    401, 403 ->
                                        ApiException.AuthException(
                                            message = response.message(),
                                            code = response.code(),
                                        )
                                    in 400..499 ->
                                        ApiException.HttpException(
                                            code = response.code(),
                                            message = response.message(),
                                            body = response.errorBody()?.string(),
                                        )
                                    in 500..599 ->
                                        ApiException.HttpException(
                                            code = response.code(),
                                            message = "Server error: ${response.message()}",
                                            body = response.errorBody()?.string(),
                                        )
                                    else ->
                                        ApiException.UnknownException(
                                            message = "Unexpected response: ${response.code()} ${response.message()}",
                                        )
                                },
                            )
                        }
                    callback.onResponse(this@ApiResponseCall, retrofit2.Response.success(apiResponse))
                }

                override fun onFailure(
                    call: Call<T>,
                    t: Throwable,
                ) {
                    val apiResponse =
                        ApiResponse.Error(
                            when (t) {
                                is java.io.IOException ->
                                    ApiException.NetworkException(
                                        message = t.message ?: "Network error occurred",
                                        cause = t,
                                    )
                                is com.squareup.moshi.JsonDataException,
                                is com.squareup.moshi.JsonEncodingException,
                                ->
                                    ApiException.ParseException(
                                        message = t.message ?: "JSON parsing error",
                                        cause = t,
                                    )
                                else ->
                                    ApiException.UnknownException(
                                        message = t.message ?: "Unknown error occurred",
                                        cause = t,
                                    )
                            },
                        )
                    callback.onResponse(this@ApiResponseCall, retrofit2.Response.success(apiResponse))
                }
            },
        )
    }

    override fun execute(): retrofit2.Response<ApiResponse<T>> {
        throw UnsupportedOperationException("ApiResponseCall doesn't support synchronous execution")
    }

    override fun clone(): Call<ApiResponse<T>> = ApiResponseCall(delegate.clone())

    override fun request() = delegate.request()

    override fun timeout() = delegate.timeout()

    override fun isExecuted() = delegate.isExecuted

    override fun isCanceled() = delegate.isCanceled

    override fun cancel() = delegate.cancel()
}
