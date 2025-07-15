package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.details.models.SourceQuality
import com.rdwatch.androidtv.ui.details.models.advanced.QualityBadge as AdvancedQualityBadge
import com.rdwatch.androidtv.ui.focus.tvFocusable

/**
 * Quality badge component for displaying video quality indicators
 * Used in source cards and quality selection interfaces
 * TV-optimized with focus support
 */
@Composable
fun QualityBadge(
    quality: SourceQuality,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    size: QualityBadgeSize = QualityBadgeSize.SMALL,
    variant: QualityBadgeVariant = QualityBadgeVariant.DEFAULT,
    focusable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        quality.isHighQuality -> getHighQualityColor(quality)
        else -> getStandardQualityColor(quality)
    }
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        quality.isHighQuality -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    // TV-optimized sizing with better visibility
    val cornerRadius = when (size) {
        QualityBadgeSize.SMALL -> 6.dp
        QualityBadgeSize.MEDIUM -> 8.dp
        QualityBadgeSize.LARGE -> 10.dp
    }
    
    val padding = when (size) {
        QualityBadgeSize.SMALL -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        QualityBadgeSize.MEDIUM -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        QualityBadgeSize.LARGE -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    }
    
    // Larger text for TV viewing distance
    val textStyle = when (size) {
        QualityBadgeSize.SMALL -> MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        QualityBadgeSize.MEDIUM -> MaterialTheme.typography.labelMedium.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        QualityBadgeSize.LARGE -> MaterialTheme.typography.labelLarge.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .then(
                if (focusable) {
                    Modifier
                        .tvFocusable(
                            enabled = focusable,
                            onFocusChanged = { isFocused = it.isFocused },
                            onKeyEvent = null
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(cornerRadius)
                        )
                        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                } else Modifier
            )
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (variant) {
                QualityBadgeVariant.DEFAULT -> quality.shortName
                QualityBadgeVariant.FULL_NAME -> quality.displayName
                QualityBadgeVariant.ICON_ONLY -> quality.shortName.take(2)
            },
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Multi-quality badge component for showing multiple quality options
 */
@Composable
fun MultiQualityBadge(
    qualities: List<SourceQuality>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
    size: QualityBadgeSize = QualityBadgeSize.SMALL,
    spacing: Int = 4
) {
    val sortedQualities = qualities.sortedByDescending { it.priority }
    val visibleQualities = sortedQualities.take(maxVisible)
    val remainingCount = qualities.size - visibleQualities.size
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleQualities.forEach { quality ->
            QualityBadge(
                quality = quality,
                size = size
            )
        }
        
        if (remainingCount > 0) {
            QualityBadge(
                quality = SourceQuality.QUALITY_AUTO, // Use as placeholder
                size = size,
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(4.dp)
                )
            )
        }
    }
}

/**
 * Feature badge for additional streaming features
 */
@Composable
fun FeatureBadge(
    feature: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    size: QualityBadgeSize = QualityBadgeSize.SMALL
) {
    val backgroundColor = when {
        isHighlighted -> MaterialTheme.colorScheme.secondary
        feature.contains("Dolby", ignoreCase = true) -> Color(0xFF1A1A1A)
        feature.contains("HDR", ignoreCase = true) -> Color(0xFF4A90E2)
        feature.contains("4K", ignoreCase = true) -> Color(0xFF8B5CF6)
        feature.contains("8K", ignoreCase = true) -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when {
        isHighlighted -> MaterialTheme.colorScheme.onSecondary
        feature.contains("Dolby", ignoreCase = true) -> Color.White
        feature.contains("HDR", ignoreCase = true) -> Color.White
        feature.contains("4K", ignoreCase = true) -> Color.White
        feature.contains("8K", ignoreCase = true) -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val cornerRadius = when (size) {
        QualityBadgeSize.SMALL -> 4.dp
        QualityBadgeSize.MEDIUM -> 6.dp
        QualityBadgeSize.LARGE -> 8.dp
    }
    
    val padding = when (size) {
        QualityBadgeSize.SMALL -> PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        QualityBadgeSize.MEDIUM -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        QualityBadgeSize.LARGE -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    }
    
    val textStyle = when (size) {
        QualityBadgeSize.SMALL -> MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        QualityBadgeSize.MEDIUM -> MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        QualityBadgeSize.LARGE -> MaterialTheme.typography.labelLarge.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = feature,
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Horizontal row of quality badges - TV-optimized with focus navigation
 */
@Composable
fun QualityBadgeRow(
    badges: List<AdvancedQualityBadge>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 6,
    spacing: Int = 8,
    badgeSize: QualityBadgeSize = QualityBadgeSize.SMALL,
    focusable: Boolean = false,
    onBadgeClick: ((AdvancedQualityBadge) -> Unit)? = null
) {
    val visibleBadges = badges.sortedByDescending { it.priority }.take(maxVisible)
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(visibleBadges) { badge ->
            AdvancedQualityBadgeComponent(
                badge = badge,
                size = badgeSize,
                focusable = focusable,
                onClick = if (onBadgeClick != null) { { onBadgeClick(badge) } } else null
            )
        }
        
        // Show overflow indicator if there are more badges
        if (badges.size > maxVisible) {
            item {
                OverflowBadge(
                    count = badges.size - maxVisible,
                    size = badgeSize
                )
            }
        }
    }
}

/**
 * Combined quality and feature badges row
 */
@Composable
fun QualityFeatureBadgeRow(
    quality: SourceQuality,
    features: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    maxFeatures: Int = 2,
    spacing: Int = 4,
    badgeSize: QualityBadgeSize = QualityBadgeSize.SMALL
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QualityBadge(
            quality = quality,
            size = badgeSize
        )
        
        features.take(maxFeatures).forEach { feature ->
            FeatureBadge(
                feature = feature,
                size = badgeSize
            )
        }
    }
}

/**
 * Pricing badge for showing cost information
 */
@Composable
fun PricingBadge(
    priceText: String,
    modifier: Modifier = Modifier,
    isFree: Boolean = false,
    size: QualityBadgeSize = QualityBadgeSize.SMALL
) {
    val backgroundColor = when {
        isFree -> Color(0xFF10B981)
        priceText.contains("Free", ignoreCase = true) -> Color(0xFF10B981)
        priceText.contains("Rent", ignoreCase = true) -> Color(0xFFF59E0B)
        priceText.contains("Buy", ignoreCase = true) -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    
    val textColor = when {
        isFree -> Color.White
        priceText.contains("Free", ignoreCase = true) -> Color.White
        priceText.contains("Rent", ignoreCase = true) -> Color.White
        priceText.contains("Buy", ignoreCase = true) -> Color.White
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    val cornerRadius = when (size) {
        QualityBadgeSize.SMALL -> 4.dp
        QualityBadgeSize.MEDIUM -> 6.dp
        QualityBadgeSize.LARGE -> 8.dp
    }
    
    val padding = when (size) {
        QualityBadgeSize.SMALL -> PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        QualityBadgeSize.MEDIUM -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        QualityBadgeSize.LARGE -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priceText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = when (size) {
                    QualityBadgeSize.SMALL -> 10.sp
                    QualityBadgeSize.MEDIUM -> 12.sp
                    QualityBadgeSize.LARGE -> 14.sp
                },
                fontWeight = FontWeight.Medium
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Advanced quality badge component for new metadata system
 * Supports different badge types with appropriate styling
 */
@Composable
fun AdvancedQualityBadgeComponent(
    badge: AdvancedQualityBadge,
    modifier: Modifier = Modifier,
    size: QualityBadgeSize = QualityBadgeSize.SMALL,
    focusable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor = getEnhancedBadgeColor(badge)
    val textColor = getEnhancedBadgeTextColor(badge)
    
    val cornerRadius = when (size) {
        QualityBadgeSize.SMALL -> 6.dp
        QualityBadgeSize.MEDIUM -> 8.dp
        QualityBadgeSize.LARGE -> 10.dp
    }
    
    val padding = when (size) {
        QualityBadgeSize.SMALL -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        QualityBadgeSize.MEDIUM -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        QualityBadgeSize.LARGE -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    }
    
    val textStyle = when (size) {
        QualityBadgeSize.SMALL -> MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        QualityBadgeSize.MEDIUM -> MaterialTheme.typography.labelMedium.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        QualityBadgeSize.LARGE -> MaterialTheme.typography.labelLarge.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .then(
                if (focusable) {
                    Modifier
                        .tvFocusable(
                            enabled = focusable,
                            onFocusChanged = { isFocused = it.isFocused },
                            onKeyEvent = null
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(cornerRadius)
                        )
                        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                } else Modifier
            )
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badge.text,
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Overflow badge to show remaining badge count
 */
@Composable
fun OverflowBadge(
    count: Int,
    modifier: Modifier = Modifier,
    size: QualityBadgeSize = QualityBadgeSize.SMALL
) {
    val cornerRadius = when (size) {
        QualityBadgeSize.SMALL -> 6.dp
        QualityBadgeSize.MEDIUM -> 8.dp
        QualityBadgeSize.LARGE -> 10.dp
    }
    
    val padding = when (size) {
        QualityBadgeSize.SMALL -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        QualityBadgeSize.MEDIUM -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        QualityBadgeSize.LARGE -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    }
    
    val textStyle = when (size) {
        QualityBadgeSize.SMALL -> MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        QualityBadgeSize.MEDIUM -> MaterialTheme.typography.labelMedium.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        QualityBadgeSize.LARGE -> MaterialTheme.typography.labelLarge.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+$count",
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Badge size options
 */
enum class QualityBadgeSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Badge variant options
 */
enum class QualityBadgeVariant {
    DEFAULT,      // Shows short name (e.g., "4K")
    FULL_NAME,    // Shows full name (e.g., "4K Ultra HD")
    ICON_ONLY     // Shows minimal text (e.g., "4K")
}

/**
 * Helper functions for quality colors
 */
private fun getHighQualityColor(quality: SourceQuality): Color {
    return when (quality) {
        SourceQuality.QUALITY_8K -> Color(0xFFEF4444)
        SourceQuality.QUALITY_4K, SourceQuality.QUALITY_4K_HDR -> Color(0xFF8B5CF6)
        SourceQuality.QUALITY_1080P_HDR, SourceQuality.QUALITY_720P_HDR -> Color(0xFF4A90E2)
        else -> Color(0xFF10B981)
    }
}

private fun getStandardQualityColor(quality: SourceQuality): Color {
    return when (quality) {
        SourceQuality.QUALITY_1080P -> Color(0xFF6B7280)
        SourceQuality.QUALITY_720P -> Color(0xFF9CA3AF)
        SourceQuality.QUALITY_480P -> Color(0xFFD1D5DB)
        SourceQuality.QUALITY_360P, SourceQuality.QUALITY_240P -> Color(0xFFE5E7EB)
        else -> Color(0xFF6B7280)
    }
}

/**
 * Helper functions for advanced badge colors based on badge type
 * Colors optimized for TV viewing with high contrast
 */
private fun getAdvancedBadgeColor(type: AdvancedQualityBadge.Type): Color {
    return when (type) {
        AdvancedQualityBadge.Type.RESOLUTION -> when {
            // Different colors for different resolutions for quick recognition
            true -> Color(0xFF8B5CF6) // Purple for resolution
            else -> Color(0xFF8B5CF6)
        }
        AdvancedQualityBadge.Type.HDR -> Color(0xFF4A90E2) // Blue for HDR
        AdvancedQualityBadge.Type.CODEC -> Color(0xFF10B981) // Green for codec
        AdvancedQualityBadge.Type.AUDIO -> Color(0xFFF59E0B) // Orange for audio
        AdvancedQualityBadge.Type.RELEASE -> Color(0xFF6366F1) // Indigo for release type
        AdvancedQualityBadge.Type.HEALTH -> Color(0xFFEF4444) // Red for health (P2P)
        AdvancedQualityBadge.Type.FEATURE -> Color(0xFF06B6D4) // Cyan for features
        AdvancedQualityBadge.Type.PROVIDER -> Color(0xFF84CC16) // Lime for provider
    }
}

/**
 * Get enhanced badge color based on specific badge content
 * Provides more granular color coding for better TV visibility
 */
private fun getEnhancedBadgeColor(badge: AdvancedQualityBadge): Color {
    return when (badge.type) {
        AdvancedQualityBadge.Type.RESOLUTION -> when (badge.text) {
            "8K" -> Color(0xFFDC2626) // Red for 8K
            "4K" -> Color(0xFF8B5CF6) // Purple for 4K
            "1440p" -> Color(0xFF7C3AED) // Violet for 2K
            "1080p" -> Color(0xFF2563EB) // Blue for 1080p
            "720p" -> Color(0xFF059669) // Green for 720p
            else -> Color(0xFF6B7280) // Gray for lower resolutions
        }
        AdvancedQualityBadge.Type.HDR -> when (badge.text) {
            "DV" -> Color(0xFF000000) // Black for Dolby Vision with white text
            "HDR10+" -> Color(0xFF1E40AF) // Dark blue for HDR10+
            "HDR10" -> Color(0xFF4A90E2) // Blue for HDR10
            else -> Color(0xFF4A90E2)
        }
        AdvancedQualityBadge.Type.CODEC -> when (badge.text) {
            "AV1" -> Color(0xFF059669) // Emerald for AV1
            "H.265", "HEVC" -> Color(0xFF10B981) // Green for HEVC
            "H.264", "AVC" -> Color(0xFF34D399) // Light green for H.264
            else -> Color(0xFF6EE7B7) // Pale green for others
        }
        AdvancedQualityBadge.Type.AUDIO -> when {
            badge.text.contains("Atmos") -> Color(0xFF1F2937) // Near black for Atmos
            badge.text.contains("DTS:X") -> Color(0xFF374151) // Dark gray for DTS:X
            badge.text.contains("TrueHD") -> Color(0xFFEA580C) // Dark orange for TrueHD
            badge.text.contains("DTS-HD") -> Color(0xFFF59E0B) // Orange for DTS-HD
            else -> Color(0xFFFBBF24) // Yellow for standard audio
        }
        AdvancedQualityBadge.Type.RELEASE -> when (badge.text) {
            "REMUX" -> Color(0xFF7C3AED) // Violet for REMUX
            "BluRay" -> Color(0xFF2563EB) // Blue for BluRay
            "WEB-DL" -> Color(0xFF0891B2) // Cyan for WEB-DL
            "WebRip" -> Color(0xFF0EA5E9) // Light blue for WebRip
            else -> Color(0xFF6B7280) // Gray for others
        }
        AdvancedQualityBadge.Type.HEALTH -> when {
            badge.text.contains("1000") -> Color(0xFF059669) // Green for excellent health
            badge.text.contains("500") -> Color(0xFF10B981) // Light green for good health
            badge.text.contains("100") -> Color(0xFFF59E0B) // Orange for fair health
            else -> Color(0xFFEF4444) // Red for poor health
        }
        AdvancedQualityBadge.Type.FEATURE -> when {
            badge.text.contains("Cached") -> Color(0xFF059669) // Green for cached
            badge.text.contains("Pack") -> Color(0xFF8B5CF6) // Purple for season pack
            else -> Color(0xFF06B6D4) // Cyan for other features
        }
        else -> getAdvancedBadgeColor(badge.type)
    }
}

private fun getAdvancedBadgeTextColor(type: AdvancedQualityBadge.Type): Color {
    // Most badges use white text for better contrast
    return Color.White
}

/**
 * Get text color with consideration for specific badge backgrounds
 */
private fun getEnhancedBadgeTextColor(badge: AdvancedQualityBadge): Color {
    return when {
        badge.type == AdvancedQualityBadge.Type.HDR && badge.text == "DV" -> Color.White
        badge.type == AdvancedQualityBadge.Type.AUDIO && badge.text.contains("Atmos") -> Color.White
        badge.type == AdvancedQualityBadge.Type.AUDIO && badge.text.contains("DTS:X") -> Color.White
        else -> Color.White
    }
}

/**
 * Preview/Demo configurations for QualityBadge
 */
object QualityBadgePreview {
    @Composable
    fun AllQualityBadges() {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("High Quality Badges", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceQuality.getHighQualityOptions().forEach { quality ->
                    QualityBadge(quality = quality)
                }
            }
            
            Text("Standard Quality Badges", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceQuality.getStandardQualityOptions().forEach { quality ->
                    QualityBadge(quality = quality)
                }
            }
            
            Text("Feature Badges", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureBadge("Dolby Vision")
                FeatureBadge("Dolby Atmos")
                FeatureBadge("HDR10")
                FeatureBadge("Download")
            }
            
            Text("Pricing Badges", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PricingBadge("Free", isFree = true)
                PricingBadge("Free with ads")
                PricingBadge("Rent $4.99")
                PricingBadge("Buy $14.99")
            }
            
            Text("Advanced Quality Badges", style = MaterialTheme.typography.headlineSmall)
            QualityBadgeRow(
                badges = listOf(
                    AdvancedQualityBadge("4K", AdvancedQualityBadge.Type.RESOLUTION, 100),
                    AdvancedQualityBadge("DV", AdvancedQualityBadge.Type.HDR, 95),
                    AdvancedQualityBadge("H.265", AdvancedQualityBadge.Type.CODEC, 80),
                    AdvancedQualityBadge("Atmos", AdvancedQualityBadge.Type.AUDIO, 70),
                    AdvancedQualityBadge("REMUX", AdvancedQualityBadge.Type.RELEASE, 60),
                    AdvancedQualityBadge("150S", AdvancedQualityBadge.Type.HEALTH, 50)
                )
            )
            
            Text("TV Focus-enabled Badges", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QualityBadge(
                    quality = SourceQuality.QUALITY_4K,
                    focusable = true,
                    onClick = { /* Handle click */ }
                )
                FeatureBadge(
                    feature = "HDR10",
                    isHighlighted = true
                )
            }
        }
    }
}