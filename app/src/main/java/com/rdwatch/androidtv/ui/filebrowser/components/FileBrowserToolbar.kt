package com.rdwatch.androidtv.ui.filebrowser.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.presentation.components.tvFocusable
import com.rdwatch.androidtv.ui.filebrowser.models.FileSortOption

/**
 * Toolbar component with sorting, search, and actions
 */
@Composable
fun FileBrowserToolbar(
    sortOption: FileSortOption,
    onSortChange: (FileSortOption) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onBulkDelete: () -> Unit,
    onBulkPlay: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: Search and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search files...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onSearchChange("") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            // Action buttons
            if (isSelectionMode) {
                // Bulk actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bulk play button
                    var playButtonFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onBulkPlay,
                        enabled = selectedCount > 0,
                        modifier = Modifier.tvFocusable(
                            onFocusChanged = { playButtonFocused = it }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play selected",
                            tint = if (playButtonFocused) {
                                MaterialTheme.colorScheme.primary
                            } else if (selectedCount > 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                    
                    // Bulk delete button
                    var deleteButtonFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onBulkDelete,
                        enabled = selectedCount > 0,
                        modifier = Modifier.tvFocusable(
                            onFocusChanged = { deleteButtonFocused = it }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete selected",
                            tint = if (deleteButtonFocused) {
                                MaterialTheme.colorScheme.error
                            } else if (selectedCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
            } else {
                // Regular actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Refresh button
                    var refreshButtonFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.tvFocusable(
                            onFocusChanged = { refreshButtonFocused = it }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (refreshButtonFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    
                    // Selection mode toggle
                    var selectionButtonFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onToggleSelectionMode,
                        modifier = Modifier.tvFocusable(
                            onFocusChanged = { selectionButtonFocused = it }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckBox,
                            contentDescription = "Select files",
                            tint = if (selectionButtonFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
        
        // Bottom row: Sort options
        if (!isSelectionMode) {
            SortOptionsRow(
                selectedSort = sortOption,
                onSortChange = onSortChange
            )
        }
    }
}

/**
 * Row of sort option chips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOptionsRow(
    selectedSort: FileSortOption,
    onSortChange: (FileSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(FileSortOption.values().toList()) { sortOption ->
            var isFocused by remember { mutableStateOf(false) }
            
            FilterChip(
                onClick = { onSortChange(sortOption) },
                label = {
                    Text(
                        text = sortOption.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (sortOption == selectedSort || isFocused) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                selected = sortOption == selectedSort,
                modifier = Modifier.tvFocusable(
                    onFocusChanged = { isFocused = it }
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (sortOption == selectedSort) {
                        MaterialTheme.colorScheme.primary
                    } else if (isFocused) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    labelColor = if (sortOption == selectedSort) {
                        MaterialTheme.colorScheme.onPrimary
                    } else if (isFocused) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = if (isFocused && sortOption != selectedSort) {
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 2.dp
                    )
                } else null
            )
        }
    }
}