package com.rdwatch.androidtv.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Hilt module for ViewModel-scoped dependencies.
 *
 * Note: ViewModels annotated with @HiltViewModel are automatically handled by Hilt
 * and don't need manual binding here. This module is for providing ViewModel-scoped
 * dependencies and custom bindings when needed.
 *
 * Active ViewModels in the app (automatically handled by @HiltViewModel):
 * - AuthViewModel
 * - HomeViewModel
 * - BrowseViewModel
 * - SearchViewModel
 * - MovieDetailsViewModel
 * - TVDetailsViewModel
 * - SettingsViewModel
 * - ScraperSettingsViewModel
 * - ProfileViewModel
 * - AccountFileBrowserViewModel
 * - PlaybackViewModel
 * - MainViewModel
 */
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    /**
     * Provides IO dispatcher for ViewModel operations
     */
    @Provides
    @ViewModelScoped
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides Main dispatcher for ViewModel operations
     */
    @Provides
    @ViewModelScoped
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /**
     * Provides Default dispatcher for ViewModel operations
     */
    @Provides
    @ViewModelScoped
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

/**
 * Qualifier annotations for different dispatchers
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
