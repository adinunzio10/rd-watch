package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.auth.DataStoreTokenStorage
import com.rdwatch.androidtv.auth.TokenStorage
import com.rdwatch.androidtv.auth.TokenStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

/**
 * Hilt module for authentication-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    
    /**
     * Binds the DataStore-based token storage implementation
     * This replaces the SharedPreferences-based implementation for better performance
     * and coroutine support
     */
    @Binds
    abstract fun bindTokenStorage(impl: DataStoreTokenStorage): TokenStorage
    
    /**
     * Legacy SharedPreferences implementation kept for backward compatibility
     * Remove this binding when migration is complete
     */
    @Binds
    @LegacyTokenStorage
    abstract fun bindLegacyTokenStorage(impl: TokenStorageImpl): TokenStorage
}

/**
 * Qualifier for the legacy SharedPreferences-based token storage
 * Used during migration period for backward compatibility
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LegacyTokenStorage