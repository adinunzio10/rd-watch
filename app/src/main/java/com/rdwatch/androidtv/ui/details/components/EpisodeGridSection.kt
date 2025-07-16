package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.R
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata
import com.rdwatch.androidtv.ui.theme.RdwatchTheme

/**
 * Main episode grid section component that combines season selector and episode grid
 * Provides complete TV show episode browsing experience with season navigation
 * Now includes source availability indicators
 */
@Composable
fun EpisodeGridSection(
    tvShowDetail: TVShowDetail,
    selectedSeasonNumber: Int,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeClick: (TVEpisode) -> Unit,
    modifier: Modifier = Modifier,
    uiState: EpisodeGridUiState = EpisodeGridUiState(),
    showProgress: Boolean = true,
    gridLayout: EpisodeGridLayout = EpisodeGridLayout.GRID,
    paginationState: EpisodePaginationState? = null,
    onLoadMore: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    sectionTitle: String = "Episodes",
    // Source integration
    episodeSourcesMap: Map<String, List<SourceMetadata>> = emptyMap(),
    onEpisodeSourceSelection: ((TVEpisode) -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val isCompactMode = configuration.screenWidthDp < 1280
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Section title
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Only show season selector if there are multiple seasons
        if (tvShowDetail.hasMultipleSeasons()) {
            if (isCompactMode) {
                CompactSeasonSelector(
                    seasons = tvShowDetail.seasons,
                    selectedSeasonNumber = selectedSeasonNumber,
                    onSeasonSelected = onSeasonSelected,
                    showProgress = showProgress,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                SeasonSelector(
                    seasons = tvShowDetail.seasons,
                    selectedSeasonNumber = selectedSeasonNumber,
                    onSeasonSelected = onSeasonSelected,
                    selectedSeason = tvShowDetail.seasons.find { it.seasonNumber == selectedSeasonNumber },
                    showProgress = showProgress,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.padding(bottom = 18.dp)
                )
            }
        }
        
        // Episode grid content
        EpisodeGridContent(
            episodes = uiState.currentSeasonEpisodes,
            onEpisodeClick = onEpisodeClick,
            uiState = uiState,
            showProgress = showProgress,
            gridLayout = gridLayout,
            paginationState = paginationState,
            onLoadMore = onLoadMore,
            onRetry = onRetry,
            episodeSourcesMap = episodeSourcesMap,
            onEpisodeSourceSelection = onEpisodeSourceSelection
        )
    }
}

/**
 * Episode grid content with loading, empty, and error states
 */
@Composable
private fun EpisodeGridContent(
    episodes: List<TVEpisode>,
    onEpisodeClick: (TVEpisode) -> Unit,
    uiState: EpisodeGridUiState,
    showProgress: Boolean,
    gridLayout: EpisodeGridLayout,
    paginationState: EpisodePaginationState?,
    onLoadMore: (() -> Unit)?,
    onRetry: (() -> Unit)?,
    episodeSourcesMap: Map<String, List<SourceMetadata>>,
    onEpisodeSourceSelection: ((TVEpisode) -> Unit)?
) {
    when {
        // Error state
        uiState.isInError() -> {
            EpisodeGridErrorState(
                message = uiState.error ?: "Failed to load episodes",
                onRetry = onRetry,
                modifier = Modifier.height(400.dp)
            )
        }
        
        // Loading state (when no episodes are available)
        uiState.shouldShowLoading() -> {
            EpisodeGridLoadingState(
                layout = gridLayout,
                loadingMessage = uiState.getLoadingMessage(),
                modifier = Modifier.height(400.dp)
            )
        }
        
        // Empty state
        episodes.isEmpty() && !uiState.isLoading -> {
            val emptyMessage = when {
                uiState.selectedSeasonNumber > 0 -> "No episodes found for Season ${uiState.selectedSeasonNumber}"
                else -> "No episodes available for this season"
            }
            EpisodeGridEmptyState(
                message = emptyMessage,
                modifier = Modifier.height(400.dp)
            )
        }
        
        // Content state - show episodes with optional refresh indicator
        else -> {
            Column {
                // Show refresh indicator when loading additional episodes
                if (uiState.isRefreshingEpisodes()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Refreshing episodes...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                EpisodeGrid(
                    episodes = episodes,
                    onEpisodeClick = onEpisodeClick,
                    layout = gridLayout,
                    showProgress = showProgress,
                    isLoading = uiState.isLoadingCurrentSeason(),
                    paginationState = paginationState,
                    onLoadMore = onLoadMore,
                    focusedEpisodeId = uiState.focusedEpisodeId,
                    onFocusedEpisodeChanged = { /* Handle focus change if needed */ },
                    requestInitialFocus = false,
                    episodeSourcesMap = episodeSourcesMap,
                    onEpisodeSourceSelection = onEpisodeSourceSelection
                )
            }
        }
    }
}

/**
 * Loading state for episode grid
 */
@Composable
private fun EpisodeGridLoadingState(
    layout: EpisodeGridLayout,
    modifier: Modifier = Modifier,
    loadingMessage: String = "Loading episodes..."
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = loadingMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/**
 * Compact episode grid section for smaller screens
 */
@Composable
fun CompactEpisodeGridSection(
    tvShowDetail: TVShowDetail,
    selectedSeasonNumber: Int,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeClick: (TVEpisode) -> Unit,
    modifier: Modifier = Modifier,
    uiState: EpisodeGridUiState = EpisodeGridUiState(),
    showProgress: Boolean = true,
    onRetry: (() -> Unit)? = null,
    episodeSourcesMap: Map<String, List<SourceMetadata>> = emptyMap(),
    onEpisodeSourceSelection: ((TVEpisode) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Compact header with season selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Episodes",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            
            if (tvShowDetail.hasMultipleSeasons()) {
                DropdownSeasonSelector(
                    seasons = tvShowDetail.seasons,
                    selectedSeasonNumber = selectedSeasonNumber,
                    onSeasonSelected = onSeasonSelected,
                    modifier = Modifier.width(200.dp)
                )
            }
        }
        
        // Episode grid content
        EpisodeGridContent(
            episodes = uiState.currentSeasonEpisodes,
            onEpisodeClick = onEpisodeClick,
            uiState = uiState,
            showProgress = showProgress,
            gridLayout = EpisodeGridLayout.COMPACT,
            paginationState = null,
            onLoadMore = null,
            onRetry = onRetry,
            episodeSourcesMap = episodeSourcesMap,
            onEpisodeSourceSelection = onEpisodeSourceSelection
        )
    }
}

/**
 * Episode grid section with statistics
 */
@Composable
fun EpisodeGridSectionWithStats(
    tvShowDetail: TVShowDetail,
    selectedSeasonNumber: Int,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeClick: (TVEpisode) -> Unit,
    modifier: Modifier = Modifier,
    uiState: EpisodeGridUiState = EpisodeGridUiState(),
    showProgress: Boolean = true,
    gridLayout: EpisodeGridLayout = EpisodeGridLayout.GRID,
    onRetry: (() -> Unit)? = null,
    episodeSourcesMap: Map<String, List<SourceMetadata>> = emptyMap(),
    onEpisodeSourceSelection: ((TVEpisode) -> Unit)? = null
) {
    val selectedSeason = tvShowDetail.seasons.find { it.seasonNumber == selectedSeasonNumber }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Header with statistics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Episodes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                
                selectedSeason?.let { season ->
                    Text(
                        text = if (showProgress && season.hasProgress()) {
                            "${season.getWatchedEpisodesCount()} of ${season.episodeCount} watched"
                        } else {
                            season.getFormattedEpisodeCount()
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Season progress indicator
            if (showProgress && selectedSeason?.hasProgress() == true) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${(selectedSeason.getWatchProgress() * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    LinearProgressIndicator(
                        progress = selectedSeason.getWatchProgress(),
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .padding(top = 2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Season selector
        if (tvShowDetail.hasMultipleSeasons()) {
            SeasonSelector(
                seasons = tvShowDetail.seasons,
                selectedSeasonNumber = selectedSeasonNumber,
                onSeasonSelected = onSeasonSelected,
                selectedSeason = tvShowDetail.seasons.find { it.seasonNumber == selectedSeasonNumber },
                showProgress = showProgress,
                isLoading = uiState.isLoading,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        // Episode grid content
        EpisodeGridContent(
            episodes = uiState.currentSeasonEpisodes,
            onEpisodeClick = onEpisodeClick,
            uiState = uiState,
            showProgress = showProgress,
            gridLayout = gridLayout,
            paginationState = null,
            onLoadMore = null,
            onRetry = onRetry,
            episodeSourcesMap = episodeSourcesMap,
            onEpisodeSourceSelection = onEpisodeSourceSelection
        )
    }
}

/**
 * Extension function to check if TV show has multiple seasons
 */
private fun TVShowDetail.hasMultipleSeasons(): Boolean = numberOfSeasons > 1

// Preview composables
@Preview(showBackground = true)
@Composable
fun EpisodeGridSectionPreview() {
    RdwatchTheme {
        val sampleEpisodes = listOf(
            TVEpisode(
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
                isWatched = true,
                watchProgress = 1.0f
            ),
            TVEpisode(
                id = "2",
                seasonNumber = 1,
                episodeNumber = 2,
                title = "The Adventure Continues",
                description = "Our hero faces their first challenge in this action-packed episode.",
                thumbnailUrl = null,
                airDate = "2023-01-08",
                runtime = 42,
                stillPath = null,
                voteAverage = 8.2f,
                isWatched = false,
                watchProgress = 0.3f
            ),
            TVEpisode(
                id = "3",
                seasonNumber = 1,
                episodeNumber = 3,
                title = "New Discoveries",
                description = "Unexpected revelations change everything.",
                thumbnailUrl = null,
                airDate = "2023-01-15",
                runtime = 48,
                stillPath = null,
                voteAverage = 8.8f,
                isWatched = false,
                watchProgress = 0.0f
            )
        )
        
        val sampleSeason = TVSeason(
            id = "s1",
            seasonNumber = 1,
            name = "Season 1",
            overview = "The first season of this amazing show",
            posterPath = null,
            airDate = "2023-01-01",
            episodeCount = 10,
            episodes = sampleEpisodes
        )
        
        val sampleTVShow = TVShowDetail(
            id = "demo-show",
            title = "Demo TV Show",
            originalTitle = "Demo TV Show",
            overview = "This is a demo TV show for preview purposes",
            posterPath = null,
            backdropPath = null,
            firstAirDate = "2023-01-01",
            lastAirDate = null,
            status = "Returning Series",
            type = "Scripted",
            genres = listOf("Drama", "Mystery"),
            numberOfSeasons = 2,
            numberOfEpisodes = 20,
            seasons = listOf(sampleSeason),
            networks = listOf("Demo Network"),
            voteAverage = 8.5f,
            voteCount = 1234
        )
        
        val uiState = EpisodeGridUiState(
            selectedSeasonNumber = 1,
            availableSeasons = listOf(sampleSeason),
            currentSeasonEpisodes = sampleEpisodes,
            isLoading = false
        )
        
        EpisodeGridSection(
            tvShowDetail = sampleTVShow,
            selectedSeasonNumber = 1,
            onSeasonSelected = {},
            onEpisodeClick = {},
            uiState = uiState
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CompactEpisodeGridSectionPreview() {
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
        
        val sampleSeason = TVSeason(
            id = "s1",
            seasonNumber = 1,
            name = "Season 1",
            overview = "The first season",
            posterPath = null,
            airDate = "2023-01-01",
            episodeCount = 10,
            episodes = sampleEpisodes
        )
        
        val sampleTVShow = TVShowDetail(
            id = "demo-show",
            title = "Demo TV Show",
            originalTitle = "Demo TV Show",
            overview = "This is a demo TV show",
            posterPath = null,
            backdropPath = null,
            firstAirDate = "2023-01-01",
            lastAirDate = null,
            status = "Returning Series",
            type = "Scripted",
            genres = listOf("Drama"),
            numberOfSeasons = 1,
            numberOfEpisodes = 10,
            seasons = listOf(sampleSeason),
            networks = listOf("Demo Network"),
            voteAverage = 8.5f,
            voteCount = 1234
        )
        
        val uiState = EpisodeGridUiState(
            selectedSeasonNumber = 1,
            availableSeasons = listOf(sampleSeason),
            currentSeasonEpisodes = sampleEpisodes,
            isLoading = false
        )
        
        CompactEpisodeGridSection(
            tvShowDetail = sampleTVShow,
            selectedSeasonNumber = 1,
            onSeasonSelected = {},
            onEpisodeClick = {},
            uiState = uiState
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EpisodeGridSectionWithStatsPreview() {
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
        
        val sampleSeason = TVSeason(
            id = "s1",
            seasonNumber = 1,
            name = "Season 1",
            overview = "The first season",
            posterPath = null,
            airDate = "2023-01-01",
            episodeCount = 10,
            episodes = sampleEpisodes
        )
        
        val sampleTVShow = TVShowDetail(
            id = "demo-show",
            title = "Demo TV Show",
            originalTitle = "Demo TV Show",
            overview = "This is a demo TV show",
            posterPath = null,
            backdropPath = null,
            firstAirDate = "2023-01-01",
            lastAirDate = null,
            status = "Returning Series",
            type = "Scripted",
            genres = listOf("Drama"),
            numberOfSeasons = 2,
            numberOfEpisodes = 20,
            seasons = listOf(sampleSeason),
            networks = listOf("Demo Network"),
            voteAverage = 8.5f,
            voteCount = 1234
        )
        
        val uiState = EpisodeGridUiState(
            selectedSeasonNumber = 1,
            availableSeasons = listOf(sampleSeason),
            currentSeasonEpisodes = sampleEpisodes,
            isLoading = false
        )
        
        EpisodeGridSectionWithStats(
            tvShowDetail = sampleTVShow,
            selectedSeasonNumber = 1,
            onSeasonSelected = {},
            onEpisodeClick = {},
            uiState = uiState
        )
    }
}