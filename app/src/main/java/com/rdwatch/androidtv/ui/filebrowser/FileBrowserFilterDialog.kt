package com.rdwatch.androidtv.ui.filebrowser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Filter Dialog for File Browser - Allows users to filter content by type, status, etc.
 * Optimized for TV remote navigation
 */
@Composable
fun FileBrowserFilterDialog(
    filterOptions: FilterOptions,
    onFilterChange: (FilterOptions) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val applyFocusRequester = remember { FocusRequester() }
    var currentFilters by remember { mutableStateOf(filterOptions) }

    LaunchedEffect(Unit) {
        applyFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        Surface(
            modifier =
                modifier
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Filter Files",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    var closeFocused by remember { mutableStateOf(false) }

                    TVFocusIndicator(isFocused = closeFocused) {
                        IconButton(
                            onClick = onDismiss,
                            modifier =
                                Modifier.tvFocusable(
                                    onFocusChanged = { closeFocused = it.isFocused },
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint =
                                    if (closeFocused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                    }
                }

                // Filter content
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // General filters
                    FilterSection(
                        title = "General Filters",
                        content = {
                            FilterCheckbox(
                                label = "Show only playable files",
                                checked = currentFilters.showOnlyPlayable,
                                onCheckedChange = {
                                    currentFilters = currentFilters.copy(showOnlyPlayable = it)
                                },
                            )

                            FilterCheckbox(
                                label = "Show only downloaded files",
                                checked = currentFilters.showOnlyDownloaded,
                                onCheckedChange = {
                                    currentFilters = currentFilters.copy(showOnlyDownloaded = it)
                                },
                            )
                        },
                    )

                    // File type filters
                    FilterSection(
                        title = "File Types",
                        content = {
                            FileType.values().forEach { fileType ->
                                FilterCheckbox(
                                    label = fileType.displayName,
                                    checked = currentFilters.fileTypeFilter.contains(fileType),
                                    onCheckedChange = { isChecked ->
                                        val newFilter =
                                            if (isChecked) {
                                                currentFilters.fileTypeFilter + fileType
                                            } else {
                                                currentFilters.fileTypeFilter - fileType
                                            }
                                        currentFilters = currentFilters.copy(fileTypeFilter = newFilter)
                                    },
                                )
                            }
                        },
                    )

                    // Status filters
                    FilterSection(
                        title = "File Status",
                        content = {
                            FileStatus.values().forEach { status ->
                                FilterCheckbox(
                                    label =
                                        status.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.titlecase() },
                                    checked = currentFilters.statusFilter.contains(status),
                                    onCheckedChange = { isChecked ->
                                        val newFilter =
                                            if (isChecked) {
                                                currentFilters.statusFilter + status
                                            } else {
                                                currentFilters.statusFilter - status
                                            }
                                        currentFilters = currentFilters.copy(statusFilter = newFilter)
                                    },
                                )
                            }
                        },
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Reset button
                    var resetFocused by remember { mutableStateOf(false) }

                    TVFocusIndicator(isFocused = resetFocused) {
                        OutlinedButton(
                            onClick = {
                                currentFilters = FilterOptions()
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .tvFocusable(
                                        onFocusChanged = { resetFocused = it.isFocused },
                                    ),
                        ) {
                            Text("Reset")
                        }
                    }

                    // Cancel button
                    var cancelFocused by remember { mutableStateOf(false) }

                    TVFocusIndicator(isFocused = cancelFocused) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .tvFocusable(
                                        onFocusChanged = { cancelFocused = it.isFocused },
                                    ),
                        ) {
                            Text("Cancel")
                        }
                    }

                    // Apply button
                    var applyFocused by remember { mutableStateOf(false) }

                    TVFocusIndicator(isFocused = applyFocused) {
                        Button(
                            onClick = {
                                onFilterChange(currentFilters)
                                onDismiss()
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .focusRequester(applyFocusRequester)
                                    .tvFocusable(
                                        onFocusChanged = { applyFocused = it.isFocused },
                                    ),
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            content()
        }
    }
}

@Composable
private fun FilterCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused },
                    ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor =
                            if (isFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                    ),
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}
