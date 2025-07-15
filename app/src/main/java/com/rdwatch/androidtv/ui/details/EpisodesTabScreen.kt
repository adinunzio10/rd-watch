package com.rdwatch.androidtv.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.details.components.*
import com.rdwatch.androidtv.ui.details.models.*

/**
 * Dedicated Episodes tab screen that replaces nested scrolling with single container
 * Eliminates the LazyColumn/LazyVerticalGrid conflict in TVDetailsScreen
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
    firstFocusRequester: FocusRequester? = null
) {
    // Get authoritative season data to ensure consistency
    val authoritativeSeasons = remember(tvShow.id) { tvShow.getSeasons() }
    val authoritativeSelectedSeason = remember(selectedSeason, authoritativeSeasons) {
        selectedSeason ?: authoritativeSeasons.firstOrNull()
    }
    
    // Force recomposition when season or episodes change
    LaunchedEffect(authoritativeSelectedSeason?.seasonNumber, authoritativeSelectedSeason?.episodes?.size) {
        // This ensures the UI updates when season data changes
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        authoritativeSelectedSeason?.let { season ->
            // Create proper UI state with current season episodes - use remember with keys
            val episodeGridUiState = remember(season.seasonNumber, season.episodes.size, selectedEpisode?.id) {
                EpisodeGridUiState(
                    isLoading = false,
                    selectedSeasonNumber = season.seasonNumber,
                    availableSeasons = authoritativeSeasons,
                    currentSeasonEpisodes = season.episodes,
                    focusedEpisodeId = selectedEpisode?.id,
                    error = null,
                    isRefreshing = false
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
                onEpisodeClick = onEpisodeSelected,
                uiState = episodeGridUiState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        } ?: run {
            // No seasons available - show empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No episodes available",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Button(
                    onClick = onBackToDetails,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Back to Details")
                }
            }
        }
    }
}