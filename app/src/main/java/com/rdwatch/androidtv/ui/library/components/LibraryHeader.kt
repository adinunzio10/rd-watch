package com.rdwatch.androidtv.ui.library.components

import androidx.compose.foundation.layout.*
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
import com.rdwatch.androidtv.presentation.components.tvFocusable
import com.rdwatch.androidtv.ui.library.LibraryStats

/**
 * Header component for Library Screen with back button and statistics
 */
@Composable
fun LibraryHeader(
    stats: LibraryStats?,
    onBackPressed: () -> Unit,
    firstFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button and title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconButton(
                onClick = onBackPressed,
                modifier =
                    Modifier
                        .focusRequester(firstFocusRequester)
                        .tvFocusable(),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = "My Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Statistics
        stats?.let { libraryStats ->
            LibraryStatsRow(
                stats = libraryStats,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

/**
 * Display library statistics in a horizontal row
 */
@Composable
private fun LibraryStatsRow(
    stats: LibraryStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Total items
        StatItem(
            icon = Icons.Default.VideoLibrary,
            label = "Total",
            value = stats.totalItems.toString(),
        )

        // Favorites
        if (stats.favoriteItems > 0) {
            StatItem(
                icon = Icons.Default.Favorite,
                label = "Favorites",
                value = stats.favoriteItems.toString(),
            )
        }

        // Downloaded size
        if (stats.downloadedSizeBytes > 0) {
            StatItem(
                icon = Icons.Default.Download,
                label = "Downloaded",
                value = stats.getDownloadedSizeFormatted(),
            )
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )

        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
