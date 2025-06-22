package com.rdwatch.androidtv.di

import android.content.Context
import com.rdwatch.androidtv.player.ExoPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    
    @Provides
    @Singleton
    fun provideExoPlayerManager(
        @ApplicationContext context: Context
    ): ExoPlayerManager {
        return ExoPlayerManager(context)
    }
}