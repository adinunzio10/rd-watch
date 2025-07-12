package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rdwatch.androidtv.R
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.presentation.components.tvFocusBorder
import com.rdwatch.androidtv.presentation.components.tvFocusScale
import com.rdwatch.androidtv.ui.details.models.TVSeason
import com.rdwatch.androidtv.ui.theme.RdwatchTheme
import kotlinx.coroutines.launch

/**
 * Season selector component for TV show episode navigation
 * Provides horizontal scrolling selection of seasons with progress indicators
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonSelector(
    seasons: List<TVSeason>,
    selectedSeasonNumber: Int,
    onSeasonSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedSeason: TVSeason? = null,
    showProgress: Boolean = true,
    isLoading: Boolean = false
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Scroll to selected season when it changes
    LaunchedEffect(selectedSeasonNumber) {
        val index = seasons.indexOfFirst { it.seasonNumber == selectedSeasonNumber }
        if (index != -1) {
            coroutineScope.launch {
                listState.animateScrollToItem(index)
            }
        }
    }
    
    Column(
        modifier = modifier
    ) {
        // Season selector header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Seasons",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            
            // Episode count for selected season - using authoritative data
            val displaySeason = selectedSeason ?: seasons.find { it.seasonNumber == selectedSeasonNumber }
            displaySeason?.let { season ->
                android.util.Log.d("SeasonSelector", "=== SeasonSelector UI Debug ===")
                android.util.Log.d("SeasonSelector", "Using ${if (selectedSeason != null) "authoritative selectedSeason" else "found season from authoritative list"}")
                android.util.Log.d("SeasonSelector", "Selected season: ${season.name} (S${season.seasonNumber})")
                android.util.Log.d("SeasonSelector", "  - episodeCount: ${season.episodeCount}")
                android.util.Log.d("SeasonSelector", "  - episodes.size: ${season.episodes.size}")
                android.util.Log.d("SeasonSelector", "  - getFormattedEpisodeCount(): ${season.getFormattedEpisodeCount()}")
                android.util.Log.d("SeasonSelector", "  - Data source: ${if (selectedSeason != null) "Authoritative ViewModel State" else "Fallback from list"}")
                
                Text(
                    text = season.getFormattedEpisodeCount(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
        
        // Season selection row
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(seasons) { season ->
                SeasonSelectorItem(
                    season = season,
                    isSelected = season.seasonNumber == selectedSeasonNumber,
                    onClick = { onSeasonSelected(season.seasonNumber) },
                    showProgress = showProgress,
                    isLoading = isLoading && season.seasonNumber == selectedSeasonNumber
                )
            }
        }
    }
}

/**
 * Individual season selector item
 */
@Composable
private fun SeasonSelectorItem(
    season: TVSeason,
    isSelected: Boolean,
    onClick: () -> Unit,
    showProgress: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .width(120.dp)
            .height(180.dp)
            .tvFocusable(onFocusChanged = { isFocused = it.isFocused })
            .tvFocusBorder(isFocused)
            .tvFocusScale(isFocused)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Season poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(season.posterPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Season ${season.seasonNumber} poster",
                    modifier = Modifier.fillMaxSize(),
                    placeholder = null,
                    error = null
                )
                
                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Selection indicator
                if (isSelected && !isLoading) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                // Progress indicator
                if (showProgress && season.hasProgress()) {
                    LinearProgressIndicator(
                        progress = season.getWatchProgress(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
            
            // Season information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Season title
                Text(
                    text = season.getFormattedTitle(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Episode count
                Text(
                    text = season.getFormattedEpisodeCount(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                // Watch progress text
                if (showProgress && season.hasProgress()) {
                    val watchedCount = season.getWatchedEpisodesCount()
                    Text(
                        text = if (season.isFullyWatched()) {
                            "Completed"
                        } else {
                            "$watchedCount watched"
                        },
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact season selector for smaller screens
 */
@Composable
fun CompactSeasonSelector(
    seasons: List<TVSeason>,
    selectedSeasonNumber: Int,
    onSeasonSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(seasons) { season ->
            CompactSeasonSelectorItem(
                season = season,
                isSelected = season.seasonNumber == selectedSeasonNumber,
                onClick = { onSeasonSelected(season.seasonNumber) },
                showProgress = showProgress
            )
        }
    }
}

/**
 * Compact season selector item
 */
@Composable
private fun CompactSeasonSelectorItem(
    season: TVSeason,
    isSelected: Boolean,
    onClick: () -> Unit,
    showProgress: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .width(80.dp)
            .height(40.dp)
            .tvFocusable(onFocusChanged = { isFocused = it.isFocused })
            .tvFocusBorder(isFocused)
            .tvFocusScale(isFocused)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = season.getFormattedTitle(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Progress indicator
                if (showProgress && season.hasProgress()) {
                    LinearProgressIndicator(
                        progress = season.getWatchProgress(),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(2.dp)
                            .padding(top = 2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

/**
 * Dropdown-style season selector
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSeasonSelector(
    seasons: List<TVSeason>,
    selectedSeasonNumber: Int,
    onSeasonSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    
    val selectedSeason = seasons.find { it.seasonNumber == selectedSeasonNumber }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSeason?.getFormattedTitle() ?: "Select Season",
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .tvFocusable(onFocusChanged = { isFocused = it.isFocused })
                .tvFocusBorder(isFocused)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            seasons.forEach { season ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = season.getFormattedTitle(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = season.getFormattedEpisodeCount(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    onClick = {
                        onSeasonSelected(season.seasonNumber)
                        expanded = false
                    },
                    leadingIcon = if (season.seasonNumber == selectedSeasonNumber) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null
                )
            }
        }
    }
}

// Preview composables
@Preview(showBackground = true)
@Composable
fun SeasonSelectorPreview() {
    RdwatchTheme {
        val sampleSeasons = listOf(
            TVSeason(
                id = "s1",
                seasonNumber = 1,
                name = "Season 1",
                overview = "The first season",
                posterPath = null,
                airDate = "2023-01-01",
                episodeCount = 10,
                episodes = emptyList()
            ),
            TVSeason(
                id = "s2",
                seasonNumber = 2,
                name = "Season 2",
                overview = "The second season",
                posterPath = null,
                airDate = "2023-06-01",
                episodeCount = 12,
                episodes = emptyList()
            )
        )
        
        SeasonSelector(
            seasons = sampleSeasons,
            selectedSeasonNumber = 1,
            onSeasonSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CompactSeasonSelectorPreview() {
    RdwatchTheme {
        val sampleSeasons = listOf(
            TVSeason(
                id = "s1",
                seasonNumber = 1,
                name = "Season 1",
                overview = "The first season",
                posterPath = null,
                airDate = "2023-01-01",
                episodeCount = 10,
                episodes = emptyList()
            ),
            TVSeason(
                id = "s2",
                seasonNumber = 2,
                name = "Season 2",
                overview = "The second season",
                posterPath = null,
                airDate = "2023-06-01",
                episodeCount = 12,
                episodes = emptyList()
            )
        )
        
        CompactSeasonSelector(
            seasons = sampleSeasons,
            selectedSeasonNumber = 1,
            onSeasonSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DropdownSeasonSelectorPreview() {
    RdwatchTheme {
        val sampleSeasons = listOf(
            TVSeason(
                id = "s1",
                seasonNumber = 1,
                name = "Season 1",
                overview = "The first season",
                posterPath = null,
                airDate = "2023-01-01",
                episodeCount = 10,
                episodes = emptyList()
            ),
            TVSeason(
                id = "s2",
                seasonNumber = 2,
                name = "Season 2",
                overview = "The second season",
                posterPath = null,
                airDate = "2023-06-01",
                episodeCount = 12,
                episodes = emptyList()
            )
        )
        
        DropdownSeasonSelector(
            seasons = sampleSeasons,
            selectedSeasonNumber = 1,
            onSeasonSelected = {}
        )
    }
}