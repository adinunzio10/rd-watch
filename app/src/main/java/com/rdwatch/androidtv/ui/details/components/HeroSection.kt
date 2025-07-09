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
import androidx.compose.ui.draw.blur
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
import com.rdwatch.androidtv.presentation.components.tvFocusable
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator

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
        
        // Floating poster image
        FloatingPosterImage(
            imageUrl = content.cardImageUrl,
            contentDescription = content.title,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(overscanMargin)
                .padding(bottom = 120.dp)
        )
        
        // Content info overlay
        HeroContentInfo(
            content = content,
            progress = progress,
            onActionClick = onActionClick,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(overscanMargin)
                .padding(start = 200.dp) // Offset for poster image
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
            .blur(radius = 1.dp) // Subtle blur effect for better text readability
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
                    onFocusChanged = { backButtonFocused = it }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Content title with enhanced typography
        Text(
            text = content.getDisplayTitle(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = MaterialTheme.typography.displayLarge.lineHeight * 0.9f
            ),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Subtitle/tagline from description (first line)
        content.description?.let { description ->
            if (description.isNotBlank()) {
                Text(
                    text = description.lines().first().take(120) + if (description.length > 120) "..." else "",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 1.1f
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Metadata chips with improved spacing
        HeroMetadataRow(
            content = content,
            progress = progress
        )
        
        // Progress indicator with enhanced styling
        if (progress.hasProgress && !progress.isCompleted) {
            HeroProgressIndicator(progress = progress)
        }
        
        // Primary action button with enhanced styling
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
        // Display content-type specific metadata chips
        val chips = getContentTypeSpecificChips(content)
        chips.take(5).forEach { chip ->
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

/**
 * Get content-type specific metadata chips
 */
private fun getContentTypeSpecificChips(content: ContentDetail): List<MetadataChip> {
    val chips = mutableListOf<MetadataChip>()
    
    when (content.contentType) {
        ContentType.MOVIE -> {
            // Movie-specific metadata: Quality, Rating, Year, Duration, Studio
            content.metadata.quality?.let { chips.add(MetadataChip.Quality(it)) }
            if (content.metadata.is4K) chips.add(MetadataChip.Quality("4K"))
            if (content.metadata.isHDR) chips.add(MetadataChip.Quality("HDR"))
            content.metadata.rating?.let { chips.add(MetadataChip.Rating(it)) }
            content.metadata.year?.let { chips.add(MetadataChip.Year(it)) }
            content.metadata.duration?.let { chips.add(MetadataChip.Duration(it)) }
            content.metadata.studio?.let { chips.add(MetadataChip.Studio(it)) }
        }
        ContentType.TV_SHOW -> {
            // TV Show-specific metadata: Quality, Rating, Year, Seasons, Network
            content.metadata.quality?.let { chips.add(MetadataChip.Quality(it)) }
            if (content.metadata.is4K) chips.add(MetadataChip.Quality("4K"))
            if (content.metadata.isHDR) chips.add(MetadataChip.Quality("HDR"))
            content.metadata.rating?.let { chips.add(MetadataChip.Rating(it)) }
            content.metadata.year?.let { chips.add(MetadataChip.Year(it)) }
            
            // Add seasons info from custom metadata
            content.metadata.customMetadata["seasons"]?.let { seasons ->
                chips.add(MetadataChip.Custom("$seasons Seasons"))
            }
            content.metadata.customMetadata["episodes"]?.let { episodes ->
                chips.add(MetadataChip.Custom("$episodes Episodes"))
            }
            
            content.metadata.studio?.let { chips.add(MetadataChip.Studio(it)) }
        }
        ContentType.TV_EPISODE -> {
            // Episode-specific metadata: Quality, Rating, Season/Episode, Duration
            content.metadata.quality?.let { chips.add(MetadataChip.Quality(it)) }
            if (content.metadata.is4K) chips.add(MetadataChip.Quality("4K"))
            if (content.metadata.isHDR) chips.add(MetadataChip.Quality("HDR"))
            content.metadata.rating?.let { chips.add(MetadataChip.Rating(it)) }
            
            // Show season and episode info
            val season = content.metadata.season
            val episode = content.metadata.episode
            if (season != null && episode != null) {
                chips.add(MetadataChip.Custom("S${season}E${episode}"))
            }
            
            content.metadata.duration?.let { chips.add(MetadataChip.Duration(it)) }
            content.metadata.year?.let { chips.add(MetadataChip.Year(it)) }
        }
        else -> {
            // Default metadata for other content types
            content.metadata.quality?.let { chips.add(MetadataChip.Quality(it)) }
            if (content.metadata.is4K) chips.add(MetadataChip.Quality("4K"))
            if (content.metadata.isHDR) chips.add(MetadataChip.Quality("HDR"))
            content.metadata.rating?.let { chips.add(MetadataChip.Rating(it)) }
            content.metadata.year?.let { chips.add(MetadataChip.Year(it)) }
            content.metadata.duration?.let { chips.add(MetadataChip.Duration(it)) }
        }
    }
    
    return chips
}

@Composable
private fun HeroMetadataChip(
    chip: MetadataChip
) {
    when (chip) {
        is MetadataChip.Rating -> {
            RatingBadge(rating = chip.text)
        }
        is MetadataChip.Quality -> {
            QualityBadge(quality = chip.text)
        }
        else -> {
            DefaultMetadataChip(chip = chip)
        }
    }
}

@Composable
private fun RatingBadge(
    rating: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700), // Gold color for star
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = rating,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun QualityBadge(
    quality: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (quality) {
        "4K" -> Color(0xFF4CAF50) // Green for 4K
        "HDR" -> Color(0xFFFF9800) // Orange for HDR
        "HD" -> Color(0xFF2196F3) // Blue for HD
        else -> Color.Black.copy(alpha = 0.7f)
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (quality in listOf("4K", "HDR", "HD")) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = quality,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DefaultMetadataChip(
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = progress.getProgressText(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White.copy(alpha = 0.95f)
            )
        }
        LinearProgressIndicator(
            progress = { progress.watchPercentage },
            modifier = Modifier
                .width(240.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.2f)
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
                    onFocusChanged = { isFocused = it }
                )
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                }
            ),
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isFocused) 8.dp else 4.dp
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = if (isResume) "Resume Playing" else action.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * Floating poster image component that overlays on the hero section
 */
@Composable
private fun FloatingPosterImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(width = 160.dp, height = 240.dp),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        SmartTVImageLoader(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            priority = ImagePriority.HIGH,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        )
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