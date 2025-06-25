package com.rdwatch.androidtv.ui.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.data.entities.ContentEntity
import com.rdwatch.androidtv.data.entities.TorrentEntity
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.TorrentRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.util.RefreshManager
import com.rdwatch.androidtv.util.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Real-Debrid content with performance optimizations
 * 
 * Features:
 * - Pagination support for large datasets
 * - Memory cache layer integration
 * - Debounced search queries
 * - Background sync status tracking
 * - Flow optimization with distinctUntilChanged
 */
@HiltViewModel
class RealDebridContentViewModel @Inject constructor(
    private val realDebridRepository: RealDebridContentRepository,
    private val torrentRepository: TorrentRepository,
    private val refreshManager: RefreshManager,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    // Search query state with debouncing
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Content type filter
    private val _contentFilter = MutableStateFlow(ContentFilter.ALL)
    val contentFilter: StateFlow<ContentFilter> = _contentFilter.asStateFlow()

    // Sync state from RefreshManager
    val syncState: StateFlow<SyncState> = refreshManager.syncState

    // Performance optimized: Debounced search with proper Flow operators
    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce(300) // 300ms debounce for search
        .distinctUntilChanged() // Only emit when value changes
        .flowOn(dispatcherProvider.default) // Use default dispatcher for CPU work

    // Performance optimized: Combined filter state
    private val filterState = combine(
        debouncedSearchQuery,
        _contentFilter
    ) { query, filter ->
        FilterState(query, filter)
    }.distinctUntilChanged() // Prevent unnecessary recompositions

    // Paginated torrents with performance optimizations
    val paginatedTorrents: Flow<PagingData<TorrentEntity>> = filterState
        .flatMapLatest { (query, filter) ->
            val apiFilter = when (filter) {
                ContentFilter.ALL -> null
                ContentFilter.DOWNLOADING -> "downloading"
                ContentFilter.COMPLETED -> "downloaded"
                ContentFilter.QUEUED -> "queued"
            }
            
            torrentRepository.getTorrentsPaginated(apiFilter)
        }
        .cachedIn(viewModelScope) // Cache paging data in ViewModel scope
        .flowOn(dispatcherProvider.io) // Network operations on IO dispatcher

    // Content entities with caching and performance optimizations
    val allContent: Flow<Result<List<ContentEntity>>> = filterState
        .flatMapLatest { (query, _) ->
            if (query.isBlank()) {
                realDebridRepository.getAllContent()
            } else {
                realDebridRepository.searchContent(query)
            }
        }
        .distinctUntilChanged() // Prevent duplicate emissions
        .flowOn(dispatcherProvider.io) // Network operations on IO dispatcher

    // Local torrents for offline access
    val localTorrents: Flow<List<TorrentEntity>> = filterState
        .flatMapLatest { (query, filter) ->
            when {
                query.isNotBlank() -> torrentRepository.searchTorrents(query)
                filter == ContentFilter.DOWNLOADING -> torrentRepository.getActiveTorrents()
                filter == ContentFilter.COMPLETED -> torrentRepository.getCompletedTorrents()
                else -> torrentRepository.getAllTorrentsLocal()
            }
        }
        .distinctUntilChanged() // Prevent duplicate emissions
        .flowOn(dispatcherProvider.io) // Database operations on IO dispatcher

    // Active torrent count for status display
    private val _activeTorrentCount = MutableStateFlow(0)
    val activeTorrentCount: StateFlow<Int> = _activeTorrentCount.asStateFlow()

    init {
        // Monitor active torrent count
        viewModelScope.launch {
            torrentRepository.getActiveTorrents()
                .distinctUntilChanged()
                .flowOn(dispatcherProvider.io)
                .collect { torrents ->
                    _activeTorrentCount.value = torrents.size
                }
        }
    }

    /**
     * Update search query with automatic debouncing
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Update content filter
     */
    fun updateContentFilter(filter: ContentFilter) {
        _contentFilter.value = filter
    }

    /**
     * Request manual refresh with debouncing
     */
    fun refresh() {
        viewModelScope.launch {
            refreshManager.requestRefresh()
        }
    }

    /**
     * Force refresh bypassing debounce
     */
    fun forceRefresh() {
        viewModelScope.launch {
            refreshManager.requestRefresh(force = true)
        }
    }

    /**
     * Get torrent details by ID with caching
     */
    fun getTorrentById(id: String): Flow<TorrentEntity?> = flow {
        emit(torrentRepository.getTorrentById(id))
    }.flowOn(dispatcherProvider.io)

    /**
     * Update torrent progress (for background sync updates)
     */
    fun updateTorrentProgress(torrentId: String, progress: Float, status: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            torrentRepository.updateTorrentProgress(torrentId, progress, status)
        }
    }

    /**
     * Delete torrent with cache invalidation
     */
    fun deleteTorrent(id: String) {
        viewModelScope.launch {
            realDebridRepository.deleteTorrent(id)
        }
    }

    /**
     * Check if refresh is available (not debounced)
     */
    fun isRefreshAvailable(): Boolean {
        return refreshManager.getSyncInfo().isRefreshAvailable
    }
}

/**
 * Content filter options
 */
enum class ContentFilter {
    ALL,
    DOWNLOADING,
    COMPLETED,
    QUEUED
}

/**
 * Internal filter state for combining search and filter
 */
private data class FilterState(
    val searchQuery: String,
    val contentFilter: ContentFilter
)