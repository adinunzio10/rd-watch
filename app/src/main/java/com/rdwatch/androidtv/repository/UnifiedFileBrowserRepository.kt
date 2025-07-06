package com.rdwatch.androidtv.repository

import androidx.paging.PagingData
import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.data.paging.*
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.repository.bulk.*
import com.rdwatch.androidtv.repository.cache.FileBrowserCacheManager
import com.rdwatch.androidtv.repository.filtering.FileFilteringService
import com.rdwatch.androidtv.repository.sorting.FileSortingService
import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified repository that integrates all File Browser components into a single,
 * easy-to-use API for the UI layer
 */
@Singleton
class UnifiedFileBrowserRepository @Inject constructor(
    private val fileBrowserRepository: FileBrowserRepository,
    private val cacheManager: FileBrowserCacheManager,
    private val dataProcessor: FileBrowserDataProcessor,
    private val sortingService: FileSortingService,
    private val filteringService: FileFilteringService,
    private val bulkOperationsCoordinator: BulkOperationsCoordinator,
    private val paginationStateManager: PaginationStateManager,
    private val enhancedPagingSourceFactory: EnhancedAccountFilesPagingSourceFactory,
    private val searchPagingSourceFactory: SearchPagingSourceFactory,
    private val dispatcherProvider: DispatcherProvider
) {
    
    // Expose bulk operations state
    val bulkOperationsState: Flow<BulkOperationsState> = bulkOperationsCoordinator.getCombinedState()
    val paginationState: StateFlow<PaginationState> = paginationStateManager.paginationState
    val paginationAnalytics: StateFlow<PaginationAnalytics> = paginationStateManager.paginationAnalytics
    
    /**
     * Get paginated account files with enhanced features
     */
    fun getAccountFilesPaged(
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        filter: FileFilterCriteria = FileFilterCriteria(),
        scenario: PaginationScenario = PaginationScenario.STANDARD,
        coroutineScope: CoroutineScope
    ): Flow<PagingData<AccountFileItem>> {
        return paginationStateManager.createPagingFlow(sortOption, filter, scenario, coroutineScope)
    }
    
    /**
     * Search files with pagination
     */
    fun searchFilesPaged(
        query: String,
        additionalFilter: FileFilterCriteria = FileFilterCriteria(),
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        useRanking: Boolean = true,
        coroutineScope: CoroutineScope
    ): Flow<PagingData<AccountFileItem>> {
        val config = PaginationConfig.getConfigForScenario(PaginationScenario.SEARCH)
        
        return androidx.paging.Pager(
            config = config,
            pagingSourceFactory = {
                searchPagingSourceFactory.create(query, additionalFilter, sortOption, useRanking)
            }
        ).flow.cachedIn(coroutineScope)
    }
    
    /**
     * Get account files without pagination (for smaller datasets)
     */
    fun getAccountFiles(
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        filter: FileFilterCriteria = FileFilterCriteria(),
        limit: Int? = null
    ): Flow<Result<ProcessedFileResult>> = flow {
        emit(Result.Loading)
        
        try {
            // Try to get from cache first
            val cachedFiles = cacheManager.getAccountFiles(filter).first()
            
            if (cachedFiles.isNotEmpty()) {
                val result = dataProcessor.processFiles(cachedFiles, sortOption, filter, limit)
                emit(Result.Success(result))
            } else {
                // Fetch from repository
                fileBrowserRepository.getAccountFiles(filter).collect { repositoryResult ->
                    when (repositoryResult) {
                        is Result.Loading -> emit(Result.Loading)
                        is Result.Error -> emit(repositoryResult)
                        is Result.Success -> {
                            val result = dataProcessor.processFiles(
                                repositoryResult.data, 
                                sortOption, 
                                filter, 
                                limit
                            )
                            emit(Result.Success(result))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(dispatcherProvider.io)
    
    /**
     * Search files with advanced processing
     */
    fun searchFiles(
        query: String,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        additionalFilter: FileFilterCriteria = FileFilterCriteria(),
        limit: Int? = null,
        useRanking: Boolean = true
    ): Flow<Result<ProcessedFileResult>> = flow {
        emit(Result.Loading)
        
        try {
            val allFiles = cacheManager.getAccountFiles().first()
            
            if (allFiles.isNotEmpty()) {
                val result = dataProcessor.searchFiles(
                    files = allFiles,
                    query = query,
                    sortOption = sortOption,
                    additionalFilter = additionalFilter,
                    limit = limit,
                    useRanking = useRanking
                )
                emit(Result.Success(result))
            } else {
                emit(Result.Success(ProcessedFileResult(
                    files = emptyList(),
                    totalCount = 0,
                    filteredCount = 0,
                    statistics = sortingService.getFilterStatistics(emptyList()),
                    processingTimeMs = 0
                )))
            }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(dispatcherProvider.io)
    
    /**
     * Get grouped files
     */
    fun getGroupedFiles(
        groupBy: GroupByOption,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        filter: FileFilterCriteria = FileFilterCriteria()
    ): Flow<Result<GroupedFileResult>> = flow {
        emit(Result.Loading)
        
        try {
            val allFiles = cacheManager.getAccountFiles().first()
            val result = dataProcessor.getGroupedFiles(allFiles, groupBy, sortOption, filter)
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(dispatcherProvider.io)
    
    /**
     * Get storage usage information
     */
    fun getStorageUsage(): Flow<Result<StorageUsageInfo>> {
        return fileBrowserRepository.getStorageUsage()
    }
    
    /**
     * Get file statistics
     */
    fun getFileStatistics(): Flow<Result<Map<FileTypeCategory, Pair<Int, Long>>>> {
        return fileBrowserRepository.getFileStatistics()
    }
    
    /**
     * Get filter suggestions
     */
    suspend fun getFilterSuggestions(): Result<FilterSuggestions> = withContext(dispatcherProvider.io) {
        try {
            val allFiles = cacheManager.getAccountFiles().first()
            val suggestions = dataProcessor.getFilterSuggestions(allFiles)
            Result.Success(suggestions)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Apply filter preset
     */
    fun applyFilterPreset(
        preset: FileFilteringService.FilterPreset,
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        limit: Int? = null
    ): Flow<Result<ProcessedFileResult>> = flow {
        emit(Result.Loading)
        
        try {
            val allFiles = cacheManager.getAccountFiles().first()
            val result = dataProcessor.applyFilterPreset(allFiles, preset, sortOption, limit)
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(dispatcherProvider.io)
    
    /**
     * Refresh all data
     */
    suspend fun refreshData(): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            // Clear cache
            cacheManager.clearAllCaches()
            
            // Refresh repository data
            val result = fileBrowserRepository.refreshFileData()
            
            // Refresh pagination
            paginationStateManager.refresh()
            
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Update files for bulk operations
     */
    fun updateFilesForBulkOperations(files: List<AccountFileItem>) {
        bulkOperationsCoordinator.updateAvailableFiles(files)
    }
    
    /**
     * Execute bulk operation
     */
    fun executeBulkOperation(
        operationType: BulkOperationType,
        files: List<AccountFileItem>? = null,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationProgress> {
        return if (files != null) {
            bulkOperationsCoordinator.executeOperationOnFiles(files, operationType, options)
        } else {
            bulkOperationsCoordinator.executeOperationOnSelected(operationType, options)
                ?: flowOf(BulkOperationProgress(
                    operationId = "empty",
                    operationType = operationType,
                    totalItems = 0,
                    completedItems = 0,
                    failedItems = 0,
                    currentItem = null,
                    isCompleted = true,
                    errors = listOf("No files selected")
                ))
        }
    }
    
    /**
     * Delete files with confirmation
     */
    fun deleteFiles(
        files: List<AccountFileItem>? = null,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationResult> {
        return bulkOperationsCoordinator.executeDeleteWithConfirmation(files, options)
    }
    
    /**
     * Generate download URLs
     */
    fun generateDownloadUrls(
        files: List<AccountFileItem>? = null,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationProgress> {
        return bulkOperationsCoordinator.executeDownload(files, options)
    }
    
    /**
     * Get streaming URLs for playable files
     */
    fun getStreamingUrls(
        files: List<AccountFileItem>? = null,
        options: BulkOperationOptions = BulkOperationOptions()
    ): Flow<BulkOperationProgress> {
        return bulkOperationsCoordinator.executePlay(files, options)
    }
    
    // Bulk Selection Methods
    
    fun toggleSelectionMode() = bulkOperationsCoordinator.toggleSelectionMode()
    fun toggleFileSelection(fileId: String) = bulkOperationsCoordinator.toggleFileSelection(fileId)
    fun selectAllFiles() = bulkOperationsCoordinator.selectAllFiles()
    fun clearSelection() = bulkOperationsCoordinator.clearSelection()
    fun exitSelectionMode() = bulkOperationsCoordinator.exitSelectionMode()
    fun quickSelectByType(fileType: FileTypeCategory) = bulkOperationsCoordinator.quickSelectByType(fileType)
    fun quickSelectBySource(source: FileSource) = bulkOperationsCoordinator.quickSelectBySource(source)
    fun quickSelectStreamableFiles() = bulkOperationsCoordinator.quickSelectStreamableFiles()
    fun quickSelectLargeFiles() = bulkOperationsCoordinator.quickSelectLargeFiles()
    fun invertSelection() = bulkOperationsCoordinator.invertSelection()
    
    fun getSelectionStatistics(): SelectionStatistics = bulkOperationsCoordinator.getSelectionStatistics()
    fun getAvailableOperations(): List<BulkOperationType> = bulkOperationsCoordinator.getAvailableOperations()
    fun getSuggestedSelectionActions(): List<SelectionAction> = bulkOperationsCoordinator.getSuggestedSelectionActions()
    
    // Pagination Methods
    
    fun updateFilter(
        newFilter: FileFilterCriteria,
        coroutineScope: CoroutineScope
    ): Flow<PagingData<AccountFileItem>>? {
        return paginationStateManager.updateFilter(newFilter, coroutineScope)
    }
    
    fun updateSortOption(
        newSortOption: FileEnhancedSortOption,
        coroutineScope: CoroutineScope
    ): Flow<PagingData<AccountFileItem>>? {
        return paginationStateManager.updateSortOption(newSortOption, coroutineScope)
    }
    
    fun switchPaginationScenario(
        newScenario: PaginationScenario,
        coroutineScope: CoroutineScope
    ): Flow<PagingData<AccountFileItem>>? {
        return paginationStateManager.switchScenario(newScenario, coroutineScope)
    }
    
    fun getPaginationPerformanceMetrics(): PaginationPerformanceMetrics {
        return paginationStateManager.getPerformanceMetrics()
    }
    
    // Utility Methods
    
    suspend fun getFileDetails(fileId: String, source: FileSource): Result<AccountFileItem?> {
        return fileBrowserRepository.getFileDetails(fileId, source)
    }
    
    suspend fun checkStreamableStatus(fileIds: List<String>): Result<Map<String, Boolean>> {
        return fileBrowserRepository.checkStreamableStatus(fileIds)
    }
    
    suspend fun getRecentFiles(limit: Int = 50): Result<List<AccountFileItem>> {
        return fileBrowserRepository.getRecentFiles(limit)
    }
    
    suspend fun getLargestFiles(limit: Int = 50): Result<List<AccountFileItem>> {
        return fileBrowserRepository.getLargestFiles(limit)
    }
    
    suspend fun clearCache() {
        cacheManager.clearAllCaches()
    }
    
    suspend fun cleanupStaleCache() {
        cacheManager.cleanupStaleCache()
    }
    
    /**
     * Get comprehensive repository status
     */
    suspend fun getRepositoryStatus(): RepositoryStatus = withContext(dispatcherProvider.io) {
        try {
            val allFiles = cacheManager.getAccountFiles().first()
            val statistics = sortingService.getFilterStatistics(allFiles)
            val selectionStats = getSelectionStatistics()
            val paginationMetrics = getPaginationPerformanceMetrics()
            val activeOperations = bulkOperationsCoordinator.getActiveOperations()
            
            RepositoryStatus(
                totalFiles = statistics.totalFiles,
                fileStatistics = statistics,
                selectionStatistics = selectionStats,
                paginationMetrics = paginationMetrics,
                activeOperationsCount = activeOperations.size,
                isCacheHealthy = allFiles.isNotEmpty(),
                lastUpdateTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            RepositoryStatus(
                totalFiles = 0,
                fileStatistics = sortingService.getFilterStatistics(emptyList()),
                selectionStatistics = SelectionStatistics(0, 0, 0f, 0L, emptyMap(), emptyMap(), 0, 0),
                paginationMetrics = getPaginationPerformanceMetrics(),
                activeOperationsCount = 0,
                isCacheHealthy = false,
                lastUpdateTime = System.currentTimeMillis(),
                error = e.message
            )
        }
    }
}

/**
 * Comprehensive status of the repository
 */
data class RepositoryStatus(
    val totalFiles: Int,
    val fileStatistics: FileFilterStatistics,
    val selectionStatistics: SelectionStatistics,
    val paginationMetrics: PaginationPerformanceMetrics,
    val activeOperationsCount: Int,
    val isCacheHealthy: Boolean,
    val lastUpdateTime: Long,
    val error: String? = null
) {
    val isHealthy: Boolean get() = error == null && isCacheHealthy
    val hasActiveOperations: Boolean get() = activeOperationsCount > 0
}