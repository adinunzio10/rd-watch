package com.rdwatch.androidtv.ui.focus

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*

/**
 * TV remote control key event handler
 */
class TVKeyEventHandler {
    // D-pad navigation handlers
    var onDPadUp: (() -> Boolean)? = null
    var onDPadDown: (() -> Boolean)? = null
    var onDPadLeft: (() -> Boolean)? = null
    var onDPadRight: (() -> Boolean)? = null
    var onDPadCenter: (() -> Boolean)? = null

    // Media control handlers
    var onPlayPause: (() -> Boolean)? = null
    var onStop: (() -> Boolean)? = null
    var onFastForward: (() -> Boolean)? = null
    var onRewind: (() -> Boolean)? = null

    // Navigation handlers
    var onBack: (() -> Boolean)? = null
    var onHome: (() -> Boolean)? = null
    var onMenu: (() -> Boolean)? = null

    // Channel/Volume handlers (for TV devices)
    var onChannelUp: (() -> Boolean)? = null
    var onChannelDown: (() -> Boolean)? = null
    var onVolumeUp: (() -> Boolean)? = null
    var onVolumeDown: (() -> Boolean)? = null
    var onMute: (() -> Boolean)? = null

    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false

        return when (keyEvent.key) {
            // D-pad navigation
            Key.DirectionUp -> onDPadUp?.invoke() ?: false
            Key.DirectionDown -> onDPadDown?.invoke() ?: false
            Key.DirectionLeft -> onDPadLeft?.invoke() ?: false
            Key.DirectionRight -> onDPadRight?.invoke() ?: false
            Key.DirectionCenter, Key.Enter -> onDPadCenter?.invoke() ?: false

            // Media controls
            Key.MediaPlayPause, Key.Spacebar -> onPlayPause?.invoke() ?: false
            Key.MediaStop -> onStop?.invoke() ?: false
            Key.MediaFastForward -> onFastForward?.invoke() ?: false
            Key.MediaRewind -> onRewind?.invoke() ?: false

            // Navigation
            Key.Back, Key.Escape -> onBack?.invoke() ?: false
            Key.Home -> onHome?.invoke() ?: false
            Key.Menu -> onMenu?.invoke() ?: false

            // Channel/Volume (if supported by device)
            Key.ChannelUp -> onChannelUp?.invoke() ?: false
            Key.ChannelDown -> onChannelDown?.invoke() ?: false
            Key.VolumeUp -> onVolumeUp?.invoke() ?: false
            Key.VolumeDown -> onVolumeDown?.invoke() ?: false
            Key.VolumeMute -> onMute?.invoke() ?: false

            else -> false
        }
    }
}

@Composable
fun rememberTVKeyEventHandler(): TVKeyEventHandler {
    return remember { TVKeyEventHandler() }
}

/**
 * Pre-configured key handlers for common TV navigation patterns
 */
object TVKeyHandlers {
    /**
     * Standard drawer navigation handler
     */
    fun drawerNavigation(
        onOpenDrawer: () -> Unit,
        onCloseDrawer: () -> Unit,
        isDrawerOpen: Boolean,
    ): TVKeyEventHandler {
        return TVKeyEventHandler().apply {
            onDPadLeft = {
                if (!isDrawerOpen) {
                    onOpenDrawer()
                    true
                } else {
                    false
                }
            }
            onBack = {
                if (isDrawerOpen) {
                    onCloseDrawer()
                    true
                } else {
                    false
                }
            }
            onMenu = {
                if (isDrawerOpen) {
                    onCloseDrawer()
                } else {
                    onOpenDrawer()
                }
                true
            }
        }
    }

    /**
     * Standard content row navigation handler
     */
    fun contentRowNavigation(
        onNavigateRow: (direction: NavigationDirection) -> Boolean,
        onSelectItem: () -> Boolean,
    ): TVKeyEventHandler {
        return TVKeyEventHandler().apply {
            onDPadUp = { onNavigateRow(NavigationDirection.UP) }
            onDPadDown = { onNavigateRow(NavigationDirection.DOWN) }
            onDPadLeft = { onNavigateRow(NavigationDirection.LEFT) }
            onDPadRight = { onNavigateRow(NavigationDirection.RIGHT) }
            onDPadCenter = { onSelectItem() }
        }
    }

    /**
     * Media player navigation handler
     */
    fun mediaPlayerNavigation(
        onPlayPause: () -> Unit,
        onStop: () -> Unit,
        onSeek: (forward: Boolean) -> Unit,
        onBack: () -> Unit,
    ): TVKeyEventHandler {
        return TVKeyEventHandler().apply {
            this.onPlayPause = {
                onPlayPause()
                true
            }
            this.onStop = {
                onStop()
                true
            }
            onFastForward = {
                onSeek(true)
                true
            }
            onRewind = {
                onSeek(false)
                true
            }
            this.onBack = {
                onBack()
                true
            }
        }
    }
}

enum class NavigationDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

/**
 * Focus movement coordinator for complex layouts
 */
class TVFocusCoordinator {
    private val focusMap = mutableMapOf<String, TVFocusNode>()
    private var currentFocusId: String? = null

    fun registerNode(
        id: String,
        focusRequester: FocusRequester,
        neighbors: Map<NavigationDirection, String> = emptyMap(),
    ) {
        focusMap[id] = TVFocusNode(id, focusRequester, neighbors.toMutableMap())
    }

    fun updateNeighbors(
        id: String,
        neighbors: Map<NavigationDirection, String>,
    ) {
        focusMap[id]?.neighbors?.putAll(neighbors)
    }

    fun navigateToNeighbor(direction: NavigationDirection): Boolean {
        val currentNode = currentFocusId?.let { focusMap[it] } ?: return false
        val neighborId = currentNode.neighbors[direction] ?: return false
        val neighborNode = focusMap[neighborId] ?: return false

        neighborNode.focusRequester.requestFocus()
        currentFocusId = neighborId
        return true
    }

    fun focusNode(id: String): Boolean {
        val node = focusMap[id] ?: return false
        node.focusRequester.requestFocus()
        currentFocusId = id
        return true
    }

    fun getCurrentFocusId(): String? = currentFocusId
}

data class TVFocusNode(
    val id: String,
    val focusRequester: FocusRequester,
    val neighbors: MutableMap<NavigationDirection, String>,
)

@Composable
fun rememberTVFocusCoordinator(): TVFocusCoordinator {
    return remember { TVFocusCoordinator() }
}

/**
 * Grid focus navigation helper
 */
class TVGridFocusManager(
    private val columns: Int,
    private val rows: Int,
) {
    private var currentRow = 0
    private var currentColumn = 0
    private val focusRequesters = Array(rows) { Array(columns) { FocusRequester() } }

    fun getFocusRequester(
        row: Int,
        column: Int,
    ): FocusRequester? {
        return if (row in 0 until rows && column in 0 until columns) {
            focusRequesters[row][column]
        } else {
            null
        }
    }

    fun navigateUp(): Boolean {
        if (currentRow > 0) {
            currentRow--
            focusRequesters[currentRow][currentColumn].requestFocus()
            return true
        }
        return false
    }

    fun navigateDown(): Boolean {
        if (currentRow < rows - 1) {
            currentRow++
            focusRequesters[currentRow][currentColumn].requestFocus()
            return true
        }
        return false
    }

    fun navigateLeft(): Boolean {
        if (currentColumn > 0) {
            currentColumn--
            focusRequesters[currentRow][currentColumn].requestFocus()
            return true
        }
        return false
    }

    fun navigateRight(): Boolean {
        if (currentColumn < columns - 1) {
            currentColumn++
            focusRequesters[currentRow][currentColumn].requestFocus()
            return true
        }
        return false
    }

    fun focusPosition(
        row: Int,
        column: Int,
    ): Boolean {
        if (row in 0 until rows && column in 0 until columns) {
            currentRow = row
            currentColumn = column
            focusRequesters[row][column].requestFocus()
            return true
        }
        return false
    }

    fun getCurrentPosition(): Pair<Int, Int> = Pair(currentRow, currentColumn)
}

@Composable
fun rememberTVGridFocusManager(
    columns: Int,
    rows: Int,
): TVGridFocusManager {
    return remember(columns, rows) { TVGridFocusManager(columns, rows) }
}
