package com.rdwatch.androidtv.ui.filebrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.StateFlow
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.ui.filebrowser.components.*
import java.text.SimpleDateFormat
import java.util.*

/** Account File Browser Screen for browsing debrid service files with TV-optimized UI */
@Composable
fun AccountFileBrowserScreen(
    modifier: Modifier = Modifier,
    onFileClick: (FileItem.File) -> Unit = {},
    onFolderClick: (FileItem.Folder) -> Unit = {},
    onTorrentClick: (FileItem.Torrent) -> Unit = {},
    onBackPressed: () -> Unit = {},
    viewModel: AccountFileBrowserViewModel = hiltViewModel()
) {
    val overscanMargin = 24.dp
    val firstFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    
    // Observe ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    
    // Dialog states
    var showFileDetails by remember { mutableStateOf<FileItem?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var isFilterPanelExpanded by remember { mutableStateOf(false) }
    
    // Handle ViewModel events
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is FileBrowserEvent.ShowFileDetails -> {
                    showFileDetails = event.file
                }
                is FileBrowserEvent.NavigateToPlayer -> {
                    // Handle navigation to player
                    // This will be implemented based on your navigation setup
                }
                is FileBrowserEvent.ShowError -> {
                    // Handle error display - you can implement a snackbar or toast here
                }
                is FileBrowserEvent.ShowSuccess -> {
                    // Handle success message display
                }
                is FileBrowserEvent.NavigateBack -> {
                    onBackPressed()
                }
                is FileBrowserEvent.ShowConfirmDialog -> {
                    // Handle confirmation dialog - you can implement this based on your needs
                }
                is FileBrowserEvent.ViewModeChanged -> {
                    // View mode changed - UI will update automatically via state
                }
                else -> {
                    // Handle other events as needed
                }
            }
        }
    }
    
    LaunchedEffect(Unit) { 
        firstFocusRequester.requestFocus() 
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(overscanMargin),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with navigation and account info
        FileBrowserHeader(
            currentPath = uiState.currentPath,
            accountType = uiState.accountType,
            onBackPressed = onBackPressed,
            onNavigateBack = { viewModel.navigateBack() },
            firstFocusRequester = firstFocusRequester
        )
        
        // Bulk Selection Mode Bar
        BulkSelectionModeBar(
            isEnabled = uiState.isMultiSelectMode,
            selectionState = viewModel.getSelectionState(),
            onToggleBulkMode = { viewModel.toggleMultiSelect() },
            onSelectAll = { viewModel.selectAll() },
            onDeselectAll = { viewModel.clearSelection() },
            onDownloadSelected = { viewModel.downloadSelectedFiles() },
            onDeleteSelected = { viewModel.deleteSelectedFiles() },
            onPlaySelected = { viewModel.playSelectedFiles() }
        )
        
        // Enhanced sorting controls, view mode selector, and multi-select in single compact row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced Sorting UI
            SortingControlPanel(
                sortingOptions = uiState.sortingOptions,
                onSortingChange = { viewModel.updateSorting(it) },
                displayMode = SortDisplayMode.DROPDOWN,
                modifier = Modifier.weight(1f)
            )
            
            // View mode selector
            ViewModeSelector(
                currentViewMode = uiState.viewMode,
                onViewModeChange = { viewModel.changeViewMode(it) }
            )
            
            // Multi-select toggle button
            if (!uiState.isMultiSelectMode) {
                var multiSelectFocused by remember { mutableStateOf(false) }
                
                TVFocusIndicator(isFocused = multiSelectFocused) {
                    IconButton(
                        onClick = { viewModel.toggleMultiSelect() },
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusable(
                                onFocusChanged = { multiSelectFocused = it.isFocused }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckBox,
                            contentDescription = "Enable multi-select",
                            modifier = Modifier.size(20.dp),
                            tint = if (multiSelectFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
        
        // Enhanced Filter Panel
        EnhancedFilterPanel(
            filterOptions = uiState.filterOptions,
            onFilterChange = { viewModel.updateFilter(it) },
            isExpanded = isFilterPanelExpanded,
            onToggleExpanded = { isFilterPanelExpanded = !isFilterPanelExpanded }
        )
        
        // File list content
        FileBrowserContent(
            contentState = uiState.contentState,
            selectedItems = uiState.selectedItems,
            isMultiSelectMode = uiState.isMultiSelectMode,
            paginationState = uiState.paginationState,
            viewMode = uiState.viewMode,
            onFileClick = onFileClick,
            onFolderClick = onFolderClick,
            onTorrentClick = onTorrentClick,
            onItemLongClick = { viewModel.enterBulkSelectionMode(it.id) },
            onItemSelect = { viewModel.toggleItemSelection(it.id) },
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMoreContent() },
            listState = listState
        )
    }
    
    // File Details Dialog
    showFileDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { showFileDetails = null },
            onPlayFile = { file ->
                showFileDetails = null
                viewModel.selectFile(file)
            },
            onDownloadFile = { fileItem ->
                showFileDetails = null
                viewModel.downloadSelectedFiles()
            },
            onDeleteFile = { fileItem ->
                showFileDetails = null
                viewModel.deleteSelectedFiles()
            },
            onCopyLink = { file ->
                showFileDetails = null
                // Handle copy link - you can implement clipboard functionality here
            }
        )
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FileBrowserFilterDialog(
            filterOptions = uiState.filterOptions,
            onFilterChange = { viewModel.updateFilter(it) },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun FileBrowserHeader(
    currentPath: String,
    accountType: AccountType,
    onBackPressed: () -> Unit,
    onNavigateBack: () -> Unit,
    firstFocusRequester: FocusRequester
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top row with back button and account type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            var backButtonFocused by remember { mutableStateOf(false) }
            
            TVFocusIndicator(isFocused = backButtonFocused) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier
                        .focusRequester(firstFocusRequester)
                        .tvFocusable(
                            onFocusChanged = { backButtonFocused = it.isFocused }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (backButtonFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
            }
            
            // Account type badge
            Surface(
                modifier = Modifier,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = accountType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Navigate back button (if not root)
            if (currentPath != "/") {
                var navBackFocused by remember { mutableStateOf(false) }
                
                TVFocusIndicator(isFocused = navBackFocused) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.tvFocusable(
                            onFocusChanged = { navBackFocused = it.isFocused }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Go up",
                            tint = if (navBackFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }
                }
            }
        }
        
        // Current path breadcrumb
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = if (currentPath == "/") "Root" else currentPath,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
private fun FileBrowserContent(
    contentState: com.rdwatch.androidtv.ui.common.UiState<List<FileItem>>,
    selectedItems: Set<String>,
    isMultiSelectMode: Boolean,
    paginationState: PaginationState,
    viewMode: ViewMode,
    onFileClick: (FileItem.File) -> Unit,
    onFolderClick: (FileItem.Folder) -> Unit,
    onTorrentClick: (FileItem.Torrent) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onItemSelect: (FileItem) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    when (contentState) {
        is com.rdwatch.androidtv.ui.common.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Loading files...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        is com.rdwatch.androidtv.ui.common.UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = contentState.message,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    var retryFocused by remember { mutableStateOf(false) }
                    
                    TVFocusIndicator(isFocused = retryFocused) {
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier.tvFocusable(
                                onFocusChanged = { retryFocused = it.isFocused }
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
        is com.rdwatch.androidtv.ui.common.UiState.Success -> {
            if (contentState.data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No files found",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "This folder is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                when (viewMode) {
                    ViewMode.LIST -> {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(contentState.data) { item ->
                                SelectableFileItem(
                                    item = item,
                                    isSelected = selectedItems.contains(item.id),
                                    isMultiSelectMode = isMultiSelectMode,
                                    onSelect = { onItemSelect(it) },
                                    onLongPress = { onItemLongClick(it) },
                                    onClick = { clickedItem ->
                                        when (clickedItem) {
                                            is FileItem.File -> onFileClick(clickedItem)
                                            is FileItem.Folder -> onFolderClick(clickedItem)
                                            is FileItem.Torrent -> onTorrentClick(clickedItem)
                                        }
                                    }
                                )
                            }
                            
                            // Pagination loading item
                            if (paginationState.hasMore || paginationState.isLoadingMore) {
                                item {
                                    LoadMoreItem(
                                        isLoading = paginationState.isLoadingMore,
                                        hasMore = paginationState.hasMore,
                                        onLoadMore = onLoadMore
                                    )
                                }
                            }
                        }
                        
                        // Auto-trigger load more for list view
                        LaunchedEffect(listState) {
                            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                                .collect { visibleItems ->
                                    val totalItems = listState.layoutInfo.totalItemsCount
                                    val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: 0
                                    
                                    if (totalItems > 0 && lastVisibleIndex >= totalItems - 5 && paginationState.hasMore && !paginationState.isLoadingMore) {
                                        onLoadMore()
                                    }
                                }
                        }
                    }
                    
                    ViewMode.TILES -> {
                        val gridState = rememberLazyGridState()
                        
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 180.dp),
                            state = gridState,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(contentState.data) { item ->
                                TileViewItem(
                                    item = item,
                                    isSelected = selectedItems.contains(item.id),
                                    isMultiSelectMode = isMultiSelectMode,
                                    onSelect = { onItemSelect(it) },
                                    onLongPress = { onItemLongClick(it) },
                                    onClick = { clickedItem ->
                                        when (clickedItem) {
                                            is FileItem.File -> onFileClick(clickedItem)
                                            is FileItem.Folder -> onFolderClick(clickedItem)
                                            is FileItem.Torrent -> onTorrentClick(clickedItem)
                                        }
                                    }
                                )
                            }
                            
                            // Pagination loading item for tiles
                            if (paginationState.hasMore || paginationState.isLoadingMore) {
                                item {
                                    LoadMoreItem(
                                        isLoading = paginationState.isLoadingMore,
                                        hasMore = paginationState.hasMore,
                                        onLoadMore = onLoadMore,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        
                        // Auto-trigger load more for tiles view
                        LaunchedEffect(gridState) {
                            snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
                                .collect { visibleItems ->
                                    val totalItems = gridState.layoutInfo.totalItemsCount
                                    val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: 0
                                    
                                    if (totalItems > 0 && lastVisibleIndex >= totalItems - 10 && paginationState.hasMore && !paginationState.isLoadingMore) {
                                        onLoadMore()
                                    }
                                }
                        }
                    }
                    
                    ViewMode.GRID -> {
                        val gridState = rememberLazyGridState()
                        
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            state = gridState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(contentState.data) { item ->
                                GridViewItem(
                                    item = item,
                                    isSelected = selectedItems.contains(item.id),
                                    isMultiSelectMode = isMultiSelectMode,
                                    onSelect = { onItemSelect(it) },
                                    onLongPress = { onItemLongClick(it) },
                                    onClick = { clickedItem ->
                                        when (clickedItem) {
                                            is FileItem.File -> onFileClick(clickedItem)
                                            is FileItem.Folder -> onFolderClick(clickedItem)
                                            is FileItem.Torrent -> onTorrentClick(clickedItem)
                                        }
                                    }
                                )
                            }
                            
                            // Pagination loading item for grid
                            if (paginationState.hasMore || paginationState.isLoadingMore) {
                                item {
                                    LoadMoreItem(
                                        isLoading = paginationState.isLoadingMore,
                                        hasMore = paginationState.hasMore,
                                        onLoadMore = onLoadMore,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        
                        // Auto-trigger load more for grid view
                        LaunchedEffect(gridState) {
                            snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
                                .collect { visibleItems ->
                                    val totalItems = gridState.layoutInfo.totalItemsCount
                                    val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: 0
                                    
                                    if (totalItems > 0 && lastVisibleIndex >= totalItems - 15 && paginationState.hasMore && !paginationState.isLoadingMore) {
                                        onLoadMore()
                                    }
                                }
                        }
                    }
                }
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.width(40.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = color.copy(alpha = 0.7f)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
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
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        FileStatus.UNAVAILABLE -> {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Unavailable",
                modifier = Modifier.size(16.dp),
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            TorrentStatus.DOWNLOADING -> {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.width(40.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.3f)
                )
            }
            TorrentStatus.DOWNLOADED -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            TorrentStatus.ERROR, TorrentStatus.DEAD -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                Text(
                    text = status.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

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
                FileType.ARCHIVE -> Icons.Default.Folder
                FileType.SUBTITLE -> Icons.Default.TextFields
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

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
private fun LoadMoreItem(
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Loading more...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else if (hasMore) {
            TVFocusIndicator(isFocused = isFocused) {
                OutlinedButton(
                    onClick = onLoadMore,
                    modifier = Modifier.tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load More")
                }
            }
        }
    }
}

