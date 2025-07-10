package com.rdwatch.androidtv.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Enhanced navigation extensions with safety checks and state management
 */

// Navigation event channel for handling navigation side effects
private val navigationEventChannel = Channel<NavigationEvent>(Channel.UNLIMITED)
val navigationEvents = navigationEventChannel.receiveAsFlow()

sealed class NavigationEvent {
    data class NavigatedTo(val screen: Screen) : NavigationEvent()
    data class NavigatedBack(val fromScreen: Screen?) : NavigationEvent()
    data class NavigationError(val error: String) : NavigationEvent()
}

/**
 * Safe navigation to any screen with error handling
 */
fun NavController.navigateToScreen(
    screen: Screen,
    navOptions: NavOptions? = null
) {
    try {
        navigate(screen, navOptions)
        navigationEventChannel.trySend(NavigationEvent.NavigatedTo(screen))
    } catch (e: Exception) {
        navigationEventChannel.trySend(
            NavigationEvent.NavigationError("Failed to navigate to $screen: ${e.message}")
        )
    }
}

/**
 * Navigate with pop up behavior and state management
 */
fun NavController.navigateToScreenWithPopUp(
    screen: Screen,
    popUpToRoute: String? = null,
    inclusive: Boolean = false,
    saveState: Boolean = true
) {
    val navOptions = NavOptions.Builder().apply {
        if (popUpToRoute != null) {
            setPopUpTo(popUpToRoute, inclusive, saveState)
        }
        setLaunchSingleTop(true)
        setRestoreState(saveState)
    }.build()
    
    navigateToScreen(screen, navOptions)
}

/**
 * Navigate to home with option to clear entire back stack
 */
fun NavController.navigateToHome(clearBackStack: Boolean = false) {
    val navOptions = if (clearBackStack) {
        NavOptions.Builder()
            .setPopUpTo(graph.findStartDestination().id, true)
            .setLaunchSingleTop(true)
            .build()
    } else {
        null
    }
    navigateToScreen(Screen.Home, navOptions)
}

/**
 * Navigate to movie details with validation
 */
fun NavController.navigateToMovieDetails(movieId: String) {
    if (movieId.isBlank()) {
        navigationEventChannel.trySend(
            NavigationEvent.NavigationError("Invalid movie ID provided")
        )
        return
    }
    navigateToScreen(Screen.MovieDetails(movieId))
}

/**
 * Navigate to TV show details with validation
 */
fun NavController.navigateToTVDetails(tvShowId: String) {
    if (tvShowId.isBlank()) {
        navigationEventChannel.trySend(
            NavigationEvent.NavigationError("Invalid TV show ID provided")
        )
        return
    }
    navigateToScreen(Screen.TVDetails(tvShowId))
}

/**
 * Navigate to video player with validation
 */
fun NavController.navigateToVideoPlayer(videoUrl: String, title: String = "") {
    if (videoUrl.isBlank()) {
        navigationEventChannel.trySend(
            NavigationEvent.NavigationError("Invalid video URL provided")
        )
        return
    }
    navigateToScreen(Screen.VideoPlayer(videoUrl, title))
}

/**
 * Navigate to search screen
 */
fun NavController.navigateToSearch() {
    navigateToScreen(Screen.Search)
}

/**
 * Navigate to browse screen
 */
fun NavController.navigateToBrowse() {
    navigateToScreen(Screen.Browse)
}

/**
 * Navigate to settings screen
 */
fun NavController.navigateToSettings() {
    navigateToScreen(Screen.Settings)
}

/**
 * Navigate to scraper settings screen
 */
fun NavController.navigateToScraperSettings() {
    navigateToScreen(Screen.ScraperSettings)
}

/**
 * Navigate to profile screen
 */
fun NavController.navigateToProfile() {
    navigateToScreen(Screen.Profile)
}

/**
 * Navigate to authentication screen with back stack clearing
 */
fun NavController.navigateToAuthentication(clearBackStack: Boolean = true) {
    val navOptions = if (clearBackStack) {
        NavOptions.Builder()
            .setPopUpTo(0, true)
            .setLaunchSingleTop(true)
            .build()
    } else {
        null
    }
    navigateToScreen(Screen.Authentication, navOptions)
}

/**
 * Navigate to account file browser
 */
fun NavController.navigateToAccountFileBrowser(accountType: String = "realdebrid") {
    navigateToScreen(Screen.AccountFileBrowser(accountType))
}

/**
 * Navigate to error screen with message and retry capability
 */
fun NavController.navigateToError(message: String, canRetry: Boolean = true) {
    navigateToScreen(Screen.Error(message, canRetry))
}

/**
 * Enhanced back navigation with event tracking
 */
fun NavController.navigateBack(): Boolean {
    val currentDestination = currentDestination
    val result = navigateUp()
    
    if (result) {
        navigationEventChannel.trySend(
            NavigationEvent.NavigatedBack(getCurrentScreen(currentDestination))
        )
    }
    
    return result
}

/**
 * Pop back stack to specific route with safety checks
 */
fun NavController.popBackStackToRoute(route: String, inclusive: Boolean = false): Boolean {
    return try {
        popBackStack(route, inclusive)
    } catch (e: Exception) {
        navigationEventChannel.trySend(
            NavigationEvent.NavigationError("Failed to pop back stack to $route: ${e.message}")
        )
        false
    }
}

/**
 * Pop back stack to home screen
 */
fun NavController.popBackStackToHome(): Boolean {
    return popBackStackToRoute(Screen.Home::class.qualifiedName ?: "home", false)
}

/**
 * Check if we can navigate back
 */
fun NavController.canNavigateBack(): Boolean {
    return previousBackStackEntry != null
}

/**
 * Get current screen from destination
 */
private fun getCurrentScreen(destination: NavDestination?): Screen? {
    return destination?.route?.let { route ->
        try {
            when {
                route.contains("home") -> Screen.Home
                route.contains("browse") -> Screen.Browse
                route.contains("movie_details") -> Screen.MovieDetails("")
                route.contains("tv_details") -> Screen.TVDetails("")
                route.contains("video_player") -> Screen.VideoPlayer("", "")
                route.contains("search") -> Screen.Search
                route.contains("settings") -> Screen.Settings
                route.contains("scraper_settings") -> Screen.ScraperSettings
                route.contains("profile") -> Screen.Profile
                route.contains("authentication") -> Screen.Authentication
                route.contains("error") -> Screen.Error("", true)
                route.contains("account_file_browser") -> Screen.AccountFileBrowser("")
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Get current screen safely
 */
fun NavController.getCurrentScreen(): Screen? {
    return getCurrentScreen(currentDestination)
}

/**
 * Check if currently on specific screen
 */
fun NavController.isCurrentScreen(screen: Screen): Boolean {
    val currentScreen = getCurrentScreen()
    return when {
        screen is Screen.Home && currentScreen is Screen.Home -> true
        screen is Screen.Browse && currentScreen is Screen.Browse -> true
        screen is Screen.Search && currentScreen is Screen.Search -> true
        screen is Screen.Settings && currentScreen is Screen.Settings -> true
        screen is Screen.ScraperSettings && currentScreen is Screen.ScraperSettings -> true
        screen is Screen.Profile && currentScreen is Screen.Profile -> true
        screen is Screen.Authentication && currentScreen is Screen.Authentication -> true
        screen is Screen.MovieDetails && currentScreen is Screen.MovieDetails -> true
        screen is Screen.TVDetails && currentScreen is Screen.TVDetails -> true
        screen is Screen.VideoPlayer && currentScreen is Screen.VideoPlayer -> true
        screen is Screen.Error && currentScreen is Screen.Error -> true
        screen is Screen.AccountFileBrowser && currentScreen is Screen.AccountFileBrowser -> true
        else -> false
    }
}