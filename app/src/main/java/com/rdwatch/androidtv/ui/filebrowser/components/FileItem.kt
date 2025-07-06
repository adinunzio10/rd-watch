package com.rdwatch.androidtv.ui.filebrowser.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.presentation.components.tvFocusable
import com.rdwatch.androidtv.ui.filebrowser.models.FileItemUiModel
import com.rdwatch.androidtv.ui.filebrowser.models.FileTypeCategory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Individual file item component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileItem(
    file: FileItemUiModel,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onPlayFile: () -> Unit,
    onDeleteFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                onFocusChanged = { isFocused = it }
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isFocused -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator or file type icon
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null // Handled by parent click
                )
            } else {
                Icon(
                    imageVector = getFileTypeIcon(file.fileTypeCategory),
                    contentDescription = file.fileTypeCategory.name,
                    modifier = Modifier.size(32.dp),
                    tint = getFileTypeColor(file.fileTypeCategory)
                )
            }
            
            // File information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // File name
                Text(
                    text = file.filename,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // File details
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.formattedFileSize,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            .format(file.dateAdded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    if (file.isStreamable) {
                        Icon(
                            imageVector = Icons.Default.Stream,
                            contentDescription = "Streamable",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Host and mime type
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        text = file.mimeType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Action buttons (only show when focused and not in selection mode)
            if (isFocused && !isSelectionMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play button (only for streamable files)
                    if (file.isStreamable) {
                        var playButtonFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = onPlayFile,
                            modifier = Modifier.tvFocusable(
                                onFocusChanged = { playButtonFocused = it }
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (playButtonFocused) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    
                    // More actions button
                    var moreButtonFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.tvFocusable(
                            onFocusChanged = { moreButtonFocused = it }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = if (moreButtonFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Context menu
    if (showContextMenu) {
        FileContextMenu(
            file = file,
            onPlayFile = onPlayFile,
            onDeleteFile = onDeleteFile,
            onDismiss = { showContextMenu = false }
        )
    }
}

/**
 * Context menu for file actions
 */
@Composable
private fun FileContextMenu(
    file: FileItemUiModel,
    onPlayFile: () -> Unit,
    onDeleteFile: () -> Unit,
    onDismiss: () -> Unit
) {
    // For now, we'll use a simple dialog
    // In a full implementation, this would be a proper context menu
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "File Actions",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = file.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (file.isStreamable) {
                    TextButton(
                        onClick = {
                            onPlayFile()
                            onDismiss()
                        }
                    ) {
                        Text("Play")
                    }
                }
                
                TextButton(
                    onClick = {
                        onDeleteFile()
                        onDismiss()
                    }
                ) {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Gets the appropriate icon for a file type
 */
private fun getFileTypeIcon(fileType: FileTypeCategory): ImageVector {
    return when (fileType) {
        FileTypeCategory.VIDEO -> Icons.Default.Movie
        FileTypeCategory.AUDIO -> Icons.Default.AudioFile
        FileTypeCategory.IMAGE -> Icons.Default.Image
        FileTypeCategory.TEXT -> Icons.Default.TextFields
        FileTypeCategory.ARCHIVE -> Icons.Default.Archive
        FileTypeCategory.SUBTITLE -> Icons.Default.Subtitles
        FileTypeCategory.OTHER -> Icons.Default.InsertDriveFile
    }
}

/**
 * Gets the appropriate color for a file type
 */
@Composable
private fun getFileTypeColor(fileType: FileTypeCategory): Color {
    return when (fileType) {
        FileTypeCategory.VIDEO -> MaterialTheme.colorScheme.primary
        FileTypeCategory.AUDIO -> MaterialTheme.colorScheme.secondary
        FileTypeCategory.IMAGE -> MaterialTheme.colorScheme.tertiary
        FileTypeCategory.TEXT -> MaterialTheme.colorScheme.onSurface
        FileTypeCategory.ARCHIVE -> MaterialTheme.colorScheme.outline
        FileTypeCategory.SUBTITLE -> MaterialTheme.colorScheme.secondary
        FileTypeCategory.OTHER -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
}