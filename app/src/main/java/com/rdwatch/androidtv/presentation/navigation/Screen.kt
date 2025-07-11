package com.rdwatch.androidtv.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Home : Screen()
    
    @Serializable
    data object Browse : Screen()
    
    @Serializable
    data class MovieDetails(val movieId: String) : Screen()
    
    @Serializable
    data class TVDetails(val tvShowId: String) : Screen()
    
    @Serializable
    data class VideoPlayer(val videoUrl: String, val title: String = "") : Screen()
    
    @Serializable
    data object Search : Screen()
    
    @Serializable
    data object Settings : Screen()
    
    @Serializable
    data object ScraperSettings : Screen()
    
    @Serializable
    data object Profile : Screen()
    
    @Serializable
    data object Authentication : Screen()
    
    @Serializable
    data class Error(val message: String, val canRetry: Boolean = true) : Screen()
    
    @Serializable
    data class AccountFileBrowser(val accountType: String = "realdebrid") : Screen()
}

object Routes {
    const val HOME = "home"
    const val BROWSE = "browse"
    const val MOVIE_DETAILS = "movie_details/{movieId}"
    const val TV_DETAILS = "tv_details/{tvShowId}"
    const val VIDEO_PLAYER = "video_player/{videoUrl}/{title}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val SCRAPER_SETTINGS = "scraper_settings"
    const val PROFILE = "profile"
    const val AUTHENTICATION = "authentication"
    const val ERROR = "error/{message}/{canRetry}"
    const val ACCOUNT_FILE_BROWSER = "account_file_browser/{accountType}"
    
    object Args {
        const val MOVIE_ID = "movieId"
        const val TV_SHOW_ID = "tvShowId"
        const val VIDEO_URL = "videoUrl"
        const val TITLE = "title"
        const val MESSAGE = "message"
        const val CAN_RETRY = "canRetry"
        const val ACCOUNT_TYPE = "accountType"
    }
}