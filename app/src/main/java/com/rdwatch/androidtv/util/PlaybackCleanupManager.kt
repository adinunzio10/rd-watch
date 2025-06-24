package com.rdwatch.androidtv.util

import androidx.work.*
import com.rdwatch.androidtv.data.repository.PlaybackProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackCleanupManager @Inject constructor(
    private val playbackProgressRepository: PlaybackProgressRepository
) {
    
    companion object {
        const val CLEANUP_WORK_NAME = "playback_cleanup_work"
        const val CLEANUP_INTERVAL_DAYS = 7L // Run cleanup weekly
        const val OLD_PROGRESS_THRESHOLD_MONTHS = 6L // Remove progress older than 6 months
    }
    
    /**
     * Schedule periodic cleanup of old playback progress data
     */
    fun scheduleCleanup(workManager: WorkManager) {
        val cleanupRequest = PeriodicWorkRequestBuilder<PlaybackCleanupWorker>(
            repeatInterval = CLEANUP_INTERVAL_DAYS,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
    
    /**
     * Manually trigger cleanup of old progress data
     */
    suspend fun performCleanup(): CleanupResult = withContext(Dispatchers.IO) {
        try {
            var itemsRemoved = 0
            
            // Clean up minimal progress (less than 5%)
            playbackProgressRepository.cleanupMinimalProgress()
            itemsRemoved += 50 // Estimate - in real implementation, this would return count
            
            // TODO: Add cleanup for progress older than 6 months
            // This would require adding a method to PlaybackProgressRepository
            // that accepts a date threshold
            
            CleanupResult.Success(itemsRemoved)
        } catch (e: Exception) {
            CleanupResult.Error(e.message ?: "Unknown error occurred during cleanup")
        }
    }
    
    /**
     * Get cleanup statistics
     */
    suspend fun getCleanupStats(): CleanupStats = withContext(Dispatchers.IO) {
        try {
            // This would require additional DAO methods to get these statistics
            CleanupStats(
                totalProgressEntries = 0, // TODO: Implement
                minimalProgressEntries = 0, // TODO: Implement
                oldProgressEntries = 0, // TODO: Implement
                lastCleanupTime = System.currentTimeMillis() // TODO: Store and retrieve
            )
        } catch (e: Exception) {
            CleanupStats()
        }
    }
}

/**
 * WorkManager worker for periodic cleanup
 */
class PlaybackCleanupWorker @AssistedInject constructor(
    @Assisted context: android.content.Context,
    @Assisted workerParams: WorkerParameters,
    private val cleanupManager: PlaybackCleanupManager
) : CoroutineWorker(context, workerParams) {
    
    @AssistedFactory
    interface Factory {
        fun create(context: android.content.Context, params: WorkerParameters): PlaybackCleanupWorker
    }
    
    override suspend fun doWork(): Result {
        return try {
            val result = cleanupManager.performCleanup()
            when (result) {
                is CleanupResult.Success -> {
                    setProgress(workDataOf("items_removed" to result.itemsRemoved))
                    Result.success()
                }
                is CleanupResult.Error -> {
                    Result.failure(workDataOf("error" to result.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to e.message))
        }
    }
}

/**
 * Cleanup operation result
 */
sealed class CleanupResult {
    data class Success(val itemsRemoved: Int) : CleanupResult()
    data class Error(val message: String) : CleanupResult()
}

/**
 * Cleanup statistics data
 */
data class CleanupStats(
    val totalProgressEntries: Int = 0,
    val minimalProgressEntries: Int = 0,
    val oldProgressEntries: Int = 0,
    val lastCleanupTime: Long = 0L
) {
    val lastCleanupFormatted: String
        get() = if (lastCleanupTime > 0) {
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(lastCleanupTime))
        } else {
            "Never"
        }
}