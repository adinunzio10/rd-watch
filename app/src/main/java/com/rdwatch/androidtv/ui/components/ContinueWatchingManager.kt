package com.rdwatch.androidtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.data.entities.WatchProgressEntity
import com.rdwatch.androidtv.ui.focus.tvFocusable
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ContinueWatchingManager(
    inProgressContent: List<WatchProgressEntity>,
    movies: List<Movie>,
    onPlayClick: (Movie) -> Unit,
    onRemoveClick: (String) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    
    Dialog(
        onDismissRequest = onCloseClick,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize(0.9f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continue Watching",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = onCloseClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (inProgressContent.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "No content in progress",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Start watching something to see it here",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Content list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(inProgressContent) { progressEntity ->
                            val movie = movies.find { it.videoUrl == progressEntity.contentId }
                            if (movie != null) {
                                ContinueWatchingItem(
                                    movie = movie,
                                    progressEntity = progressEntity,
                                    onPlayClick = { onPlayClick(movie) },
                                    onRemoveClick = { onRemoveClick(progressEntity.contentId) },
                                    modifier = if (progressEntity == inProgressContent.first()) {
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
    }
    
    // Auto-focus first item
    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContinueWatchingItem(
    movie: Movie,
    progressEntity: WatchProgressEntity,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    TVFocusIndicator(
        isFocused = isFocused
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 8.dp else 2.dp
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    SmartTVImageLoader(
                        imageUrl = movie.cardImageUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        priority = ImagePriority.NORMAL,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Progress overlay
                    LinearProgressIndicator(
                        progress = { progressEntity.watchPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
                
                // Content info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = movie.title ?: "Unknown Title",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = "${(progressEntity.watchPercentage * 100).toInt()}% watched",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "Last watched: ${dateFormatter.format(progressEntity.updatedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ContinueWatchingActionButton(
                            text = "Play",
                            icon = Icons.Default.PlayArrow,
                            onClick = onPlayClick,
                            isPrimary = true
                        )
                        
                        ContinueWatchingActionButton(
                            text = "Remove",
                            icon = Icons.Default.Delete,
                            onClick = onRemoveClick,
                            isPrimary = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(
        isFocused = isFocused
    ) {
        Button(
            onClick = onClick,
            modifier = modifier
                .height(36.dp)
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                contentColor = if (isPrimary) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            ),
            contentPadding = PaddingValues(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}