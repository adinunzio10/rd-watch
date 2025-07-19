package com.rdwatch.androidtv.ui.details.models

import com.rdwatch.androidtv.Movie

/**
 * Movie-specific implementation of ContentDetail
 * Adapts the existing Movie data model to the ContentDetail interface
 */
data class MovieContentDetail(
    private val movie: Movie,
    private val progress: ContentProgress = ContentProgress(),
    private val isInWatchlist: Boolean = false,
    private val isLiked: Boolean = false,
    private val isDownloaded: Boolean = false,
    private val isDownloading: Boolean = false,
    private val isFromRealDebrid: Boolean = false,
    private val additionalMetadata: ContentMetadata = ContentMetadata(),
) : ContentDetail {
    override val id: String = movie.id.toString()
    override val title: String = movie.title ?: "Unknown Title"
    override val description: String? = movie.description
    override val backgroundImageUrl: String? = movie.backgroundImageUrl
    override val cardImageUrl: String? = movie.cardImageUrl
    override val contentType: ContentType = ContentType.MOVIE
    override val videoUrl: String? = movie.videoUrl

    override val metadata: ContentMetadata =
        ContentMetadata(
            year = additionalMetadata.year ?: "2023", // TODO: Get from movie metadata
            duration = additionalMetadata.duration ?: "2h 15m", // TODO: Get from movie metadata
            rating = additionalMetadata.rating ?: "PG-13", // TODO: Get from movie metadata
            language = additionalMetadata.language ?: "English", // TODO: Get from movie metadata
            genre = additionalMetadata.genre.ifEmpty { listOf("Drama") }, // TODO: Get from movie metadata
            studio = movie.studio,
            quality = additionalMetadata.quality ?: "HD",
            is4K = additionalMetadata.is4K,
            isHDR = additionalMetadata.isHDR,
            customMetadata = additionalMetadata.customMetadata,
        )

    override val actions: List<ContentAction> =
        buildList {
            // Play action
            add(ContentAction.Play(isResume = progress.isPartiallyWatched))

            // Watchlist action
            add(ContentAction.AddToWatchlist(isInWatchlist))

            // Like action
            add(ContentAction.Like(isLiked))

            // Share action
            add(ContentAction.Share())

            // Download action
            add(ContentAction.Download(isDownloaded, isDownloading))

            // Delete action (only for Real-Debrid content)
            if (isFromRealDebrid) {
                add(ContentAction.Delete())
            }
        }

    /**
     * Get the underlying Movie object
     */
    fun getMovie(): Movie = movie

    /**
     * Get content progress information
     */
    fun getProgress(): ContentProgress = progress

    /**
     * Check if this is Real-Debrid content
     */
    fun isRealDebridContent(): Boolean = isFromRealDebrid

    /**
     * Create a copy with updated progress
     */
    fun withProgress(newProgress: ContentProgress): MovieContentDetail {
        return copy(progress = newProgress)
    }

    /**
     * Create a copy with updated watchlist status
     */
    fun withWatchlistStatus(inWatchlist: Boolean): MovieContentDetail {
        return copy(isInWatchlist = inWatchlist)
    }

    /**
     * Create a copy with updated like status
     */
    fun withLikeStatus(liked: Boolean): MovieContentDetail {
        return copy(isLiked = liked)
    }

    /**
     * Create a copy with updated download status
     */
    fun withDownloadStatus(
        downloaded: Boolean,
        downloading: Boolean = false,
    ): MovieContentDetail {
        return copy(isDownloaded = downloaded, isDownloading = downloading)
    }

    /**
     * Create a copy with additional metadata
     */
    fun withMetadata(metadata: ContentMetadata): MovieContentDetail {
        return copy(additionalMetadata = metadata)
    }

    companion object {
        /**
         * Create MovieContentDetail from Movie and UI state
         */
        fun fromMovie(
            movie: Movie,
            progress: ContentProgress = ContentProgress(),
            isInWatchlist: Boolean = false,
            isLiked: Boolean = false,
            isDownloaded: Boolean = false,
            isDownloading: Boolean = false,
            isFromRealDebrid: Boolean = false,
            metadata: ContentMetadata = ContentMetadata(),
        ): MovieContentDetail {
            return MovieContentDetail(
                movie = movie,
                progress = progress,
                isInWatchlist = isInWatchlist,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                isFromRealDebrid = isFromRealDebrid,
                additionalMetadata = metadata,
            )
        }
    }
}
