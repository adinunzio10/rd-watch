package com.rdwatch.androidtv.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.player.PlaybackState
import com.rdwatch.androidtv.player.PlayerState
import com.rdwatch.androidtv.ui.focus.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun TvPlayerControls(
    playerState: PlayerState,
    isVisible: Boolean,
    onPlayPause: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onMenuToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Initialize focus management
    val focusManager = rememberTVFocusManager()
    val progressBarFocus = remember { FocusRequester() }
    val rewindFocus = remember { FocusRequester() }
    val playPauseFocus = remember { FocusRequester() }
    val forwardFocus = remember { FocusRequester() }
    val menuFocus = remember { FocusRequester() }

    // Track current focus group: 0=progress, 1=center_controls, 2=menu
    var currentFocusGroup by remember { mutableStateOf(1) }
    var currentCenterControlIndex by remember { mutableStateOf(1) } // 0=rewind, 1=play, 2=forward
    var showSpeedIndicator by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var showActionFeedback by remember { mutableStateOf("") }

    // Show speed indicator temporarily when speed changes
    LaunchedEffect(playerState.playbackSpeed) {
        if (playerState.playbackSpeed != currentSpeed) {
            currentSpeed = playerState.playbackSpeed
            showSpeedIndicator = true
            delay(2000) // Show for 2 seconds
            showSpeedIndicator = false
        }
    }

    // Auto-focus play button when controls become visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(100)
            playPauseFocus.requestFocus()
            currentFocusGroup = 1
            currentCenterControlIndex = 1
        }
    }

    if (isVisible) {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .onKeyEvent { keyEvent ->
                        when {
                            keyEvent.type == KeyEventType.KeyDown -> {
                                when (keyEvent.key) {
                                    // Vertical navigation between control groups
                                    Key.DirectionUp -> {
                                        when (currentFocusGroup) {
                                            1 -> { // From center controls to progress bar
                                                currentFocusGroup = 0
                                                progressBarFocus.requestFocus()
                                            }
                                            2 -> { // From menu to center controls
                                                currentFocusGroup = 1
                                                when (currentCenterControlIndex) {
                                                    0 -> rewindFocus.requestFocus()
                                                    1 -> playPauseFocus.requestFocus()
                                                    2 -> forwardFocus.requestFocus()
                                                }
                                            }
                                        }
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        when (currentFocusGroup) {
                                            0 -> { // From progress bar to center controls
                                                currentFocusGroup = 1
                                                when (currentCenterControlIndex) {
                                                    0 -> rewindFocus.requestFocus()
                                                    1 -> playPauseFocus.requestFocus()
                                                    2 -> forwardFocus.requestFocus()
                                                }
                                            }
                                            1 -> { // From center controls to menu
                                                currentFocusGroup = 2
                                                menuFocus.requestFocus()
                                            }
                                        }
                                        true
                                    }
                                    // Horizontal navigation within center controls
                                    Key.DirectionLeft -> {
                                        when (currentFocusGroup) {
                                            1 -> { // Center controls navigation
                                                when (currentCenterControlIndex) {
                                                    1 -> { // From play to rewind
                                                        currentCenterControlIndex = 0
                                                        rewindFocus.requestFocus()
                                                    }
                                                    2 -> { // From forward to play
                                                        currentCenterControlIndex = 1
                                                        playPauseFocus.requestFocus()
                                                    }
                                                }
                                            }
                                            else -> {
                                                // Fallback to seek backward
                                                onSeekBackward()
                                                showSpeedIndicator = true
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    delay(1500)
                                                    showSpeedIndicator = false
                                                }
                                            }
                                        }
                                        true
                                    }
                                    Key.DirectionRight -> {
                                        when (currentFocusGroup) {
                                            1 -> { // Center controls navigation
                                                when (currentCenterControlIndex) {
                                                    0 -> { // From rewind to play
                                                        currentCenterControlIndex = 1
                                                        playPauseFocus.requestFocus()
                                                    }
                                                    1 -> { // From play to forward
                                                        currentCenterControlIndex = 2
                                                        forwardFocus.requestFocus()
                                                    }
                                                }
                                            }
                                            else -> {
                                                // Fallback to seek forward
                                                onSeekForward()
                                                showSpeedIndicator = true
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    delay(1500)
                                                    showSpeedIndicator = false
                                                }
                                            }
                                        }
                                        true
                                    }
                                    // Action keys
                                    Key.DirectionCenter, Key.Enter -> {
                                        when (currentFocusGroup) {
                                            0 -> false // Let progress bar handle it
                                            1 -> {
                                                when (currentCenterControlIndex) {
                                                    0 -> {
                                                        onSeekBackward()
                                                        showSpeedIndicator = true
                                                        CoroutineScope(Dispatchers.Main).launch {
                                                            delay(1500)
                                                            showSpeedIndicator = false
                                                        }
                                                    }
                                                    1 -> {
                                                        showActionFeedback = if (playerState.isPlaying) "Pausing..." else "Playing..."
                                                        onPlayPause()
                                                        // Clear feedback after short delay
                                                        CoroutineScope(Dispatchers.Main).launch {
                                                            delay(500)
                                                            showActionFeedback = ""
                                                        }
                                                    }
                                                    2 -> {
                                                        onSeekForward()
                                                        showSpeedIndicator = true
                                                        CoroutineScope(Dispatchers.Main).launch {
                                                            delay(1500)
                                                            showSpeedIndicator = false
                                                        }
                                                    }
                                                }
                                            }
                                            2 -> onMenuToggle()
                                        }
                                        true
                                    }
                                    Key.MediaPlay, Key.MediaPause, Key.MediaPlayPause, Key.Spacebar -> {
                                        onPlayPause()
                                        true
                                    }
                                    Key.MediaRewind -> {
                                        onSeekBackward()
                                        true
                                    }
                                    Key.MediaFastForward -> {
                                        onSeekForward()
                                        true
                                    }
                                    Key.Menu -> {
                                        onMenuToggle()
                                        true
                                    }
                                    else -> false
                                }
                            }
                            else -> false
                        }
                    },
        ) {
            // Progress bar at the top
            TvProgressBar(
                currentPosition = playerState.currentPosition,
                duration = playerState.duration,
                onSeek = onSeek,
                focusRequester = progressBarFocus,
                isFocused = currentFocusGroup == 0,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 32.dp),
            )

            // Center controls
            Row(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TVFocusIndicator(
                    isFocused = currentFocusGroup == 1 && currentCenterControlIndex == 0,
                ) {
                    TvControlButton(
                        icon = Icons.Default.Replay,
                        contentDescription = "Rewind 10 seconds",
                        onClick = {
                            onSeekBackward()
                            showSpeedIndicator = true
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(1500)
                                showSpeedIndicator = false
                            }
                        },
                        focusRequester = rewindFocus,
                        isFocused = currentFocusGroup == 1 && currentCenterControlIndex == 0,
                        onFocusChanged = {
                            if (it.isFocused) {
                                currentFocusGroup = 1
                                currentCenterControlIndex = 0
                            }
                        },
                    )
                }

                TVFocusIndicator(
                    isFocused = currentFocusGroup == 1 && currentCenterControlIndex == 1,
                ) {
                    TvControlButton(
                        icon = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        onClick = {
                            showActionFeedback = if (playerState.isPlaying) "Pausing..." else "Playing..."
                            onPlayPause()
                            // Clear feedback after short delay
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(500)
                                showActionFeedback = ""
                            }
                        },
                        isPrimary = true,
                        focusRequester = playPauseFocus,
                        isFocused = currentFocusGroup == 1 && currentCenterControlIndex == 1,
                        onFocusChanged = {
                            if (it.isFocused) {
                                currentFocusGroup = 1
                                currentCenterControlIndex = 1
                            }
                        },
                    )
                }

                TVFocusIndicator(
                    isFocused = currentFocusGroup == 1 && currentCenterControlIndex == 2,
                ) {
                    TvControlButton(
                        icon = Icons.Default.FastForward,
                        contentDescription = "Forward 10 seconds",
                        onClick = {
                            onSeekForward()
                            showSpeedIndicator = true
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(1500)
                                showSpeedIndicator = false
                            }
                        },
                        focusRequester = forwardFocus,
                        isFocused = currentFocusGroup == 1 && currentCenterControlIndex == 2,
                        onFocusChanged = {
                            if (it.isFocused) {
                                currentFocusGroup = 1
                                currentCenterControlIndex = 2
                            }
                        },
                    )
                }
            }

            // Action feedback overlay (centered above controls)
            if (showActionFeedback.isNotEmpty()) {
                Card(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = (-80).dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.8f),
                        ),
                ) {
                    Text(
                        text = showActionFeedback,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // Speed indicator overlay
            if (showSpeedIndicator && (playerState.playbackSpeed != 1.0f || showSpeedIndicator)) {
                Card(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = (-120).dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.8f),
                        ),
                ) {
                    Text(
                        text = "${playerState.playbackSpeed}x",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // Bottom information and controls
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 32.dp),
            ) {
                // Media title and time info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        playerState.title?.let { title ->
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        when (playerState.playbackState) {
                            PlaybackState.BUFFERING -> {
                                Text(
                                    text = "Buffering...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                )
                            }
                            PlaybackState.READY -> {
                                Text(
                                    text = "${formatTime(playerState.currentPosition)} / ${formatTime(playerState.duration)}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                )
                            }
                            else -> Unit
                        }
                    }

                    // Secondary controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (playerState.playbackSpeed != 1.0f) {
                            Text(
                                text = "${playerState.playbackSpeed}x",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier =
                                    Modifier
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }

                        TVFocusIndicator(
                            isFocused = currentFocusGroup == 2,
                        ) {
                            TvControlButton(
                                icon = Icons.Default.Menu,
                                contentDescription = "Menu",
                                onClick = onMenuToggle,
                                isSmall = true,
                                focusRequester = menuFocus,
                                isFocused = currentFocusGroup == 2,
                                onFocusChanged = {
                                    if (it.isFocused) {
                                        currentFocusGroup = 2
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isSmall: Boolean = false,
    focusRequester: FocusRequester? = null,
    isFocused: Boolean = false,
    onFocusChanged: ((androidx.compose.ui.focus.FocusState) -> Unit)? = null,
) {
    var internalFocused by remember { mutableStateOf(false) }
    val effectiveFocused = isFocused || internalFocused

    val buttonSize =
        if (isSmall) {
            48.dp
        } else if (isPrimary) {
            88.dp
        } else {
            72.dp
        }
    val iconSize =
        if (isSmall) {
            24.dp
        } else if (isPrimary) {
            48.dp
        } else {
            32.dp
        }

    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                .size(buttonSize)
                .clip(
                    RoundedCornerShape(
                        if (isPrimary) {
                            44.dp
                        } else if (isSmall) {
                            24.dp
                        } else {
                            36.dp
                        },
                    ),
                )
                .background(
                    when {
                        effectiveFocused -> Color.White
                        isPrimary -> Color.White.copy(alpha = 0.9f)
                        else -> Color.White.copy(alpha = 0.3f)
                    },
                )
                .focusable()
                .onFocusChanged { focusState ->
                    internalFocused = focusState.isFocused
                    onFocusChanged?.invoke(focusState)
                },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (effectiveFocused || isPrimary) Color.Black else Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun TvProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    isFocused: Boolean = false,
) {
    var internalFocused by remember { mutableStateOf(false) }
    val effectiveFocused = isFocused || internalFocused

    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                    .fillMaxWidth()
                    .height(if (effectiveFocused) 8.dp else 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .focusable()
                    .onFocusChanged { internalFocused = it.isFocused }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && effectiveFocused) {
                            when (keyEvent.key) {
                                Key.DirectionLeft -> {
                                    val newPosition = (currentPosition - 10000L).coerceAtLeast(0L)
                                    onSeek(newPosition)
                                    true
                                }
                                Key.DirectionRight -> {
                                    val newPosition = (currentPosition + 10000L).coerceAtMost(duration)
                                    onSeek(newPosition)
                                    true
                                }
                                Key.DirectionCenter, Key.Enter -> {
                                    // Could implement scrubbing mode here
                                    false
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
            color = if (effectiveFocused) Color.Red else Color.Red.copy(alpha = 0.8f),
            trackColor = Color.White.copy(alpha = if (effectiveFocused) 0.5f else 0.3f),
        )

        if (effectiveFocused) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    fontSize = 14.sp,
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val seconds = (timeMs / 1000).toInt()
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

@Preview(showBackground = true)
@Composable
private fun TvPlayerControlsPreview() {
    TvPlayerControls(
        playerState =
            PlayerState(
                isPlaying = true,
                playbackState = PlaybackState.READY,
                currentPosition = 60000L,
                duration = 300000L,
                title = "Sample Video Title",
                playbackSpeed = 1.0f,
            ),
        isVisible = true,
        onPlayPause = {},
        onSeekBackward = {},
        onSeekForward = {},
        onSeek = {},
        onSpeedChange = {},
        onMenuToggle = {},
    )
}
