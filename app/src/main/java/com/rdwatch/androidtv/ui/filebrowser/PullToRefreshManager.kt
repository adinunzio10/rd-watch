package com.rdwatch.androidtv.ui.filebrowser

import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.filebrowser.cache.FileBrowserCacheManager
import com.rdwatch.androidtv.ui.filebrowser.repository.FileBrowserRepository
import com.rdwatch.androidtv.ui.filebrowser.models.RefreshState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages pull-to-refresh operations for the file browser
 * Handles cache invalidation, UI state updates, and error handling
 */
@Singleton
class PullToRefreshManager @Inject constructor(
    private val repository: FileBrowserRepository,
    private val cacheManager: FileBrowserCacheManager,
    private val dispatcherProvider: DispatcherProvider
) {
    
    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()
    
    /**
     * Perform pull-to-refresh operation
     * @param clearCache Whether to clear cache before refresh
     * @return Result of the refresh operation
     */
    suspend fun performRefresh(clearCache: Boolean = true): Result<Unit> = withContext(dispatcherProvider.io) {
        _refreshState.value = RefreshState(
            isRefreshing = true,
            lastRefreshTime = System.currentTimeMillis()
        )
        
        try {
            // Clear cache if requested
            if (clearCache) {
                cacheManager.invalidateAll()
            }
            
            // Perform the refresh
            val result = repository.pullToRefresh()
            
            when (result) {
                is Result.Success -> {
                    _refreshState.value = RefreshState(
                        isRefreshing = false,
                        lastRefreshTime = System.currentTimeMillis(),
                        isSuccess = true
                    )
                    result
                }
                is Result.Error -> {
                    _refreshState.value = RefreshState(
                        isRefreshing = false,
                        lastRefreshTime = System.currentTimeMillis(),
                        isSuccess = false,
                        error = result.exception.message
                    )
                    result
                }
                is Result.Loading -> {
                    // This shouldn't happen for pull-to-refresh
                    _refreshState.value = RefreshState(
                        isRefreshing = false,
                        lastRefreshTime = System.currentTimeMillis(),
                        isSuccess = false,
                        error = "Unexpected loading state"
                    )
                    Result.Error(Exception("Unexpected loading state"))
                }
            }
        } catch (e: Exception) {
            _refreshState.value = RefreshState(
                isRefreshing = false,
                lastRefreshTime = System.currentTimeMillis(),
                isSuccess = false,
                error = e.message
            )
            Result.Error(e)
        }
    }
    
    /**
     * Check if refresh is needed based on cache expiration
     * @return true if refresh is recommended
     */
    suspend fun isRefreshNeeded(): Boolean = withContext(dispatcherProvider.io) {
        val cacheInfo = repository.getCacheInfo()
        cacheInfo.isExpired || (System.currentTimeMillis() - _refreshState.value.lastRefreshTime) > REFRESH_THRESHOLD_MS
    }
    
    /**
     * Get time since last refresh
     * @return milliseconds since last refresh
     */
    fun getTimeSinceLastRefresh(): Long {
        return System.currentTimeMillis() - _refreshState.value.lastRefreshTime
    }
    
    /**
     * Reset refresh state
     */
    fun resetRefreshState() {
        _refreshState.value = RefreshState()
    }
    
    companion object {
        private const val REFRESH_THRESHOLD_MS = 5 * 60 * 1000L // 5 minutes
    }
}

