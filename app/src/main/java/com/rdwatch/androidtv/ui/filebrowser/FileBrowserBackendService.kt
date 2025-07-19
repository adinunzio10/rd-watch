package com.rdwatch.androidtv.ui.filebrowser

import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.filebrowser.repository.CacheInfo
import com.rdwatch.androidtv.ui.filebrowser.repository.FileBrowserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backend service for file browser operations
 * Coordinates bulk operations, pull-to-refresh, and error recovery
 */
@Singleton
class FileBrowserBackendService
    @Inject
    constructor(
        private val repository: FileBrowserRepository,
        private val bulkOperationsManager: BulkOperationsManager,
        private val pullToRefreshManager: PullToRefreshManager,
        private val errorRecoveryManager: ErrorRecoveryManager,
        private val dispatcherProvider: DispatcherProvider,
    ) {
        /**
         * Get combined state flows for the file browser
         */
        fun getStateFlows(): FileBrowserStateFlows {
            return FileBrowserStateFlows(
                bulkOperationState = bulkOperationsManager.operationState,
                refreshState = pullToRefreshManager.refreshState,
                recoveryState = errorRecoveryManager.recoveryState,
                operationProgress = bulkOperationsManager.operationProgress,
            )
        }

        /**
         * Perform bulk delete operation with comprehensive error handling
         */
        suspend fun performBulkDelete(
            itemIds: Set<String>,
            itemNames: Map<String, String>,
            config: BulkOperationConfig = BulkOperationConfig(),
        ): Result<BulkOperationResult> =
            withContext(dispatcherProvider.io) {
                try {
                    // Perform bulk delete
                    val result = bulkOperationsManager.bulkDelete(itemIds, itemNames, config)

                    // If there were failures, attempt recovery
                    if (result.failedCount > 0 && config.enableRollback) {
                        val recoveryResult = errorRecoveryManager.recoverFromBulkFailure(result)
                        when (recoveryResult) {
                            is Result.Success -> Result.Success(recoveryResult.data)
                            is Result.Error -> Result.Success(result) // Return original result if recovery fails
                            is Result.Loading -> Result.Success(result)
                        }
                    } else {
                        Result.Success(result)
                    }
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }

        /**
         * Perform bulk download operation with comprehensive error handling
         */
        suspend fun performBulkDownload(
            fileIds: Set<String>,
            fileNames: Map<String, String>,
            config: BulkOperationConfig = BulkOperationConfig(),
        ): Result<BulkOperationResult> =
            withContext(dispatcherProvider.io) {
                try {
                    // Perform bulk download
                    val result = bulkOperationsManager.bulkDownload(fileIds, fileNames, config)

                    // If there were failures, attempt recovery
                    if (result.failedCount > 0) {
                        val recoveryResult = errorRecoveryManager.recoverFromBulkFailure(result)
                        when (recoveryResult) {
                            is Result.Success -> Result.Success(recoveryResult.data)
                            is Result.Error -> Result.Success(result) // Return original result if recovery fails
                            is Result.Loading -> Result.Success(result)
                        }
                    } else {
                        Result.Success(result)
                    }
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }

        /**
         * Perform pull-to-refresh operation
         */
        suspend fun performPullToRefresh(clearCache: Boolean = true): Result<Unit> =
            withContext(dispatcherProvider.io) {
                pullToRefreshManager.performRefresh(clearCache)
            }

        /**
         * Cancel ongoing bulk operations
         */
        fun cancelBulkOperations() {
            bulkOperationsManager.cancelOperation()
        }

        /**
         * Retry failed items from the last bulk operation
         */
        suspend fun retryFailedItems(config: BulkOperationConfig = BulkOperationConfig()): Result<BulkOperationResult> =
            withContext(dispatcherProvider.io) {
                try {
                    bulkOperationsManager.retryFailedItems(config)
                    Result.Success(
                        BulkOperationResult(
                            operationType = BulkOperationType.DELETE, // Will be updated based on actual operation
                            totalItems = 0,
                            successCount = 0,
                            failedCount = 0,
                            errors = emptyList(),
                        ),
                    )
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }

        /**
         * Get comprehensive cache information
         */
        suspend fun getCacheInformation(): Result<CacheInformation> =
            withContext(dispatcherProvider.io) {
                try {
                    val cacheInfo = repository.getCacheInfo()
                    val refreshState = pullToRefreshManager.refreshState.value
                    val failureStats = errorRecoveryManager.getFailureStatistics()

                    val cacheInformation =
                        CacheInformation(
                            basic = cacheInfo,
                            refreshInfo =
                                RefreshInformation(
                                    lastRefreshTime = refreshState.lastRefreshTime,
                                    isRefreshing = refreshState.isRefreshing,
                                    needsRefresh = pullToRefreshManager.isRefreshNeeded(),
                                ),
                            failureInfo =
                                FailureInformation(
                                    totalFailures = failureStats.totalFailures,
                                    recentFailures = failureStats.recentFailures,
                                    mostCommonError = failureStats.mostCommonError,
                                    lastFailureTime = failureStats.lastFailureTime,
                                ),
                            recommendations = generateRecommendations(cacheInfo, refreshState, failureStats),
                        )

                    Result.Success(cacheInformation)
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }

        /**
         * Clear all caches and reset state
         */
        suspend fun clearAllCaches(): Result<Unit> =
            withContext(dispatcherProvider.io) {
                try {
                    repository.clearCache()
                    pullToRefreshManager.resetRefreshState()
                    errorRecoveryManager.clearFailureHistory()
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }

        /**
         * Get operation health status
         */
        suspend fun getOperationHealth(): Result<OperationHealth> =
            withContext(dispatcherProvider.io) {
                try {
                    val isAuthenticated = repository.isAuthenticated()
                    val cacheInfo = repository.getCacheInfo()
                    val failureStats = errorRecoveryManager.getFailureStatistics()
                    val refreshState = pullToRefreshManager.refreshState.value

                    val health =
                        OperationHealth(
                            isAuthenticated = isAuthenticated,
                            cacheHealth = if (cacheInfo.isExpired) HealthStatus.POOR else HealthStatus.GOOD,
                            networkHealth = determineNetworkHealth(failureStats),
                            overallHealth = determineOverallHealth(isAuthenticated, cacheInfo, failureStats, refreshState),
                            lastChecked = System.currentTimeMillis(),
                        )

                    Result.Success(health)
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }

        private fun generateRecommendations(
            cacheInfo: CacheInfo,
            refreshState: RefreshState,
            failureStats: FailureStatistics,
        ): List<String> {
            val recommendations = mutableListOf<String>()

            if (cacheInfo.isExpired) {
                recommendations.add("Cache is expired - consider refreshing")
            }

            if (refreshState.hasError) {
                recommendations.add("Last refresh failed - check network connection")
            }

            if (failureStats.recentFailures > 5) {
                recommendations.add("High failure rate detected - check service status")
            }

            if (failureStats.mostCommonError != null) {
                recommendations.add("Most common error: ${failureStats.mostCommonError}")
            }

            if (System.currentTimeMillis() - refreshState.lastRefreshTime > 300000) { // 5 minutes
                recommendations.add("Consider refreshing for latest data")
            }

            return recommendations
        }

        private fun determineNetworkHealth(failureStats: FailureStatistics): HealthStatus {
            return when {
                failureStats.recentFailures > 10 -> HealthStatus.POOR
                failureStats.recentFailures > 5 -> HealthStatus.MODERATE
                else -> HealthStatus.GOOD
            }
        }

        private fun determineOverallHealth(
            isAuthenticated: Boolean,
            cacheInfo: CacheInfo,
            failureStats: FailureStatistics,
            refreshState: RefreshState,
        ): HealthStatus {
            if (!isAuthenticated) return HealthStatus.POOR

            val issues =
                listOf(
                    cacheInfo.isExpired,
                    refreshState.hasError,
                    failureStats.recentFailures > 5,
                ).count { it }

            return when {
                issues >= 2 -> HealthStatus.POOR
                issues == 1 -> HealthStatus.MODERATE
                else -> HealthStatus.GOOD
            }
        }
    }

/**
 * Combined state flows for the file browser
 */
data class FileBrowserStateFlows(
    val bulkOperationState: StateFlow<BulkOperationState>,
    val refreshState: StateFlow<RefreshState>,
    val recoveryState: StateFlow<RecoveryState>,
    val operationProgress: SharedFlow<BulkOperationProgress>,
)

/**
 * Comprehensive cache information
 */
data class CacheInformation(
    val basic: CacheInfo,
    val refreshInfo: RefreshInformation,
    val failureInfo: FailureInformation,
    val recommendations: List<String>,
)

/**
 * Refresh-related information
 */
data class RefreshInformation(
    val lastRefreshTime: Long,
    val isRefreshing: Boolean,
    val needsRefresh: Boolean,
)

/**
 * Failure-related information
 */
data class FailureInformation(
    val totalFailures: Int,
    val recentFailures: Int,
    val mostCommonError: String?,
    val lastFailureTime: Long,
)

/**
 * Overall operation health
 */
data class OperationHealth(
    val isAuthenticated: Boolean,
    val cacheHealth: HealthStatus,
    val networkHealth: HealthStatus,
    val overallHealth: HealthStatus,
    val lastChecked: Long,
)

/**
 * Health status levels
 */
enum class HealthStatus {
    GOOD,
    MODERATE,
    POOR,
}
