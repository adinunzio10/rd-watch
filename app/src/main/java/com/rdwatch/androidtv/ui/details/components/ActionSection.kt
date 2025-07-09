package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.presentation.components.tvFocusable

/**
 * Action section component for content detail screens
 * Displays action buttons like Play, Add to Watchlist, Like, Share, Download, etc.
 */
@Composable
fun ActionSection(
    content: ContentDetail,
    onActionClick: (ContentAction) -> Unit,
    modifier: Modifier = Modifier,
    maxVisibleActions: Int = 5,
    showLabels: Boolean = true
) {
    val availableActions = content.actions.take(maxVisibleActions)
    
    if (availableActions.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section title (optional)
            Text(
                text = "Actions",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            
            // Action buttons row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(availableActions) { action ->
                    ActionButton(
                        action = action,
                        onClick = { onActionClick(action) },
                        showLabel = showLabels
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButton(
    action: ContentAction,
    onClick: () -> Unit,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    val isEnabled = when (action) {
        is ContentAction.Download -> !action.isDownloading
        else -> true
    }
    
    TVFocusIndicator(isFocused = isFocused) {
        OutlinedCard(
            onClick = {
                if (isEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            },
            enabled = isEnabled,
            modifier = modifier
                .width(if (showLabel) 140.dp else 64.dp)
                .tvFocusable(
                    enabled = isEnabled,
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = when {
                    !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            border = if (isFocused) {
                CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary
                        )
                    ),
                    width = 2.dp
                )
            } else {
                CardDefaults.outlinedCardBorder()
            }
        ) {
            ActionButtonContent(
                action = action,
                isFocused = isFocused,
                isEnabled = isEnabled,
                showLabel = showLabel
            )
        }
    }
}

@Composable
private fun ActionButtonContent(
    action: ContentAction,
    isFocused: Boolean,
    isEnabled: Boolean,
    showLabel: Boolean
) {
    val icon = getActionIcon(action)
    val contentColor = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    if (showLabel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(64.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = action.title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Primary action button (usually Play) displayed prominently
 */
@Composable
fun PrimaryActionButton(
    action: ContentAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    TVFocusIndicator(isFocused = isFocused) {
        Button(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = modifier
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                }
            ),
            contentPadding = if (isLarge) {
                PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            } else {
                PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            },
            shape = RoundedCornerShape(if (isLarge) 12.dp else 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getActionIcon(action),
                    contentDescription = null,
                    modifier = Modifier.size(if (isLarge) 28.dp else 24.dp)
                )
                Text(
                    text = action.title,
                    style = if (isLarge) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Secondary action buttons displayed in a compact row
 */
@Composable
fun SecondaryActionRow(
    actions: List<ContentAction>,
    onActionClick: (ContentAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(actions) { action ->
            SecondaryActionButton(
                action = action,
                onClick = { onActionClick(action) }
            )
        }
    }
}

@Composable
private fun SecondaryActionButton(
    action: ContentAction,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    TVFocusIndicator(isFocused = isFocused) {
        FilledTonalButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = Modifier
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getActionIcon(action),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Get the appropriate icon for an action
 */
private fun getActionIcon(action: ContentAction): ImageVector {
    return when (action) {
        is ContentAction.Play -> Icons.Default.PlayArrow
        is ContentAction.AddToWatchlist -> if (action.isInWatchlist) Icons.Default.Remove else Icons.Default.Add
        is ContentAction.Like -> if (action.isLiked) Icons.Default.Favorite else Icons.Default.ThumbUp
        is ContentAction.Share -> Icons.Default.Share
        is ContentAction.Download -> {
            when {
                action.isDownloaded -> Icons.Default.CloudDone
                action.isDownloading -> Icons.Default.Download
                else -> Icons.Default.Download
            }
        }
        is ContentAction.Delete -> Icons.Default.Delete
        is ContentAction.Custom -> Icons.Default.Star // Default icon for custom actions
    }
}

/**
 * Action status indicator for showing loading states
 */
@Composable
fun ActionStatusIndicator(
    action: ContentAction,
    modifier: Modifier = Modifier
) {
    when (action) {
        is ContentAction.Download -> {
            if (action.isDownloading) {
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Downloading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        else -> {
            // No status indicator for other actions
        }
    }
}

/**
 * Preview/Demo configurations for ActionSection
 */
object ActionSectionPreview {
    fun createSampleMovieContent(): ContentDetail {
        return object : ContentDetail {
            override val id: String = "1"
            override val title: String = "Sample Movie"
            override val description: String? = "A sample movie"
            override val backgroundImageUrl: String? = null
            override val cardImageUrl: String? = null
            override val contentType: ContentType = ContentType.MOVIE
            override val videoUrl: String? = "https://example.com/video.mp4"
            override val metadata: ContentMetadata = ContentMetadata()
            override val actions: List<ContentAction> = listOf(
                ContentAction.Play(isResume = false),
                ContentAction.AddToWatchlist(isInWatchlist = false),
                ContentAction.Like(isLiked = false),
                ContentAction.Share(),
                ContentAction.Download(isDownloaded = false, isDownloading = false)
            )
        }
    }
    
    fun createSampleContentWithDownloading(): ContentDetail {
        return object : ContentDetail {
            override val id: String = "2"
            override val title: String = "Downloading Movie"
            override val description: String? = "A movie being downloaded"
            override val backgroundImageUrl: String? = null
            override val cardImageUrl: String? = null
            override val contentType: ContentType = ContentType.MOVIE
            override val videoUrl: String? = "https://example.com/video.mp4"
            override val metadata: ContentMetadata = ContentMetadata()
            override val actions: List<ContentAction> = listOf(
                ContentAction.Play(isResume = true),
                ContentAction.AddToWatchlist(isInWatchlist = true),
                ContentAction.Like(isLiked = true),
                ContentAction.Share(),
                ContentAction.Download(isDownloaded = false, isDownloading = true)
            )
        }
    }
}