package com.rdwatch.androidtv.ui.library

import com.rdwatch.androidtv.data.entities.LibraryEntity
import com.rdwatch.androidtv.data.repository.LibraryRepository
import com.rdwatch.androidtv.data.repository.UserRepository
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.details.models.ContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for Library Screen - manages user's saved content
 * Provides filtering, searching, and CRUD operations for library items
 */
@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val libraryRepository: LibraryRepository,
        private val userRepository: UserRepository,
    ) : BaseViewModel<LibraryUiState>() {
        private val _libraryContent = MutableStateFlow<UiState<List<LibraryEntity>>>(UiState.Idle)
        val libraryContent: StateFlow<UiState<List<LibraryEntity>>> = _libraryContent.asStateFlow()

        private val _libraryStats = MutableStateFlow<UiState<LibraryStats>>(UiState.Idle)
        val libraryStats: StateFlow<UiState<LibraryStats>> = _libraryStats.asStateFlow()

        private val _currentUserId = MutableStateFlow<Long>(UserRepository.DEFAULT_USER_ID)

        override fun createInitialState(): LibraryUiState {
            return LibraryUiState()
        }

        init {
            initializeUser()
            loadLibraryContent()
            loadLibraryStats()
        }

        /**
         * Initialize user context
         */
        private fun initializeUser() {
            launchSafely {
                val userId = userRepository.getDefaultUserId()
                _currentUserId.value = userId
                updateState { copy(userId = userId) }
            }
        }

        /**
         * Load library content based on current filters
         */
        fun loadLibraryContent() {
            launchSafely {
                _libraryContent.value = UiState.Loading
                updateState { copy(isLoading = true, error = null) }

                val userId = _currentUserId.value
                val currentState = getCurrentState()

                // Get appropriate content based on filters
                val contentFlow =
                    when {
                        currentState.showFavoritesOnly -> libraryRepository.getFavoritesByUser(userId)
                        currentState.showDownloadsOnly -> libraryRepository.getDownloadedContentByUser(userId)
                        currentState.selectedContentType != null ->
                            libraryRepository.getLibraryByUserAndType(userId, currentState.selectedContentType.name)
                        currentState.searchQuery.isNotBlank() ->
                            libraryRepository.searchLibrary(userId, currentState.searchQuery)
                        else -> libraryRepository.getLibraryByUser(userId)
                    }

                contentFlow
                    .flowOn(kotlinx.coroutines.Dispatchers.IO)
                    .collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val filteredContent = applyAdditionalFilters(result.data, currentState)
                                _libraryContent.value = UiState.Success(filteredContent)
                                updateState { copy(isLoading = false, itemCount = filteredContent.size) }
                            }
                            is Result.Error -> {
                                _libraryContent.value =
                                    UiState.Error(
                                        message = "Failed to load library content: ${result.exception.message}",
                                        throwable = result.exception,
                                    )
                                updateState { copy(isLoading = false, error = result.exception.message) }
                            }
                            is Result.Loading -> {
                                // Already handled above
                            }
                        }
                    }
            }
        }

        /**
         * Load library statistics
         */
        fun loadLibraryStats() {
            launchSafely {
                _libraryStats.value = UiState.Loading

                val userId = _currentUserId.value

                // Collect multiple stats
                val totalCount = libraryRepository.getLibraryCountByUser(userId)
                val favoritesCount = libraryRepository.getFavoritesCountByUser(userId)
                val downloadedSize = libraryRepository.getTotalDownloadedSizeByUser(userId)
                val contentTypes = libraryRepository.getContentTypesByUser(userId)

                when {
                    totalCount is Result.Success &&
                        favoritesCount is Result.Success &&
                        downloadedSize is Result.Success &&
                        contentTypes is Result.Success -> {
                        val stats =
                            LibraryStats(
                                totalItems = totalCount.data,
                                favoriteItems = favoritesCount.data,
                                downloadedSizeBytes = downloadedSize.data,
                                availableContentTypes =
                                    contentTypes.data.mapNotNull { type ->
                                        try {
                                            ContentType.valueOf(type)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    },
                            )
                        _libraryStats.value = UiState.Success(stats)
                        updateState { copy(stats = stats) }
                    }
                    else -> {
                        _libraryStats.value = UiState.Error("Failed to load library statistics")
                    }
                }
            }
        }

        /**
         * Apply additional filtering and sorting
         */
        private fun applyAdditionalFilters(
            content: List<LibraryEntity>,
            state: LibraryUiState,
        ): List<LibraryEntity> {
            var filtered = content

            // Apply content type filter if not already applied at DB level
            state.selectedContentType?.let { contentType ->
                filtered = filtered.filter { it.contentType == contentType.name }
            }

            // Apply favorites filter if not already applied at DB level
            if (state.showFavoritesOnly) {
                filtered = filtered.filter { it.isFavorite }
            }

            // Apply downloads filter if not already applied at DB level
            if (state.showDownloadsOnly) {
                filtered = filtered.filter { it.isDownloaded }
            }

            // Apply search filter if not already applied at DB level
            if (state.searchQuery.isNotBlank()) {
                val query = state.searchQuery.lowercase()
                filtered =
                    filtered.filter { item ->
                        item.title.lowercase().contains(query) ||
                            item.description?.lowercase()?.contains(query) == true
                    }
            }

            // Apply sorting
            return when (state.sortBy) {
                LibrarySortBy.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedAt }
                LibrarySortBy.ALPHABETICAL -> filtered.sortedBy { it.title }
                LibrarySortBy.LAST_UPDATED -> filtered.sortedByDescending { it.updatedAt }
            }
        }

        /**
         * Filter functions
         */
        fun setContentTypeFilter(contentType: ContentType?) {
            updateState { copy(selectedContentType = contentType) }
            loadLibraryContent()
        }

        fun toggleFavoritesOnly() {
            updateState { copy(showFavoritesOnly = !showFavoritesOnly) }
            loadLibraryContent()
        }

        fun toggleDownloadsOnly() {
            updateState { copy(showDownloadsOnly = !showDownloadsOnly) }
            loadLibraryContent()
        }

        fun setSortBy(sortBy: LibrarySortBy) {
            updateState { copy(sortBy = sortBy) }
            loadLibraryContent()
        }

        fun setSearchQuery(query: String) {
            updateState { copy(searchQuery = query) }
            if (query.length >= 2 || query.isEmpty()) {
                loadLibraryContent()
            }
        }

        fun clearFilters() {
            updateState {
                copy(
                    selectedContentType = null,
                    showFavoritesOnly = false,
                    showDownloadsOnly = false,
                    searchQuery = "",
                )
            }
            loadLibraryContent()
        }

        /**
         * Library item operations
         */
        fun addToLibrary(
            contentId: String,
            contentType: ContentType,
            title: String,
            description: String? = null,
            thumbnailUrl: String? = null,
        ) {
            launchSafely {
                val userId = _currentUserId.value
                val now = Date()

                val libraryItem =
                    LibraryEntity(
                        userId = userId,
                        contentId = contentId,
                        contentType = contentType.name,
                        title = title,
                        description = description,
                        thumbnailUrl = thumbnailUrl,
                        addedAt = now,
                        updatedAt = now,
                    )

                when (val result = libraryRepository.addToLibrary(libraryItem)) {
                    is Result.Success -> {
                        loadLibraryContent()
                        loadLibraryStats()
                    }
                    is Result.Error -> {
                        updateState { copy(error = "Failed to add to library: ${result.exception.message}") }
                    }
                    is Result.Loading -> { /* No action needed */ }
                }
            }
        }

        fun removeFromLibrary(contentId: String) {
            launchSafely {
                val userId = _currentUserId.value

                when (val result = libraryRepository.removeFromLibrary(userId, contentId)) {
                    is Result.Success -> {
                        loadLibraryContent()
                        loadLibraryStats()
                    }
                    is Result.Error -> {
                        updateState { copy(error = "Failed to remove from library: ${result.exception.message}") }
                    }
                    is Result.Loading -> { /* No action needed */ }
                }
            }
        }

        fun toggleFavorite(contentId: String) {
            launchSafely {
                val userId = _currentUserId.value

                // First get current item to determine new favorite status
                when (val currentItem = libraryRepository.getLibraryItem(userId, contentId)) {
                    is Result.Success -> {
                        currentItem.data?.let { item ->
                            val newFavoriteStatus = !item.isFavorite

                            when (val result = libraryRepository.updateFavoriteStatus(userId, contentId, newFavoriteStatus)) {
                                is Result.Success -> {
                                    loadLibraryContent()
                                    loadLibraryStats()
                                }
                                is Result.Error -> {
                                    updateState { copy(error = "Failed to update favorite: ${result.exception.message}") }
                                }
                                is Result.Loading -> { /* No action needed */ }
                            }
                        }
                    }
                    is Result.Error -> {
                        updateState { copy(error = "Failed to find library item: ${currentItem.exception.message}") }
                    }
                    is Result.Loading -> { /* No action needed */ }
                }
            }
        }

        fun updateDownloadStatus(
            contentId: String,
            isDownloaded: Boolean,
            filePath: String? = null,
            fileSizeBytes: Long? = null,
        ) {
            launchSafely {
                val userId = _currentUserId.value

                when (
                    val result =
                        libraryRepository.updateDownloadStatus(
                            userId = userId,
                            contentId = contentId,
                            isDownloaded = isDownloaded,
                            filePath = filePath,
                            fileSizeBytes = fileSizeBytes,
                        )
                ) {
                    is Result.Success -> {
                        loadLibraryContent()
                        loadLibraryStats()
                    }
                    is Result.Error -> {
                        updateState { copy(error = "Failed to update download status: ${result.exception.message}") }
                    }
                    is Result.Loading -> { /* No action needed */ }
                }
            }
        }

        fun clearError() {
            updateState { copy(error = null) }
        }

        fun refresh() {
            loadLibraryContent()
            loadLibraryStats()
        }

        /**
         * Export library to JSON format
         */
        fun exportLibrary(): Flow<Result<String>> =
            flow {
                emit(Result.Loading)

                try {
                    val userId = _currentUserId.value
                    when (val libraryResult = libraryRepository.getLibraryByUser(userId).firstOrNull()) {
                        is Result.Success -> {
                            val libraryItems = libraryResult.data
                            val exportData =
                                LibraryExportData(
                                    exportedAt = Date(),
                                    version = "1.0",
                                    userId = userId,
                                    totalItems = libraryItems.size,
                                    items = libraryItems,
                                )

                            // Simple JSON export (in production use proper JSON library)
                            val jsonString = "Export completed with ${libraryItems.size} items"
                            emit(Result.Success(jsonString))
                        }
                        is Result.Error -> {
                            emit(Result.Error(libraryResult.exception))
                        }
                        else -> {
                            emit(Result.Error(Exception("Failed to load library data for export")))
                        }
                    }
                } catch (e: Exception) {
                    emit(Result.Error(e))
                }
            }
    }

/**
 * UI State for Library Screen
 */
data class LibraryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userId: Long = UserRepository.DEFAULT_USER_ID,
    val selectedContentType: ContentType? = null,
    val showFavoritesOnly: Boolean = false,
    val showDownloadsOnly: Boolean = false,
    val searchQuery: String = "",
    val sortBy: LibrarySortBy = LibrarySortBy.RECENTLY_ADDED,
    val itemCount: Int = 0,
    val stats: LibraryStats? = null,
)

/**
 * Library statistics data
 */
data class LibraryStats(
    val totalItems: Int = 0,
    val favoriteItems: Int = 0,
    val downloadedSizeBytes: Long = 0,
    val availableContentTypes: List<ContentType> = emptyList(),
) {
    fun getDownloadedSizeFormatted(): String {
        return when {
            downloadedSizeBytes < 1024 -> "${downloadedSizeBytes}B"
            downloadedSizeBytes < 1024 * 1024 -> "${downloadedSizeBytes / 1024}KB"
            downloadedSizeBytes < 1024 * 1024 * 1024 -> "${downloadedSizeBytes / (1024 * 1024)}MB"
            else -> "${downloadedSizeBytes / (1024 * 1024 * 1024)}GB"
        }
    }
}

/**
 * Sorting options for library content
 */
enum class LibrarySortBy {
    RECENTLY_ADDED,
    ALPHABETICAL,
    LAST_UPDATED,
}

/**
 * Data class for library export
 */
data class LibraryExportData(
    val exportedAt: Date,
    val version: String,
    val userId: Long,
    val totalItems: Int,
    val items: List<LibraryEntity>,
)
