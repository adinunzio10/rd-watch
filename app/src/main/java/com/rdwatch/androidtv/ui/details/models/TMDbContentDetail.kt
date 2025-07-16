package com.rdwatch.androidtv.ui.details.models

/**
 * TMDb-specific ContentDetail implementation for movies
 * Provides movie-specific metadata and actions
 */
data class TMDbMovieContentDetail(
    override val id: String,
    val tmdbId: Int,
    override val title: String,
    val originalTitle: String,
    override val description: String?,
    override val backgroundImageUrl: String?,
    override val cardImageUrl: String?,
    override val videoUrl: String?,
    
    // Movie-specific fields
    val releaseDate: String?,
    val voteAverage: Float,
    val voteCount: Int,
    val popularity: Float,
    val adult: Boolean,
    val originalLanguage: String,
    val genres: List<String>,
    val runtime: Int?,
    val budget: Long,
    val revenue: Long,
    val status: String,
    val tagline: String?,
    val homepage: String?,
    val imdbId: String?,
    val productionCompanies: List<String>,
    val productionCountries: List<String>,
    val spokenLanguages: List<String>
) : ContentDetail {
    
    override val contentType: ContentType = ContentType.MOVIE
    
    override val metadata: ContentMetadata by lazy {
        ContentMetadata(
            year = releaseDate?.take(4),
            duration = runtime?.let { formatRuntime(it) },
            rating = if (voteAverage > 0) "${"%.1f".format(voteAverage)}/10" else null,
            language = spokenLanguages.firstOrNull(),
            genre = genres,
            studio = productionCompanies.firstOrNull(),
            cast = emptyList(), // Would be populated from credits
            director = null, // Would be populated from credits
            quality = null, // Not available from TMDb
            isHDR = false,
            is4K = false,
            customMetadata = buildCustomMetadata()
        )
    }
    
    override val actions: List<ContentAction> by lazy {
        buildList {
            // Play action (would need to be integrated with streaming sources)
            add(ContentAction.Play())
            
            // Add to watchlist action
            add(ContentAction.AddToWatchlist())
            
            // Like action
            add(ContentAction.Like())
            
            // Share action
            add(ContentAction.Share())
            
            // Custom TMDb actions
            homepage?.let {
                add(ContentAction.Custom("Visit Homepage", "open_in_browser") { 
                    // Would open homepage URL
                })
            }
            
            imdbId?.let {
                add(ContentAction.Custom("View on IMDb", "movie") { 
                    // Would open IMDb page
                })
            }
        }
    }
    
    override fun getDisplayTitle(): String {
        return if (originalTitle != title && originalTitle.isNotBlank()) {
            "$title ($originalTitle)"
        } else {
            title
        }
    }
    
    override fun getDisplayDescription(): String {
        return when {
            !description.isNullOrBlank() -> description
            !tagline.isNullOrBlank() -> tagline
            else -> "No description available"
        }
    }
    
    private fun formatRuntime(runtime: Int): String {
        return if (runtime < 60) {
            "${runtime}m"
        } else {
            val hours = runtime / 60
            val minutes = runtime % 60
            if (minutes == 0) {
                "${hours}h"
            } else {
                "${hours}h ${minutes}m"
            }
        }
    }
    
    private fun buildCustomMetadata(): Map<String, String> {
        return buildMap {
            put("tmdb_id", tmdbId.toString())
            put("vote_count", voteCount.toString())
            put("popularity", popularity.toString())
            put("original_language", originalLanguage)
            put("status", status)
            if (budget > 0) put("budget", "$${budget}")
            if (revenue > 0) put("revenue", "$${revenue}")
            imdbId?.let { put("imdb_id", it) }
            homepage?.let { put("homepage", it) }
            productionCountries.firstOrNull()?.let { put("country", it) }
        }
    }
}

/**
 * TMDb-specific ContentDetail implementation for TV shows
 * Provides TV show-specific metadata and actions
 */
data class TMDbTVContentDetail(
    override val id: String,
    val tmdbId: Int,
    override val title: String,
    val originalTitle: String,
    override val description: String?,
    override val backgroundImageUrl: String?,
    override val cardImageUrl: String?,
    override val videoUrl: String?,
    
    // TV show-specific fields
    val firstAirDate: String?,
    val lastAirDate: String?,
    val voteAverage: Float,
    val voteCount: Int,
    val popularity: Float,
    val adult: Boolean,
    val originalLanguage: String,
    val genres: List<String>,
    val numberOfEpisodes: Int?,
    val numberOfSeasons: Int?,
    val status: String?,
    val type: String?,
    val homepage: String?,
    val inProduction: Boolean?,
    val imdbId: String?,
    val networks: List<String>,
    val originCountry: List<String>,
    val productionCompanies: List<String>,
    val productionCountries: List<String>,
    val spokenLanguages: List<String>
) : ContentDetail {
    
    override val contentType: ContentType = ContentType.TV_SHOW
    
    override val metadata: ContentMetadata by lazy {
        ContentMetadata(
            year = firstAirDate?.take(4),
            duration = null, // TV shows don't have a single duration
            rating = if (voteAverage > 0) "${"%.1f".format(voteAverage)}/10" else null,
            language = spokenLanguages.firstOrNull(),
            genre = genres,
            studio = networks.firstOrNull() ?: productionCompanies.firstOrNull(),
            cast = emptyList(), // Would be populated from credits
            director = null, // Would be populated from credits
            quality = null, // Not available from TMDb
            isHDR = false,
            is4K = false,
            customMetadata = buildCustomMetadata()
        )
    }
    
    override val actions: List<ContentAction> by lazy {
        buildList {
            // Play action (would need to be integrated with streaming sources)
            add(ContentAction.Play())
            
            // Add to watchlist action
            add(ContentAction.AddToWatchlist())
            
            // Like action
            add(ContentAction.Like())
            
            // Share action
            add(ContentAction.Share())
            
            // Custom TMDb actions
            homepage?.let {
                add(ContentAction.Custom("Visit Homepage", "open_in_browser") { 
                    // Would open homepage URL
                })
            }
            
            imdbId?.let {
                add(ContentAction.Custom("View on IMDb", "tv") { 
                    // Would open IMDb page
                })
            }
        }
    }
    
    override fun getDisplayTitle(): String {
        return if (originalTitle != title && originalTitle.isNotBlank()) {
            "$title ($originalTitle)"
        } else {
            title
        }
    }
    
    override fun getDisplayDescription(): String {
        return description ?: "No description available"
    }
    
    private fun buildCustomMetadata(): Map<String, String> {
        return buildMap {
            put("tmdb_id", tmdbId.toString())
            put("vote_count", voteCount.toString())
            put("popularity", popularity.toString())
            put("original_language", originalLanguage)
            status?.let { put("status", it) }
            type?.let { put("type", it) }
            numberOfSeasons?.let { put("seasons", it.toString()) }
            numberOfEpisodes?.let { put("episodes", it.toString()) }
            inProduction?.let { put("in_production", it.toString()) }
            homepage?.let { put("homepage", it) }
            imdbId?.let { put("imdb_id", it) }
            originCountry.firstOrNull()?.let { put("country", it) }
            if (networks.isNotEmpty()) put("networks", networks.joinToString(", "))
            if (lastAirDate != null) put("last_air_date", lastAirDate)
        }
    }
}

/**
 * TMDb-specific ContentDetail implementation for episodes
 * Provides episode-specific metadata and actions
 */
data class TMDbEpisodeContentDetail(
    override val id: String,
    val tmdbId: Int,
    val showId: Int,
    override val title: String,
    val showTitle: String,
    override val description: String?,
    override val backgroundImageUrl: String?,
    override val cardImageUrl: String?,
    override val videoUrl: String?,
    
    // Episode-specific fields
    val episodeNumber: Int,
    val seasonNumber: Int,
    val airDate: String?,
    val voteAverage: Float,
    val voteCount: Int,
    val runtime: Int?,
    val stillPath: String?,
    val productionCode: String?
) : ContentDetail {
    
    override val contentType: ContentType = ContentType.TV_EPISODE
    
    override val metadata: ContentMetadata by lazy {
        ContentMetadata(
            year = airDate?.take(4),
            duration = runtime?.let { "${it}m" },
            rating = if (voteAverage > 0) "${"%.1f".format(voteAverage)}/10" else null,
            season = seasonNumber,
            episode = episodeNumber,
            quality = null,
            isHDR = false,
            is4K = false,
            customMetadata = buildCustomMetadata()
        )
    }
    
    override val actions: List<ContentAction> by lazy {
        buildList {
            // Play action
            add(ContentAction.Play())
            
            // Add to watchlist action
            add(ContentAction.AddToWatchlist())
            
            // Like action
            add(ContentAction.Like())
            
            // Share action
            add(ContentAction.Share())
        }
    }
    
    override fun getDisplayTitle(): String {
        return "$showTitle - S${seasonNumber}E${episodeNumber}: $title"
    }
    
    override fun getDisplayDescription(): String {
        return description ?: "No description available"
    }
    
    private fun buildCustomMetadata(): Map<String, String> {
        return buildMap {
            put("tmdb_id", tmdbId.toString())
            put("show_id", showId.toString())
            put("season_number", seasonNumber.toString())
            put("episode_number", episodeNumber.toString())
            put("vote_count", voteCount.toString())
            airDate?.let { put("air_date", it) }
            productionCode?.let { put("production_code", it) }
        }
    }
}