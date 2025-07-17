package com.rdwatch.androidtv.ui.filebrowser.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

/**
 * TV-optimized long press gesture handler for entering bulk selection mode
 * Handles both remote control and touch gestures with appropriate feedback
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongPressGestureHandler(
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    longPressDurationMs: Long = 800L, // Longer duration for TV remotes
    content: @Composable () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    var isLongPressing by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier.combinedClickable(
                enabled = enabled,
                onClick = {
                    if (!isLongPressing) {
                        onClick()
                    }
                    isLongPressing = false
                },
                onLongClick = {
                    isLongPressing = true
                    hapticFeedback.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                    )
                    onLongPress()
                },
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(),
            ),
    ) {
        content()
    }
}

/**
 * TV Remote D-Pad long press detector
 * Specifically designed for detecting long D-Pad center button presses
 */
@Composable
fun TVRemoteLongPressDetector(
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    longPressDurationMs: Long = 1000L, // TV remote requires longer hold
    content: @Composable () -> Unit,
) {
    var pressStartTime by remember { mutableLongStateOf(0L) }
    var isPressed by remember { mutableStateOf(false) }
    var hasTriggeredLongPress by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier.pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        pressStartTime = System.currentTimeMillis()
                        isPressed = true
                        hasTriggeredLongPress = false

                        // Wait for long press duration
                        try {
                            awaitPointerEventScope {
                                // Check if still pressed after duration
                                val startTime = System.currentTimeMillis()
                                while (isPressed && !hasTriggeredLongPress) {
                                    if (System.currentTimeMillis() - startTime >= longPressDurationMs) {
                                        hasTriggeredLongPress = true
                                        onLongPress()
                                        break
                                    }
                                    awaitPointerEvent()
                                }
                            }
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        if (!hasTriggeredLongPress) {
                            onClick()
                        }
                    },
                )
            },
    ) {
        content()
    }
}

/**
 * Progress indicator for long press gesture
 * Shows visual feedback during long press detection
 */
@Composable
fun LongPressProgressIndicator(
    isActive: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    // This could be implemented as a circular progress indicator
    // or a linear progress bar that appears during long press
    // For now, we'll rely on the TV focus indicators and haptic feedback
}
