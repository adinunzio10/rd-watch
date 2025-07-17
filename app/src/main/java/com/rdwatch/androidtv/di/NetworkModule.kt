package com.rdwatch.androidtv.di

import android.content.Context
import com.rdwatch.androidtv.BuildConfig
import com.rdwatch.androidtv.di.qualifiers.AuthenticatedClient
import com.rdwatch.androidtv.di.qualifiers.CachingClient
import com.rdwatch.androidtv.di.qualifiers.MainApi
import com.rdwatch.androidtv.di.qualifiers.OAuthApi
import com.rdwatch.androidtv.di.qualifiers.PublicClient
import com.rdwatch.androidtv.di.qualifiers.RealDebridApi
import com.rdwatch.androidtv.di.qualifiers.TMDbApi
import com.rdwatch.androidtv.network.ApiService
import com.rdwatch.androidtv.network.adapters.ApiResponseCallAdapterFactory
import com.rdwatch.androidtv.network.adapters.BooleanAdapter
import com.rdwatch.androidtv.network.adapters.DateAdapter
import com.rdwatch.androidtv.network.adapters.NullToEmptyStringAdapter
import com.rdwatch.androidtv.network.api.OAuth2ApiService
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.network.api.TMDbSearchService
import com.rdwatch.androidtv.network.api.TMDbTVService
import com.rdwatch.androidtv.network.interceptors.AuthInterceptor
import com.rdwatch.androidtv.network.interceptors.IPv4DnsResolver
import com.rdwatch.androidtv.network.interceptors.NetworkMonitoringInterceptor
import com.rdwatch.androidtv.network.interceptors.TMDbApiKeyInterceptor
import com.rdwatch.androidtv.network.interceptors.TokenAuthenticator
import com.rdwatch.androidtv.network.interceptors.TokenProvider
import com.rdwatch.androidtv.network.interceptors.TokenProviderImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    abstract fun bindTokenProvider(impl: TokenProviderImpl): TokenProvider

    companion object {
        @Provides
        @Singleton
        fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
            return HttpLoggingInterceptor().apply {
                level =
                    if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
            }
        }

        @Provides
        @Singleton
        @PublicClient
        fun providePublicOkHttpClient(
            loggingInterceptor: HttpLoggingInterceptor,
            monitoringInterceptor: NetworkMonitoringInterceptor,
            ipv4DnsResolver: IPv4DnsResolver,
        ): OkHttpClient {
            return OkHttpClient.Builder()
                .dns(ipv4DnsResolver) // Use IPv4 DNS resolver to avoid IPv6 issues
                .addInterceptor(monitoringInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(20, TimeUnit.SECONDS) // Reduced from 30s
                .readTimeout(20, TimeUnit.SECONDS) // Reduced from 30s
                .writeTimeout(20, TimeUnit.SECONDS) // Reduced from 30s
                .build()
        }

        @Provides
        @Singleton
        @AuthenticatedClient
        fun provideAuthenticatedOkHttpClient(
            loggingInterceptor: HttpLoggingInterceptor,
            authInterceptor: AuthInterceptor,
            tokenAuthenticator: TokenAuthenticator,
            monitoringInterceptor: NetworkMonitoringInterceptor,
        ): OkHttpClient {
            return OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(monitoringInterceptor)
                .addInterceptor(loggingInterceptor)
                .authenticator(tokenAuthenticator)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        @CachingClient
        fun provideCachingOkHttpClient(
            @ApplicationContext context: Context,
            loggingInterceptor: HttpLoggingInterceptor,
            monitoringInterceptor: NetworkMonitoringInterceptor,
        ): OkHttpClient {
            val cacheSize = 50 * 1024 * 1024L // 50 MB
            val cacheDir = File(context.cacheDir, "http_cache")
            val cache = Cache(cacheDir, cacheSize)

            return OkHttpClient.Builder()
                .addInterceptor(monitoringInterceptor)
                .addInterceptor(loggingInterceptor)
                .cache(cache)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideMoshi(): Moshi {
            return Moshi.Builder()
                .add(DateAdapter())
                .add(NullToEmptyStringAdapter())
                .add(BooleanAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()
        }

        @Provides
        @Singleton
        @MainApi
        fun provideMainRetrofit(
            @PublicClient okHttpClient: OkHttpClient,
            moshi: Moshi,
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(RealDebridApiService.BASE_URL)
                .client(okHttpClient)
                .addCallAdapterFactory(ApiResponseCallAdapterFactory())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        @Provides
        @Singleton
        fun provideApiService(
            @MainApi retrofit: Retrofit,
        ): ApiService {
            return retrofit.create(ApiService::class.java)
        }

        @Provides
        @Singleton
        @RealDebridApi
        fun provideRealDebridRetrofit(
            @AuthenticatedClient okHttpClient: OkHttpClient,
            moshi: Moshi,
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(RealDebridApiService.BASE_URL)
                .client(okHttpClient)
                .addCallAdapterFactory(ApiResponseCallAdapterFactory())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        @Provides
        @Singleton
        fun provideRealDebridApiService(
            @RealDebridApi retrofit: Retrofit,
        ): RealDebridApiService {
            return retrofit.create(RealDebridApiService::class.java)
        }

        @Provides
        @Singleton
        @OAuthApi
        fun provideOAuthRetrofit(
            @PublicClient okHttpClient: OkHttpClient,
            moshi: Moshi,
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(OAuth2ApiService.OAUTH_BASE_URL)
                .client(okHttpClient)
                .addCallAdapterFactory(ApiResponseCallAdapterFactory())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        @Provides
        @Singleton
        fun provideOAuth2ApiService(
            @OAuthApi retrofit: Retrofit,
        ): OAuth2ApiService {
            return retrofit.create(OAuth2ApiService::class.java)
        }

        @Provides
        @Singleton
        @TMDbApi
        fun provideTMDbOkHttpClient(
            @ApplicationContext context: Context,
            loggingInterceptor: HttpLoggingInterceptor,
            monitoringInterceptor: NetworkMonitoringInterceptor,
            tmdbApiKeyInterceptor: TMDbApiKeyInterceptor,
        ): OkHttpClient {
            val cacheSize = 20 * 1024 * 1024L // 20 MB cache for TMDb data
            val cacheDir = File(context.cacheDir, "tmdb_cache")
            val cache = Cache(cacheDir, cacheSize)

            return OkHttpClient.Builder()
                .addInterceptor(tmdbApiKeyInterceptor) // Add API key to all requests
                .addInterceptor(monitoringInterceptor)
                .addInterceptor(loggingInterceptor)
                .cache(cache)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        @TMDbApi
        fun provideTMDbRetrofit(
            @TMDbApi okHttpClient: OkHttpClient,
            moshi: Moshi,
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(TMDbMovieService.BASE_URL)
                .client(okHttpClient)
                .addCallAdapterFactory(ApiResponseCallAdapterFactory())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        @Provides
        @Singleton
        fun provideTMDbMovieService(
            @TMDbApi retrofit: Retrofit,
        ): TMDbMovieService {
            return retrofit.create(TMDbMovieService::class.java)
        }

        @Provides
        @Singleton
        fun provideTMDbTVService(
            @TMDbApi retrofit: Retrofit,
        ): TMDbTVService {
            return retrofit.create(TMDbTVService::class.java)
        }

        @Provides
        @Singleton
        fun provideTMDbSearchService(
            @TMDbApi retrofit: Retrofit,
        ): TMDbSearchService {
            return retrofit.create(TMDbSearchService::class.java)
        }
    }
}
