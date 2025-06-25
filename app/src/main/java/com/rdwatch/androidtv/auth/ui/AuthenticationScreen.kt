package com.rdwatch.androidtv.auth.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rdwatch.androidtv.auth.models.AuthState
import com.rdwatch.androidtv.auth.models.DeviceCodeInfo
import kotlinx.coroutines.delay

@Composable
fun AuthenticationScreen(
    onAuthenticationSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val qrCodeBitmap by viewModel.qrCodeBitmap.collectAsStateWithLifecycle()
    
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthenticationSuccess()
        }
    }
    
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = authState) {
            is AuthState.Initializing -> {
                InitializingContent()
            }
            is AuthState.Unauthenticated -> {
                UnauthenticatedContent(
                    onStartAuthentication = { viewModel.startAuthentication() }
                )
            }
            is AuthState.WaitingForUser -> {
                WaitingForUserContent(
                    deviceCodeInfo = state.deviceCodeInfo,
                    qrCodeBitmap = qrCodeBitmap,
                    onRetry = { viewModel.startAuthentication() }
                )
            }
            is AuthState.Authenticated -> {
                AuthenticatedContent()
            }
            is AuthState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.startAuthentication() }
                )
            }
        }
    }
}

@Composable
private fun InitializingContent() {
    Card(
        modifier = Modifier.fillMaxWidth(0.8f),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Setting up authentication...",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Please wait while we prepare your authentication",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnauthenticatedContent(
    onStartAuthentication: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.8f),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Authentication Required",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Authentication Required",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Please sign in to access Real Debrid features",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onStartAuthentication,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Start Authentication",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun WaitingForUserContent(
    deviceCodeInfo: DeviceCodeInfo,
    qrCodeBitmap: Bitmap?,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var timeLeft by remember { mutableIntStateOf(deviceCodeInfo.expiresIn) }
    var isExpired by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    
    // Handle lifecycle events to pause/resume timer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> isPaused = true
                Lifecycle.Event.ON_RESUME -> isPaused = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Enhanced countdown timer with lifecycle handling
    LaunchedEffect(timeLeft, isPaused) {
        if (timeLeft > 0 && !isPaused && !isExpired) {
            delay(1000L)
            timeLeft--
        } else if (timeLeft <= 0 && !isExpired) {
            isExpired = true
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Authenticate with Real Debrid",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // QR Code Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan QR Code",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    qrCodeBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code for authentication",
                            modifier = Modifier.size(300.dp)
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier.size(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                // OR Divider
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Manual Code Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Code Manually",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Visit:",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = deviceCodeInfo.verificationUri,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Enter code:",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = deviceCodeInfo.userCode,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Enhanced timer section with better visual hierarchy
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isExpired -> MaterialTheme.colorScheme.errorContainer
                        timeLeft < 120 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        timeLeft < 300 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Timer icon that changes based on state
                        Icon(
                            imageVector = if (isExpired) Icons.Default.Warning else Icons.Default.Schedule,
                            contentDescription = if (isExpired) "Expired" else "Timer",
                            modifier = Modifier.size(32.dp),
                            tint = when {
                                isExpired -> MaterialTheme.colorScheme.error
                                timeLeft < 60 -> MaterialTheme.colorScheme.error
                                timeLeft < 120 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        
                        // Circular progress indicator
                        CircularProgressIndicator(
                            progress = { if (isExpired) 0f else timeLeft.toFloat() / deviceCodeInfo.expiresIn },
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 6.dp,
                            color = when {
                                isExpired -> MaterialTheme.colorScheme.error
                                timeLeft < 60 -> MaterialTheme.colorScheme.error
                                timeLeft < 120 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        
                        Column {
                            Text(
                                text = if (isExpired) "Code Expired" else "Code expires in:",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = when {
                                    isExpired -> MaterialTheme.colorScheme.error
                                    timeLeft < 60 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = if (isExpired) "00:00" else formatTime(timeLeft),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = when {
                                    isExpired -> MaterialTheme.colorScheme.error
                                    timeLeft < 60 -> MaterialTheme.colorScheme.error
                                    timeLeft < 120 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                    
                    // Status message based on timer state
                    if (isExpired) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "The authentication code has expired. Click 'Get New Code' to generate a fresh code.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else if (timeLeft < 120) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "⚠️ Code expires soon! Please complete authentication quickly.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center,
                            color = if (timeLeft < 60) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons section
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExpired) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Get New Code",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Waiting for authentication...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✓ Authentication Successful!",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Welcome to RD Watch",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    val retryFocusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        retryFocusRequester.requestFocus()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Authentication Error",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = getErrorMessage(message),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error description with helpful information
            Text(
                text = getErrorDescription(message),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .focusRequester(retryFocusRequester)
                    .onFocusChanged { hasFocus = it.hasFocus }
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
                        width = if (hasFocus) 3.dp else 0.dp,
                        color = if (hasFocus) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Try Again",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

private fun getErrorMessage(message: String): String {
    return when {
        message.contains("network", ignoreCase = true) -> "Network Connection Error"
        message.contains("timeout", ignoreCase = true) -> "Request Timed Out"
        message.contains("unauthorized", ignoreCase = true) -> "Authentication Failed"
        message.contains("forbidden", ignoreCase = true) -> "Access Denied"
        message.contains("not found", ignoreCase = true) -> "Service Not Found"
        message.contains("server", ignoreCase = true) -> "Server Error"
        message.contains("expired", ignoreCase = true) -> "Authentication Expired"
        message.contains("invalid", ignoreCase = true) -> "Invalid Request"
        else -> "Authentication Error"
    }
}

private fun getErrorDescription(message: String): String {
    return when {
        message.contains("network", ignoreCase = true) -> 
            "Please check your internet connection and try again."
        message.contains("timeout", ignoreCase = true) -> 
            "The request took too long to complete. Please try again."
        message.contains("unauthorized", ignoreCase = true) -> 
            "Your credentials are invalid. Please check and try again."
        message.contains("forbidden", ignoreCase = true) -> 
            "You don't have permission to access this service."
        message.contains("not found", ignoreCase = true) -> 
            "The authentication service is currently unavailable."
        message.contains("server", ignoreCase = true) -> 
            "There's a problem with the server. Please try again later."
        message.contains("expired", ignoreCase = true) -> 
            "Your authentication code has expired. A new code will be generated."
        message.contains("invalid", ignoreCase = true) -> 
            "There was an error with the authentication request."
        else -> "An unexpected error occurred during authentication. Please try again."
    }
}