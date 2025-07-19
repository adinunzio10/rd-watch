package com.rdwatch.androidtv.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.presentation.navigation.Screen
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.ui.viewmodel.PlaybackViewModel

/**
 * Profile Screen with user preferences and watch history
 * Designed for Android TV with D-pad navigation support
 */
@OptIn(UnstableApi::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onMovieClick: (Movie) -> Unit = {},
    onNavigateToScreen: ((Any) -> Unit)? = null,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val overscanMargin = 32.dp
    val firstFocusRequester = remember { FocusRequester() }

    // Observe ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    val favoriteMovies by viewModel.favoriteMovies.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()

    // Observe playback data
    val inProgressContent by playbackViewModel.inProgressContent.collectAsState()

    // Get watched movies from progress data
    val watchedMovies =
        remember(inProgressContent, favoriteMovies, watchHistory) {
            // Combine all available movies from favorites and history to find matches
            val allAvailableMovies = (favoriteMovies + watchHistory).distinctBy { it.id }
            inProgressContent.mapNotNull { progress ->
                allAvailableMovies.find { it.videoUrl == progress.contentId }
            }.take(10)
        }

    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(overscanMargin),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Header with back button
        item {
            ProfileHeader(
                onBackPressed = onBackPressed,
                firstFocusRequester = firstFocusRequester,
            )
        }

        // User profile section
        item {
            UserProfileSection(
                userProfile = uiState.userProfile,
                isLoading = uiState.isLoading,
            )
        }

        // Watch statistics cards
        item {
            WatchStatisticsSection(watchStatistics = uiState.watchStatistics)
        }

        // Continue watching section
        if (watchedMovies.isNotEmpty()) {
            item {
                ContinueWatchingSection(
                    movies = watchedMovies,
                    onMovieClick = onMovieClick,
                    onNavigateToScreen = onNavigateToScreen,
                    playbackViewModel = playbackViewModel,
                )
            }
        }

        // Favorites section
        item {
            FavoriteMoviesSection(
                movies = favoriteMovies,
                onMovieClick = onMovieClick,
                onRemoveFromFavorites = { movie -> viewModel.removeFromFavorites(movie) },
            )
        }

        // Profile actions section
        item {
            ProfileActionsSection(
                viewModel = viewModel,
                onNavigateToScreen = onNavigateToScreen,
            )
        }

        // Bottom spacing for TV overscan
        item {
            Spacer(modifier = Modifier.height(overscanMargin))
        }
    }
}

@Composable
private fun ProfileHeader(
    onBackPressed: () -> Unit,
    firstFocusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button
        var backButtonFocused by remember { mutableStateOf(false) }

        TVFocusIndicator(isFocused = backButtonFocused) {
            IconButton(
                onClick = onBackPressed,
                modifier =
                    Modifier
                        .focusRequester(firstFocusRequester)
                        .tvFocusable(
                            onFocusChanged = { backButtonFocused = it.isFocused },
                        ),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint =
                        if (backButtonFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                )
            }
        }

        // Title
        Text(
            text = "Profile",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun UserProfileSection(
    userProfile: UserProfile?,
    isLoading: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Profile avatar
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                )
            }

            // User info
            if (isLoading) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .height(24.dp)
                                .width(120.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp),
                                ),
                    )
                    Box(
                        modifier =
                            Modifier
                                .height(16.dp)
                                .width(180.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp),
                                ),
                    )
                }
            } else if (userProfile != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = userProfile.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = userProfile.email,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "${userProfile.membershipType} Member",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchStatisticsSection(watchStatistics: WatchStatistics?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Watch Statistics",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatisticCard(
                title = "Movies Watched",
                value = watchStatistics?.moviesWatched?.toString() ?: "0",
                icon = Icons.Default.Movie,
                modifier = Modifier.weight(1f),
            )
            StatisticCard(
                title = "Hours Watched",
                value = watchStatistics?.hoursWatched?.toString() ?: "0",
                icon = Icons.Default.AccessTime,
                modifier = Modifier.weight(1f),
            )
            StatisticCard(
                title = "Favorites",
                value = watchStatistics?.favoritesCount?.toString() ?: "0",
                icon = Icons.Default.Favorite,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ContinueWatchingSection(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onNavigateToScreen: ((Any) -> Unit)?,
    playbackViewModel: PlaybackViewModel,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Continue Watching",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )

            // View all button
            TextButton(onClick = {
                // Navigate to Browse screen to see all content
                onNavigateToScreen?.invoke(Screen.Browse)
            }) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(movies) { movie ->
                val progress = playbackViewModel.getContentProgress(movie.videoUrl ?: "")
                ProfileMovieCard(
                    movie = movie,
                    progress = progress,
                    onClick = { onMovieClick(movie) },
                )
            }
        }
    }
}

@Composable
private fun FavoriteMoviesSection(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onRemoveFromFavorites: (Movie) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "My Favorites",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(movies) { movie ->
                ProfileMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) },
                    showFavoriteIcon = true,
                    onRemoveFromFavorites = { onRemoveFromFavorites(movie) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    progress: Float = 0f,
    showFavoriteIcon: Boolean = false,
    onRemoveFromFavorites: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = onClick,
            modifier =
                Modifier
                    .size(
                        width = if (isFocused) 180.dp else 160.dp,
                        height = if (isFocused) 240.dp else 220.dp,
                    )
                    .tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused },
                    ),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = if (isFocused) 8.dp else 2.dp,
                ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Box {
                Column {
                    // Movie poster
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    ) {
                        SmartTVImageLoader(
                            imageUrl = movie.cardImageUrl,
                            contentDescription = movie.title,
                            priority = ImagePriority.NORMAL,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                        )

                        // Progress indicator
                        if (progress > 0f) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .align(Alignment.BottomCenter),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.3f),
                            )
                        }
                    }

                    // Movie title
                    Text(
                        text = movie.title ?: "Unknown Title",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                // Favorite icon
                if (showFavoriteIcon) {
                    IconButton(
                        onClick = { onRemoveFromFavorites?.invoke() },
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Remove from favorites",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileActionsSection(
    viewModel: ProfileViewModel,
    onNavigateToScreen: ((Any) -> Unit)?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Account Actions",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(
                listOf(
                    ProfileAction("Edit Profile", Icons.Default.Edit) {
                        // Navigate to Settings screen for profile editing
                        onNavigateToScreen?.invoke(Screen.Settings)
                    },
                    ProfileAction("Privacy Settings", Icons.Default.Security) {
                        // Navigate to Settings screen for privacy options
                        onNavigateToScreen?.invoke(Screen.Settings)
                    },
                    ProfileAction("Notifications", Icons.Default.Notifications) {
                        // Navigate to Settings screen for notification preferences
                        onNavigateToScreen?.invoke(Screen.Settings)
                    },
                    ProfileAction("Help & Support", Icons.Default.Help) {
                        // Navigate to Settings screen for help options
                        onNavigateToScreen?.invoke(Screen.Settings)
                    },
                    ProfileAction("Sign Out", Icons.Default.ExitToApp) { viewModel.signOut() },
                ),
            ) { action ->
                ProfileActionCard(
                    title = action.title,
                    icon = action.icon,
                    onClick = action.onClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        OutlinedCard(
            onClick = onClick,
            modifier =
                Modifier
                    .width(140.dp)
                    .tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused },
                    ),
            colors =
                CardDefaults.outlinedCardColors(
                    containerColor =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                ),
            border =
                if (isFocused) {
                    CardDefaults.outlinedCardBorder().copy(
                        width = 2.dp,
                    )
                } else {
                    CardDefaults.outlinedCardBorder()
                },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class ProfileAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
