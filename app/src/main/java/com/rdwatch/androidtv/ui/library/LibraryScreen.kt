package com.rdwatch.androidtv.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.data.entities.LibraryEntity
import com.rdwatch.androidtv.presentation.components.rememberTVFocusRequester
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.library.components.LibraryFilterBar
import com.rdwatch.androidtv.ui.library.components.LibraryHeader
import com.rdwatch.androidtv.ui.library.components.LibraryItemCard
import com.rdwatch.androidtv.ui.library.components.LibrarySearchBar

/**
 * Main Library Screen - displays user's saved content with filtering and search
 * Optimized for Android TV D-pad navigation
 */
@Composable
fun LibraryScreen(
    onItemClick: (LibraryEntity) -> Unit,
    onBackPressed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val overscanMargin = 32.dp
    val firstFocusRequester = rememberTVFocusRequester()

    // Observe ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    val libraryContent by viewModel.libraryContent.collectAsState()
    val libraryStats by viewModel.libraryStats.collectAsState()

    // Request focus on first composable
    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(overscanMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header with back button and library stats
        LibraryHeader(
            stats = libraryStats.dataOrNull,
            onBackPressed = onBackPressed,
            firstFocusRequester = firstFocusRequester,
        )

        // Search bar
        LibrarySearchBar(
            searchQuery = uiState.searchQuery,
            onSearchQueryChanged = viewModel::setSearchQuery,
            isEnabled = !uiState.isLoading,
        )

        // Filter bar with content type, favorites, downloads, and sort options
        LibraryFilterBar(
            selectedContentType = uiState.selectedContentType,
            showFavoritesOnly = uiState.showFavoritesOnly,
            showDownloadsOnly = uiState.showDownloadsOnly,
            sortBy = uiState.sortBy,
            availableContentTypes = libraryStats.dataOrNull?.availableContentTypes ?: emptyList(),
            onContentTypeSelected = viewModel::setContentTypeFilter,
            onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
            onToggleDownloadsOnly = viewModel::toggleDownloadsOnly,
            onSortByChanged = viewModel::setSortBy,
            onClearFilters = viewModel::clearFilters,
            isEnabled = !uiState.isLoading,
        )

        // Content grid
        LibraryContentGrid(
            contentState = libraryContent,
            onItemClick = onItemClick,
            onToggleFavorite = viewModel::toggleFavorite,
            onRemoveFromLibrary = viewModel::removeFromLibrary,
            isLoading = uiState.isLoading,
            itemCount = uiState.itemCount,
        )
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error message for a brief moment, then clear it
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

/**
 * Library content grid with loading and empty states
 */
@Composable
private fun LibraryContentGrid(
    contentState: UiState<List<LibraryEntity>>,
    onItemClick: (LibraryEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRemoveFromLibrary: (String) -> Unit,
    isLoading: Boolean,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val gridColumns = 4 // Optimized for TV screens

    when {
        contentState.isLoading || isLoading -> {
            LibraryLoadingGrid(
                columns = gridColumns,
                modifier = modifier,
            )
        }

        contentState.isError -> {
            LibraryErrorState(
                message = contentState.errorMessageOrNull ?: "Failed to load library",
                modifier = modifier,
            )
        }

        contentState.isSuccess -> {
            val items = contentState.dataOrNull ?: emptyList()

            if (items.isEmpty()) {
                LibraryEmptyState(
                    itemCount = itemCount,
                    modifier = modifier,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(items, key = { it.libraryId }) { item ->
                        LibraryItemCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onToggleFavorite = { onToggleFavorite(item.contentId) },
                            onRemoveFromLibrary = { onRemoveFromLibrary(item.contentId) },
                            modifier = Modifier.animateItemPlacement(),
                        )
                    }
                }
            }
        }

        else -> {
            // Idle state - show empty library message
            LibraryEmptyState(
                itemCount = 0,
                modifier = modifier,
            )
        }
    }
}

/**
 * Loading state with skeleton grid
 */
@Composable
private fun LibraryLoadingGrid(
    columns: Int,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(8) { // Show 8 skeleton items
            LibraryItemCardSkeleton()
        }
    }
}

/**
 * Skeleton loading card
 */
@Composable
private fun LibraryItemCardSkeleton() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(220.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Image placeholder
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small,
                        ),
            )

            // Title placeholder
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small,
                        ),
            )

            // Description placeholder
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small,
                        ),
            )
        }
    }
}

/**
 * Error state display
 */
@Composable
private fun LibraryErrorState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp),
            )

            Text(
                text = "Error Loading Library",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Empty state display
 */
@Composable
private fun LibraryEmptyState(
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = if (itemCount == 0) Icons.Default.VideoLibrary else Icons.Default.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp),
            )

            Text(
                text = if (itemCount == 0) "Your Library is Empty" else "No Items Match Your Filters",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text =
                    if (itemCount == 0) {
                        "Add content to your library by selecting 'Add to Library' from content details"
                    } else {
                        "Try adjusting your filters or search terms to find content"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}
