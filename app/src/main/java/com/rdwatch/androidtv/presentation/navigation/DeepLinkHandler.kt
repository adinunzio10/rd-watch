package com.rdwatch.androidtv.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController

class DeepLinkHandler {
    
    companion object {
        const val SCHEME = "rdwatch"
        const val HOST = "app"
        
        // Deep link patterns
        private const val MOVIE_DETAILS_PATTERN = "$SCHEME://$HOST/movie/{movieId}"
        private const val VIDEO_PLAYER_PATTERN = "$SCHEME://$HOST/player/{videoUrl}"
        private const val SEARCH_PATTERN = "$SCHEME://$HOST/search"
        private const val SETTINGS_PATTERN = "$SCHEME://$HOST/settings"
    }
    
    fun handleDeepLink(intent: Intent, navController: NavController): Boolean {
        val uri = intent.data ?: return false
        
        return when {
            uri.scheme == SCHEME && uri.host == HOST -> {
                handleAppDeepLink(uri, navController)
            }
            else -> false
        }
    }
    
    private fun handleAppDeepLink(uri: Uri, navController: NavController): Boolean {
        val pathSegments = uri.pathSegments
        
        return when {
            pathSegments.isNotEmpty() -> {
                when (pathSegments[0]) {
                    "movie" -> {
                        val movieId = pathSegments.getOrNull(1)
                        if (movieId != null) {
                            navController.navigateToMovieDetails(movieId)
                            true
                        } else false
                    }
                    "player" -> {
                        val videoUrl = pathSegments.getOrNull(1)
                        if (videoUrl != null) {
                            val title = uri.getQueryParameter("title") ?: ""
                            navController.navigateToVideoPlayer(
                                videoUrl = Uri.decode(videoUrl),
                                title = title
                            )
                            true
                        } else false
                    }
                    "search" -> {
                        navController.navigateToSearch()
                        true
                    }
                    "settings" -> {
                        navController.navigateToSettings()
                        true
                    }
                    "scrapers" -> {
                        navController.navigate(Screen.ScraperSettings)
                        true
                    }
                    "auth" -> {
                        navController.navigate(Screen.Authentication)
                        true
                    }
                    else -> false
                }
            }
            else -> false
        }
    }
    
    fun createDeepLink(screen: Screen): String {
        return when (screen) {
            is Screen.Home -> "$SCHEME://$HOST/"
            is Screen.Browse -> "$SCHEME://$HOST/browse"
            is Screen.MovieDetails -> "$SCHEME://$HOST/movie/${screen.movieId}"
            is Screen.TVDetails -> "$SCHEME://$HOST/tv/${screen.tvShowId}"
            is Screen.VideoPlayer -> {
                val encodedUrl = Uri.encode(screen.videoUrl)
                val titleParam = if (screen.title.isNotEmpty()) "?title=${Uri.encode(screen.title)}" else ""
                "$SCHEME://$HOST/player/$encodedUrl$titleParam"
            }
            is Screen.Search -> "$SCHEME://$HOST/search"
            is Screen.Settings -> "$SCHEME://$HOST/settings"
            is Screen.ScraperSettings -> "$SCHEME://$HOST/scrapers"
            is Screen.Profile -> "$SCHEME://$HOST/profile"
            is Screen.Authentication -> "$SCHEME://$HOST/auth"
            is Screen.Error -> "$SCHEME://$HOST/error?message=${Uri.encode(screen.message)}&canRetry=${screen.canRetry}"
            is Screen.AccountFileBrowser -> "$SCHEME://$HOST/account_file_browser/${screen.accountType}"
        }
    }
}