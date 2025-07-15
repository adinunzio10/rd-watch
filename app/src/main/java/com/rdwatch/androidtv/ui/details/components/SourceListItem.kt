package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata
import com.rdwatch.androidtv.ui.focus.tvFocusable
import java.util.Date

/**
 * Source list item component for displaying streaming sources
 * Optimized for TV navigation with focus states and quality badges
 */
@Composable
fun SourceListItem(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val cardColors = when {
        isSelected -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
        isFocused -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
        else -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .tvFocusable(
                enabled = true,
                onFocusChanged = { isFocused = it.isFocused },
                onKeyEvent = null
            )
            .border(
                width = if (isFocused || isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        colors = cardColors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provider icon/logo area
            ProviderIcon(
                provider = sourceMetadata.provider,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content area
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title row with file name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sourceMetadata.file.name ?: "Unknown Source",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // File size
                    sourceMetadata.file.getFormattedSize()?.let { size ->
                        Text(
                            text = size,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Quality badges row
                CompactBadgeRow(
                    sourceMetadata = sourceMetadata,
                    maxBadges = 5
                )
                
                // Enhanced metadata row with smart progressive disclosure
                EnhancedMetadataRow(
                    sourceMetadata = sourceMetadata,
                    compact = true
                )
            }
        }
    }
}

/**
 * Provider icon/logo component
 */
@Composable
fun ProviderIcon(
    provider: com.rdwatch.androidtv.ui.details.models.advanced.SourceProviderInfo,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (provider.type) {
        com.rdwatch.androidtv.ui.details.models.advanced.SourceProviderInfo.ProviderType.TORRENT -> Color(0xFF8B5CF6)
        com.rdwatch.androidtv.ui.details.models.advanced.SourceProviderInfo.ProviderType.DIRECT_STREAM -> Color(0xFF3B82F6)
        com.rdwatch.androidtv.ui.details.models.advanced.SourceProviderInfo.ProviderType.DEBRID -> Color(0xFF10B981)
        else -> Color(0xFF6B7280)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = backgroundColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = provider.displayName.take(2).uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = backgroundColor
        )
    }
}

/**
 * Health status text for P2P sources
 */
@Composable
fun HealthStatusText(
    seeders: Int,
    leechers: Int,
    modifier: Modifier = Modifier
) {
    val healthColor = when {
        seeders > 1000 -> Color(0xFF059669)
        seeders > 500 -> Color(0xFF10B981)
        seeders > 100 -> Color(0xFF84CC16)
        seeders > 50 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    
    Text(
        text = "${seeders}S/${leechers}L",
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        ),
        color = healthColor,
        modifier = modifier
    )
}

/**
 * Expanded source list item with more details
 */
@Composable
fun ExpandedSourceListItem(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                enabled = true,
                onFocusChanged = { isFocused = it.isFocused },
                onKeyEvent = null
            )
            .clickable { onClick() },
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with provider and file info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sourceMetadata.file.name ?: "Unknown Source",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sourceMetadata.provider.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        sourceMetadata.file.getFormattedSize()?.let { size ->
                            Text(
                                text = size,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Quality score indicator
                QualityScoreIndicator(
                    score = sourceMetadata.getQualityScore(),
                    modifier = Modifier.size(60.dp)
                )
            }
            
            // Full badge display
            SourceBadgeContainer(
                sourceMetadata = sourceMetadata,
                badgeSize = QualityBadgeSize.MEDIUM,
                showProvider = false // Already shown in header
            )
            
            // Enhanced metadata details with comprehensive technical specs
            DetailedMetadataSection(
                sourceMetadata = sourceMetadata,
                showTechnicalSpecs = true,
                showP2PHealth = true,
                showAvailabilityInfo = true,
                showReleaseDetails = true
            )
        }
    }
}

/**
 * Quality score indicator
 */
@Composable
fun QualityScoreIndicator(
    score: Int,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score > 1000 -> Color(0xFF8B5CF6) // Purple for excellent
        score > 800 -> Color(0xFF3B82F6) // Blue for very good
        score > 600 -> Color(0xFF10B981) // Green for good
        score > 400 -> Color(0xFFF59E0B) // Orange for fair
        else -> Color(0xFFEF4444) // Red for poor
    }
    
    val scoreText = when {
        score > 1000 -> "A+"
        score > 900 -> "A"
        score > 800 -> "B+"
        score > 700 -> "B"
        score > 600 -> "C+"
        score > 500 -> "C"
        else -> "D"
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(scoreColor.copy(alpha = 0.1f))
            .border(
                width = 2.dp,
                color = scoreColor,
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = scoreText,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = scoreColor
        )
    }
}

/**
 * Enhanced metadata row with smart progressive disclosure
 * Shows key information in compact format with visual indicators
 */
@Composable
fun EnhancedMetadataRow(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
    compact: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider type indicator with visual distinction
        ProviderTypeIndicator(
            providerType = sourceMetadata.provider.type,
            reliability = sourceMetadata.provider.reliability
        )
        
        // Codec efficiency indicator
        CodecEfficiencyIndicator(
            codec = sourceMetadata.codec,
            size = if (compact) QualityBadgeSize.SMALL else QualityBadgeSize.MEDIUM
        )
        
        // Audio format indicator
        AudioFormatIndicator(
            audio = sourceMetadata.audio,
            size = if (compact) QualityBadgeSize.SMALL else QualityBadgeSize.MEDIUM
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Health indicator for P2P with visual status
        sourceMetadata.health.seeders?.let { seeders ->
            if (seeders > 0) {
                EnhancedHealthStatusIndicator(
                    health = sourceMetadata.health,
                    compact = compact
                )
            }
        }
        
        // Age indicator for content freshness
        sourceMetadata.file.addedDate?.let { date ->
            AgeIndicator(
                addedDate = date,
                compact = compact
            )
        }
        
        // Cached/availability status
        AvailabilityStatusIndicator(
            availability = sourceMetadata.availability,
            compact = compact
        )
    }
}

/**
 * Provider type indicator with reliability visual cues
 */
@Composable
fun ProviderTypeIndicator(
    providerType: SourceProviderInfo.ProviderType,
    reliability: SourceProviderInfo.ProviderReliability,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, iconText, textColor) = when (providerType) {
        SourceProviderInfo.ProviderType.TORRENT -> Triple(
            Color(0xFF8B5CF6),
            "T",
            Color.White
        )
        SourceProviderInfo.ProviderType.DIRECT_STREAM -> Triple(
            Color(0xFF3B82F6),
            "S",
            Color.White
        )
        SourceProviderInfo.ProviderType.DEBRID -> Triple(
            Color(0xFF10B981),
            "D",
            Color.White
        )
        else -> Triple(
            Color(0xFF6B7280),
            "?",
            Color.White
        )
    }
    
    // Add reliability indicator as border
    val borderColor = when (reliability) {
        SourceProviderInfo.ProviderReliability.EXCELLENT -> Color(0xFF059669)
        SourceProviderInfo.ProviderReliability.GOOD -> Color(0xFF10B981)
        SourceProviderInfo.ProviderReliability.FAIR -> Color(0xFFF59E0B)
        SourceProviderInfo.ProviderReliability.POOR -> Color(0xFFEF4444)
        else -> Color.Transparent
    }
    
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}

/**
 * Codec efficiency indicator with visual performance hints
 */
@Composable
fun CodecEfficiencyIndicator(
    codec: CodecInfo,
    modifier: Modifier = Modifier,
    size: QualityBadgeSize = QualityBadgeSize.SMALL
) {
    val efficiencyColor = when (codec.type.efficiencyBonus) {
        in 45..Int.MAX_VALUE -> Color(0xFF059669) // Excellent (AV1, HEVC)
        in 35..44 -> Color(0xFF10B981) // Very Good (VP9)
        in 25..34 -> Color(0xFF84CC16) // Good (H.264)
        in 15..24 -> Color(0xFFF59E0B) // Fair (MPEG-4)
        else -> Color(0xFFEF4444) // Poor (older codecs)
    }
    
    val badgeText = codec.type.shortName
    val padding = when (size) {
        QualityBadgeSize.SMALL -> PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        QualityBadgeSize.MEDIUM -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        QualityBadgeSize.LARGE -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(efficiencyColor.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = efficiencyColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = when (size) {
                    QualityBadgeSize.SMALL -> 10.sp
                    QualityBadgeSize.MEDIUM -> 12.sp
                    QualityBadgeSize.LARGE -> 14.sp
                },
                fontWeight = FontWeight.Medium
            ),
            color = efficiencyColor
        )
    }
}

/**
 * Audio format indicator with channel and quality information
 */
@Composable
fun AudioFormatIndicator(
    audio: AudioInfo,
    modifier: Modifier = Modifier,
    size: QualityBadgeSize = QualityBadgeSize.SMALL
) {
    val (backgroundColor, textColor, displayText) = when {
        audio.dolbyAtmos -> Triple(
            Color(0xFF1F2937),
            Color.White,
            "Atmos"
        )
        audio.dtsX -> Triple(
            Color(0xFF374151),
            Color.White,
            "DTS:X"
        )
        audio.format.qualityBonus >= 40 -> Triple(
            Color(0xFFEA580C),
            Color.White,
            audio.format.shortName
        )
        audio.format.qualityBonus >= 30 -> Triple(
            Color(0xFFF59E0B),
            Color.White,
            audio.format.shortName
        )
        else -> Triple(
            Color(0xFFFBBF24),
            Color.Black,
            audio.format.shortName
        )
    }
    
    val channels = audio.channels?.let { " $it" } ?: ""
    val fullText = "$displayText$channels"
    
    val padding = when (size) {
        QualityBadgeSize.SMALL -> PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        QualityBadgeSize.MEDIUM -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        QualityBadgeSize.LARGE -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = fullText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = when (size) {
                    QualityBadgeSize.SMALL -> 10.sp
                    QualityBadgeSize.MEDIUM -> 12.sp
                    QualityBadgeSize.LARGE -> 14.sp
                },
                fontWeight = FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

/**
 * Enhanced health status with visual health indicators
 */
@Composable
fun EnhancedHealthStatusIndicator(
    health: HealthInfo,
    modifier: Modifier = Modifier,
    compact: Boolean = true
) {
    val seeders = health.seeders ?: 0
    val leechers = health.leechers ?: 0
    
    val healthStatus = health.getHealthStatus()
    val (backgroundColor, textColor) = when (healthStatus) {
        HealthInfo.HealthStatus.EXCELLENT -> Color(0xFF059669) to Color.White
        HealthInfo.HealthStatus.GOOD -> Color(0xFF10B981) to Color.White
        HealthInfo.HealthStatus.FAIR -> Color(0xFF84CC16) to Color.Black
        HealthInfo.HealthStatus.POOR -> Color(0xFFF59E0B) to Color.Black
        HealthInfo.HealthStatus.DEAD -> Color(0xFFEF4444) to Color.White
        else -> Color(0xFF6B7280) to Color.White
    }
    
    val displayText = if (compact) {
        when {
            seeders > 1000 -> "${seeders / 1000}kS"
            seeders > 0 -> "${seeders}S"
            else -> "0S"
        }
    } else {
        "${seeders}S/${leechers}L"
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}

/**
 * Age indicator showing content freshness
 */
@Composable
fun AgeIndicator(
    addedDate: Date,
    modifier: Modifier = Modifier,
    compact: Boolean = true
) {
    val now = Date()
    val ageInDays = ((now.time - addedDate.time) / (1000 * 60 * 60 * 24)).toInt()
    
    val (ageText, backgroundColor, textColor) = when {
        ageInDays < 1 -> Triple("NEW", Color(0xFF10B981), Color.White)
        ageInDays < 7 -> Triple("${ageInDays}d", Color(0xFF84CC16), Color.Black)
        ageInDays < 30 -> Triple("${ageInDays}d", Color(0xFFF59E0B), Color.Black)
        ageInDays < 365 -> {
            val months = ageInDays / 30
            Triple("${months}mo", Color(0xFF6B7280), Color.White)
        }
        else -> {
            val years = ageInDays / 365
            Triple("${years}y", Color(0xFF4B5563), Color.White)
        }
    }
    
    if (ageInDays <= 30 || !compact) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ageText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = textColor
            )
        }
    }
}

/**
 * Availability status indicator with visual caching/region info
 */
@Composable
fun AvailabilityStatusIndicator(
    availability: AvailabilityInfo,
    modifier: Modifier = Modifier,
    compact: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cached status
        if (availability.cached) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF059669))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CACHED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
        
        // Debrid service indicator
        availability.debridService?.let { service ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3B82F6))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = service.uppercase().take(2),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
        
        // Region restriction indicator
        availability.region?.let { region ->
            if (!compact) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF6B7280))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = region.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Detailed metadata section for expanded view with complete technical specs
 */
@Composable
fun DetailedMetadataSection(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
    showTechnicalSpecs: Boolean = true,
    showP2PHealth: Boolean = true,
    showAvailabilityInfo: Boolean = true,
    showReleaseDetails: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showTechnicalSpecs) {
            TechnicalSpecsSection(sourceMetadata = sourceMetadata)
        }
        
        if (showP2PHealth && sourceMetadata.health.seeders != null) {
            P2PHealthSection(health = sourceMetadata.health)
        }
        
        if (showReleaseDetails) {
            ReleaseDetailsSection(
                release = sourceMetadata.release,
                file = sourceMetadata.file
            )
        }
        
        if (showAvailabilityInfo) {
            AvailabilityDetailsSection(
                availability = sourceMetadata.availability,
                provider = sourceMetadata.provider
            )
        }
    }
}

/**
 * Technical specifications section with comprehensive codec/audio info
 */
@Composable
fun TechnicalSpecsSection(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Technical Specifications",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Video specifications
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Video",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sourceMetadata.quality.getDisplayText(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                sourceMetadata.quality.bitrate?.let { bitrate ->
                    Text(
                        text = "${bitrate / 1_000_000} Mbps",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Codec specifications
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Codec",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sourceMetadata.codec.getDisplayText(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Efficiency: ${getEfficiencyRating(sourceMetadata.codec.type)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Audio specifications
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Audio",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sourceMetadata.audio.getDisplayText(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                sourceMetadata.audio.bitrate?.let { bitrate ->
                    Text(
                        text = "$bitrate kbps",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * P2P health section with comprehensive download statistics
 */
@Composable
fun P2PHealthSection(
    health: HealthInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "P2P Health Statistics",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Seeders/Leechers
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Peers",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${health.seeders ?: 0} seeders, ${health.leechers ?: 0} leechers",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val ratio = if ((health.leechers ?: 0) > 0) {
                    (health.seeders ?: 0).toFloat() / (health.leechers ?: 1).toFloat()
                } else {
                    Float.MAX_VALUE
                }
                
                val ratioText = when {
                    ratio == Float.MAX_VALUE -> "∞"
                    ratio >= 10f -> String.format("%.1f", ratio)
                    else -> String.format("%.2f", ratio)
                }
                
                Text(
                    text = "Ratio: $ratioText",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = when {
                        ratio >= 2f -> Color(0xFF059669)
                        ratio >= 1f -> Color(0xFF84CC16)
                        ratio >= 0.5f -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                )
            }
            
            // Availability
            health.availability?.let { availability ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Availability",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(availability * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = when {
                            availability >= 0.9f -> Color(0xFF059669)
                            availability >= 0.7f -> Color(0xFF84CC16)
                            availability >= 0.5f -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }
                    )
                    Text(
                        text = "of file available",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Last checked
            health.lastChecked?.let { lastChecked ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Last Checked",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimeAgo(lastChecked),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Release details section with group reputation and edition info
 */
@Composable
fun ReleaseDetailsSection(
    release: ReleaseInfo,
    file: FileInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Release Information",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Release type and quality
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = release.type.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Quality Score: ${release.type.qualityBonus}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = when {
                        release.type.qualityBonus >= 90 -> Color(0xFF059669)
                        release.type.qualityBonus >= 70 -> Color(0xFF84CC16)
                        release.type.qualityBonus >= 50 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                )
            }
            
            // Release group with reputation
            release.group?.let { group ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Release Group",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = group,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Release group reputation indicator
                    val reputation = getReleaseGroupReputation(group)
                    Text(
                        text = reputation,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = when (reputation) {
                            "Trusted" -> Color(0xFF059669)
                            "Known" -> Color(0xFF84CC16)
                            "Unknown" -> Color(0xFFF59E0B)
                            else -> Color(0xFF6B7280)
                        }
                    )
                }
            }
            
            // File details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "File Details",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                file.extension?.let { ext ->
                    Text(
                        text = ext.uppercase(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                file.addedDate?.let { date ->
                    Text(
                        text = "Added ${formatTimeAgo(date)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Edition information if available
        release.edition?.let { edition ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edition: ",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = edition,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Availability details section with provider and caching info
 */
@Composable
fun AvailabilityDetailsSection(
    availability: AvailabilityInfo,
    provider: SourceProviderInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Availability & Access",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Provider information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Provider",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Reliability: ${provider.reliability.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = when (provider.reliability) {
                        SourceProviderInfo.ProviderReliability.EXCELLENT -> Color(0xFF059669)
                        SourceProviderInfo.ProviderReliability.GOOD -> Color(0xFF84CC16)
                        SourceProviderInfo.ProviderReliability.FAIR -> Color(0xFFF59E0B)
                        SourceProviderInfo.ProviderReliability.POOR -> Color(0xFFEF4444)
                        else -> Color(0xFF6B7280)
                    }
                )
            }
            
            // Access method
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Access Method",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when {
                        availability.cached -> "Instant (Cached)"
                        availability.debridService != null -> "Debrid Service"
                        provider.type == SourceProviderInfo.ProviderType.DIRECT_STREAM -> "Direct Stream"
                        else -> "P2P Download"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = when {
                        availability.cached -> Color(0xFF059669)
                        availability.debridService != null -> Color(0xFF3B82F6)
                        provider.type == SourceProviderInfo.ProviderType.DIRECT_STREAM -> Color(0xFF10B981)
                        else -> Color(0xFF8B5CF6)
                    }
                )
            }
            
            // Region/Expiry
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Availability",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                availability.region?.let { region ->
                    Text(
                        text = "Region: $region",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                availability.expiryDate?.let { expiry ->
                    Text(
                        text = "Expires: ${formatTimeAgo(expiry)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } ?: run {
                    Text(
                        text = "No expiry",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFF059669)
                    )
                }
            }
        }
    }
}

/**
 * Compact badge row for source list items with intelligent overflow
 */
@Composable
fun CompactBadgeRow(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
    maxBadges: Int = 5
) {
    val badges = sourceMetadata.getQualityBadges()
    val visibleBadges = badges.take(maxBadges)
    val overflowCount = badges.size - visibleBadges.size
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(visibleBadges) { badge ->
            AdvancedQualityBadgeComponent(
                badge = badge,
                size = QualityBadgeSize.SMALL
            )
        }
        
        if (overflowCount > 0) {
            item {
                OverflowBadge(
                    count = overflowCount,
                    size = QualityBadgeSize.SMALL
                )
            }
        }
    }
}

/**
 * Helper functions for metadata display
 */
private fun getEfficiencyRating(codec: VideoCodec): String {
    return when (codec.efficiencyBonus) {
        in 45..Int.MAX_VALUE -> "Excellent"
        in 35..44 -> "Very Good"
        in 25..34 -> "Good"
        in 15..24 -> "Fair"
        else -> "Poor"
    }
}

private fun getReleaseGroupReputation(group: String): String {
    // This would typically come from a database of known release groups
    // For demo purposes, using some common patterns
    val trustedGroups = setOf(
        "RARBG", "YTS", "ETRG", "FGT", "SPARKS", "AMZN", "NTb", "DEFLATE", "ROVERS",
        "ION10", "HEVC-PSA", "x265-RARBG", "BluRayDesuYo"
    )
    
    return when {
        trustedGroups.any { group.contains(it, ignoreCase = true) } -> "Trusted"
        group.length >= 3 -> "Known"
        else -> "Unknown"
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

/**
 * Source badge container for comprehensive badge display
 */
@Composable
fun SourceBadgeContainer(
    sourceMetadata: SourceMetadata,
    modifier: Modifier = Modifier,
    badgeSize: QualityBadgeSize = QualityBadgeSize.MEDIUM,
    showProvider: Boolean = true,
    maxBadges: Int = 8
) {
    val badges = sourceMetadata.getQualityBadges()
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(badges.take(maxBadges)) { badge ->
            AdvancedQualityBadgeComponent(
                badge = badge,
                size = badgeSize
            )
        }
        
        if (badges.size > maxBadges) {
            item {
                OverflowBadge(
                    count = badges.size - maxBadges,
                    size = badgeSize
                )
            }
        }
    }
}