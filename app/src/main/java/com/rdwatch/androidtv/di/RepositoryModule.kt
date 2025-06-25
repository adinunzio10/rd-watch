package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.core.error.ErrorHandler
import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.repository.MovieRepository
import com.rdwatch.androidtv.repository.MovieRepositoryImpl
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.RealDebridContentRepositoryImpl
import com.rdwatch.androidtv.repository.TorrentRepository
import com.rdwatch.androidtv.repository.base.BaseRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(
        movieRepositoryImpl: MovieRepositoryImpl
    ): MovieRepository

    @Binds
    @Singleton
    abstract fun bindRealDebridContentRepository(
        realDebridContentRepositoryImpl: RealDebridContentRepositoryImpl
    ): RealDebridContentRepository

    companion object {
        @Provides
        @Singleton
        fun provideRepositoryDependencies(
            dispatcherProvider: DispatcherProvider,
            errorHandler: ErrorHandler
        ): RepositoryDependencies = RepositoryDependencies(
            dispatcherProvider = dispatcherProvider,
            errorHandler = errorHandler
        )
    }
}

data class RepositoryDependencies(
    val dispatcherProvider: DispatcherProvider,
    val errorHandler: ErrorHandler
)