package com.rdwatch.androidtv.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.ui.details.components.*
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.ui.details.models.advanced.*

/**
 * Dedicated Episodes tab screen that replaces nested scrolling with single container
 * Eliminates the LazyColumn/LazyVerticalGrid conflict in TVDetailsScreen
 * Now includes advanced source selection integration
 */
@Composable
fun EpisodesTabScreen(
    tvShow: TVShowContentDetail,
    selectedSeason: TVSeason?,
    selectedEpisode: TVEpisode?,
    onSeasonSelected: (TVSeason) -> Unit,
    onEpisodeSelected: (TVEpisode) -> Unit,
    onBackToDetails: () -> Unit,
    modifier: Modifier = Modifier,
    firstFocusRequester: FocusRequester? = null,
    // Advanced source selection parameters
    viewModel: TVDetailsViewModel = hiltViewModel(),
    onSourceSelected: ((SourceMetadata) -> Unit)? = null,
    onPlayWithSource: ((TVEpisode, SourceMetadata) -> Unit)? = null,
) {
    // Advanced source selection state
    val episodeSourcesMap by viewModel.episodeSourcesMap.collectAsState()
    val showSourceSelection by viewModel.showSourceSelection.collectAsState()
    val sourceSelectionState by viewModel.sourceSelectionState.collectAsState()
    // Get authoritative season data to ensure consistency
    val authoritativeSeasons = remember(tvShow.id) { tvShow.getSeasons() }
    val authoritativeSelectedSeason =
        remember(selectedSeason, authoritativeSeasons) {
            selectedSeason ?: authoritativeSeasons.firstOrNull()
        }

    // Force recomposition when season or episodes change
    LaunchedEffect(authoritativeSelectedSeason?.seasonNumber, authoritativeSelectedSeason?.episodes?.size) {
        // This ensures the UI updates when season data changes
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        authoritativeSelectedSeason?.let { season ->
            // Create proper UI state with current season episodes - use remember with keys
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

            // Use single scrolling container - no nested LazyColumn
            EpisodeGridSection(
                tvShowDetail = tvShow.getTVShowDetail(),
                selectedSeasonNumber = season.seasonNumber,
                onSeasonSelected = { seasonNumber ->
                    // Find season by number and call callback
                    authoritativeSeasons.find { season -> season.seasonNumber == seasonNumber }?.let { foundSeason ->
                        onSeasonSelected(foundSeason)
                    }
                },
                onEpisodeClick = { episode ->
                    // Always trigger advanced source selection on episode click
                    // The selectSourcesForEpisode method will handle loading sources if needed
                    onEpisodeSelected(episode) // Update selected episode state
                    viewModel.selectSourcesForEpisode(episode) // Always show source selection
                },
                uiState = episodeGridUiState,
                episodeSourcesMap = episodeSourcesMap, // Pass source data to grid
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
            )

            // Advanced Source Selection Bottom Sheet
            SourceListBottomSheet(
                isVisible = showSourceSelection,
                sources = sourceSelectionState.filteredSources,
                selectedSource = sourceSelectionState.selectedSource,
                state = sourceSelectionState,
                onDismiss = { viewModel.hideSourceSelection() },
                onSourceSelected = { source ->
                    viewModel.onSourceSelected(source)
                    onSourceSelected?.invoke(source)

                    // Trigger playback if callback provided
                    sourceSelectionState.selectedEpisode?.let { episode ->
                        onPlayWithSource?.invoke(episode, source)
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
                        onPlayWithSource?.invoke(episode, source)
                    }
                },
                onDownloadSource = { source ->
                    // TODO: Implement download functionality
                    android.util.Log.d("EpisodesTabScreen", "Download source requested for ${source.provider.displayName}")
                },
                onAddToPlaylist = { source ->
                    // TODO: Implement playlist functionality
                    android.util.Log.d("EpisodesTabScreen", "Add to playlist requested for ${source.provider.displayName}")
                },
            )
        } ?: run {
            // No seasons available - show empty state
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No episodes available",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Button(
                    onClick = onBackToDetails,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Back to Details")
                }
            }
        }
    }
}
