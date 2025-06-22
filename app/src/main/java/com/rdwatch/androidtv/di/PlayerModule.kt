package com.rdwatch.androidtv.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.player.ExoPlayerManager
import com.rdwatch.androidtv.player.MediaSourceFactory
import com.rdwatch.androidtv.player.state.PlaybackStateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    
    @Provides
    @Singleton
    fun provideMediaSourceFactory(
        @ApplicationContext context: Context
    ): MediaSourceFactory {
        return MediaSourceFactory(context)
    }
    
    @Provides
    @Singleton
    fun providePlaybackStateRepository(
        @ApplicationContext context: Context
    ): PlaybackStateRepository {
        return PlaybackStateRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideExoPlayerManager(
        @ApplicationContext context: Context,
        mediaSourceFactory: MediaSourceFactory,
        stateRepository: PlaybackStateRepository
    ): ExoPlayerManager {
        return ExoPlayerManager(context, mediaSourceFactory, stateRepository)
    }
}