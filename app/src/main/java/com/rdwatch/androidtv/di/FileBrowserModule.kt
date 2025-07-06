package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.dao.*
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.repository.*
import com.rdwatch.androidtv.repository.cache.FileBrowserCacheManager
import com.rdwatch.androidtv.repository.filtering.FileFilteringService
import com.rdwatch.androidtv.repository.sorting.FileSortingService
import com.rdwatch.androidtv.repository.bulk.*
import com.rdwatch.androidtv.data.paging.*
import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for File Browser components
 */
@Module
@InstallIn(SingletonComponent::class)
object FileBrowserModule {
    
    @Provides
    @Singleton
    fun provideAccountFileDao(database: AppDatabase): AccountFileDao {
        return database.accountFileDao()
    }
    
    @Provides
    @Singleton
    fun provideStorageUsageDao(database: AppDatabase): StorageUsageDao {
        return database.storageUsageDao()
    }
    
    @Provides
    @Singleton
    fun provideFileTypeStatsDao(database: AppDatabase): FileTypeStatsDao {
        return database.fileTypeStatsDao()
    }
    
    @Provides
    @Singleton
    fun provideFileBrowserPreferencesDao(database: AppDatabase): FileBrowserPreferencesDao {
        return database.fileBrowserPreferencesDao()
    }
    
    @Provides
    @Singleton
    fun provideFileBrowserCacheManager(
        accountFileDao: AccountFileDao,
        storageUsageDao: StorageUsageDao,
        fileTypeStatsDao: FileTypeStatsDao,
        fileBrowserPreferencesDao: FileBrowserPreferencesDao,
        dispatcherProvider: DispatcherProvider
    ): FileBrowserCacheManager {
        return FileBrowserCacheManager(
            accountFileDao,
            storageUsageDao,
            fileTypeStatsDao,
            fileBrowserPreferencesDao,
            dispatcherProvider
        )
    }
    
    @Provides
    @Singleton
    fun provideFileSortingService(): FileSortingService {
        return FileSortingService()
    }
    
    @Provides
    @Singleton
    fun provideFileFilteringService(): FileFilteringService {
        return FileFilteringService()
    }
    
    @Provides
    @Singleton
    fun provideFileBrowserDataProcessor(
        sortingService: FileSortingService,
        filteringService: FileFilteringService
    ): FileBrowserDataProcessor {
        return FileBrowserDataProcessor(sortingService, filteringService)
    }
    
    @Provides
    @Singleton
    fun provideBulkOperationsService(
        apiService: RealDebridApiService,
        cacheManager: FileBrowserCacheManager,
        dispatcherProvider: DispatcherProvider
    ): BulkOperationsService {
        return BulkOperationsService(apiService, cacheManager, dispatcherProvider)
    }
    
    @Provides
    @Singleton
    fun provideBulkSelectionManager(): BulkSelectionManager {
        return BulkSelectionManager()
    }
    
    @Provides
    @Singleton
    fun provideBulkOperationsCoordinator(
        bulkOperationsService: BulkOperationsService,
        bulkSelectionManager: BulkSelectionManager
    ): BulkOperationsCoordinator {
        return BulkOperationsCoordinator(bulkOperationsService, bulkSelectionManager)
    }
    
    @Provides
    @Singleton
    fun provideFileBrowserRepository(
        apiService: RealDebridApiService,
        torrentDao: TorrentDao,
        downloadDao: DownloadDao,
        dispatcherProvider: DispatcherProvider
    ): FileBrowserRepository {
        return FileBrowserRepositoryImpl(
            apiService,
            torrentDao,
            downloadDao,
            dispatcherProvider
        )
    }
    
    @Provides
    @Singleton
    fun provideEnhancedAccountFilesPagingSourceFactory(
        apiService: RealDebridApiService,
        dataProcessor: FileBrowserDataProcessor,
        cacheManager: FileBrowserCacheManager
    ): EnhancedAccountFilesPagingSourceFactory {
        return EnhancedAccountFilesPagingSourceFactory(
            apiService,
            dataProcessor,
            cacheManager
        )
    }
    
    @Provides
    @Singleton
    fun provideSearchPagingSourceFactory(
        dataProcessor: FileBrowserDataProcessor,
        cacheManager: FileBrowserCacheManager
    ): SearchPagingSourceFactory {
        return SearchPagingSourceFactory(dataProcessor, cacheManager)
    }
    
    @Provides
    @Singleton
    fun providePaginationStateManager(
        pagingSourceFactory: EnhancedAccountFilesPagingSourceFactory
    ): PaginationStateManager {
        return PaginationStateManager(pagingSourceFactory)
    }
}