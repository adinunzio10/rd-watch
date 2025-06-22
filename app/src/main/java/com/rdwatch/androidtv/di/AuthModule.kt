package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.auth.TokenStorage
import com.rdwatch.androidtv.auth.TokenStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    
    @Binds
    abstract fun bindTokenStorage(impl: TokenStorageImpl): TokenStorage
}