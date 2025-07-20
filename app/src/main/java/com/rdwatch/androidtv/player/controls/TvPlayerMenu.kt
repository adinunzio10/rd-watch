package com.rdwatch.androidtv.player.controls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.player.PlayerState
import com.rdwatch.androidtv.player.subtitle.AvailableSubtitle
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator

data class MenuOption(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val action: () -> Unit,
)

data class SubMenuOption(
    val id: String,
    val title: String,
    val isSelected: Boolean = false,
    val action: () -> Unit,
)

@Composable
fun TvPlayerMenu(
    playerState: PlayerState,
    isVisible: Boolean,
    availableSubtitles: List<AvailableSubtitle> = emptyList(),
    currentSubtitleTrack: AvailableSubtitle? = null,
    subtitlesEnabled: Boolean = true,
    onSubtitleTrackSelected: (AvailableSubtitle?) -> Unit = {},
    onSubtitlesToggle: (Boolean) -> Unit = {},
    onPlaybackSpeedSelected: (Float) -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        var currentSubMenu by remember { mutableStateOf<String?>(null) }
        val listState = rememberLazyListState()
        val firstItemFocus = remember { FocusRequester() }

        // Auto-focus first item when menu becomes visible
        LaunchedEffect(isVisible) {
            if (isVisible) {
                kotlinx.coroutines.delay(100)
                firstItemFocus.requestFocus()
            }
        }

        // Available playback speeds
        val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

        // Main menu options
        val mainMenuOptions =
            listOf(
                MenuOption(
                    id = "subtitles",
                    title = "Subtitles",
                    subtitle =
                        if (subtitlesEnabled) {
                            currentSubtitleTrack?.label ?: "No track selected"
                        } else {
                            "Disabled"
                        },
                    icon = Icons.Default.Subtitles,
                    action = { currentSubMenu = "subtitles" },
                ),
                MenuOption(
                    id = "playback_speed",
                    title = "Playback Speed",
                    subtitle = "${playerState.playbackSpeed}x",
                    icon = Icons.Default.Speed,
                    action = { currentSubMenu = "playback_speed" },
                ),
                MenuOption(
                    id = "audio",
                    title = "Audio Track",
                    subtitle = "Default",
                    icon = Icons.Default.VolumeUp,
                    action = { /* TODO: Implement audio track selection */ },
                ),
            )

        Card(
            modifier =
                modifier
                    .fillMaxHeight()
                    .width(400.dp)
                    .padding(16.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.9f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .onKeyEvent { keyEvent ->
                            when {
                                keyEvent.type == KeyEventType.KeyDown -> {
                                    when (keyEvent.key) {
                                        Key.Back, Key.Escape -> {
                                            if (currentSubMenu != null) {
                                                currentSubMenu = null
                                            } else {
                                                onClose()
                                            }
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                else -> false
                            }
                        },
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            when (currentSubMenu) {
                                "subtitles" -> "Subtitle Settings"
                                "playback_speed" -> "Playback Speed"
                                else -> "Player Settings"
                            },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    if (currentSubMenu != null) {
                        TextButton(
                            onClick = { currentSubMenu = null },
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor = Color.White.copy(alpha = 0.7f),
                                ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(16.dp),
                                )
                                Text("Back")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // Content
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (currentSubMenu) {
                        "subtitles" -> {
                            // Subtitle enable/disable toggle
                            item {
                                SubtitleToggleItem(
                                    enabled = subtitlesEnabled,
                                    onToggle = onSubtitlesToggle,
                                    focusRequester = firstItemFocus,
                                )
                            }

                            if (subtitlesEnabled && availableSubtitles.isNotEmpty()) {
                                // Available subtitle tracks
                                items(availableSubtitles) { subtitle ->
                                    SubtitleTrackItem(
                                        subtitle = subtitle,
                                        isSelected = subtitle.id == currentSubtitleTrack?.id,
                                        onSelected = { onSubtitleTrackSelected(subtitle) },
                                    )
                                }

                                // Option to disable subtitles
                                item {
                                    SubtitleTrackItem(
                                        subtitle = null,
                                        isSelected = currentSubtitleTrack == null && subtitlesEnabled,
                                        onSelected = { onSubtitleTrackSelected(null) },
                                    )
                                }
                            }
                        }

                        "playback_speed" -> {
                            items(playbackSpeeds) { speed ->
                                val isFirst = speed == playbackSpeeds.first()
                                PlaybackSpeedItem(
                                    speed = speed,
                                    isSelected = kotlin.math.abs(speed - playerState.playbackSpeed) < 0.01f,
                                    onSelected = { onPlaybackSpeedSelected(speed) },
                                    focusRequester = if (isFirst) firstItemFocus else null,
                                )
                            }
                        }

                        else -> {
                            // Main menu
                            items(mainMenuOptions) { option ->
                                val isFirst = option == mainMenuOptions.first()
                                MenuOptionItem(
                                    option = option,
                                    focusRequester = if (isFirst) firstItemFocus else null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuOptionItem(
    option: MenuOption,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = option.action,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                    .onFocusChanged { isFocused = it.isFocused },
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isFocused) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        },
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    option.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SubtitleToggleItem(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = { onToggle(!enabled) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                    .onFocusChanged { isFocused = it.isFocused },
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isFocused) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        },
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )

                    Text(
                        text = "Enable Subtitles",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f),
                        ),
                )
            }
        }
    }
}

@Composable
private fun SubtitleTrackItem(
    subtitle: AvailableSubtitle?,
    isSelected: Boolean,
    onSelected: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = onSelected,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                    .onFocusChanged { isFocused = it.isFocused },
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            isFocused -> Color.White.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        },
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )

                Text(
                    text = subtitle?.label ?: "No subtitles",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun PlaybackSpeedItem(
    speed: Float,
    isSelected: Boolean,
    onSelected: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        Card(
            onClick = onSelected,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                    .onFocusChanged { isFocused = it.isFocused },
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            isFocused -> Color.White.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        },
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )

                Text(
                    text = "${speed}x",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                )

                if (speed == 1.0f) {
                    Text(
                        text = "(Normal)",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
