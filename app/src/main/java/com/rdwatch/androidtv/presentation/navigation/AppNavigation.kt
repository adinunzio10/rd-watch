package com.rdwatch.androidtv.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import com.rdwatch.androidtv.auth.ui.AuthenticationScreen
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
    onAuthenticationSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Screen.Authentication> {
            AuthenticationScreen(
                onAuthenticationSuccess = onAuthenticationSuccess
            )
        }
        
        composable<Screen.Home>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            TVHomeScreen(
                onNavigateToScreen = { screen ->
                    navController.navigate(screen)
                },
                onMovieClick = { movie ->
                    navController.navigate(Screen.MovieDetails(movie.id.toString()))
                }
            )
        }
        
        composable<Screen.Browse>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            BrowseScreen(
                onMovieClick = { movie ->
                    navController.navigate(Screen.MovieDetails(movie.id.toString()))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.MovieDetails>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
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
        
        composable<Screen.Search>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            SearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onItemSelected = { movieId ->
                    navController.navigate(Screen.MovieDetails(movieId))
                }
            )
        }
        
        composable<Screen.Settings>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            val settingsViewModel: com.rdwatch.androidtv.ui.settings.SettingsViewModel = hiltViewModel()
            SettingsScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onSignOut = {
                    settingsViewModel.signOut()
                    // Navigate to authentication and clear back stack
                    navController.navigate(Screen.Authentication) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable<Screen.Profile>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
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