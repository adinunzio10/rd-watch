package com.rdwatch.androidtv.ui.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Settings Screen for Android TV with categorized settings Follows TV accessibility guidelines and
 * focus management
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onBackPressed: () -> Unit = {}) {
    val overscanMargin = 32.dp
    val firstFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Settings state management
    var videoQuality by remember { mutableStateOf(VideoQuality.AUTO) }
    var playbackSpeed by remember { mutableStateOf(PlaybackSpeed.NORMAL) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var autoPlay by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(true) }
    var parentalControlsEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var dataUsageLimit by remember { mutableStateOf(DataUsageLimit.UNLIMITED) }

    LaunchedEffect(Unit) { firstFocusRequester.requestFocus() }

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(overscanMargin),
            verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        SettingsHeader(onBackPressed = onBackPressed, firstFocusRequester = firstFocusRequester)

        // Settings content
        LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
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
                                currentValue = videoQuality.displayName,
                                options = VideoQuality.values().map { it.displayName },
                                onValueSelected = { selected ->
                                    videoQuality =
                                            VideoQuality.values().find {
                                                it.displayName == selected
                                            }
                                                    ?: VideoQuality.AUTO
                                }
                        )

                        // Playback Speed
                        DropdownSetting(
                                title = "Playback Speed",
                                subtitle = "Adjust video playback speed",
                                icon = Icons.Default.Speed,
                                currentValue = playbackSpeed.displayName,
                                options = PlaybackSpeed.values().map { it.displayName },
                                onValueSelected = { selected ->
                                    playbackSpeed =
                                            PlaybackSpeed.values().find {
                                                it.displayName == selected
                                            }
                                                    ?: PlaybackSpeed.NORMAL
                                }
                        )

                        // Auto Play
                        SwitchSetting(
                                title = "Auto Play",
                                subtitle = "Automatically play next episode",
                                icon = Icons.Default.PlayArrow,
                                checked = autoPlay,
                                onCheckedChange = { autoPlay = it }
                        )

                        // Subtitles
                        SwitchSetting(
                                title = "Subtitles",
                                subtitle = "Enable subtitles by default",
                                icon = Icons.Default.Subtitles,
                                checked = subtitlesEnabled,
                                onCheckedChange = { subtitlesEnabled = it }
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
                                checked = darkMode,
                                onCheckedChange = { darkMode = it }
                        )
                    }
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
                                checked = parentalControlsEnabled,
                                onCheckedChange = { parentalControlsEnabled = it }
                        )

                        // Notifications
                        SwitchSetting(
                                title = "Notifications",
                                subtitle = "Receive app notifications",
                                icon = Icons.Default.Notifications,
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
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
                            currentValue = dataUsageLimit.displayName,
                            options = DataUsageLimit.values().map { it.displayName },
                            onValueSelected = { selected ->
                                dataUsageLimit =
                                        DataUsageLimit.values().find { it.displayName == selected }
                                                ?: DataUsageLimit.UNLIMITED
                            }
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
                                icon = Icons.Default.Info
                        )

                        InfoSetting(
                                title = "Build",
                                subtitle = "Release Build",
                                icon = Icons.Default.Build
                        )

                        ActionSetting(
                                title = "Privacy Policy",
                                subtitle = "View our privacy policy",
                                icon = Icons.Default.Policy,
                                onClick = { /* TODO: Open privacy policy */}
                        )

                        ActionSetting(
                                title = "Terms of Service",
                                subtitle = "View terms of service",
                                icon = Icons.Default.Description,
                                onClick = { /* TODO: Open terms */}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBackPressed: () -> Unit, firstFocusRequester: FocusRequester) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        var backButtonFocused by remember { mutableStateOf(false) }

        TVFocusIndicator(isFocused = backButtonFocused) {
            IconButton(
                    onClick = onBackPressed,
                    modifier =
                            Modifier.focusRequester(firstFocusRequester)
                                    .tvFocusable(
                                            onFocusChanged = { backButtonFocused = it.isFocused }
                                    )
            ) {
                Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint =
                                if (backButtonFocused) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                }
                )
            }
        }

        // Title
        Text(
                text = "Settings",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
        )

        Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                shadowElevation = 2.dp
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
        onCheckedChange: (Boolean) -> Unit
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
                                                    alpha = 0.3f
                                            )
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                        )
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                ) {
                    Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint =
                                    if (isFocused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                    )

                    Column {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                        )
                        Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                                                MaterialTheme.colorScheme.surfaceVariant
                                )
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
        onValueSelected: (String) -> Unit
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
                                                    alpha = 0.3f
                                            )
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                        )
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                ) {
                    Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint =
                                    if (isFocused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                    )

                    Column {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                        )
                        Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = currentValue,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                    )
                    Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSetting(title: String, subtitle: String, icon: ImageVector) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
            )

            Column {
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                )
                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionSetting(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        Card(
                onClick = onClick,
                modifier =
                        Modifier.fillMaxWidth()
                                .tvFocusable(onFocusChanged = { isFocused = it.isFocused }),
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        if (isFocused) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.3f
                                            )
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                        )
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                ) {
                    Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint =
                                    if (isFocused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                    )

                    Column {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                        )
                        Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

