package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rdwatch.androidtv.ui.details.models.SourceProvider
import com.rdwatch.androidtv.ui.details.models.SourceQuality
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import com.rdwatch.androidtv.ui.details.models.SourceSortOption
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Full-screen dialog for detailed source selection
 * Provides filtering, sorting, and detailed view of all available sources
 */
@Composable
fun SourceSelectionDialog(
    sources: List<StreamingSource>,
    onSourceSelected: (StreamingSource) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    selectedSourceId: String? = null,
    title: String = "Select Source"
) {
    var sortOption by remember { mutableStateOf(SourceSortOption.PRIORITY) }
    var selectedQuality by remember { mutableStateOf<SourceQuality?>(null) }
    var selectedProvider by remember { mutableStateOf<SourceProvider?>(null) }
    var showP2POnly by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    
    val filteredAndSortedSources = remember(sources, sortOption, selectedQuality, selectedProvider, showP2POnly) {
        sources.filter { source ->
            val qualityMatch = selectedQuality?.let { it == source.quality } ?: true
            val providerMatch = selectedProvider?.let { it.id == source.provider.id } ?: true
            val p2pMatch = if (showP2POnly) source.isP2P() else true
            
            qualityMatch && providerMatch && p2pMatch
        }.sortedWith(getSortComparator(sortOption))
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog header
                SourceDialogHeader(
                    title = title,
                    onDismiss = onDismiss,
                    onToggleFilters = { showFilters = !showFilters },
                    showFilters = showFilters
                )
                
                // Filters section
                if (showFilters) {
                    SourceFiltersSection(
                        sources = sources,
                        selectedQuality = selectedQuality,
                        selectedProvider = selectedProvider,
                        showP2POnly = showP2POnly,
                        sortOption = sortOption,
                        onQualitySelected = { selectedQuality = it },
                        onProviderSelected = { selectedProvider = it },
                        onP2POnlyChanged = { showP2POnly = it },
                        onSortChanged = { sortOption = it }
                    )
                }
                
                // Sources grid
                SourcesGrid(
                    sources = filteredAndSortedSources,
                    selectedSourceId = selectedSourceId,
                    onSourceSelected = onSourceSelected,
                    modifier = Modifier.weight(1f)
                )
                
                // Dialog footer
                SourceDialogFooter(
                    totalSources = sources.size,
                    filteredSources = filteredAndSortedSources.size,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * Dialog header with title and controls
 */
@Composable
private fun SourceDialogHeader(
    title: String,
    onDismiss: () -> Unit,
    onToggleFilters: () -> Unit,
    showFilters: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Toggle filters button
            DialogActionButton(
                icon = Icons.Default.FilterList,
                contentDescription = "Toggle filters",
                onClick = onToggleFilters,
                isActive = showFilters
            )
            
            // Close button
            DialogActionButton(
                icon = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onDismiss
            )
        }
    }
}

/**
 * Filters section
 */
@Composable
private fun SourceFiltersSection(
    sources: List<StreamingSource>,
    selectedQuality: SourceQuality?,
    selectedProvider: SourceProvider?,
    showP2POnly: Boolean,
    sortOption: SourceSortOption,
    onQualitySelected: (SourceQuality?) -> Unit,
    onProviderSelected: (SourceProvider?) -> Unit,
    onP2POnlyChanged: (Boolean) -> Unit,
    onSortChanged: (SourceSortOption) -> Unit
) {
    val availableQualities = remember(sources) {
        sources.map { it.quality }.distinct().sortedByDescending { it.priority }
    }
    
    val availableProviders = remember(sources) {
        sources.map { it.provider }.distinctBy { it.id }.sortedBy { it.displayName }
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quality filter
            FilterSection(
                title = "Quality",
                content = {
                    FilterChipRow(
                        options = listOf(null) + availableQualities,
                        selectedOption = selectedQuality,
                        onOptionSelected = onQualitySelected,
                        optionText = { quality ->
                            quality?.displayName ?: "All"
                        }
                    )
                }
            )
            
            // Provider filter
            FilterSection(
                title = "Provider",
                content = {
                    FilterChipRow(
                        options = listOf(null) + availableProviders,
                        selectedOption = selectedProvider,
                        onOptionSelected = onProviderSelected,
                        optionText = { provider ->
                            provider?.displayName ?: "All"
                        }
                    )
                }
            )
            
            // Additional filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // P2P only toggle
                FilterToggle(
                    label = "P2P sources only",
                    checked = showP2POnly,
                    onCheckedChange = onP2POnlyChanged
                )
                
                // Sort option
                SortDropdown(
                    selectedOption = sortOption,
                    onOptionSelected = onSortChanged
                )
            }
        }
    }
}

/**
 * Filter section with title and content
 */
@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        content()
    }
}

/**
 * Filter chip row
 */
@Composable
private fun <T> FilterChipRow(
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    optionText: (T?) -> String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { option ->
            FilterChip(
                text = optionText(option),
                isSelected = selectedOption == option,
                onClick = { onOptionSelected(option) }
            )
        }
    }
}

/**
 * Filter chip component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    TVFocusIndicator(isFocused = isFocused) {
        FilterChip(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            label = {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            selected = isSelected,
            modifier = Modifier.tvFocusable(
                onFocusChanged = { isFocused = it.isFocused }
            )
        )
    }
}

/**
 * Filter toggle component
 */
@Composable
private fun FilterToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TVFocusIndicator(isFocused = isFocused) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Sort dropdown component
 */
@Composable
private fun SortDropdown(
    selectedOption: SourceSortOption,
    onOptionSelected: (SourceSortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    
    Box {
        TVFocusIndicator(isFocused = isFocused) {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Sort: ${selectedOption.displayName}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SourceSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Sources grid component
 */
@Composable
private fun SourcesGrid(
    sources: List<StreamingSource>,
    selectedSourceId: String?,
    onSourceSelected: (StreamingSource) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sources.isEmpty()) {
        // Empty state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No sources match your filters",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Try adjusting your filter settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(sources) { source ->
                DetailedSourceCard(
                    source = source,
                    onClick = onSourceSelected,
                    isSelected = source.id == selectedSourceId
                )
            }
        }
    }
}

/**
 * Dialog footer
 */
@Composable
private fun SourceDialogFooter(
    totalSources: Int,
    filteredSources: Int,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (filteredSources == totalSources) {
                "$totalSources sources"
            } else {
                "$filteredSources of $totalSources sources"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        TextButton(
            onClick = onDismiss
        ) {
            Text("Done")
        }
    }
}

/**
 * Dialog action button
 */
@Composable
private fun DialogActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    TVFocusIndicator(isFocused = isFocused) {
        IconButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = Modifier.tvFocusable(
                onFocusChanged = { isFocused = it.isFocused }
            ),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = when {
                    isActive -> MaterialTheme.colorScheme.primaryContainer
                    isFocused -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

// SourceSortOption is now imported from com.rdwatch.androidtv.ui.details.models

/**
 * Get sort comparator for sources
 */
private fun getSortComparator(sortOption: SourceSortOption): Comparator<StreamingSource> {
    return when (sortOption) {
        SourceSortOption.PRIORITY -> compareByDescending { it.getPriorityScore() }
        SourceSortOption.QUALITY -> compareByDescending { it.quality.priority }
        SourceSortOption.PROVIDER -> compareBy { it.provider.displayName }
        SourceSortOption.SEEDERS -> compareByDescending { 
            it.features.seeders ?: 0
        }
        SourceSortOption.RELIABILITY -> compareByDescending {
            when (it.sourceType.reliability) {
                com.rdwatch.androidtv.ui.details.models.SourceType.SourceReliability.HIGH -> 3
                com.rdwatch.androidtv.ui.details.models.SourceType.SourceReliability.MEDIUM -> 2
                com.rdwatch.androidtv.ui.details.models.SourceType.SourceReliability.LOW -> 1
                com.rdwatch.androidtv.ui.details.models.SourceType.SourceReliability.UNKNOWN -> 0
            }
        }
        SourceSortOption.AVAILABILITY -> compareByDescending { it.isCurrentlyAvailable() }
        // Handle the new advanced options by falling back to reasonable defaults
        SourceSortOption.QUALITY_SCORE -> compareByDescending { it.getPriorityScore() } // Same as PRIORITY
        SourceSortOption.FILE_SIZE -> compareByDescending { it.size?.toLongOrNull() ?: 0L }
        SourceSortOption.ADDED_DATE -> compareByDescending { it.addedDate ?: "" }
        SourceSortOption.RELEASE_TYPE -> compareBy { it.title ?: "" }
    }
}

/**
 * Preview/Demo configurations for SourceSelectionDialog
 */
object SourceSelectionDialogPreview {
    @Composable
    fun SampleSourceDialog() {
        var showDialog by remember { mutableStateOf(true) }
        var selectedSourceId by remember { mutableStateOf<String?>(null) }
        
        if (showDialog) {
            SourceSelectionDialog(
                sources = StreamingSource.createSampleSources(),
                onSourceSelected = { 
                    selectedSourceId = it.id
                    showDialog = false
                },
                onDismiss = { showDialog = false },
                selectedSourceId = selectedSourceId
            )
        }
    }
}