package com.rdwatch.androidtv.di

import android.content.Context
import com.rdwatch.androidtv.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for Settings-related components.
 * Provides singleton instances of settings repositories and related services.
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    
    /**
     * Provides singleton instance of SettingsRepository
     */
    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }
}