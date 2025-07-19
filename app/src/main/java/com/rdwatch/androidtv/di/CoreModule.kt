package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.core.error.RetryHandler
import com.rdwatch.androidtv.core.reactive.DefaultDispatcherProvider
import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(defaultDispatcherProvider: DefaultDispatcherProvider): DispatcherProvider

    companion object {
        @Provides
        @Singleton
        fun provideRetryHandler(): RetryHandler = RetryHandler()
    }
}
