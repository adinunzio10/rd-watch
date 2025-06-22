package com.rdwatch.androidtv.ui.focus

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay

/**
 * TV-optimized focus management system for D-pad navigation
 */
class TVFocusManager {
    private var currentFocusId: String? = null
    private val focusRequesters = mutableMapOf<String, FocusRequester>()
    private val focusCallbacks = mutableMapOf<String, () -> Unit>()
    
    fun registerFocusRequester(id: String, focusRequester: FocusRequester) {
        focusRequesters[id] = focusRequester
    }
    
    fun registerFocusCallback(id: String, callback: () -> Unit) {
        focusCallbacks[id] = callback
    }
    
    fun requestFocus(id: String) {
        currentFocusId = id
        focusRequesters[id]?.requestFocus()
        focusCallbacks[id]?.invoke()
    }
    
    fun getCurrentFocusId(): String? = currentFocusId
    
    fun clearFocus() {
        currentFocusId = null
    }
}

@Composable
fun rememberTVFocusManager(): TVFocusManager {
    return remember { TVFocusManager() }
}

/**
 * Spatial navigation coordinator for TV remote control
 */
@Composable
fun TVSpatialNavigation(
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Boolean)? = null,
    onNavigateDown: (() -> Boolean)? = null,
    onNavigateLeft: (() -> Boolean)? = null,
    onNavigateRight: (() -> Boolean)? = null,
    onSelect: (() -> Boolean)? = null,
    onBack: (() -> Boolean)? = null,
    content: @Composable () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionUp -> onNavigateUp?.invoke() ?: false
                        Key.DirectionDown -> onNavigateDown?.invoke() ?: false
                        Key.DirectionLeft -> onNavigateLeft?.invoke() ?: false
                        Key.DirectionRight -> onNavigateRight?.invoke() ?: false
                        Key.Enter, Key.DirectionCenter -> onSelect?.invoke() ?: false
                        Key.Back, Key.Escape -> onBack?.invoke() ?: false
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        content()
    }
}

/**
 * Enhanced focusable modifier with TV-specific behavior
 */
@Composable
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null
): Modifier {
    var modifier = this
    
    if (focusRequester != null) {
        modifier = modifier.focusRequester(focusRequester)
    }
    
    modifier = modifier.focusable(enabled)
    
    if (onFocusChanged != null) {
        modifier = modifier.onFocusChanged(onFocusChanged)
    }
    
    if (onKeyEvent != null) {
        modifier = modifier.onKeyEvent(onKeyEvent)
    }
    
    return modifier
}

/**
 * Focus restoration utility for TV apps
 */
class TVFocusRestorer {
    private var lastFocusId: String? = null
    private var lastFocusRequester: FocusRequester? = null
    
    fun saveFocus(focusId: String, focusRequester: FocusRequester) {
        lastFocusId = focusId
        lastFocusRequester = focusRequester
    }
    
    suspend fun restoreFocus() {
        lastFocusRequester?.let {
            // Small delay to ensure UI is ready
            delay(100)
            it.requestFocus()
        }
    }
    
    fun clear() {
        lastFocusId = null
        lastFocusRequester = null
    }
}

@Composable
fun rememberTVFocusRestorer(): TVFocusRestorer {
    return remember { TVFocusRestorer() }
}

/**
 * Focus group for managing related focusable items
 */
class TVFocusGroup(val id: String) {
    private val items = mutableListOf<TVFocusItem>()
    private var currentIndex = 0
    
    fun addItem(item: TVFocusItem) {
        items.add(item)
    }
    
    fun removeItem(item: TVFocusItem) {
        items.remove(item)
        if (currentIndex >= items.size && items.isNotEmpty()) {
            currentIndex = items.size - 1
        }
    }
    
    fun focusNext(): Boolean {
        if (items.isEmpty()) return false
        
        currentIndex = (currentIndex + 1) % items.size
        items[currentIndex].focusRequester.requestFocus()
        return true
    }
    
    fun focusPrevious(): Boolean {
        if (items.isEmpty()) return false
        
        currentIndex = if (currentIndex == 0) items.size - 1 else currentIndex - 1
        items[currentIndex].focusRequester.requestFocus()
        return true
    }
    
    fun focusFirst(): Boolean {
        if (items.isEmpty()) return false
        
        currentIndex = 0
        items[currentIndex].focusRequester.requestFocus()
        return true
    }
    
    fun focusLast(): Boolean {
        if (items.isEmpty()) return false
        
        currentIndex = items.size - 1
        items[currentIndex].focusRequester.requestFocus()
        return true
    }
    
    fun getCurrentItem(): TVFocusItem? {
        return if (items.isNotEmpty() && currentIndex < items.size) {
            items[currentIndex]
        } else null
    }
}

data class TVFocusItem(
    val id: String,
    val focusRequester: FocusRequester,
    val onFocused: (() -> Unit)? = null
)

@Composable
fun rememberTVFocusGroup(id: String): TVFocusGroup {
    return remember(id) { TVFocusGroup(id) }
}

/**
 * Enhanced focus indicator for TV interfaces
 */
@Composable
fun TVFocusIndicator(
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val focusedModifier = if (isFocused) {
        modifier
            .focusable()
    } else {
        modifier.focusable()
    }
    
    Box(modifier = focusedModifier) {
        content()
    }
}

/**
 * Focus boundary to prevent focus from leaving a specific area
 */
@Composable
fun TVFocusBoundary(
    modifier: Modifier = Modifier,
    trapFocus: Boolean = true,
    content: @Composable () -> Unit
) {
    if (trapFocus) {
        FocusGroup {
            Box(modifier = modifier) {
                content()
            }
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

/**
 * Automatic focus assignment when component becomes visible
 */
@Composable
fun AutoTVFocus(
    focusRequester: FocusRequester,
    enabled: Boolean = true,
    delay: Long = 100
) {
    LaunchedEffect(enabled) {
        if (enabled) {
            delay(delay)
            focusRequester.requestFocus()
        }
    }
}

/**
 * Focus debouncer to prevent rapid focus changes
 */
class TVFocusDebouncer(private val delayMs: Long = 50) {
    private var lastFocusTime = 0L
    
    fun shouldAllowFocus(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastFocusTime > delayMs) {
            lastFocusTime = currentTime
            true
        } else {
            false
        }
    }
}

@Composable
fun rememberTVFocusDebouncer(delayMs: Long = 50): TVFocusDebouncer {
    return remember(delayMs) { TVFocusDebouncer(delayMs) }
}