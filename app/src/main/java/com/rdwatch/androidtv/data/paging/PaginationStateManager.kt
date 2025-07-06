package com.rdwatch.androidtv.data.paging

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State manager for pagination operations with analytics and caching
 */
@Singleton
class PaginationStateManager @Inject constructor(
    private val pagingSourceFactory: EnhancedAccountFilesPagingSourceFactory
) {
    
    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()
    
    private val _paginationAnalytics = MutableStateFlow(PaginationAnalytics())
    val paginationAnalytics: StateFlow<PaginationAnalytics> = _paginationAnalytics.asStateFlow()
    
    private var currentPagingFlow: Flow<PagingData<AccountFileItem>>? = null
    private val requestCounter = AtomicLong(0)
    
    /**
     * Create a new paging flow with the specified parameters
     */
    fun createPagingFlow(
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        filter: FileFilterCriteria = FileFilterCriteria(),
        scenario: PaginationScenario = PaginationScenario.STANDARD,
        coroutineScope: CoroutineScope
    ): Flow<PagingData<AccountFileItem>> {
        
        val requestId = requestCounter.incrementAndGet()
        
        // Update state
        _paginationState.value = _paginationState.value.copy(
            isLoading = true,
            currentSortOption = sortOption,
            currentFilter = filter,
            currentScenario = scenario,
            requestId = requestId
        )
        
        // Create new paging flow
        val config = PaginationConfig.getConfigForScenario(scenario)
        
        val pagingFlow = androidx.paging.Pager(
            config = config,
            pagingSourceFactory = {
                updateAnalytics { it.copy(totalPagingSourcesCreated = it.totalPagingSourcesCreated + 1) }
                pagingSourceFactory.create(sortOption, filter)
            }
        ).flow.cachedIn(coroutineScope)
        
        currentPagingFlow = pagingFlow
        
        // Monitor the flow for analytics
        coroutineScope.launch {
            pagingFlow.collect { pagingData ->
                _paginationState.value = _paginationState.value.copy(
                    isLoading = false,
                    lastUpdateTime = System.currentTimeMillis(),
                    hasData = true
                )
                
                updateAnalytics { analytics ->
                    analytics.copy(
                        totalPagesLoaded = analytics.totalPagesLoaded + 1,
                        lastLoadTime = System.currentTimeMillis()
                    )
                }
            }
        }
        
        return pagingFlow
    }
    
    /**
     * Refresh the current paging flow
     */
    fun refresh() {
        _paginationState.value = _paginationState.value.copy(
            isRefreshing = true
        )
        
        updateAnalytics { analytics ->
            analytics.copy(
                totalRefreshes = analytics.totalRefreshes + 1,
                lastRefreshTime = System.currentTimeMillis()
            )
        }
        
        // The actual refresh is handled by the PagingData.refresh() method
        // This method just updates our state tracking
    }
    
    /**
     * Clear pagination state
     */
    fun clear() {
        _paginationState.value = PaginationState()
        currentPagingFlow = null
    }
    
    /**
     * Update filter while maintaining pagination
     */
    fun updateFilter(
        newFilter: FileFilterCriteria,
        coroutineScope: CoroutineScope
    ): Flow<PagingData<AccountFileItem>>? {
        val currentState = _paginationState.value
        
        return if (currentState.currentSortOption != null) {
            createPagingFlow(
                sortOption = currentState.currentSortOption,
                filter = newFilter,
                scenario = currentState.currentScenario,
                coroutineScope = coroutineScope
            )
        } else {
            null
        }
    }
    
    /**
     * Update sort option while maintaining pagination
     */
    fun updateSortOption(
        newSortOption: FileEnhancedSortOption,
        coroutineScope: CoroutineScope
    ): Flow<PagingData<AccountFileItem>>? {
        val currentState = _paginationState.value
        
        return createPagingFlow(
            sortOption = newSortOption,
            filter = currentState.currentFilter,
            scenario = currentState.currentScenario,
            coroutineScope = coroutineScope
        )
    }
    
    /**
     * Switch pagination scenario (e.g., from standard to grid view)
     */
    fun switchScenario(
        newScenario: PaginationScenario,
        coroutineScope: CoroutineScope
    ): Flow<PagingData<AccountFileItem>>? {
        val currentState = _paginationState.value
        
        return if (currentState.currentSortOption != null) {
            createPagingFlow(
                sortOption = currentState.currentSortOption,
                filter = currentState.currentFilter,
                scenario = newScenario,
                coroutineScope = coroutineScope
            )
        } else {
            null
        }
    }
    
    /**
     * Get pagination performance metrics
     */
    fun getPerformanceMetrics(): PaginationPerformanceMetrics {
        val analytics = _paginationAnalytics.value
        val state = _paginationState.value
        
        val avgLoadTime = if (analytics.totalPagesLoaded > 0) {
            (analytics.lastLoadTime - analytics.firstLoadTime) / analytics.totalPagesLoaded
        } else {
            0L
        }
        
        return PaginationPerformanceMetrics(
            totalPagesLoaded = analytics.totalPagesLoaded,
            totalRefreshes = analytics.totalRefreshes,
            totalPagingSourcesCreated = analytics.totalPagingSourcesCreated,
            averageLoadTimeMs = avgLoadTime,
            currentRequestId = state.requestId,
            isLoading = state.isLoading,
            hasActiveFlow = currentPagingFlow != null
        )
    }
    
    /**
     * Reset analytics
     */
    fun resetAnalytics() {
        _paginationAnalytics.value = PaginationAnalytics()
    }
    
    // Private helper methods
    
    private fun updateAnalytics(update: (PaginationAnalytics) -> PaginationAnalytics) {
        _paginationAnalytics.value = update(_paginationAnalytics.value)
    }
}

/**
 * Current pagination state
 */
data class PaginationState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasData: Boolean = false,
    val currentSortOption: FileEnhancedSortOption? = null,
    val currentFilter: FileFilterCriteria = FileFilterCriteria(),
    val currentScenario: PaginationScenario = PaginationScenario.STANDARD,
    val lastUpdateTime: Long = 0L,
    val requestId: Long = 0L
)

/**
 * Pagination analytics data
 */
data class PaginationAnalytics(
    val totalPagesLoaded: Int = 0,
    val totalRefreshes: Int = 0,
    val totalPagingSourcesCreated: Int = 0,
    val firstLoadTime: Long = System.currentTimeMillis(),
    val lastLoadTime: Long = 0L,
    val lastRefreshTime: Long = 0L
)

/**
 * Performance metrics for pagination
 */
data class PaginationPerformanceMetrics(
    val totalPagesLoaded: Int,
    val totalRefreshes: Int,
    val totalPagingSourcesCreated: Int,
    val averageLoadTimeMs: Long,
    val currentRequestId: Long,
    val isLoading: Boolean,
    val hasActiveFlow: Boolean
)