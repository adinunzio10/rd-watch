package com.rdwatch.androidtv.ui.common

import com.rdwatch.androidtv.Movie

/**
 * Specific UI states for content screens
 */

/**
 * State for screens that display lists of content
 */
data class ContentListState(
    val items: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean get() = items.isEmpty() && !isLoading
}

/**
 * State for search functionality
 */
data class SearchState(
    val query: String = "",
    val results: List<Movie> = emptyList(),
    val isSearching: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val hasResults: Boolean get() = results.isNotEmpty()
}

/**
 * State for playback screens
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val bufferedPosition: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val error: String? = null
) {
    val progress: Float get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val bufferedProgress: Float get() = if (duration > 0) bufferedPosition.toFloat() / duration else 0f
}

/**
 * State for download functionality
 */
data class DownloadState(
    val downloadId: String? = null,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
) {
    val isDownloading: Boolean get() = downloadId != null && !isCompleted && !isPaused
    val canResume: Boolean get() = isPaused && downloadId != null
}

/**
 * State for Real-Debrid specific operations
 */
data class RealDebridState(
    val isConnected: Boolean = false,
    val torrents: List<RealDebridTorrent> = emptyList(),
    val downloads: List<RealDebridDownload> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Simplified RD torrent representation
 */
data class RealDebridTorrent(
    val id: String,
    val filename: String,
    val status: String,
    val progress: Float
)

/**
 * Simplified RD download representation
 */
data class RealDebridDownload(
    val id: String,
    val filename: String,
    val link: String,
    val size: Long
)