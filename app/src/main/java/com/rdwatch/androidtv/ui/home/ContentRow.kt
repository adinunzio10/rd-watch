package com.rdwatch.androidtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.components.TVBackgroundImage

@Composable
fun TVContentRow(
    title: String,
    items: List<Movie>,
    modifier: Modifier = Modifier,
    onItemClick: (Movie) -> Unit = {},
    contentType: ContentRowType = ContentRowType.STANDARD,
    firstItemFocusRequester: FocusRequester? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        // Content row
        val listState = rememberLazyListState()
        
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items) { index, item ->
                when (contentType) {
                    ContentRowType.FEATURED -> {
                        FeaturedContentCard(
                            movie = item,
                            onClick = { onItemClick(item) },
                            modifier = if (index == 0 && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                    }
                    ContentRowType.CONTINUE_WATCHING -> {
                        ContinueWatchingCard(
                            movie = item,
                            progress = 0.3f + (index * 0.1f), // Mock progress
                            onClick = { onItemClick(item) },
                            modifier = if (index == 0 && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                    }
                    ContentRowType.STANDARD -> {
                        StandardContentCard(
                            movie = item,
                            onClick = { onItemClick(item) },
                            modifier = if (index == 0 && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardContentCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .size(
                width = if (isFocused) 220.dp else 200.dp,
                height = if (isFocused) 140.dp else 120.dp
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 12.dp else 4.dp
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail image with smart loading
            SmartTVImageLoader(
                imageUrl = movie.cardImageUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                priority = ImagePriority.NORMAL,
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
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0.6f
                        )
                    )
            )
            
            // Title overlay
            Text(
                text = movie.title ?: "Unknown Title",
                style = if (isFocused) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleSmall
                },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedContentCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .size(
                width = if (isFocused) 360.dp else 320.dp,
                height = if (isFocused) 200.dp else 180.dp
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 16.dp else 6.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background image with enhanced loading
            TVBackgroundImage(
                imageUrl = movie.backgroundImageUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                overlayAlpha = 0.8f
            )
            
            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = movie.title ?: "Unknown Title",
                    style = if (isFocused) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (movie.studio != null) {
                    Text(
                        text = movie.studio!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinueWatchingCard(
    movie: Movie,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .size(
                width = if (isFocused) 280.dp else 260.dp,
                height = if (isFocused) 160.dp else 140.dp
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 14.dp else 5.dp
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail image with smart loading
            SmartTVImageLoader(
                imageUrl = movie.cardImageUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                priority = ImagePriority.HIGH, // Higher priority for continue watching
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            ),
                            startY = 0.5f
                        )
                    )
            )
            
            // Progress indicator
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            
            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .padding(bottom = 8.dp), // Account for progress bar
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = movie.title ?: "Unknown Title",
                    style = if (isFocused) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${(progress * 100).toInt()}% watched",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

enum class ContentRowType {
    STANDARD,
    FEATURED,
    CONTINUE_WATCHING
}

@Preview
@Composable
fun ContentRowPreview() {
    val sampleMovies = listOf(
        Movie(1, "Sample Movie 1", "Description", "", "", "", "Studio 1"),
        Movie(2, "Sample Movie 2", "Description", "", "", "", "Studio 2"),
        Movie(3, "Sample Movie 3", "Description", "", "", "", "Studio 3")
    )
    
    MaterialTheme {
        TVContentRow(
            title = "Featured Movies",
            items = sampleMovies,
            contentType = ContentRowType.FEATURED
        )
    }
}