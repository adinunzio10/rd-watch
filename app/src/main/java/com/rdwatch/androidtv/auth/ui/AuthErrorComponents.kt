package com.rdwatch.androidtv.auth.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthErrorCard(
    title: String,
    message: String,
    errorType: AuthErrorType,
    onRetry: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val retryFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }
    var retryHasFocus by remember { mutableStateOf(false) }
    var cancelHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        retryFocusRequester.requestFocus()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = errorType.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Button(
                    onClick = onRetry,
                    modifier =
                        Modifier
                            .focusRequester(retryFocusRequester)
                            .onFocusChanged { retryHasFocus = it.hasFocus }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp &&
                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                                ) {
                                    onRetry()
                                    true
                                } else {
                                    false
                                }
                            }
                            .border(
                                width = if (retryHasFocus) 3.dp else 0.dp,
                                color = if (retryHasFocus) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                            ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Text(
                        text = "Try Again",
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }

                if (onCancel != null) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier =
                            Modifier
                                .focusRequester(cancelFocusRequester)
                                .onFocusChanged { cancelHasFocus = it.hasFocus }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyUp &&
                                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                                    ) {
                                        onCancel()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .border(
                                    width = if (cancelHasFocus) 3.dp else 0.dp,
                                    color = if (cancelHasFocus) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                ),
                    ) {
                        Text(
                            text = "Cancel",
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthNetworkErrorDisplay(
    onRetry: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AuthErrorCard(
        title = "Network Connection Error",
        message = "Unable to connect to the authentication service. Please check your internet connection and try again.",
        errorType = AuthErrorType.NETWORK,
        onRetry = onRetry,
        onCancel = onCancel,
        modifier = modifier,
    )
}

@Composable
fun AuthTimeoutErrorDisplay(
    onRetry: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AuthErrorCard(
        title = "Authentication Timeout",
        message = "The authentication process took too long to complete. This usually happens when the server is busy. Please try again.",
        errorType = AuthErrorType.TIMEOUT,
        onRetry = onRetry,
        onCancel = onCancel,
        modifier = modifier,
    )
}

@Composable
fun AuthExpiredErrorDisplay(
    onRetry: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AuthErrorCard(
        title = "Authentication Code Expired",
        message = "Your authentication code has expired. Don't worry, we'll generate a new code for you.",
        errorType = AuthErrorType.EXPIRED,
        onRetry = onRetry,
        onCancel = onCancel,
        modifier = modifier,
    )
}

@Composable
fun AuthGenericErrorDisplay(
    message: String,
    onRetry: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AuthErrorCard(
        title = "Authentication Error",
        message = message,
        errorType = AuthErrorType.GENERIC,
        onRetry = onRetry,
        onCancel = onCancel,
        modifier = modifier,
    )
}

enum class AuthErrorType(val icon: ImageVector) {
    NETWORK(Icons.Default.NetworkCheck),
    TIMEOUT(Icons.Default.Timer),
    EXPIRED(Icons.Default.Warning),
    GENERIC(Icons.Default.Error),
}
