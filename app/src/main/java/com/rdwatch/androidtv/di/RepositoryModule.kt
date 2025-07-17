package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.core.error.ErrorHandler
import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.data.repository.TMDbMovieRepository
import com.rdwatch.androidtv.data.repository.TMDbMovieRepositoryImpl
import com.rdwatch.androidtv.data.repository.TMDbSearchRepository
import com.rdwatch.androidtv.data.repository.TMDbSearchRepositoryImpl
import com.rdwatch.androidtv.data.repository.TMDbTVRepository
import com.rdwatch.androidtv.data.repository.TMDbTVRepositoryImpl
import com.rdwatch.androidtv.repository.MovieRepository
import com.rdwatch.androidtv.repository.MovieRepositoryImpl
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.RealDebridContentRepositoryImpl
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
    abstract fun bindMovieRepository(movieRepositoryImpl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindRealDebridContentRepository(
        realDebridContentRepositoryImpl: RealDebridContentRepositoryImpl,
    ): RealDebridContentRepository

    @Binds
    @Singleton
    abstract fun bindTMDbMovieRepository(tmdbMovieRepositoryImpl: TMDbMovieRepositoryImpl): TMDbMovieRepository

    @Binds
    @Singleton
    abstract fun bindTMDbTVRepository(tmdbTVRepositoryImpl: TMDbTVRepositoryImpl): TMDbTVRepository

    @Binds
    @Singleton
    abstract fun bindTMDbSearchRepository(tmdbSearchRepositoryImpl: TMDbSearchRepositoryImpl): TMDbSearchRepository

    companion object {
        @Provides
        @Singleton
        fun provideRepositoryDependencies(
            dispatcherProvider: DispatcherProvider,
            errorHandler: ErrorHandler,
        ): RepositoryDependencies =
            RepositoryDependencies(
                dispatcherProvider = dispatcherProvider,
                errorHandler = errorHandler,
            )
    }
}

data class RepositoryDependencies(
    val dispatcherProvider: DispatcherProvider,
    val errorHandler: ErrorHandler,
)
