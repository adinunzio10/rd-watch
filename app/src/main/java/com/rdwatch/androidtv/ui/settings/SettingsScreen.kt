package com.rdwatch.androidtv.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rdwatch.androidtv.presentation.navigation.Screen
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Settings Screen for Android TV with categorized settings Follows TV accessibility guidelines and
 * focus management
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onNavigateToScreen: (Screen) -> Unit = {},
) {
    val overscanMargin = 32.dp
    val firstFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Observe settings state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { firstFocusRequester.requestFocus() }

    Column(
        modifier =
            modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(overscanMargin),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Header
        SettingsHeader(onBackPressed = onBackPressed, firstFocusRequester = firstFocusRequester)

        // Settings content
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Playback Settings Section
            item {
                SettingsSection(title = "Playback Settings") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Video Quality
                        DropdownSetting(
                            title = "Video Quality",
                            subtitle = "Choose default video quality",
                            icon = Icons.Default.HighQuality,
                            currentValue = uiState.videoQuality.displayName,
                            options = VideoQuality.values().map { it.displayName },
                            onValueSelected = { selected ->
                                val quality =
                                    VideoQuality.values().find {
                                        it.displayName == selected
                                    } ?: VideoQuality.AUTO
                                viewModel.updateVideoQuality(quality)
                            },
                        )

                        // Playback Speed
                        DropdownSetting(
                            title = "Playback Speed",
                            subtitle = "Adjust video playback speed",
                            icon = Icons.Default.Speed,
                            currentValue = uiState.playbackSpeed.displayName,
                            options = PlaybackSpeed.values().map { it.displayName },
                            onValueSelected = { selected ->
                                val speed =
                                    PlaybackSpeed.values().find {
                                        it.displayName == selected
                                    } ?: PlaybackSpeed.NORMAL
                                viewModel.updatePlaybackSpeed(speed)
                            },
                        )

                        // Auto Play
                        SwitchSetting(
                            title = "Auto Play",
                            subtitle = "Automatically play next episode",
                            icon = Icons.Default.PlayArrow,
                            checked = uiState.autoPlay,
                            onCheckedChange = { viewModel.toggleAutoPlay(it) },
                        )

                        // Subtitles
                        SwitchSetting(
                            title = "Subtitles",
                            subtitle = "Enable subtitles by default",
                            icon = Icons.Default.Subtitles,
                            checked = uiState.subtitlesEnabled,
                            onCheckedChange = { viewModel.toggleSubtitles(it) },
                        )
                    }
                }
            }

            // Display Settings Section
            item {
                SettingsSection(title = "Display Settings") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Dark Mode
                        SwitchSetting(
                            title = "Dark Mode",
                            subtitle = "Use dark theme",
                            icon = Icons.Default.DarkMode,
                            checked = uiState.darkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                        )
                    }
                }
            }

            // Scrapers Section
            item {
                SettingsSection(title = "Content Sources") {
                    ActionSetting(
                        title = "Scrapers",
                        subtitle = "Manage content scrapers and sources",
                        icon = Icons.Default.Search,
                        onClick = { onNavigateToScreen(Screen.ScraperSettings) },
                    )
                }
            }

            // Privacy & Security Section
            item {
                SettingsSection(title = "Privacy & Security") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Parental Controls
                        SwitchSetting(
                            title = "Parental Controls",
                            subtitle = "Restrict content based on ratings",
                            icon = Icons.Default.ChildCare,
                            checked = uiState.parentalControlsEnabled,
                            onCheckedChange = { viewModel.toggleParentalControls(it) },
                        )

                        // Notifications
                        SwitchSetting(
                            title = "Notifications",
                            subtitle = "Receive app notifications",
                            icon = Icons.Default.Notifications,
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) },
                        )
                    }
                }
            }

            // Data Usage Section
            item {
                SettingsSection(title = "Data Usage") {
                    DropdownSetting(
                        title = "Data Usage Limit",
                        subtitle = "Control data consumption",
                        icon = Icons.Default.DataUsage,
                        currentValue = uiState.dataUsageLimit.displayName,
                        options = DataUsageLimit.values().map { it.displayName },
                        onValueSelected = { selected ->
                            val limit =
                                DataUsageLimit.values().find { it.displayName == selected }
                                    ?: DataUsageLimit.UNLIMITED
                            viewModel.updateDataUsageLimit(limit)
                        },
                    )
                }
            }

            // About Section
            item {
                SettingsSection(title = "About") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoSetting(
                            title = "App Version",
                            subtitle = "1.0.0",
                            icon = Icons.Default.Info,
                        )

                        InfoSetting(
                            title = "Build",
                            subtitle = "Release Build",
                            icon = Icons.Default.Build,
                        )

                        ActionSetting(
                            title = "Privacy Policy",
                            subtitle = "View our privacy policy",
                            icon = Icons.Default.Policy,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://real-debrid.com/privacy"))
                                context.startActivity(intent)
                            },
                        )

                        ActionSetting(
                            title = "Terms of Service",
                            subtitle = "View terms of service",
                            icon = Icons.Default.Description,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://real-debrid.com/terms"))
                                context.startActivity(intent)
                            },
                        )

                        ActionSetting(
                            title = "Sign Out",
                            subtitle = "Sign out of your Real Debrid account",
                            icon = Icons.Default.Logout,
                            onClick = onSignOut,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    onBackPressed: () -> Unit,
    firstFocusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button
        var backButtonFocused by remember { mutableStateOf(false) }

        TVFocusIndicator(isFocused = backButtonFocused) {
            IconButton(
                onClick = onBackPressed,
                modifier =
                    Modifier.focusRequester(firstFocusRequester)
                        .tvFocusable(
                            onFocusChanged = { backButtonFocused = it.isFocused },
                        ),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint =
                        if (backButtonFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                )
            }
        }

        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 2.dp,
        ) { Box(modifier = Modifier.padding(16.dp)) { content() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = { onCheckedChange(!checked) },
            modifier =
                Modifier.fillMaxWidth()
                    .tvFocusable(onFocusChanged = { isFocused = it.isFocused }),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.3f,
                            )
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint =
                            if (isFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor =
                                MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor =
                                MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(
    title: String,
    subtitle: String,
    icon: ImageVector,
    currentValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = { expanded = true },
            modifier =
                Modifier.fillMaxWidth()
                    .tvFocusable(onFocusChanged = { isFocused = it.isFocused }),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.3f,
                            )
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint =
                            if (isFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentValue,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            // Dropdown menu
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(text = option, style = MaterialTheme.typography.bodyLarge)
                        },
                        onClick = {
                            onValueSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSetting(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionSetting(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier =
                Modifier.fillMaxWidth()
                    .tvFocusable(onFocusChanged = { isFocused = it.isFocused }),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isFocused) {
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.3f,
                            )
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint =
                            if (isFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
