package com.rdwatch.androidtv.ui.filebrowser.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.filebrowser.models.SortBy
import com.rdwatch.androidtv.ui.filebrowser.models.SortOrder
import com.rdwatch.androidtv.ui.filebrowser.models.SortingOptions
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Enhanced sorting UI component for File Browser
 * TV-optimized with both dropdown and chip interfaces
 */
@Composable
fun SortingControlPanel(
    sortingOptions: SortingOptions,
    onSortingChange: (SortingOptions) -> Unit,
    displayMode: SortDisplayMode = SortDisplayMode.DROPDOWN,
    modifier: Modifier = Modifier
) {
    when (displayMode) {
        SortDisplayMode.DROPDOWN -> {
            EnhancedSortingDropdown(
                sortingOptions = sortingOptions,
                onSortingChange = onSortingChange,
                modifier = modifier
            )
        }
        SortDisplayMode.CHIPS -> {
            SortingChips(
                sortingOptions = sortingOptions,
                onSortingChange = onSortingChange,
                modifier = modifier
            )
        }
        SortDisplayMode.COMBO -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnhancedSortingDropdown(
                    sortingOptions = sortingOptions,
                    onSortingChange = onSortingChange
                )
                SortingChips(
                    sortingOptions = sortingOptions,
                    onSortingChange = onSortingChange,
                    showActiveOnly = true
                )
            }
        }
    }
}

enum class SortDisplayMode {
    DROPDOWN,
    CHIPS,
    COMBO
}

@Composable
private fun EnhancedSortingDropdown(
    sortingOptions: SortingOptions,
    onSortingChange: (SortingOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val dropdownFocusRequester = remember { FocusRequester() }
    
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200)
    )
    
    Box(modifier = modifier) {
        TVFocusIndicator(isFocused = isFocused) {
            Surface(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .focusRequester(dropdownFocusRequester)
                    .tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused }
                    ),
                shape = RoundedCornerShape(12.dp),
                color = if (isFocused) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                tonalElevation = if (isFocused) 6.dp else 2.dp,
                border = if (isFocused) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort icon
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isFocused) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    // Sort criteria and order
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Sort by ${sortingOptions.sortBy.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isFocused) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        Text(
                            text = if (sortingOptions.sortOrder == SortOrder.ASCENDING) {
                                "Ascending"
                            } else {
                                "Descending"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFocused) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Sort order icon
                    Icon(
                        imageVector = if (sortingOptions.sortOrder == SortOrder.ASCENDING) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isFocused) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    // Dropdown arrow
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
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
        
        // Dropdown menu
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.width(300.dp)
        ) {
            Text(
                text = "Sort Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Divider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            
            SortBy.values().forEach { sortBy ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getSortByIcon(sortBy),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (sortingOptions.sortBy == sortBy) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }
                            )
                            
                            Text(
                                text = sortBy.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (sortingOptions.sortBy == sortBy) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (sortingOptions.sortBy == sortBy) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            if (sortingOptions.sortBy == sortBy) {
                                IconButton(
                                    onClick = {
                                        val newOrder = if (sortingOptions.sortOrder == SortOrder.ASCENDING) {
                                            SortOrder.DESCENDING
                                        } else {
                                            SortOrder.ASCENDING
                                        }
                                        onSortingChange(sortingOptions.copy(sortOrder = newOrder))
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (sortingOptions.sortOrder == SortOrder.ASCENDING) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                        contentDescription = "Toggle sort order",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        val newOrder = if (sortingOptions.sortBy == sortBy) {
                            if (sortingOptions.sortOrder == SortOrder.ASCENDING) {
                                SortOrder.DESCENDING
                            } else {
                                SortOrder.ASCENDING
                            }
                        } else {
                            SortOrder.ASCENDING
                        }
                        onSortingChange(sortingOptions.copy(sortBy = sortBy, sortOrder = newOrder))
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SortingChips(
    sortingOptions: SortingOptions,
    onSortingChange: (SortingOptions) -> Unit,
    showActiveOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        if (showActiveOnly) {
            // Show only active sort option
            item {
                ActiveSortChip(
                    sortBy = sortingOptions.sortBy,
                    sortOrder = sortingOptions.sortOrder,
                    onOrderToggle = {
                        val newOrder = if (sortingOptions.sortOrder == SortOrder.ASCENDING) {
                            SortOrder.DESCENDING
                        } else {
                            SortOrder.ASCENDING
                        }
                        onSortingChange(sortingOptions.copy(sortOrder = newOrder))
                    }
                )
            }
        } else {
            // Show all sort options
            items(SortBy.values()) { sortBy ->
                SortChip(
                    sortBy = sortBy,
                    isSelected = sortingOptions.sortBy == sortBy,
                    sortOrder = if (sortingOptions.sortBy == sortBy) {
                        sortingOptions.sortOrder
                    } else {
                        SortOrder.ASCENDING
                    },
                    onClick = {
                        val newOrder = if (sortingOptions.sortBy == sortBy) {
                            if (sortingOptions.sortOrder == SortOrder.ASCENDING) {
                                SortOrder.DESCENDING
                            } else {
                                SortOrder.ASCENDING
                            }
                        } else {
                            SortOrder.ASCENDING
                        }
                        onSortingChange(sortingOptions.copy(sortBy = sortBy, sortOrder = newOrder))
                    }
                )
            }
        }
    }
}

@Composable
private fun SortChip(
    sortBy: SortBy,
    isSelected: Boolean,
    sortOrder: SortOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        Surface(
            onClick = onClick,
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getSortByIcon(sortBy),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Text(
                    text = sortBy.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (isSelected) {
                    Icon(
                        imageVector = if (sortOrder == SortOrder.ASCENDING) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSortChip(
    sortBy: SortBy,
    sortOrder: SortOrder,
    onOrderToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        Surface(
            onClick = onOrderToggle,
            modifier = modifier.tvFocusable(
                onFocusChanged = { isFocused = it.isFocused }
            ),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = if (isFocused) 6.dp else 3.dp,
            border = if (isFocused) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getSortByIcon(sortBy),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = sortBy.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (sortOrder == SortOrder.ASCENDING) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = "Toggle sort order",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Quick sort toggle buttons for common operations
 */
@Composable
fun QuickSortActions(
    currentSortBy: SortBy,
    currentSortOrder: SortOrder,
    onSortingChange: (SortingOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick sort by name
        QuickSortButton(
            text = "Name",
            icon = Icons.Default.SortByAlpha,
            isActive = currentSortBy == SortBy.NAME,
            onClick = {
                onSortingChange(SortingOptions(SortBy.NAME, SortOrder.ASCENDING))
            }
        )
        
        // Quick sort by date
        QuickSortButton(
            text = "Date",
            icon = Icons.Default.DateRange,
            isActive = currentSortBy == SortBy.DATE,
            onClick = {
                onSortingChange(SortingOptions(SortBy.DATE, SortOrder.DESCENDING))
            }
        )
        
        // Quick sort by size
        QuickSortButton(
            text = "Size",
            icon = Icons.Default.Storage,
            isActive = currentSortBy == SortBy.SIZE,
            onClick = {
                onSortingChange(SortingOptions(SortBy.SIZE, SortOrder.DESCENDING))
            }
        )
        
        // Order toggle
        QuickSortButton(
            text = if (currentSortOrder == SortOrder.ASCENDING) "↑" else "↓",
            icon = if (currentSortOrder == SortOrder.ASCENDING) {
                Icons.Default.KeyboardArrowUp
            } else {
                Icons.Default.KeyboardArrowDown
            },
            isActive = true,
            onClick = {
                val newOrder = if (currentSortOrder == SortOrder.ASCENDING) {
                    SortOrder.DESCENDING
                } else {
                    SortOrder.ASCENDING
                }
                onSortingChange(SortingOptions(currentSortBy, newOrder))
            }
        )
    }
}

@Composable
private fun QuickSortButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        Surface(
            onClick = onClick,
            modifier = modifier.tvFocusable(
                onFocusChanged = { isFocused = it.isFocused }
            ),
            shape = RoundedCornerShape(8.dp),
            color = when {
                isActive -> MaterialTheme.colorScheme.primary
                isFocused -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (isFocused) 4.dp else 2.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = when {
                        isActive -> MaterialTheme.colorScheme.onPrimary
                        isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = when {
                        isActive -> MaterialTheme.colorScheme.onPrimary
                        isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

private fun getSortByIcon(sortBy: SortBy): androidx.compose.ui.graphics.vector.ImageVector {
    return when (sortBy) {
        SortBy.NAME -> Icons.Default.SortByAlpha
        SortBy.SIZE -> Icons.Default.Storage
        SortBy.DATE -> Icons.Default.DateRange
        SortBy.TYPE -> Icons.Default.Category
        SortBy.STATUS -> Icons.Default.Circle
    }
}