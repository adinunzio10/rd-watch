package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.auth.DataStoreTokenStorage
import com.rdwatch.androidtv.auth.TokenStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for authentication-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    /**
     * Binds the DataStore-based token storage implementation
     */
    @Binds
    abstract fun bindTokenStorage(impl: DataStoreTokenStorage): TokenStorage
}
