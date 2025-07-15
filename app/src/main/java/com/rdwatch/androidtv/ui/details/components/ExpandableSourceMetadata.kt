package com.rdwatch.androidtv.ui.details.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata
import com.rdwatch.androidtv.ui.focus.tvFocusable
import java.util.Date

/**
 * Expandable source metadata display with progressive disclosure
 * Optimized for TV navigation with focus states and comprehensive information display
 */
@Composable
fun ExpandableSourceMetadata(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    showTooltips: Boolean = true,
    maxCollapsedItems: Int = 3
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    var isFocused by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }
    var tooltipType by remember { mutableStateOf(TooltipType.QUICK_INFO) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                enabled = true,
                onFocusChanged = { isFocused = it.isFocused },
                onKeyEvent = null
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with expand/collapse toggle
            MetadataHeader(
                sourceMetadata = sourceMetadata,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded },
                onShowTooltip = if (showTooltips) {
                    { type ->
                        tooltipType = type
                        showTooltip = true
                    }
                } else null
            )
            
            // Progressive disclosure of metadata
            AnimatedVisibility(
                visible = true,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                if (isExpanded) {
                    ExpandedMetadataContent(sourceMetadata = sourceMetadata)
                } else {
                    CollapsedMetadataContent(
                        sourceMetadata = sourceMetadata,
                        maxItems = maxCollapsedItems
                    )
                }
            }
        }
    }
    
    // Tooltip display
    if (showTooltips && showTooltip) {
        MetadataTooltip(
            sourceMetadata = sourceMetadata,
            isVisible = showTooltip,
            onDismiss = { showTooltip = false },
            tooltipType = tooltipType
        )
    }
}

/**
 * Header with provider info and expand/collapse controls
 */
@Composable
private fun MetadataHeader(
    sourceMetadata: SourceMetadata,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onShowTooltip: ((TooltipType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider and basic info
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Provider icon
            ProviderIcon(
                provider = sourceMetadata.provider,
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = sourceMetadata.provider.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "${sourceMetadata.quality.resolution.displayName} • ${sourceMetadata.codec.type.shortName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quality score indicator
            QualityScoreChip(
                score = sourceMetadata.getQualityScore(),
                onClick = onShowTooltip?.let { { it(TooltipType.COMPREHENSIVE) } }
            )
            
            // Info button for tooltip
            if (onShowTooltip != null) {
                IconButton(
                    onClick = { onShowTooltip(TooltipType.QUICK_INFO) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Show details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Expand/collapse button
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Collapsed metadata view showing essential information only
 */
@Composable
private fun CollapsedMetadataContent(
    sourceMetadata: SourceMetadata,
    maxItems: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact badge row
        CompactBadgeRow(
            sourceMetadata = sourceMetadata,
            maxBadges = 4
        )
        
        // Essential metadata row
        EssentialMetadataRow(
            sourceMetadata = sourceMetadata,
            maxItems = maxItems
        )
    }
}

/**
 * Expanded metadata view showing comprehensive information
 */
@Composable
private fun ExpandedMetadataContent(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Complete badge display
        item {
            SourceBadgeContainer(
                sourceMetadata = sourceMetadata,
                badgeSize = QualityBadgeSize.MEDIUM,
                showProvider = false
            )
        }
        
        // File information
        item {
            MetadataSection(
                title = "File Information",
                items = buildFileMetadataItems(sourceMetadata.file)
            )
        }
        
        // Technical specifications
        item {
            MetadataSection(
                title = "Technical Specifications",
                items = buildTechnicalMetadataItems(sourceMetadata)
            )
        }
        
        // Release information
        item {
            MetadataSection(
                title = "Release Information",
                items = buildReleaseMetadataItems(sourceMetadata.release)
            )
        }
        
        // P2P health information (if applicable)
        if (sourceMetadata.health.seeders != null) {
            item {
                P2PHealthSection(
                    health = sourceMetadata.health
                )
            }
        }
        
        // Availability information
        item {
            MetadataSection(
                title = "Availability",
                items = buildAvailabilityMetadataItems(sourceMetadata.availability)
            )
        }
        
        // Features information
        if (sourceMetadata.features.subtitles.isNotEmpty() || 
            sourceMetadata.features.has3D || 
            sourceMetadata.features.hasChapters) {
            item {
                MetadataSection(
                    title = "Features",
                    items = buildFeaturesMetadataItems(sourceMetadata.features)
                )
            }
        }
    }
}

/**
 * Essential metadata row for collapsed view
 */
@Composable
internal fun EssentialMetadataRow(
    sourceMetadata: SourceMetadata,
    maxItems: Int,
    modifier: Modifier = Modifier
) {
    val essentialItems = buildList {
        // File size
        sourceMetadata.file.getFormattedSize()?.let { size ->
            add(MetadataChip("Size", size, ChipType.INFO))
        }
        
        // Audio format
        add(MetadataChip(
            "Audio", 
            if (sourceMetadata.audio.dolbyAtmos) "Atmos" 
            else if (sourceMetadata.audio.dtsX) "DTS:X" 
            else sourceMetadata.audio.format.shortName,
            ChipType.AUDIO
        ))
        
        // Release type
        add(MetadataChip(
            "Release", 
            sourceMetadata.release.type.shortName,
            ChipType.RELEASE
        ))
        
        // Health for P2P
        sourceMetadata.health.seeders?.let { seeders ->
            if (seeders > 0) {
                add(MetadataChip(
                    "Health", 
                    if (seeders > 100) "${seeders}S" else "${seeders}S/${sourceMetadata.health.leechers ?: 0}L",
                    ChipType.HEALTH
                ))
            }
        }
        
        // Cached status
        if (sourceMetadata.availability.cached) {
            add(MetadataChip("Status", "CACHED", ChipType.CACHED))
        }
        
        // Age indicator
        sourceMetadata.file.addedDate?.let { date ->
            val ageText = getAgeText(date)
            if (ageText != null) {
                add(MetadataChip("Age", ageText, ChipType.AGE))
            }
        }
    }
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(essentialItems.take(maxItems)) { chip ->
            MetadataChipComponent(chip = chip)
        }
        
        if (essentialItems.size > maxItems) {
            item {
                MetadataChipComponent(
                    chip = MetadataChip("", "+${essentialItems.size - maxItems}", ChipType.OVERFLOW)
                )
            }
        }
    }
}

/**
 * Quality score chip component
 */
@Composable
private fun QualityScoreChip(
    score: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scoreGrade = when {
        score > 1000 -> "A+"
        score > 900 -> "A"
        score > 800 -> "B+"
        score > 700 -> "B"
        score > 600 -> "C+"
        score > 500 -> "C"
        else -> "D"
    }
    
    val scoreColor = when {
        score > 1000 -> Color(0xFF8B5CF6)
        score > 800 -> Color(0xFF3B82F6)
        score > 600 -> Color(0xFF10B981)
        score > 400 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    
    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(
                width = 1.dp,
                color = scoreColor,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = scoreColor.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = scoreGrade,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = scoreColor
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = scoreColor
            )
        }
    }
}

/**
 * Metadata section component for organized display
 */
@Composable
private fun MetadataSection(
    title: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        
        items.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Metadata chip component for compact display
 */
@Composable
private fun MetadataChipComponent(
    chip: MetadataChip,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = getChipColors(chip.type)
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
        contentColor = textColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (chip.label.isNotEmpty()) {
                Text(
                    text = "${chip.label}:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            Text(
                text = chip.value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = textColor
            )
        }
    }
}

/**
 * Data classes and enums for metadata display
 */
data class MetadataChip(
    val label: String,
    val value: String,
    val type: ChipType
)

enum class ChipType {
    INFO, AUDIO, RELEASE, HEALTH, CACHED, AGE, OVERFLOW
}

/**
 * Helper functions for building metadata items
 */
private fun buildFileMetadataItems(file: com.rdwatch.androidtv.ui.details.models.advanced.FileInfo): List<Pair<String, String>> {
    return buildList {
        file.getFormattedSize()?.let { size ->
            add("Size" to size)
        }
        file.extension?.let { ext ->
            add("Format" to ext.uppercase())
        }
        file.hash?.let { hash ->
            add("Hash" to hash.take(16) + "...")
        }
        file.addedDate?.let { date ->
            add("Added" to formatTimeAgo(date))
        }
    }
}

private fun buildTechnicalMetadataItems(sourceMetadata: SourceMetadata): List<Pair<String, String>> {
    return buildList {
        add("Resolution" to sourceMetadata.quality.resolution.displayName)
        add("Codec" to sourceMetadata.codec.getDisplayText())
        add("Audio" to sourceMetadata.audio.getDisplayText())
        
        sourceMetadata.quality.frameRate?.let { fps ->
            add("Frame Rate" to "${fps} fps")
        }
        
        sourceMetadata.quality.bitrate?.let { bitrate ->
            add("Video Bitrate" to "${bitrate / 1_000_000} Mbps")
        }
        
        sourceMetadata.audio.bitrate?.let { bitrate ->
            add("Audio Bitrate" to "$bitrate kbps")
        }
        
        if (sourceMetadata.quality.hasHDR()) {
            val hdrType = when {
                sourceMetadata.quality.dolbyVision -> "Dolby Vision"
                sourceMetadata.quality.hdr10Plus -> "HDR10+"
                sourceMetadata.quality.hdr10 -> "HDR10"
                else -> "None"
            }
            add("HDR" to hdrType)
        }
    }
}

private fun buildReleaseMetadataItems(release: com.rdwatch.androidtv.ui.details.models.advanced.ReleaseInfo): List<Pair<String, String>> {
    return buildList {
        add("Type" to release.type.displayName)
        release.group?.let { group ->
            add("Group" to group)
        }
        release.edition?.let { edition ->
            add("Edition" to edition)
        }
        release.year?.let { year ->
            add("Year" to year.toString())
        }
    }
}

private fun buildAvailabilityMetadataItems(availability: com.rdwatch.androidtv.ui.details.models.advanced.AvailabilityInfo): List<Pair<String, String>> {
    return buildList {
        add("Available" to if (availability.isAvailable) "Yes" else "No")
        if (availability.cached) {
            add("Status" to "Cached")
        }
        availability.debridService?.let { service ->
            add("Debrid Service" to service.uppercase())
        }
        availability.region?.let { region ->
            add("Region" to region.uppercase())
        }
        availability.expiryDate?.let { expiry ->
            add("Expires" to formatTimeAgo(expiry))
        }
    }
}

private fun buildFeaturesMetadataItems(features: com.rdwatch.androidtv.ui.details.models.advanced.FeatureInfo): List<Pair<String, String>> {
    return buildList {
        if (features.subtitles.isNotEmpty()) {
            add("Subtitles" to "${features.subtitles.size} languages")
        }
        if (features.has3D) {
            add("3D Support" to "Yes")
        }
        if (features.hasChapters) {
            add("Chapters" to "Yes")
        }
        if (features.hasMultipleAudioTracks) {
            add("Multiple Audio" to "Yes")
        }
        if (features.isDirectPlay) {
            add("Direct Play" to "Yes")
        }
        if (features.requiresTranscoding) {
            add("Transcoding" to "Required")
        }
    }
}

private fun getChipColors(type: ChipType): Pair<Color, Color> {
    return when (type) {
        ChipType.INFO -> Color(0xFF6B7280) to Color.White
        ChipType.AUDIO -> Color(0xFFF59E0B) to Color.White
        ChipType.RELEASE -> Color(0xFF6366F1) to Color.White
        ChipType.HEALTH -> Color(0xFF10B981) to Color.White
        ChipType.CACHED -> Color(0xFF059669) to Color.White
        ChipType.AGE -> Color(0xFF84CC16) to Color.White
        ChipType.OVERFLOW -> Color(0xFF9CA3AF) to Color.White
    }
}

private fun getAgeText(date: Date): String? {
    val now = Date()
    val ageInDays = ((now.time - date.time) / (1000 * 60 * 60 * 24)).toInt()
    
    return when {
        ageInDays < 1 -> "NEW"
        ageInDays < 7 -> "${ageInDays}d"
        ageInDays < 30 -> "${ageInDays}d"
        ageInDays > 365 -> null // Don't show very old content age
        else -> null
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