package com.rdwatch.androidtv.ui.details.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.details.models.SourceProvider
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import com.rdwatch.androidtv.ui.focus.TVFocusIndicator
import com.rdwatch.androidtv.ui.focus.tvFocusable

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
    showPricing: Boolean = true,
    showQualityBadges: Boolean = true,
    variant: SourceCardVariant = SourceCardVariant.DEFAULT
) {
    var isFocused by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    val isEnabled = source.isCurrentlyAvailable()
    
    TVFocusIndicator(isFocused = isFocused) {
        OutlinedCard(
            onClick = {
                if (isEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick(source)
                }
            },
            enabled = isEnabled,
            modifier = modifier
                .width(
                    when (variant) {
                        SourceCardVariant.COMPACT -> 120.dp
                        SourceCardVariant.DEFAULT -> 160.dp
                        SourceCardVariant.DETAILED -> 200.dp
                    }
                )
                .tvFocusable(
                    enabled = isEnabled,
                    onFocusChanged = { isFocused = it.isFocused }
                ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = when {
                    !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    isFocused -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            border = when {
                isSelected -> CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary
                        )
                    ),
                    width = 2.dp
                )
                isFocused -> CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    width = 2.dp
                )
                else -> CardDefaults.outlinedCardBorder()
            }
        ) {
            SourceCardContent(
                source = source,
                isFocused = isFocused,
                isSelected = isSelected,
                isEnabled = isEnabled,
                showPricing = showPricing,
                showQualityBadges = showQualityBadges,
                variant = variant
            )
        }
    }
}

@Composable
private fun SourceCardContent(
    source: StreamingSource,
    isFocused: Boolean,
    isSelected: Boolean,
    isEnabled: Boolean,
    showPricing: Boolean,
    showQualityBadges: Boolean,
    variant: SourceCardVariant
) {
    val contentColor = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isSelected -> MaterialTheme.colorScheme.primary
        isFocused -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                when (variant) {
                    SourceCardVariant.COMPACT -> 12.dp
                    SourceCardVariant.DEFAULT -> 16.dp
                    SourceCardVariant.DETAILED -> 20.dp
                }
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Provider logo/icon placeholder and name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProviderLogo(
                provider = source.provider,
                isEnabled = isEnabled,
                size = when (variant) {
                    SourceCardVariant.COMPACT -> 24.dp
                    SourceCardVariant.DEFAULT -> 32.dp
                    SourceCardVariant.DETAILED -> 40.dp
                }
            )
            
            if (!isEnabled) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Unavailable",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Selected",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Provider name
        Text(
            text = source.provider.displayName,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = when (variant) {
                    SourceCardVariant.COMPACT -> 12.sp
                    SourceCardVariant.DEFAULT -> 14.sp
                    SourceCardVariant.DETAILED -> 16.sp
                },
                fontWeight = FontWeight.SemiBold
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Quality badges
        if (showQualityBadges) {
            QualityFeatureBadgeRow(
                quality = source.quality,
                features = source.getQualityBadges().drop(1), // Skip main quality since it's already shown
                maxFeatures = when (variant) {
                    SourceCardVariant.COMPACT -> 1
                    SourceCardVariant.DEFAULT -> 2
                    SourceCardVariant.DETAILED -> 3
                },
                badgeSize = when (variant) {
                    SourceCardVariant.COMPACT -> QualityBadgeSize.SMALL
                    SourceCardVariant.DEFAULT -> QualityBadgeSize.SMALL
                    SourceCardVariant.DETAILED -> QualityBadgeSize.MEDIUM
                }
            )
        }
        
        // Pricing information
        if (showPricing) {
            PricingBadge(
                priceText = source.pricing.getDisplayPrice(),
                isFree = source.pricing.type.name.contains("FREE"),
                size = when (variant) {
                    SourceCardVariant.COMPACT -> QualityBadgeSize.SMALL
                    SourceCardVariant.DEFAULT -> QualityBadgeSize.SMALL
                    SourceCardVariant.DETAILED -> QualityBadgeSize.MEDIUM
                }
            )
        }
        
        // Additional info for detailed variant
        if (variant == SourceCardVariant.DETAILED) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (source.features.hasAds) {
                    Text(
                        text = "With ads",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
                
                if (source.features.supportsDownload) {
                    Text(
                        text = "Download available",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
                
                if (!isEnabled) {
                    Text(
                        text = source.getAvailabilityText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Provider logo placeholder component
 */
@Composable
private fun ProviderLogo(
    provider: SourceProvider,
    isEnabled: Boolean,
    size: dp = 32.dp
) {
    val backgroundColor = provider.color?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primaryContainer
    
    val textColor = if (provider.color != null) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isEnabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = provider.displayName.take(2).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = (size.value / 3).sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isEnabled) textColor else textColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
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
    isSelected: Boolean = false
) {
    SourceCard(
        source = source,
        onClick = onClick,
        modifier = modifier,
        isSelected = isSelected,
        showPricing = false,
        showQualityBadges = true,
        variant = SourceCardVariant.COMPACT
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
    isSelected: Boolean = false
) {
    SourceCard(
        source = source,
        onClick = onClick,
        modifier = modifier,
        isSelected = isSelected,
        showPricing = true,
        showQualityBadges = true,
        variant = SourceCardVariant.DETAILED
    )
}

/**
 * Source card variants
 */
enum class SourceCardVariant {
    COMPACT,    // Minimal info, smaller size
    DEFAULT,    // Standard card with basic info
    DETAILED    // Full info with additional details
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
    maxVisibleSources: Int = 10
) {
    val sortedSources = sources
        .filter { it.isCurrentlyAvailable() }
        .sortedByDescending { it.getPriorityScore() }
        .take(maxVisibleSources)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sortedSources.forEach { source ->
            SourceCard(
                source = source,
                onClick = onSourceClick,
                isSelected = source.id == selectedSourceId,
                variant = variant
            )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Default Source Cards", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StreamingSource.createSampleSources().take(3).forEach { source ->
                    SourceCard(
                        source = source,
                        onClick = { },
                        variant = SourceCardVariant.DEFAULT
                    )
                }
            }
            
            Text("Compact Source Cards", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamingSource.createSampleSources().take(4).forEach { source ->
                    CompactSourceCard(
                        source = source,
                        onClick = { }
                    )
                }
            }
            
            Text("Detailed Source Cards", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StreamingSource.createSampleSources().take(2).forEach { source ->
                    DetailedSourceCard(
                        source = source,
                        onClick = { }
                    )
                }
            }
        }
    }
}