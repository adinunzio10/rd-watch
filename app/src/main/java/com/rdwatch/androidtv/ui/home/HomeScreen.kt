package com.rdwatch.androidtv.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.MovieList
import com.rdwatch.androidtv.ui.components.PreloadImagesEffect
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.focus.TVSpatialNavigation
import com.rdwatch.androidtv.ui.focus.TVKeyHandlers
import com.rdwatch.androidtv.ui.focus.rememberTVKeyEventHandler
import com.rdwatch.androidtv.ui.focus.AutoTVFocus
import com.rdwatch.androidtv.ui.viewmodel.PlaybackViewModel
import com.rdwatch.androidtv.ui.components.ResumeDialogOverlay
import com.rdwatch.androidtv.ui.components.ContinueWatchingManager
import com.rdwatch.androidtv.navigation.PlaybackNavigationHelper
import com.rdwatch.androidtv.presentation.navigation.Screen

@Composable
fun TVHomeScreen(
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onNavigateToScreen: ((Any) -> Unit)? = null,
    onMovieClick: ((Movie) -> Unit)? = null
) {
    var isDrawerOpen by remember { mutableStateOf(false) }
    var showContinueWatchingManager by remember { mutableStateOf(false) }
    val drawerFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    
    // Observe playback UI state for resume dialog
    val playbackUiState by playbackViewModel.uiState.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()
    val inProgressContent by playbackViewModel.inProgressContent.collectAsState()
    val movies = remember { MovieList.list }
    
    // Enhanced key event handler for TV navigation
    val keyEventHandler = rememberTVKeyEventHandler().apply {
        onDPadLeft = {
            if (!isDrawerOpen) {
                isDrawerOpen = true
                true
            } else false
        }
        onBack = {
            if (isDrawerOpen) {
                isDrawerOpen = false
                true
            } else false
        }
        onMenu = {
            isDrawerOpen = !isDrawerOpen
            true
        }
    }
    
    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) {
            drawerFocusRequester.requestFocus()
        } else {
            contentFocusRequester.requestFocus()
        }
    }
    
    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }
    
    TVSpatialNavigation(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        onNavigateLeft = { keyEventHandler.onDPadLeft?.invoke() ?: false },
        onBack = { keyEventHandler.onBack?.invoke() ?: false },
        onSelect = { keyEventHandler.onDPadCenter?.invoke() ?: false }
    ) {
        // Main content with overscan safety
        SafeAreaContent(
            modifier = Modifier.fillMaxSize(),
            contentFocusRequester = contentFocusRequester,
            onDrawerToggle = { isDrawerOpen = !isDrawerOpen },
            playbackViewModel = playbackViewModel,
            onShowContinueWatching = { showContinueWatchingManager = true },
            onMovieClick = onMovieClick
        )
        
        // Navigation drawer
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            ),
            modifier = Modifier.zIndex(1f)
        ) {
            TVNavigationDrawer(
                focusRequester = drawerFocusRequester,
                onItemSelected = { screen ->
                    isDrawerOpen = false
                    onNavigateToScreen?.invoke(screen)
                },
                onBackPressed = { isDrawerOpen = false }
            )
        }
        
        // Backdrop overlay
        if (isDrawerOpen) {
            val alpha by animateFloatAsState(
                targetValue = 0.5f,
                animationSpec = tween(300),
                label = "backdrop_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = alpha))
                    .zIndex(0.5f)
            )
        }
        
        // Resume dialog overlay
        ResumeDialogOverlay(
            showDialog = playbackUiState.showResumeDialog || playerState.shouldShowResumeDialog,
            title = playerState.title ?: "Unknown Title",
            resumePosition = playerState.formattedResumePosition,
            onResumeClick = { playbackViewModel.resumeFromDialog() },
            onRestartClick = { playbackViewModel.restartFromBeginning() },
            onDismiss = { playbackViewModel.dismissResumeDialog() }
        )
        
        // Continue Watching Manager
        if (showContinueWatchingManager) {
            ContinueWatchingManager(
                inProgressContent = inProgressContent,
                movies = movies,
                onPlayClick = { movie ->
                    // TODO: Navigate to player with movie
                    showContinueWatchingManager = false
                },
                onRemoveClick = { contentId ->
                    playbackViewModel.removeFromContinueWatching(contentId)
                },
                onCloseClick = { showContinueWatchingManager = false }
            )
        }
    }
}

@Composable
fun TVNavigationDrawer(
    focusRequester: FocusRequester,
    onItemSelected: (Any) -> Unit,
    onBackPressed: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val drawerWidth = (configuration.screenWidthDp * 0.25f).dp.coerceAtLeast(200.dp)
    
    val navigationItems = listOf(
        NavigationItem("Home", Icons.Default.Home, Screen.Home),
        NavigationItem("Browse", Icons.Default.Search, Screen.Browse),
        NavigationItem("Search", Icons.Default.Search, Screen.Search),
        NavigationItem("Profile", Icons.Default.Person, Screen.Profile),
        NavigationItem("Settings", Icons.Default.Settings, Screen.Settings)
    )
    
    Surface(
        modifier = Modifier
            .width(drawerWidth)
            .fillMaxHeight()
            .padding(start = 32.dp, top = 32.dp, bottom = 32.dp), // TV overscan safety
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drawer header
            Text(
                text = "RD Watch",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Navigation items
            navigationItems.forEachIndexed { index, item ->
                NavigationDrawerItem(
                    item = item,
                    modifier = if (index == 0) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
                    onClick = {
                        onItemSelected(item.destination)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawerItem(
    item: NavigationItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp && 
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isFocused) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFocused) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun SafeAreaContent(
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester,
    onDrawerToggle: () -> Unit,
    playbackViewModel: PlaybackViewModel,
    onShowContinueWatching: () -> Unit,
    onMovieClick: ((Movie) -> Unit)? = null
) {
    val overscanMargin = 32.dp // 5% for most TVs
    
    Column(
        modifier = modifier
            .padding(overscanMargin)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App title with menu button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDrawerToggle,
                modifier = Modifier.focusRequester(contentFocusRequester)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open navigation menu",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Text(
                text = "RD Watch",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
        }
        
        // Content rows placeholder
        TVContentGrid(
            playbackViewModel = playbackViewModel,
            onShowContinueWatching = onShowContinueWatching,
            onMovieClick = onMovieClick
        )
    }
}

@Composable
fun TVContentGrid(
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onShowContinueWatching: () -> Unit = {},
    onMovieClick: ((Movie) -> Unit)? = null
) {
    val movies = remember { MovieList.list }
    val firstRowFocusRequester = remember { FocusRequester() }
    
    // Observe playback state
    val inProgressContent by playbackViewModel.inProgressContent.collectAsState()
    val watchStatistics by playbackViewModel.watchStatistics.collectAsState()
    
    // Initialize playback state observation
    LaunchedEffect(Unit) {
        playbackViewModel.observePlayerState()
    }
    
    // Create different content categories with varying layouts
    val contentRows = remember(inProgressContent) {
        buildList {
            // Only show Continue Watching if there's content in progress
            if (inProgressContent.isNotEmpty()) {
                val continueWatchingMovies = inProgressContent.mapNotNull { progress ->
                    // Map content IDs back to movies - in a real app this would be more sophisticated
                    movies.find { it.videoUrl == progress.contentId }
                }.take(10)
                
                if (continueWatchingMovies.isNotEmpty()) {
                    add(
                        ContentRowData(
                            title = "Continue Watching",
                            movies = continueWatchingMovies,
                            type = ContentRowType.CONTINUE_WATCHING,
                            showViewAll = continueWatchingMovies.size > 3
                        )
                    )
                }
            }
            
            // Add other content rows
            addAll(
                listOf(
                    ContentRowData(
                        title = "Featured",
                        movies = movies.take(4),
                        type = ContentRowType.FEATURED
                    ),
                    ContentRowData(
                        title = "Recently Added",
                        movies = movies,
                        type = ContentRowType.STANDARD
                    ),
                    ContentRowData(
                        title = "My Library",
                        movies = movies.reversed(),
                        type = ContentRowType.STANDARD
                    ),
                    ContentRowData(
                        title = "Popular Movies",
                        movies = movies.shuffled().take(6),
                        type = ContentRowType.STANDARD
                    )
                )
            )
        }
    }
    
    // Preload images for smooth scrolling
    val allImageUrls = remember(movies) {
        movies.mapNotNull { it.cardImageUrl } + 
        movies.mapNotNull { it.backgroundImageUrl }
    }
    
    PreloadImagesEffect(
        imageUrls = allImageUrls,
        priority = ImagePriority.LOW
    )
    
    LaunchedEffect(Unit) {
        firstRowFocusRequester.requestFocus()
    }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        items(contentRows.size) { index ->
            val row = contentRows[index]
            TVContentRow(
                title = row.title,
                items = row.movies,
                contentType = row.type,
                firstItemFocusRequester = if (index == 0) firstRowFocusRequester else null,
                playbackViewModel = playbackViewModel,
                showViewAll = row.showViewAll,
                onViewAllClick = if (row.type == ContentRowType.CONTINUE_WATCHING) {
                    { onShowContinueWatching() }
                } else null,
                onItemClick = { movie ->
                    onMovieClick?.invoke(movie)
                }
            )
        }
    }
}

data class ContentRowData(
    val title: String,
    val movies: List<Movie>,
    val type: ContentRowType,
    val showViewAll: Boolean = false
)

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val destination: Any
)