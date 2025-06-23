package com.rdwatch.androidtv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.ui.search.VoiceSearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for search functionality coordinating all search components
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchOrchestrationService: SearchOrchestrationService,
    private val searchHistoryManager: SearchHistoryManager,
    private val voiceSearchManager: VoiceSearchManager,
    private val resultAggregator: SearchResultAggregator
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    
    private var currentSearchJob: Job? = null
    private var currentSearchId: String? = null
    
    // Default user ID (in real app, this would come from auth)
    private val currentUserId = 1L
    
    init {
        // Load search history
        loadSearchHistory()
        
        // Initialize voice search availability
        updateVoiceSearchAvailability()
        
        // Observe voice search state
        observeVoiceSearchState()
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    /**
     * Perform search with current query and filters
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
                searchHistoryManager.addSearchQuery(
                    userId = currentUserId,
                    query = currentState.searchQuery,
                    filtersJson = serializeFilters(currentState.searchFilters)
                )
                
                // Perform orchestrated search
                searchOrchestrationService.performSearch(
                    query = currentState.searchQuery,
                    filters = currentState.searchFilters,
                    config = SearchConfig()
                ).collect { result ->
                    handleSearchOrchestrationResult(result)
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
     * Handle search orchestration results
     */
    private suspend fun handleSearchOrchestrationResult(result: SearchOrchestrationResult) {
        when (result) {
            is SearchOrchestrationResult.Started -> {
                currentSearchId = result.searchId
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            
            is SearchOrchestrationResult.ScrapersSelected -> {
                // Could show scraper count in UI
            }
            
            is SearchOrchestrationResult.Progress -> {
                // Could show progress per scraper
            }
            
            is SearchOrchestrationResult.PartialResults -> {
                // Aggregate and display partial results
                val aggregatedResults = resultAggregator.aggregateResults(result.results)
                _uiState.update { 
                    it.copy(
                        searchResults = aggregatedResults.toSimpleResults(),
                        isLoading = result.completedScrapers < result.totalScrapers
                    )
                }
            }
            
            is SearchOrchestrationResult.ScraperError -> {
                // Could show scraper-specific errors
            }
            
            is SearchOrchestrationResult.Completed -> {
                // Final aggregated results
                val aggregatedResults = resultAggregator.aggregateResults(result.results)
                _uiState.update { 
                    it.copy(
                        searchResults = aggregatedResults.toSimpleResults(),
                        isLoading = false,
                        error = if (result.results.isEmpty()) "No results found" else null
                    )
                }
                
                // Update search history with results count
                searchHistoryManager.addSearchQuery(
                    userId = currentUserId,
                    query = _uiState.value.searchQuery,
                    resultsCount = result.results.size,
                    filtersJson = serializeFilters(_uiState.value.searchFilters)
                )
            }
            
            is SearchOrchestrationResult.Error -> {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = result.error
                    )
                }
            }
        }
    }
    
    /**
     * Clear search query and results
     */
    fun clearSearch() {
        currentSearchJob?.cancel()
        currentSearchId?.let { searchOrchestrationService.cancelSearch(it) }
        
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
            searchHistoryManager.deleteSearchQuery(currentUserId, query)
            loadSearchHistory()
        }
    }
    
    /**
     * Get search suggestions
     */
    suspend fun getSearchSuggestions(partialQuery: String): List<String> {
        return searchHistoryManager.getSearchSuggestions(currentUserId, partialQuery)
    }
    
    /**
     * Load search history
     */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            searchHistoryManager.getRecentSearchHistory(currentUserId)
                .collect { history ->
                    _searchHistory.value = history
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
        currentSearchId?.let { searchOrchestrationService.cancelSearch(it) }
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