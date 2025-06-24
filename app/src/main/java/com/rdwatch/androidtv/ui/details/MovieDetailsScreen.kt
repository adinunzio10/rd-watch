package com.rdwatch.androidtv.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.MovieList
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.viewmodel.PlaybackViewModel

/**
 * Movie Details Screen with hero layout and metadata
 * Optimized for Android TV 10-foot UI experience
 */
@Composable
fun MovieDetailsScreen(
    movie: Movie,
    modifier: Modifier = Modifier,
    onPlayClick: (Movie) -> Unit = {},
    onBackPressed: () -> Unit = {},
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    viewModel: MovieDetailsViewModel = hiltViewModel()
) {
    val overscanMargin = 32.dp
    val firstFocusRequester = remember { FocusRequester() }
    
    // Observe ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    val relatedMovies by viewModel.relatedMovies.collectAsState()
    
    // Load movie details when screen is first displayed
    LaunchedEffect(movie.id) {
        viewModel.loadMovie(movie)
    }
    
    // Get playback progress
    val contentProgress by playbackViewModel.inProgressContent.collectAsState()
    val currentMovieProgress = contentProgress.find { it.contentId == movie.videoUrl }
    val watchProgress = currentMovieProgress?.progressPercentage ?: 0f
    val isCompleted = playbackViewModel.isContentCompleted(movie.videoUrl ?: "")
    
    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Section
        item {
            MovieHeroSection(
                movie = movie,
                watchProgress = watchProgress,
                isCompleted = isCompleted,
                onPlayClick = onPlayClick,
                onBackPressed = onBackPressed,
                firstFocusRequester = firstFocusRequester,
                overscanMargin = overscanMargin
            )
        }
        
        // Movie Info Section
        item {
            MovieInfoSection(
                movie = movie,
                modifier = Modifier.padding(horizontal = overscanMargin)
            )
        }
        
        // Action Buttons Section
        item {
            ActionButtonsSection(
                movie = movie,
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = overscanMargin)
            )
        }
        
        // Related Movies Section
        if (relatedMovies.isNotEmpty()) {
            item {
                RelatedMoviesSection(
                    movies = relatedMovies,
                    onMovieClick = { /* TODO: Navigate to movie details */ },
                    modifier = Modifier.padding(horizontal = overscanMargin)
                )
            }
        }
        
        // Bottom spacing for TV overscan
        item {
            Spacer(modifier = Modifier.height(overscanMargin))
        }
    }
}

@Composable
private fun MovieHeroSection(
    movie: Movie,
    watchProgress: Float,
    isCompleted: Boolean,
    onPlayClick: (Movie) -> Unit,
    onBackPressed: () -> Unit,
    firstFocusRequester: FocusRequester,
    overscanMargin: Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Background image
        SmartTVImageLoader(
            imageUrl = movie.backgroundImageUrl ?: movie.cardImageUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            priority = ImagePriority.HIGH,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        
        // Back button
        var backButtonFocused by remember { mutableStateOf(false) }
        TVFocusIndicator(isFocused = backButtonFocused) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(overscanMargin)
                    .focusRequester(firstFocusRequester)
                    .tvFocusable(
                        onFocusChanged = { backButtonFocused = it.isFocused }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (backButtonFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White
                    }
                )
            }
        }
        
        // Movie info overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(overscanMargin)
                .fillMaxWidth(0.6f), // Use only part of width for better readability
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Movie title
            Text(
                text = movie.title ?: "Unknown Title",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Movie metadata
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (movie.studio != null) {
                    MetadataChip(text = movie.studio!!)
                }
                MetadataChip(text = "HD") // Placeholder quality
                MetadataChip(text = "2023") // Placeholder year
                if (isCompleted) {
                    MetadataChip(
                        text = "Watched",
                        icon = Icons.Default.CheckCircle
                    )
                }
            }
            
            // Progress indicator
            if (watchProgress > 0f && !isCompleted) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${(watchProgress * 100).toInt()}% watched",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    LinearProgressIndicator(
                        progress = { watchProgress },
                        modifier = Modifier
                            .width(200.dp)
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
            
            // Play button
            PlayButton(
                onClick = { onPlayClick(movie) },
                isResume = watchProgress > 0f && !isCompleted
            )
        }
    }
}

@Composable
private fun MetadataChip(
    text: String,
    icon: ImageVector? = null
) {
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayButton(
    onClick: () -> Unit,
    isResume: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                }
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isResume) "Resume" else "Play",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MovieInfoSection(
    movie: Movie,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description
        if (movie.description != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = movie.description!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                )
            }
        }
        
        // Additional info (placeholder)
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            InfoItem(label = "Duration", value = "2h 15m")
            InfoItem(label = "Language", value = "English")
            InfoItem(label = "Rating", value = "PG-13")
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionButtonsSection(
    movie: Movie,
    uiState: MovieDetailsUiState,
    viewModel: MovieDetailsViewModel,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(
            listOf(
                ActionButton(
                    title = if (uiState.isInWatchlist) "Remove from Watchlist" else "Add to Watchlist",
                    icon = if (uiState.isInWatchlist) Icons.Default.Remove else Icons.Default.Add,
                    onClick = { 
                        if (uiState.isInWatchlist) {
                            viewModel.removeFromWatchlist()
                        } else {
                            viewModel.addToWatchlist()
                        }
                    }
                ),
                ActionButton(
                    title = if (uiState.isLiked) "Unlike" else "Like",
                    icon = if (uiState.isLiked) Icons.Default.Favorite else Icons.Default.ThumbUp,
                    onClick = { viewModel.toggleLike() }
                ),
                ActionButton(
                    title = "Share",
                    icon = Icons.Default.Share,
                    onClick = { viewModel.shareMovie() }
                ),
                ActionButton(
                    title = if (uiState.isDownloaded) "Downloaded" else if (uiState.isDownloading) "Downloading..." else "Download",
                    icon = if (uiState.isDownloaded) Icons.Default.CloudDone else Icons.Default.Download,
                    onClick = { 
                        if (!uiState.isDownloaded && !uiState.isDownloading) {
                            viewModel.downloadMovie()
                        }
                    }
                )
            )
        ) { actionButton ->
            ActionButtonItem(
                title = actionButton.title,
                icon = actionButton.icon,
                onClick = actionButton.onClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButtonItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier
                .width(140.dp)
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = if (isFocused) {
                CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary
                        )
                    ),
                    width = 2.dp
                )
            } else {
                CardDefaults.outlinedCardBorder()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RelatedMoviesSection(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "More Like This",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(movies) { movie ->
                RelatedMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelatedMovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .size(
                    width = if (isFocused) 180.dp else 160.dp,
                    height = if (isFocused) 240.dp else 220.dp
                )
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 8.dp else 2.dp
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                // Movie poster
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                ) {
                    SmartTVImageLoader(
                        imageUrl = movie.cardImageUrl,
                        contentDescription = movie.title,
                        priority = ImagePriority.LOW,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Movie title
                Text(
                    text = movie.title ?: "Unknown Title",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

private data class ActionButton(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)