package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.details.models.SourceProvider
import com.rdwatch.androidtv.ui.details.models.StreamingSource

/**
 * Source card component for displaying streaming provider information
 * Used in horizontally scrollable source selection interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceCard(
    source: StreamingSource,
    onClick: (StreamingSource) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    showSourceInfo: Boolean = true,
    showQualityBadges: Boolean = true,
    variant: SourceCardVariant = SourceCardVariant.DEFAULT,
) {
    var isFocused by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    val isEnabled = source.isCurrentlyAvailable()

    OutlinedCard(
        onClick = {
            if (isEnabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick(source)
            }
        },
        enabled = isEnabled,
        modifier =
            modifier
                .width(
                    when (variant) {
                        SourceCardVariant.COMPACT -> 120.dp
                        SourceCardVariant.DEFAULT -> 160.dp
                        SourceCardVariant.DETAILED -> 200.dp
                    },
                )
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
        border =
            when {
                isSelected ->
                    BorderStroke(
                        width = 3.dp,
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary,
                                    ),
                            ),
                    )
                isFocused ->
                    BorderStroke(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                else -> CardDefaults.outlinedCardBorder()
            },
        colors =
            CardDefaults.outlinedCardColors(
                containerColor =
                    when {
                        !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        isFocused -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        SourceCardContent(
            source = source,
            isFocused = isFocused,
            isSelected = isSelected,
            isEnabled = isEnabled,
            showSourceInfo = showSourceInfo,
            showQualityBadges = showQualityBadges,
            variant = variant,
        )
    }
}

@Composable
private fun SourceCardContent(
    source: StreamingSource,
    isFocused: Boolean,
    isSelected: Boolean,
    isEnabled: Boolean,
    showSourceInfo: Boolean,
    showQualityBadges: Boolean,
    variant: SourceCardVariant,
) {
    val contentColor =
        when {
            !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            isSelected -> MaterialTheme.colorScheme.primary
            isFocused -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    when (variant) {
                        SourceCardVariant.COMPACT -> 12.dp
                        SourceCardVariant.DEFAULT -> 16.dp
                        SourceCardVariant.DETAILED -> 20.dp
                    },
                ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Provider logo/icon placeholder and name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderLogo(
                provider = source.provider,
                isEnabled = isEnabled,
                size =
                    when (variant) {
                        SourceCardVariant.COMPACT -> 24.dp
                        SourceCardVariant.DEFAULT -> 32.dp
                        SourceCardVariant.DETAILED -> 40.dp
                    },
            )

            if (!isEnabled) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Unavailable",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Selected",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Tracker name (more useful than provider name for Real-Debrid users)
        val trackerName = source.metadata["tracker"] ?: source.provider.displayName
        Text(
            text = trackerName,
            style =
                MaterialTheme.typography.titleSmall.copy(
                    fontSize =
                        when (variant) {
                            SourceCardVariant.COMPACT -> 12.sp
                            SourceCardVariant.DEFAULT -> 14.sp
                            SourceCardVariant.DETAILED -> 16.sp
                        },
                    fontWeight = FontWeight.SemiBold,
                ),
            color = getTrackerColor(trackerName, contentColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // File size display (critical for Real-Debrid storage decisions)
        source.size?.let { sizeStr ->
            val (formattedSize, sizeColor) = formatFileSize(sizeStr)
            Text(
                text = formattedSize,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize =
                            when (variant) {
                                SourceCardVariant.COMPACT -> 11.sp
                                SourceCardVariant.DEFAULT -> 13.sp
                                SourceCardVariant.DETAILED -> 15.sp
                            },
                        fontWeight = FontWeight.Bold,
                    ),
                color = sizeColor,
                maxLines = 1,
            )
        }

        // Filename display for verification (helps users confirm correct content)
        source.metadata["filename"]?.let { filename ->
            Text(
                text = filename,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize =
                            when (variant) {
                                SourceCardVariant.COMPACT -> 10.sp
                                SourceCardVariant.DEFAULT -> 11.sp
                                SourceCardVariant.DETAILED -> 12.sp
                            },
                    ),
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Quality badges
        if (showQualityBadges) {
            QualityFeatureBadgeRow(
                quality = source.quality,
                // Skip main quality since it's already shown
                features = source.getQualityBadges().drop(1),
                maxFeatures =
                    when (variant) {
                        SourceCardVariant.COMPACT -> 1
                        SourceCardVariant.DEFAULT -> 2
                        SourceCardVariant.DETAILED -> 3
                    },
                badgeSize =
                    when (variant) {
                        SourceCardVariant.COMPACT -> QualityBadgeSize.SMALL
                        SourceCardVariant.DEFAULT -> QualityBadgeSize.SMALL
                        SourceCardVariant.DETAILED -> QualityBadgeSize.MEDIUM
                    },
            )
        }

        // Source type and P2P information
        if (showSourceInfo) {
            SourceTypeBadge(
                sourceType = source.sourceType.getDisplayType(),
                reliability = source.sourceType.getReliabilityText(),
                isP2P = source.isP2P(),
                seeders = source.features.seeders,
                size =
                    when (variant) {
                        SourceCardVariant.COMPACT -> QualityBadgeSize.SMALL
                        SourceCardVariant.DEFAULT -> QualityBadgeSize.SMALL
                        SourceCardVariant.DETAILED -> QualityBadgeSize.MEDIUM
                    },
            )
        }

        // Additional info for detailed variant
        if (variant == SourceCardVariant.DETAILED) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (source.features.supportsP2P) {
                    Text(
                        text = "P2P Source",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f),
                    )
                }

                if (source.features.isConfigurable) {
                    Text(
                        text = "Configurable",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f),
                    )
                }

                source.features.seeders?.let { seeders ->
                    if (seeders > 0) {
                        Text(
                            text = "$seeders seeders",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f),
                        )
                    }
                }

                if (!isEnabled) {
                    Text(
                        text = source.getAvailabilityText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * Format file size string and return color-coded size with appropriate color
 * Colors help users quickly assess storage impact
 */
private fun formatFileSize(sizeStr: String): Pair<String, Color> {
    // Parse size from various formats (5.2GB, 1.3 GB, 800MB, etc.)
    val sizeRegex = Regex("([0-9]+\\.?[0-9]*)\\s*(GB|MB|TB)", RegexOption.IGNORE_CASE)
    val match = sizeRegex.find(sizeStr)

    if (match != null) {
        val value = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val unit = match.groupValues[2].uppercase()

        // Convert to GB for consistent comparison
        val sizeInGB =
            when (unit) {
                "TB" -> value * 1024
                "GB" -> value
                "MB" -> value / 1024
                else -> value
            }

        // Format and color code based on size
        val (displayText, color) =
            when {
                sizeInGB < 0.5 -> "${(sizeInGB * 1024).toInt()}MB" to Color(0xFF10B981) // Green for tiny files
                sizeInGB < 1.0 -> "${String.format("%.0f", sizeInGB * 1024)}MB" to Color(0xFF10B981) // Green for small files
                sizeInGB < 3.0 -> "${String.format("%.1f", sizeInGB)}GB" to Color(0xFF059669) // Green for small files
                sizeInGB < 8.0 -> "${String.format("%.1f", sizeInGB)}GB" to Color(0xFF3B82F6) // Blue for medium files
                sizeInGB < 15.0 -> "${String.format("%.1f", sizeInGB)}GB" to Color(0xFFF59E0B) // Orange for large files
                sizeInGB < 30.0 -> "${String.format("%.1f", sizeInGB)}GB" to Color(0xFFEF4444) // Red for very large files
                else -> "${String.format("%.1f", sizeInGB)}GB" to Color(0xFFDC2626) // Dark red for massive files
            }

        return displayText to color
    }

    // Fallback for unparseable sizes
    return sizeStr to Color(0xFF6B7280) // Gray for unknown
}

/**
 * Provider logo placeholder component
 */
@Composable
private fun ProviderLogo(
    provider: SourceProvider,
    isEnabled: Boolean,
    size: Dp = 32.dp,
) {
    val backgroundColor =
        provider.color?.let { Color(android.graphics.Color.parseColor(it)) }
            ?: MaterialTheme.colorScheme.primaryContainer

    val textColor =
        if (provider.color != null) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }

    Box(
        modifier =
            Modifier
                .size(size)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isEnabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = provider.displayName.take(2).uppercase(),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = (size.value / 3).sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = if (isEnabled) textColor else textColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Compact source card for smaller spaces
 */
@Composable
fun CompactSourceCard(
    source: StreamingSource,
    onClick: (StreamingSource) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    SourceCard(
        source = source,
        onClick = onClick,
        modifier = modifier,
        isSelected = isSelected,
        showSourceInfo = false,
        showQualityBadges = true,
        variant = SourceCardVariant.COMPACT,
    )
}

/**
 * Detailed source card with full information
 */
@Composable
fun DetailedSourceCard(
    source: StreamingSource,
    onClick: (StreamingSource) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    SourceCard(
        source = source,
        onClick = onClick,
        modifier = modifier,
        isSelected = isSelected,
        showSourceInfo = true,
        showQualityBadges = true,
        variant = SourceCardVariant.DETAILED,
    )
}

/**
 * Source card variants
 */
enum class SourceCardVariant {
    COMPACT, // Minimal info, smaller size
    DEFAULT, // Standard card with basic info
    DETAILED, // Full info with additional details
}

/**
 * Source card row for horizontal scrolling
 */
@Composable
fun SourceCardRow(
    sources: List<StreamingSource>,
    onSourceClick: (StreamingSource) -> Unit,
    modifier: Modifier = Modifier,
    selectedSourceId: String? = null,
    variant: SourceCardVariant = SourceCardVariant.DEFAULT,
    maxVisibleSources: Int = 10,
) {
    val sortedSources =
        sources
            .filter { it.isCurrentlyAvailable() }
            .sortedByDescending { it.getPriorityScore() }
            .take(maxVisibleSources)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        sortedSources.forEach { source ->
            SourceCard(
                source = source,
                onClick = onSourceClick,
                isSelected = source.id == selectedSourceId,
                variant = variant,
            )
        }
    }
}

/**
 * Source type badge component
 */
@Composable
fun SourceTypeBadge(
    sourceType: String,
    reliability: String,
    isP2P: Boolean,
    seeders: Int? = null,
    size: QualityBadgeSize = QualityBadgeSize.SMALL,
) {
    val badgeColor =
        when {
            isP2P && (seeders ?: 0) > 50 -> MaterialTheme.colorScheme.primary
            isP2P -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.tertiary
        }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeColor.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = 2.dp),
        ) {
            Text(
                text = sourceType,
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        if (isP2P && seeders != null && seeders > 0) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Text(
                    text = "${seeders}S",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Preview/Demo configurations for SourceCard
 */
object SourceCardPreview {
    @Composable
    fun SampleSourceCards() {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Default Source Cards", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StreamingSource.createSampleSources().take(3).forEach { source ->
                    SourceCard(
                        source = source,
                        onClick = { },
                        variant = SourceCardVariant.DEFAULT,
                    )
                }
            }

            Text("Compact Source Cards", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamingSource.createSampleSources().take(4).forEach { source ->
                    CompactSourceCard(
                        source = source,
                        onClick = { },
                    )
                }
            }

            Text("Detailed Source Cards", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StreamingSource.createSampleSources().take(2).forEach { source ->
                    DetailedSourceCard(
                        source = source,
                        onClick = { },
                    )
                }
            }
        }
    }
}

/**
 * Get color for tracker names to help users distinguish between different trackers
 */
private fun getTrackerColor(
    trackerName: String,
    fallback: Color,
): Color {
    return when (trackerName.uppercase()) {
        "YTS", "YIFY" -> Color(0xFF059669) // Green for YTS (efficient, small files)
        "EZTV", "ETTV" -> Color(0xFF3B82F6) // Blue for EZTV (TV specialist)
        "RARBG" -> Color(0xFF7C3AED) // Purple for RARBG (premium quality)
        "1337X", "LEET" -> Color(0xFF0891B2) // Cyan for 1337x (variety)
        "TPB", "THEPIRATEBAY" -> Color(0xFF6B7280) // Gray for TPB (general)
        "TGX", "TORRENTGALAXY" -> Color(0xFFEC4899) // Pink for TGX
        else -> {
            // Scene groups and unknown trackers get distinctive colors
            when {
                trackerName.length <= 4 -> Color(0xFF8B5CF6) // Purple for scene groups
                else -> fallback // Use default color for unknown trackers
            }
        }
    }
}
