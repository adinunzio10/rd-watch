package com.rdwatch.androidtv.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.data.repository.PlaybackProgressRepository
import com.rdwatch.androidtv.player.ExoPlayerManager
import com.rdwatch.androidtv.player.MediaSourceFactory
import com.rdwatch.androidtv.player.error.PlayerErrorHandler
import com.rdwatch.androidtv.player.state.PlaybackStateRepository
import com.rdwatch.androidtv.player.subtitle.SubtitleErrorHandler
import com.rdwatch.androidtv.player.subtitle.SubtitleManager
import com.rdwatch.androidtv.player.subtitle.SubtitleStyleRepository
import com.rdwatch.androidtv.player.subtitle.SubtitleSynchronizer
import com.rdwatch.androidtv.player.subtitle.parser.AssParser
import com.rdwatch.androidtv.player.subtitle.parser.SrtParser
import com.rdwatch.androidtv.player.subtitle.parser.SubtitleParserFactory
import com.rdwatch.androidtv.player.subtitle.parser.VttParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// DataStore extension for subtitle preferences
val Context.subtitlePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "subtitle_preferences",
)

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    fun provideMediaSourceFactory(
        @ApplicationContext context: Context,
    ): MediaSourceFactory {
        return MediaSourceFactory(context)
    }

    @Provides
    @Singleton
    fun providePlaybackStateRepository(
        @ApplicationContext context: Context,
        playbackProgressRepository: PlaybackProgressRepository,
    ): PlaybackStateRepository {
        return PlaybackStateRepository(context, playbackProgressRepository)
    }

    @Provides
    @Singleton
    fun providePlayerErrorHandler(): PlayerErrorHandler {
        return PlayerErrorHandler()
    }

    @Provides
    @Singleton
    fun provideExoPlayerManager(
        @ApplicationContext context: Context,
        mediaSourceFactory: MediaSourceFactory,
        stateRepository: PlaybackStateRepository,
        errorHandler: PlayerErrorHandler,
        subtitleManager: SubtitleManager,
    ): ExoPlayerManager {
        return ExoPlayerManager(context, mediaSourceFactory, stateRepository, errorHandler, subtitleManager)
    }

    // Subtitle-related providers

    @Provides
    @Singleton
    fun provideSubtitlePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return context.subtitlePreferencesDataStore
    }

    @Provides
    @Singleton
    fun provideSrtParser(): SrtParser {
        return SrtParser()
    }

    @Provides
    @Singleton
    fun provideVttParser(): VttParser {
        return VttParser()
    }

    @Provides
    @Singleton
    fun provideAssParser(): AssParser {
        return AssParser()
    }

    @Provides
    @Singleton
    fun provideSubtitleParserFactory(
        srtParser: SrtParser,
        vttParser: VttParser,
        assParser: AssParser,
    ): SubtitleParserFactory {
        return SubtitleParserFactory(srtParser, vttParser, assParser)
    }

    @Provides
    @Singleton
    fun provideSubtitleSynchronizer(): SubtitleSynchronizer {
        return SubtitleSynchronizer()
    }

    @Provides
    @Singleton
    fun provideSubtitleErrorHandler(): SubtitleErrorHandler {
        return SubtitleErrorHandler()
    }

    @Provides
    @Singleton
    fun provideSubtitleStyleRepository(dataStore: DataStore<Preferences>): SubtitleStyleRepository {
        return SubtitleStyleRepository(dataStore)
    }

    @Provides
    @Singleton
    fun provideSubtitleManager(
        @ApplicationContext context: Context,
        subtitleParserFactory: SubtitleParserFactory,
        subtitleSynchronizer: SubtitleSynchronizer,
        styleRepository: SubtitleStyleRepository,
        errorHandler: SubtitleErrorHandler,
    ): SubtitleManager {
        return SubtitleManager(context, subtitleParserFactory, subtitleSynchronizer, styleRepository, errorHandler)
    }
}
