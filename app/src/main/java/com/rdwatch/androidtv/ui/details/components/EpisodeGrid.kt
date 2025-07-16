package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.R
import com.rdwatch.androidtv.presentation.components.rememberTVFocusRequester
import com.rdwatch.androidtv.presentation.components.tvFocusRequester
import com.rdwatch.androidtv.ui.details.models.TVEpisode
import com.rdwatch.androidtv.ui.details.models.EpisodePaginationState
import com.rdwatch.androidtv.ui.details.models.EpisodeGridUiState
import com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata
import com.rdwatch.androidtv.ui.theme.RdwatchTheme

/**
 * Episode grid layout modes
 */
enum class EpisodeGridLayout {
    GRID,        // Standard grid layout
    COMPACT,     // Compact grid for smaller screens
    LIST         // List layout for minimal space
}

/**
 * Episode grid component with configurable layout and pagination
 * Optimized for Android TV D-pad navigation
 * Now includes source availability indicators
 */
@Composable
fun EpisodeGrid(
    episodes: List<TVEpisode>,
    onEpisodeClick: (TVEpisode) -> Unit,
    modifier: Modifier = Modifier,
    layout: EpisodeGridLayout = EpisodeGridLayout.GRID,
    showProgress: Boolean = true,
    isLoading: Boolean = false,
    loadingItemsCount: Int = 6,
    paginationState: EpisodePaginationState? = null,
    onLoadMore: (() -> Unit)? = null,
    focusedEpisodeId: String? = null,
    onFocusedEpisodeChanged: ((String?) -> Unit)? = null,
    requestInitialFocus: Boolean = false,
    // Source integration
    episodeSourcesMap: Map<String, List<SourceMetadata>> = emptyMap(),
    onEpisodeSourceSelection: ((TVEpisode) -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    
    // Calculate grid columns based on screen size and layout
    val columns = when (layout) {
        EpisodeGridLayout.GRID -> when {
            screenWidth >= 1920 -> 5
            screenWidth >= 1280 -> 4
            else -> 3
        }
        EpisodeGridLayout.COMPACT -> when {
            screenWidth >= 1920 -> 6
            screenWidth >= 1280 -> 4
            else -> 3
        }
        EpisodeGridLayout.LIST -> 1
    }
    
    Column(
        modifier = modifier
    ) {
        when (layout) {
            EpisodeGridLayout.LIST -> {
                EpisodeListLayout(
                    episodes = episodes,
                    onEpisodeClick = onEpisodeClick,
                    showProgress = showProgress,
                    isLoading = isLoading,
                    loadingItemsCount = loadingItemsCount,
                    paginationState = paginationState,
                    onLoadMore = onLoadMore,
                    focusedEpisodeId = focusedEpisodeId,
                    onFocusedEpisodeChanged = onFocusedEpisodeChanged,
                    requestInitialFocus = requestInitialFocus,
                    episodeSourcesMap = episodeSourcesMap,
                    onEpisodeSourceSelection = onEpisodeSourceSelection
                )
            }
            else -> {
                EpisodeGridLayout(
                    episodes = episodes,
                    onEpisodeClick = onEpisodeClick,
                    columns = columns,
                    layout = layout,
                    showProgress = showProgress,
                    isLoading = isLoading,
                    loadingItemsCount = loadingItemsCount,
                    paginationState = paginationState,
                    onLoadMore = onLoadMore,
                    focusedEpisodeId = focusedEpisodeId,
                    onFocusedEpisodeChanged = onFocusedEpisodeChanged,
                    requestInitialFocus = requestInitialFocus
                )
            }
        }
        
        // Pagination controls
        paginationState?.let { pagination ->
            if (pagination.needsPagination()) {
                EpisodePaginationControls(
                    paginationState = pagination,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

/**
 * Grid layout for episodes
 */
@Composable
private fun EpisodeGridLayout(
    episodes: List<TVEpisode>,
    onEpisodeClick: (TVEpisode) -> Unit,
    columns: Int,
    layout: EpisodeGridLayout,
    showProgress: Boolean,
    isLoading: Boolean,
    loadingItemsCount: Int,
    paginationState: EpisodePaginationState?,
    onLoadMore: (() -> Unit)?,
    focusedEpisodeId: String?,
    onFocusedEpisodeChanged: ((String?) -> Unit)?,
    requestInitialFocus: Boolean
) {
    val gridState = rememberLazyGridState()
    val focusRequester = rememberTVFocusRequester()
    
    // Auto-scroll to focused episode
    LaunchedEffect(focusedEpisodeId) {
        focusedEpisodeId?.let { episodeId ->
            val index = episodes.indexOfFirst { it.id == episodeId }
            if (index != -1) {
                gridState.animateScrollToItem(index)
            }
        }
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusRequester(focusRequester, requestInitialFocus)
    ) {
        // Episode items with explicit keys for proper recomposition
        itemsIndexed(
            items = episodes,
            key = { _, episode -> episode.id }
        ) { index, episode ->
            val isFocused = episode.id == focusedEpisodeId
            
            when (layout) {
                EpisodeGridLayout.GRID -> {
                    val episodeKey = "${episode.seasonNumber}-${episode.episodeNumber}"
                    val availableSources = episodeSourcesMap[episodeKey] ?: emptyList()
                    
                    EpisodeCard(
                        episode = episode,
                        onClick = { onEpisodeClick(episode) },
                        isFocused = isFocused,
                        showProgress = showProgress,
                        availableSources = availableSources,
                        onSourceSelectionClick = { onEpisodeSourceSelection?.invoke(episode) },
                        modifier = Modifier.focusRequester(
                            if (index == 0 && requestInitialFocus) focusRequester else FocusRequester.Default
                        )
                    )
                }
                EpisodeGridLayout.COMPACT -> {
                    val episodeKey = "${episode.seasonNumber}-${episode.episodeNumber}"
                    val availableSources = episodeSourcesMap[episodeKey] ?: emptyList()
                    
                    CompactEpisodeCard(
                        episode = episode,
                        onClick = { onEpisodeClick(episode) },
                        isFocused = isFocused,
                        showProgress = showProgress,
                        availableSources = availableSources,
                        onSourceSelectionClick = { onEpisodeSourceSelection?.invoke(episode) },
                        modifier = Modifier.focusRequester(
                            if (index == 0 && requestInitialFocus) focusRequester else FocusRequester.Default
                        )
                    )
                }
                else -> {
                    val episodeKey = "${episode.seasonNumber}-${episode.episodeNumber}"
                    val availableSources = episodeSourcesMap[episodeKey] ?: emptyList()
                    
                    EpisodeCard(
                        episode = episode,
                        onClick = { onEpisodeClick(episode) },
                        isFocused = isFocused,
                        showProgress = showProgress,
                        availableSources = availableSources,
                        onSourceSelectionClick = { onEpisodeSourceSelection?.invoke(episode) },
                        modifier = Modifier.focusRequester(
                            if (index == 0 && requestInitialFocus) focusRequester else FocusRequester.Default
                        )
                    )
                }
            }
            
            // Focus change callback
            LaunchedEffect(isFocused) {
                if (isFocused) {
                    onFocusedEpisodeChanged?.invoke(episode.id)
                }
            }
            
            // Load more when approaching end
            if (index == episodes.size - 2 && paginationState?.hasNextPage == true) {
                LaunchedEffect(Unit) {
                    onLoadMore?.invoke()
                }
            }
        }
        
        // Loading items with explicit keys
        if (isLoading) {
            items(
                count = loadingItemsCount,
                key = { index -> "loading_$index" }
            ) { index ->
                when (layout) {
                    EpisodeGridLayout.GRID -> EpisodeCardSkeleton()
                    EpisodeGridLayout.COMPACT -> CompactEpisodeCardSkeleton()
                    else -> EpisodeCardSkeleton()
                }
            }
        }
    }
}

/**
 * List layout for episodes
 */
@Composable
private fun EpisodeListLayout(
    episodes: List<TVEpisode>,
    onEpisodeClick: (TVEpisode) -> Unit,
    showProgress: Boolean,
    isLoading: Boolean,
    loadingItemsCount: Int,
    paginationState: EpisodePaginationState?,
    onLoadMore: (() -> Unit)?,
    focusedEpisodeId: String?,
    onFocusedEpisodeChanged: ((String?) -> Unit)?,
    requestInitialFocus: Boolean,
    episodeSourcesMap: Map<String, List<SourceMetadata>>,
    onEpisodeSourceSelection: ((TVEpisode) -> Unit)?
) {
    val scrollState = rememberScrollState()
    val focusRequester = rememberTVFocusRequester()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .tvFocusRequester(focusRequester, requestInitialFocus),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Episode items
        episodes.forEachIndexed { index, episode ->
            val isFocused = episode.id == focusedEpisodeId
            
            val episodeKey = "${episode.seasonNumber}-${episode.episodeNumber}"
            val availableSources = episodeSourcesMap[episodeKey] ?: emptyList()
            
            ListEpisodeCard(
                episode = episode,
                onClick = { onEpisodeClick(episode) },
                isFocused = isFocused,
                showProgress = showProgress,
                availableSources = availableSources,
                onSourceSelectionClick = { onEpisodeSourceSelection?.invoke(episode) },
                modifier = Modifier.focusRequester(
                    if (index == 0 && requestInitialFocus) focusRequester else FocusRequester.Default
                )
            )
            
            // Focus change callback
            LaunchedEffect(isFocused) {
                if (isFocused) {
                    onFocusedEpisodeChanged?.invoke(episode.id)
                }
            }
            
            // Load more when approaching end
            if (index == episodes.size - 2 && paginationState?.hasNextPage == true) {
                LaunchedEffect(Unit) {
                    onLoadMore?.invoke()
                }
            }
        }
        
        // Loading items
        if (isLoading) {
            repeat(loadingItemsCount) {
                ListEpisodeCardSkeleton()
            }
        }
    }
}

/**
 * Pagination controls for episode grid
 */
@Composable
private fun EpisodePaginationControls(
    paginationState: EpisodePaginationState,
    onLoadMore: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Page info
        Text(
            text = paginationState.getPageInfo(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        // Load more button
        if (paginationState.hasNextPage && onLoadMore != null) {
            Button(
                onClick = onLoadMore,
                enabled = !paginationState.isLoadingNextPage,
                modifier = Modifier.height(36.dp)
            ) {
                if (paginationState.isLoadingNextPage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Load More",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Empty state for episode grid
 */
@Composable
fun EpisodeGridEmptyState(
    message: String = "No episodes available",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/**
 * Error state for episode grid
 */
@Composable
fun EpisodeGridErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        if (onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Skeleton loading card for episode grid
 */
@Composable
private fun EpisodeCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .height(158.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .shimmerEffect()
            )
            
            // Content skeleton
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .shimmerEffect()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .padding(top = 4.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

/**
 * Compact skeleton loading card
 */
@Composable
private fun CompactEpisodeCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .shimmerEffect()
            )
            
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .shimmerEffect()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(10.dp)
                        .padding(top = 2.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

/**
 * List skeleton loading card
 */
@Composable
private fun ListEpisodeCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shimmerEffect()
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .shimmerEffect()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .padding(top = 4.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

/**
 * Shimmer effect for loading states
 */
@Composable
private fun Modifier.shimmerEffect(): Modifier = this.background(
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
)

// Preview composables
@Preview(showBackground = true)
@Composable
fun EpisodeGridPreview() {
    RdwatchTheme {
        val sampleEpisodes = listOf(
            TVEpisode(
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
            ),
            TVEpisode(
                id = "2",
                seasonNumber = 1,
                episodeNumber = 2,
                title = "The Adventure Continues",
                description = "Our hero faces their first challenge.",
                thumbnailUrl = null,
                airDate = "2023-01-08",
                runtime = 42,
                stillPath = null,
                voteAverage = 8.2f,
                isWatched = false,
                watchProgress = 0.3f
            )
        )
        
        EpisodeGrid(
            episodes = sampleEpisodes,
            onEpisodeClick = {},
            layout = EpisodeGridLayout.GRID
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EpisodeGridEmptyStatePreview() {
    RdwatchTheme {
        EpisodeGridEmptyState()
    }
}

@Preview(showBackground = true)
@Composable
fun EpisodeGridErrorStatePreview() {
    RdwatchTheme {
        EpisodeGridErrorState(
            message = "Failed to load episodes",
            onRetry = {}
        )
    }
}