package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rdwatch.androidtv.R
import com.rdwatch.androidtv.presentation.components.tvCardFocus
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.ui.details.models.TVEpisode
import com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata
import com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution
import com.rdwatch.androidtv.ui.theme.RdwatchTheme

/**
 * Episode card component for displaying individual TV show episodes
 * Optimized for Android TV with focus handling, progress indicators, and source availability
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeCard(
    episode: TVEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    showProgress: Boolean = true,
    aspectRatio: Float = 16f / 9f,
    // Source-related parameters
    availableSources: List<SourceMetadata> = emptyList(),
    isLoadingSources: Boolean = false,
    onSourceSelectionClick: (() -> Unit)? = null,
    showSourceIndicators: Boolean = true
) {
    var focused by remember(episode.id) { mutableStateOf(isFocused) }
    
    // Update focus state when isFocused changes
    LaunchedEffect(isFocused) {
        focused = isFocused
    }
    
    Card(
        modifier = modifier
            .width(240.dp)
            .height(140.dp) // Optimized for better screen utilization
            .tvCardFocus(focused, onClick)
            .tvFocusable(onFocusChanged = { focused = it.isFocused })
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (focused) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Episode thumbnail with progress overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            ) {
                // Episode thumbnail
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.thumbnailUrl ?: episode.stillPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Episode ${episode.episodeNumber} thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = null,
                    error = null
                )
                
                // Progress indicator overlay
                if (showProgress && episode.hasProgress()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = 0.3f)
                            )
                    ) {
                        // Progress bar
                        if (episode.isPartiallyWatched()) {
                            LinearProgressIndicator(
                                progress = episode.watchProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.BottomCenter),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                        
                        // Watch status indicator
                        if (episode.isWatched) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Watched",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (episode.isPartiallyWatched()) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        // Source availability indicators
                        if (showSourceIndicators) {
                            SourceAvailabilityIndicators(
                                sources = availableSources,
                                isLoading = isLoadingSources,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                            )
                        }
                        
                        // Episode runtime
                        episode.getFormattedRuntime()?.let { runtime ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = runtime,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            // Episode information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Take remaining space
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Episode title
                Text(
                    text = episode.getFormattedTitle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp, // Slightly larger for TV viewing
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2, // Allow 2 lines for episode titles
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                
                // Episode description
                if (episode.getDisplayDescription().isNotBlank()) {
                    Text(
                        text = episode.getDisplayDescription(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp, // Slightly larger for better readability
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                        lineHeight = 16.sp
                    )
                }
                
                // Episode metadata
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Air date
                    episode.airDate?.let { airDate ->
                        Text(
                            text = airDate,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                    
                    // Rating and source info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Source count indicator
                        if (showSourceIndicators && availableSources.isNotEmpty()) {
                            SourceCountBadge(
                                count = availableSources.size,
                                maxQuality = availableSources.maxByOrNull { it.quality.resolution.baseScore }?.quality?.resolution,
                                onClick = onSourceSelectionClick
                            )
                        }
                        
                        // Rating
                        if (episode.voteAverage > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = String.format("%.1f", episode.voteAverage),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(start = 2.dp)
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
 * Source availability indicators overlay
 */
@Composable
private fun SourceAvailabilityIndicators(
    sources: List<SourceMetadata>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .size(16.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    } else if (sources.isNotEmpty()) {
        // Show highest quality badge
        sources.maxByOrNull { it.quality.resolution.baseScore }?.let { bestSource ->
            QualityBadge(
                quality = bestSource.quality.resolution,
                modifier = modifier
            )
        }
    }
}

/**
 * Quality badge for showing highest available resolution
 */
@Composable
private fun QualityBadge(
    quality: VideoResolution,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (quality) {
        VideoResolution.RESOLUTION_4K -> "4K" to MaterialTheme.colorScheme.primary
        VideoResolution.RESOLUTION_1080P -> "HD" to MaterialTheme.colorScheme.secondary
        VideoResolution.RESOLUTION_720P -> "720" to MaterialTheme.colorScheme.tertiary
        else -> "SD" to MaterialTheme.colorScheme.outline
    }
    
    Box(
        modifier = modifier
            .background(
                color.copy(alpha = 0.9f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Source count badge that can be clicked for source selection
 */
@Composable
private fun SourceCountBadge(
    count: Int,
    maxQuality: VideoResolution?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else modifier
    
    Box(
        modifier = clickableModifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = "Sources available",
                modifier = Modifier.size(8.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = count.toString(),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Compact episode card for smaller grid layouts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactEpisodeCard(
    episode: TVEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    showProgress: Boolean = true,
    // Source-related parameters
    availableSources: List<SourceMetadata> = emptyList(),
    isLoadingSources: Boolean = false,
    onSourceSelectionClick: (() -> Unit)? = null,
    showSourceIndicators: Boolean = true
) {
    var focused by remember { mutableStateOf(isFocused) }
    
    Card(
        modifier = modifier
            .width(200.dp)
            .height(120.dp)
            .tvCardFocus(focused, onClick)
            .tvFocusable(onFocusChanged = { focused = it.isFocused })
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (focused) 6.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Episode thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.thumbnailUrl ?: episode.stillPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Episode ${episode.episodeNumber} thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = null,
                    error = null
                )
                
                // Source availability indicators
                if (showSourceIndicators) {
                    SourceAvailabilityIndicators(
                        sources = availableSources,
                        isLoading = isLoadingSources,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    )
                }
                
                // Progress indicator
                if (showProgress && episode.hasProgress()) {
                    if (episode.isPartiallyWatched()) {
                        LinearProgressIndicator(
                            progress = episode.watchProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                    
                    // Watch status
                    if (episode.isWatched) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Watched",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            
            // Episode info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = episode.getFormattedTitle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (episode.getDisplayDescription().isNotBlank()) {
                    Text(
                        text = episode.getDisplayDescription(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                // Source indicator for compact view
                if (showSourceIndicators && availableSources.isNotEmpty()) {
                    SourceCountBadge(
                        count = availableSources.size,
                        maxQuality = availableSources.maxByOrNull { it.quality.resolution.baseScore }?.quality?.resolution,
                        onClick = onSourceSelectionClick,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Episode card with list-style layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListEpisodeCard(
    episode: TVEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    showProgress: Boolean = true,
    // Source-related parameters
    availableSources: List<SourceMetadata> = emptyList(),
    isLoadingSources: Boolean = false,
    onSourceSelectionClick: (() -> Unit)? = null,
    showSourceIndicators: Boolean = true
) {
    var focused by remember { mutableStateOf(isFocused) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .tvCardFocus(focused, onClick)
            .tvFocusable(onFocusChanged = { focused = it.isFocused })
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (focused) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.thumbnailUrl ?: episode.stillPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Episode ${episode.episodeNumber} thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = null,
                    error = null
                )
                
                // Progress indicator
                if (showProgress && episode.hasProgress()) {
                    if (episode.isPartiallyWatched()) {
                        LinearProgressIndicator(
                            progress = episode.watchProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                    
                    if (episode.isWatched) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Watched",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
            
            // Episode information
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = episode.getFormattedTitle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (episode.getDisplayDescription().isNotBlank()) {
                    Text(
                        text = episode.getDisplayDescription(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            // Episode metadata
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Source indicator for list view
                if (showSourceIndicators && availableSources.isNotEmpty()) {
                    SourceCountBadge(
                        count = availableSources.size,
                        maxQuality = availableSources.maxByOrNull { it.quality.resolution.baseScore }?.quality?.resolution,
                        onClick = onSourceSelectionClick
                    )
                }
                
                // Episode runtime
                episode.getFormattedRuntime()?.let { runtime ->
                    Text(
                        text = runtime,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = if (showSourceIndicators && availableSources.isNotEmpty()) 2.dp else 0.dp)
                    )
                }
                
                // Rating
                if (episode.voteAverage > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%.1f", episode.voteAverage),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// Preview composables
@Preview(showBackground = true)
@Composable
fun EpisodeCardPreview() {
    RdwatchTheme {
        val sampleEpisode = TVEpisode(
            id = "1",
            seasonNumber = 1,
            episodeNumber = 1,
            title = "The Beginning",
            description = "A young hero starts their journey in this exciting first episode.",
            thumbnailUrl = null,
            airDate = "2023-01-01",
            runtime = 45,
            stillPath = null,
            voteAverage = 8.5f,
            isWatched = false,
            watchProgress = 0.3f
        )
        
        EpisodeCard(
            episode = sampleEpisode,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CompactEpisodeCardPreview() {
    RdwatchTheme {
        val sampleEpisode = TVEpisode(
            id = "1",
            seasonNumber = 1,
            episodeNumber = 1,
            title = "The Beginning",
            description = "A young hero starts their journey.",
            thumbnailUrl = null,
            airDate = "2023-01-01",
            runtime = 45,
            stillPath = null,
            voteAverage = 8.5f,
            isWatched = true,
            watchProgress = 1.0f
        )
        
        CompactEpisodeCard(
            episode = sampleEpisode,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListEpisodeCardPreview() {
    RdwatchTheme {
        val sampleEpisode = TVEpisode(
            id = "1",
            seasonNumber = 1,
            episodeNumber = 1,
            title = "The Beginning",
            description = "A young hero starts their journey in this exciting first episode.",
            thumbnailUrl = null,
            airDate = "2023-01-01",
            runtime = 45,
            stillPath = null,
            voteAverage = 8.5f,
            isWatched = false,
            watchProgress = 0.3f
        )
        
        ListEpisodeCard(
            episode = sampleEpisode,
            onClick = {}
        )
    }
}