package com.rdwatch.androidtv.ui.details.components

import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata
import com.rdwatch.androidtv.ui.focus.tvFocusable
import kotlinx.coroutines.delay

/**
 * Enhanced source container with comprehensive metadata display
 * Supports multiple view modes and progressive disclosure for TV navigation
 */
@Composable
fun EnhancedSourceContainer(
    sources: List<SourceMetadata>,
    modifier: Modifier = Modifier,
    displayMode: SourceDisplayMode = SourceDisplayMode.COMPACT,
    onSourceSelect: (SourceMetadata) -> Unit,
    onSourcePlay: (SourceMetadata) -> Unit,
    selectedSourceId: String? = null,
    showMetadataTooltips: Boolean = true,
    maxDisplayedSources: Int = 10,
) {
    var currentDisplayMode by remember { mutableStateOf(displayMode) }
    val listState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header with view controls
        SourceContainerHeader(
            totalSources = sources.size,
            displayMode = currentDisplayMode,
            onDisplayModeChange = { currentDisplayMode = it },
            maxDisplayed = maxDisplayedSources,
        )

        // Source list with different display modes
        when (currentDisplayMode) {
            SourceDisplayMode.COMPACT -> {
                CompactSourceList(
                    sources = sources.take(maxDisplayedSources),
                    selectedSourceId = selectedSourceId,
                    onSourceSelect = onSourceSelect,
                    onSourcePlay = onSourcePlay,
                    showTooltips = showMetadataTooltips,
                    listState = listState,
                )
            }
            SourceDisplayMode.DETAILED -> {
                DetailedSourceList(
                    sources = sources.take(maxDisplayedSources),
                    selectedSourceId = selectedSourceId,
                    onSourceSelect = onSourceSelect,
                    onSourcePlay = onSourcePlay,
                    showTooltips = showMetadataTooltips,
                    listState = listState,
                )
            }
            SourceDisplayMode.GRID -> {
                GridSourceList(
                    sources = sources.take(maxDisplayedSources),
                    selectedSourceId = selectedSourceId,
                    onSourceSelect = onSourceSelect,
                    onSourcePlay = onSourcePlay,
                    showTooltips = showMetadataTooltips,
                )
            }
        }

        // Load more indicator if there are more sources
        if (sources.size > maxDisplayedSources) {
            LoadMoreIndicator(
                remainingCount = sources.size - maxDisplayedSources,
                onLoadMore = { /* Implement load more logic */ },
            )
        }
    }
}

/**
 * Container header with display mode controls
 */
@Composable
private fun SourceContainerHeader(
    totalSources: Int,
    displayMode: SourceDisplayMode,
    onDisplayModeChange: (SourceDisplayMode) -> Unit,
    maxDisplayed: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Source count and quality indicator
        Column {
            Text(
                text = "Available Sources",
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$totalSources sources found",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Display mode toggle buttons
        DisplayModeToggle(
            currentMode = displayMode,
            onModeChange = onDisplayModeChange,
        )
    }
}

/**
 * Display mode toggle component
 */
@Composable
private fun DisplayModeToggle(
    currentMode: SourceDisplayMode,
    onModeChange: (SourceDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
        ) {
            SourceDisplayMode.entries.forEach { mode ->
                val isSelected = mode == currentMode

                Surface(
                    modifier =
                        Modifier
                            .clickable { onModeChange(mode) }
                            .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                ) {
                    Text(
                        text = mode.displayName,
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * Compact source list for space-efficient display
 */
@Composable
private fun CompactSourceList(
    sources: List<SourceMetadata>,
    selectedSourceId: String?,
    onSourceSelect: (SourceMetadata) -> Unit,
    onSourcePlay: (SourceMetadata) -> Unit,
    showTooltips: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.heightIn(max = 400.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(sources, key = { it.id }) { source ->
            CompactSourceItem(
                sourceMetadata = source,
                isSelected = source.id == selectedSourceId,
                onSelect = { onSourceSelect(source) },
                onPlay = { onSourcePlay(source) },
                showTooltips = showTooltips,
            )
        }
    }
}

/**
 * Detailed source list with expanded metadata
 */
@Composable
private fun DetailedSourceList(
    sources: List<SourceMetadata>,
    selectedSourceId: String?,
    onSourceSelect: (SourceMetadata) -> Unit,
    onSourcePlay: (SourceMetadata) -> Unit,
    showTooltips: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.heightIn(max = 600.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(sources, key = { it.id }) { source ->
            DetailedSourceItem(
                sourceMetadata = source,
                isSelected = source.id == selectedSourceId,
                onSelect = { onSourceSelect(source) },
                onPlay = { onSourcePlay(source) },
                showTooltips = showTooltips,
            )
        }
    }
}

/**
 * Grid source list for visual browsing
 */
@Composable
private fun GridSourceList(
    sources: List<SourceMetadata>,
    selectedSourceId: String?,
    onSourceSelect: (SourceMetadata) -> Unit,
    onSourcePlay: (SourceMetadata) -> Unit,
    showTooltips: Boolean,
    modifier: Modifier = Modifier,
) {
    // For TV, we'll use a 2-column grid for better navigation
    val chunkedSources = sources.chunked(2)

    LazyColumn(
        modifier = modifier.heightIn(max = 500.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(items = chunkedSources) { rowSources ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowSources.forEach { source ->
                    GridSourceItem(
                        sourceMetadata = source,
                        isSelected = source.id == selectedSourceId,
                        onSelect = { onSourceSelect(source) },
                        onPlay = { onSourcePlay(source) },
                        showTooltips = showTooltips,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Fill remaining space if odd number of items in last row
                if (rowSources.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Compact source item component
 */
@Composable
private fun CompactSourceItem(
    sourceMetadata: SourceMetadata,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    showTooltips: Boolean,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(80.dp)
                .tvFocusable(
                    enabled = true,
                    onFocusChanged = { isFocused = it.isFocused },
                    onKeyEvent = null,
                )
                .border(
                    width = if (isFocused || isSelected) 2.dp else 0.dp,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else if (isFocused) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        } else {
                            Color.Transparent
                        },
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable {
                    onSelect()
                    if (showTooltips) showTooltip = true
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isFocused -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 6.dp else 2.dp,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Provider icon
            ProviderIcon(
                provider = sourceMetadata.provider,
                modifier = Modifier.size(32.dp),
            )

            // Main content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Title and size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = sourceMetadata.quality.getDisplayText(),
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    sourceMetadata.file.getFormattedSize()?.let { size ->
                        Text(
                            text = size,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Essential metadata badges
                EssentialMetadataRow(
                    sourceMetadata = sourceMetadata,
                    maxItems = 4,
                )
            }

            // Quality score
            QualityScoreIndicator(
                score = sourceMetadata.getQualityScore(),
                modifier = Modifier.size(40.dp),
            )
        }
    }

    // Tooltip
    if (showTooltips && showTooltip) {
        LaunchedEffect(showTooltip) {
            delay(3000) // Auto-dismiss after 3 seconds
            showTooltip = false
        }

        MetadataTooltip(
            sourceMetadata = sourceMetadata,
            isVisible = showTooltip,
            onDismiss = { showTooltip = false },
            tooltipType = TooltipType.QUICK_INFO,
        )
    }
}

/**
 * Detailed source item component
 */
@Composable
private fun DetailedSourceItem(
    sourceMetadata: SourceMetadata,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    showTooltips: Boolean,
    modifier: Modifier = Modifier,
) {
    ExpandableSourceMetadata(
        sourceMetadata = sourceMetadata,
        modifier = modifier.clickable { onSelect() },
        initiallyExpanded = isSelected,
        showTooltips = showTooltips,
        maxCollapsedItems = 5,
    )
}

/**
 * Grid source item component
 */
@Composable
private fun GridSourceItem(
    sourceMetadata: SourceMetadata,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    showTooltips: Boolean,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier =
            modifier
                .aspectRatio(1.5f)
                .tvFocusable(
                    enabled = true,
                    onFocusChanged = { isFocused = it.isFocused },
                    onKeyEvent = null,
                )
                .border(
                    width = if (isFocused || isSelected) 2.dp else 0.dp,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else if (isFocused) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        } else {
                            Color.Transparent
                        },
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable { onSelect() },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isFocused -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 8.dp else 4.dp,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Provider and quality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProviderIcon(
                    provider = sourceMetadata.provider,
                    modifier = Modifier.size(28.dp),
                )

                QualityScoreIndicator(
                    score = sourceMetadata.getQualityScore(),
                    modifier = Modifier.size(36.dp),
                )
            }

            // Quality text
            Text(
                text = sourceMetadata.quality.getDisplayText(),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Compact badges
            CompactBadgeRow(
                sourceMetadata = sourceMetadata,
                maxBadges = 3,
            )

            Spacer(modifier = Modifier.weight(1f))

            // File size and provider
            Column {
                sourceMetadata.file.getFormattedSize()?.let { size ->
                    Text(
                        text = size,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = sourceMetadata.provider.displayName,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Load more indicator component
 */
@Composable
private fun LoadMoreIndicator(
    remainingCount: Int,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .tvFocusable(
                    enabled = true,
                    onFocusChanged = { isFocused = it.isFocused },
                    onKeyEvent = null,
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable { onLoadMore() },
        shape = RoundedCornerShape(8.dp),
        color =
            if (isFocused) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Load $remainingCount more sources",
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                color =
                    if (isFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

/**
 * Source display mode enumeration
 */
enum class SourceDisplayMode(val displayName: String) {
    COMPACT("Compact"),
    DETAILED("Detailed"),
    GRID("Grid"),
}
