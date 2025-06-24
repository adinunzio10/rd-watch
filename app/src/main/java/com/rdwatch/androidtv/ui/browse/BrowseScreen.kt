package com.rdwatch.androidtv.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.MovieList
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator

/**
 * Browse Screen for discovering content by categories
 * Follows Android TV 10-foot UI guidelines
 */
@Composable
fun BrowseScreen(
    modifier: Modifier = Modifier,
    onMovieClick: (Movie) -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val overscanMargin = 32.dp
    val firstFocusRequester = remember { FocusRequester() }
    var selectedCategory by remember { mutableStateOf(BrowseCategory.ALL) }
    
    // Filter movies based on selected category
    val movies = remember { MovieList.list }
    val filteredMovies = remember(selectedCategory) {
        when (selectedCategory) {
            BrowseCategory.ALL -> movies
            BrowseCategory.ACTION -> movies.filter { it.studio?.contains("Action", ignoreCase = true) == true || it.title?.contains("Action", ignoreCase = true) == true }
            BrowseCategory.DRAMA -> movies.filter { it.studio?.contains("Drama", ignoreCase = true) == true || it.title?.contains("Drama", ignoreCase = true) == true }
            BrowseCategory.COMEDY -> movies.filter { it.studio?.contains("Comedy", ignoreCase = true) == true || it.title?.contains("Comedy", ignoreCase = true) == true }
            BrowseCategory.THRILLER -> movies.filter { it.studio?.contains("Thriller", ignoreCase = true) == true || it.title?.contains("Thriller", ignoreCase = true) == true }
            BrowseCategory.DOCUMENTARY -> movies.filter { it.studio?.contains("Documentary", ignoreCase = true) == true || it.title?.contains("Documentary", ignoreCase = true) == true }
        }
    }
    
    LaunchedEffect(Unit) {
        firstFocusRequester.requestFocus()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(overscanMargin),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        BrowseHeader(
            onBackPressed = onBackPressed,
            firstFocusRequester = firstFocusRequester
        )
        
        // Category filters
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )
        
        // Content grid
        BrowseContentGrid(
            movies = filteredMovies,
            onMovieClick = onMovieClick,
            category = selectedCategory
        )
    }
}

@Composable
private fun BrowseHeader(
    onBackPressed: () -> Unit,
    firstFocusRequester: FocusRequester
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        var backButtonFocused by remember { mutableStateOf(false) }
        
        TVFocusIndicator(isFocused = backButtonFocused) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
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
                        MaterialTheme.colorScheme.onBackground
                    }
                )
            }
        }
        
        // Title
        Text(
            text = "Browse Content",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: BrowseCategory,
    onCategorySelected: (BrowseCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(BrowseCategory.values().toList()) { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(
    category: BrowseCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        FilterChip(
            onClick = onClick,
            label = {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            selected = isSelected,
            leadingIcon = {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else if (isFocused) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                labelColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else if (isFocused) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                iconColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else if (isFocused) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
            border = if (isFocused && !isSelected) {
                FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 2.dp
                )
            } else null
        )
    }
}

@Composable
private fun BrowseContentGrid(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    category: BrowseCategory
) {
    if (movies.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "No content found in ${category.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Try selecting a different category",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        // Content grid - using LazyColumn with chunked rows for better TV navigation
        val chunkedMovies = movies.chunked(4) // 4 items per row for TV
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(chunkedMovies) { rowIndex, movieRow ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(movieRow) { columnIndex, movie ->
                        BrowseMovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = onClick,
            modifier = modifier
                .size(
                    width = if (isFocused) 200.dp else 180.dp,
                    height = if (isFocused) 280.dp else 260.dp
                )
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 12.dp else 4.dp
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Movie poster
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                ) {
                    SmartTVImageLoader(
                        imageUrl = movie.cardImageUrl,
                        contentDescription = movie.title,
                        priority = ImagePriority.NORMAL,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Movie info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = movie.title ?: "Unknown Title",
                        style = if (isFocused) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (movie.studio != null) {
                        Text(
                            text = movie.studio!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

enum class BrowseCategory(
    val displayName: String,
    val icon: ImageVector
) {
    ALL("All", Icons.Default.Apps),
    ACTION("Action", Icons.Default.LocalFireDepartment),
    DRAMA("Drama", Icons.Default.TheaterComedy),
    COMEDY("Comedy", Icons.Default.SentimentSatisfied),
    THRILLER("Thriller", Icons.Default.Warning),
    DOCUMENTARY("Documentary", Icons.Default.DocumentScanner)
}