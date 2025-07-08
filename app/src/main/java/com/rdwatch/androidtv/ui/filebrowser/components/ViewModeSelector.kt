package com.rdwatch.androidtv.ui.filebrowser.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.filebrowser.models.ViewMode
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * TV-optimized view mode selector for file browser
 * Allows switching between List, Tiles, and Grid views
 */
@Composable
fun ViewModeSelector(
    currentViewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    Box(modifier = modifier) {
        TVFocusIndicator(isFocused = isFocused) {
            Surface(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused }
                    ),
                shape = RoundedCornerShape(8.dp),
                color = if (isFocused) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                tonalElevation = if (isFocused) 4.dp else 2.dp,
                border = if (isFocused) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getViewModeIcon(currentViewMode),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isFocused) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Text(
                        text = currentViewMode.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isFocused) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
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
            onDismissRequest = { isExpanded = false }
        ) {
            ViewMode.values().forEach { viewMode ->
                ViewModeMenuItem(
                    viewMode = viewMode,
                    isSelected = currentViewMode == viewMode,
                    onClick = {
                        onViewModeChange(viewMode)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

/**
 * Compact view mode selector with just icons
 */
@Composable
fun CompactViewModeSelector(
    currentViewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ViewMode.values().forEach { viewMode ->
            ViewModeIconButton(
                viewMode = viewMode,
                isSelected = currentViewMode == viewMode,
                onClick = { onViewModeChange(viewMode) }
            )
        }
    }
}

@Composable
private fun ViewModeMenuItem(
    viewMode: ViewMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getViewModeIcon(viewMode),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Text(
                    text = viewMode.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        onClick = onClick
    )
}

@Composable
private fun ViewModeIconButton(
    viewMode: ViewMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isFocused -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(200)
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200)
    )
    
    TVFocusIndicator(isFocused = isFocused) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(36.dp)
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                )
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = backgroundColor,
                tonalElevation = if (isFocused && !isSelected) 4.dp else 0.dp
            ) {
                Icon(
                    imageVector = getViewModeIcon(viewMode),
                    contentDescription = viewMode.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    tint = contentColor
                )
            }
        }
    }
}

private fun getViewModeIcon(viewMode: ViewMode): ImageVector {
    return when (viewMode) {
        ViewMode.LIST -> Icons.Default.ViewList
        ViewMode.TILES -> Icons.Default.ViewModule
        ViewMode.GRID -> Icons.Default.GridView
    }
}