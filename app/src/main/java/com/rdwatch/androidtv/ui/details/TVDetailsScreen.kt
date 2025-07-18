package com.rdwatch.androidtv.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.components.CastCrewSection
import com.rdwatch.androidtv.ui.details.components.ActionSection
import com.rdwatch.androidtv.ui.details.components.ContentDetailTabs
import com.rdwatch.androidtv.ui.details.components.EpisodeGridSection
import com.rdwatch.androidtv.ui.details.components.HeroSection
import com.rdwatch.androidtv.ui.details.components.InfoSection
import com.rdwatch.androidtv.ui.details.components.InfoSectionTabMode
import com.rdwatch.androidtv.ui.details.components.RelatedSection
import com.rdwatch.androidtv.ui.details.components.SourceListBottomSheet
import com.rdwatch.androidtv.ui.details.models.ContentAction
import com.rdwatch.androidtv.ui.details.models.ContentProgress
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.EpisodeGridUiState
import com.rdwatch.androidtv.ui.details.models.ExtendedContentMetadata
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import com.rdwatch.androidtv.ui.details.models.TVEpisode
import com.rdwatch.androidtv.ui.details.models.TVSeason
import com.rdwatch.androidtv.ui.details.models.TVShowContentDetail
import com.rdwatch.androidtv.ui.viewmodel.PlaybackViewModel

/**
 * TV Details Screen with hero layout, episode grid, and action buttons Optimized for Android TV
 * 10-foot UI experience
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
    viewModel: TVDetailsViewModel = hiltViewModel(),
) {
    val tvShowState by viewModel.tvShowState.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val selectedEpisode by viewModel.selectedEpisode.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by playbackViewModel.inProgressContent.collectAsState()
    val creditsState by viewModel.creditsState.collectAsState()
    val sourcesState by viewModel.sourcesState.collectAsState()
    val episodeSourcesMap by viewModel.episodeSourcesMap.collectAsState()

    // Focus management
    val backButtonFocusRequester = remember { FocusRequester() }
    val tabFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Initialize with TV show ID
    LaunchedEffect(tvShowId) { viewModel.loadTVShow(tvShowId) }

    // External IDs will be fetched on-demand at episode level when needed for source scraping

    when {
        uiState.isLoading -> {
            TVDetailsLoadingScreen(modifier = modifier)
        }
        uiState.error != null -> {
            TVDetailsErrorScreen(
                error = uiState.error ?: "Unknown error",
                onRetry = { viewModel.loadTVShow(tvShowId) },
                onBackPressed = onBackPressed,
                modifier = modifier,
            )
        }
        tvShowState != null -> {
            val tvShow = tvShowState!!

            TVDetailsContent(
                tvShow = tvShow,
                selectedSeason = selectedSeason,
                selectedEpisode = selectedEpisode,
                selectedTabIndex = selectedTabIndex,
                progress = progress,
                creditsState = creditsState,
                sourcesState = sourcesState,
                episodeSourcesMap = episodeSourcesMap,
                viewModel = viewModel,
                playbackViewModel = playbackViewModel,
                onActionClick = { action ->
                    when (action) {
                        // TODO: Determine which episode to show advanced source selection for
                        // Should consider: selected episode, next unwatched episode, or first episode
                        // Remove ContentAction.Play - now handled by episode-specific source selection
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
                onSeasonSelected = { season -> viewModel.selectSeason(season) },
                onEpisodeSelected = { episode ->
                    viewModel.selectEpisode(episode)
                    onEpisodeClick(episode)
                },
                onTabSelected = { tabIndex -> viewModel.selectTab(tabIndex) },
                onBackPressed = onBackPressed,
                backButtonFocusRequester = backButtonFocusRequester,
                tabFocusRequester = tabFocusRequester,
                listState = listState,
                modifier = modifier,
            )
        }
        else -> {
            // Initial state - show loading screen while tvShowState is being loaded
            TVDetailsLoadingScreen(modifier = modifier)
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
    creditsState: UiState<ExtendedContentMetadata>,
    sourcesState: UiState<List<StreamingSource>>,
    episodeSourcesMap: Map<String, List<com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata>>,
    viewModel: TVDetailsViewModel,
    playbackViewModel: PlaybackViewModel,
    onActionClick: (ContentAction) -> Unit,
    onSeasonSelected: (TVSeason) -> Unit,
    onEpisodeSelected: (TVEpisode) -> Unit,
    onTabSelected: (Int) -> Unit,
    onBackPressed: () -> Unit,
    backButtonFocusRequester: FocusRequester,
    tabFocusRequester: FocusRequester,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Hero section with backdrop, title, and primary action
        item {
            HeroSection(
                content = tvShow,
                progress =
                    ContentProgress(
                        watchPercentage =
                            progress
                                .find {
                                    it.contentId == selectedEpisode?.videoUrl
                                }
                                ?.watchPercentage
                                ?: 0f,
                        isCompleted =
                            (
                                progress
                                    .find {
                                        it.contentId == selectedEpisode?.videoUrl
                                    }
                                    ?.watchPercentage
                                    ?: 0f
                            ) >= 0.9f,
                    ),
                onActionClick = onActionClick,
                onBackPressed = onBackPressed,
                firstFocusRequester = backButtonFocusRequester,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        // Action buttons row
        item {
            ActionSection(
                content = tvShow,
                onActionClick = onActionClick,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )
        }

        // Tab navigation
        item {
            ContentDetailTabs(
                selectedTabIndex = selectedTabIndex,
                contentType = tvShow.contentType,
                onTabSelected = onTabSelected,
                firstTabFocusRequester = tabFocusRequester,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }

        // Tab content based on selected tab
        when (selectedTabIndex) {
            0 -> {
                // Overview Tab
                item {
                    InfoSection(
                        content = tvShow,
                        tabMode = InfoSectionTabMode.OVERVIEW,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    )
                }

                // Continue watching for TV shows
                selectedEpisode?.let { episode ->
                    item {
                        TVNextEpisodeSection(
                            episode = episode,
                            season = selectedSeason,
                            onPlayClick = { onActionClick(ContentAction.Play()) },
                            modifier =
                                Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
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
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    )
                }

                // Related content section
                item {
                    RelatedSection(
                        relatedContent = emptyList(),
                        onContentClick = { /* TODO: Handle related content click */ },
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    )
                }
            }
            2 -> {
                // Episodes Tab - integrated into normal tab flow
                if (tvShow.contentType == ContentType.TV_SHOW) {
                    item {
                        // Get authoritative season data to ensure consistency
                        val authoritativeSeasons = remember(tvShow.id) { tvShow.getSeasons() }
                        val authoritativeSelectedSeason =
                            remember(selectedSeason, authoritativeSeasons) {
                                selectedSeason ?: authoritativeSeasons.firstOrNull()
                            }

                        authoritativeSelectedSeason?.let { season ->
                            // Create proper UI state with current season episodes
                            val episodeGridUiState =
                                remember(season.seasonNumber, season.episodes.size, selectedEpisode?.id) {
                                    EpisodeGridUiState(
                                        isLoading = false,
                                        selectedSeasonNumber = season.seasonNumber,
                                        availableSeasons = authoritativeSeasons,
                                        currentSeasonEpisodes = season.episodes,
                                        focusedEpisodeId = selectedEpisode?.id,
                                        error = null,
                                        isRefreshing = false,
                                    )
                                }

                            // Wrap in a Box with padding first, then apply fixed height to inner content
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 16.dp),
                            ) {
                                // Fixed height container to prevent infinite height constraints for LazyVerticalGrid
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(600.dp),
                                ) {
                                    EpisodeGridSection(
                                        tvShowDetail = tvShow.getTVShowDetail(),
                                        selectedSeasonNumber = season.seasonNumber,
                                        onSeasonSelected = { seasonNumber ->
                                            // Find season by number and call callback
                                            authoritativeSeasons.find { s -> s.seasonNumber == seasonNumber }?.let { foundSeason ->
                                                onSeasonSelected(foundSeason)
                                            }
                                        },
                                        onEpisodeClick = { episode ->
                                            // Always trigger advanced source selection on episode click
                                            onEpisodeSelected(episode) // Update selected episode state
                                            viewModel.selectSourcesForEpisode(episode) // Show source selection
                                        },
                                        uiState = episodeGridUiState,
                                        episodeSourcesMap = episodeSourcesMap,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        } ?: run {
                            // No seasons available - show empty state
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = "No episodes available",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
            3 -> {
                // Cast & Crew Tab
                item {
                    when (creditsState) {
                        is UiState.Idle, is UiState.Loading -> {
                            // Show loading indicator for cast/crew
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(
                                            horizontal = 32.dp,
                                            vertical = 16.dp,
                                        ),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                        }
                        is UiState.Success -> {
                            CastCrewSection(
                                metadata = creditsState.data,
                                onCastMemberClick = { /* TODO: Handle cast member click */ },
                                onCrewMemberClick = { /* TODO: Handle crew member click */ },
                                modifier =
                                    Modifier.padding(
                                        horizontal = 32.dp,
                                        vertical = 16.dp,
                                    ),
                            )
                        }
                        is UiState.Error -> {
                            // Show error message for cast/crew loading failure
                            Surface(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 32.dp,
                                        vertical = 16.dp,
                                    ),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = "Failed to load cast and crew information",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Advanced Source Selection Bottom Sheet
    val showSourceSelection by viewModel.showSourceSelection.collectAsState()
    val sourceSelectionState by viewModel.sourceSelectionState.collectAsState()

    SourceListBottomSheet(
        isVisible = showSourceSelection,
        sources = sourceSelectionState.filteredSources,
        selectedSource = sourceSelectionState.selectedSource,
        state = sourceSelectionState,
        onDismiss = { viewModel.hideSourceSelection() },
        onSourceSelected = { source ->
            viewModel.onSourceSelected(source)
            // Trigger playback if episode is selected
            sourceSelectionState.selectedEpisode?.let { episode ->
                playbackViewModel.startEpisodePlaybackWithSource(
                    tvShow = tvShow,
                    episode = episode,
                    source = source,
                )
            }
        },
        onRefresh = {
            sourceSelectionState.selectedEpisode?.let { episode ->
                viewModel.loadAdvancedSourcesForEpisode(tvShow, episode)
            }
        },
        onFilterChanged = { filter ->
            viewModel.updateSourceFilter(filter)
        },
        onSortChanged = { sortOption ->
            viewModel.updateSortOption(sortOption)
        },
        onGroupToggle = { groupId ->
            viewModel.toggleGroup(groupId)
        },
        onViewModeChanged = { viewMode ->
            viewModel.updateViewMode(viewMode)
        },
        onPlaySource = { source ->
            sourceSelectionState.selectedEpisode?.let { episode ->
                playbackViewModel.startEpisodePlaybackWithSource(
                    tvShow = tvShow,
                    episode = episode,
                    source = source,
                )
            }
        },
        onDownloadSource = { source ->
            // TODO: Implement download functionality
            android.util.Log.d("TVDetailsScreen", "Download source requested for ${source.provider.displayName}")
        },
        onAddToPlaylist = { source ->
            // TODO: Implement playlist functionality
            android.util.Log.d("TVDetailsScreen", "Add to playlist requested for ${source.provider.displayName}")
        },
    )
}

@Composable
private fun TVNextEpisodeSection(
    episode: TVEpisode,
    season: TVSeason?,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Continue Watching",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Episode thumbnail placeholder
                Surface(
                    modifier = Modifier.size(120.dp, 68.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                // Episode info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text =
                            "S${season?.seasonNumber ?: 1}E${episode.episodeNumber} • ${episode.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )

                    episode.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                        )
                    }
                }

                // Play button
                Button(
                    onClick = onPlayClick,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
            }
        }
    }
}

@Composable
private fun TVDetailsLoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Loading TV show details...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TVDetailsErrorScreen(
    error: String,
    onRetry: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Error loading TV show",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onRetry,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) { Text("Retry") }
                OutlinedButton(onClick = onBackPressed) { Text("Back") }
            }
        }
    }
}

/** Preview configurations for TVDetailsScreen */
object TVDetailsScreenPreview {
    @Composable
    fun Preview() {
        MaterialTheme { TVDetailsScreen(tvShowId = "demo-show") }
    }
}
