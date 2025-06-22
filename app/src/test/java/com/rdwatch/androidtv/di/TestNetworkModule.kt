package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.di.qualifiers.MainApi
import com.rdwatch.androidtv.di.qualifiers.PublicClient
import com.rdwatch.androidtv.network.ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Test module that replaces NetworkModule in tests.
 * Uses mock implementations and test-friendly configurations.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object TestNetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE // Disable logging in tests
        }
    }

    @Provides
    @Singleton
    @PublicClient
    fun provideTestOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(5, TimeUnit.SECONDS) // Shorter timeouts for tests
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    @MainApi
    fun provideTestRetrofit(
        @PublicClient okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://localhost:8080/") // Use localhost for tests
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideMockApiService(): ApiService {
        return mockk<ApiService>(relaxed = true)
    }
}