package com.rdwatch.androidtv.data.initialization

import com.rdwatch.androidtv.data.repository.UserRepository
import com.rdwatch.androidtv.ui.search.SearchHistoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val searchHistoryManager: SearchHistoryManager,
    ) {
        private val initializationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /**
         * Initialize database with default data required for app operation
         */
        fun initialize() {
            initializationScope.launch {
                try {
                    // Ensure default user exists for Android TV app
                    userRepository.ensureDefaultUserExists()

                    // Initialize with sample search history for development/testing
                    initializeSampleSearchHistory()
                } catch (e: Exception) {
                    // Log error but don't crash the app
                    android.util.Log.e("DatabaseInitializer", "Failed to initialize database", e)
                }
            }
        }

        /**
         * Initialize sample search history entries for testing/development
         */
        private suspend fun initializeSampleSearchHistory() {
            try {
                val userId = userRepository.getDefaultUserId()

                // Add some sample search history entries
                val sampleSearches =
                    listOf(
                        "futurama" to 25,
                        "chuck" to 18,
                        "breaking bad" to 62,
                        "the office" to 201,
                        "stranger things" to 34,
                    )

                sampleSearches.forEach { (query, resultCount) ->
                    searchHistoryManager.addSearchQuery(
                        userId = userId,
                        query = query,
                        resultsCount = resultCount,
                        searchType = "general",
                    )
                }

                android.util.Log.d("DatabaseInitializer", "Sample search history initialized")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseInitializer", "Failed to initialize sample search history", e)
            }
        }
    }
