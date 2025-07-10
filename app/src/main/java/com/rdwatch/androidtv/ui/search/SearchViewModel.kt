package com.rdwatch.androidtv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.data.repository.UserRepository
import com.rdwatch.androidtv.data.repository.TMDbSearchRepository
import com.rdwatch.androidtv.ui.search.VoiceSearchState
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.TMDbMovieContentDetail
import com.rdwatch.androidtv.ui.details.models.TMDbTVContentDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.Manifest
import androidx.annotation.RequiresPermission

/**
 * ViewModel for search functionality using TMDb API
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tmdbSearchRepository: TMDbSearchRepository,
    private val searchHistoryManager: SearchHistoryManager,
    private val voiceSearchManager: VoiceSearchManager,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    
    private var currentSearchJob: Job? = null
    
    // User ID - will be initialized from UserRepository
    private var currentUserId = UserRepository.DEFAULT_USER_ID
    
    init {
        // Initialize user and load search history
        initializeUser()
        
        // Initialize voice search availability
        updateVoiceSearchAvailability()
        
        // Observe voice search state
        observeVoiceSearchState()
    }
    
    /**
     * Initialize user and load search history
     */
    private fun initializeUser() {
        viewModelScope.launch {
            try {
                currentUserId = userRepository.getDefaultUserId()
                loadSearchHistory()
            } catch (e: Exception) {
                // If user initialization fails, continue with default ID
                // The database initializer should have created the user already
                android.util.Log.e("SearchViewModel", "Failed to initialize user", e)
                loadSearchHistory()
            }
        }
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    /**
     * Perform search with current query using TMDb API
     */
    fun performSearch() {
        val currentState = _uiState.value
        if (currentState.searchQuery.isBlank()) return
        
        // Cancel any existing search
        currentSearchJob?.cancel()
        
        currentSearchJob = viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isLoading = true,
                        error = null,
                        searchResults = emptyList()
                    )
                }
                
                // Add to search history
                try {
                    val userId = userRepository.getDefaultUserId()
                    currentUserId = userId
                    searchHistoryManager.addSearchQuery(
                        userId = userId,
                        query = currentState.searchQuery,
                        filtersJson = serializeFilters(currentState.searchFilters)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("SearchViewModel", "Failed to add to search history", e)
                }
                
                // Perform TMDb multi-search
                tmdbSearchRepository.multiSearchAsContentDetails(
                    query = currentState.searchQuery,
                    includeAdult = !currentState.searchFilters.excludeAdult
                ).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val searchResults = result.data.map { contentDetail ->
                                contentDetail.toSearchResultItem()
                            }
                            
                            _uiState.update { 
                                it.copy(
                                    searchResults = searchResults,
                                    isLoading = false,
                                    error = if (searchResults.isEmpty()) "No results found" else null
                                )
                            }
                            
                            // Update search history with results count
                            try {
                                val userId = userRepository.getDefaultUserId()
                                searchHistoryManager.addSearchQuery(
                                    userId = userId,
                                    query = currentState.searchQuery,
                                    resultsCount = searchResults.size,
                                    filtersJson = serializeFilters(currentState.searchFilters)
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("SearchViewModel", "Failed to update search history", e)
                            }
                        }
                        is Result.Error -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = "Search failed: ${result.exception.message}"
                                )
                            }
                        }
                        is Result.Loading -> {
                            // Keep loading state
                        }
                    }
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Search failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Convert ContentDetail to SearchResultItem
     */
    private fun ContentDetail.toSearchResultItem(): SearchResultItem {
        return SearchResultItem(
            id = when (this.contentType) {
                ContentType.MOVIE -> "movie:${this.id}"
                ContentType.TV_SHOW -> "tv:${this.id}"
                else -> "unknown:${this.id}"
            },
            title = this.title,
            description = this.description?.take(200),
            thumbnailUrl = this.cardImageUrl,
            year = when (this) {
                is TMDbMovieContentDetail -> this.releaseDate?.take(4)?.toIntOrNull()
                is TMDbTVContentDetail -> this.firstAirDate?.take(4)?.toIntOrNull()
                else -> this.metadata.year?.toIntOrNull()
            },
            rating = when (this) {
                is TMDbMovieContentDetail -> this.voteAverage
                is TMDbTVContentDetail -> this.voteAverage
                else -> null
            },
            scraperSource = "TMDb"
        )
    }
    
    /**
     * Clear search query and results
     */
    fun clearSearch() {
        currentSearchJob?.cancel()
        
        _uiState.update { 
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isLoading = false,
                error = null
            )
        }
    }
    
    /**
     * Update search filters
     */
    fun updateFilters(filters: SearchFilters) {
        _uiState.update { it.copy(searchFilters = filters) }
    }
    
    /**
     * Start voice search
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startVoiceSearch() {
        if (!_uiState.value.isVoiceSearchAvailable) return
        
        _uiState.update { it.copy(voiceSearchState = VoiceSearchState.LISTENING) }
        
        voiceSearchManager.startVoiceRecognition(
            onResult = { recognizedText ->
                _uiState.update { 
                    it.copy(
                        searchQuery = recognizedText,
                        voiceSearchState = VoiceSearchState.COMPLETED
                    )
                }
                
                // Automatically perform search with voice input
                performSearch()
            },
            onError = { error ->
                _uiState.update { 
                    it.copy(
                        voiceSearchState = VoiceSearchState.ERROR,
                        error = error
                    )
                }
            }
        )
    }
    
    /**
     * Stop voice search
     */
    fun stopVoiceSearch() {
        voiceSearchManager.stopVoiceRecognition()
        _uiState.update { it.copy(voiceSearchState = VoiceSearchState.IDLE) }
    }
    
    /**
     * Delete search history item
     */
    fun deleteSearchHistoryItem(query: String) {
        viewModelScope.launch {
            try {
                val userId = userRepository.getDefaultUserId()
                searchHistoryManager.deleteSearchQuery(userId, query)
                loadSearchHistory()
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Failed to delete search history", e)
            }
        }
    }
    
    /**
     * Get search suggestions
     */
    suspend fun getSearchSuggestions(partialQuery: String): List<String> {
        return try {
            val userId = userRepository.getDefaultUserId()
            searchHistoryManager.getSearchSuggestions(userId, partialQuery)
        } catch (e: Exception) {
            android.util.Log.e("SearchViewModel", "Failed to get search suggestions", e)
            emptyList()
        }
    }
    
    /**
     * Load search history
     */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            try {
                val userId = userRepository.getDefaultUserId()
                searchHistoryManager.getRecentSearchHistory(userId)
                    .collect { history ->
                        _searchHistory.value = history
                    }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Failed to load search history", e)
                _searchHistory.value = emptyList()
            }
        }
    }
    
    /**
     * Update voice search availability
     */
    private fun updateVoiceSearchAvailability() {
        val isAvailable = voiceSearchManager.isVoiceSearchAvailable() && 
                         voiceSearchManager.hasMicrophone()
        
        _uiState.update { it.copy(isVoiceSearchAvailable = isAvailable) }
    }
    
    /**
     * Observe voice search state changes
     */
    private fun observeVoiceSearchState() {
        viewModelScope.launch {
            voiceSearchManager.voiceSearchState.collect { state ->
                _uiState.update { it.copy(voiceSearchState = state) }
            }
        }
        
        viewModelScope.launch {
            voiceSearchManager.partialResults.collect { partialText ->
                // Could update UI with partial results
            }
        }
        
        viewModelScope.launch {
            voiceSearchManager.error.collect { error ->
                error?.let {
                    _uiState.update { state -> state.copy(error = it) }
                }
            }
        }
    }
    
    /**
     * Serialize filters to JSON string
     */
    private fun serializeFilters(filters: SearchFilters): String {
        // Simplified serialization - in real app use proper JSON library
        return buildString {
            append("{")
            append("\"contentTypes\":${filters.contentTypes}")
            append(",\"minYear\":${filters.minYear}")
            append(",\"maxYear\":${filters.maxYear}")
            append(",\"minRating\":${filters.minRating}")
            append(",\"genres\":${filters.genres}")
            append(",\"languages\":${filters.languages}")
            append(",\"qualityPreferences\":${filters.qualityPreferences}")
            append(",\"excludeAdult\":${filters.excludeAdult}")
            append("}")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        currentSearchJob?.cancel()
        voiceSearchManager.cleanup()
    }
}

/**
 * UI state for search screen
 */
data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<SearchResultItem> = emptyList(),
    val searchFilters: SearchFilters = SearchFilters(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val voiceSearchState: VoiceSearchState = VoiceSearchState.IDLE,
    val isVoiceSearchAvailable: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = searchFilters.hasActiveFilters()
}