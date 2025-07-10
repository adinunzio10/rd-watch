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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.details.models.*

/**
 * Info section component for content detail screens
 * Displays description, metadata, genres, and other detailed information
 */
@Composable
fun InfoSection(
    content: ContentDetail,
    modifier: Modifier = Modifier,
    maxDescriptionLines: Int = 4,
    showExpandableDescription: Boolean = false,
    tabMode: InfoSectionTabMode = InfoSectionTabMode.FULL
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (tabMode) {
            InfoSectionTabMode.OVERVIEW -> {
                // Overview: Description summary + key metadata + genres
                if (content.description != null) {
                    InfoDescriptionSection(
                        description = content.description!!,
                        maxLines = 3,
                        showExpandable = false
                    )
                }
                
                // Key metadata only
                InfoMetadataGrid(content = content, isOverview = true)
                
                // Genres
                if (content.metadata.genre.isNotEmpty()) {
                    InfoGenresSection(genres = content.metadata.genre.take(4))
                }
            }
            
            InfoSectionTabMode.DETAILS -> {
                // Details: Full description + complete metadata + cast
                if (content.description != null) {
                    InfoDescriptionSection(
                        description = content.description!!,
                        maxLines = maxDescriptionLines,
                        showExpandable = showExpandableDescription
                    )
                }
                
                // Complete metadata grid
                InfoMetadataGrid(content = content, isOverview = false)
                
                // All genres
                if (content.metadata.genre.isNotEmpty()) {
                    InfoGenresSection(genres = content.metadata.genre)
                }
                
                // Cast & crew
                if (content.metadata.cast.isNotEmpty()) {
                    InfoCastSection(cast = content.metadata.cast)
                }
                
                // Quality indicators
                InfoQualitySection(content = content)
            }
            
            InfoSectionTabMode.FULL -> {
                // Full mode (original behavior)
                if (content.description != null) {
                    InfoDescriptionSection(
                        description = content.description!!,
                        maxLines = maxDescriptionLines,
                        showExpandable = showExpandableDescription
                    )
                }
                
                InfoMetadataGrid(content = content)
                
                if (content.metadata.genre.isNotEmpty()) {
                    InfoGenresSection(genres = content.metadata.genre)
                }
                
                if (content.metadata.cast.isNotEmpty()) {
                    InfoCastSection(cast = content.metadata.cast)
                }
            }
        }
    }
}

@Composable
private fun InfoDescriptionSection(
    description: String,
    maxLines: Int,
    showExpandable: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Description",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2,
            maxLines = if (showExpandable && isExpanded) Int.MAX_VALUE else maxLines
        )
        
        if (showExpandable && description.length > 200) {
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.padding(0.dp)
            ) {
                Text(
                    text = if (isExpanded) "Show Less" else "Show More",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InfoMetadataGrid(content: ContentDetail, isOverview: Boolean = false) {
    val metadataItems = buildList {
        if (isOverview) {
            // Overview: Show only key metadata
            content.metadata.year?.let { add(InfoMetadataItem("Year", it)) }
            content.metadata.duration?.let { add(InfoMetadataItem("Duration", it)) }
            content.metadata.rating?.let { add(InfoMetadataItem("Rating", it)) }
            
            // Add season/episode info for TV content
            if (content.contentType == ContentType.TV_EPISODE) {
                content.metadata.season?.let { season ->
                    content.metadata.episode?.let { episode ->
                        add(InfoMetadataItem("Episode", "S${season}E${episode}"))
                    }
                }
            }
        } else {
            // Details: Show all available metadata
            content.metadata.duration?.let { add(InfoMetadataItem("Duration", it)) }
            content.metadata.language?.let { add(InfoMetadataItem("Language", it)) }
            content.metadata.rating?.let { add(InfoMetadataItem("Rating", it)) }
            content.metadata.year?.let { add(InfoMetadataItem("Year", it)) }
            content.metadata.director?.let { add(InfoMetadataItem("Director", it)) }
            content.metadata.studio?.let { add(InfoMetadataItem("Studio", it)) }
            
            // Add season/episode info for TV content
            if (content.contentType == ContentType.TV_EPISODE) {
                content.metadata.season?.let { season ->
                    content.metadata.episode?.let { episode ->
                        add(InfoMetadataItem("Episode", "S${season}E${episode}"))
                    }
                }
            }
        }
    }
    
    if (metadataItems.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(metadataItems) { item ->
                InfoMetadataItemCard(item = item)
            }
        }
    }
}

@Composable
private fun InfoMetadataItemCard(item: InfoMetadataItem) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            text = item.value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoGenresSection(genres: List<String>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Genres",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(genres) { genre ->
                InfoGenreChip(genre = genre)
            }
        }
    }
}

@Composable
private fun InfoGenreChip(genre: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun InfoCastSection(cast: List<String>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Cast",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(cast.take(8)) { castMember ->
                InfoCastMemberCard(name = castMember)
            }
        }
    }
}

@Composable
private fun InfoCastMemberCard(name: String) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Placeholder for cast member image
            Surface(
                modifier = Modifier
                    .size(80.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}

/**
 * Quality indicators section for video content
 */
@Composable
fun InfoQualitySection(
    content: ContentDetail,
    modifier: Modifier = Modifier
) {
    val qualityItems = buildList {
        content.metadata.quality?.let { add(it) }
        if (content.metadata.is4K) add("4K")
        if (content.metadata.isHDR) add("HDR")
    }
    
    if (qualityItems.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quality",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(qualityItems) { quality ->
                    InfoQualityBadge(quality = quality)
                }
            }
        }
    }
}

@Composable
private fun InfoQualityBadge(quality: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (quality) {
                    "4K" -> Icons.Default.HighQuality
                    "HDR" -> Icons.Default.HighQuality
                    else -> Icons.Default.VideoLabel
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = quality,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Data class for metadata items
 */
private data class InfoMetadataItem(
    val label: String,
    val value: String
)

/**
 * Tab modes for InfoSection content filtering
 */
enum class InfoSectionTabMode {
    OVERVIEW,   // Essential information only
    DETAILS,    // Complete detailed information
    FULL        // Original full behavior (backward compatibility)
}

/**
 * Preview/Demo configurations for InfoSection
 */
object InfoSectionPreview {
    fun createSampleMovieContent(): ContentDetail {
        return object : ContentDetail {
            override val id: String = "1"
            override val title: String = "Sample Movie"
            override val description: String? = "This is a longer description that demonstrates how the info section handles multi-line text. It includes details about the plot, characters, and what makes this content interesting. The description can be quite long and might need to be truncated with a show more/less functionality."
            override val backgroundImageUrl: String? = null
            override val cardImageUrl: String? = null
            override val contentType: ContentType = ContentType.MOVIE
            override val videoUrl: String? = "https://example.com/video.mp4"
            override val metadata: ContentMetadata = ContentMetadata(
                year = "2023",
                duration = "2h 15m",
                rating = "PG-13",
                language = "English",
                genre = listOf("Action", "Adventure", "Drama", "Thriller"),
                studio = "Studio Example",
                cast = listOf("Actor One", "Actor Two", "Actor Three", "Actor Four", "Actor Five"),
                director = "Director Name",
                quality = "4K",
                is4K = true,
                isHDR = true
            )
            override val actions: List<ContentAction> = emptyList()
        }
    }
    
    fun createSampleTVEpisodeContent(): ContentDetail {
        return object : ContentDetail {
            override val id: String = "2"
            override val title: String = "Episode Title"
            override val description: String? = "A TV episode description that shows how the info section adapts to different content types."
            override val backgroundImageUrl: String? = null
            override val cardImageUrl: String? = null
            override val contentType: ContentType = ContentType.TV_EPISODE
            override val videoUrl: String? = "https://example.com/episode.mp4"
            override val metadata: ContentMetadata = ContentMetadata(
                year = "2023",
                duration = "45m",
                rating = "TV-14",
                language = "English",
                genre = listOf("Drama", "Mystery"),
                studio = "TV Network",
                cast = listOf("Lead Actor", "Supporting Actor"),
                director = "Episode Director",
                season = 1,
                episode = 5
            )
            override val actions: List<ContentAction> = emptyList()
        }
    }
}