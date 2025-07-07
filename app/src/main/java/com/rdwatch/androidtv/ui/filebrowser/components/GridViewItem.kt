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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Grid view item for file browser - compact presentation for more items on screen
 * Optimized for TV with D-pad navigation
 */
@Composable
fun GridViewItem(
    item: FileItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onSelect: (FileItem) -> Unit,
    onLongPress: (FileItem) -> Unit,
    onClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val cardElevation by animateDpAsState(
        targetValue = when {
            isSelected -> 10.dp
            isFocused -> 6.dp
            else -> 2.dp
        },
        animationSpec = tween(200)
    )
    
    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = {
                if (isMultiSelectMode) {
                    onSelect(item)
                } else {
                    onClick(item)
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Square aspect ratio
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
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Compact icon with status
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = getFileTypeIcon(item),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = getFileTypeIconTint(item, isSelected, isFocused)
                        )
                        
                        // Small status indicator
                        when (item) {
                            is FileItem.File -> {
                                if (item.status == FileStatus.DOWNLOADING) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                            is FileItem.Torrent -> {
                                if (item.status == TorrentStatus.DOWNLOADING) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = item.progress,
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Compact file name
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                    
                    // Very compact size info
                    if (item.size > 0) {
                        Text(
                            text = formatCompactFileSize(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                    
                    // Special indicators
                    when (item) {
                        is FileItem.Folder -> {
                            if (item.itemCount > 0) {
                                Text(
                                    text = "${item.itemCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    }
                                )
                            }
                        }
                        is FileItem.File -> {
                            if (item.isPlayable) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Playable",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            }
                        }
                        else -> {}
                    }
                }
                
                // Compact selection indicator
                if (isMultiSelectMode) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        },
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper functions
private fun getFileTypeIcon(item: FileItem): androidx.compose.ui.graphics.vector.ImageVector {
    return when (item) {
        is FileItem.Folder -> Icons.Default.Folder
        is FileItem.Torrent -> Icons.Default.Download
        is FileItem.File -> {
            val extension = item.name.substringAfterLast('.', "").lowercase()
            when (FileType.fromExtension(extension)) {
                FileType.VIDEO -> Icons.Default.PlayArrow
                FileType.AUDIO -> Icons.Default.MusicNote
                FileType.IMAGE -> Icons.Default.Image
                FileType.DOCUMENT -> Icons.Default.Description
                FileType.ARCHIVE -> Icons.Default.FolderZip
                FileType.SUBTITLE -> Icons.Default.Subtitles
                FileType.OTHER -> Icons.Default.Description
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
    val baseColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    return when (item) {
        is FileItem.Folder -> baseColor
        is FileItem.Torrent -> baseColor
        is FileItem.File -> {
            val extension = item.name.substringAfterLast('.', "").lowercase()
            when (FileType.fromExtension(extension)) {
                FileType.VIDEO -> if (isSelected || isFocused) baseColor else MaterialTheme.colorScheme.secondary
                FileType.AUDIO -> if (isSelected || isFocused) baseColor else MaterialTheme.colorScheme.tertiary
                else -> baseColor
            }
        }
    }
}

// Compact file size format for grid view
private fun formatCompactFileSize(bytes: Long): String {
    val units = arrayOf("B", "K", "M", "G", "T")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return if (size >= 100) {
        "${size.toInt()}${units[unitIndex]}"
    } else if (size >= 10) {
        "%.0f${units[unitIndex]}".format(size)
    } else {
        "%.1f${units[unitIndex]}".format(size)
    }
}