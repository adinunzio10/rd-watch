package com.rdwatch.androidtv.player.error

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlayerErrorScreen(
    error: PlayerError,
    onRetry: () -> Unit,
    onGoBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(48.dp)
                .sizeIn(maxWidth = 600.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Error icon
                Icon(
                    imageVector = getErrorIcon(error),
                    contentDescription = null,
                    tint = getErrorColor(error),
                    modifier = Modifier.size(64.dp)
                )
                
                // Error title
                Text(
                    text = getErrorTitle(error),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                // Error message
                Text(
                    text = error.message,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (error.retryable) {
                        ErrorActionButton(
                            text = "Retry",
                            icon = Icons.Default.Refresh,
                            onClick = onRetry,
                            isPrimary = true
                        )
                    }
                    
                    ErrorActionButton(
                        text = "Go Back",
                        icon = Icons.Default.ArrowBack,
                        onClick = onGoBack
                    )
                    
                    ErrorActionButton(
                        text = "Dismiss",
                        icon = Icons.Default.Close,
                        onClick = onDismiss
                    )
                }
                
                // Additional info for debugging (only in debug builds)
                if (BuildConfig.DEBUG && error.originalException != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Debug: ${error.originalException.message}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        modifier = modifier
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .let { mod ->
                if (isFocused) {
                    mod.border(
                        2.dp,
                        Color.White,
                        RoundedCornerShape(8.dp)
                    )
                } else mod
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) Color.Red else Color.White.copy(alpha = 0.2f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getErrorIcon(error: PlayerError): ImageVector {
    return when (error) {
        is PlayerError.NetworkError -> Icons.Default.WifiOff
        is PlayerError.HttpError -> Icons.Default.CloudOff
        is PlayerError.FormatError -> Icons.Default.Movie
        is PlayerError.DecoderError -> Icons.Default.DeviceUnknown
        is PlayerError.DrmError -> Icons.Default.Lock
        is PlayerError.UnknownError -> Icons.Default.Error
    }
}

private fun getErrorColor(error: PlayerError): Color {
    return when (error) {
        is PlayerError.NetworkError -> Color.Yellow
        is PlayerError.HttpError -> Color.Red
        is PlayerError.FormatError -> Color.Magenta
        is PlayerError.DecoderError -> Color.Cyan
        is PlayerError.DrmError -> Color.Red
        is PlayerError.UnknownError -> Color.White
    }
}

private fun getErrorTitle(error: PlayerError): String {
    return when (error) {
        is PlayerError.NetworkError -> "Connection Error"
        is PlayerError.HttpError -> "Server Error"
        is PlayerError.FormatError -> "Format Error"
        is PlayerError.DecoderError -> "Playback Error"
        is PlayerError.DrmError -> "Content Protected"
        is PlayerError.UnknownError -> "Playback Error"
    }
}

// Create a simple BuildConfig object for preview
private object BuildConfig {
    const val DEBUG = true
}

@Preview(showBackground = true)
@Composable
private fun PlayerErrorScreenPreview() {
    PlayerErrorScreen(
        error = PlayerError.NetworkError(
            message = "Unable to connect to the server. Please check your internet connection and try again.",
            originalException = null,
            retryable = true
        ),
        onRetry = {},
        onGoBack = {},
        onDismiss = {}
    )
}