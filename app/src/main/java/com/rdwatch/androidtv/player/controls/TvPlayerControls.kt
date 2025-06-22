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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.player.PlayerState
import com.rdwatch.androidtv.player.PlaybackState
import java.text.SimpleDateFormat
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
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .onKeyEvent { keyEvent ->
                    when {
                        keyEvent.type == KeyEventType.KeyDown -> {
                            when (keyEvent.key) {
                                Key.MediaPlay, Key.MediaPause, Key.MediaPlayPause, Key.Spacebar -> {
                                    onPlayPause()
                                    true
                                }
                                Key.DirectionLeft, Key.MediaRewind -> {
                                    onSeekBackward()
                                    true
                                }
                                Key.DirectionRight, Key.MediaFastForward -> {
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
                }
        ) {
            // Progress bar at the top
            TvProgressBar(
                currentPosition = playerState.currentPosition,
                duration = playerState.duration,
                onSeek = onSeek,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            )
            
            // Center controls
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvControlButton(
                    icon = Icons.Default.Replay10,
                    contentDescription = "Rewind 10 seconds",
                    onClick = onSeekBackward
                )
                
                TvControlButton(
                    icon = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    onClick = onPlayPause,
                    isPrimary = true
                )
                
                TvControlButton(
                    icon = Icons.Default.Forward10,
                    contentDescription = "Forward 10 seconds",
                    onClick = onSeekForward
                )
            }
            
            // Bottom information and controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                // Media title and time info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        playerState.title?.let { title ->
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        when (playerState.playbackState) {
                            PlaybackState.BUFFERING -> {
                                Text(
                                    text = "Buffering...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp
                                )
                            }
                            PlaybackState.READY -> {
                                Text(
                                    text = "${formatTime(playerState.currentPosition)} / ${formatTime(playerState.duration)}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp
                                )
                            }
                            else -> Unit
                        }
                    }
                    
                    // Secondary controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (playerState.playbackSpeed != 1.0f) {
                            Text(
                                text = "${playerState.playbackSpeed}x",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        TvControlButton(
                            icon = Icons.Default.Menu,
                            contentDescription = "Menu",
                            onClick = onMenuToggle,
                            isSmall = true
                        )
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
    isSmall: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val buttonSize = if (isSmall) 48.dp else if (isPrimary) 88.dp else 72.dp
    val iconSize = if (isSmall) 24.dp else if (isPrimary) 48.dp else 32.dp
    
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(if (isPrimary) 44.dp else if (isSmall) 24.dp else 36.dp))
            .background(
                when {
                    isFocused -> Color.White
                    isPrimary -> Color.White.copy(alpha = 0.9f)
                    else -> Color.White.copy(alpha = 0.3f)
                }
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused || isPrimary) Color.Black else Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun TvProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused) 8.dp else 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .focusable()
                .onFocusChanged { isFocused = it.isFocused },
            color = Color.Red,
            trackColor = Color.White.copy(alpha = 0.3f)
        )
        
        if (isFocused) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    fontSize = 14.sp
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
        playerState = PlayerState(
            isPlaying = true,
            playbackState = PlaybackState.READY,
            currentPosition = 60000L,
            duration = 300000L,
            title = "Sample Video Title",
            playbackSpeed = 1.0f
        ),
        isVisible = true,
        onPlayPause = {},
        onSeekBackward = {},
        onSeekForward = {},
        onSeek = {},
        onSpeedChange = {},
        onMenuToggle = {}
    )
}