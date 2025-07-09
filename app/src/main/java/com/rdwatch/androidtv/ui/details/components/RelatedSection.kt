package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Related content section component for content detail screens
 * Displays related movies, TV shows, or other content based on the current content
 */
@Composable
fun RelatedSection(
    relatedContent: List<ContentDetail>,
    onContentClick: (ContentDetail) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "More Like This",
    maxItems: Int = 12,
    cardStyle: RelatedCardStyle = RelatedCardStyle.STANDARD
) {
    if (relatedContent.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            
            // Related content row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(relatedContent.take(maxItems)) { content ->
                    RelatedContentCard(
                        content = content,
                        onClick = { onContentClick(content) },
                        cardStyle = cardStyle
                    )
                }
            }
        }
    }
}

/**
 * Multiple related content sections with different categories
 */
@Composable
fun RelatedContentSections(
    relatedContentState: RelatedContentState,
    onContentClick: (ContentDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Similar content
        val similarContent = relatedContentState.similar.dataOrNull
        if (!similarContent.isNullOrEmpty()) {
            RelatedSection(
                relatedContent = similarContent,
                onContentClick = onContentClick,
                title = "Similar Content",
                cardStyle = RelatedCardStyle.STANDARD
            )
        }
        
        // Recommended content
        val recommendedContent = relatedContentState.recommended.dataOrNull
        if (!recommendedContent.isNullOrEmpty()) {
            RelatedSection(
                relatedContent = recommendedContent,
                onContentClick = onContentClick,
                title = "Recommended for You",
                cardStyle = RelatedCardStyle.STANDARD
            )
        }
        
        // Movies
        val movies = relatedContentState.movies.dataOrNull
        if (!movies.isNullOrEmpty()) {
            RelatedSection(
                relatedContent = movies,
                onContentClick = onContentClick,
                title = "More Movies",
                cardStyle = RelatedCardStyle.COMPACT
            )
        }
        
        // TV Shows
        val tvShows = relatedContentState.tvShows.dataOrNull
        if (!tvShows.isNullOrEmpty()) {
            RelatedSection(
                relatedContent = tvShows,
                onContentClick = onContentClick,
                title = "TV Shows",
                cardStyle = RelatedCardStyle.COMPACT
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelatedContentCard(
    content: ContentDetail,
    onClick: () -> Unit,
    cardStyle: RelatedCardStyle,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val cardSize = when (cardStyle) {
        RelatedCardStyle.STANDARD -> CardSize(width = 160.dp, height = 220.dp)
        RelatedCardStyle.COMPACT -> CardSize(width = 140.dp, height = 200.dp)
        RelatedCardStyle.LARGE -> CardSize(width = 180.dp, height = 240.dp)
    }
    
    val focusedSize = CardSize(
        width = cardSize.width + 20.dp,
        height = cardSize.height + 20.dp
    )
    
    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = onClick,
            modifier = modifier
                .size(
                    width = if (isFocused) focusedSize.width else cardSize.width,
                    height = if (isFocused) focusedSize.height else cardSize.height
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
            RelatedContentCardContent(
                content = content,
                isFocused = isFocused,
                cardStyle = cardStyle
            )
        }
    }
}

@Composable
private fun RelatedContentCardContent(
    content: ContentDetail,
    isFocused: Boolean,
    cardStyle: RelatedCardStyle
) {
    Column {
        // Content poster/thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            SmartTVImageLoader(
                imageUrl = content.cardImageUrl ?: content.backgroundImageUrl,
                contentDescription = content.title,
                priority = ImagePriority.LOW,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Content type indicator
            ContentTypeIndicator(
                contentType = content.contentType,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
            
            // Quality indicators
            QualityIndicators(
                content = content,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
            
            // Progress indicator (if applicable)
            if (content is MovieContentDetail) {
                val progress = content.getProgress()
                if (progress.hasProgress) {
                    ProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }
            }
        }
        
        // Content title and metadata
        RelatedContentInfo(
            content = content,
            isFocused = isFocused,
            cardStyle = cardStyle
        )
    }
}

@Composable
private fun RelatedContentInfo(
    content: ContentDetail,
    isFocused: Boolean,
    cardStyle: RelatedCardStyle
) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Content title
        Text(
            text = content.title,
            style = when (cardStyle) {
                RelatedCardStyle.STANDARD -> MaterialTheme.typography.titleSmall
                RelatedCardStyle.COMPACT -> MaterialTheme.typography.bodyMedium
                RelatedCardStyle.LARGE -> MaterialTheme.typography.titleMedium
            },
            color = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Content metadata
        val metadata = buildList {
            content.metadata.year?.let { add(it) }
            content.metadata.duration?.let { add(it) }
            content.metadata.rating?.let { add(it) }
        }
        
        if (metadata.isNotEmpty()) {
            Text(
                text = metadata.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContentTypeIndicator(
    contentType: ContentType,
    modifier: Modifier = Modifier
) {
    val (icon, label) = when (contentType) {
        ContentType.MOVIE -> Icons.Default.Movie to "Movie"
        ContentType.TV_SHOW -> Icons.Default.Tv to "TV Show"
        ContentType.TV_EPISODE -> Icons.Default.VideoLibrary to "Episode"
        ContentType.DOCUMENTARY -> Icons.Default.VideoLibrary to "Doc"
        ContentType.SPORTS -> Icons.Default.Sports to "Sports"
        ContentType.MUSIC_VIDEO -> Icons.Default.MusicVideo to "Music"
        ContentType.PODCAST -> Icons.Default.Podcast to "Podcast"
    }
    
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun QualityIndicators(
    content: ContentDetail,
    modifier: Modifier = Modifier
) {
    val qualityItems = buildList {
        if (content.metadata.is4K) add("4K")
        if (content.metadata.isHDR) add("HDR")
        content.metadata.quality?.let { quality ->
            if (quality !in listOf("4K", "HDR")) add(quality)
        }
    }
    
    if (qualityItems.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            qualityItems.forEach { quality ->
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = quality,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressIndicator(
    progress: ContentProgress,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = { progress.watchPercentage },
        modifier = modifier.height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.White.copy(alpha = 0.3f)
    )
}

/**
 * Empty state for when no related content is available
 */
@Composable
fun RelatedContentEmptyState(
    modifier: Modifier = Modifier,
    message: String = "No related content available"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Loading state for related content
 */
@Composable
fun RelatedContentLoadingState(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(itemCount) {
            RelatedContentLoadingCard()
        }
    }
}

@Composable
private fun RelatedContentLoadingCard() {
    Card(
        modifier = Modifier.size(width = 160.dp, height = 220.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Card styles for different use cases
 */
enum class RelatedCardStyle {
    STANDARD,
    COMPACT,
    LARGE
}

private data class CardSize(
    val width: androidx.compose.ui.unit.Dp,
    val height: androidx.compose.ui.unit.Dp
)

/**
 * Preview/Demo configurations for RelatedSection
 */
object RelatedSectionPreview {
    fun createSampleRelatedContent(): List<ContentDetail> {
        return listOf(
            createSampleContent("1", "Sample Movie 1", ContentType.MOVIE),
            createSampleContent("2", "TV Show Example", ContentType.TV_SHOW),
            createSampleContent("3", "Documentary Film", ContentType.DOCUMENTARY),
            createSampleContent("4", "Another Movie", ContentType.MOVIE),
            createSampleContent("5", "Sports Special", ContentType.SPORTS),
            createSampleContent("6", "Music Video", ContentType.MUSIC_VIDEO)
        )
    }
    
    private fun createSampleContent(
        id: String,
        title: String,
        contentType: ContentType
    ): ContentDetail {
        return object : ContentDetail {
            override val id: String = id
            override val title: String = title
            override val description: String? = "Sample description for $title"
            override val backgroundImageUrl: String? = "https://example.com/backdrop$id.jpg"
            override val cardImageUrl: String? = "https://example.com/poster$id.jpg"
            override val contentType: ContentType = contentType
            override val videoUrl: String? = "https://example.com/video$id.mp4"
            override val metadata: ContentMetadata = ContentMetadata(
                year = "2023",
                duration = if (contentType == ContentType.TV_EPISODE) "45m" else "2h 15m",
                rating = "PG-13",
                quality = if (id.toInt() % 2 == 0) "4K" else "HD",
                is4K = id.toInt() % 2 == 0,
                isHDR = id.toInt() % 3 == 0
            )
            override val actions: List<ContentAction> = emptyList()
        }
    }
}