package com.rdwatch.androidtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.data.entities.ShowProgressEntity
import com.rdwatch.androidtv.data.repository.NextEpisodeResult
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Continue Watching row component for home screen
 * Shows next episodes to watch with smart episode progression
 */
@Composable
fun ContinueWatchingRow(
    continueWatchingItems: List<ContinueWatchingItem>,
    onPlayEpisode: (ContinueWatchingItem) -> Unit,
    onManageContinueWatching: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (continueWatchingItems.isEmpty()) return

    val listState = rememberLazyListState()
    val firstItemFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section header
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Continue Watching",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )

            TextButton(
                onClick = onManageContinueWatching,
                modifier = Modifier.tvFocusable(),
            ) {
                Text(
                    text = "Manage",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // Horizontal list of continue watching items
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(continueWatchingItems) { item ->
                ContinueWatchingCard(
                    item = item,
                    onPlayClick = { onPlayEpisode(item) },
                    modifier =
                        if (item == continueWatchingItems.first()) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier
                        },
                )
            }
        }
    }

    // Auto-focus first item when row appears
    LaunchedEffect(continueWatchingItems) {
        if (continueWatchingItems.isNotEmpty()) {
            firstItemFocusRequester.requestFocus()
        }
    }
}

/**
 * Individual continue watching card for a show/episode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(
        isFocused = isFocused,
    ) {
        Card(
            onClick = onPlayClick,
            modifier =
                modifier
                    .width(320.dp)
                    .height(180.dp)
                    .tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused },
                    ),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = if (isFocused) 8.dp else 2.dp,
                ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Background image
                SmartTVImageLoader(
                    imageUrl = item.backdropUrl ?: item.posterUrl,
                    contentDescription = item.showTitle,
                    contentScale = ContentScale.Crop,
                    priority = ImagePriority.NORMAL,
                    modifier = Modifier.fillMaxSize(),
                )

                // Gradient overlay
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                brush =
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.7f),
                                            ),
                                        startY = 0f,
                                        endY = Float.POSITIVE_INFINITY,
                                    ),
                            ),
                )

                // Content overlay
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Top section with new season indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (item.isNewSeason) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Text(
                                    text = "New Season",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        // Auto-play indicator
                        if (item.canAutoPlay) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Auto-play enabled",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Bottom section with show info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Show title
                        Text(
                            text = item.showTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Episode info
                        Text(
                            text = item.nextEpisodeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Progress indicator
                        if (item.progressPercentage > 0f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                LinearProgressIndicator(
                                    progress = { item.progressPercentage },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .height(3.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.White.copy(alpha = 0.3f),
                                )
                                Text(
                                    text = "${(item.progressPercentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }

                // Play button overlay when focused
                if (isFocused) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a continue watching item
 */
data class ContinueWatchingItem(
    val showProgressEntity: ShowProgressEntity,
    val nextEpisodeResult: NextEpisodeResult,
    val showTitle: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val nextEpisodeText: String,
    val progressPercentage: Float,
    val isNewSeason: Boolean,
    val canAutoPlay: Boolean,
) {
    companion object {
        /**
         * Create ContinueWatchingItem from ShowProgressEntity and NextEpisodeResult
         */
        fun from(
            showProgress: ShowProgressEntity,
            nextEpisode: NextEpisodeResult,
            posterUrl: String? = null,
            backdropUrl: String? = null,
        ): ContinueWatchingItem {
            return ContinueWatchingItem(
                showProgressEntity = showProgress,
                nextEpisodeResult = nextEpisode,
                showTitle = showProgress.showTitle,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                nextEpisodeText = nextEpisode.getDisplayText(),
                progressPercentage = showProgress.showCompletionPercentage,
                isNewSeason = nextEpisode.isNewSeason,
                canAutoPlay = nextEpisode.canAutoPlay,
            )
        }
    }
}
