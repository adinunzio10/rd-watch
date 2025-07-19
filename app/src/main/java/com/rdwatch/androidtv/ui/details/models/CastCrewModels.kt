package com.rdwatch.androidtv.ui.details.models

/**
 * Enhanced data model for cast members with profile images and character information
 */
data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profileImageUrl: String? = null,
    val order: Int = 0,
) {
    companion object {
        private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        private const val PROFILE_IMAGE_SIZE = "w185" // Optimal size for TV viewing

        /**
         * Build full profile image URL from TMDb path
         */
        fun buildProfileImageUrl(profilePath: String?): String? {
            return profilePath?.let { "$TMDB_IMAGE_BASE_URL$PROFILE_IMAGE_SIZE$it" }
        }
    }
}

/**
 * Enhanced data model for crew members with profile images and job information
 */
data class CrewMember(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
    val profileImageUrl: String? = null,
) {
    companion object {
        private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        private const val PROFILE_IMAGE_SIZE = "w185" // Optimal size for TV viewing

        /**
         * Build full profile image URL from TMDb path
         */
        fun buildProfileImageUrl(profilePath: String?): String? {
            return profilePath?.let { "$TMDB_IMAGE_BASE_URL$PROFILE_IMAGE_SIZE$it" }
        }

        /**
         * Key crew roles to highlight
         */
        val KEY_ROLES =
            setOf(
                "Director",
                "Producer",
                "Executive Producer",
                "Writer",
                "Screenplay",
                "Creator",
                "Showrunner",
                "Director of Photography",
                "Composer",
                "Editor",
            )

        /**
         * Check if this is a key crew role
         */
        fun isKeyRole(job: String): Boolean = job in KEY_ROLES
    }
}

/**
 * Extended content metadata that includes full cast and crew lists
 */
data class ExtendedContentMetadata(
    val year: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val language: String? = null,
    val genre: List<String> = emptyList(),
    val studio: String? = null,
    val cast: List<String> = emptyList(), // Legacy string list for backward compatibility
    val fullCast: List<CastMember> = emptyList(), // Enhanced cast list with images
    val director: String? = null, // Legacy single director for backward compatibility
    val crew: List<CrewMember> = emptyList(), // Full crew list
    val season: Int? = null,
    val episode: Int? = null,
    val quality: String? = null,
    val isHDR: Boolean = false,
    val is4K: Boolean = false,
    val customMetadata: Map<String, String> = emptyMap(),
) {
    /**
     * Get key crew members (directors, producers, writers)
     */
    fun getKeyCrew(): List<CrewMember> {
        return crew.filter { CrewMember.isKeyRole(it.job) }
            .sortedBy {
                // Sort by importance of role
                when (it.job) {
                    "Director" -> 0
                    "Creator", "Showrunner" -> 1
                    "Producer", "Executive Producer" -> 2
                    "Writer", "Screenplay" -> 3
                    else -> 4
                }
            }
    }

    /**
     * Get directors from crew list
     */
    fun getDirectors(): List<CrewMember> {
        return crew.filter { it.job == "Director" }
    }

    /**
     * Get writers from crew list
     */
    fun getWriters(): List<CrewMember> {
        return crew.filter { it.job in listOf("Writer", "Screenplay", "Story") }
    }

    /**
     * Get producers from crew list
     */
    fun getProducers(): List<CrewMember> {
        return crew.filter { it.job.contains("Producer") }
    }

    /**
     * Convert to ContentMetadata for backward compatibility
     */
    fun toContentMetadata(): ContentMetadata {
        return ContentMetadata(
            year = year,
            duration = duration,
            rating = rating,
            language = language,
            genre = genre,
            studio = studio,
            cast = cast,
            director = director,
            season = season,
            episode = episode,
            quality = quality,
            isHDR = isHDR,
            is4K = is4K,
            customMetadata = customMetadata,
        )
    }
}
