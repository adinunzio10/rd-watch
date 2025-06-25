package com.rdwatch.androidtv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.ui.browse.BrowseScreen
import com.rdwatch.androidtv.ui.settings.SettingsScreen
import com.rdwatch.androidtv.ui.details.MovieDetailsScreen
import com.rdwatch.androidtv.ui.profile.ProfileScreen
import com.rdwatch.androidtv.ui.home.TVHomeScreen
import com.rdwatch.androidtv.ui.search.SearchScreen
import com.rdwatch.androidtv.MovieList

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
            TVHomeScreen(
                onNavigateToScreen = { screen ->
                    navController.navigate(screen)
                },
                onMovieClick = { movie ->
                    navController.navigate(Screen.MovieDetails(movie.id.toString()))
                }
            )
        }
        
        composable<Screen.Browse> {
            BrowseScreen(
                onMovieClick = { movie ->
                    navController.navigate(Screen.MovieDetails(movie.id.toString()))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.MovieDetails> { backStackEntry ->
            val movieDetails = backStackEntry.toRoute<Screen.MovieDetails>()
            // Find the movie by ID
            val movie = MovieList.list.find { it.id.toString() == movieDetails.movieId }
            
            if (movie != null) {
                MovieDetailsScreen(
                    movie = movie,
                    onPlayClick = { selectedMovie ->
                        navController.navigate(
                            Screen.VideoPlayer(
                                videoUrl = selectedMovie.videoUrl ?: "",
                                title = selectedMovie.title ?: ""
                            )
                        )
                    },
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            } else {
                // Handle movie not found - navigate to error screen
                navController.navigate(
                    Screen.Error(
                        message = "Movie not found",
                        canRetry = true
                    )
                )
            }
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
            SearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onItemSelected = { movieId ->
                    navController.navigate(Screen.MovieDetails(movieId))
                }
            )
        }
        
        composable<Screen.Settings> {
            SettingsScreen(
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.Profile> {
            ProfileScreen(
                onMovieClick = { movie ->
                    navController.navigate(Screen.MovieDetails(movie.id.toString()))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
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