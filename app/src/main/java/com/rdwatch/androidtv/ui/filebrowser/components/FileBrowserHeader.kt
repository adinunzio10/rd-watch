package com.rdwatch.androidtv.ui.filebrowser.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.presentation.components.tvFocusable

/**
 * Header component for the File Browser screen
 */
@Composable
fun FileBrowserHeader(
    onBackPressed: () -> Unit,
    firstFocusRequester: FocusRequester,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var backButtonFocused by remember { mutableStateOf(false) }
    var clearButtonFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button or Clear selection button
        if (isSelectionMode) {
            IconButton(
                onClick = onClearSelection,
                modifier = Modifier
                    .tvFocusable(
                        onFocusChanged = { clearButtonFocused = it }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear selection",
                    tint = if (clearButtonFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                )
            }
        } else {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .focusRequester(firstFocusRequester)
                    .tvFocusable(
                        onFocusChanged = { backButtonFocused = it }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (backButtonFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                )
            }
        }

        // Title
        Text(
            text = if (isSelectionMode) {
                "$selectedCount selected"
            } else {
                "File Browser"
            },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        // Spacer to push content to the right if needed
        Spacer(modifier = Modifier.weight(1f))
        
        // Additional header content can go here
        // For example: account info, storage usage, etc.
    }
}