package com.rdwatch.androidtv.ui.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rdwatch.androidtv.ui.details.models.advanced.*
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.ui.focus.rememberTVFocusGroup
import com.rdwatch.androidtv.ui.focus.AutoTVFocus
import com.rdwatch.androidtv.ui.focus.TVFocusItem
import kotlinx.coroutines.delay

/**
 * Comprehensive source selection bottom sheet for advanced source selection
 * Features: TV navigation, grouping, filtering, sorting, and quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceListBottomSheet(
    isVisible: Boolean,
    sources: List<SourceMetadata>,
    selectedSource: SourceMetadata? = null,
    state: SourceSelectionState = SourceSelectionState(),
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onSourceSelected: (SourceMetadata) -> Unit = {},
    onRefresh: () -> Unit = {},
    onFilterChanged: (SourceFilter) -> Unit = {},
    onSortChanged: (SourceSortOption) -> Unit = {},
    onGroupToggle: (String) -> Unit = {},
    onViewModeChanged: (SourceSelectionState.ViewMode) -> Unit = {},
    onPlaySource: (SourceMetadata) -> Unit = {},
    onDownloadSource: (SourceMetadata) -> Unit = {},
    onAddToPlaylist: (SourceMetadata) -> Unit = {}
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    val mainFocusGroup = rememberTVFocusGroup("source_sheet_main")
    
    // Auto-dismiss when not visible
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            bottomSheetState.hide()
        }
    }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .width(40.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            }
        ) {
            SourceListContent(
                sources = sources,
                selectedSource = selectedSource,
                state = state,
                focusGroup = mainFocusGroup,
                onSourceSelected = onSourceSelected,
                onRefresh = onRefresh,
                onFilterChanged = onFilterChanged,
                onSortChanged = onSortChanged,
                onGroupToggle = onGroupToggle,
                onViewModeChanged = onViewModeChanged,
                onPlaySource = onPlaySource,
                onDownloadSource = onDownloadSource,
                onAddToPlaylist = onAddToPlaylist
            )
        }
    }
}

/**
 * Main content of the source list with all features
 */
@Composable
private fun SourceListContent(
    sources: List<SourceMetadata>,
    selectedSource: SourceMetadata?,
    state: SourceSelectionState,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup,
    onSourceSelected: (SourceMetadata) -> Unit,
    onRefresh: () -> Unit,
    onFilterChanged: (SourceFilter) -> Unit,
    onSortChanged: (SourceSortOption) -> Unit,
    onGroupToggle: (String) -> Unit,
    onViewModeChanged: (SourceSelectionState.ViewMode) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    onDownloadSource: (SourceMetadata) -> Unit,
    onAddToPlaylist: (SourceMetadata) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
            .padding(16.dp)
    ) {
        // Header with title and controls
        SourceListHeader(
            sourcesCount = state.filteredSources.size,
            totalSources = state.sources.size,
            viewMode = state.viewMode,
            sortOption = state.sortOption,
            isLoading = state.isLoading,
            onRefresh = onRefresh,
            onViewModeChanged = onViewModeChanged,
            onSortChanged = onSortChanged,
            focusGroup = focusGroup
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick filters
        QuickFiltersRow(
            quickFilters = state.getQuickFilters(),
            currentFilter = state.filter,
            onFilterChanged = onFilterChanged,
            focusGroup = focusGroup
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics summary
        SourceStatisticsSummary(
            statistics = state.getStatistics(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Source list based on view mode
        when (state.viewMode) {
            SourceSelectionState.ViewMode.GRID -> {
                SourceGridView(
                    sources = state.filteredSources,
                    selectedSource = selectedSource,
                    expandedGroups = state.expandedGroups,
                    onSourceSelected = onSourceSelected,
                    onGroupToggle = onGroupToggle,
                    onPlaySource = onPlaySource,
                    onDownloadSource = onDownloadSource,
                    onAddToPlaylist = onAddToPlaylist,
                    focusGroup = focusGroup
                )
            }
            SourceSelectionState.ViewMode.LIST -> {
                SourceListView(
                    sources = state.filteredSources,
                    selectedSource = selectedSource,
                    expandedGroups = state.expandedGroups,
                    onSourceSelected = onSourceSelected,
                    onGroupToggle = onGroupToggle,
                    onPlaySource = onPlaySource,
                    onDownloadSource = onDownloadSource,
                    onAddToPlaylist = onAddToPlaylist,
                    focusGroup = focusGroup
                )
            }
            SourceSelectionState.ViewMode.COMPACT -> {
                SourceCompactView(
                    sources = state.filteredSources,
                    selectedSource = selectedSource,
                    onSourceSelected = onSourceSelected,
                    onPlaySource = onPlaySource,
                    focusGroup = focusGroup
                )
            }
        }
        
        // Loading and error states
        if (state.isLoading) {
            LoadingState(modifier = Modifier.fillMaxWidth())
        }
        
        state.error?.let { error ->
            ErrorState(
                error = error,
                onRetry = onRefresh,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        if (state.filteredSources.isEmpty() && !state.isLoading && state.error == null) {
            EmptyState(modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Header section with title, controls, and statistics
 */
@Composable
private fun SourceListHeader(
    sourcesCount: Int,
    totalSources: Int,
    viewMode: SourceSelectionState.ViewMode,
    sortOption: SourceSortOption,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onViewModeChanged: (SourceSelectionState.ViewMode) -> Unit,
    onSortChanged: (SourceSortOption) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and count
        Column {
            Text(
                text = "Select Source",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (sourcesCount == totalSources) {
                    "$sourcesCount sources available"
                } else {
                    "$sourcesCount of $totalSources sources"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Refresh button
            val refreshFocusRequester = remember { FocusRequester() }
            var refreshFocused by remember { mutableStateOf(false) }
            
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .tvFocusable(
                        enabled = true,
                        focusRequester = refreshFocusRequester,
                        onFocusChanged = { refreshFocused = it.isFocused }
                    )
                    .then(
                        if (refreshFocused) {
                            Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            )
                        } else Modifier
                    ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh sources"
                    )
                }
            }
            
            // View mode selector
            ViewModeSelector(
                currentMode = viewMode,
                onModeChanged = onViewModeChanged,
                focusGroup = focusGroup
            )
            
            // Sort selector
            SortSelector(
                currentSort = sortOption,
                onSortChanged = onSortChanged,
                focusGroup = focusGroup
            )
        }
    }
    
    // Register focus items
    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "refresh_button",
                focusRequester = refreshFocusRequester
            )
        )
    }
}

/**
 * Quick filters row for common filter presets
 */
@Composable
private fun QuickFiltersRow(
    quickFilters: QuickFilters,
    currentFilter: SourceFilter,
    onFilterChanged: (SourceFilter) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup
) {
    if (quickFilters.has4K || quickFilters.hasHDR || quickFilters.hasCached || quickFilters.hasP2P) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            if (quickFilters.has4K) {
                item {
                    QuickFilterChip(
                        label = "4K",
                        isSelected = currentFilter.minQuality == VideoResolution.RESOLUTION_4K,
                        onClick = {
                            onFilterChanged(
                                if (currentFilter.minQuality == VideoResolution.RESOLUTION_4K) {
                                    currentFilter.copy(minQuality = null)
                                } else {
                                    currentFilter.copy(minQuality = VideoResolution.RESOLUTION_4K)
                                }
                            )
                        },
                        focusGroup = focusGroup
                    )
                }
            }
            
            if (quickFilters.hasHDR) {
                item {
                    QuickFilterChip(
                        label = "HDR",
                        isSelected = currentFilter.requireHDR,
                        onClick = {
                            onFilterChanged(currentFilter.copy(requireHDR = !currentFilter.requireHDR))
                        },
                        focusGroup = focusGroup
                    )
                }
            }
            
            if (quickFilters.hasCached) {
                item {
                    QuickFilterChip(
                        label = "Cached",
                        isSelected = currentFilter.requireCached,
                        onClick = {
                            onFilterChanged(currentFilter.copy(requireCached = !currentFilter.requireCached))
                        },
                        focusGroup = focusGroup
                    )
                }
            }
            
            if (quickFilters.hasP2P) {
                item {
                    QuickFilterChip(
                        label = "P2P",
                        isSelected = currentFilter.minSeeders != null,
                        onClick = {
                            onFilterChanged(
                                if (currentFilter.minSeeders != null) {
                                    currentFilter.copy(minSeeders = null)
                                } else {
                                    currentFilter.copy(minSeeders = 1)
                                }
                            )
                        },
                        focusGroup = focusGroup
                    )
                }
            }
            
            item {
                QuickFilterChip(
                    label = "Clear",
                    isSelected = false,
                    onClick = { onFilterChanged(SourceFilter()) },
                    focusGroup = focusGroup
                )
            }
        }
    }
}

/**
 * Quick filter chip component
 */
@Composable
private fun QuickFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = Modifier
            .tvFocusable(
                enabled = true,
                focusRequester = focusRequester,
                onFocusChanged = { isFocused = it.isFocused }
            )
            .then(
                if (isFocused) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    )
                } else Modifier
            )
    )
    
    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "filter_$label",
                focusRequester = focusRequester
            )
        )
    }
}

/**
 * Statistics summary section
 */
@Composable
private fun SourceStatisticsSummary(
    statistics: SourceStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem(
                label = "Total",
                value = statistics.totalSources.toString()
            )
            StatisticItem(
                label = "Cached",
                value = statistics.cachedSources.toString()
            )
            StatisticItem(
                label = "HDR",
                value = statistics.hdrSources.toString()
            )
            StatisticItem(
                label = "Providers",
                value = statistics.providerCount.toString()
            )
            statistics.averageFileSize?.let { avgSize ->
                StatisticItem(
                    label = "Avg Size",
                    value = String.format("%.1f GB", avgSize / (1024.0 * 1024.0 * 1024.0))
                )
            }
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * View mode selector component
 */
@Composable
private fun ViewModeSelector(
    currentMode: SourceSelectionState.ViewMode,
    onModeChanged: (SourceSelectionState.ViewMode) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .tvFocusable(
                    enabled = true,
                    focusRequester = focusRequester,
                    onFocusChanged = { isFocused = it.isFocused }
                )
                .then(
                    if (isFocused) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                )
        ) {
            Icon(
                when (currentMode) {
                    SourceSelectionState.ViewMode.GRID -> Icons.Default.GridView
                    SourceSelectionState.ViewMode.LIST -> Icons.Default.List
                    SourceSelectionState.ViewMode.COMPACT -> Icons.Default.ViewAgenda
                },
                contentDescription = "Change view mode"
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SourceSelectionState.ViewMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onModeChanged(mode)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            when (mode) {
                                SourceSelectionState.ViewMode.GRID -> Icons.Default.GridView
                                SourceSelectionState.ViewMode.LIST -> Icons.Default.List
                                SourceSelectionState.ViewMode.COMPACT -> Icons.Default.ViewAgenda
                            },
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "view_mode_selector",
                focusRequester = focusRequester
            )
        )
    }
}

/**
 * Sort selector component
 */
@Composable
private fun SortSelector(
    currentSort: SourceSortOption,
    onSortChanged: (SourceSortOption) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .tvFocusable(
                    enabled = true,
                    focusRequester = focusRequester,
                    onFocusChanged = { isFocused = it.isFocused }
                )
                .then(
                    if (isFocused) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                )
        ) {
            Icon(
                Icons.Default.Sort,
                contentDescription = "Sort sources"
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SourceSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            option.name.lowercase()
                                .split('_')
                                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                        )
                    },
                    onClick = {
                        onSortChanged(option)
                        expanded = false
                    }
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "sort_selector",
                focusRequester = focusRequester
            )
        )
    }
}

/**
 * Loading state component
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading sources...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error state component
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error loading sources",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Empty state component
 */
@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No sources found",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Try adjusting your filters or refreshing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}