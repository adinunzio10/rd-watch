package com.rdwatch.androidtv.di

import android.content.Context
import androidx.room.Room
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.MovieDao
import com.rdwatch.androidtv.data.dao.*
import com.rdwatch.androidtv.di.qualifiers.MainDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    @MainDatabase
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(*com.rdwatch.androidtv.data.migrations.Migrations.ALL_MIGRATIONS)
            .fallbackToDestructiveMigration() // For development only - remove in production
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieDao(@MainDatabase database: AppDatabase): MovieDao {
        return database.movieDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(@MainDatabase database: AppDatabase): UserDao {
        return database.userDao()
    }


    @Provides
    @Singleton
    fun provideWatchProgressDao(@MainDatabase database: AppDatabase): WatchProgressDao {
        return database.watchProgressDao()
    }

    @Provides
    @Singleton
    fun provideLibraryDao(@MainDatabase database: AppDatabase): LibraryDao {
        return database.libraryDao()
    }

    @Provides
    @Singleton
    fun provideScraperManifestDao(@MainDatabase database: AppDatabase): ScraperManifestDao {
        return database.scraperManifestDao()
    }

    @Provides
    @Singleton
    fun provideSearchHistoryDao(@MainDatabase database: AppDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    @Singleton
    fun provideRelationshipDao(@MainDatabase database: AppDatabase): RelationshipDao {
        return database.relationshipDao()
    }

    @Provides
    @Singleton
    fun provideContentDao(@MainDatabase database: AppDatabase): ContentDao {
        return database.contentDao()
    }

    @Provides
    @Singleton
    fun provideTorrentDao(@MainDatabase database: AppDatabase): TorrentDao {
        return database.torrentDao()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(@MainDatabase database: AppDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    @Singleton
    fun provideFileHashDao(@MainDatabase database: AppDatabase): FileHashDao {
        return database.fileHashDao()
    }
}