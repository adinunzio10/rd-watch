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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.components.CastCrewSection
import com.rdwatch.androidtv.ui.details.components.*
import com.rdwatch.androidtv.ui.details.components.InfoSectionTabMode
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
        viewModel: TVDetailsViewModel = hiltViewModel()
) {
    val tvShowState by viewModel.tvShowState.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val selectedEpisode by viewModel.selectedEpisode.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by playbackViewModel.inProgressContent.collectAsState()
    val creditsState by viewModel.creditsState.collectAsState()
    val sourcesState by viewModel.sourcesState.collectAsState()

    // Focus management
    val backButtonFocusRequester = remember { FocusRequester() }
    val tabFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Initialize with TV show ID
    LaunchedEffect(tvShowId) { viewModel.loadTVShow(tvShowId) }
    
    // Ensure IMDb ID is loaded for source scraping (even from cached data)
    LaunchedEffect(tvShowState) {
        if (tvShowState != null) {
            viewModel.ensureIMDbIdIsLoaded()
        }
    }

    when {
        uiState.isLoading -> {
            TVDetailsLoadingScreen(modifier = modifier)
        }
        uiState.error != null -> {
            TVDetailsErrorScreen(
                    error = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.loadTVShow(tvShowId) },
                    onBackPressed = onBackPressed,
                    modifier = modifier
            )
        }
        tvShowState != null -> {
            val tvShow = tvShowState!!
            
            // Conditional rendering: Episodes tab gets dedicated screen to avoid nested scrolling
            if (selectedTabIndex == 2 && tvShow.contentType == ContentType.TV_SHOW) {
                EpisodesTabScreen(
                    tvShow = tvShow,
                    selectedSeason = selectedSeason,
                    selectedEpisode = selectedEpisode,
                    onSeasonSelected = { season -> viewModel.selectSeason(season) },
                    onEpisodeSelected = { episode ->
                        viewModel.selectEpisode(episode)
                        onEpisodeClick(episode)
                    },
                    onBackToDetails = { viewModel.selectTab(0) }, // Return to Overview tab
                    viewModel = viewModel, // Pass ViewModel for advanced source management
                    onSourceSelected = { source ->
                        // Handle source selection for episode playback
                        // This could trigger playback with the selected source
                    },
                    onPlayWithSource = { episode, source ->
                        // Handle playing episode with specific source
                        playbackViewModel.startEpisodePlaybackWithSource(
                            tvShow = tvShow,
                            episode = episode,
                            source = source
                        )
                    },
                    modifier = modifier
                )
            } else {
                TVDetailsContent(
                        tvShow = tvShow,
                        selectedSeason = selectedSeason,
                        selectedEpisode = selectedEpisode,
                        selectedTabIndex = selectedTabIndex,
                        progress = progress,
                        creditsState = creditsState,
                        sourcesState = sourcesState,
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
                        modifier = modifier
                )
            }
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
                                            (progress
                                                    .find {
                                                        it.contentId == selectedEpisode?.videoUrl
                                                    }
                                                    ?.watchPercentage
                                                    ?: 0f) >= 0.9f
                            ),
                    onActionClick = onActionClick,
                    onBackPressed = onBackPressed,
                    firstFocusRequester = backButtonFocusRequester,
                    modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Action buttons row
        item {
            ActionSection(
                    content = tvShow,
                    onActionClick = onActionClick,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }

        // Source Selection Section - Only show when sources are requested (not Idle)
        if (sourcesState !is UiState.Idle) {
            item {
                var selectedSourceId by remember { mutableStateOf<String?>(null) }
                var showSourceDialog by remember { mutableStateOf(false) }

                when (val currentSourcesState = sourcesState) {
                    is UiState.Idle -> {
                        // This should never happen due to the outer condition, but included for
                        // completeness
                    }
                    is UiState.Loading -> {
                        // Show loading state for sources
                        Surface(
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                            text = "Loading sources for episode...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    selectedEpisode?.let { episode ->
                                        Text(
                                                text =
                                                        "S${episode.seasonNumber}E${episode.episodeNumber} - ${episode.title}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is UiState.Success -> {
                        val sources = currentSourcesState.data
                        if (sources.isNotEmpty()) {
                            Column {
                                // Show current episode info
                                selectedEpisode?.let { episode ->
                                    Text(
                                            text =
                                                    "Sources for S${episode.seasonNumber}E${episode.episodeNumber} - ${episode.title}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier =
                                                    Modifier.padding(
                                                            horizontal = 32.dp,
                                                            vertical = 8.dp
                                                    )
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
                                                        source = source
                                                )
                                            }
                                        },
                                        selectedSourceId = selectedSourceId,
                                        onViewAllClick = { showSourceDialog = true },
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 32.dp,
                                                        vertical = 8.dp
                                                )
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
                                                            source = source
                                                    )
                                                }
                                            },
                                            onDismiss = { showSourceDialog = false },
                                            selectedSourceId = selectedSourceId,
                                            title = "Select Episode Source"
                                    )
                                }
                            }
                        } else {
                            // No sources available
                            Surface(
                                    modifier =
                                            Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    selectedEpisode?.let { episode ->
                                        Text(
                                                text =
                                                        "No sources for S${episode.seasonNumber}E${episode.episodeNumber}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                            text = "Try selecting a different episode or refresh",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color =
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.7f
                                                    )
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error -> {
                        // Show error state with retry option
                        Surface(
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                        text = "Failed to load episode sources",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                        text = currentSourcesState.message ?: "Unknown error",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color =
                                                MaterialTheme.colorScheme.onErrorContainer.copy(
                                                        alpha = 0.8f
                                                )
                                )
                                selectedEpisode?.let { episode ->
                                    Text(
                                            text =
                                                    "S${episode.seasonNumber}E${episode.episodeNumber} - ${episode.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color =
                                                    MaterialTheme.colorScheme.onErrorContainer.copy(
                                                            alpha = 0.7f
                                                    )
                                    )
                                }
                                Button(
                                        onClick = { viewModel.retryLoadingSources() },
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.error
                                                )
                                ) { Text("Retry") }
                            }
                        }
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
                        modifier = Modifier.padding(horizontal = 32.dp)
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
                                    modifier =
                                            Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
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
                                onContentClick = { /* TODO: Handle related content click */},
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                        )
                    }
                }
                2 -> {
                    // Episodes Tab - now handled by dedicated EpisodesTabScreen
                    // This case should not be reached due to conditional rendering above
                    item {
                        Text(
                            text = "Episodes view moved to dedicated screen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                        )
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
                                                                vertical = 16.dp
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                            }
                            is UiState.Success -> {
                                CastCrewSection(
                                        metadata = creditsState.data,
                                        onCastMemberClick = { /* TODO: Handle cast member click */},
                                        onCrewMemberClick = { /* TODO: Handle crew member click */},
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 32.dp,
                                                        vertical = 16.dp
                                                )
                                )
                            }
                            is UiState.Error -> {
                                // Show error message for cast/crew loading failure
                                Surface(
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 32.dp,
                                                        vertical = 16.dp
                                                ),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                            text = "Failed to load cast and crew information",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(16.dp)
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
                text = "Continue Watching",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
        )

        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                // Episode thumbnail placeholder
                Surface(
                        modifier = Modifier.size(120.dp, 68.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            text =
                                    "S${season?.seasonNumber ?: 1}E${episode.episodeNumber} • ${episode.title}",
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
                        colors =
                                ButtonDefaults.buttonColors(
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
private fun TVDetailsLoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                        onClick = onRetry,
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                )
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
