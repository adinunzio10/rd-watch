package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * TV-optimized tabs for content detail screens
 * Supports different tab configurations based on content type
 */
@Composable
fun ContentDetailTabs(
    selectedTabIndex: Int,
    contentType: ContentType,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    firstTabFocusRequester: FocusRequester? = null
) {
    val tabs = getTabsForContentType(contentType)
    
    Row(
        modifier = modifier
            .selectableGroup()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            ContentDetailTabItem(
                text = tab.title,
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                modifier = Modifier
                    .then(
                        if (index == 0 && firstTabFocusRequester != null) {
                            Modifier.focusRequester(firstTabFocusRequester)
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

/**
 * Individual tab item optimized for TV navigation
 */
@Composable
fun ContentDetailTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .tvFocusable(
                onFocusChanged = { /* Focus handled by Surface styling */ }
            ),
        onClick = onClick,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        },
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Tab configuration for different content types
 */
data class ContentDetailTab(
    val title: String,
    val key: String
)

/**
 * Get tabs configuration based on content type
 */
private fun getTabsForContentType(contentType: ContentType): List<ContentDetailTab> {
    return when (contentType) {
        ContentType.MOVIE, ContentType.DOCUMENTARY -> listOf(
            ContentDetailTab("Overview", "overview"),
            ContentDetailTab("Details", "details")
        )
        ContentType.TV_SHOW, ContentType.TV_EPISODE -> listOf(
            ContentDetailTab("Overview", "overview"),
            ContentDetailTab("Details", "details"),
            ContentDetailTab("Episodes", "episodes")
        )
        else -> listOf(
            ContentDetailTab("Overview", "overview"),
            ContentDetailTab("Details", "details")
        )
    }
}

/**
 * Get tab index by key
 */
fun getTabIndexByKey(contentType: ContentType, key: String): Int {
    val tabs = getTabsForContentType(contentType)
    return tabs.indexOfFirst { it.key == key }.takeIf { it >= 0 } ?: 0
}

/**
 * Get tab key by index
 */
fun getTabKeyByIndex(contentType: ContentType, index: Int): String {
    val tabs = getTabsForContentType(contentType)
    return tabs.getOrNull(index)?.key ?: "overview"
}