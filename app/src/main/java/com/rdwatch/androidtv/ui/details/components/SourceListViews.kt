package com.rdwatch.androidtv.ui.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.rdwatch.androidtv.ui.details.models.advanced.*
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.ui.focus.TVFocusItem

/**
 * Grid view for sources with cards layout
 */
@Composable
fun SourceGridView(
    sources: List<SourceMetadata>,
    selectedSource: SourceMetadata?,
    expandedGroups: Set<String>,
    onSourceSelected: (SourceMetadata) -> Unit,
    onGroupToggle: (String) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    onDownloadSource: (SourceMetadata) -> Unit,
    onAddToPlaylist: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup,
    modifier: Modifier = Modifier
) {
    val groupedSources = sources.groupBy { it.provider }
    
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedSources.forEach { (provider, providerSources) ->
            item {
                ProviderGroupCard(
                    provider = provider,
                    sources = providerSources,
                    isExpanded = expandedGroups.contains(provider.id),
                    selectedSource = selectedSource,
                    onGroupToggle = { onGroupToggle(provider.id) },
                    onSourceSelected = onSourceSelected,
                    onPlaySource = onPlaySource,
                    onDownloadSource = onDownloadSource,
                    onAddToPlaylist = onAddToPlaylist,
                    focusGroup = focusGroup
                )
            }
        }
    }
}

/**
 * List view for sources with detailed information
 */
@Composable
fun SourceListView(
    sources: List<SourceMetadata>,
    selectedSource: SourceMetadata?,
    expandedGroups: Set<String>,
    onSourceSelected: (SourceMetadata) -> Unit,
    onGroupToggle: (String) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    onDownloadSource: (SourceMetadata) -> Unit,
    onAddToPlaylist: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(sources) { index, source ->
            SourceListItem(
                source = source,
                isSelected = source == selectedSource,
                index = index,
                onSourceSelected = onSourceSelected,
                onPlaySource = onPlaySource,
                onDownloadSource = onDownloadSource,
                onAddToPlaylist = onAddToPlaylist,
                focusGroup = focusGroup
            )
        }
    }
}

/**
 * Compact view for sources with minimal information
 */
@Composable
fun SourceCompactView(
    sources: List<SourceMetadata>,
    selectedSource: SourceMetadata?,
    onSourceSelected: (SourceMetadata) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(sources) { index, source ->
            SourceCompactItem(
                source = source,
                isSelected = source == selectedSource,
                index = index,
                onSourceSelected = onSourceSelected,
                onPlaySource = onPlaySource,
                focusGroup = focusGroup
            )
        }
    }
}

/**
 * Provider group card with expandable source list
 */
@Composable
private fun ProviderGroupCard(
    provider: SourceProviderInfo,
    sources: List<SourceMetadata>,
    isExpanded: Boolean,
    selectedSource: SourceMetadata?,
    onGroupToggle: () -> Unit,
    onSourceSelected: (SourceMetadata) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    onDownloadSource: (SourceMetadata) -> Unit,
    onAddToPlaylist: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Group header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGroupToggle() }
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
                                RoundedCornerShape(12.dp)
                            )
                        } else Modifier
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Provider icon/logo placeholder
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = provider.displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Column {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sources.size} sources • ${provider.type.name.lowercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reliability indicator
                    ProviderReliabilityBadge(reliability = provider.reliability)
                    
                    // Expand/collapse icon
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            // Expanded source grid
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sources) { source ->
                        SourceCard(
                            source = source,
                            isSelected = source == selectedSource,
                            onSourceSelected = onSourceSelected,
                            onPlaySource = onPlaySource,
                            onDownloadSource = onDownloadSource,
                            onAddToPlaylist = onAddToPlaylist,
                            focusGroup = focusGroup
                        )
                    }
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "provider_${provider.id}",
                focusRequester = focusRequester
            )
        )
    }
}

/**
 * Individual source card for grid view
 */
@Composable
private fun SourceCard(
    source: SourceMetadata,
    isSelected: Boolean,
    onSourceSelected: (SourceMetadata) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    onDownloadSource: (SourceMetadata) -> Unit,
    onAddToPlaylist: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSourceSelected(source) }
            .tvFocusable(
                enabled = true,
                focusRequester = focusRequester,
                onFocusChanged = { isFocused = it.isFocused }
            )
            .then(
                if (isFocused || isSelected) {
                    Modifier.border(
                        2.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quality and badges
            QualityBadgeRow(
                badges = source.getQualityBadges(),
                maxVisible = 4,
                badgeSize = QualityBadgeSize.SMALL
            )
            
            // File info
            source.file.getFormattedSize()?.let { size ->
                Text(
                    text = size,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Health info for P2P
            source.health.seeders?.let { seeders ->
                Text(
                    text = "${seeders}S/${source.health.leechers ?: 0}L",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        seeders > 100 -> Color(0xFF10B981)
                        seeders > 50 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                )
            }
            
            // Quick actions
            if (isFocused) {
                SourceQuickActions(
                    source = source,
                    onPlay = onPlaySource,
                    onDownload = onDownloadSource,
                    onAddToPlaylist = onAddToPlaylist,
                    focusGroup = focusGroup,
                    compact = true
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "source_card_${source.id}",
                focusRequester = focusRequester
            )
        )
    }
}

/**
 * Individual source list item for list view
 */
@Composable
private fun SourceListItem(
    source: SourceMetadata,
    isSelected: Boolean,
    index: Int,
    onSourceSelected: (SourceMetadata) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    onDownloadSource: (SourceMetadata) -> Unit,
    onAddToPlaylist: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSourceSelected(source) }
            .tvFocusable(
                enabled = true,
                focusRequester = focusRequester,
                onFocusChanged = { isFocused = it.isFocused }
            )
            .then(
                if (isFocused || isSelected) {
                    Modifier.border(
                        2.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        RoundedCornerShape(8.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - source info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Provider and quality
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = source.provider.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ProviderReliabilityBadge(reliability = source.provider.reliability)
                }
                
                // Quality badges
                QualityBadgeRow(
                    badges = source.getQualityBadges(),
                    maxVisible = 6,
                    badgeSize = QualityBadgeSize.SMALL
                )
                
                // File and health info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    source.file.getFormattedSize()?.let { size ->
                        Text(
                            text = "Size: $size",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    source.health.seeders?.let { seeders ->
                        Text(
                            text = "Health: ${seeders}S/${source.health.leechers ?: 0}L",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                seeders > 100 -> Color(0xFF10B981)
                                seeders > 50 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }
                }
            }
            
            // Right side - actions
            if (isFocused) {
                SourceQuickActions(
                    source = source,
                    onPlay = onPlaySource,
                    onDownload = onDownloadSource,
                    onAddToPlaylist = onAddToPlaylist,
                    focusGroup = focusGroup,
                    compact = false
                )
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "source_item_${index}",
                focusRequester = focusRequester
            )
        )
    }
}

/**
 * Compact source item for compact view
 */
@Composable
private fun SourceCompactItem(
    source: SourceMetadata,
    isSelected: Boolean,
    index: Int,
    onSourceSelected: (SourceMetadata) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSourceSelected(source) }
            .tvFocusable(
                enabled = true,
                focusRequester = focusRequester,
                onFocusChanged = { isFocused = it.isFocused }
            )
            .then(
                if (isFocused || isSelected) {
                    Modifier.border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        RoundedCornerShape(4.dp)
                    )
                } else Modifier
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - minimal info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source.provider.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = source.quality.resolution.shortName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                source.file.getFormattedSize()?.let { size ->
                    Text(
                        text = size,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Right side - play button
            IconButton(
                onClick = { onPlaySource(source) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "source_compact_${index}",
                focusRequester = focusRequester
            )
        )
    }
}

/**
 * Quick action buttons for sources
 */
@Composable
private fun SourceQuickActions(
    source: SourceMetadata,
    onPlay: (SourceMetadata) -> Unit,
    onDownload: (SourceMetadata) -> Unit,
    onAddToPlaylist: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup,
    compact: Boolean = false
) {
    val playFocusRequester = remember { FocusRequester() }
    val downloadFocusRequester = remember { FocusRequester() }
    val playlistFocusRequester = remember { FocusRequester() }
    
    val buttonSize = if (compact) 32.dp else 40.dp
    val iconSize = if (compact) 16.dp else 20.dp
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Play button
        IconButton(
            onClick = { onPlay(source) },
            modifier = Modifier
                .size(buttonSize)
                .tvFocusable(
                    enabled = true,
                    focusRequester = playFocusRequester
                )
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Download button
        IconButton(
            onClick = { onDownload(source) },
            modifier = Modifier
                .size(buttonSize)
                .tvFocusable(
                    enabled = true,
                    focusRequester = downloadFocusRequester
                )
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        
        // Add to playlist button
        IconButton(
            onClick = { onAddToPlaylist(source) },
            modifier = Modifier
                .size(buttonSize)
                .tvFocusable(
                    enabled = true,
                    focusRequester = playlistFocusRequester
                )
        ) {
            Icon(
                Icons.Default.PlaylistAdd,
                contentDescription = "Add to Playlist",
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
    
    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "action_play_${source.id}",
                focusRequester = playFocusRequester
            )
        )
        focusGroup.addItem(
            TVFocusItem(
                id = "action_download_${source.id}",
                focusRequester = downloadFocusRequester
            )
        )
        focusGroup.addItem(
            TVFocusItem(
                id = "action_playlist_${source.id}",
                focusRequester = playlistFocusRequester
            )
        )
    }
}

/**
 * Provider reliability badge
 */
@Composable
private fun ProviderReliabilityBadge(
    reliability: SourceProviderInfo.ProviderReliability
) {
    val (color, text) = when (reliability) {
        SourceProviderInfo.ProviderReliability.EXCELLENT -> Color(0xFF10B981) to "★★★"
        SourceProviderInfo.ProviderReliability.GOOD -> Color(0xFF3B82F6) to "★★"
        SourceProviderInfo.ProviderReliability.FAIR -> Color(0xFFF59E0B) to "★"
        SourceProviderInfo.ProviderReliability.POOR -> Color(0xFFEF4444) to "!"
        SourceProviderInfo.ProviderReliability.UNKNOWN -> Color(0xFF6B7280) to "?"
    }
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}