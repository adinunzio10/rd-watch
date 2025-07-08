package com.rdwatch.androidtv.ui.filebrowser.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Enhanced collapsible filter panel for File Browser
 * TV-optimized with smooth animations and intelligent layout
 */
@Composable
fun EnhancedFilterPanel(
    filterOptions: FilterOptions,
    onFilterChange: (FilterOptions) -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headerFocusRequester = remember { FocusRequester() }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300)
    )
    
    // Auto-focus header when collapsed
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            headerFocusRequester.requestFocus()
        }
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Collapsible header
            FilterPanelHeader(
                isExpanded = isExpanded,
                onToggleExpanded = onToggleExpanded,
                filterOptions = filterOptions,
                focusRequester = headerFocusRequester,
                rotationAngle = rotationAngle
            )
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) + 
                       expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) +
                      shrinkVertically(animationSpec = tween(300))
            ) {
                FilterPanelContent(
                    filterOptions = filterOptions,
                    onFilterChange = onFilterChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FilterPanelHeader(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    filterOptions: FilterOptions,
    focusRequester: FocusRequester,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val activeFilterCount = getActiveFilterCount(filterOptions)
    
    TVFocusIndicator(isFocused = isFocused) {
        Surface(
            onClick = onToggleExpanded,
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            color = if (isFocused) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isExpanded) 0.dp else 16.dp,
                bottomEnd = if (isExpanded) 0.dp else 16.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter icon
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isFocused) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                // Title and filter count
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Filter Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFocused) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    if (activeFilterCount > 0) {
                        Text(
                            text = "$activeFilterCount active filter${if (activeFilterCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFocused) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    } else {
                        Text(
                            text = "No filters applied",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFocused) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
                
                // Active filters preview
                if (activeFilterCount > 0 && !isExpanded) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.width(150.dp)
                    ) {
                        items(getActiveFilterTags(filterOptions).take(3)) { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        if (activeFilterCount > 3) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "+${activeFilterCount - 3}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Expand/collapse arrow
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                    tint = if (isFocused) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterPanelContent(
    filterOptions: FilterOptions,
    onFilterChange: (FilterOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentFilters by remember { mutableStateOf(filterOptions) }
    
    LaunchedEffect(filterOptions) {
        currentFilters = filterOptions
    }
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        // Search filter
        item {
            SearchFilterSection(
                searchQuery = currentFilters.searchQuery,
                onSearchChange = { query ->
                    currentFilters = currentFilters.copy(searchQuery = query)
                    onFilterChange(currentFilters)
                },
                modifier = Modifier.width(280.dp)
            )
        }
        
        // Quick filters (toggles)
        item {
            QuickFiltersSection(
                showOnlyPlayable = currentFilters.showOnlyPlayable,
                showOnlyDownloaded = currentFilters.showOnlyDownloaded,
                onPlayableToggle = { 
                    currentFilters = currentFilters.copy(showOnlyPlayable = it)
                    onFilterChange(currentFilters)
                },
                onDownloadedToggle = { 
                    currentFilters = currentFilters.copy(showOnlyDownloaded = it)
                    onFilterChange(currentFilters)
                },
                modifier = Modifier.width(240.dp)
            )
        }
        
        // File type filters
        item {
            FileTypeFiltersSection(
                selectedTypes = currentFilters.fileTypeFilter,
                onTypeToggle = { fileType ->
                    val newFilter = if (currentFilters.fileTypeFilter.contains(fileType)) {
                        currentFilters.fileTypeFilter - fileType
                    } else {
                        currentFilters.fileTypeFilter + fileType
                    }
                    currentFilters = currentFilters.copy(fileTypeFilter = newFilter)
                    onFilterChange(currentFilters)
                },
                modifier = Modifier.width(320.dp)
            )
        }
        
        // Status filters
        item {
            StatusFiltersSection(
                selectedStatuses = currentFilters.statusFilter,
                onStatusToggle = { status ->
                    val newFilter = if (currentFilters.statusFilter.contains(status)) {
                        currentFilters.statusFilter - status
                    } else {
                        currentFilters.statusFilter + status
                    }
                    currentFilters = currentFilters.copy(statusFilter = newFilter)
                    onFilterChange(currentFilters)
                },
                modifier = Modifier.width(280.dp)
            )
        }
        
        // Action buttons
        item {
            FilterActionButtons(
                hasActiveFilters = getActiveFilterCount(currentFilters) > 0,
                onReset = {
                    currentFilters = FilterOptions()
                    onFilterChange(currentFilters)
                },
                onApply = {
                    onFilterChange(currentFilters)
                },
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
private fun SearchFilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        TVFocusIndicator(isFocused = isFocused) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused }
                    ),
                placeholder = {
                    Text("Search files and folders...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        var clearFocused by remember { mutableStateOf(false) }
                        
                        TVFocusIndicator(isFocused = clearFocused) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.tvFocusable(
                                    onFocusChanged = { clearFocused = it.isFocused }
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = if (clearFocused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
        }
    }
}

@Composable
private fun QuickFiltersSection(
    showOnlyPlayable: Boolean,
    showOnlyDownloaded: Boolean,
    onPlayableToggle: (Boolean) -> Unit,
    onDownloadedToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Quick Filters",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterToggleChip(
                text = "Playable Only",
                icon = Icons.Default.PlayArrow,
                isSelected = showOnlyPlayable,
                onToggle = onPlayableToggle,
                modifier = Modifier.weight(1f)
            )
            
            FilterToggleChip(
                text = "Downloaded",
                icon = Icons.Default.Download,
                isSelected = showOnlyDownloaded,
                onToggle = onDownloadedToggle,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FileTypeFiltersSection(
    selectedTypes: Set<FileType>,
    onTypeToggle: (FileType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "File Types",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(FileType.values().filter { it != FileType.OTHER }) { fileType ->
                FilterToggleChip(
                    text = fileType.displayName,
                    icon = getFileTypeIcon(fileType),
                    isSelected = selectedTypes.contains(fileType),
                    onToggle = { onTypeToggle(fileType) }
                )
            }
        }
    }
}

@Composable
private fun StatusFiltersSection(
    selectedStatuses: Set<FileStatus>,
    onStatusToggle: (FileStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "File Status",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(FileStatus.values()) { status ->
                FilterToggleChip(
                    text = status.name.lowercase().replace("_", " ")
                        .replaceFirstChar { it.titlecase() },
                    icon = getFileStatusIcon(status),
                    isSelected = selectedStatuses.contains(status),
                    onToggle = { onStatusToggle(status) }
                )
            }
        }
    }
}

@Composable
private fun FilterToggleChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        Surface(
            onClick = { onToggle(!isSelected) },
            modifier = modifier.tvFocusable(
                onFocusChanged = { isFocused = it.isFocused }
            ),
            shape = RoundedCornerShape(20.dp),
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isFocused -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (isFocused) 4.dp else 2.dp,
            border = if (isFocused && !isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterActionButtons(
    hasActiveFilters: Boolean,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Reset button
        var resetFocused by remember { mutableStateOf(false) }
        
        TVFocusIndicator(isFocused = resetFocused) {
            OutlinedButton(
                onClick = onReset,
                enabled = hasActiveFilters,
                modifier = Modifier
                    .weight(1f)
                    .tvFocusable(
                        onFocusChanged = { resetFocused = it.isFocused }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
        }
        
        // Apply button
        var applyFocused by remember { mutableStateOf(false) }
        
        TVFocusIndicator(isFocused = applyFocused) {
            Button(
                onClick = onApply,
                modifier = Modifier
                    .weight(1f)
                    .tvFocusable(
                        onFocusChanged = { applyFocused = it.isFocused }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply")
            }
        }
    }
}

// Helper functions
private fun getActiveFilterCount(filterOptions: FilterOptions): Int {
    var count = 0
    if (filterOptions.showOnlyPlayable) count++
    if (filterOptions.showOnlyDownloaded) count++
    if (filterOptions.fileTypeFilter.isNotEmpty()) count += filterOptions.fileTypeFilter.size
    if (filterOptions.statusFilter.isNotEmpty()) count += filterOptions.statusFilter.size
    if (filterOptions.searchQuery.isNotBlank()) count++
    return count
}

private fun getActiveFilterTags(filterOptions: FilterOptions): List<String> {
    val tags = mutableListOf<String>()
    if (filterOptions.showOnlyPlayable) tags.add("Playable")
    if (filterOptions.showOnlyDownloaded) tags.add("Downloaded")
    if (filterOptions.searchQuery.isNotBlank()) tags.add("Search")
    filterOptions.fileTypeFilter.forEach { tags.add(it.displayName) }
    filterOptions.statusFilter.forEach { tags.add(it.name) }
    return tags
}

private fun getFileTypeIcon(fileType: FileType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (fileType) {
        FileType.VIDEO -> Icons.Default.PlayArrow
        FileType.AUDIO -> Icons.Default.MusicNote
        FileType.DOCUMENT -> Icons.Default.Description
        FileType.IMAGE -> Icons.Default.Image
        FileType.ARCHIVE -> Icons.Default.FolderZip
        FileType.SUBTITLE -> Icons.Default.Subtitles
        FileType.OTHER -> Icons.Default.Description
    }
}

private fun getFileStatusIcon(status: FileStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        FileStatus.READY -> Icons.Default.CheckCircle
        FileStatus.DOWNLOADING -> Icons.Default.Download
        FileStatus.ERROR -> Icons.Default.Error
        FileStatus.UNAVAILABLE -> Icons.Default.CloudOff
    }
}