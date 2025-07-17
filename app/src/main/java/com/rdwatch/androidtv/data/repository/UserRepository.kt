package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.UserDao
import com.rdwatch.androidtv.data.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository
    @Inject
    constructor(
        private val userDao: UserDao,
    ) {
        /**
         * Get all active users
         */
        fun getAllActiveUsers(): Flow<List<UserEntity>> {
            return userDao.getAllActiveUsers()
        }

        /**
         * Get user by ID
         */
        suspend fun getUserById(userId: Long): UserEntity? {
            return userDao.getUserById(userId)
        }

        /**
         * Create default user for Android TV app
         * This is used when the app runs without proper user authentication
         */
        suspend fun createDefaultUser(): Long {
            val existingUser = userDao.getUserById(DEFAULT_USER_ID)
            if (existingUser != null) {
                return existingUser.userId
            }

            val defaultUser =
                UserEntity(
                    userId = 0, // Will be auto-generated
                    username = "androidtv_user",
                    email = "user@androidtv.local",
                    passwordHash = "", // Not used for TV app
                    createdAt = Date(),
                    updatedAt = Date(),
                    isActive = true,
                    displayName = "Android TV User",
                    profileImageUrl = null,
                )

            return try {
                val userId = userDao.insertUser(defaultUser)
                userId
            } catch (e: Exception) {
                // If insertion fails, try to get existing user again
                // This handles race conditions during app startup
                userDao.getUserById(DEFAULT_USER_ID)?.userId ?: DEFAULT_USER_ID
            }
        }

        /**
         * Ensure default user exists, create if not
         */
        suspend fun ensureDefaultUserExists(): Long {
            val existingUser = userDao.getUserById(DEFAULT_USER_ID)
            return if (existingUser != null) {
                existingUser.userId
            } else {
                createDefaultUser()
            }
        }

        /**
         * Get or create the default user ID for Android TV app
         */
        suspend fun getDefaultUserId(): Long {
            return ensureDefaultUserExists()
        }

        companion object {
            const val DEFAULT_USER_ID = 1L
        }
    }
