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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.components.CastCrewSection
import com.rdwatch.androidtv.ui.details.components.*
import com.rdwatch.androidtv.ui.details.components.InfoSectionTabMode
import com.rdwatch.androidtv.ui.details.components.SourceListBottomSheet
import com.rdwatch.androidtv.ui.details.components.SourceSelectionDialog
import com.rdwatch.androidtv.ui.details.components.SourceSelectionSection
import com.rdwatch.androidtv.ui.details.models.*
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

    // Convert advanced sources to legacy format for UI compatibility
    val advancedSources =
        selectedEpisode?.let { episode ->
            val episodeKey = "${episode.seasonNumber}-${episode.episodeNumber}"
            episodeSourcesMap[episodeKey]?.map { sourceMetadata ->
                convertSourceMetadataToStreamingSource(sourceMetadata)
            } ?: emptyList()
        } ?: emptyList()

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
                advancedSources = advancedSources,
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
    advancedSources: List<StreamingSource>,
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

        // Source Selection Section - Only show when sources are available from advanced sources
        if (advancedSources.isNotEmpty()) {
            item {
                var selectedSourceId by remember { mutableStateOf<String?>(null) }
                var showSourceDialog by remember { mutableStateOf(false) }

                // Use advanced sources converted to legacy format
                val sources = advancedSources

                Column {
                    // Show current episode info
                    selectedEpisode?.let { episode ->
                        Text(
                            text = "Sources for S${episode.seasonNumber}E${episode.episodeNumber} - ${episode.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                        )
                    }

                    SourceSelectionSection(
                        sources = sources,
                        onSourceSelected = { source ->
                            selectedSourceId = source.id
                            // Play the selected episode with the chosen source
                            selectedEpisode?.let { episode ->
                                playbackViewModel.startEpisodePlayback(
                                    tvShow = tvShow,
                                    episode = episode,
                                    source = source,
                                )
                            }
                        },
                        selectedSourceId = selectedSourceId,
                        onViewAllClick = { showSourceDialog = true },
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                    )

                    if (showSourceDialog) {
                        SourceSelectionDialog(
                            sources = sources,
                            onSourceSelected = { source ->
                                selectedSourceId = source.id
                                showSourceDialog = false
                                // Play the selected episode with the chosen source
                                selectedEpisode?.let { episode ->
                                    playbackViewModel.startEpisodePlayback(
                                        tvShow = tvShow,
                                        episode = episode,
                                        source = source,
                                    )
                                }
                            },
                            onDismiss = { showSourceDialog = false },
                            selectedSourceId = selectedSourceId,
                            title = "Select Episode Source",
                        )
                    }
                }
            }
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
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                            )
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
        onPlaySource = { source ->
            sourceSelectionState.selectedEpisode?.let { episode ->
                playbackViewModel.startEpisodePlaybackWithSource(
                    tvShow = tvShow,
                    episode = episode,
                    source = source,
                )
            }
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

/**
 * Convert SourceMetadata (advanced sources) to StreamingSource (legacy format) for UI compatibility
 */
private fun convertSourceMetadataToStreamingSource(
    sourceMetadata: com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata,
): StreamingSource {
    return StreamingSource(
        id = sourceMetadata.id,
        provider =
            com.rdwatch.androidtv.ui.details.models.SourceProvider(
                id = sourceMetadata.provider.id,
                name = sourceMetadata.provider.name,
                displayName = sourceMetadata.provider.displayName,
                logoUrl = sourceMetadata.provider.logoUrl,
            ),
        url = sourceMetadata.metadata["originalUrl"] ?: "",
        quality = mapVideoResolutionToSourceQuality(sourceMetadata.quality.resolution),
        sourceType =
            com.rdwatch.androidtv.ui.details.models.SourceType(
                com.rdwatch.androidtv.ui.details.models.SourceType.ScraperSourceType.DIRECT_LINK,
            ),
        features =
            com.rdwatch.androidtv.ui.details.models.SourceFeatures(
                supportsP2P = sourceMetadata.provider.type == com.rdwatch.androidtv.ui.details.models.advanced.SourceProviderInfo.ProviderType.TORRENT,
                supportsDolbyVision = sourceMetadata.quality.dolbyVision,
                supportsDolbyAtmos = sourceMetadata.audio.dolbyAtmos,
                seeders = sourceMetadata.health.seeders,
                leechers = sourceMetadata.health.leechers,
            ),
        isAvailable = sourceMetadata.availability.isAvailable,
        metadata = sourceMetadata.metadata,
    )
}

/**
 * Map VideoResolution to SourceQuality
 */
private fun mapVideoResolutionToSourceQuality(resolution: com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution): SourceQuality {
    return when (resolution) {
        com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution.RESOLUTION_8K -> SourceQuality.QUALITY_8K
        com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution.RESOLUTION_4K -> SourceQuality.QUALITY_4K
        com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution.RESOLUTION_1440P -> SourceQuality.QUALITY_1080P // Map 1440p to 1080p
        com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution.RESOLUTION_1080P -> SourceQuality.QUALITY_1080P
        com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution.RESOLUTION_720P -> SourceQuality.QUALITY_720P
        com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution.RESOLUTION_480P -> SourceQuality.QUALITY_480P
        com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution.RESOLUTION_360P -> SourceQuality.QUALITY_360P
        com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution.RESOLUTION_240P -> SourceQuality.QUALITY_240P
        com.rdwatch.androidtv.ui.details.models.advanced.VideoResolution.UNKNOWN -> SourceQuality.QUALITY_AUTO
    }
}

/** Preview configurations for TVDetailsScreen */
object TVDetailsScreenPreview {
    @Composable
    fun Preview() {
        MaterialTheme { TVDetailsScreen(tvShowId = "demo-show") }
    }
}
