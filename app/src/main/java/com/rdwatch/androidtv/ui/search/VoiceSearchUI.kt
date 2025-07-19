package com.rdwatch.androidtv.ui.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Voice search UI component with TV-optimized visual feedback
 */
@Composable
fun VoiceSearchUI(
    onCancel: () -> Unit,
    listeningState: VoiceSearchState,
    modifier: Modifier = Modifier,
    partialText: String = "",
    recognizedText: String = "",
    error: String? = null,
    suggestions: List<String> = emptyList(),
) {
    val cancelButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelButtonFocusRequester.requestFocus()
    }

    Surface(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(0.8f)
                        .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Voice Search",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )

                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.focusRequester(cancelButtonFocusRequester),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    // Voice indicator with animation
                    VoiceIndicator(
                        state = listeningState,
                        modifier = Modifier.size(120.dp),
                    )

                    // Status text
                    VoiceStatusText(
                        state = listeningState,
                        error = error,
                    )

                    // Partial/recognized text display
                    VoiceTextDisplay(
                        partialText = partialText,
                        recognizedText = recognizedText,
                        state = listeningState,
                    )

                    // Error display
                    error?.let { errorMessage ->
                        VoiceErrorDisplay(
                            error = errorMessage,
                            onRetry = { /* Could trigger retry */ },
                        )
                    }

                    // Suggestions when idle or error
                    if (listeningState == VoiceSearchState.IDLE || listeningState == VoiceSearchState.ERROR) {
                        VoiceSuggestions(suggestions = suggestions)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceIndicator(
    state: VoiceSearchState,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_animation")

    // Pulsing animation for listening state
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse_scale",
    )

    // Color animation
    val pulseColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary,
        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse_color",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Outer pulsing circle (only when listening)
        if (state == VoiceSearchState.LISTENING) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .clip(CircleShape)
                        .background(pulseColor.copy(alpha = 0.3f)),
            )
        }

        // Inner circle with icon
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color =
                when (state) {
                    VoiceSearchState.LISTENING -> MaterialTheme.colorScheme.primary
                    VoiceSearchState.PROCESSING -> MaterialTheme.colorScheme.secondary
                    VoiceSearchState.COMPLETED -> MaterialTheme.colorScheme.tertiary
                    VoiceSearchState.ERROR -> MaterialTheme.colorScheme.error
                    VoiceSearchState.IDLE -> MaterialTheme.colorScheme.surfaceVariant
                },
            shadowElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    VoiceSearchState.LISTENING -> {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Listening",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    VoiceSearchState.PROCESSING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 3.dp,
                        )
                    }
                    VoiceSearchState.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    VoiceSearchState.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    VoiceSearchState.IDLE -> {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Idle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceStatusText(
    state: VoiceSearchState,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val statusText =
        when {
            error != null -> "Error occurred"
            state == VoiceSearchState.LISTENING -> "Listening..."
            state == VoiceSearchState.PROCESSING -> "Processing..."
            state == VoiceSearchState.COMPLETED -> "Search completed"
            state == VoiceSearchState.IDLE -> "Ready to listen"
            else -> "Voice search"
        }

    val statusColor =
        when {
            error != null -> MaterialTheme.colorScheme.error
            state == VoiceSearchState.LISTENING -> MaterialTheme.colorScheme.primary
            state == VoiceSearchState.PROCESSING -> MaterialTheme.colorScheme.secondary
            state == VoiceSearchState.COMPLETED -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurface
        }

    Text(
        text = statusText,
        style = MaterialTheme.typography.titleMedium,
        color = statusColor,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun VoiceTextDisplay(
    partialText: String,
    recognizedText: String,
    state: VoiceSearchState,
    modifier: Modifier = Modifier,
) {
    val displayText =
        when {
            recognizedText.isNotEmpty() -> recognizedText
            partialText.isNotEmpty() && state == VoiceSearchState.LISTENING -> partialText
            else -> ""
        }

    AnimatedVisibility(
        visible = displayText.isNotEmpty(),
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (partialText.isNotEmpty() && recognizedText.isEmpty()) "Hearing:" else "Recognized:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = if (recognizedText.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun VoiceErrorDisplay(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Voice Search Error",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun VoiceSuggestions(
    suggestions: List<String>,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isNotEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Try saying:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )

            suggestions.take(3).forEach { suggestion ->
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * Simplified voice button for inline use
 */
@Composable
fun VoiceSearchButton(
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    isListening: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale",
    )

    IconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.scale(scale),
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = if (isListening) "Stop voice search" else "Start voice search",
            tint =
                if (isListening) {
                    MaterialTheme.colorScheme.primary
                } else if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
        )
    }
}

/**
 * Voice waveform animation for visual feedback
 */
@Composable
fun VoiceWaveform(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            val animationDelay = index * 100
            val height by infiniteTransition.animateFloat(
                initialValue = 4.dp.value,
                targetValue = if (isActive) 16.dp.value else 4.dp.value,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = 600,
                                delayMillis = animationDelay,
                                easing = LinearEasing,
                            ),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "wave_$index",
            )

            Box(
                modifier =
                    Modifier
                        .width(3.dp)
                        .height(height.dp)
                        .background(
                            color =
                                if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                },
                            shape = RoundedCornerShape(1.5.dp),
                        ),
            )
        }
    }
}
