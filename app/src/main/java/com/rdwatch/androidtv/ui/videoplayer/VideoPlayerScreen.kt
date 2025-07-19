package com.rdwatch.androidtv.ui.videoplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.player.ExoPlayerManager
import com.rdwatch.androidtv.player.TvPlayerView
import com.rdwatch.androidtv.player.subtitle.SubtitleManager
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.ui.viewmodel.PlaybackViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@UnstableApi
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    title: String,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    videoPlayerViewModel: VideoPlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by videoPlayerViewModel.uiState.collectAsState()
    val playbackUiState by playbackViewModel.uiState.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()

    // Initialize the video when the screen loads
    LaunchedEffect(videoUrl, title) {
        videoPlayerViewModel.initializeVideo(videoUrl, title)
    }

    // Handle back navigation with confirmation if video is playing
    BackHandler(enabled = true) {
        if (playerState.isPlaying) {
            videoPlayerViewModel.showExitConfirmation()
        } else {
            onBackPressed()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                LoadingScreen(
                    title = title,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.hasError -> {
                ErrorScreen(
                    title = title,
                    error = uiState.errorMessage ?: "Unknown error occurred",
                    onRetry = { videoPlayerViewModel.retry(videoUrl, title) },
                    onBack = onBackPressed,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.hasVideo -> {
                uiState.exoPlayerManager?.let { exoPlayerManager ->
                    uiState.subtitleManager?.let { subtitleManager ->
                        TvPlayerView(
                            exoPlayerManager = exoPlayerManager,
                            subtitleManager = subtitleManager,
                            onMenuToggle = {
                                videoPlayerViewModel.togglePlayerMenu()
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        // Exit confirmation dialog
        if (uiState.showExitConfirmation) {
            ExitConfirmationDialog(
                onConfirm = {
                    videoPlayerViewModel.dismissExitConfirmation()
                    onBackPressed()
                },
                onDismiss = {
                    videoPlayerViewModel.dismissExitConfirmation()
                },
            )
        }

        // Resume dialog
        if (playbackUiState.showResumeDialog && playbackUiState.resumePosition != null) {
            ResumeDialog(
                resumePosition = playbackUiState.formattedResumePosition,
                onResume = playbackViewModel::resumeFromDialog,
                onRestart = playbackViewModel::restartFromBeginning,
                onDismiss = playbackViewModel::dismissResumeDialog,
            )
        }
    }
}

@Composable
private fun LoadingScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Loading $title...",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ErrorScreen(
    title: String,
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Error Playing $title",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
            ) {
                Text("Go Back")
            }

            Button(
                onClick = onRetry,
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Exit Video Player")
        },
        text = {
            Text("Are you sure you want to exit? Your progress will be saved.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Exit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue Watching")
            }
        },
    )
}

@Composable
private fun ResumeDialog(
    resumePosition: String,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Resume Playback")
        },
        text = {
            Text("Resume from $resumePosition or start from the beginning?")
        },
        confirmButton = {
            TextButton(onClick = onResume) {
                Text("Resume")
            }
        },
        dismissButton = {
            TextButton(onClick = onRestart) {
                Text("Start Over")
            }
        },
    )
}

@HiltViewModel
class VideoPlayerViewModel
    @Inject
    constructor(
        private val exoPlayerManager: ExoPlayerManager,
        private val subtitleManager: SubtitleManager,
    ) : BaseViewModel<VideoPlayerUiState>() {
        override fun createInitialState(): VideoPlayerUiState {
            return VideoPlayerUiState()
        }

        fun initializeVideo(
            videoUrl: String,
            title: String,
        ) {
            updateState { copy(isLoading = true, errorMessage = null, title = title) }

            launchSafely(
                onError = { exception ->
                    updateState {
                        copy(
                            isLoading = false,
                            hasError = true,
                            errorMessage = "Failed to load video: ${exception.message}",
                        )
                    }
                },
            ) {
                try {
                    // Initialize ExoPlayer with the video URL
                    exoPlayerManager.prepareMedia(videoUrl, title)

                    updateState {
                        copy(
                            isLoading = false,
                            hasVideo = true,
                            hasError = false,
                            exoPlayerManager = exoPlayerManager,
                            subtitleManager = subtitleManager,
                            videoUrl = videoUrl,
                            title = title,
                        )
                    }
                } catch (e: Exception) {
                    updateState {
                        copy(
                            isLoading = false,
                            hasError = true,
                            errorMessage = "Failed to initialize video player: ${e.message}",
                        )
                    }
                }
            }
        }

        fun retry(
            videoUrl: String,
            title: String,
        ) {
            initializeVideo(videoUrl, title)
        }

        fun showExitConfirmation() {
            updateState { copy(showExitConfirmation = true) }
        }

        fun dismissExitConfirmation() {
            updateState { copy(showExitConfirmation = false) }
        }

        fun togglePlayerMenu() {
            updateState { copy(showPlayerMenu = !showPlayerMenu) }
        }

        override fun handleError(exception: Throwable) {
            updateState {
                copy(
                    isLoading = false,
                    hasError = true,
                    errorMessage = "An error occurred: ${exception.message}",
                )
            }
        }
    }

data class VideoPlayerUiState(
    val isLoading: Boolean = false,
    val hasVideo: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val title: String = "",
    val videoUrl: String = "",
    val showExitConfirmation: Boolean = false,
    val showPlayerMenu: Boolean = false,
    val exoPlayerManager: ExoPlayerManager? = null,
    val subtitleManager: SubtitleManager? = null,
)
