package com.rdwatch.androidtv.ui.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.details.models.advanced.*
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
    modifier: Modifier = Modifier,
) {
    val groupedSources = sources.groupBy { it.provider }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    focusGroup = focusGroup,
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                focusGroup = focusGroup,
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(sources) { index, source ->
            SourceCompactItem(
                source = source,
                isSelected = source == selectedSource,
                index = index,
                onSourceSelected = onSourceSelected,
                onPlaySource = onPlaySource,
                focusGroup = focusGroup,
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
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            // Group header
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onGroupToggle() }
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        },
                border =
                    if (isFocused) {
                        BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
                    } else {
                        null
                    },
                color =
                    if (isFocused) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Provider icon/logo placeholder
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = provider.displayName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Column {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${sources.size} sources • ${provider.type.name.lowercase()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Expand/collapse icon
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                    )
                }
            }

            // Expanded source grid
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sources) { source ->
                        SourceCard(
                            source = source,
                            isSelected = source == selectedSource,
                            onSourceSelected = onSourceSelected,
                            onPlaySource = onPlaySource,
                            onDownloadSource = onDownloadSource,
                            onAddToPlaylist = onAddToPlaylist,
                            focusGroup = focusGroup,
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
                focusRequester = focusRequester,
            ),
        )
    }
}

/**
 * Individual source card for grid view
 * Enhanced with provider prominence, reliability indicators, and practical decision factors
 */
@Composable
private fun SourceCard(
    source: SourceMetadata,
    isSelected: Boolean,
    onSourceSelected: (SourceMetadata) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    onDownloadSource: (SourceMetadata) -> Unit,
    onAddToPlaylist: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp) // Increased height for more content
                .clickable { onSourceSelected(source) }
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
        border =
            if (isFocused || isSelected) {
                BorderStroke(
                    3.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            } else {
                null
            },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header: Tracker name (more useful than provider name for Real-Debrid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Show tracker name instead of redundant "Torrentio RD"
                val trackerName = source.release.group ?: source.provider.displayName
                Text(
                    text = trackerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        when (trackerName.uppercase()) {
                            "YTS", "YIFY" -> Color(0xFF059669) // Green for YTS (efficient)
                            "EZTV", "ETTV" -> Color(0xFF3B82F6) // Blue for EZTV (TV specialist)
                            "RARBG" -> Color(0xFF7C3AED) // Purple for RARBG (premium)
                            "1337X", "LEET" -> Color(0xFF0891B2) // Cyan for 1337x (variety)
                            else -> MaterialTheme.colorScheme.onSurface // Default for unknown trackers
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Show tracker tier instead of reliability stars
                source.release.group?.let { trackerName ->
                    val tierText =
                        when (trackerName.uppercase()) {
                            "YTS", "YIFY" -> "COMPACT"
                            "EZTV", "ETTV" -> "TV"
                            "RARBG" -> "PREMIUM"
                            "1337X", "LEET", "THEPIRATEBAY", "TPB" -> "VARIETY"
                            else -> "SCENE"
                        }
                    Text(
                        text = tierText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Prominent file size display
            source.file.sizeInBytes?.let { sizeBytes ->
                val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
                val (sizeText, sizeColor) =
                    when {
                        sizeGB < 1.0 -> String.format("%.0f MB", sizeBytes / (1024.0 * 1024.0)) to Color(0xFF10B981)
                        sizeGB < 8.0 -> String.format("%.1f GB", sizeGB) to Color(0xFF3B82F6)
                        sizeGB < 15.0 -> String.format("%.1f GB", sizeGB) to Color(0xFFF59E0B)
                        else -> String.format("%.1f GB", sizeGB) to Color(0xFFEF4444)
                    }

                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = sizeColor,
                )
            }

            // Quality badges - prioritized for grid view
            CompactBadgeRow(
                sourceMetadata = source,
                maxBadges = 4,
                viewMode = "grid",
            )

            // Release quality indicator (more useful than availability status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Release type (REMUX, WEB-DL, etc.)
                Text(
                    text = source.release.type.shortName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        when (source.release.type.qualityBonus) {
                            in 90..Int.MAX_VALUE -> Color(0xFF7C3AED) // Purple for premium (REMUX)
                            in 70..89 -> Color(0xFF2563EB) // Blue for high quality (BluRay, WEB-DL)
                            in 50..69 -> Color(0xFF059669) // Green for good quality
                            else -> Color(0xFF6B7280) // Gray for lower quality
                        },
                )

                // Additional space for future enhancement
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Quick actions on focus
            if (isFocused) {
                SourceQuickActions(
                    source = source,
                    onPlay = onPlaySource,
                    onDownload = onDownloadSource,
                    onAddToPlaylist = onAddToPlaylist,
                    focusGroup = focusGroup,
                    compact = true,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "source_card_${source.id}",
                focusRequester = focusRequester,
            ),
        )
    }
}

/**
 * Individual source list item for list view
 * Enhanced with improved horizontal layout and better information hierarchy
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
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(100.dp) // Fixed height for consistency
                .clickable { onSourceSelected(source) }
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
        border =
            if (isFocused || isSelected) {
                BorderStroke(
                    3.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            } else {
                null
            },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left side - tracker info (more useful than provider reliability)
            Column(
                modifier = Modifier.width(140.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Show tracker name instead of redundant "Torrentio RD"
                val trackerName = source.release.group ?: source.provider.displayName
                Text(
                    text = trackerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        when (trackerName.uppercase()) {
                            "YTS", "YIFY" -> Color(0xFF059669) // Green for YTS (efficient)
                            "EZTV", "ETTV" -> Color(0xFF3B82F6) // Blue for EZTV (TV specialist)
                            "RARBG" -> Color(0xFF7C3AED) // Purple for RARBG (premium)
                            "1337X", "LEET" -> Color(0xFF0891B2) // Cyan for 1337x (variety)
                            else -> MaterialTheme.colorScheme.onSurface // Default for unknown trackers
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Show tracker characteristic instead of reliability stars
                source.release.group?.let { trackerName ->
                    val characteristic =
                        when (trackerName.uppercase()) {
                            "YTS", "YIFY" -> "Small files"
                            "EZTV", "ETTV" -> "TV specialist"
                            "RARBG" -> "High quality"
                            "1337X", "LEET", "THEPIRATEBAY", "TPB" -> "General"
                            else -> "Scene"
                        }
                    Text(
                        text = characteristic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Center - enhanced metadata
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Top row: File size (resolution now shown in badges)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    source.file.sizeInBytes?.let { sizeBytes ->
                        val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
                        val (sizeText, sizeColor) =
                            when {
                                sizeGB < 1.0 -> String.format("%.0f MB", sizeBytes / (1024.0 * 1024.0)) to Color(0xFF10B981)
                                sizeGB < 8.0 -> String.format("%.1f GB", sizeGB) to Color(0xFF3B82F6)
                                sizeGB < 15.0 -> String.format("%.1f GB", sizeGB) to Color(0xFFF59E0B)
                                else -> String.format("%.1f GB", sizeGB) to Color(0xFFEF4444)
                            }

                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = sizeColor,
                        )
                    }
                }

                // Bottom row: Priority badges
                CompactBadgeRow(
                    sourceMetadata = source,
                    maxBadges = 5,
                    viewMode = "list",
                )
            }

            // Right side - status and actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Release type indicator (more useful than availability status)
                Text(
                    text = source.release.type.shortName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                        when (source.release.type.qualityBonus) {
                            in 90..Int.MAX_VALUE -> Color(0xFF7C3AED) // Purple for premium (REMUX)
                            in 70..89 -> Color(0xFF2563EB) // Blue for high quality (BluRay, WEB-DL)
                            in 50..69 -> Color(0xFF059669) // Green for good quality
                            else -> Color(0xFF6B7280) // Gray for lower quality
                        },
                )

                // Actions
                if (isFocused) {
                    SourceQuickActions(
                        source = source,
                        onPlay = onPlaySource,
                        onDownload = onDownloadSource,
                        onAddToPlaylist = onAddToPlaylist,
                        focusGroup = focusGroup,
                        compact = true,
                    )
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "source_item_$index",
                focusRequester = focusRequester,
            ),
        )
    }
}

/**
 * Compact source item for compact view
 * Enhanced with essential decision-making information in minimal space
 */
@Composable
private fun SourceCompactItem(
    source: SourceMetadata,
    isSelected: Boolean,
    index: Int,
    onSourceSelected: (SourceMetadata) -> Unit,
    onPlaySource: (SourceMetadata) -> Unit,
    focusGroup: com.rdwatch.androidtv.ui.focus.TVFocusGroup,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp) // Fixed height for consistency
                .clickable { onSourceSelected(source) }
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
        border =
            if (isFocused || isSelected) {
                BorderStroke(
                    3.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            } else {
                null
            },
        color =
            when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else -> Color.Transparent
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: Tracker name (more useful than provider + reliability)
            Row(
                modifier = Modifier.width(100.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Show tracker name instead of redundant "Torrentio RD"
                val trackerName = source.release.group ?: source.provider.displayName
                Text(
                    text = trackerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        when (trackerName.uppercase()) {
                            "YTS", "YIFY" -> Color(0xFF059669) // Green for YTS (efficient)
                            "EZTV", "ETTV" -> Color(0xFF3B82F6) // Blue for EZTV (TV specialist)
                            "RARBG" -> Color(0xFF7C3AED) // Purple for RARBG (premium)
                            "1337X", "LEET" -> Color(0xFF0891B2) // Cyan for 1337x (variety)
                            else -> MaterialTheme.colorScheme.onSurface // Default for unknown trackers
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // Center: Essential info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // File size with color coding (resolution now shown in badges)
                source.file.sizeInBytes?.let { sizeBytes ->
                    val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
                    val (sizeText, sizeColor) =
                        when {
                            sizeGB < 1.0 -> String.format("%.0fM", sizeBytes / (1024.0 * 1024.0)) to Color(0xFF10B981)
                            sizeGB < 8.0 -> String.format("%.1fG", sizeGB) to Color(0xFF3B82F6)
                            sizeGB < 15.0 -> String.format("%.1fG", sizeGB) to Color(0xFFF59E0B)
                            else -> String.format("%.1fG", sizeGB) to Color(0xFFEF4444)
                        }

                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = sizeColor,
                    )
                }

                // Most critical badge only
                val priorityBadges = source.getQualityBadges().take(1)
                if (priorityBadges.isNotEmpty()) {
                    AdvancedQualityBadgeComponent(
                        badge = priorityBadges.first(),
                        size = QualityBadgeSize.SMALL,
                    )
                }
            }

            // Right: Play action only
            IconButton(
                onClick = { onPlaySource(source) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "source_compact_$index",
                focusRequester = focusRequester,
            ),
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
    compact: Boolean = false,
) {
    val playFocusRequester = remember { FocusRequester() }
    val downloadFocusRequester = remember { FocusRequester() }
    val playlistFocusRequester = remember { FocusRequester() }

    val buttonSize = if (compact) 32.dp else 40.dp
    val iconSize = if (compact) 16.dp else 20.dp

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Play button
        IconButton(
            onClick = { onPlay(source) },
            modifier =
                Modifier
                    .size(buttonSize)
                    .onFocusChanged { focusState ->
                        // Handle focus state if needed
                    },
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        // Download button
        IconButton(
            onClick = { onDownload(source) },
            modifier =
                Modifier
                    .size(buttonSize)
                    .onFocusChanged { focusState ->
                        // Handle focus state if needed
                    },
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }

        // Add to playlist button
        IconButton(
            onClick = { onAddToPlaylist(source) },
            modifier =
                Modifier
                    .size(buttonSize)
                    .onFocusChanged { focusState ->
                        // Handle focus state if needed
                    },
        ) {
            Icon(
                Icons.Default.PlaylistAdd,
                contentDescription = "Add to Playlist",
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
    }

    LaunchedEffect(Unit) {
        focusGroup.addItem(
            TVFocusItem(
                id = "action_play_${source.id}",
                focusRequester = playFocusRequester,
            ),
        )
        focusGroup.addItem(
            TVFocusItem(
                id = "action_download_${source.id}",
                focusRequester = downloadFocusRequester,
            ),
        )
        focusGroup.addItem(
            TVFocusItem(
                id = "action_playlist_${source.id}",
                focusRequester = playlistFocusRequester,
            ),
        )
    }
}

/**
 * Provider reliability badge
 */
@Composable
private fun ProviderReliabilityBadge(reliability: SourceProviderInfo.ProviderReliability) {
    val (color, text) =
        when (reliability) {
            SourceProviderInfo.ProviderReliability.EXCELLENT -> Color(0xFF10B981) to "★★★"
            SourceProviderInfo.ProviderReliability.GOOD -> Color(0xFF3B82F6) to "★★"
            SourceProviderInfo.ProviderReliability.FAIR -> Color(0xFFF59E0B) to "★"
            SourceProviderInfo.ProviderReliability.POOR -> Color(0xFFEF4444) to "!"
            SourceProviderInfo.ProviderReliability.UNKNOWN -> Color(0xFF6B7280) to "?"
        }

    Box(
        modifier =
            Modifier
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}
