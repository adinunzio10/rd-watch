package com.rdwatch.androidtv.ui.details.models.advanced

/**
 * Events for tracking source selection user behavior and analytics
 */
sealed class SourceSelectionEvents {
    
    abstract class SourceEvent
    
    data class SourcePlayEvent(
        val sourceId: String,
        val provider: String,
        val quality: String,
        val playTimeMs: Long
    ) : SourceEvent()
    
    data class SourceLoadEvent(
        val sourceId: String,
        val provider: String,
        val loadTimeMs: Long,
        val success: Boolean
    ) : SourceEvent()
    
    data class SourceErrorEvent(
        val sourceId: String,
        val provider: String,
        val errorType: String,
        val errorMessage: String
    ) : SourceEvent()
    
    data class SourceSelectedEvent(
        val sourceId: String,
        val provider: String,
        val selectionTimeMs: Long,
        val position: Int
    ) : SourceEvent()
    
    data class SourceFilterEvent(
        val filterType: String,
        val filterValue: String,
        val resultCount: Int
    ) : SourceEvent()
    
    data class SourceSortEvent(
        val sortOption: String,
        val sortDirection: String
    ) : SourceEvent()
    
    data class SourceViewEvent(
        val viewType: String,
        val sourceCount: Int,
        val viewTimeMs: Long
    ) : SourceEvent()
    
    data class SourceDownloadEvent(
        val sourceId: String,
        val provider: String,
        val downloadStarted: Boolean,
        val downloadTimeMs: Long
    ) : SourceEvent()
    
    data class SourcePlaylistEvent(
        val action: String,
        val sourceId: String,
        val provider: String,
        val playlistSize: Int
    ) : SourceEvent()
}