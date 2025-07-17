package com.rdwatch.androidtv.ui.settings.scrapers

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.ValidationStatus
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Individual scraper item component for the scrapers list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScraperListItem(
    scraper: ScraperManifest,
    onToggleEnabled: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .tvFocusable(onFocusChanged = {
                    isFocused = it.isFocused
                    if (it.isFocused) showActions = true
                }),
        onClick = { showActions = !showActions },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isFocused) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        TVFocusIndicator(isFocused = isFocused) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                // Main content row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Scraper info
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = scraper.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            // Status indicator
                            StatusIndicator(
                                isEnabled = scraper.isEnabled,
                                validationStatus = scraper.metadata.validationStatus,
                            )
                        }

                        Text(
                            text = "v${scraper.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )

                        scraper.description?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

                        scraper.author?.let { author ->
                            Text(
                                text = "by $author",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }

                    // Enable/Disable switch
                    Switch(
                        checked = scraper.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                }

                // Expandable actions
                if (showActions) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Refresh button
                        ActionButton(
                            icon = Icons.Default.Refresh,
                            text = "Refresh",
                            onClick = onRefresh,
                            modifier = Modifier.weight(1f),
                        )

                        // Remove button
                        ActionButton(
                            icon = Icons.Default.Delete,
                            text = "Remove",
                            onClick = onRemove,
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    isEnabled: Boolean,
    validationStatus: ValidationStatus,
) {
    val (icon, color, contentDescription) =
        when {
            !isEnabled ->
                Triple(
                    Icons.Default.Pause,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    "Disabled",
                )
            validationStatus == ValidationStatus.VALID ->
                Triple(
                    Icons.Default.CheckCircle,
                    MaterialTheme.colorScheme.primary,
                    "Valid",
                )
            validationStatus == ValidationStatus.INVALID ->
                Triple(
                    Icons.Default.Error,
                    MaterialTheme.colorScheme.error,
                    "Invalid",
                )
            validationStatus == ValidationStatus.PENDING ->
                Triple(
                    Icons.Default.Schedule,
                    MaterialTheme.colorScheme.outline,
                    "Pending validation",
                )
            else ->
                Triple(
                    Icons.Default.Warning,
                    MaterialTheme.colorScheme.error,
                    "Error",
                )
        }

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = color,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) {
    var isFocused by remember { mutableStateOf(false) }

    TVFocusIndicator(isFocused = isFocused) {
        TextButton(
            onClick = onClick,
            modifier =
                modifier
                    .tvFocusable(onFocusChanged = { isFocused = it.isFocused }),
            colors = colors,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
