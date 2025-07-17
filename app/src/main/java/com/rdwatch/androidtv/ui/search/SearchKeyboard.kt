package com.rdwatch.androidtv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TV-optimized keyboard layout designed for D-pad navigation
 * Provides QWERTY layout with special keys for search functionality
 */
@Composable
fun TVSearchKeyboard(
    onTextChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onVoiceSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    initialText: String = "",
    placeholder: String = "Search for movies, TV shows...",
    isVoiceSearchEnabled: Boolean = true,
) {
    var currentText by remember { mutableStateOf(initialText) }
    val firstKeyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(initialText) {
        currentText = initialText
        onTextChanged(currentText)
    }

    LaunchedEffect(Unit) {
        firstKeyFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Search input display
        SearchInputDisplay(
            text = currentText,
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth(),
        )

        // Keyboard grid
        TVKeyboardGrid(
            onKeyPressed = { key ->
                when (key.type) {
                    KeyType.CHARACTER -> {
                        currentText += key.value
                        onTextChanged(currentText)
                    }
                    KeyType.BACKSPACE -> {
                        if (currentText.isNotEmpty()) {
                            currentText = currentText.dropLast(1)
                            onTextChanged(currentText)
                        }
                    }
                    KeyType.SPACE -> {
                        currentText += " "
                        onTextChanged(currentText)
                    }
                    KeyType.CLEAR -> {
                        currentText = ""
                        onTextChanged(currentText)
                        onClear()
                    }
                    KeyType.SEARCH -> onSearch()
                    KeyType.VOICE -> if (isVoiceSearchEnabled) onVoiceSearch()
                }
            },
            firstKeyFocusRequester = firstKeyFocusRequester,
            isVoiceSearchEnabled = isVoiceSearchEnabled,
        )
    }
}

@Composable
private fun SearchInputDisplay(
    text: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(60.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Cursor simulation
            if (text.isNotEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun TVKeyboardGrid(
    onKeyPressed: (KeyboardKey) -> Unit,
    firstKeyFocusRequester: FocusRequester,
    isVoiceSearchEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val keyboardLayout = getTVKeyboardLayout(isVoiceSearchEnabled)

    LazyVerticalGrid(
        columns = GridCells.Fixed(13), // Standard QWERTY width
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(keyboardLayout.flatten().size) { index ->
            val key = keyboardLayout.flatten()[index]

            TVKeyboardKey(
                key = key,
                onKeyPressed = onKeyPressed,
                modifier =
                    if (index == 0) {
                        Modifier.focusRequester(firstKeyFocusRequester)
                    } else {
                        Modifier
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TVKeyboardKey(
    key: KeyboardKey,
    onKeyPressed: (KeyboardKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = { onKeyPressed(key) },
        modifier =
            modifier
                .size(
                    width = if (key.type == KeyType.SPACE) 200.dp else 60.dp,
                    height = 60.dp,
                )
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionCenter, Key.Enter -> {
                                onKeyPressed(key)
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isFocused -> MaterialTheme.colorScheme.primary
                        key.type in listOf(KeyType.BACKSPACE, KeyType.CLEAR, KeyType.SEARCH, KeyType.VOICE) ->
                            MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.surface
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 8.dp else 4.dp,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (key.type) {
                KeyType.CHARACTER, KeyType.SPACE -> {
                    Text(
                        text = key.displayValue,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                            ),
                        color =
                            if (isFocused) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    key.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = key.displayValue,
                            tint =
                                if (isFocused) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSecondary
                                },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generates the TV keyboard layout with optimal focus navigation
 */
private fun getTVKeyboardLayout(isVoiceSearchEnabled: Boolean): List<List<KeyboardKey>> {
    return listOf(
        // Row 1: Numbers
        listOf(
            KeyboardKey("1", "1", KeyType.CHARACTER),
            KeyboardKey("2", "2", KeyType.CHARACTER),
            KeyboardKey("3", "3", KeyType.CHARACTER),
            KeyboardKey("4", "4", KeyType.CHARACTER),
            KeyboardKey("5", "5", KeyType.CHARACTER),
            KeyboardKey("6", "6", KeyType.CHARACTER),
            KeyboardKey("7", "7", KeyType.CHARACTER),
            KeyboardKey("8", "8", KeyType.CHARACTER),
            KeyboardKey("9", "9", KeyType.CHARACTER),
            KeyboardKey("0", "0", KeyType.CHARACTER),
            KeyboardKey("-", "-", KeyType.CHARACTER),
            KeyboardKey("=", "=", KeyType.CHARACTER),
            KeyboardKey("⌫", "Backspace", KeyType.BACKSPACE, Icons.Default.Backspace),
        ),
        // Row 2: QWERTY first row
        listOf(
            KeyboardKey("q", "Q", KeyType.CHARACTER),
            KeyboardKey("w", "W", KeyType.CHARACTER),
            KeyboardKey("e", "E", KeyType.CHARACTER),
            KeyboardKey("r", "R", KeyType.CHARACTER),
            KeyboardKey("t", "T", KeyType.CHARACTER),
            KeyboardKey("y", "Y", KeyType.CHARACTER),
            KeyboardKey("u", "U", KeyType.CHARACTER),
            KeyboardKey("i", "I", KeyType.CHARACTER),
            KeyboardKey("o", "O", KeyType.CHARACTER),
            KeyboardKey("p", "P", KeyType.CHARACTER),
            KeyboardKey("[", "[", KeyType.CHARACTER),
            KeyboardKey("]", "]", KeyType.CHARACTER),
            KeyboardKey("\\", "\\", KeyType.CHARACTER),
        ),
        // Row 3: ASDF row
        listOf(
            KeyboardKey("a", "A", KeyType.CHARACTER),
            KeyboardKey("s", "S", KeyType.CHARACTER),
            KeyboardKey("d", "D", KeyType.CHARACTER),
            KeyboardKey("f", "F", KeyType.CHARACTER),
            KeyboardKey("g", "G", KeyType.CHARACTER),
            KeyboardKey("h", "H", KeyType.CHARACTER),
            KeyboardKey("j", "J", KeyType.CHARACTER),
            KeyboardKey("k", "K", KeyType.CHARACTER),
            KeyboardKey("l", "L", KeyType.CHARACTER),
            KeyboardKey(";", ";", KeyType.CHARACTER),
            KeyboardKey("'", "'", KeyType.CHARACTER),
            KeyboardKey("", "Clear", KeyType.CLEAR, Icons.Default.Clear),
            KeyboardKey("", "Search", KeyType.SEARCH, Icons.Default.Search),
        ),
        // Row 4: ZXCV row
        listOf(
            KeyboardKey("z", "Z", KeyType.CHARACTER),
            KeyboardKey("x", "X", KeyType.CHARACTER),
            KeyboardKey("c", "C", KeyType.CHARACTER),
            KeyboardKey("v", "V", KeyType.CHARACTER),
            KeyboardKey("b", "B", KeyType.CHARACTER),
            KeyboardKey("n", "N", KeyType.CHARACTER),
            KeyboardKey("m", "M", KeyType.CHARACTER),
            KeyboardKey(",", ",", KeyType.CHARACTER),
            KeyboardKey(".", ".", KeyType.CHARACTER),
            KeyboardKey("/", "/", KeyType.CHARACTER),
            KeyboardKey("", "Space", KeyType.SPACE, Icons.Default.SpaceBar),
            // Conditional voice search key
            if (isVoiceSearchEnabled) {
                KeyboardKey("", "Voice Search", KeyType.VOICE, Icons.Default.Mic)
            } else {
                KeyboardKey("", "Done", KeyType.SEARCH, Icons.Default.Done)
            },
            KeyboardKey("", "Done", KeyType.SEARCH, Icons.Default.Done),
        ),
    )
}

/**
 * Represents a keyboard key with its properties
 */
data class KeyboardKey(
    val value: String,
    val displayValue: String,
    val type: KeyType,
    val icon: ImageVector? = null,
)

/**
 * Types of keyboard keys for different functionality
 */
enum class KeyType {
    CHARACTER,
    BACKSPACE,
    SPACE,
    CLEAR,
    SEARCH,
    VOICE,
}
