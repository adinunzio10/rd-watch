package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import com.rdwatch.androidtv.ui.details.models.SourceProvider
import com.rdwatch.androidtv.ui.details.models.SourceQuality
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Source selection section for content detail screens
 * Displays available streaming sources with horizontal scrolling and TV navigation
 */
@Composable
fun SourceSelectionSection(
    sources: List<StreamingSource>,
    onSourceSelected: (StreamingSource) -> Unit,
    modifier: Modifier = Modifier,
    selectedSourceId: String? = null,
    showSectionTitle: Boolean = true,
    showViewAllButton: Boolean = true,
    onViewAllClick: () -> Unit = {},
    maxVisibleSources: Int = 6,
    cardVariant: SourceCardVariant = SourceCardVariant.DEFAULT,
    groupByProvider: Boolean = false
) {
    val availableSources = remember(sources) {
        println("DEBUG [SourceSelectionSection]: Total sources received: ${sources.size}")
        sources.forEachIndexed { index, source ->
            println("DEBUG [SourceSelectionSection]: Source $index: ${source.provider.displayName} - Available: ${source.isAvailable}, Provider Available: ${source.provider.isAvailable}, Currently Available: ${source.isCurrentlyAvailable()}")
        }
        val filtered = sources.filter { it.isCurrentlyAvailable() }
            .sortedByDescending { it.getPriorityScore() }
        println("DEBUG [SourceSelectionSection]: Filtered to ${filtered.size} available sources")
        filtered
    }
    
    val displaySources = remember(availableSources, groupByProvider) {
        if (groupByProvider) {
            // Group by provider and take the best quality from each
            availableSources.groupBy { it.provider.id }
                .map { (_, providerSources) ->
                    providerSources.maxByOrNull { it.quality.priority }
                }
                .filterNotNull()
                .sortedByDescending { it.getPriorityScore() }
                .take(maxVisibleSources)
        } else {
            availableSources.take(maxVisibleSources)
        }
    }
    
    if (displaySources.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            if (showSectionTitle) {
                SourceSelectionHeader(
                    title = "Watch on",
                    totalSources = availableSources.size,
                    showViewAllButton = showViewAllButton && availableSources.size > maxVisibleSources,
                    onViewAllClick = onViewAllClick
                )
            }
            
            // Sources row
            SourceSelectionRow(
                sources = displaySources,
                onSourceClick = onSourceSelected,
                selectedSourceId = selectedSourceId,
                cardVariant = cardVariant
            )
            
            // Source summary info
            if (displaySources.isNotEmpty()) {
                SourceSummaryInfo(
                    sources = displaySources,
                    selectedSourceId = selectedSourceId
                )
            }
        }
    } else {
        // No sources available
        NoSourcesAvailable(
            modifier = modifier
        )
    }
}

/**
 * Header component for source selection section
 */
@Composable
private fun SourceSelectionHeader(
    title: String,
    totalSources: Int,
    showViewAllButton: Boolean,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            if (totalSources > 1) {
                Text(
                    text = "$totalSources sources available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
        
        if (showViewAllButton) {
            ViewAllSourcesButton(
                onViewAllClick = onViewAllClick
            )
        }
    }
}

/**
 * View all sources button
 */
@Composable
private fun ViewAllSourcesButton(
    onViewAllClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    TVFocusIndicator(isFocused = isFocused) {
        TextButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onViewAllClick()
            },
            modifier = Modifier.tvFocusable(
                onFocusChanged = { isFocused = it.isFocused }
            ),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Horizontally scrollable row of source cards
 */
@Composable
private fun SourceSelectionRow(
    sources: List<StreamingSource>,
    onSourceClick: (StreamingSource) -> Unit,
    selectedSourceId: String?,
    cardVariant: SourceCardVariant
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Auto-scroll to selected source
    LaunchedEffect(selectedSourceId) {
        selectedSourceId?.let { selectedId ->
            val selectedIndex = sources.indexOfFirst { it.id == selectedId }
            if (selectedIndex != -1) {
                scope.launch {
                    delay(100) // Small delay to ensure UI is ready
                    listState.animateScrollToItem(selectedIndex)
                }
            }
        }
    }
    
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(sources) { source ->
            SourceCard(
                source = source,
                onClick = onSourceClick,
                isSelected = source.id == selectedSourceId,
                variant = cardVariant
            )
        }
    }
}

/**
 * Summary information about available sources
 */
@Composable
private fun SourceSummaryInfo(
    sources: List<StreamingSource>,
    selectedSourceId: String?
) {
    val selectedSource = sources.find { it.id == selectedSourceId }
    val reliableSourcesCount = sources.count { it.isReliable() }
    val p2pSourcesCount = sources.count { it.isP2P() }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Selected source info
        selectedSource?.let { source ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = "${source.provider.displayName} - ${source.quality.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
                SourceTypeBadge(
                    sourceType = source.sourceType.getDisplayType(),
                    reliability = source.sourceType.getReliabilityText(),
                    isP2P = source.isP2P(),
                    seeders = source.features.seeders,
                    size = QualityBadgeSize.SMALL
                )
            }
        }
        
        // Source type summary
        if (sources.size > 1) {
            val summaryText = buildString {
                if (reliableSourcesCount > 0) {
                    append("$reliableSourcesCount reliable")
                }
                if (p2pSourcesCount > 0) {
                    if (reliableSourcesCount > 0) append(" • ")
                    append("$p2pSourcesCount P2P")
                }
                val directSourcesCount = sources.size - p2pSourcesCount
                if (directSourcesCount > 0 && p2pSourcesCount > 0) {
                    append(" • $directSourcesCount direct")
                }
            }
            
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * No sources available state
 */
@Composable
private fun NoSourcesAvailable(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No streaming sources available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Check back later for availability updates",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Source selection with quality filter
 */
@Composable
fun SourceSelectionWithQualityFilter(
    sources: List<StreamingSource>,
    onSourceSelected: (StreamingSource) -> Unit,
    modifier: Modifier = Modifier,
    selectedSourceId: String? = null,
    selectedQuality: SourceQuality? = null,
    onQualitySelected: (SourceQuality?) -> Unit = {},
    showQualityFilter: Boolean = true
) {
    val filteredSources = remember(sources, selectedQuality) {
        if (selectedQuality != null) {
            sources.filter { it.quality == selectedQuality }
        } else {
            sources
        }
    }
    
    val availableQualities = remember(sources) {
        sources.map { it.quality }
            .distinct()
            .sortedByDescending { it.priority }
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quality filter (if enabled and multiple qualities available)
        if (showQualityFilter && availableQualities.size > 1) {
            QualityFilterRow(
                qualities = availableQualities,
                selectedQuality = selectedQuality,
                onQualitySelected = onQualitySelected
            )
        }
        
        // Source selection section
        SourceSelectionSection(
            sources = filteredSources,
            onSourceSelected = onSourceSelected,
            selectedSourceId = selectedSourceId,
            showSectionTitle = false // Title handled by parent
        )
    }
}

/**
 * Quality filter row
 */
@Composable
private fun QualityFilterRow(
    qualities: List<SourceQuality>,
    selectedQuality: SourceQuality?,
    onQualitySelected: (SourceQuality?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Quality",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            // All qualities option
            item {
                QualityFilterChip(
                    text = "All",
                    isSelected = selectedQuality == null,
                    onClick = { onQualitySelected(null) }
                )
            }
            
            // Individual quality options
            items(qualities) { quality ->
                QualityFilterChip(
                    text = quality.displayName,
                    isSelected = selectedQuality == quality,
                    onClick = { onQualitySelected(quality) }
                )
            }
        }
    }
}

/**
 * Quality filter chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualityFilterChip(
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
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            },
            selected = isSelected,
            modifier = Modifier.tvFocusable(
                onFocusChanged = { isFocused = it.isFocused }
            ),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                selectedLabelColor = if (isFocused) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        )
    }
}

/**
 * Preview/Demo configurations for SourceSelectionSection
 */
object SourceSelectionSectionPreview {
    @Composable
    fun SampleSourceSelection() {
        val sampleSources = StreamingSource.createSampleSources()
        var selectedSourceId by remember { mutableStateOf<String?>(null) }
        
        SourceSelectionSection(
            sources = sampleSources,
            onSourceSelected = { selectedSourceId = it.id },
            selectedSourceId = selectedSourceId,
            onViewAllClick = { /* Handle view all */ }
        )
    }
    
    @Composable
    fun SourceSelectionWithFilter() {
        val sampleSources = StreamingSource.createSampleSources()
        var selectedSourceId by remember { mutableStateOf<String?>(null) }
        var selectedQuality by remember { mutableStateOf<SourceQuality?>(null) }
        
        SourceSelectionWithQualityFilter(
            sources = sampleSources,
            onSourceSelected = { selectedSourceId = it.id },
            selectedSourceId = selectedSourceId,
            selectedQuality = selectedQuality,
            onQualitySelected = { selectedQuality = it }
        )
    }
}