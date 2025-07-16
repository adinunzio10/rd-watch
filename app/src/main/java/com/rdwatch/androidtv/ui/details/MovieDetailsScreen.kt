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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.viewmodel.PlaybackViewModel
import com.rdwatch.androidtv.ui.details.components.ActionSection
import com.rdwatch.androidtv.ui.details.components.ContentDetailTabs
import com.rdwatch.androidtv.ui.components.CastCrewSection
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.ui.details.MovieDetailsUiState
import com.rdwatch.androidtv.ui.details.components.SourceSelectionSection
import com.rdwatch.androidtv.ui.details.components.SourceSelectionDialog
import com.rdwatch.androidtv.ui.details.components.SourceListBottomSheet
import com.rdwatch.androidtv.ui.details.models.advanced.*
import androidx.media3.common.util.UnstableApi

/**
 * Movie Details Screen with hero layout and metadata
 * Optimized for Android TV 10-foot UI experience
 */
@OptIn(UnstableApi::class)
@Composable
fun MovieDetailsScreen(
    movieId: String,
    modifier: Modifier = Modifier,
    onPlayClick: (Movie) -> Unit = {},
    onMovieClick: (Movie) -> Unit = {},
    onBackPressed: () -> Unit = {},
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    viewModel: MovieDetailsViewModel = hiltViewModel()
) {
    val overscanMargin = 32.dp
    val firstFocusRequester = remember { FocusRequester() }
    
    // Observe ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    val movieState by viewModel.movieState.collectAsState()
    val relatedMoviesState by viewModel.relatedMoviesState.collectAsState()
    val creditsState by viewModel.creditsState.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val sourcesState by viewModel.sourcesState.collectAsState()
    
    // Advanced source selection state
    val advancedSources by viewModel.advancedSources.collectAsState()
    val showSourceSelection by viewModel.showSourceSelection.collectAsState()
    val sourceSelectionState by viewModel.sourceSelectionState.collectAsState()
    
    // Debug logging for UI state
    println("DEBUG [MovieDetailsScreen]: UI state availableSources count: ${uiState.availableSources.size}")
    println("DEBUG [MovieDetailsScreen]: Sources state: ${sourcesState.javaClass.simpleName}")
    
    // Load movie details when screen is first displayed
    LaunchedEffect(movieId) {
        viewModel.loadMovieDetails(movieId)
    }
    
    // Get the movie from the ViewModel state
    val movie = uiState.movie
    
    
    // Get playback progress
    val contentProgress by playbackViewModel.inProgressContent.collectAsState()
    val currentMovieProgress = movie?.let { contentProgress.find { it.contentId == movie.videoUrl } }
    val watchProgress = currentMovieProgress?.watchPercentage ?: 0f
    val isCompleted = movie?.let { playbackViewModel.isContentCompleted(it.videoUrl ?: "") } ?: false
    
    
    when (movieState) {
        is UiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Loading movie details...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        is UiState.Error -> {
            LaunchedEffect(Unit) {
                firstFocusRequester.requestFocus()
            }
            
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Failed to load movie",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    val currentMovieState = movieState
                    Text(
                        text = if (currentMovieState is UiState.Error) currentMovieState.message ?: "Unknown error" else "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        onClick = onBackPressed,
                        modifier = Modifier.focusRequester(firstFocusRequester)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
        is UiState.Success -> {
            if (movie == null) {
                LaunchedEffect(Unit) {
                    firstFocusRequester.requestFocus()
                }
                
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Movie not found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = onBackPressed,
                            modifier = Modifier.focusRequester(firstFocusRequester)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            } else {
                LaunchedEffect(Unit) {
                    firstFocusRequester.requestFocus()
                }
                
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Hero Section
                    item {
                        MovieHeroSection(
                            movie = movie,
                            uiState = uiState,
                            watchProgress = watchProgress,
                            isCompleted = isCompleted,
                            onPlayClick = onPlayClick,
                            onBackPressed = onBackPressed,
                            firstFocusRequester = firstFocusRequester,
                            overscanMargin = overscanMargin
                        )
                    }
                    
                    // Action Buttons Section
                    item {
                        val movieContentDetail = MovieContentDetail(
                            movie = movie,
                            progress = ContentProgress(
                                watchPercentage = watchProgress,
                                isCompleted = isCompleted
                            ),
                            isInWatchlist = uiState.isInWatchlist,
                            isLiked = uiState.isLiked,
                            isDownloaded = uiState.isDownloaded,
                            isDownloading = uiState.isDownloading,
                            isFromRealDebrid = uiState.isFromRealDebrid
                        )
                        
                        ActionSection(
                            content = movieContentDetail,
                            onActionClick = { action ->
                                when (action) {
                                    is ContentAction.Play -> {
                                        onPlayClick(movie)
                                    }
                                    is ContentAction.AddToWatchlist -> {
                                        if (action.isInWatchlist) {
                                            viewModel.removeFromWatchlist()
                                        } else {
                                            viewModel.addToWatchlist()
                                        }
                                    }
                                    is ContentAction.Like -> {
                                        viewModel.toggleLike()
                                    }
                                    is ContentAction.Share -> {
                                        viewModel.shareMovie()
                                    }
                                    is ContentAction.Download -> {
                                        if (!action.isDownloaded && !action.isDownloading) {
                                            viewModel.downloadMovie()
                                        }
                                    }
                                    is ContentAction.Delete -> {
                                        viewModel.deleteFromRealDebrid()
                                    }
                                    else -> {
                                        // Handle other actions
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = overscanMargin, vertical = 8.dp)
                        )
                    }
                    
                    // Source Selection Section
                    item {
                        var selectedSourceId by remember { mutableStateOf<String?>(null) }
                        var showSourceDialog by remember { mutableStateOf(false) }
                        
                        when (sourcesState) {
                            is UiState.Loading -> {
                                // Show loading state for sources
                                Surface(
                                    modifier = Modifier.padding(horizontal = overscanMargin, vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Loading streaming sources...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            is UiState.Success -> {
                                val sources = (sourcesState as UiState.Success).data
                                println("DEBUG [MovieDetailsScreen]: UiState.Success received with ${sources.size} sources")
                                sources.forEach { source ->
                                    println("DEBUG [MovieDetailsScreen]: UI Source: ${source.provider.displayName} - ${source.quality.displayName} - ${source.id}")
                                }
                                if (sources.isNotEmpty()) {
                                    SourceSelectionSection(
                                        sources = sources,
                                        onSourceSelected = { source ->
                                            selectedSourceId = source.id
                                            // TODO: Handle source selection for movie playback
                                        },
                                        selectedSourceId = selectedSourceId,
                                        onViewAllClick = { showSourceDialog = true },
                                        modifier = Modifier.padding(horizontal = overscanMargin, vertical = 8.dp),
                                        showAllSources = true // DEBUG: Temporarily show all sources to test
                                    )
                                    
                                    if (showSourceDialog) {
                                        SourceSelectionDialog(
                                            sources = sources,
                                            onSourceSelected = { source ->
                                                selectedSourceId = source.id
                                                showSourceDialog = false
                                                // TODO: Handle source selection for movie playback
                                            },
                                            onDismiss = { showSourceDialog = false },
                                            selectedSourceId = selectedSourceId,
                                            title = "Select Movie Source"
                                        )
                                    
                                    // Show basic sources for fallback
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        items(sources.take(4)) { source -> // Show max 4 sources as preview
                                            Surface(
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .height(60.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                tonalElevation = 2.dp
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(8.dp),
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = source.provider.displayName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = source.quality.displayName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // No sources available
                                    Surface(
                                        modifier = Modifier.padding(horizontal = overscanMargin, vertical = 8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "No streaming sources available",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Check back later or try refreshing",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                            is UiState.Error -> {
                                // Show error state for sources
                                Surface(
                                    modifier = Modifier.padding(horizontal = overscanMargin, vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Failed to load streaming sources",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                            else -> {
                                // Idle state or any other state
                                // No UI to show
                            }
                        }
                    }
                    
                    // Tab navigation
                    item {
                        ContentDetailTabs(
                            selectedTabIndex = selectedTabIndex,
                            contentType = ContentType.MOVIE,
                            onTabSelected = { tabIndex -> viewModel.selectTab(tabIndex) },
                            modifier = Modifier.padding(horizontal = overscanMargin)
                        )
                    }
                    
                    // Tab content based on selected tab
                    when (selectedTabIndex) {
                        0 -> {
                            // Overview Tab
                            item {
                                MovieInfoSection(
                                    movie = movie,
                                    uiState = uiState,
                                    isOverview = true,
                                    modifier = Modifier.padding(horizontal = overscanMargin)
                                )
                            }
                        }
                        
                        1 -> {
                            // Details Tab
                            item {
                                MovieInfoSection(
                                    movie = movie,
                                    uiState = uiState,
                                    isOverview = false,
                                    modifier = Modifier.padding(horizontal = overscanMargin)
                                )
                            }
                            
                            // Related Movies Section
                            when (val currentRelatedMoviesState = relatedMoviesState) {
                                is com.rdwatch.androidtv.ui.common.UiState.Success -> {
                                    if (currentRelatedMoviesState.data.isNotEmpty()) {
                                        item {
                                            RelatedMoviesSection(
                                                movies = currentRelatedMoviesState.data,
                                                onMovieClick = onMovieClick,
                                                modifier = Modifier.padding(horizontal = overscanMargin)
                                            )
                                        }
                                    }
                                }
                                is com.rdwatch.androidtv.ui.common.UiState.Loading -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = overscanMargin),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                                is com.rdwatch.androidtv.ui.common.UiState.Error -> {
                                    // Don't show error for related movies, just skip section
                                }
                                else -> {
                                    // Handle UiState.Idle or any other state - just skip section
                                }
                            }
                        }
                        
                        2 -> {
                            // Cast & Crew Tab
                            when (val currentCreditsState = creditsState) {
                                is UiState.Success -> {
                                    item {
                                        CastCrewSection(
                                            metadata = currentCreditsState.data,
                                            modifier = Modifier.padding(horizontal = overscanMargin)
                                        )
                                    }
                                }
                                is UiState.Loading -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = overscanMargin),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                                is UiState.Error -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = overscanMargin),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Failed to load cast & crew",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    // Handle UiState.Idle or any other state - just skip section
                                }
                            }
                        }
                    }
                    
                    // Bottom spacing for TV overscan
                    item {
                        Spacer(modifier = Modifier.height(overscanMargin))
                    }
                }
            }
        }
        else -> {
            // Handle UiState.Idle or any other state
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
    
    // Advanced Source Selection Bottom Sheet
    SourceListBottomSheet(
        isVisible = showSourceSelection,
        sources = sourceSelectionState.filteredSources,
        selectedSource = sourceSelectionState.selectedSource,
        state = sourceSelectionState,
        onDismiss = { viewModel.hideSourceSelection() },
        onSourceSelected = { source ->
            viewModel.onSourceSelected(source)
            
            // Trigger movie playback with selected source
            movie?.let { currentMovie ->
                playbackViewModel.startMoviePlaybackWithSource(
                    movie = currentMovie,
                    source = source
                )
            }
        },
        onRefresh = {
            movie?.let { currentMovie ->
                val tmdbId = currentMovie.id.toString()
                val imdbId = uiState.tmdbResponse?.imdbId
                viewModel.loadSourcesForMovie(tmdbId, imdbId)
            }
        },
        onPlaySource = { source ->
            movie?.let { currentMovie ->
                playbackViewModel.startMoviePlaybackWithSource(
                    movie = currentMovie,
                    source = source
                )
            }
        }
    )
}

@Composable
private fun MovieHeroSection(
    movie: Movie,
    uiState: MovieDetailsUiState,
    watchProgress: Float,
    isCompleted: Boolean,
    onPlayClick: (Movie) -> Unit,
    onBackPressed: () -> Unit,
    firstFocusRequester: FocusRequester,
    overscanMargin: androidx.compose.ui.unit.Dp
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
                MetadataChip(text = uiState.getMovieYear())
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
    uiState: MovieDetailsUiState,
    isOverview: Boolean = false,
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
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2,
                    maxLines = if (isOverview) 3 else Int.MAX_VALUE
                )
            }
        }
        
        // Additional info (placeholder)
        if (isOverview) {
            // Overview: Show key metadata only
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                InfoItem(label = "Duration", value = uiState.getMovieRuntime())
                InfoItem(label = "Rating", value = uiState.getMovieRating())
            }
        } else {
            // Details: Show all metadata
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                item { InfoItem(label = "Duration", value = uiState.getMovieRuntime()) }
                item { InfoItem(label = "Language", value = "English") }
                item { InfoItem(label = "Rating", value = uiState.getMovieRating()) }
                item { InfoItem(label = "Studio", value = movie.studio ?: "Unknown") }
                item { InfoItem(label = "Year", value = uiState.getMovieYear()) }
                item { InfoItem(label = "Quality", value = "HD") }
            }
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

