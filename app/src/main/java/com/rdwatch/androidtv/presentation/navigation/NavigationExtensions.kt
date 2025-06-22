package com.rdwatch.androidtv.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions

fun NavController.navigateToScreen(
    screen: Screen,
    navOptions: NavOptions? = null
) {
    navigate(screen, navOptions)
}

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
    
    navigate(screen, navOptions)
}

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

fun NavController.navigateToMovieDetails(movieId: String) {
    navigateToScreen(Screen.MovieDetails(movieId))
}

fun NavController.navigateToVideoPlayer(videoUrl: String, title: String = "") {
    navigateToScreen(Screen.VideoPlayer(videoUrl, title))
}

fun NavController.navigateToSearch() {
    navigateToScreen(Screen.Search)
}

fun NavController.navigateToSettings() {
    navigateToScreen(Screen.Settings)
}

fun NavController.navigateToProfile() {
    navigateToScreen(Screen.Profile)
}

fun NavController.navigateToError(message: String, canRetry: Boolean = true) {
    navigateToScreen(Screen.Error(message, canRetry))
}

fun NavController.navigateUp(): Boolean {
    return navigateUp()
}

fun NavController.popBackStackToRoute(route: String, inclusive: Boolean = false): Boolean {
    return popBackStack(route, inclusive)
}