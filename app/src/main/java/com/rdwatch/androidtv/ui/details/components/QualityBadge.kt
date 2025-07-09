package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.details.models.SourceQuality

/**
 * Quality badge component for displaying video quality indicators
 * Used in source cards and quality selection interfaces
 */
@Composable
fun QualityBadge(
    quality: SourceQuality,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    size: QualityBadgeSize = QualityBadgeSize.SMALL,
    variant: QualityBadgeVariant = QualityBadgeVariant.DEFAULT
) {
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
            fontWeight = FontWeight.Bold
        )
        QualityBadgeSize.MEDIUM -> MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        QualityBadgeSize.LARGE -> MaterialTheme.typography.labelLarge.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
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
        }
    }
}