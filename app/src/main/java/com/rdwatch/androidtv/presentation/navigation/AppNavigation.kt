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
import com.rdwatch.androidtv.auth.ui.AuthGuard
import com.rdwatch.androidtv.ui.browse.BrowseScreen
import com.rdwatch.androidtv.ui.settings.SettingsScreen
import com.rdwatch.androidtv.ui.settings.scrapers.ScraperSettingsScreen
import com.rdwatch.androidtv.ui.details.MovieDetailsScreen
import com.rdwatch.androidtv.ui.details.MovieDetailsViewModel
import com.rdwatch.androidtv.ui.details.TVDetailsScreen
import com.rdwatch.androidtv.ui.details.TVDetailsViewModel
import com.rdwatch.androidtv.ui.profile.ProfileScreen
import com.rdwatch.androidtv.ui.home.TVHomeScreen
import com.rdwatch.androidtv.ui.search.SearchScreen
import com.rdwatch.androidtv.ui.filebrowser.AccountFileBrowserScreen

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
            AuthGuard(
                onAuthenticationRequired = {
                    // Navigate to authentication and clear back stack
                    navController.navigate(Screen.Authentication) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                content = {
                    TVHomeScreen(
                        onNavigateToScreen = { screen ->
                            navController.navigate(screen)
                        },
                        onMovieClick = { movie ->
                            navController.navigate(Screen.MovieDetails(movie.id.toString()))
                        }
                    )
                },
                showLoadingOnInitializing = true
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
            AuthGuard(
                onAuthenticationRequired = {
                    navController.navigate(Screen.Authentication) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                content = {
                    BrowseScreen(
                        onMovieClick = { movie ->
                            navController.navigate(Screen.MovieDetails(movie.id.toString()))
                        },
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
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
            val viewModel: MovieDetailsViewModel = hiltViewModel()
            
            // Load movie details using the ViewModel
            MovieDetailsScreen(
                movieId = movieDetails.movieId,
                viewModel = viewModel,
                onPlayClick = { selectedMovie ->
                    navController.navigate(
                        Screen.VideoPlayer(
                            videoUrl = selectedMovie.videoUrl ?: "",
                            title = selectedMovie.title ?: ""
                        )
                    )
                },
                onMovieClick = { movie ->
                    navController.navigate(Screen.MovieDetails(movie.id.toString()))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.TVDetails>(
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
            val tvDetails = backStackEntry.toRoute<Screen.TVDetails>()
            val viewModel: TVDetailsViewModel = hiltViewModel()
            
            TVDetailsScreen(
                tvShowId = tvDetails.tvShowId,
                viewModel = viewModel,
                onPlayClick = { selectedEpisode ->
                    navController.navigate(
                        Screen.VideoPlayer(
                            videoUrl = selectedEpisode.videoUrl ?: "",
                            title = selectedEpisode.title
                        )
                    )
                },
                onEpisodeClick = { episode ->
                    // Handle episode click if needed
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.VideoPlayer> { backStackEntry ->
            val videoPlayer = backStackEntry.toRoute<Screen.VideoPlayer>()
            com.rdwatch.androidtv.ui.videoplayer.VideoPlayerScreen(
                videoUrl = videoPlayer.videoUrl,
                title = videoPlayer.title,
                onBackPressed = {
                    navController.popBackStack()
                }
            )
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
            AuthGuard(
                onAuthenticationRequired = {
                    navController.navigate(Screen.Authentication) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                content = {
                    SearchScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onItemSelected = { movieId ->
                            navController.navigate(Screen.MovieDetails(movieId))
                        }
                    )
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
                viewModel = settingsViewModel,
                onBackPressed = {
                    navController.popBackStack()
                },
                onSignOut = {
                    settingsViewModel.signOut()
                    // Navigate to authentication and clear back stack
                    navController.navigate(Screen.Authentication) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToScreen = { screen ->
                    navController.navigate(screen)
                }
            )
        }
        
        composable<Screen.ScraperSettings>(
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
            ScraperSettingsScreen(
                onBackPressed = {
                    navController.popBackStack()
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
            AuthGuard(
                onAuthenticationRequired = {
                    navController.navigate(Screen.Authentication) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                content = {
                    ProfileScreen(
                        onMovieClick = { movie ->
                            navController.navigate(Screen.MovieDetails(movie.id.toString()))
                        },
                        onNavigateToScreen = { screen ->
                            navController.navigate(screen)
                        },
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
                }
            )
        }
        
        composable<Screen.AccountFileBrowser>(
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
            val accountFileBrowser = backStackEntry.toRoute<Screen.AccountFileBrowser>()
            AccountFileBrowserScreen(
                onFileClick = { file ->
                    // Navigate to video player if it's a playable file
                    if (file.isPlayable && file.streamUrl != null) {
                        navController.navigate(
                            Screen.VideoPlayer(
                                videoUrl = file.streamUrl,
                                title = file.name
                            )
                        )
                    }
                },
                onFolderClick = { folder ->
                    // Handle folder navigation if needed
                },
                onTorrentClick = { torrent ->
                    // Handle torrent navigation - this would show torrent files
                    // For now, this is handled within the screen itself
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.Error> { backStackEntry ->
            val error = backStackEntry.toRoute<Screen.Error>()
            com.rdwatch.androidtv.ui.error.ErrorScreen(
                message = error.message,
                canRetry = error.canRetry,
                onRetry = {
                    // Navigate back and let the previous screen handle retry
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}