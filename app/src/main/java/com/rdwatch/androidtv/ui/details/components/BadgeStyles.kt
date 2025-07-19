package com.rdwatch.androidtv.ui.details.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Badge style configuration for TV-optimized display
 * Ensures consistent styling across all badge components
 */
object BadgeStyles {
    /**
     * Badge color palette optimized for TV viewing
     * High contrast colors for visibility from distance
     */
    object Colors {
        // Resolution colors
        val resolution8K = Color(0xFFDC2626) // Bright red
        val resolution4K = Color(0xFF8B5CF6) // Purple
        val resolution2K = Color(0xFF7C3AED) // Violet
        val resolution1080p = Color(0xFF2563EB) // Blue
        val resolution720p = Color(0xFF059669) // Green
        val resolutionSD = Color(0xFF6B7280) // Gray

        // HDR colors
        val hdrDolbyVision = Color(0xFF000000) // Black with white text
        val hdrHDR10Plus = Color(0xFF1E40AF) // Dark blue
        val hdrHDR10 = Color(0xFF4A90E2) // Blue

        // Codec colors
        val codecAV1 = Color(0xFF059669) // Emerald
        val codecHEVC = Color(0xFF10B981) // Green
        val codecH264 = Color(0xFF34D399) // Light green
        val codecOther = Color(0xFF6EE7B7) // Pale green

        // Audio colors
        val audioAtmos = Color(0xFF1F2937) // Near black
        val audioDTSX = Color(0xFF374151) // Dark gray
        val audioTrueHD = Color(0xFFEA580C) // Dark orange
        val audioDTSHD = Color(0xFFF59E0B) // Orange
        val audioStandard = Color(0xFFFBBF24) // Yellow

        // Release colors
        val releaseREMUX = Color(0xFF7C3AED) // Violet
        val releaseBluRay = Color(0xFF2563EB) // Blue
        val releaseWEBDL = Color(0xFF0891B2) // Cyan
        val releaseWebRip = Color(0xFF0EA5E9) // Light blue
        val releaseOther = Color(0xFF6B7280) // Gray

        // Health colors
        val healthExcellent = Color(0xFF059669) // Green
        val healthGood = Color(0xFF10B981) // Light green
        val healthFair = Color(0xFF84CC16) // Lime
        val healthPoor = Color(0xFFF59E0B) // Orange
        val healthBad = Color(0xFFEF4444) // Red

        // Feature colors
        val featureCached = Color(0xFF059669) // Green
        val featurePack = Color(0xFF8B5CF6) // Purple
        val featureDefault = Color(0xFF06B6D4) // Cyan

        // Provider colors
        val providerExcellent = Color(0xFF059669) // Green
        val providerGood = Color(0xFF10B981) // Light green
        val providerFair = Color(0xFFF59E0B) // Orange
        val providerPoor = Color(0xFFEF4444) // Red
        val providerUnknown = Color(0xFF6B7280) // Gray
    }

    /**
     * Badge sizing configuration
     */
    object Sizing {
        data class BadgeSizeConfig(
            val cornerRadius: Dp,
            val horizontalPadding: Dp,
            val verticalPadding: Dp,
            val fontSize: Int,
            val fontWeight: FontWeight,
            val minWidth: Dp,
            val height: Dp,
        )

        val small =
            BadgeSizeConfig(
                cornerRadius = 6.dp,
                horizontalPadding = 8.dp,
                verticalPadding = 4.dp,
                fontSize = 12,
                fontWeight = FontWeight.Bold,
                minWidth = 40.dp,
                height = 24.dp,
            )

        val medium =
            BadgeSizeConfig(
                cornerRadius = 8.dp,
                horizontalPadding = 12.dp,
                verticalPadding = 6.dp,
                fontSize = 14,
                fontWeight = FontWeight.Bold,
                minWidth = 48.dp,
                height = 32.dp,
            )

        val large =
            BadgeSizeConfig(
                cornerRadius = 10.dp,
                horizontalPadding = 16.dp,
                verticalPadding = 8.dp,
                fontSize = 16,
                fontWeight = FontWeight.Bold,
                minWidth = 56.dp,
                height = 40.dp,
            )
    }

    /**
     * Badge animation durations
     */
    object Animations {
        const val focusAnimationDuration = 200
        const val colorTransitionDuration = 150
        const val scaleAnimationDuration = 150
    }

    /**
     * Badge spacing configuration
     */
    object Spacing {
        val betweenBadgesSmall = 4.dp
        val betweenBadgesMedium = 6.dp
        val betweenBadgesLarge = 8.dp
        val betweenRows = 12.dp
    }

    /**
     * Focus state configuration
     */
    object Focus {
        val borderWidth = 2.dp
        val borderColor = Color(0xFF3B82F6) // Blue focus ring
        val scaleOnFocus = 1.05f
        val elevationOnFocus = 8.dp
    }

    /**
     * Get color for specific badge content
     */
    fun getColorForBadge(
        type: String,
        content: String,
    ): Color {
        return when (type.uppercase()) {
            "RESOLUTION" ->
                when (content) {
                    "8K" -> Colors.resolution8K
                    "4K" -> Colors.resolution4K
                    "1440p", "2K" -> Colors.resolution2K
                    "1080p" -> Colors.resolution1080p
                    "720p" -> Colors.resolution720p
                    else -> Colors.resolutionSD
                }
            "HDR" ->
                when (content) {
                    "DV", "Dolby Vision" -> Colors.hdrDolbyVision
                    "HDR10+" -> Colors.hdrHDR10Plus
                    "HDR10" -> Colors.hdrHDR10
                    else -> Colors.hdrHDR10
                }
            "CODEC" ->
                when {
                    content.contains("AV1") -> Colors.codecAV1
                    content.contains("H.265") || content.contains("HEVC") -> Colors.codecHEVC
                    content.contains("H.264") || content.contains("AVC") -> Colors.codecH264
                    else -> Colors.codecOther
                }
            "AUDIO" ->
                when {
                    content.contains("Atmos") -> Colors.audioAtmos
                    content.contains("DTS:X") -> Colors.audioDTSX
                    content.contains("TrueHD") -> Colors.audioTrueHD
                    content.contains("DTS-HD") -> Colors.audioDTSHD
                    else -> Colors.audioStandard
                }
            "RELEASE" ->
                when (content) {
                    "REMUX" -> Colors.releaseREMUX
                    "BluRay" -> Colors.releaseBluRay
                    "WEB-DL" -> Colors.releaseWEBDL
                    "WebRip" -> Colors.releaseWebRip
                    else -> Colors.releaseOther
                }
            else -> Colors.featureDefault
        }
    }

    /**
     * Badge presets for common use cases
     */
    object Presets {
        val premium4K =
            listOf(
                "4K" to Colors.resolution4K,
                "DV" to Colors.hdrDolbyVision,
                "Atmos" to Colors.audioAtmos,
            )

        val standard1080p =
            listOf(
                "1080p" to Colors.resolution1080p,
                "H.264" to Colors.codecH264,
                "5.1" to Colors.audioStandard,
            )

        val cachedDebrid =
            listOf(
                "CACHED" to Colors.featureCached,
            )
    }
}
