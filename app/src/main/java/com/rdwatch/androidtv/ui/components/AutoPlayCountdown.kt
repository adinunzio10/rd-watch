package com.rdwatch.androidtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rdwatch.androidtv.data.repository.NextEpisodeResult
import com.rdwatch.androidtv.ui.components.SmartTVImageLoader
import com.rdwatch.androidtv.ui.components.ImagePriority
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable
import kotlinx.coroutines.delay

/**
 * Auto-play countdown overlay that appears when an episode ends
 * Shows countdown timer and next episode information with user controls
 */
@Composable
fun AutoPlayCountdown(
    nextEpisode: NextEpisodeResult,
    showTitle: String,
    posterUrl: String? = null,
    countdownSeconds: Int = 10,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeLeft by remember { mutableIntStateOf(countdownSeconds) }
    val playNowFocusRequester = remember { FocusRequester() }

    // Countdown timer logic
    LaunchedEffect(countdownSeconds) {
        timeLeft = countdownSeconds
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        // Auto-play when countdown reaches zero
        if (timeLeft == 0) {
            onPlayNow()
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.6f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 32.dp,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Header
                Text(
                    text = "Next Episode",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                // Show/Episode info section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Thumbnail placeholder or poster
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (posterUrl != null) {
                            SmartTVImageLoader(
                                imageUrl = posterUrl,
                                contentDescription = showTitle,
                                contentScale = ContentScale.Crop,
                                priority = ImagePriority.NORMAL,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    // Episode details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = showTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = nextEpisode.getFormattedEpisodeId(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )

                        if (nextEpisode.episodeTitle != null) {
                            Text(
                                text = nextEpisode.episodeTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (nextEpisode.isNewSeason) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.wrapContentWidth(),
                            ) {
                                Text(
                                    text = "New Season",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                // Countdown circle and timer
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    // Circular progress background
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .border(
                                width = 6.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = CircleShape,
                            ),
                    )

                    // Countdown progress
                    CircularProgressIndicator(
                        progress = { (timeLeft.toFloat() / countdownSeconds.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.size(120.dp),
                        strokeWidth = 6.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                    )

                    // Timer text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = timeLeft.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AutoPlayCountdownButton(
                        text = "Cancel",
                        icon = Icons.Default.Close,
                        onClick = onCancel,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )

                    AutoPlayCountdownButton(
                        text = "Play Now",
                        icon = Icons.Default.PlayArrow,
                        onClick = onPlayNow,
                        isPrimary = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(playNowFocusRequester),
                    )
                }
            }
        }
    }

    // Auto-focus the Play Now button
    LaunchedEffect(Unit) {
        playNowFocusRequester.requestFocus()
    }
}

@Composable
private fun AutoPlayCountdownButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(
        isFocused = isFocused,
    ) {
        Button(
            onClick = onClick,
            modifier = modifier
                .height(48.dp)
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused },
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isPrimary) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isFocused) 8.dp else 2.dp,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}