package com.rdwatch.androidtv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: Screen = Screen.Home,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Screen.Home> {
            // TODO: Implement HomeScreen composable
            // HomeScreen(navController = navController)
        }
        
        composable<Screen.Browse> {
            // TODO: Implement BrowseScreen composable
            // BrowseScreen(navController = navController)
        }
        
        composable<Screen.MovieDetails> { backStackEntry ->
            val movieDetails = backStackEntry.toRoute<Screen.MovieDetails>()
            // TODO: Implement MovieDetailsScreen composable
            // MovieDetailsScreen(
            //     movieId = movieDetails.movieId,
            //     navController = navController
            // )
        }
        
        composable<Screen.VideoPlayer> { backStackEntry ->
            val videoPlayer = backStackEntry.toRoute<Screen.VideoPlayer>()
            // TODO: Implement VideoPlayerScreen composable
            // VideoPlayerScreen(
            //     videoUrl = videoPlayer.videoUrl,
            //     title = videoPlayer.title,
            //     navController = navController
            // )
        }
        
        composable<Screen.Search> {
            // TODO: Implement SearchScreen composable
            // SearchScreen(navController = navController)
        }
        
        composable<Screen.Settings> {
            // TODO: Implement SettingsScreen composable
            // SettingsScreen(navController = navController)
        }
        
        composable<Screen.Profile> {
            // TODO: Implement ProfileScreen composable
            // ProfileScreen(navController = navController)
        }
        
        composable<Screen.Error> { backStackEntry ->
            val error = backStackEntry.toRoute<Screen.Error>()
            // TODO: Implement ErrorScreen composable
            // ErrorScreen(
            //     message = error.message,
            //     canRetry = error.canRetry,
            //     navController = navController
            // )
        }
    }
}