package com.rdwatch.androidtv.di

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.MovieDao
import com.rdwatch.androidtv.di.qualifiers.MainDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test module that replaces DatabaseModule in tests.
 * Uses in-memory database for fast and isolated testing.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    @MainDatabase
    fun provideInMemoryDatabase(): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries() // Allow main thread queries for testing
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieDao(@MainDatabase database: AppDatabase): MovieDao {
        return database.movieDao()
    }
}