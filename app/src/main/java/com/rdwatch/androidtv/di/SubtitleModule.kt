package com.rdwatch.androidtv.di

import android.content.Context
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.dao.SubtitleDao
import com.rdwatch.androidtv.di.qualifiers.CachingClient
import com.rdwatch.androidtv.di.qualifiers.PublicClient
import com.rdwatch.androidtv.player.subtitle.SubtitleRateLimiter
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiClient
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import com.rdwatch.androidtv.player.subtitle.cache.SubtitleCache
import com.rdwatch.androidtv.player.subtitle.ranking.SubtitleResultRanker
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Dependency injection module for the external subtitle integration system.
 *
 * Provides all necessary components for:
 * - Multiple subtitle API clients
 * - Caching and rate limiting
 * - Result ranking and coordination
 * - Repository pattern implementation
 *
 * Integrates with existing app architecture (Hilt, Room, OkHttp).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SubtitleModule {
    // ============ API Client Bindings ============

    /**
     * Bind all subtitle API clients into a set for orchestration.
     * Each provider is automatically included in the coordination system.
     */
    @Binds
    @IntoSet
    abstract fun bindSubdlApiClient(client: SubdlApiClient): SubtitleApiClient

    @Binds
    @IntoSet
    abstract fun bindSubDbApiClient(client: SubDbApiClient): SubtitleApiClient

    @Binds
    @IntoSet
    abstract fun bindPodnapisiApiClient(client: PodnapisiApiClient): SubtitleApiClient

    @Binds
    @IntoSet
    abstract fun bindAddic7edApiClient(client: Addic7edApiClient): SubtitleApiClient

    @Binds
    @IntoSet
    abstract fun bindLocalFilesApiClient(client: LocalFilesApiClient): SubtitleApiClient

    // ============ Repository Binding ============
    // Note: Repository will be implemented by other agents

    companion object {
        // ============ Database Components ============

        @Provides
        @Singleton
        fun provideSubtitleDao(database: AppDatabase): SubtitleDao {
            return database.subtitleDao()
        }

        // ============ Core Subtitle Components ============

        @Provides
        @Singleton
        fun provideSubtitleCache(
            @ApplicationContext context: Context,
            subtitleDao: SubtitleDao,
        ): SubtitleCache {
            return SubtitleCache(context, subtitleDao)
        }

        @Provides
        @Singleton
        fun provideSubtitleRateLimiter(): SubtitleRateLimiter {
            return SubtitleRateLimiter()
        }

        @Provides
        @Singleton
        fun provideSubtitleResultRanker(): SubtitleResultRanker {
            return SubtitleResultRanker()
        }

        // ============ Network Components for Subtitle APIs ============

        @Provides
        @Singleton
        @SubtitleApi
        fun provideSubtitleRetrofit(
            @PublicClient okHttpClient: OkHttpClient,
            moshi: Moshi,
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl("https://api.subdl.com/") // Default base URL, clients override as needed
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        @Provides
        @Singleton
        @SubtitleApi
        fun provideSubtitleCachingRetrofit(
            @CachingClient okHttpClient: OkHttpClient,
            moshi: Moshi,
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl("https://api.subdl.com/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        // ============ API Service Providers ============

        /**
         * Subdl API service for free subtitle downloads.
         */
        @Provides
        @Singleton
        fun provideSubdlApiService(
            @SubtitleApi retrofit: Retrofit,
        ): SubdlApiService {
            return retrofit.newBuilder()
                .baseUrl(SubtitleApiProvider.SUBDL.baseUrl + "/")
                .build()
                .create(SubdlApiService::class.java)
        }

        /**
         * SubDB API service for hash-based subtitle matching.
         */
        @Provides
        @Singleton
        fun provideSubDbApiService(
            @SubtitleApi retrofit: Retrofit,
        ): SubDbApiService {
            return retrofit.newBuilder()
                .baseUrl(SubtitleApiProvider.SUBDB.baseUrl + "/")
                .build()
                .create(SubDbApiService::class.java)
        }

        /**
         * Podnapisi API service for European subtitle content.
         */
        @Provides
        @Singleton
        fun providePodnapisiApiService(
            @SubtitleApi retrofit: Retrofit,
        ): PodnapisiApiService {
            return retrofit.newBuilder()
                .baseUrl(SubtitleApiProvider.PODNAPISI.baseUrl + "/")
                .build()
                .create(PodnapisiApiService::class.java)
        }

        /**
         * Addic7ed alternative API service for TV show subtitles.
         */
        @Provides
        @Singleton
        fun provideAddic7edApiService(
            @SubtitleApi retrofit: Retrofit,
        ): Addic7edApiService {
            return retrofit.newBuilder()
                .baseUrl(SubtitleApiProvider.ADDIC7ED_ALT.baseUrl + "/")
                .build()
                .create(Addic7edApiService::class.java)
        }

        // ============ Configuration and Settings ============

        /**
         * Provide subtitle search configuration.
         * This can be made configurable through user settings in the future.
         */
        @Provides
        @Singleton
        fun provideSubtitleSearchConfig(): SubtitleSearchConfig {
            return SubtitleSearchConfig(
                enableHashMatching = true,
                enableFuzzyMatching = true,
                maxResults = 20,
                timeoutMs = 10000,
                retryAttempts = 2,
                cacheExpirationHours = 24,
                preferredProviders =
                    listOf(
                        SubtitleApiProvider.SUBDB, // Hash-based, most accurate
                        SubtitleApiProvider.SUBDL, // Good API coverage
                        SubtitleApiProvider.PODNAPISI, // European content
                    ),
                excludedProviders = emptyList(),
                autoDownloadBest = false,
                hearingImpairedPreference = null,
            )
        }
    }
}

/**
 * Qualifier annotation for subtitle-specific Retrofit instances.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SubtitleApi

/**
 * Configuration class for subtitle search behavior.
 * This will be expanded to include user preferences in the future.
 */
data class SubtitleSearchConfig(
    val enableHashMatching: Boolean,
    val enableFuzzyMatching: Boolean,
    val maxResults: Int,
    val timeoutMs: Long,
    val retryAttempts: Int,
    val cacheExpirationHours: Int,
    val preferredProviders: List<SubtitleApiProvider>,
    val excludedProviders: List<SubtitleApiProvider>,
    val autoDownloadBest: Boolean,
    val hearingImpairedPreference: Boolean?,
)

// ============ Placeholder API Service Interfaces ============
// These will be implemented by other agents or in future iterations

/**
 * Retrofit interface for Subdl API.
 * Implementation will be created by API-specific agents.
 */
interface SubdlApiService {
    // API methods will be defined by implementation agents
}

/**
 * Retrofit interface for SubDB API.
 * Focuses on hash-based subtitle matching.
 */
interface SubDbApiService {
    // API methods will be defined by implementation agents
}

/**
 * Retrofit interface for Podnapisi API.
 * European subtitle database access.
 */
interface PodnapisiApiService {
    // API methods will be defined by implementation agents
}

/**
 * Retrofit interface for Addic7ed alternative API.
 * TV show subtitle specialization.
 */
interface Addic7edApiService {
    // API methods will be defined by implementation agents
}

// ============ Placeholder API Client Implementations ============
// These are stubs that other agents will implement

/**
 * Placeholder implementation for Subdl API client.
 * Will be implemented by specialized agents.
 */
@Singleton
class SubdlApiClient
    @Inject
    constructor() : SubtitleApiClient {
        override fun getProvider() = SubtitleApiProvider.SUBDL

        override fun isEnabled() = false // Disabled until implemented

        override suspend fun searchSubtitles(request: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest) =
            emptyList<com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult>()

        override suspend fun downloadSubtitle(result: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult) = ""

        override suspend fun testConnection() = false
    }

/**
 * Placeholder implementation for SubDB API client.
 */
@Singleton
class SubDbApiClient
    @Inject
    constructor() : SubtitleApiClient {
        override fun getProvider() = SubtitleApiProvider.SUBDB

        override fun isEnabled() = false

        override suspend fun searchSubtitles(request: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest) =
            emptyList<com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult>()

        override suspend fun downloadSubtitle(result: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult) = ""

        override suspend fun testConnection() = false
    }

/**
 * Placeholder implementation for Podnapisi API client.
 */
@Singleton
class PodnapisiApiClient
    @Inject
    constructor() : SubtitleApiClient {
        override fun getProvider() = SubtitleApiProvider.PODNAPISI

        override fun isEnabled() = false

        override suspend fun searchSubtitles(request: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest) =
            emptyList<com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult>()

        override suspend fun downloadSubtitle(result: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult) = ""

        override suspend fun testConnection() = false
    }

/**
 * Placeholder implementation for Addic7ed API client.
 */
@Singleton
class Addic7edApiClient
    @Inject
    constructor() : SubtitleApiClient {
        override fun getProvider() = SubtitleApiProvider.ADDIC7ED_ALT

        override fun isEnabled() = false

        override suspend fun searchSubtitles(request: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest) =
            emptyList<com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult>()

        override suspend fun downloadSubtitle(result: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult) = ""

        override suspend fun testConnection() = false
    }

/**
 * Implementation for local subtitle files.
 * This is fully functional and handles manually added subtitle files.
 */
@Singleton
class LocalFilesApiClient
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SubtitleApiClient {
        override fun getProvider() = SubtitleApiProvider.LOCAL_FILES

        override fun isEnabled() = true

        override suspend fun searchSubtitles(
            request: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest,
        ): List<com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult> {
            // Scan for local subtitle files
            // Implementation would search app's subtitle directory
            return emptyList()
        }

        override suspend fun downloadSubtitle(result: com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult): String {
            // For local files, this just returns the existing path
            return result.downloadUrl
        }

        override suspend fun testConnection() = true
    }
