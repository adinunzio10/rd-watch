package com.rdwatch.androidtv.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.details.components.*
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.ui.viewmodel.PlaybackViewModel

/**
 * TV Details Screen with hero layout, episode grid, and action buttons
 * Optimized for Android TV 10-foot UI experience
 */
@Composable
fun TVDetailsScreen(
    tvShowId: String,
    modifier: Modifier = Modifier,
    onPlayClick: (TVEpisode) -> Unit = {},
    onEpisodeClick: (TVEpisode) -> Unit = {},
    onBackPressed: () -> Unit = {},
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    viewModel: TVDetailsViewModel = hiltViewModel()
) {
    val tvShowState by viewModel.tvShowState.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val selectedEpisode by viewModel.selectedEpisode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by playbackViewModel.inProgressContent.collectAsState()
    
    // Focus management
    val backButtonFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    
    // Initialize with TV show ID
    LaunchedEffect(tvShowId) {
        viewModel.loadTVShow(tvShowId)
    }
    
    // Request focus on back button when screen loads
    LaunchedEffect(Unit) {
        backButtonFocusRequester.requestFocus()
    }
    
    when (uiState) {
        is UiState.Loading -> {
            TVDetailsLoadingScreen(modifier = modifier)
        }
        is UiState.Error -> {
            val errorState = uiState as UiState.Error
            TVDetailsErrorScreen(
                error = errorState.message,
                onRetry = { viewModel.loadTVShow(tvShowId) },
                onBackPressed = onBackPressed,
                modifier = modifier
            )
        }
        is UiState.Success<*> -> {
            tvShowState?.let { tvShow ->
                TVDetailsContent(
                    tvShow = tvShow,
                    selectedSeason = selectedSeason,
                    selectedEpisode = selectedEpisode,
                    progress = progress,
                    onActionClick = { action ->
                        when (action) {
                            is ContentAction.Play -> {
                                selectedEpisode?.let { episode ->
                                    onPlayClick(episode)
                                } ?: run {
                                    tvShow.getNextEpisode()?.let { episode ->
                                        onPlayClick(episode)
                                    }
                                }
                            }
                            is ContentAction.AddToWatchlist -> {
                                viewModel.toggleWatchlist(tvShow.id)
                            }
                            is ContentAction.Like -> {
                                viewModel.toggleLike(tvShow.id)
                            }
                            is ContentAction.Share -> {
                                viewModel.shareContent(tvShow)
                            }
                            is ContentAction.Download -> {
                                selectedEpisode?.let { episode ->
                                    viewModel.downloadEpisode(episode)
                                }
                            }
                            else -> {
                                // Handle other actions
                            }
                        }
                    },
                    onSeasonSelected = { season ->
                        viewModel.selectSeason(season)
                    },
                    onEpisodeSelected = { episode ->
                        viewModel.selectEpisode(episode)
                        onEpisodeClick(episode)
                    },
                    onBackPressed = onBackPressed,
                    backButtonFocusRequester = backButtonFocusRequester,
                    listState = listState,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun TVDetailsContent(
    tvShow: TVShowContentDetail,
    selectedSeason: TVSeason?,
    selectedEpisode: TVEpisode?,
    progress: List<com.rdwatch.androidtv.data.entities.WatchProgressEntity>,
    onActionClick: (ContentAction) -> Unit,
    onSeasonSelected: (TVSeason) -> Unit,
    onEpisodeSelected: (TVEpisode) -> Unit,
    onBackPressed: () -> Unit,
    backButtonFocusRequester: FocusRequester,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero section with backdrop, title, and primary action
        item {
            HeroSection(
                content = tvShow,
                progress = ContentProgress(
                    watchPercentage = progress.find { it.contentId == selectedEpisode?.videoUrl }?.watchPercentage ?: 0f,
                    isCompleted = (progress.find { it.contentId == selectedEpisode?.videoUrl }?.watchPercentage ?: 0f) >= 0.9f
                ),
                onActionClick = onActionClick,
                onBackPressed = onBackPressed,
                firstFocusRequester = backButtonFocusRequester,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
        
        // Action buttons row
        item {
            ActionSection(
                content = tvShow,
                onActionClick = onActionClick,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }
        
        // Season selector if multiple seasons
        if (tvShow.hasMultipleSeasons()) {
            item {
                SeasonSelector(
                    seasons = tvShow.getSeasons(),
                    selectedSeasonNumber = selectedSeason?.seasonNumber ?: 1,
                    onSeasonSelected = { seasonNumber ->
                        tvShow.getSeasonByNumber(seasonNumber)?.let { season ->
                            onSeasonSelected(season)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )
            }
        }
        
        // Episode grid for selected season
        selectedSeason?.let { season ->
            item {
                EpisodeGridSection(
                    tvShowDetail = tvShow.getTVShowDetail(),
                    selectedSeasonNumber = season.seasonNumber,
                    onSeasonSelected = { seasonNumber ->
                        tvShow.getSeasonByNumber(seasonNumber)?.let { selectedSeason ->
                            onSeasonSelected(selectedSeason)
                        }
                    },
                    onEpisodeClick = onEpisodeSelected,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )
            }
        }
        
        // TV show info section
        item {
            InfoSection(
                content = tvShow,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }
        
        // Related content section
        item {
            RelatedSection(
                relatedContent = emptyList(),
                onContentClick = { /* TODO: Handle related content click */ },
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun TVDetailsLoadingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading TV show details...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TVDetailsErrorScreen(
    error: String,
    onRetry: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Error loading TV show",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Retry")
                }
                OutlinedButton(
                    onClick = onBackPressed
                ) {
                    Text("Back")
                }
            }
        }
    }
}

/**
 * Preview configurations for TVDetailsScreen
 */
object TVDetailsScreenPreview {
    @Composable
    fun Preview() {
        MaterialTheme {
            TVDetailsScreen(
                tvShowId = "demo-show"
            )
        }
    }
}