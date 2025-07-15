package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rdwatch.androidtv.ui.details.models.advanced.*
import com.rdwatch.androidtv.ui.details.viewmodels.SourceListViewModel
import com.rdwatch.androidtv.ui.theme.RdwatchTheme

/**
 * Demo component showing the complete source selection flow
 * Integrates all components: bottom sheet, views, state management, and sorting
 */
@Composable
fun SourceSelectionDemo(
    modifier: Modifier = Modifier,
    viewModel: SourceListViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Observe state
    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsState()
    val sourceState by viewModel.state.collectAsState()
    val currentMovieId by viewModel.currentMovieId.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Advanced Source Selection Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "This demonstrates the complete source selection UI with TV navigation, " +
                    "smart sorting, filtering, and quality badge integration.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Demo controls
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Demo Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            viewModel.showSourceSelection("demo_movie_1")
                        }
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Show Source Selection")
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.refreshSources() },
                        enabled = currentMovieId != null
                    ) {
                        Text("Refresh Sources")
                    }
                }
                
                if (currentMovieId != null) {
                    Text(
                        text = "Current Movie: $currentMovieId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // State information
        if (sourceState.sources.isNotEmpty() || sourceState.isLoading) {
            StateInformationCard(
                state = sourceState,
                onApplySmartSort = { viewModel.applySmartSort() },
                onClearFilters = { viewModel.clearFilters() }
            )
        }
        
        // Analytics information
        if (sourceState.sources.isNotEmpty()) {
            AnalyticsCard(
                analytics = viewModel.getSourceAnalytics(),
                usageStats = viewModel.getUsageStatistics()
            )
        }
    }
    
    // Source selection bottom sheet
    SourceListBottomSheet(
        isVisible = isBottomSheetVisible,
        sources = sourceState.sources,
        selectedSource = sourceState.selectedSource,
        state = sourceState,
        onDismiss = { viewModel.hideSourceSelection() },
        onSourceSelected = { source -> viewModel.selectSource(source) },
        onRefresh = { viewModel.refreshSources() },
        onFilterChanged = { filter -> viewModel.applyFilter(filter) },
        onSortChanged = { sort -> viewModel.applySortOption(sort) },
        onGroupToggle = { groupId -> viewModel.toggleGroup(groupId) },
        onViewModeChanged = { viewMode -> viewModel.changeViewMode(viewMode) },
        onPlaySource = { source -> 
            viewModel.playSource(source)
            // In a real app, this would start playback
        },
        onDownloadSource = { source -> 
            viewModel.downloadSource(source)
            // In a real app, this would start download
        },
        onAddToPlaylist = { source -> 
            viewModel.addToPlaylist(source)
            // In a real app, this would add to playlist
        }
    )
}

/**
 * Card showing current state information
 */
@Composable
private fun StateInformationCard(
    state: SourceSelectionState,
    onApplySmartSort: () -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current State",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.filter != SourceFilter()) {
                        OutlinedButton(
                            onClick = onClearFilters,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Clear Filters", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    Button(
                        onClick = onApplySmartSort,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Smart Sort", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            if (state.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Loading sources...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Sources: ${state.filteredSources.size} of ${state.sources.size} available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "View Mode: ${state.viewMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Sort: ${state.sortOption.name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    state.selectedSource?.let { selected ->
                        Text(
                            text = "Selected: ${selected.provider.displayName} - ${selected.quality.resolution.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            state.error?.let { error ->
                Text(
                    text = "Error: $error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Card showing analytics and usage information
 */
@Composable
private fun AnalyticsCard(
    analytics: SourceCollectionAnalysis,
    usageStats: SourceUsageStatistics
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Analytics & Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Source Analytics
            Text(
                text = "Source Quality Distribution:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            analytics.qualityDistribution.forEach { (quality, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = quality.displayName,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Divider()
            
            // Usage Statistics
            Text(
                text = "Usage Statistics:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Total Events: ${usageStats.totalEvents}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Play Events: ${usageStats.playEvents}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Filter Events: ${usageStats.filterEvents}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                if (usageStats.mostUsedProviders.isNotEmpty()) {
                    Text(
                        text = "Most Used: ${usageStats.mostUsedProviders.take(3).joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (usageStats.preferredQualities.isNotEmpty()) {
                    Text(
                        text = "Preferred Qualities: ${usageStats.preferredQualities.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Preview for development
 */
@Preview(showBackground = true)
@Composable
fun SourceSelectionDemoPreview() {
    RdwatchTheme {
        SourceSelectionDemo()
    }
}