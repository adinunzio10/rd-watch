package com.rdwatch.androidtv.ui.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata
import java.util.Date

/**
 * Tooltip component for displaying detailed metadata
 * Optimized for TV with larger text and clear information hierarchy
 */
@Composable
fun MetadataTooltip(
    sourceMetadata: SourceMetadata,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tooltipType: TooltipType = TooltipType.COMPREHENSIVE,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
    ) {
        Popup(
            onDismissRequest = onDismiss,
            properties =
                PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
        ) {
            Card(
                modifier =
                    modifier
                        .widthIn(min = 400.dp, max = 600.dp)
                        .heightIn(max = 500.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(16.dp),
                        ),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            ) {
                when (tooltipType) {
                    TooltipType.QUICK_INFO -> QuickInfoTooltip(sourceMetadata)
                    TooltipType.TECHNICAL_SPECS -> TechnicalSpecsTooltip(sourceMetadata)
                    TooltipType.P2P_HEALTH -> P2PHealthTooltip(sourceMetadata)
                    TooltipType.COMPREHENSIVE -> ComprehensiveTooltip(sourceMetadata)
                }
            }
        }
    }
}

/**
 * Quick info tooltip with essential metadata
 */
@Composable
private fun QuickInfoTooltip(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Text(
            text = "Source Information",
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )

        // Essential info
        InfoRow(
            label = "Quality",
            value = sourceMetadata.quality.getDisplayText(),
            valueColor = MaterialTheme.colorScheme.primary,
        )

        InfoRow(
            label = "Codec",
            value = sourceMetadata.codec.getDisplayText(),
        )

        InfoRow(
            label = "Audio",
            value = sourceMetadata.audio.getDisplayText(),
        )

        sourceMetadata.file.getFormattedSize()?.let { size ->
            InfoRow(
                label = "Size",
                value = size,
            )
        }

        InfoRow(
            label = "Provider",
            value = sourceMetadata.provider.displayName,
            valueColor = getProviderColor(sourceMetadata.provider.type),
        )

        // Health info for P2P
        sourceMetadata.health.seeders?.let { seeders ->
            val leechers = sourceMetadata.health.leechers ?: 0
            InfoRow(
                label = "Health",
                value = "${seeders}S / ${leechers}L",
                valueColor = getHealthColor(seeders),
            )
        }

        // Quality score
        InfoRow(
            label = "Score",
            value = "${sourceMetadata.getQualityScore()}",
            valueColor = getScoreColor(sourceMetadata.getQualityScore()),
        )
    }
}

/**
 * Technical specifications tooltip
 */
@Composable
private fun TechnicalSpecsTooltip(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Technical Specifications",
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
        }

        // Video specifications
        item {
            SpecificationSection(
                title = "Video",
                items =
                    buildList {
                        add("Resolution" to sourceMetadata.quality.resolution.displayName)
                        sourceMetadata.quality.frameRate?.let { fps ->
                            add("Frame Rate" to "$fps fps")
                        }
                        sourceMetadata.quality.bitrate?.let { bitrate ->
                            add("Bitrate" to "${bitrate / 1_000_000} Mbps")
                        }
                        if (sourceMetadata.quality.hasHDR()) {
                            val hdrType =
                                when {
                                    sourceMetadata.quality.dolbyVision -> "Dolby Vision"
                                    sourceMetadata.quality.hdr10Plus -> "HDR10+"
                                    sourceMetadata.quality.hdr10 -> "HDR10"
                                    else -> "None"
                                }
                            add("HDR" to hdrType)
                        }
                    },
            )
        }

        // Codec specifications
        item {
            SpecificationSection(
                title = "Codec",
                items =
                    buildList {
                        add("Type" to sourceMetadata.codec.type.displayName)
                        add("Efficiency" to getEfficiencyRating(sourceMetadata.codec.type))
                        sourceMetadata.codec.profile?.let { profile ->
                            add("Profile" to profile)
                        }
                        sourceMetadata.codec.level?.let { level ->
                            add("Level" to level)
                        }
                    },
            )
        }

        // Audio specifications
        item {
            SpecificationSection(
                title = "Audio",
                items =
                    buildList {
                        when {
                            sourceMetadata.audio.dolbyAtmos -> add("Format" to "Dolby Atmos")
                            sourceMetadata.audio.dtsX -> add("Format" to "DTS:X")
                            else -> add("Format" to sourceMetadata.audio.format.displayName)
                        }
                        sourceMetadata.audio.channels?.let { channels ->
                            add("Channels" to channels)
                        }
                        sourceMetadata.audio.bitrate?.let { bitrate ->
                            add("Bitrate" to "$bitrate kbps")
                        }
                        sourceMetadata.audio.language?.let { language ->
                            add("Language" to language)
                        }
                    },
            )
        }
    }
}

/**
 * P2P health tooltip with download statistics
 */
@Composable
private fun P2PHealthTooltip(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "P2P Health & Statistics",
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )

        val health = sourceMetadata.health

        // Peer information
        health.seeders?.let { seeders ->
            val leechers = health.leechers ?: 0
            val ratio = if (leechers > 0) seeders.toFloat() / leechers.toFloat() else Float.MAX_VALUE

            InfoRow(
                label = "Seeders",
                value = seeders.toString(),
                valueColor = getHealthColor(seeders),
            )

            InfoRow(
                label = "Leechers",
                value = leechers.toString(),
            )

            InfoRow(
                label = "Ratio",
                value = if (ratio == Float.MAX_VALUE) "∞" else String.format("%.2f", ratio),
                valueColor = getRatioColor(ratio),
            )

            InfoRow(
                label = "Health Status",
                value = health.getHealthStatus().name.lowercase().replaceFirstChar { it.uppercase() },
                valueColor = getHealthStatusColor(health.getHealthStatus()),
            )
        }

        // Availability
        health.availability?.let { availability ->
            InfoRow(
                label = "Availability",
                value = "${(availability * 100).toInt()}%",
                valueColor = getAvailabilityColor(availability),
            )
        }

        // Download speeds
        health.downloadSpeed?.let { speed ->
            InfoRow(
                label = "Download Speed",
                value = formatSpeed(speed),
            )
        }

        health.uploadSpeed?.let { speed ->
            InfoRow(
                label = "Upload Speed",
                value = formatSpeed(speed),
            )
        }

        // Last checked
        health.lastChecked?.let { lastChecked ->
            InfoRow(
                label = "Last Checked",
                value = formatTimeAgo(lastChecked),
            )
        }
    }
}

/**
 * Comprehensive tooltip with all available metadata
 */
@Composable
private fun ComprehensiveTooltip(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = sourceMetadata.file.name ?: "Source Details",
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
        }

        // Provider & file info
        item {
            SpecificationSection(
                title = "Source Information",
                items =
                    buildList {
                        add("Provider" to sourceMetadata.provider.displayName)
                        add("Type" to sourceMetadata.provider.type.name.lowercase().replaceFirstChar { it.uppercase() })
                        add("Reliability" to sourceMetadata.provider.reliability.name.lowercase().replaceFirstChar { it.uppercase() })
                        sourceMetadata.file.getFormattedSize()?.let { size ->
                            add("File Size" to size)
                        }
                        sourceMetadata.file.extension?.let { ext ->
                            add("Format" to ext.uppercase())
                        }
                        sourceMetadata.file.addedDate?.let { date ->
                            add("Added" to formatTimeAgo(date))
                        }
                    },
            )
        }

        // Technical specs
        item {
            SpecificationSection(
                title = "Technical Specifications",
                items =
                    buildList {
                        add("Quality" to sourceMetadata.quality.getDisplayText())
                        add("Codec" to sourceMetadata.codec.getDisplayText())
                        add("Audio" to sourceMetadata.audio.getDisplayText())
                        add("Release Type" to sourceMetadata.release.type.displayName)
                        sourceMetadata.release.group?.let { group ->
                            add("Release Group" to group)
                        }
                        sourceMetadata.release.edition?.let { edition ->
                            add("Edition" to edition)
                        }
                    },
            )
        }

        // Availability info
        item {
            SpecificationSection(
                title = "Availability",
                items =
                    buildList {
                        add("Available" to if (sourceMetadata.availability.isAvailable) "Yes" else "No")
                        if (sourceMetadata.availability.cached) {
                            add("Status" to "Cached")
                        }
                        sourceMetadata.availability.debridService?.let { service ->
                            add("Debrid Service" to service.uppercase())
                        }
                        sourceMetadata.availability.region?.let { region ->
                            add("Region" to region.uppercase())
                        }
                        sourceMetadata.availability.expiryDate?.let { expiry ->
                            add("Expires" to formatTimeAgo(expiry))
                        }
                    },
            )
        }

        // P2P health if available
        if (sourceMetadata.health.seeders != null) {
            item {
                SpecificationSection(
                    title = "P2P Health",
                    items =
                        buildList {
                            sourceMetadata.health.seeders?.let { seeders ->
                                add("Seeders" to seeders.toString())
                            }
                            sourceMetadata.health.leechers?.let { leechers ->
                                add("Leechers" to leechers.toString())
                            }
                            sourceMetadata.health.availability?.let { availability ->
                                add("Availability" to "${(availability * 100).toInt()}%")
                            }
                            sourceMetadata.health.lastChecked?.let { lastChecked ->
                                add("Last Checked" to formatTimeAgo(lastChecked))
                            }
                        },
                )
            }
        }

        // Quality score
        item {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Overall Quality Score",
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = "${sourceMetadata.getQualityScore()}",
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = getScoreColor(sourceMetadata.getQualityScore()),
                    )
                }
            }
        }
    }
}

/**
 * Specification section component
 */
@Composable
private fun SpecificationSection(
    title: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.primary,
        )

        items.forEach { (label, value) ->
            InfoRow(
                label = label,
                value = value,
            )
        }
    }
}

/**
 * Information row component
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Tooltip types for different levels of detail
 */
enum class TooltipType {
    QUICK_INFO, // Essential information only
    TECHNICAL_SPECS, // Detailed technical specifications
    P2P_HEALTH, // P2P download statistics
    COMPREHENSIVE, // All available metadata
}

/**
 * Helper functions for color coding
 */
private fun getProviderColor(type: com.rdwatch.androidtv.ui.details.models.advanced.SourceProviderInfo.ProviderType): Color {
    return when (type) {
        com.rdwatch.androidtv.ui.details.models.advanced.SourceProviderInfo.ProviderType.TORRENT -> Color(0xFF8B5CF6)
        com.rdwatch.androidtv.ui.details.models.advanced.SourceProviderInfo.ProviderType.DIRECT_STREAM -> Color(0xFF3B82F6)
        com.rdwatch.androidtv.ui.details.models.advanced.SourceProviderInfo.ProviderType.DEBRID -> Color(0xFF10B981)
        else -> Color(0xFF6B7280)
    }
}

private fun getHealthColor(seeders: Int): Color {
    return when {
        seeders > 1000 -> Color(0xFF059669)
        seeders > 500 -> Color(0xFF10B981)
        seeders > 100 -> Color(0xFF84CC16)
        seeders > 50 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
}

private fun getRatioColor(ratio: Float): Color {
    return when {
        ratio >= 2f -> Color(0xFF059669)
        ratio >= 1f -> Color(0xFF84CC16)
        ratio >= 0.5f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
}

private fun getHealthStatusColor(status: com.rdwatch.androidtv.ui.details.models.advanced.HealthInfo.HealthStatus): Color {
    return when (status) {
        com.rdwatch.androidtv.ui.details.models.advanced.HealthInfo.HealthStatus.EXCELLENT -> Color(0xFF059669)
        com.rdwatch.androidtv.ui.details.models.advanced.HealthInfo.HealthStatus.GOOD -> Color(0xFF10B981)
        com.rdwatch.androidtv.ui.details.models.advanced.HealthInfo.HealthStatus.FAIR -> Color(0xFF84CC16)
        com.rdwatch.androidtv.ui.details.models.advanced.HealthInfo.HealthStatus.POOR -> Color(0xFFF59E0B)
        com.rdwatch.androidtv.ui.details.models.advanced.HealthInfo.HealthStatus.DEAD -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }
}

private fun getAvailabilityColor(availability: Float): Color {
    return when {
        availability >= 0.9f -> Color(0xFF059669)
        availability >= 0.7f -> Color(0xFF84CC16)
        availability >= 0.5f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
}

private fun getScoreColor(score: Int): Color {
    return when {
        score > 1000 -> Color(0xFF8B5CF6)
        score > 800 -> Color(0xFF3B82F6)
        score > 600 -> Color(0xFF10B981)
        score > 400 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
}

private fun getEfficiencyRating(codec: com.rdwatch.androidtv.ui.details.models.advanced.VideoCodec): String {
    return when (codec.efficiencyBonus) {
        in 45..Int.MAX_VALUE -> "Excellent"
        in 35..44 -> "Very Good"
        in 25..34 -> "Good"
        in 15..24 -> "Fair"
        else -> "Poor"
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond >= 1_000_000_000L -> String.format("%.1f GB/s", bytesPerSecond / 1_000_000_000.0)
        bytesPerSecond >= 1_000_000L -> String.format("%.1f MB/s", bytesPerSecond / 1_000_000.0)
        bytesPerSecond >= 1_000L -> String.format("%.1f KB/s", bytesPerSecond / 1_000.0)
        else -> "$bytesPerSecond B/s"
    }
}

private fun formatTimeAgo(date: Date): String {
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

    return when {
        diffInDays < 1 -> "Today"
        diffInDays < 7 -> "${diffInDays}d ago"
        diffInDays < 30 -> "${diffInDays / 7}w ago"
        diffInDays < 365 -> "${diffInDays / 30}mo ago"
        else -> "${diffInDays / 365}y ago"
    }
}
