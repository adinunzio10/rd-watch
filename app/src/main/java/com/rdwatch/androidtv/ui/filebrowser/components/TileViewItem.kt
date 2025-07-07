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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tile view item for file browser - larger visual presentation
 * Optimized for TV with D-pad navigation
 */
@Composable
fun TileViewItem(
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
            isSelected -> 12.dp
            isFocused -> 8.dp
            else -> 4.dp
        },
        animationSpec = tween(200)
    )
    
    // Remove scale animation for now - will use elevation only
    
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
                .aspectRatio(1.2f) // Slightly wider than tall
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
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Icon and status area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Large file type icon
                        Icon(
                            imageVector = getFileTypeIcon(item),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = getFileTypeIconTint(item, isSelected, isFocused)
                        )
                        
                        // Status overlay (for downloading, error, etc.)
                        when (item) {
                            is FileItem.File -> {
                                if (item.status != FileStatus.READY) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                            .padding(4.dp)
                                    ) {
                                        FileStatusIndicator(
                                            status = item.status,
                                            progress = item.progress,
                                            isSelected = isSelected
                                        )
                                    }
                                }
                            }
                            is FileItem.Torrent -> {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                        .padding(4.dp)
                                ) {
                                    TorrentStatusIndicator(
                                        status = item.status,
                                        progress = item.progress,
                                        isSelected = isSelected
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                    
                    // File details
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // File name
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        
                        // Size and additional info
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.size > 0) {
                                Text(
                                    text = formatFileSize(item.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    }
                                )
                            }
                            
                            // Additional info based on type
                            when (item) {
                                is FileItem.Folder -> {
                                    if (item.itemCount > 0) {
                                        Text(
                                            text = "${item.itemCount} items",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            }
                                        )
                                    }
                                }
                                is FileItem.File -> {
                                    if (item.isPlayable) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = "Playable",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
                
                // Selection overlay
                if (isMultiSelectMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        },
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelect(item) },
                            modifier = Modifier.size(32.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }
}

// Helper functions (reusing from SelectableFileItem)
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

@Composable
private fun FileStatusIndicator(
    status: FileStatus,
    progress: Float?,
    isSelected: Boolean
) {
    val color = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    when (status) {
        FileStatus.DOWNLOADING -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (progress != null) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(20.dp),
                        color = color,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = color.copy(alpha = 0.7f)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = color,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        FileStatus.ERROR -> {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        FileStatus.UNAVAILABLE -> {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Unavailable",
                modifier = Modifier.size(20.dp),
                tint = color.copy(alpha = 0.5f)
            )
        }
        FileStatus.READY -> {
            // No indicator needed
        }
    }
}

@Composable
private fun TorrentStatusIndicator(
    status: TorrentStatus,
    progress: Float,
    isSelected: Boolean
) {
    val color = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            TorrentStatus.DOWNLOADING -> {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(20.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = color.copy(alpha = 0.7f)
                )
            }
            TorrentStatus.DOWNLOADED -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            TorrentStatus.ERROR, TorrentStatus.DEAD -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                Text(
                    text = status.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
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