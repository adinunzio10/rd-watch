package com.rdwatch.androidtv.ui.filebrowser.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.filebrowser.models.FileItem
import com.rdwatch.androidtv.ui.filebrowser.models.SelectionState
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Bulk Selection Mode UI component for File Browser
 * TV-optimized interface for mass file operations
 */
@Composable
fun BulkSelectionModeBar(
    isEnabled: Boolean,
    selectionState: SelectionState,
    onToggleBulkMode: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDownloadSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onPlaySelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectAllFocusRequester = remember { FocusRequester() }
    
    // Auto-focus select all when entering bulk mode
    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            selectAllFocusRequester.requestFocus()
        }
    }
    
    AnimatedVisibility(
        visible = isEnabled,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with selection count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Bulk Selection Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    // Exit bulk mode button
                    var exitFocused by remember { mutableStateOf(false) }
                    
                    TVFocusIndicator(isFocused = exitFocused) {
                        IconButton(
                            onClick = onToggleBulkMode,
                            modifier = Modifier.tvFocusable(
                                onFocusChanged = { exitFocused = it.isFocused }
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit bulk mode",
                                tint = if (exitFocused) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }
                
                // Selection count and stats
                SelectionStatsCard(
                    selectionState = selectionState,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Select All/None buttons
                    BulkActionButton(
                        text = "Select All",
                        icon = Icons.Default.SelectAll,
                        onClick = onSelectAll,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(selectAllFocusRequester),
                        enabled = selectionState.selectedCount == 0
                    )
                    
                    BulkActionButton(
                        text = "Clear All",
                        icon = Icons.Default.Clear,
                        onClick = onDeselectAll,
                        modifier = Modifier.weight(1f),
                        enabled = selectionState.selectedCount > 0
                    )
                    
                    // Action buttons
                    if (selectionState.canPlay) {
                        BulkActionButton(
                            text = "Play",
                            icon = Icons.Default.PlayArrow,
                            onClick = onPlaySelected,
                            modifier = Modifier.weight(1f),
                            enabled = true,
                            buttonType = BulkActionButtonType.PRIMARY
                        )
                    }
                    
                    if (selectionState.canDownload) {
                        BulkActionButton(
                            text = "Download",
                            icon = Icons.Default.Download,
                            onClick = onDownloadSelected,
                            modifier = Modifier.weight(1f),
                            enabled = true
                        )
                    }
                    
                    if (selectionState.canDelete) {
                        BulkActionButton(
                            text = "Delete",
                            icon = Icons.Default.Delete,
                            onClick = onDeleteSelected,
                            modifier = Modifier.weight(1f),
                            enabled = true,
                            buttonType = BulkActionButtonType.DANGER
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionStatsCard(
    selectionState: SelectionState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionStat(
                label = "Selected",
                value = selectionState.selectedCount.toString(),
                icon = Icons.Default.CheckCircle
            )
            
            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            
            SelectionStat(
                label = "Playable",
                value = if (selectionState.canPlay) "Yes" else "No",
                icon = Icons.Default.PlayArrow,
                isEnabled = selectionState.canPlay
            )
            
            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            
            SelectionStat(
                label = "Actions",
                value = buildList {
                    if (selectionState.canPlay) add("Play")
                    if (selectionState.canDownload) add("Download")
                    if (selectionState.canDelete) add("Delete")
                }.size.toString(),
                icon = Icons.Default.Settings
            )
        }
    }
}

@Composable
private fun SelectionStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            }
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isEnabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            }
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

enum class BulkActionButtonType {
    DEFAULT,
    PRIMARY,
    DANGER
}

@Composable
private fun BulkActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonType: BulkActionButtonType = BulkActionButtonType.DEFAULT
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        when (buttonType) {
            BulkActionButtonType.PRIMARY -> {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = modifier.tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused }
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            BulkActionButtonType.DANGER -> {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = modifier.tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused }
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            else -> {
                OutlinedButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = modifier.tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused }
                    ),
                    // Use default outlined button styling
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Enhanced file item with selection indicators and long-press detection
 */
@Composable
fun SelectableFileItem(
    item: FileItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onSelect: (FileItem) -> Unit,
    onLongPress: (FileItem) -> Unit,
    onClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var isLongPressing by remember { mutableStateOf(false) }
    
    // Visual selection indicators
    val selectionIndicatorWidth by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = tween(200)
    )
    
    val cardElevation by animateDpAsState(
        targetValue = when {
            isSelected -> 8.dp
            isFocused -> 6.dp
            else -> 2.dp
        },
        animationSpec = tween(200)
    )
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Selection indicator bar
        Box(
            modifier = Modifier
                .width(selectionIndicatorWidth)
                .height(72.dp)
                .background(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(
                        topEnd = 4.dp,
                        bottomEnd = 4.dp
                    )
                )
        )
        
        // File item content
        TVFocusIndicator(isFocused = isFocused) {
            Card(
                onClick = {
                    if (isMultiSelectMode) {
                        onSelect(item)
                    } else {
                        onClick(item)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isFocused -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = cardElevation
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selection checkbox (visible in multi-select mode)
                    AnimatedVisibility(
                        visible = isMultiSelectMode,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelect(item) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                    
                    // File type icon
                    Icon(
                        imageVector = getFileTypeIcon(item),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = getFileTypeIconTint(item, isSelected, isFocused)
                    )
                    
                    // File details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1
                        )
                        
                        if (item.size > 0) {
                            Text(
                                text = formatFileSize(item.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                    
                    // Selection count indicator for multi-select
                    if (isMultiSelectMode && isSelected) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(4.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper functions (these would normally be in a separate utility file)
private fun getFileTypeIcon(item: FileItem): androidx.compose.ui.graphics.vector.ImageVector {
    return when (item) {
        is FileItem.Folder -> Icons.Default.Folder
        is FileItem.Torrent -> Icons.Default.Download
        is FileItem.File -> {
            val extension = item.name.substringAfterLast('.', "").lowercase()
            when {
                extension in setOf("mp4", "mkv", "avi", "mov") -> Icons.Default.PlayArrow
                extension in setOf("mp3", "wav", "flac", "aac") -> Icons.Default.MusicNote
                extension in setOf("jpg", "jpeg", "png", "gif") -> Icons.Default.Image
                extension in setOf("pdf", "doc", "docx", "txt") -> Icons.Default.Description
                extension in setOf("zip", "rar", "7z") -> Icons.Default.FolderZip
                else -> Icons.Default.Description
            }
        }
    }
}

@Composable
private fun getFileTypeIconTint(
    item: FileItem,
    isSelected: Boolean,
    isFocused: Boolean
): androidx.compose.ui.graphics.Color {
    return if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

private fun formatFileSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return if (size >= 100) {
        "${size.toInt()} ${units[unitIndex]}"
    } else {
        "%.1f ${units[unitIndex]}".format(size)
    }
}