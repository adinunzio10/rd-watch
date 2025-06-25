package com.rdwatch.androidtv.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for periodic synchronization with Real-Debrid API
 * 
 * This worker:
 * - Runs every 6 hours
 * - Fetches latest torrents/downloads from RD API
 * - Updates local database via repository
 * - Handles network failures with exponential backoff
 */
@HiltWorker
class RealDebridSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val realDebridRepository: RealDebridContentRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "real_debrid_sync"
        
        // Sync constraints for TV
        fun getPeriodicWorkRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only for TV
                .setRequiresBatteryNotLow(false) // TV devices are usually plugged in
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false) // Allow sync while TV is active
                .build()

            return PeriodicWorkRequestBuilder<RealDebridSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15000L, // 15 seconds minimum backoff
                    TimeUnit.MILLISECONDS
                )
                .addTag(WORK_NAME)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                performSync()
            }
            Result.success()
        } catch (exception: Exception) {
            // Return retry for network errors, failure for other errors
            if (isNetworkError(exception)) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun performSync() {
        // Sync using repository which handles both content entities
        val contentSyncResult = realDebridRepository.syncContent()
        
        when (contentSyncResult) {
            is com.rdwatch.androidtv.repository.base.Result.Success -> {
                // Sync completed successfully
            }
            is com.rdwatch.androidtv.repository.base.Result.Error -> {
                throw contentSyncResult.exception
            }
            is com.rdwatch.androidtv.repository.base.Result.Loading -> {
                // This shouldn't happen in syncContent, but handle it
                throw Exception("Sync operation is still loading")
            }
        }
    }

    private fun isNetworkError(exception: Exception): Boolean {
        return exception.message?.contains("network", ignoreCase = true) == true ||
               exception.message?.contains("connection", ignoreCase = true) == true ||
               exception.message?.contains("timeout", ignoreCase = true) == true ||
               exception is java.net.SocketTimeoutException ||
               exception is java.net.UnknownHostException ||
               exception is java.net.ConnectException
    }
}