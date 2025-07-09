package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Hero section component for content detail screens
 * Displays backdrop image, title, metadata, progress, and primary action
 */
@Composable
fun HeroSection(
    content: ContentDetail,
    progress: ContentProgress = ContentProgress(),
    onActionClick: (ContentAction) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    firstFocusRequester: FocusRequester = remember { FocusRequester() },
    overscanMargin: androidx.compose.ui.unit.Dp = 32.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Background image
        HeroBackground(
            imageUrl = content.getPrimaryImageUrl(),
            contentDescription = content.title,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlay for text readability
        HeroGradientOverlay(modifier = Modifier.fillMaxSize())
        
        // Back button
        HeroBackButton(
            onBackPressed = onBackPressed,
            firstFocusRequester = firstFocusRequester,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(overscanMargin)
        )
        
        // Content info overlay
        HeroContentInfo(
            content = content,
            progress = progress,
            onActionClick = onActionClick,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(overscanMargin)
                .fillMaxWidth(0.6f)
        )
    }
}

@Composable
private fun HeroBackground(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    SmartTVImageLoader(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        priority = ImagePriority.HIGH,
        modifier = modifier
    )
}

@Composable
private fun HeroGradientOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
}

@Composable
private fun HeroBackButton(
    onBackPressed: () -> Unit,
    firstFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var backButtonFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = backButtonFocused) {
        IconButton(
            onClick = onBackPressed,
            modifier = modifier
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
}

@Composable
private fun HeroContentInfo(
    content: ContentDetail,
    progress: ContentProgress,
    onActionClick: (ContentAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Content title
        Text(
            text = content.getDisplayTitle(),
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Metadata chips
        HeroMetadataRow(
            content = content,
            progress = progress
        )
        
        // Progress indicator
        if (progress.hasProgress && !progress.isCompleted) {
            HeroProgressIndicator(progress = progress)
        }
        
        // Primary action button
        HeroPrimaryAction(
            content = content,
            progress = progress,
            onActionClick = onActionClick
        )
    }
}

@Composable
private fun HeroMetadataRow(
    content: ContentDetail,
    progress: ContentProgress
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display metadata chips
        content.getMetadataChips().take(4).forEach { chip ->
            HeroMetadataChip(chip = chip)
        }
        
        // Show completion status if watched
        if (progress.isCompleted) {
            HeroMetadataChip(
                chip = MetadataChip.Custom(
                    text = "Watched",
                    icon = "check_circle"
                )
            )
        }
    }
}

@Composable
private fun HeroMetadataChip(
    chip: MetadataChip
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
            // Icon support would need to be implemented based on chip.icon
            chip.icon?.let { iconName ->
                val icon = getIconForName(iconName)
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = chip.text,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HeroProgressIndicator(progress: ContentProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = progress.getProgressText(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
        LinearProgressIndicator(
            progress = { progress.watchPercentage },
            modifier = Modifier
                .width(200.dp)
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun HeroPrimaryAction(
    content: ContentDetail,
    progress: ContentProgress,
    onActionClick: (ContentAction) -> Unit
) {
    val primaryAction = content.actions.find { it is ContentAction.Play }
    
    if (primaryAction != null) {
        HeroActionButton(
            action = primaryAction,
            onClick = { onActionClick(primaryAction) },
            isResume = progress.isPartiallyWatched
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeroActionButton(
    action: ContentAction,
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
                    text = if (isResume) "Resume" else action.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Helper function to get icon for string name
 * This would need to be expanded based on actual icon requirements
 */
private fun getIconForName(iconName: String): ImageVector? {
    return when (iconName) {
        "check_circle" -> Icons.Default.CheckCircle
        "play_arrow" -> Icons.Default.PlayArrow
        "star" -> Icons.Default.Star
        "hd" -> Icons.Default.HighQuality
        else -> null
    }
}

/**
 * Preview/Demo configurations for HeroSection
 */
object HeroSectionPreview {
    fun createSampleMovieContent(): ContentDetail {
        return object : ContentDetail {
            override val id: String = "1"
            override val title: String = "Sample Movie"
            override val description: String? = "A great movie for testing the hero section"
            override val backgroundImageUrl: String? = "https://example.com/backdrop.jpg"
            override val cardImageUrl: String? = "https://example.com/poster.jpg"
            override val contentType: ContentType = ContentType.MOVIE
            override val videoUrl: String? = "https://example.com/video.mp4"
            override val metadata: ContentMetadata = ContentMetadata(
                year = "2023",
                duration = "2h 15m",
                rating = "PG-13",
                quality = "4K",
                is4K = true,
                isHDR = true
            )
            override val actions: List<ContentAction> = listOf(
                ContentAction.Play(),
                ContentAction.AddToWatchlist(),
                ContentAction.Like(),
                ContentAction.Share()
            )
        }
    }
    
    fun createSampleProgress(): ContentProgress {
        return ContentProgress(
            watchPercentage = 0.35f,
            isCompleted = false,
            resumePosition = 3600000L, // 1 hour
            totalDuration = 8100000L // 2h 15m
        )
    }
}