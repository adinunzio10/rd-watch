package com.rdwatch.androidtv.ui.profile

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.data.mappers.ContentEntityToMovieMapper.toMovies
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * ViewModel for Profile Screen - manages user profile data and watch history
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val realDebridContentRepository: RealDebridContentRepository,
    ) : BaseViewModel<ProfileUiState>() {
        private val _favoriteMovies = MutableStateFlow<List<Movie>>(emptyList())
        val favoriteMovies: StateFlow<List<Movie>> = _favoriteMovies.asStateFlow()

        private val _watchHistory = MutableStateFlow<List<Movie>>(emptyList())
        val watchHistory: StateFlow<List<Movie>> = _watchHistory.asStateFlow()

        override fun createInitialState(): ProfileUiState {
            return ProfileUiState()
        }

        init {
            loadUserProfile()
        }

        /**
         * Load user profile and related data
         */
        private fun loadUserProfile() {
            launchSafely {
                updateState { copy(isLoading = true, error = null) }

                try {
                    // In real implementation, load from user repository/database
                    // For now, using mock data
                    val userProfile =
                        UserProfile(
                            name = "John Doe",
                            email = "john.doe@example.com",
                            membershipType = "Premium",
                            avatarUrl = null,
                        )

                    val statistics =
                        WatchStatistics(
                            moviesWatched = 24,
                            hoursWatched = 156,
                            favoritesCount = 8,
                            totalMinutesWatched = 9360, // 156 hours
                        )

                    // Load favorite movies and watch history from Real-Debrid content
                    val contentResult = realDebridContentRepository.getAllContent().first()
                    when (contentResult) {
                        is Result.Success -> {
                            val allMovies = contentResult.data.toMovies()

                            // For now, simulate favorites and history by taking random content
                            // In a real implementation, this would come from user preferences/database
                            val favorites = allMovies.shuffled().take(8)
                            _favoriteMovies.value = favorites

                            val history = allMovies.shuffled().take(10)
                            _watchHistory.value = history
                        }
                        is Result.Error -> {
                            // Handle error case - keep empty lists
                            _favoriteMovies.value = emptyList()
                            _watchHistory.value = emptyList()
                        }
                        is Result.Loading -> {
                            // Keep loading state
                        }
                    }

                    updateState {
                        copy(
                            userProfile = userProfile,
                            watchStatistics = statistics,
                            isLoading = false,
                            isLoaded = true,
                        )
                    }
                } catch (e: Exception) {
                    updateState {
                        copy(
                            isLoading = false,
                            error = "Failed to load profile: ${e.message}",
                        )
                    }
                }
            }
        }

        /**
         * Update user profile information
         */
        fun updateUserProfile(
            name: String,
            email: String,
        ) {
            launchSafely {
                val currentProfile = uiState.value.userProfile
                if (currentProfile != null) {
                    val updatedProfile =
                        currentProfile.copy(
                            name = name,
                            email = email,
                        )

                    updateState { copy(userProfile = updatedProfile) }

                    // TODO: In real implementation, save to repository
                    saveProfileToStorage(updatedProfile)
                }
            }
        }

        /**
         * Add movie to favorites
         */
        fun addToFavorites(movie: Movie) {
            launchSafely {
                val currentFavorites = _favoriteMovies.value.toMutableList()
                if (!currentFavorites.contains(movie)) {
                    currentFavorites.add(movie)
                    _favoriteMovies.value = currentFavorites

                    // Update statistics
                    val currentStats = uiState.value.watchStatistics
                    if (currentStats != null) {
                        val updatedStats =
                            currentStats.copy(
                                favoritesCount = currentFavorites.size,
                            )
                        updateState { copy(watchStatistics = updatedStats) }
                    }

                    // TODO: In real implementation, save to database
                }
            }
        }

        /**
         * Remove movie from favorites
         */
        fun removeFromFavorites(movie: Movie) {
            launchSafely {
                val currentFavorites = _favoriteMovies.value.toMutableList()
                if (currentFavorites.remove(movie)) {
                    _favoriteMovies.value = currentFavorites

                    // Update statistics
                    val currentStats = uiState.value.watchStatistics
                    if (currentStats != null) {
                        val updatedStats =
                            currentStats.copy(
                                favoritesCount = currentFavorites.size,
                            )
                        updateState { copy(watchStatistics = updatedStats) }
                    }

                    // TODO: In real implementation, update database
                }
            }
        }

        /**
         * Clear watch history
         */
        fun clearWatchHistory() {
            launchSafely {
                _watchHistory.value = emptyList()

                // Reset watch statistics
                val currentStats = uiState.value.watchStatistics
                if (currentStats != null) {
                    val updatedStats =
                        currentStats.copy(
                            moviesWatched = 0,
                            hoursWatched = 0,
                            totalMinutesWatched = 0,
                        )
                    updateState { copy(watchStatistics = updatedStats) }
                }

                // TODO: In real implementation, clear from database
            }
        }

        /**
         * Update privacy settings
         */
        fun updatePrivacySettings(settings: PrivacySettings) {
            updateState { copy(privacySettings = settings) }
            savePrivacySettings(settings)
        }

        /**
         * Update notification preferences
         */
        fun updateNotificationPreferences(preferences: NotificationPreferences) {
            updateState { copy(notificationPreferences = preferences) }
            saveNotificationPreferences(preferences)
        }

        /**
         * Sign out user
         */
        fun signOut() {
            launchSafely {
                updateState { copy(isSigningOut = true) }

                // TODO: In real implementation, clear user session and navigate to auth
                kotlinx.coroutines.delay(1000) // Simulate sign out process

                updateState {
                    ProfileUiState().copy(isSignedOut = true)
                }
            }
        }

        /**
         * Refresh profile data
         */
        fun refresh() {
            loadUserProfile()
        }

        /**
         * Save profile to storage (placeholder)
         */
        private suspend fun saveProfileToStorage(profile: UserProfile) {
            // TODO: Implement actual storage
        }

        /**
         * Save privacy settings (placeholder)
         */
        private fun savePrivacySettings(settings: PrivacySettings) {
            // TODO: Implement actual storage
        }

        /**
         * Save notification preferences (placeholder)
         */
        private fun saveNotificationPreferences(preferences: NotificationPreferences) {
            // TODO: Implement actual storage
        }

        override fun handleError(exception: Throwable) {
            updateState {
                copy(
                    isLoading = false,
                    isSigningOut = false,
                    error = "Profile error: ${exception.message}",
                )
            }
        }
    }

/**
 * UI State for Profile Screen
 */
data class ProfileUiState(
    val userProfile: UserProfile? = null,
    val watchStatistics: WatchStatistics? = null,
    val privacySettings: PrivacySettings = PrivacySettings(),
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val isSigningOut: Boolean = false,
    val isSignedOut: Boolean = false,
    val error: String? = null,
) {
    val hasProfile: Boolean get() = userProfile != null
    val hasStatistics: Boolean get() = watchStatistics != null
}

/**
 * User profile data
 */
data class UserProfile(
    val name: String,
    val email: String,
    val membershipType: String,
    val avatarUrl: String? = null,
    val joinDate: Long = System.currentTimeMillis(),
)

/**
 * Watch statistics data
 */
data class WatchStatistics(
    val moviesWatched: Int,
    val hoursWatched: Int,
    val favoritesCount: Int,
    val totalMinutesWatched: Int,
) {
    val averageWatchTime: Int get() = if (moviesWatched > 0) totalMinutesWatched / moviesWatched else 0
}

/**
 * Privacy settings
 */
data class PrivacySettings(
    val profileVisibility: ProfileVisibility = ProfileVisibility.PRIVATE,
    val shareWatchHistory: Boolean = false,
    val allowRecommendations: Boolean = true,
    val dataCollection: Boolean = true,
)

/**
 * Notification preferences
 */
data class NotificationPreferences(
    val pushNotifications: Boolean = true,
    val emailNotifications: Boolean = true,
    val newContentAlerts: Boolean = true,
    val watchReminders: Boolean = false,
)

enum class ProfileVisibility {
    PUBLIC,
    FRIENDS_ONLY,
    PRIVATE,
}
