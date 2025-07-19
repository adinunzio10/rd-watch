package com.rdwatch.androidtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

@Composable
fun ResumeDialog(
    title: String,
    resumePosition: String,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resumeFocusRequester = remember { FocusRequester() }

    // Auto-focus on resume button when dialog opens
    LaunchedEffect(Unit) {
        resumeFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Surface(
            modifier =
                modifier
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Dialog title
                Text(
                    text = "Resume Watching",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                // Content title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )

                // Resume position info
                Text(
                    text = "Continue from $resumePosition?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Resume button
                    ResumeDialogButton(
                        text = "Resume",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            onResumeClick()
                            onDismiss()
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(resumeFocusRequester),
                        isPrimary = true,
                    )

                    // Start over button
                    ResumeDialogButton(
                        text = "Start Over",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            onRestartClick()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeDialogButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(
        isFocused = isFocused,
    ) {
        Button(
            onClick = onClick,
            modifier =
                modifier
                    .height(56.dp)
                    .tvFocusable(
                        onFocusChanged = { isFocused = it.isFocused },
                    ),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        if (isPrimary) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    contentColor =
                        if (isPrimary) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondary
                        },
                ),
            elevation =
                ButtonDefaults.buttonElevation(
                    defaultElevation = if (isFocused) 12.dp else 4.dp,
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
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun ResumeDialogOverlay(
    showDialog: Boolean,
    title: String,
    resumePosition: String,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        // Backdrop overlay
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            ResumeDialog(
                title = title,
                resumePosition = resumePosition,
                onResumeClick = onResumeClick,
                onRestartClick = onRestartClick,
                onDismiss = onDismiss,
            )
        }
    }
}
