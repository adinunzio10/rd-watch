package com.rdwatch.androidtv.data.paging

import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataDiffer
import androidx.paging.PagingSource
import com.rdwatch.androidtv.ui.filebrowser.models.AccountFileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Utility functions for pagination operations
 */
object PaginationUtils {
    
    /**
     * Check if pagination is in loading state
     */
    fun isLoading(loadState: LoadState): Boolean {
        return loadState is LoadState.Loading
    }
    
    /**
     * Check if pagination has an error
     */
    fun hasError(loadState: LoadState): Boolean {
        return loadState is LoadState.Error
    }
    
    /**
     * Get error message from load state
     */
    fun getErrorMessage(loadState: LoadState): String? {
        return when (loadState) {
            is LoadState.Error -> loadState.error.message
            else -> null
        }
    }
    
    /**
     * Check if pagination is refreshing
     */
    fun isRefreshing(loadStates: androidx.paging.CombinedLoadStates): Boolean {
        return loadStates.refresh is LoadState.Loading
    }
    
    /**
     * Check if pagination is appending (loading more items)
     */
    fun isAppending(loadStates: androidx.paging.CombinedLoadStates): Boolean {
        return loadStates.append is LoadState.Loading
    }
    
    /**
     * Check if pagination is prepending
     */
    fun isPrepending(loadStates: androidx.paging.CombinedLoadStates): Boolean {
        return loadStates.prepend is LoadState.Loading
    }
    
    /**
     * Check if pagination has reached the end
     */
    fun hasReachedEnd(loadStates: androidx.paging.CombinedLoadStates): Boolean {
        return loadStates.append is LoadState.NotLoading && loadStates.append.endOfPaginationReached
    }
    
    /**
     * Check if pagination has any error
     */
    fun hasAnyError(loadStates: androidx.paging.CombinedLoadStates): Boolean {
        return loadStates.refresh is LoadState.Error || 
               loadStates.append is LoadState.Error || 
               loadStates.prepend is LoadState.Error
    }
    
    /**
     * Get the first error found in load states
     */
    fun getFirstError(loadStates: androidx.paging.CombinedLoadStates): Throwable? {
        return when {
            loadStates.refresh is LoadState.Error -> (loadStates.refresh as LoadState.Error).error
            loadStates.append is LoadState.Error -> (loadStates.append as LoadState.Error).error
            loadStates.prepend is LoadState.Error -> (loadStates.prepend as LoadState.Error).error
            else -> null
        }
    }
    
    /**
     * Transform PagingData to add additional metadata
     */
    fun addMetadata(
        pagingData: Flow<PagingData<AccountFileItem>>,
        metadata: (AccountFileItem) -> AccountFileItem
    ): Flow<PagingData<AccountFileItem>> {
        return pagingData.map { data ->
            data.map { item -> metadata(item) }
        }
    }
    
    /**
     * Filter PagingData items
     */
    fun filter(
        pagingData: Flow<PagingData<AccountFileItem>>,
        predicate: (AccountFileItem) -> Boolean
    ): Flow<PagingData<AccountFileItem>> {
        return pagingData.map { data ->
            data.filter { item -> predicate(item) }
        }
    }
    
    /**
     * Calculate optimal page size based on screen dimensions and item size
     */
    fun calculateOptimalPageSize(
        screenWidth: Int,
        screenHeight: Int,
        itemWidth: Int,
        itemHeight: Int,
        columnsInGrid: Int = 1
    ): Int {
        val itemsPerRow = (screenWidth / itemWidth).coerceAtLeast(1)
        val rowsOnScreen = (screenHeight / itemHeight).coerceAtLeast(1)
        val itemsOnScreen = itemsPerRow * rowsOnScreen
        
        // Load 2-3 screens worth of items for smooth scrolling
        return (itemsOnScreen * 2.5).toInt().coerceAtLeast(10)
    }
    
    /**
     * Get pagination state summary
     */
    fun getPaginationStateSummary(loadStates: androidx.paging.CombinedLoadStates): PaginationStateSummary {
        return PaginationStateSummary(
            isInitialLoading = loadStates.refresh is LoadState.Loading,
            isLoadingMore = loadStates.append is LoadState.Loading,
            hasError = hasAnyError(loadStates),
            errorMessage = getFirstError(loadStates)?.message,
            hasReachedEnd = hasReachedEnd(loadStates),
            canLoadMore = loadStates.append is LoadState.NotLoading && !loadStates.append.endOfPaginationReached
        )
    }
    
    /**
     * Create a retry action for failed pagination
     */
    fun createRetryAction(
        loadStates: androidx.paging.CombinedLoadStates,
        retry: () -> Unit
    ): (() -> Unit)? {
        return if (hasAnyError(loadStates)) {
            retry
        } else {
            null
        }
    }
    
    /**
     * Estimate total item count from paging source
     */
    suspend fun estimateItemCount(pagingSource: PagingSource<*, *>): Int? {
        return try {
            // This is a rough estimation - in practice, you might want to
            // implement this based on your specific data source
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if two PagingData flows have the same configuration
     */
    fun haveSameConfiguration(
        config1: androidx.paging.PagingConfig,
        config2: androidx.paging.PagingConfig
    ): Boolean {
        return config1.pageSize == config2.pageSize &&
               config1.prefetchDistance == config2.prefetchDistance &&
               config1.enablePlaceholders == config2.enablePlaceholders &&
               config1.initialLoadSize == config2.initialLoadSize &&
               config1.maxSize == config2.maxSize
    }
}

/**
 * Summary of pagination state for UI components
 */
data class PaginationStateSummary(
    val isInitialLoading: Boolean,
    val isLoadingMore: Boolean,
    val hasError: Boolean,
    val errorMessage: String?,
    val hasReachedEnd: Boolean,
    val canLoadMore: Boolean
) {
    val isLoading: Boolean get() = isInitialLoading || isLoadingMore
    val canRetry: Boolean get() = hasError
    val showLoadingIndicator: Boolean get() = isInitialLoading
    val showLoadMoreIndicator: Boolean get() = isLoadingMore
    val showEndOfListIndicator: Boolean get() = hasReachedEnd && !hasError
}