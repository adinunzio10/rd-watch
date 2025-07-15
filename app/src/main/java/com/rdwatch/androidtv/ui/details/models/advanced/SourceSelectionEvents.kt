package com.rdwatch.androidtv.ui.details.models.advanced

/**
 * Events for source selection interactions
 */
sealed class SourceSelectionEvent {
    
    // Source selection events
    data class SourceSelected(val source: SourceMetadata) : SourceSelectionEvent()
    data class SourcePlayRequested(val source: SourceMetadata) : SourceSelectionEvent()
    data class SourceDownloadRequested(val source: SourceMetadata) : SourceSelectionEvent()
    data class SourceInfoRequested(val source: SourceMetadata) : SourceSelectionEvent()
    
    // Filter events
    data class FilterApplied(val filter: SourceFilter) : SourceSelectionEvent()
    data class QuickFilterApplied(val preset: QuickFilterPreset) : SourceSelectionEvent()
    object FilterReset : SourceSelectionEvent()
    
    // Sort events
    data class SortOptionChanged(val sortOption: SourceSortOption) : SourceSelectionEvent()
    
    // View mode events
    data class ViewModeChanged(val viewMode: SourceSelectionState.ViewMode) : SourceSelectionEvent()
    
    // Group events
    data class GroupToggled(val groupId: String) : SourceSelectionEvent()
    
    // Debrid events
    data class AddToDebridRequested(val source: SourceMetadata) : SourceSelectionEvent()
    data class CheckCacheRequested(val sources: List<SourceMetadata>) : SourceSelectionEvent()
    
    // Refresh events
    object RefreshRequested : SourceSelectionEvent()
    data class RefreshHealthRequested(val sources: List<SourceMetadata>) : SourceSelectionEvent()
    
    // Navigation events
    object BackPressed : SourceSelectionEvent()
    data class ProviderSettingsRequested(val providerId: String) : SourceSelectionEvent()
    
    // Error events
    data class ErrorOccurred(val message: String) : SourceSelectionEvent()
    object ErrorDismissed : SourceSelectionEvent()
}

/**
 * UI actions that can be performed on sources
 */
sealed class SourceAction {
    data class Play(val source: SourceMetadata) : SourceAction()
    data class Download(val source: SourceMetadata) : SourceAction()
    data class ShowInfo(val source: SourceMetadata) : SourceAction()
    data class AddToDebrid(val source: SourceMetadata) : SourceAction()
    data class CopyLink(val source: SourceMetadata) : SourceAction()
    data class Share(val source: SourceMetadata) : SourceAction()
    data class ReportIssue(val source: SourceMetadata) : SourceAction()
}

/**
 * Analytics events for source selection
 */
sealed class SourceSelectionAnalytics {
    data class SourceViewed(
        val sourceId: String,
        val provider: String,
        val quality: String,
        val codec: String,
        val fileSize: Long?
    ) : SourceSelectionAnalytics()
    
    data class SourcePlayed(
        val sourceId: String,
        val provider: String,
        val quality: String,
        val isDebrid: Boolean,
        val loadTime: Long
    ) : SourceSelectionAnalytics()
    
    data class FilterUsed(
        val filterType: String,
        val filterValue: String,
        val resultCount: Int
    ) : SourceSelectionAnalytics()
    
    data class SortUsed(
        val sortOption: String,
        val direction: String
    ) : SourceSelectionAnalytics()
    
    data class ErrorEncountered(
        val errorType: String,
        val errorMessage: String,
        val sourceId: String?
    ) : SourceSelectionAnalytics()
}