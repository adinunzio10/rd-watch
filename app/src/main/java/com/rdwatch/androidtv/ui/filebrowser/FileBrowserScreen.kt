package com.rdwatch.androidtv.ui.filebrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.rdwatch.androidtv.presentation.components.LoadingState
import com.rdwatch.androidtv.presentation.components.EmptyState
import com.rdwatch.androidtv.presentation.components.tvFocusable
import com.rdwatch.androidtv.presentation.state.UiState
import com.rdwatch.androidtv.ui.filebrowser.components.FileBrowserHeader
import com.rdwatch.androidtv.ui.filebrowser.components.FileBrowserToolbar
import com.rdwatch.androidtv.ui.filebrowser.components.FileItem
import com.rdwatch.androidtv.ui.filebrowser.models.FileItemUiModel
import com.rdwatch.androidtv.ui.filebrowser.models.FileSortOption
import com.rdwatch.androidtv.ui.filebrowser.state.FileBrowserUiState

/**
 * Real Debrid File Browser Screen
 * 
 * Displays files from Real Debrid account with TV-optimized navigation,
 * sorting, filtering, and bulk operations.
 */
@Composable
fun FileBrowserScreen(
    onBackPressed: () -> Unit,
    onFileSelected: (FileItemUiModel) -> Unit,
    onPlayFile: (FileItemUiModel) -> Unit,
    onDeleteFile: (FileItemUiModel) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FileBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val firstFocusRequester = remember { FocusRequester() }
    val overscanMargin = 32.dp
    
    // Handle initial focus
    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }
    
    // Main layout with TV-optimized spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(overscanMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button and title
        FileBrowserHeader(
            onBackPressed = onBackPressed,
            firstFocusRequester = firstFocusRequester,
            isSelectionMode = uiState.isSelectionMode,
            selectedCount = uiState.selectedFiles.size,
            onClearSelection = { viewModel.clearSelection() }
        )
        
        // Toolbar with sorting, search, and actions
        FileBrowserToolbar(
            sortOption = uiState.sortOption,
            onSortChange = { viewModel.setSortOption(it) },
            searchQuery = uiState.searchQuery,
            onSearchChange = { viewModel.setSearchQuery(it) },
            isSelectionMode = uiState.isSelectionMode,
            selectedCount = uiState.selectedFiles.size,
            onBulkDelete = { viewModel.deleteSelectedFiles() },
            onBulkPlay = { viewModel.playSelectedFiles() },
            onToggleSelectionMode = { viewModel.toggleSelectionMode() },
            onRefresh = onRefresh
        )
        
        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState(
                        message = "Loading files...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                uiState.error != null -> {
                    SimpleErrorState(
                        message = uiState.error ?: "Unknown error",
                        onRetry = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                uiState.filteredFiles.isEmpty() -> {
                    EmptyState(
                        title = if (uiState.searchQuery.isNotEmpty()) "No files found" else "No files available",
                        description = if (uiState.searchQuery.isNotEmpty()) 
                            "Try adjusting your search query" 
                        else 
                            "Your Real Debrid account doesn't have any files yet",
                        icon = if (uiState.searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                else -> {
                    // File list
                    FileBrowserContent(
                        files = uiState.filteredFiles,
                        isSelectionMode = uiState.isSelectionMode,
                        selectedFiles = uiState.selectedFiles,
                        onFileClick = { file ->
                            if (uiState.isSelectionMode) {
                                viewModel.toggleFileSelection(file)
                            } else {
                                onFileSelected(file)
                            }
                        },
                        onFileLongPress = { file ->
                            if (!uiState.isSelectionMode) {
                                viewModel.toggleSelectionMode()
                            }
                            viewModel.toggleFileSelection(file)
                        },
                        onPlayFile = onPlayFile,
                        onDeleteFile = onDeleteFile,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Pull to refresh indicator
            if (uiState.isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

/**
 * Content area showing the file list
 */
@Composable
private fun FileBrowserContent(
    files: List<FileItemUiModel>,
    isSelectionMode: Boolean,
    selectedFiles: Set<String>,
    onFileClick: (FileItemUiModel) -> Unit,
    onFileLongPress: (FileItemUiModel) -> Unit,
    onPlayFile: (FileItemUiModel) -> Unit,
    onDeleteFile: (FileItemUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = files,
            key = { file -> file.id }
        ) { file ->
            FileItem(
                file = file,
                isSelectionMode = isSelectionMode,
                isSelected = selectedFiles.contains(file.id),
                onClick = { onFileClick(file) },
                onLongPress = { onFileLongPress(file) },
                onPlayFile = { onPlayFile(file) },
                onDeleteFile = { onDeleteFile(file) }
            )
        }
    }
}

/**
 * Simple error state component
 */
@Composable
private fun SimpleErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            if (onRetry != null) {
                var retryButtonFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.tvFocusable(
                        onFocusChanged = { retryButtonFocused = it }
                    )
                ) {
                    Text("Retry")
                }
            }
        }
    }
}