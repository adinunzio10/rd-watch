package com.rdwatch.androidtv.ui.filebrowser

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable
import java.text.SimpleDateFormat
import java.util.*

/**
 * File Details Dialog - Shows detailed information about a selected file, folder, or torrent
 * Optimized for TV remote navigation
 */
@Composable
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit,
    onPlayFile: ((FileItem.File) -> Unit)? = null,
    onDownloadFile: ((FileItem) -> Unit)? = null,
    onDeleteFile: ((FileItem) -> Unit)? = null,
    onCopyLink: ((FileItem.File) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val closeFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        closeFocusRequester.requestFocus()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with icon and title
                FileDetailsHeader(
                    item = item,
                    onDismiss = onDismiss,
                    closeFocusRequester = closeFocusRequester
                )
                
                // Content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Basic information
                    FileBasicInfo(item = item)
                    
                    // Type-specific information
                    when (item) {
                        is FileItem.File -> FileSpecificInfo(item)
                        is FileItem.Torrent -> TorrentSpecificInfo(item)
                        is FileItem.Folder -> FolderSpecificInfo(item)
                    }
                    
                    // Status information
                    FileStatusInfo(item = item)
                }
                
                // Action buttons
                FileDetailsActions(
                    item = item,
                    onPlayFile = onPlayFile,
                    onDownloadFile = onDownloadFile,
                    onDeleteFile = onDeleteFile,
                    onCopyLink = onCopyLink
                )
            }
        }
    }
}

@Composable
private fun FileDetailsHeader(
    item: FileItem,
    onDismiss: () -> Unit,
    closeFocusRequester: FocusRequester
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon and title
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getFileTypeIcon(item),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = getFileTypeColor(item)
            )
            
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = getFileTypeLabel(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        // Close button
        var closeFocused by remember { mutableStateOf(false) }
        
        TVFocusIndicator(isFocused = closeFocused) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .focusRequester(closeFocusRequester)
                    .tvFocusable(
                        onFocusChanged = { closeFocused = it.isFocused }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (closeFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun FileBasicInfo(item: FileItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // File size
            if (item.size > 0) {
                DetailRow(
                    label = "Size",
                    value = formatFileSize(item.size)
                )
            }
            
            // Modified date
            DetailRow(
                label = "Modified",
                value = formatDate(item.modifiedDate)
            )
            
            // ID
            DetailRow(
                label = "ID",
                value = item.id
            )
        }
    }
}

@Composable
private fun FileSpecificInfo(file: FileItem.File) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "File Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // MIME type
            if (file.mimeType != null) {
                DetailRow(
                    label = "Type",
                    value = file.mimeType
                )
            }
            
            // Extension
            val extension = file.name.substringAfterLast('.', "")
            if (extension.isNotEmpty()) {
                DetailRow(
                    label = "Extension",
                    value = extension.uppercase()
                )
            }
            
            // File category
            val fileType = FileType.fromExtension(extension)
            DetailRow(
                label = "Category",
                value = fileType.displayName
            )
            
            // Playable status
            DetailRow(
                label = "Playable",
                value = if (file.isPlayable) "Yes" else "No"
            )
            
            // Progress (if downloading)
            if (file.progress != null) {
                DetailRow(
                    label = "Progress",
                    value = "${(file.progress * 100).toInt()}%"
                )
            }
        }
    }
}

@Composable
private fun TorrentSpecificInfo(torrent: FileItem.Torrent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Torrent Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Hash
            DetailRow(
                label = "Hash",
                value = torrent.hash
            )
            
            // Progress
            DetailRow(
                label = "Progress",
                value = "${(torrent.progress * 100).toInt()}%"
            )
            
            // Seeders
            if (torrent.seeders != null) {
                DetailRow(
                    label = "Seeders",
                    value = torrent.seeders.toString()
                )
            }
            
            // Speed
            if (torrent.speed != null) {
                DetailRow(
                    label = "Speed",
                    value = formatSpeed(torrent.speed)
                )
            }
            
            // File count
            DetailRow(
                label = "Files",
                value = torrent.files.size.toString()
            )
            
            // Playable files
            val playableFiles = torrent.files.count { it.isPlayable }
            if (playableFiles > 0) {
                DetailRow(
                    label = "Playable Files",
                    value = playableFiles.toString()
                )
            }
        }
    }
}

@Composable
private fun FolderSpecificInfo(folder: FileItem.Folder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Folder Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Path
            DetailRow(
                label = "Path",
                value = folder.path
            )
            
            // Item count
            DetailRow(
                label = "Items",
                value = folder.itemCount.toString()
            )
        }
    }
}

@Composable
private fun FileStatusInfo(item: FileItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item) {
                is FileItem.File -> when (item.status) {
                    FileStatus.READY -> MaterialTheme.colorScheme.primaryContainer
                    FileStatus.DOWNLOADING -> MaterialTheme.colorScheme.tertiaryContainer
                    FileStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                    FileStatus.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceVariant
                }
                is FileItem.Torrent -> when (item.status) {
                    TorrentStatus.DOWNLOADED -> MaterialTheme.colorScheme.primaryContainer
                    TorrentStatus.DOWNLOADING -> MaterialTheme.colorScheme.tertiaryContainer
                    TorrentStatus.ERROR, TorrentStatus.DEAD -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getStatusIcon(item),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = getStatusColor(item)
                )
                
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            val statusText = when (item) {
                is FileItem.File -> item.status.name.replace("_", " ").lowercase()
                    .replaceFirstChar { it.titlecase() }
                is FileItem.Torrent -> item.status.displayName
                else -> "Ready"
            }
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FileDetailsActions(
    item: FileItem,
    onPlayFile: ((FileItem.File) -> Unit)?,
    onDownloadFile: ((FileItem) -> Unit)?,
    onDeleteFile: ((FileItem) -> Unit)?,
    onCopyLink: ((FileItem.File) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Play button (for playable files)
        if (item is FileItem.File && item.isPlayable && onPlayFile != null) {
            var playFocused by remember { mutableStateOf(false) }
            
            TVFocusIndicator(isFocused = playFocused) {
                Button(
                    onClick = { onPlayFile(item) },
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable(
                            onFocusChanged = { playFocused = it.isFocused }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
            }
        }
        
        // Download button
        if (onDownloadFile != null) {
            var downloadFocused by remember { mutableStateOf(false) }
            
            TVFocusIndicator(isFocused = downloadFocused) {
                OutlinedButton(
                    onClick = { onDownloadFile(item) },
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable(
                            onFocusChanged = { downloadFocused = it.isFocused }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download")
                }
            }
        }
        
        // Copy link button (for files with URLs)
        if (item is FileItem.File && item.streamUrl != null && onCopyLink != null) {
            var copyFocused by remember { mutableStateOf(false) }
            
            TVFocusIndicator(isFocused = copyFocused) {
                OutlinedButton(
                    onClick = { onCopyLink(item) },
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable(
                            onFocusChanged = { copyFocused = it.isFocused }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Link")
                }
            }
        }
        
        // Delete button
        if (onDeleteFile != null) {
            var deleteFocused by remember { mutableStateOf(false) }
            
            TVFocusIndicator(isFocused = deleteFocused) {
                OutlinedButton(
                    onClick = { onDeleteFile(item) },
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable(
                            onFocusChanged = { deleteFocused = it.isFocused }
                        ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(2f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Helper functions
private fun getFileTypeIcon(item: FileItem): ImageVector {
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
                FileType.ARCHIVE -> Icons.Default.Archive
                FileType.SUBTITLE -> Icons.Default.Subtitles
                FileType.OTHER -> Icons.Default.InsertDriveFile
            }
        }
    }
}

@Composable
private fun getFileTypeColor(item: FileItem): androidx.compose.ui.graphics.Color {
    return when (item) {
        is FileItem.Folder -> MaterialTheme.colorScheme.primary
        is FileItem.Torrent -> MaterialTheme.colorScheme.secondary
        is FileItem.File -> {
            val extension = item.name.substringAfterLast('.', "").lowercase()
            when (FileType.fromExtension(extension)) {
                FileType.VIDEO -> MaterialTheme.colorScheme.secondary
                FileType.AUDIO -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurface
            }
        }
    }
}

private fun getFileTypeLabel(item: FileItem): String {
    return when (item) {
        is FileItem.Folder -> "Folder"
        is FileItem.Torrent -> "Torrent"
        is FileItem.File -> {
            val extension = item.name.substringAfterLast('.', "").lowercase()
            FileType.fromExtension(extension).displayName
        }
    }
}

private fun getStatusIcon(item: FileItem): ImageVector {
    return when (item) {
        is FileItem.File -> when (item.status) {
            FileStatus.READY -> Icons.Default.CheckCircle
            FileStatus.DOWNLOADING -> Icons.Default.Download
            FileStatus.ERROR -> Icons.Default.Error
            FileStatus.UNAVAILABLE -> Icons.Default.CloudOff
        }
        is FileItem.Torrent -> when (item.status) {
            TorrentStatus.DOWNLOADED -> Icons.Default.CheckCircle
            TorrentStatus.DOWNLOADING -> Icons.Default.Download
            TorrentStatus.ERROR, TorrentStatus.DEAD -> Icons.Default.Error
            else -> Icons.Default.Info
        }
        else -> Icons.Default.Info
    }
}

@Composable
private fun getStatusColor(item: FileItem): androidx.compose.ui.graphics.Color {
    return when (item) {
        is FileItem.File -> when (item.status) {
            FileStatus.READY -> MaterialTheme.colorScheme.primary
            FileStatus.DOWNLOADING -> MaterialTheme.colorScheme.tertiary
            FileStatus.ERROR -> MaterialTheme.colorScheme.error
            FileStatus.UNAVAILABLE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        }
        is FileItem.Torrent -> when (item.status) {
            TorrentStatus.DOWNLOADED -> MaterialTheme.colorScheme.primary
            TorrentStatus.DOWNLOADING -> MaterialTheme.colorScheme.tertiary
            TorrentStatus.ERROR, TorrentStatus.DEAD -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
        else -> MaterialTheme.colorScheme.onSurface
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

private fun formatSpeed(bytesPerSecond: Long): String {
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    var speed = bytesPerSecond.toDouble()
    var unitIndex = 0
    
    while (speed >= 1024 && unitIndex < units.size - 1) {
        speed /= 1024
        unitIndex++
    }
    
    return if (speed >= 100) {
        "${speed.toInt()} ${units[unitIndex]}"
    } else {
        "%.1f ${units[unitIndex]}".format(speed)
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}