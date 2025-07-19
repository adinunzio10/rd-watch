package com.rdwatch.androidtv.data.initialization

import com.rdwatch.androidtv.data.repository.UserRepository
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
                } catch (e: Exception) {
                    // Log error but don't crash the app
                    android.util.Log.e("DatabaseInitializer", "Failed to initialize database", e)
                }
            }
        }
    }
