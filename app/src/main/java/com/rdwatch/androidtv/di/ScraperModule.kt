package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.di.qualifiers.PublicClient
import com.rdwatch.androidtv.scraper.cache.CacheConfig
import com.rdwatch.androidtv.scraper.cache.InMemoryManifestCache
import com.rdwatch.androidtv.scraper.cache.ManifestCache
import com.rdwatch.androidtv.scraper.repository.ManifestRepository
import com.rdwatch.androidtv.scraper.repository.ManifestRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScraperModule {

    @Binds
    @Singleton
    abstract fun bindManifestRepository(
        manifestRepositoryImpl: ManifestRepositoryImpl
    ): ManifestRepository

    @Binds
    @Singleton
    abstract fun bindManifestCache(
        inMemoryManifestCache: InMemoryManifestCache
    ): ManifestCache

    companion object {
        @Provides
        @Singleton
        fun provideCacheConfig(): CacheConfig = CacheConfig()
        
        @Provides
        @Singleton
        fun provideOkHttpClient(@PublicClient publicClient: OkHttpClient): OkHttpClient = publicClient
    }
}