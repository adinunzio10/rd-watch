package com.rdwatch.androidtv.ui.library.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.data.entities.LibraryEntity
import com.rdwatch.androidtv.presentation.components.tvFocusable
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.details.models.ContentType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Library item card component with TV focus handling and quick actions
 * Shows content poster, title, metadata, and interactive elements
 */
@Composable
fun LibraryItemCard(
    item: LibraryEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(false) }

    // Animations
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "card_scale",
    )

    val borderColor =
        if (isFocused) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        }

    // Show quick actions when focused for a period
    LaunchedEffect(isFocused) {
        if (isFocused) {
            kotlinx.coroutines.delay(1000) // Show after 1 second of focus
            showQuickActions = true
        } else {
            showQuickActions = false
        }
    }

    Card(
        onClick = onClick,
        modifier =
            modifier
                .width(200.dp)
                .height(280.dp)
                .scale(scale)
                .border(
                    width = 3.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .tvFocusable { focused -> isFocused = focused },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 8.dp else 4.dp,
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Poster image
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                ) {
                    SmartTVImageLoader(
                        imageUrl = item.thumbnailUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        // Uses default placeholder from SmartTVImageLoader
                    )

                    // Content type badge
                    ContentTypeBadge(
                        contentType = item.contentType,
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                    )

                    // Favorite and download indicators
                    StatusIndicators(
                        isFavorite = item.isFavorite,
                        isDownloaded = item.isDownloaded,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                    )
                }

                // Content info
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Title
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Description or metadata
                    item.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Added date
                    Text(
                        text = "Added ${formatDate(item.addedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Quick actions overlay (simplified for now)
            if (showQuickActions && isFocused) {
                QuickActionsOverlay(
                    isFavorite = item.isFavorite,
                    onToggleFavorite = onToggleFavorite,
                    onRemoveFromLibrary = onRemoveFromLibrary,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * Content type badge
 */
@Composable
private fun ContentTypeBadge(
    contentType: String,
    modifier: Modifier = Modifier,
) {
    val displayType =
        try {
            ContentType.valueOf(contentType).getDisplayName()
        } catch (e: Exception) {
            contentType.uppercase()
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
    ) {
        Text(
            text = displayType,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Status indicators (favorite and download)
 */
@Composable
private fun StatusIndicators(
    isFavorite: Boolean,
    isDownloaded: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }

        if (isDownloaded) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Downloaded",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Quick actions overlay with favorite and remove buttons
 */
@Composable
private fun QuickActionsOverlay(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(12.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Toggle favorite button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.tvFocusable(),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Remove from library button
            IconButton(
                onClick = onRemoveFromLibrary,
                modifier = Modifier.tvFocusable(),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from library",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Helper functions
 */
private fun ContentType.getDisplayName(): String =
    when (this) {
        ContentType.MOVIE -> "Movie"
        ContentType.TV_SHOW -> "TV Show"
        ContentType.TV_EPISODE -> "Episode"
        ContentType.DOCUMENTARY -> "Doc"
        ContentType.SPORTS -> "Sports"
        ContentType.MUSIC_VIDEO -> "Music"
        ContentType.PODCAST -> "Podcast"
    }

private fun formatDate(date: Date): String {
    val now = Date()
    val diffInMillis = now.time - date.time
    val days = diffInMillis / (1000 * 60 * 60 * 24)

    return when {
        days < 1 -> "today"
        days < 7 -> "${days}d ago"
        days < 30 -> "${days / 7}w ago"
        days < 365 -> "${days / 30}mo ago"
        else -> SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
    }
}
