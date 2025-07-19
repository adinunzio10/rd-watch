package com.rdwatch.androidtv.data.mappers

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.data.entities.ContentEntity
import com.rdwatch.androidtv.data.entities.ContentSource

/**
 * Mapper to convert ContentEntity (Real-Debrid data) to Movie (UI model)
 * This bridges the gap between the repository layer and UI layer
 */
object ContentEntityToMovieMapper {
    /**
     * Convert a single ContentEntity to Movie
     */
    fun ContentEntity.toMovie(): Movie {
        return Movie(
            id = id,
            title = buildDisplayTitle(),
            description = buildDisplayDescription(),
            backgroundImageUrl = backdropUrl ?: generatePlaceholderBackdrop(),
            cardImageUrl = posterUrl ?: generatePlaceholderPoster(),
            videoUrl = generateVideoUrl(),
            studio = buildStudioInfo(),
        )
    }

    /**
     * Convert a list of ContentEntity to List<Movie>
     */
    fun List<ContentEntity>.toMovies(): List<Movie> {
        return map { it.toMovie() }
    }

    /**
     * Build display title including year and quality if available
     */
    private fun ContentEntity.buildDisplayTitle(): String {
        val titleParts = mutableListOf(title)

        year?.let { titleParts.add("($it)") }
        quality?.let { titleParts.add("[$it]") }

        return titleParts.joinToString(" ")
    }

    /**
     * Build comprehensive description combining available metadata
     */
    private fun ContentEntity.buildDisplayDescription(): String {
        val parts = mutableListOf<String>()

        // Original description first
        description?.let { parts.add(it) }

        // Add metadata info
        val metadata = mutableListOf<String>()

        if (source == ContentSource.REAL_DEBRID) {
            metadata.add("Source: Real-Debrid")
        }

        director?.let { metadata.add("Director: $it") }

        genres?.takeIf { it.isNotEmpty() }?.let {
            metadata.add("Genres: ${it.joinToString(", ")}")
        }

        cast?.takeIf { it.isNotEmpty() }?.take(3)?.let {
            metadata.add("Cast: ${it.joinToString(", ")}")
        }

        duration?.let { metadata.add("Duration: ${it}min") }

        rating?.let { metadata.add("Rating: ${"%.1f".format(it)}/10") }

        if (metadata.isNotEmpty()) {
            parts.add(metadata.joinToString(" • "))
        }

        return parts.joinToString("\n\n").ifBlank {
            "No description available."
        }
    }

    /**
     * Generate video URL for playback
     * For Real-Debrid content, this would be the unrestricted link
     * For now, we'll use a placeholder that can be resolved by the video player
     */
    private fun ContentEntity.generateVideoUrl(): String {
        return when (source) {
            ContentSource.REAL_DEBRID -> {
                // Use Real-Debrid ID to construct a URL that can be resolved later
                realDebridId?.let { "rd://$it" } ?: ""
            }
            ContentSource.LOCAL -> {
                // For local content, return actual file path if available
                // This would be stored in a different field in a real implementation
                ""
            }
        }
    }

    /**
     * Build studio information including source and quality
     */
    private fun ContentEntity.buildStudioInfo(): String {
        val parts = mutableListOf<String>()

        when (source) {
            ContentSource.REAL_DEBRID -> parts.add("RealDebrid")
            ContentSource.LOCAL -> parts.add("Local")
        }

        quality?.let { parts.add(it) }

        return parts.joinToString(" • ")
    }

    /**
     * Generate placeholder poster URL based on title
     * In a real implementation, this could fetch from TMDB or other sources
     */
    private fun ContentEntity.generatePlaceholderPoster(): String {
        // Generate a placeholder image URL using a service like placeholders.dev
        val sanitizedTitle = title.replace(" ", "%20")
        return "https://via.placeholder.com/300x450/1a1a1a/ffffff?text=$sanitizedTitle"
    }

    /**
     * Generate placeholder backdrop URL based on title
     */
    private fun ContentEntity.generatePlaceholderBackdrop(): String {
        // Generate a placeholder backdrop image
        val sanitizedTitle = title.replace(" ", "%20")
        return "https://via.placeholder.com/1920x1080/2a2a2a/ffffff?text=$sanitizedTitle"
    }

    /**
     * Helper method to find movie by Real-Debrid ID
     */
    fun List<ContentEntity>.findByRealDebridId(realDebridId: String): Movie? {
        return find { it.realDebridId == realDebridId }?.toMovie()
    }

    /**
     * Helper method to find movie by ID (for compatibility with existing navigation)
     */
    fun List<ContentEntity>.findMovieById(movieId: Long): Movie? {
        return find { it.id == movieId }?.toMovie()
    }
}
