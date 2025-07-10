package com.rdwatch.androidtv.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.details.components.*
import com.rdwatch.androidtv.ui.details.components.InfoSectionTabMode
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.ui.viewmodel.PlaybackViewModel
import androidx.media3.common.util.UnstableApi

/**
 * TV Details Screen with hero layout, episode grid, and action buttons
 * Optimized for Android TV 10-foot UI experience
 */
@OptIn(UnstableApi::class)
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
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by playbackViewModel.inProgressContent.collectAsState()
    
    // Focus management
    val backButtonFocusRequester = remember { FocusRequester() }
    val tabFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    
    // Initialize with TV show ID
    LaunchedEffect(tvShowId) {
        viewModel.loadTVShow(tvShowId)
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
                    selectedTabIndex = selectedTabIndex,
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
                    onTabSelected = { tabIndex ->
                        viewModel.selectTab(tabIndex)
                    },
                    onBackPressed = onBackPressed,
                    backButtonFocusRequester = backButtonFocusRequester,
                    tabFocusRequester = tabFocusRequester,
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
    selectedTabIndex: Int,
    progress: List<com.rdwatch.androidtv.data.entities.WatchProgressEntity>,
    onActionClick: (ContentAction) -> Unit,
    onSeasonSelected: (TVSeason) -> Unit,
    onEpisodeSelected: (TVEpisode) -> Unit,
    onTabSelected: (Int) -> Unit,
    onBackPressed: () -> Unit,
    backButtonFocusRequester: FocusRequester,
    tabFocusRequester: FocusRequester,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Hero section with backdrop, title, and primary action
        HeroSection(
            content = tvShow,
            progress = ContentProgress(
                watchPercentage = progress.find { it.contentId == selectedEpisode?.videoUrl }?.watchPercentage ?: 0f,
                isCompleted = (progress.find { it.contentId == selectedEpisode?.videoUrl }?.watchPercentage ?: 0f) >= 0.9f
            ),
            onActionClick = onActionClick,
            onBackPressed = onBackPressed,
            firstFocusRequester = backButtonFocusRequester,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Action buttons row
        ActionSection(
            content = tvShow,
            onActionClick = onActionClick,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
        
        // Tab navigation
        ContentDetailTabs(
            selectedTabIndex = selectedTabIndex,
            contentType = tvShow.contentType,
            onTabSelected = onTabSelected,
            firstTabFocusRequester = tabFocusRequester
        )
        
        // Tab content
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // Overview Tab
                    item {
                        InfoSection(
                            content = tvShow,
                            tabMode = InfoSectionTabMode.OVERVIEW,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                        )
                    }
                    
                    // Continue watching for TV shows
                    selectedEpisode?.let { episode ->
                        item {
                            TVNextEpisodeSection(
                                episode = episode,
                                season = selectedSeason,
                                onPlayClick = { onActionClick(ContentAction.Play()) },
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                            )
                        }
                    }
                }
                
                1 -> {
                    // Details Tab
                    item {
                        InfoSection(
                            content = tvShow,
                            tabMode = InfoSectionTabMode.DETAILS,
                            showExpandableDescription = true,
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
                
                2 -> {
                    // Episodes Tab (TV shows only)
                    if (tvShow.contentType == ContentType.TV_SHOW) {
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
                    }
                }
            }
        }
    }
}

@Composable
private fun TVNextEpisodeSection(
    episode: TVEpisode,
    season: TVSeason?,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Continue Watching",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Episode thumbnail placeholder
                Surface(
                    modifier = Modifier.size(120.dp, 68.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Episode info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "S${season?.seasonNumber ?: 1}E${episode.episodeNumber} • ${episode.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    
                    episode.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2
                        )
                    }
                }
                
                // Play button
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
            }
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