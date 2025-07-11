package com.rdwatch.androidtv.ui.navigation

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.ui.details.models.ContentType

/**
 * Utility class to detect content type from Movie objects
 * This bridges the gap between the legacy Movie class and the new ContentDetail system
 */
object ContentTypeDetector {
    
    /**
     * Detects content type from a Movie object
     * Uses heuristics based on movie properties to determine if it's a TV show or movie
     */
    fun detectContentType(movie: Movie): ContentType {
        // Since Movie objects are converted from ContentDetail, we can use some heuristics
        // to determine the content type based on the properties
        
        // Check if the title contains TV show indicators
        val title = movie.title?.lowercase() ?: ""
        
        // Common TV show patterns
        val tvShowPatterns = listOf(
            "season", "episode", "series", "tv", "show",
            "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9",
            "e0", "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9"
        )
        
        // Check for TV show patterns in title
        if (tvShowPatterns.any { pattern -> title.contains(pattern) }) {
            return ContentType.TV_SHOW
        }
        
        // Check description for TV show indicators
        val description = movie.description?.lowercase() ?: ""
        val tvDescriptionPatterns = listOf(
            "season", "episode", "series", "episodes", "aired", "seasons",
            "tv series", "television", "drama series", "comedy series"
        )
        
        if (tvDescriptionPatterns.any { pattern -> description.contains(pattern) }) {
            return ContentType.TV_SHOW
        }
        
        // Check studio for TV networks
        val studio = movie.studio?.lowercase() ?: ""
        val tvNetworks = listOf(
            "netflix", "hbo", "amazon prime", "disney+", "hulu", "abc", "cbs", "nbc", "fox",
            "cw", "fx", "amc", "showtime", "starz", "apple tv+", "paramount+", "peacock"
        )
        
        if (tvNetworks.any { network -> studio.contains(network) }) {
            return ContentType.TV_SHOW
        }
        
        // Default to movie if no TV show indicators found
        return ContentType.MOVIE
    }
    
    /**
     * Detects content type from movie ID and title
     * Alternative method when only basic information is available
     */
    fun detectContentType(movieId: String, title: String?): ContentType {
        // Create a temporary Movie object to use the main detection method
        val tempMovie = Movie(
            id = movieId.hashCode().toLong(),
            title = title
        )
        return detectContentType(tempMovie)
    }
}