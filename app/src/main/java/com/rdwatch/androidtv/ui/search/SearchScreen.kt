package com.rdwatch.androidtv.ui.search

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rdwatch.androidtv.ui.components.TVImageLoader
import com.rdwatch.androidtv.ui.focus.TVSpatialNavigation

/**
 * Main search screen with TV keyboard, voice search, and results
 */
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    
    var showKeyboard by remember { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }
    var isVoiceSearchActive by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isVoiceSearchActive = true
            viewModel.startVoiceSearch()
        }
    }
    
    val backButtonFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        backButtonFocusRequester.requestFocus()
    }
    
    TVSpatialNavigation(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        onBack = { 
            when {
                showFilters -> showFilters = false
                isVoiceSearchActive -> {
                    isVoiceSearchActive = false
                    viewModel.stopVoiceSearch()
                }
                uiState.searchQuery.isNotEmpty() -> {
                    viewModel.clearSearch()
                    showKeyboard = true
                }
                else -> onNavigateBack()
            }
            true
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp), // TV overscan safety
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header with back button and title
                SearchHeader(
                    onNavigateBack = onNavigateBack,
                    onToggleFilters = { showFilters = !showFilters },
                    backButtonFocusRequester = backButtonFocusRequester,
                    hasFiltersApplied = uiState.hasActiveFilters
                )
                
                // Search content area
                when {
                    isVoiceSearchActive -> {
                        VoiceSearchUI(
                            onCancel = { 
                                isVoiceSearchActive = false
                                viewModel.stopVoiceSearch()
                            },
                            listeningState = uiState.voiceSearchState
                        )
                    }
                    
                    showKeyboard || uiState.searchQuery.isEmpty() -> {
                        // Show keyboard and search history
                        SearchInputSection(
                            searchQuery = uiState.searchQuery,
                            searchHistory = searchHistory,
                            onTextChanged = viewModel::updateSearchQuery,
                            onSearch = { 
                                viewModel.performSearch()
                                showKeyboard = false
                            },
                            onVoiceSearch = { 
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    isVoiceSearchActive = true
                                    viewModel.startVoiceSearch()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onClear = viewModel::clearSearch,
                            onHistoryItemSelected = { query ->
                                viewModel.updateSearchQuery(query)
                                viewModel.performSearch()
                                showKeyboard = false
                            },
                            onDeleteHistoryItem = viewModel::deleteSearchHistoryItem,
                            isVoiceSearchEnabled = uiState.isVoiceSearchAvailable
                        )
                    }
                    
                    else -> {
                        // Show search results
                        SearchResultsSection(
                            searchResults = uiState.searchResults,
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            onItemSelected = onItemSelected,
                            onRetry = viewModel::performSearch,
                            onShowKeyboard = { showKeyboard = true }
                        )
                    }
                }
            }
            
            // Filters panel overlay
            AnimatedVisibility(
                visible = showFilters,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(1f)
            ) {
                SearchFiltersPanel(
                    filters = uiState.searchFilters,
                    onFiltersChanged = viewModel::updateFilters,
                    onClose = { showFilters = false }
                )
            }
            
            // Backdrop overlay for filters
            if (showFilters) {
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
        }
    }
}

@Composable
private fun SearchHeader(
    onNavigateBack: () -> Unit,
    onToggleFilters: () -> Unit,
    backButtonFocusRequester: FocusRequester,
    hasFiltersApplied: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.focusRequester(backButtonFocusRequester)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Text(
                text = "Search",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
        
        IconButton(
            onClick = onToggleFilters
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filters",
                tint = if (hasFiltersApplied) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
            )
        }
    }
}

@Composable
private fun SearchInputSection(
    searchQuery: String,
    searchHistory: List<String>,
    onTextChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onVoiceSearch: () -> Unit,
    onClear: () -> Unit,
    onHistoryItemSelected: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    isVoiceSearchEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // TV Keyboard
        TVSearchKeyboard(
            onTextChanged = onTextChanged,
            onSearch = onSearch,
            onVoiceSearch = onVoiceSearch,
            onClear = onClear,
            initialText = searchQuery,
            isVoiceSearchEnabled = isVoiceSearchEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Search History
        if (searchHistory.isNotEmpty() && searchQuery.isEmpty()) {
            SearchHistorySection(
                searchHistory = searchHistory,
                onHistoryItemSelected = onHistoryItemSelected,
                onDeleteHistoryItem = onDeleteHistoryItem
            )
        }
    }
}

@Composable
private fun SearchHistorySection(
    searchHistory: List<String>,
    onHistoryItemSelected: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(searchHistory.take(10)) { query ->
                SearchHistoryItem(
                    query = query,
                    onSelected = { onHistoryItemSelected(query) },
                    onDelete = { onDeleteHistoryItem(query) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchHistoryItem(
    query: String,
    onSelected: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onSelected,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack, // Using as close/delete icon
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    searchResults: List<SearchResultItem>,
    isLoading: Boolean,
    error: String?,
    onItemSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onShowKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Results header with search again button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLoading) "Searching..." else "Search Results",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            TextButton(onClick = onShowKeyboard) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Again")
            }
        }
        
        // Results content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            error != null -> {
                SearchErrorState(
                    error = error,
                    onRetry = onRetry
                )
            }
            
            searchResults.isEmpty() -> {
                SearchEmptyState(onSearchAgain = onShowKeyboard)
            }
            
            else -> {
                SearchResultsList(
                    results = searchResults,
                    onItemSelected = onItemSelected
                )
            }
        }
    }
}

@Composable
private fun SearchErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Search Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    onSearchAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No Results Found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Try different keywords or check your spelling",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSearchAgain) {
                Text("Search Again")
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SearchResultItem>,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(results) { result ->
            SearchResultCard(
                result = result,
                onSelected = { onItemSelected(result.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultCard(
    result: SearchResultItem,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onSelected,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Poster image
            TVImageLoader(
                imageUrl = result.thumbnailUrl,
                contentDescription = result.title,
                modifier = Modifier.size(80.dp, 120.dp),
                shape = RoundedCornerShape(8.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                result.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    result.year?.let { year ->
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    result.rating?.let { rating ->
                        Text(
                            text = "★ $rating",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a search result item
 */
data class SearchResultItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val year: Int? = null,
    val rating: Float? = null,
    val scraperSource: String? = null
)