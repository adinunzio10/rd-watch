package com.rdwatch.androidtv.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.data.converters.Converters
import com.rdwatch.androidtv.data.dao.*
import com.rdwatch.androidtv.data.entities.*

@Database(
    entities = [
        Movie::class,
        UserEntity::class,
        WatchProgressEntity::class,
        LibraryEntity::class,
        ScraperManifestEntity::class,
        SearchHistoryEntity::class,
        ContentEntity::class,
        TorrentEntity::class,
        DownloadEntity::class,
        FileHashEntity::class,
        SubtitleCacheEntity::class,
        SubtitleResultEntity::class,
        SubtitleFileEntity::class,
        SubtitleProviderStatsEntity::class,
        // TMDb entities
        TMDbMovieEntity::class,
        TMDbTVEntity::class,
        TMDbSearchResultEntity::class,
        TMDbCreditsEntity::class,
        TMDbRecommendationEntity::class,
        TMDbImagesEntity::class,
        TMDbVideosEntity::class,
        TMDbGenreEntity::class,
        TMDbConfigEntity::class,
        TMDbEpisodeExternalIdsEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    abstract fun userDao(): UserDao

    abstract fun watchProgressDao(): WatchProgressDao

    abstract fun libraryDao(): LibraryDao

    abstract fun scraperManifestDao(): ScraperManifestDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun relationshipDao(): RelationshipDao

    abstract fun contentDao(): ContentDao

    abstract fun torrentDao(): TorrentDao

    abstract fun downloadDao(): DownloadDao

    abstract fun fileHashDao(): FileHashDao

    abstract fun subtitleDao(): SubtitleDao

    // TMDb DAOs
    abstract fun tmdbMovieDao(): TMDbMovieDao

    abstract fun tmdbTVDao(): TMDbTVDao

    abstract fun tmdbSearchDao(): TMDbSearchDao

    companion object {
        const val DATABASE_NAME = "rdwatch_database"
    }
}
