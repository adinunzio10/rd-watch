package com.rdwatch.androidtv.ui.details.models

/**
 * Abstract representation of content that can be displayed in a detail screen
 * Supports movies, TV shows, episodes, and other content types
 */
interface ContentDetail {
    val id: String
    val title: String
    val description: String?
    val backgroundImageUrl: String?
    val cardImageUrl: String?
    val contentType: ContentType
    val metadata: ContentMetadata
    val actions: List<ContentAction>
    val videoUrl: String?
    val sources: List<StreamingSource>
        get() = emptyList() // Default implementation for backward compatibility

    /**
     * Get display title for the content
     */
    fun getDisplayTitle(): String = title

    /**
     * Get formatted description or fallback text
     */
    fun getDisplayDescription(): String = description ?: "No description available"

    /**
     * Get primary image URL (background or card)
     */
    fun getPrimaryImageUrl(): String? = backgroundImageUrl ?: cardImageUrl

    /**
     * Check if content is playable
     */
    fun isPlayable(): Boolean = videoUrl != null || sources.isNotEmpty()

    /**
     * Get content-specific metadata chips
     */
    fun getMetadataChips(): List<MetadataChip> = metadata.toChips()

    /**
     * Get available streaming sources
     */
    fun getAvailableSources(): List<StreamingSource> = sources.filter { it.isCurrentlyAvailable() }

    /**
     * Get the best quality source (if available)
     */
    fun getBestQualitySource(): StreamingSource? = getAvailableSources().maxByOrNull { it.quality.priority }

    /**
     * Get reliable streaming sources
     */
    fun getReliableSources(): List<StreamingSource> = getAvailableSources().filter { it.isReliable() }

    /**
     * Get P2P streaming sources
     */
    fun getP2PSources(): List<StreamingSource> = getAvailableSources().filter { it.isP2P() }

    /**
     * Check if content has reliable streaming options
     */
    fun hasReliableStreaming(): Boolean = getReliableSources().isNotEmpty()

    /**
     * Check if content has P2P streaming options
     */
    fun hasP2PStreaming(): Boolean = getP2PSources().isNotEmpty()
}

/**
 * Types of content that can be displayed in detail screens
 */
enum class ContentType {
    MOVIE,
    TV_SHOW,
    TV_EPISODE,
    DOCUMENTARY,
    SPORTS,
    MUSIC_VIDEO,
    PODCAST,
}

/**
 * Content metadata that varies by content type
 */
data class ContentMetadata(
    val year: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val language: String? = null,
    val genre: List<String> = emptyList(),
    val studio: String? = null,
    val cast: List<String> = emptyList(),
    val director: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val quality: String? = null,
    val isHDR: Boolean = false,
    val is4K: Boolean = false,
    val customMetadata: Map<String, String> = emptyMap(),
) {
    /**
     * Convert metadata to display chips
     */
    fun toChips(): List<MetadataChip> {
        val chips = mutableListOf<MetadataChip>()

        quality?.let { chips.add(MetadataChip.Quality(it)) }
        if (is4K) chips.add(MetadataChip.Quality("4K"))
        if (isHDR) chips.add(MetadataChip.Quality("HDR"))
        year?.let { chips.add(MetadataChip.Year(it)) }
        rating?.let { chips.add(MetadataChip.Rating(it)) }
        duration?.let { chips.add(MetadataChip.Duration(it)) }
        studio?.let { chips.add(MetadataChip.Studio(it)) }

        return chips
    }
}

/**
 * Metadata chips for display in the UI
 */
sealed class MetadataChip(val text: String, val icon: String? = null) {
    class Quality(quality: String) : MetadataChip(quality)

    class Year(year: String) : MetadataChip(year)

    class Rating(rating: String) : MetadataChip(rating)

    class Duration(duration: String) : MetadataChip(duration)

    class Studio(studio: String) : MetadataChip(studio)

    class Genre(genre: String) : MetadataChip(genre)

    class Language(language: String) : MetadataChip(language)

    class Custom(text: String, icon: String? = null) : MetadataChip(text, icon)
}

/**
 * Actions that can be performed on content
 */
sealed class ContentAction(val title: String, val icon: String) {
    class Play(val isResume: Boolean = false) : ContentAction(
        title = if (isResume) "Resume" else "Play",
        icon = "play_arrow",
    )

    class AddToWatchlist(val isInWatchlist: Boolean = false) : ContentAction(
        title = if (isInWatchlist) "Remove from Watchlist" else "Add to Watchlist",
        icon = if (isInWatchlist) "remove" else "add",
    )

    class Like(val isLiked: Boolean = false) : ContentAction(
        title = if (isLiked) "Unlike" else "Like",
        icon = if (isLiked) "favorite" else "thumb_up",
    )

    class Share : ContentAction("Share", "share")

    class Download(val isDownloaded: Boolean = false, val isDownloading: Boolean = false) : ContentAction(
        title =
            when {
                isDownloaded -> "Downloaded"
                isDownloading -> "Downloading..."
                else -> "Download"
            },
        icon =
            when {
                isDownloaded -> "cloud_done"
                else -> "download"
            },
    )

    class Delete : ContentAction("Delete", "delete")

    class Custom(title: String, icon: String, val action: () -> Unit) : ContentAction(title, icon)
}

/**
 * Progress information for content playback
 */
data class ContentProgress(
    val watchPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    val resumePosition: Long = 0L,
    val totalDuration: Long = 0L,
) {
    val hasProgress: Boolean get() = watchPercentage > 0f
    val isPartiallyWatched: Boolean get() = hasProgress && !isCompleted

    fun getProgressText(): String {
        return if (isCompleted) {
            "Watched"
        } else if (hasProgress) {
            "${(watchPercentage * 100).toInt()}% watched"
        } else {
            ""
        }
    }
}

/**
 * Layout configuration for different content types
 */
data class DetailLayoutConfig(
    val showHeroSection: Boolean = true,
    val showInfoSection: Boolean = true,
    val showActionButtons: Boolean = true,
    val showRelatedContent: Boolean = true,
    val showSeasonEpisodeGrid: Boolean = false,
    val showCastCrew: Boolean = false,
    val customSections: List<String> = emptyList(),
    val overscanMargin: Int = 32,
) {
    companion object {
        fun forContentType(contentType: ContentType): DetailLayoutConfig {
            return when (contentType) {
                ContentType.MOVIE ->
                    DetailLayoutConfig(
                        showCastCrew = true,
                    )
                ContentType.TV_SHOW ->
                    DetailLayoutConfig(
                        showSeasonEpisodeGrid = true,
                        showCastCrew = true,
                    )
                ContentType.TV_EPISODE ->
                    DetailLayoutConfig(
                        showSeasonEpisodeGrid = false,
                        showCastCrew = true,
                    )
                ContentType.DOCUMENTARY ->
                    DetailLayoutConfig(
                        showCastCrew = true,
                    )
                ContentType.SPORTS ->
                    DetailLayoutConfig(
                        showCastCrew = false,
                        showRelatedContent = true,
                    )
                ContentType.MUSIC_VIDEO ->
                    DetailLayoutConfig(
                        showCastCrew = false,
                        showRelatedContent = true,
                    )
                ContentType.PODCAST ->
                    DetailLayoutConfig(
                        showCastCrew = false,
                        showRelatedContent = true,
                    )
            }
        }
    }
}
