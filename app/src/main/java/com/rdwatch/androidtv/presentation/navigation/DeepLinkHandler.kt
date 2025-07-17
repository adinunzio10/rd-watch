package com.rdwatch.androidtv.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.net.URLDecoder
import java.util.regex.Pattern

/**
 * Enhanced deep link handler with validation, error handling, and analytics
 */
class DeepLinkHandler {
    companion object {
        const val SCHEME = "rdwatch"
        const val HOST = "app"

        // Deep link patterns with regex validation
        private const val MOVIE_DETAILS_PATTERN = "$SCHEME://$HOST/movie/{movieId}"
        private const val TV_DETAILS_PATTERN = "$SCHEME://$HOST/tv/{tvShowId}"
        private const val VIDEO_PLAYER_PATTERN = "$SCHEME://$HOST/player/{videoUrl}"
        private const val SEARCH_PATTERN = "$SCHEME://$HOST/search"
        private const val SETTINGS_PATTERN = "$SCHEME://$HOST/settings"
        private const val BROWSE_PATTERN = "$SCHEME://$HOST/browse"
        private const val PROFILE_PATTERN = "$SCHEME://$HOST/profile"
        private const val AUTH_PATTERN = "$SCHEME://$HOST/auth"
        private const val ERROR_PATTERN = "$SCHEME://$HOST/error"
        private const val ACCOUNT_FILE_BROWSER_PATTERN = "$SCHEME://$HOST/account_file_browser/{accountType}"
        private const val SCRAPER_SETTINGS_PATTERN = "$SCHEME://$HOST/scrapers"

        // Validation patterns
        private val MOVIE_ID_PATTERN = Pattern.compile("^[0-9]+$")
        private val TV_SHOW_ID_PATTERN = Pattern.compile("^[0-9]+$")
        private val URL_PATTERN = Pattern.compile("^https?://.*")
        private val ACCOUNT_TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$")
    }

    // Event tracking for analytics
    private val _deepLinkEvents = Channel<DeepLinkEvent>(Channel.UNLIMITED)
    val deepLinkEvents = _deepLinkEvents.receiveAsFlow()

    /**
     * Handle deep link with comprehensive validation and error handling
     */
    fun handleDeepLink(
        intent: Intent,
        navController: NavController,
    ): Boolean {
        val uri = intent.data

        if (uri == null) {
            emitEvent(DeepLinkEvent.Error("No URI provided in intent"))
            return false
        }

        return try {
            val result =
                when {
                    uri.scheme == SCHEME && uri.host == HOST -> {
                        handleAppDeepLink(uri, navController)
                    }
                    isWebDeepLink(uri) -> {
                        handleWebDeepLink(uri, navController)
                    }
                    else -> {
                        emitEvent(DeepLinkEvent.UnsupportedScheme(uri.toString()))
                        false
                    }
                }

            if (result) {
                emitEvent(DeepLinkEvent.Success(uri.toString()))
            }

            result
        } catch (e: Exception) {
            emitEvent(DeepLinkEvent.Error("Failed to handle deep link: ${e.message}"))
            false
        }
    }

    /**
     * Handle app-specific deep links
     */
    private fun handleAppDeepLink(
        uri: Uri,
        navController: NavController,
    ): Boolean {
        val pathSegments = uri.pathSegments

        if (pathSegments.isEmpty()) {
            // Navigate to home if no path
            navController.navigateToHome()
            return true
        }

        return when (val firstSegment = pathSegments[0]) {
            "movie" -> handleMovieDetailsDeepLink(uri, pathSegments, navController)
            "tv" -> handleTVDetailsDeepLink(uri, pathSegments, navController)
            "player" -> handleVideoPlayerDeepLink(uri, pathSegments, navController)
            "search" -> handleSearchDeepLink(uri, navController)
            "browse" -> handleBrowseDeepLink(uri, navController)
            "settings" -> handleSettingsDeepLink(uri, navController)
            "scrapers" -> handleScraperSettingsDeepLink(uri, navController)
            "profile" -> handleProfileDeepLink(uri, navController)
            "auth" -> handleAuthDeepLink(uri, navController)
            "error" -> handleErrorDeepLink(uri, navController)
            "account_file_browser" -> handleAccountFileBrowserDeepLink(uri, pathSegments, navController)
            else -> {
                emitEvent(DeepLinkEvent.UnsupportedPath(firstSegment))
                false
            }
        }
    }

    /**
     * Handle web deep links (e.g., from shared URLs)
     */
    private fun handleWebDeepLink(
        uri: Uri,
        navController: NavController,
    ): Boolean {
        // Handle web URLs that should deep link into the app
        // This could include shared movie URLs, etc.
        emitEvent(DeepLinkEvent.WebLinkAttempt(uri.toString()))
        return false // Not implemented yet
    }

    /**
     * Individual deep link handlers with validation
     */
    private fun handleMovieDetailsDeepLink(
        uri: Uri,
        pathSegments: List<String>,
        navController: NavController,
    ): Boolean {
        val movieId = pathSegments.getOrNull(1)

        if (movieId.isNullOrBlank()) {
            emitEvent(DeepLinkEvent.ValidationError("Missing movie ID"))
            return false
        }

        if (!MOVIE_ID_PATTERN.matcher(movieId).matches()) {
            emitEvent(DeepLinkEvent.ValidationError("Invalid movie ID format: $movieId"))
            return false
        }

        navController.navigateToMovieDetails(movieId)
        return true
    }

    private fun handleTVDetailsDeepLink(
        uri: Uri,
        pathSegments: List<String>,
        navController: NavController,
    ): Boolean {
        val tvShowId = pathSegments.getOrNull(1)

        if (tvShowId.isNullOrBlank()) {
            emitEvent(DeepLinkEvent.ValidationError("Missing TV show ID"))
            return false
        }

        if (!TV_SHOW_ID_PATTERN.matcher(tvShowId).matches()) {
            emitEvent(DeepLinkEvent.ValidationError("Invalid TV show ID format: $tvShowId"))
            return false
        }

        navController.navigateToTVDetails(tvShowId)
        return true
    }

    private fun handleVideoPlayerDeepLink(
        uri: Uri,
        pathSegments: List<String>,
        navController: NavController,
    ): Boolean {
        val encodedVideoUrl = pathSegments.getOrNull(1)

        if (encodedVideoUrl.isNullOrBlank()) {
            emitEvent(DeepLinkEvent.ValidationError("Missing video URL"))
            return false
        }

        val videoUrl =
            try {
                URLDecoder.decode(encodedVideoUrl, "UTF-8")
            } catch (e: Exception) {
                emitEvent(DeepLinkEvent.ValidationError("Invalid URL encoding: ${e.message}"))
                return false
            }

        if (!URL_PATTERN.matcher(videoUrl).matches()) {
            emitEvent(DeepLinkEvent.ValidationError("Invalid video URL format: $videoUrl"))
            return false
        }

        val title = uri.getQueryParameter("title") ?: ""
        navController.navigateToVideoPlayer(videoUrl, title)
        return true
    }

    private fun handleSearchDeepLink(
        uri: Uri,
        navController: NavController,
    ): Boolean {
        val query = uri.getQueryParameter("q")
        navController.navigateToSearch()
        // TODO: Pre-populate search with query if provided
        return true
    }

    private fun handleBrowseDeepLink(
        uri: Uri,
        navController: NavController,
    ): Boolean {
        navController.navigateToBrowse()
        return true
    }

    private fun handleSettingsDeepLink(
        uri: Uri,
        navController: NavController,
    ): Boolean {
        val section = uri.getQueryParameter("section")
        navController.navigateToSettings()
        // TODO: Navigate to specific settings section if provided
        return true
    }

    private fun handleScraperSettingsDeepLink(
        uri: Uri,
        navController: NavController,
    ): Boolean {
        navController.navigateToScraperSettings()
        return true
    }

    private fun handleProfileDeepLink(
        uri: Uri,
        navController: NavController,
    ): Boolean {
        navController.navigateToProfile()
        return true
    }

    private fun handleAuthDeepLink(
        uri: Uri,
        navController: NavController,
    ): Boolean {
        val clearBackStack = uri.getBooleanQueryParameter("clearBackStack", true)
        navController.navigateToAuthentication(clearBackStack)
        return true
    }

    private fun handleErrorDeepLink(
        uri: Uri,
        navController: NavController,
    ): Boolean {
        val message = uri.getQueryParameter("message") ?: "Unknown error"
        val canRetry = uri.getBooleanQueryParameter("canRetry", true)
        navController.navigateToError(message, canRetry)
        return true
    }

    private fun handleAccountFileBrowserDeepLink(
        uri: Uri,
        pathSegments: List<String>,
        navController: NavController,
    ): Boolean {
        val accountType = pathSegments.getOrNull(1) ?: "realdebrid"

        if (!ACCOUNT_TYPE_PATTERN.matcher(accountType).matches()) {
            emitEvent(DeepLinkEvent.ValidationError("Invalid account type format: $accountType"))
            return false
        }

        navController.navigateToAccountFileBrowser(accountType)
        return true
    }

    /**
     * Create deep link URL for a screen with validation
     */
    fun createDeepLink(screen: Screen): String {
        return when (screen) {
            is Screen.Home -> "$SCHEME://$HOST/"
            is Screen.Browse -> "$SCHEME://$HOST/browse"
            is Screen.MovieDetails -> {
                validateMovieId(screen.movieId)
                "$SCHEME://$HOST/movie/${screen.movieId}"
            }
            is Screen.TVDetails -> {
                validateTVShowId(screen.tvShowId)
                "$SCHEME://$HOST/tv/${screen.tvShowId}"
            }
            is Screen.VideoPlayer -> {
                validateVideoUrl(screen.videoUrl)
                val encodedUrl = Uri.encode(screen.videoUrl)
                val titleParam = if (screen.title.isNotEmpty()) "?title=${Uri.encode(screen.title)}" else ""
                "$SCHEME://$HOST/player/$encodedUrl$titleParam"
            }
            is Screen.Search -> "$SCHEME://$HOST/search"
            is Screen.Settings -> "$SCHEME://$HOST/settings"
            is Screen.ScraperSettings -> "$SCHEME://$HOST/scrapers"
            is Screen.Profile -> "$SCHEME://$HOST/profile"
            is Screen.Authentication -> "$SCHEME://$HOST/auth"
            is Screen.Error -> {
                val messageParam = "message=${Uri.encode(screen.message)}"
                val retryParam = "canRetry=${screen.canRetry}"
                "$SCHEME://$HOST/error?$messageParam&$retryParam"
            }
            is Screen.AccountFileBrowser -> {
                validateAccountType(screen.accountType)
                "$SCHEME://$HOST/account_file_browser/${screen.accountType}"
            }
        }
    }

    /**
     * Validation methods
     */
    private fun validateMovieId(movieId: String) {
        if (!MOVIE_ID_PATTERN.matcher(movieId).matches()) {
            throw IllegalArgumentException("Invalid movie ID format: $movieId")
        }
    }

    private fun validateTVShowId(tvShowId: String) {
        if (!TV_SHOW_ID_PATTERN.matcher(tvShowId).matches()) {
            throw IllegalArgumentException("Invalid TV show ID format: $tvShowId")
        }
    }

    private fun validateVideoUrl(videoUrl: String) {
        if (!URL_PATTERN.matcher(videoUrl).matches()) {
            throw IllegalArgumentException("Invalid video URL format: $videoUrl")
        }
    }

    private fun validateAccountType(accountType: String) {
        if (!ACCOUNT_TYPE_PATTERN.matcher(accountType).matches()) {
            throw IllegalArgumentException("Invalid account type format: $accountType")
        }
    }

    /**
     * Utility methods
     */
    private fun isWebDeepLink(uri: Uri): Boolean {
        return uri.scheme in listOf("http", "https")
    }

    private fun emitEvent(event: DeepLinkEvent) {
        _deepLinkEvents.trySend(event)
    }

    /**
     * Check if deep link is valid without navigating
     */
    fun validateDeepLink(uri: Uri): DeepLinkValidationResult {
        return try {
            when {
                uri.scheme != SCHEME -> DeepLinkValidationResult.Invalid("Unsupported scheme: ${uri.scheme}")
                uri.host != HOST -> DeepLinkValidationResult.Invalid("Unsupported host: ${uri.host}")
                else -> {
                    val pathSegments = uri.pathSegments
                    if (pathSegments.isEmpty()) {
                        DeepLinkValidationResult.Valid
                    } else {
                        validatePathSegments(pathSegments, uri)
                    }
                }
            }
        } catch (e: Exception) {
            DeepLinkValidationResult.Invalid("Validation error: ${e.message}")
        }
    }

    private fun validatePathSegments(
        pathSegments: List<String>,
        uri: Uri,
    ): DeepLinkValidationResult {
        return when (val firstSegment = pathSegments[0]) {
            "movie" -> {
                val movieId = pathSegments.getOrNull(1)
                when {
                    movieId.isNullOrBlank() -> DeepLinkValidationResult.Invalid("Missing movie ID")
                    !MOVIE_ID_PATTERN.matcher(movieId).matches() -> DeepLinkValidationResult.Invalid("Invalid movie ID format")
                    else -> DeepLinkValidationResult.Valid
                }
            }
            "tv" -> {
                val tvShowId = pathSegments.getOrNull(1)
                when {
                    tvShowId.isNullOrBlank() -> DeepLinkValidationResult.Invalid("Missing TV show ID")
                    !TV_SHOW_ID_PATTERN.matcher(tvShowId).matches() -> DeepLinkValidationResult.Invalid("Invalid TV show ID format")
                    else -> DeepLinkValidationResult.Valid
                }
            }
            "player" -> {
                val encodedVideoUrl = pathSegments.getOrNull(1)
                when {
                    encodedVideoUrl.isNullOrBlank() -> DeepLinkValidationResult.Invalid("Missing video URL")
                    else -> {
                        try {
                            val videoUrl = URLDecoder.decode(encodedVideoUrl, "UTF-8")
                            if (!URL_PATTERN.matcher(videoUrl).matches()) {
                                DeepLinkValidationResult.Invalid("Invalid video URL format")
                            } else {
                                DeepLinkValidationResult.Valid
                            }
                        } catch (e: Exception) {
                            DeepLinkValidationResult.Invalid("Invalid URL encoding")
                        }
                    }
                }
            }
            "account_file_browser" -> {
                val accountType = pathSegments.getOrNull(1) ?: "realdebrid"
                if (!ACCOUNT_TYPE_PATTERN.matcher(accountType).matches()) {
                    DeepLinkValidationResult.Invalid("Invalid account type format")
                } else {
                    DeepLinkValidationResult.Valid
                }
            }
            in listOf("search", "browse", "settings", "scrapers", "profile", "auth", "error") -> {
                DeepLinkValidationResult.Valid
            }
            else -> DeepLinkValidationResult.Invalid("Unsupported path: $firstSegment")
        }
    }
}

/**
 * Deep link events for analytics and error handling
 */
sealed class DeepLinkEvent {
    data class Success(val url: String) : DeepLinkEvent()

    data class Error(val message: String) : DeepLinkEvent()

    data class ValidationError(val message: String) : DeepLinkEvent()

    data class UnsupportedScheme(val url: String) : DeepLinkEvent()

    data class UnsupportedPath(val path: String) : DeepLinkEvent()

    data class WebLinkAttempt(val url: String) : DeepLinkEvent()
}

/**
 * Deep link validation result
 */
sealed class DeepLinkValidationResult {
    object Valid : DeepLinkValidationResult()

    data class Invalid(val reason: String) : DeepLinkValidationResult()

    fun isValid(): Boolean = this is Valid

    fun getErrorMessage(): String? =
        when (this) {
            is Invalid -> reason
            is Valid -> null
        }
}

/**
 * Extension function for getting boolean query parameters
 */
private fun Uri.getBooleanQueryParameter(
    key: String,
    defaultValue: Boolean,
): Boolean {
    return getQueryParameter(key)?.toBooleanStrictOrNull() ?: defaultValue
}
