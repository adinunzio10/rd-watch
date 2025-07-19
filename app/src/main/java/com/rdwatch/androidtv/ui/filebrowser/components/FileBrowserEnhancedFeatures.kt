package com.rdwatch.androidtv.ui.filebrowser.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.filebrowser.models.*

/**
 * Enhanced File Browser with Phase 4 advanced features:
 * 1. Bulk Selection Mode - Long-press to enter, visual selection indicators
 * 2. Enhanced Sorting UI - Dropdown with chips and quick actions
 * 3. Collapsible Filter Panel - Smart filter organization
 * 4. TV-Optimized Controls - Smooth remote navigation
 */
@Composable
fun EnhancedFileBrowserDemo(modifier: Modifier = Modifier) {
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var sortingOptions by remember { mutableStateOf(SortingOptions()) }
    var filterOptions by remember { mutableStateOf(FilterOptions()) }

    val sampleFiles =
        remember {
            listOf(
                FileItem.File(
                    id = "1",
                    name = "Movie 2023 1080p.mkv",
                    size = 2147483648L,
                    modifiedDate = System.currentTimeMillis(),
                    mimeType = "video/mkv",
                    downloadUrl = null,
                    streamUrl = null,
                    isPlayable = true,
                    status = FileStatus.READY,
                ),
                FileItem.File(
                    id = "2",
                    name = "Soundtrack.mp3",
                    size = 8388608L,
                    modifiedDate = System.currentTimeMillis() - 86400000,
                    mimeType = "audio/mp3",
                    downloadUrl = null,
                    streamUrl = null,
                    isPlayable = true,
                    status = FileStatus.READY,
                ),
                FileItem.Folder(
                    id = "3",
                    name = "TV Shows",
                    modifiedDate = System.currentTimeMillis() - 172800000,
                    itemCount = 15,
                    path = "/tv-shows",
                ),
                FileItem.Torrent(
                    id = "4",
                    name = "Linux Distribution.iso",
                    size = 4294967296L,
                    modifiedDate = System.currentTimeMillis() - 259200000,
                    hash = "abcd1234",
                    progress = 0.75f,
                    status = TorrentStatus.DOWNLOADING,
                    seeders = 25,
                    speed = 1048576L,
                ),
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Feature showcase header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Phase 4: Advanced File Browser Features",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Text(
                    text =
                        "• Bulk Selection Mode with long-press activation\n" +
                            "• Enhanced Sorting UI with multiple display modes\n" +
                            "• Collapsible Filter Panel with smart organization\n" +
                            "• TV-optimized controls and smooth interactions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Bulk Selection Mode Bar
        BulkSelectionModeBar(
            isEnabled = isMultiSelectMode,
            selectionState =
                SelectionState(
                    selectedCount = selectedItems.size,
                    canDownload = selectedItems.isNotEmpty(),
                    canDelete = selectedItems.isNotEmpty(),
                    canPlay =
                        selectedItems.any { id ->
                            sampleFiles.find { it.id == id }?.let { item ->
                                when (item) {
                                    is FileItem.File -> item.isPlayable
                                    else -> false
                                }
                            } ?: false
                        },
                ),
            onToggleBulkMode = { isMultiSelectMode = !isMultiSelectMode },
            onSelectAll = {
                selectedItems = sampleFiles.map { it.id }.toSet()
            },
            onDeselectAll = { selectedItems = emptySet() },
            onDownloadSelected = { /* Demo action */ },
            onDeleteSelected = { /* Demo action */ },
            onPlaySelected = { /* Demo action */ },
        )

        // Enhanced Sorting UI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SortingControlPanel(
                sortingOptions = sortingOptions,
                onSortingChange = { sortingOptions = it },
                displayMode = SortDisplayMode.DROPDOWN,
                modifier = Modifier.weight(1f),
            )

            // Quick sort actions
            QuickSortActions(
                currentSortBy = sortingOptions.sortBy,
                currentSortOrder = sortingOptions.sortOrder,
                onSortingChange = { sortingOptions = it },
            )
        }

        // Enhanced Filter Panel
        EnhancedFilterPanel(
            filterOptions = filterOptions,
            onFilterChange = { filterOptions = it },
            isExpanded = isFilterExpanded,
            onToggleExpanded = { isFilterExpanded = !isFilterExpanded },
        )

        // File list with enhanced selection
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(sampleFiles) { item ->
                SelectableFileItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isMultiSelectMode = isMultiSelectMode,
                    onSelect = { selectedItem ->
                        selectedItems =
                            if (selectedItems.contains(selectedItem.id)) {
                                selectedItems - selectedItem.id
                            } else {
                                selectedItems + selectedItem.id
                            }
                    },
                    onLongPress = { longPressedItem ->
                        isMultiSelectMode = true
                        selectedItems = setOf(longPressedItem.id)
                    },
                    onClick = { /* Demo click action */ },
                )
            }
        }

        // Feature summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "TV-Optimized Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                FeatureList()
            }
        }
    }
}

@Composable
private fun FeatureList() {
    val features =
        listOf(
            "Long-press D-Pad center to enter bulk selection mode",
            "Visual selection indicators with smooth animations",
            "Enhanced sorting with dropdown and chip interfaces",
            "Collapsible filter panel with intelligent organization",
            "TV remote navigation with focus management",
            "Bulk actions: Select All, Download, Delete, Play",
            "Real-time selection statistics and capabilities",
            "Smooth transitions and visual feedback",
        )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        features.forEach { feature ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Usage instructions for the enhanced file browser
 */
@Composable
fun FileBrowserUsageGuide(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "How to Use Advanced Features",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            UsageSection(
                title = "Bulk Selection Mode",
                instructions =
                    listOf(
                        "Long-press any file with D-Pad center button",
                        "Use Select All/Clear All buttons in the toolbar",
                        "Navigate with D-Pad and select files with center button",
                        "Exit mode with the X button or Back button",
                    ),
            )

            UsageSection(
                title = "Enhanced Sorting",
                instructions =
                    listOf(
                        "Use the sorting dropdown for detailed options",
                        "Click sort criteria to toggle ascending/descending",
                        "Use quick sort buttons for common operations",
                        "Sort by: Name, Size, Date, Type, Status",
                    ),
            )

            UsageSection(
                title = "Filter Panel",
                instructions =
                    listOf(
                        "Click the filter header to expand/collapse",
                        "Use search to find specific files",
                        "Toggle quick filters for playable/downloaded files",
                        "Select file types and statuses as needed",
                    ),
            )
        }
    }
}

@Composable
private fun UsageSection(
    title: String,
    instructions: List<String>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        instructions.forEach { instruction ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )

                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
