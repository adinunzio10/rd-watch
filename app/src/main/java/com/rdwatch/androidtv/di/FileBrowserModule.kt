package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.core.error.ErrorHandler
import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.ui.filebrowser.repository.FileBrowserRepository
import com.rdwatch.androidtv.ui.filebrowser.repository.RealDebridFileBrowserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Dagger module for File Browser feature
 * Provides dependencies for the file browser functionality
 */
@Module
@InstallIn(ViewModelComponent::class)
object FileBrowserModule {
    /**
     * Provides FileBrowserRepository
     * Currently only supports Real-Debrid, but structured for future expansion
     */
    @Provides
    @ViewModelScoped
    fun provideFileBrowserRepository(
        realDebridApiService: RealDebridApiService,
        realDebridContentRepository: RealDebridContentRepository,
        dispatcherProvider: DispatcherProvider,
        errorHandler: ErrorHandler,
    ): FileBrowserRepository {
        // Default to Real-Debrid for now
        // In the future, this could be based on user preferences or multiple implementations
        return RealDebridFileBrowserRepository(
            apiService = realDebridApiService,
            contentRepository = realDebridContentRepository,
            dispatcherProvider = dispatcherProvider,
            errorHandler = errorHandler,
        )
    }
}
