package com.rdwatch.androidtv.ui.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.presentation.components.tvFocusable
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.library.LibrarySortBy

/**
 * Filter bar component with content type, favorites, downloads, and sort options
 * Optimized for Android TV D-pad navigation
 */
@Composable
fun LibraryFilterBar(
    selectedContentType: ContentType?,
    showFavoritesOnly: Boolean,
    showDownloadsOnly: Boolean,
    sortBy: LibrarySortBy,
    availableContentTypes: List<ContentType>,
    onContentTypeSelected: (ContentType?) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onToggleDownloadsOnly: () -> Unit,
    onSortByChanged: (LibrarySortBy) -> Unit,
    onClearFilters: () -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val hasActiveFilters = selectedContentType != null || showFavoritesOnly || showDownloadsOnly

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Filter chips row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            // Content type filters
            if (availableContentTypes.isNotEmpty()) {
                item {
                    FilterChip(
                        selected = selectedContentType == null,
                        onClick = { onContentTypeSelected(null) },
                        label = { Text("All") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        enabled = isEnabled,
                        modifier = Modifier.tvFocusable(),
                    )
                }

                items(availableContentTypes) { contentType ->
                    FilterChip(
                        selected = selectedContentType == contentType,
                        onClick = {
                            onContentTypeSelected(
                                if (selectedContentType == contentType) null else contentType,
                            )
                        },
                        label = { Text(contentType.getDisplayName()) },
                        leadingIcon = {
                            Icon(
                                imageVector = contentType.getIcon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        enabled = isEnabled,
                        modifier = Modifier.tvFocusable(),
                    )
                }
            }

            // Favorites filter
            item {
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = onToggleFavoritesOnly,
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    enabled = isEnabled,
                    modifier = Modifier.tvFocusable(),
                )
            }

            // Downloads filter
            item {
                FilterChip(
                    selected = showDownloadsOnly,
                    onClick = onToggleDownloadsOnly,
                    label = { Text("Downloaded") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    enabled = isEnabled,
                    modifier = Modifier.tvFocusable(),
                )
            }
        }

        // Sort and clear row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sort options
            SortOptionsRow(
                currentSortBy = sortBy,
                onSortByChanged = onSortByChanged,
                isEnabled = isEnabled,
            )

            // Clear filters button
            if (hasActiveFilters) {
                TextButton(
                    onClick = onClearFilters,
                    enabled = isEnabled,
                    modifier = Modifier.tvFocusable(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Filters")
                }
            }
        }
    }
}

/**
 * Sort options row
 */
@Composable
private fun SortOptionsRow(
    currentSortBy: LibrarySortBy,
    onSortByChanged: (LibrarySortBy) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Sort by:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )

        LibrarySortBy.values().forEach { sortOption ->
            FilterChip(
                selected = currentSortBy == sortOption,
                onClick = { onSortByChanged(sortOption) },
                label = {
                    Text(
                        text = sortOption.getDisplayName(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                enabled = isEnabled,
                modifier = Modifier.tvFocusable(),
            )
        }
    }
}

/**
 * Extension functions for display names and icons
 */
private fun ContentType.getDisplayName(): String =
    when (this) {
        ContentType.MOVIE -> "Movies"
        ContentType.TV_SHOW -> "TV Shows"
        ContentType.TV_EPISODE -> "Episodes"
        ContentType.DOCUMENTARY -> "Documentaries"
        ContentType.SPORTS -> "Sports"
        ContentType.MUSIC_VIDEO -> "Music Videos"
        ContentType.PODCAST -> "Podcasts"
    }

private fun ContentType.getIcon(): ImageVector =
    when (this) {
        ContentType.MOVIE -> Icons.Default.Movie
        ContentType.TV_SHOW -> Icons.Default.Tv
        ContentType.TV_EPISODE -> Icons.Default.PlaylistPlay
        ContentType.DOCUMENTARY -> Icons.Default.DocumentScanner
        ContentType.SPORTS -> Icons.Default.SportsFootball
        ContentType.MUSIC_VIDEO -> Icons.Default.MusicVideo
        ContentType.PODCAST -> Icons.Default.Podcasts
    }

private fun LibrarySortBy.getDisplayName(): String =
    when (this) {
        LibrarySortBy.RECENTLY_ADDED -> "Recently Added"
        LibrarySortBy.ALPHABETICAL -> "A-Z"
        LibrarySortBy.LAST_UPDATED -> "Last Updated"
    }
