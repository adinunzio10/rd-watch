package com.rdwatch.androidtv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.theme.RdwatchTheme
import com.rdwatch.androidtv.ui.theme.UIConstants
import kotlin.math.min

/**
 * Episode progress bar showing watched/total episodes per season
 * Displays individual episode markers with progress indication
 */
@Composable
fun EpisodeProgressBar(
    watchedEpisodes: Int,
    totalEpisodes: Int,
    modifier: Modifier = Modifier,
    size: EpisodeProgressSize = EpisodeProgressSize.Standard,
    showText: Boolean = true,
    animationEnabled: Boolean = true,
    // Limit for TV readability
    maxDisplayedEpisodes: Int = 20,
) {
    val displayEpisodes = min(totalEpisodes, maxDisplayedEpisodes)
    val progress = if (totalEpisodes > 0) watchedEpisodes.toFloat() / totalEpisodes else 0f

    // Animate progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec =
            tween(
                durationMillis = if (animationEnabled) UIConstants.Animations.STANDARD_DURATION_MS else 0,
            ),
        label = "episode_progress",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(size.spacing),
    ) {
        // Episode markers row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(size.markerSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Individual episode markers
            repeat(displayEpisodes) { index ->
                val isWatched = index < (animatedProgress * totalEpisodes).toInt()
                EpisodeMarker(
                    isWatched = isWatched,
                    size = size.markerSize,
                    animationEnabled = animationEnabled,
                )
            }

            // Overflow indicator if more episodes exist
            if (totalEpisodes > maxDisplayedEpisodes) {
                Text(
                    text = "+${totalEpisodes - maxDisplayedEpisodes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        // Progress text
        if (showText && size != EpisodeProgressSize.Mini) {
            Text(
                text = "$watchedEpisodes/$totalEpisodes episodes",
                style =
                    when (size) {
                        EpisodeProgressSize.Large -> MaterialTheme.typography.bodyMedium
                        EpisodeProgressSize.Standard -> MaterialTheme.typography.bodySmall
                        EpisodeProgressSize.Compact, EpisodeProgressSize.Mini -> MaterialTheme.typography.labelSmall
                    },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * Individual episode marker
 */
@Composable
private fun EpisodeMarker(
    isWatched: Boolean,
    size: Dp,
    animationEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue =
            if (isWatched) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            },
        animationSpec =
            tween(
                durationMillis = if (animationEnabled) UIConstants.Animations.FAST_DURATION_MS else 0,
            ),
        label = "marker_color",
    )

    Box(
        modifier =
            modifier
                .size(size)
                .background(
                    color = color,
                    shape = CircleShape,
                ),
    )
}

/**
 * Season progress indicator for multi-season shows
 * Shows progress across multiple seasons with segment visualization
 */
@Composable
fun SeasonProgressIndicator(
    seasonProgresses: List<SeasonProgress>,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    showDetails: Boolean = false,
    animationEnabled: Boolean = true,
) {
    if (seasonProgresses.isEmpty()) return

    val totalSeasons = seasonProgresses.size
    val overallProgress = seasonProgresses.sumOf { it.progress.toDouble() }.toFloat() / totalSeasons

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Multi-segment progress bar
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        ) {
            // Draw season segments
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                val segmentWidth = size.width / totalSeasons
                seasonProgresses.forEachIndexed { index, seasonProgress ->
                    drawSeasonSegment(
                        seasonProgress = seasonProgress,
                        segmentIndex = index,
                        segmentWidth = segmentWidth,
                        totalHeight = size.height,
                        animationEnabled = animationEnabled,
                    )
                }
            }
        }

        // Season details
        if (showDetails) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Season ${seasonProgresses.firstOrNull()?.seasonNumber ?: 1}-${seasonProgresses.lastOrNull()?.seasonNumber ?: totalSeasons}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Text(
                    text = "${(overallProgress * 100).toInt()}% complete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * Mini progress badge for show cards
 * Small circular or rectangular indicator showing progress state
 */
@Composable
fun MiniProgressBadge(
    progress: Float,
    totalEpisodes: Int = 0,
    watchedEpisodes: Int = 0,
    modifier: Modifier = Modifier,
    style: ProgressBadgeStyle = ProgressBadgeStyle.Circular,
    showPercentage: Boolean = false,
) {
    val backgroundColor by animateColorAsState(
        targetValue =
            when {
                progress >= 1f -> MaterialTheme.colorScheme.primary
                progress > 0f -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            },
        animationSpec = tween(UIConstants.Animations.FAST_DURATION_MS),
        label = "badge_background",
    )

    val contentColor by animateColorAsState(
        targetValue =
            when {
                progress >= 1f -> MaterialTheme.colorScheme.onPrimary
                progress > 0f -> MaterialTheme.colorScheme.onSecondary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
        animationSpec = tween(UIConstants.Animations.FAST_DURATION_MS),
        label = "badge_content",
    )

    val shape =
        when (style) {
            ProgressBadgeStyle.Circular -> CircleShape
            ProgressBadgeStyle.Rounded -> RoundedCornerShape(4.dp)
            ProgressBadgeStyle.Square -> RoundedCornerShape(2.dp)
        }

    Box(
        modifier =
            modifier
                .size(
                    width = if (style == ProgressBadgeStyle.Circular) 24.dp else 32.dp,
                    height = 24.dp,
                )
                .background(backgroundColor, shape)
                .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        val displayText =
            when {
                showPercentage -> "${(progress * 100).toInt()}%"
                totalEpisodes > 0 -> "$watchedEpisodes/$totalEpisodes"
                progress >= 1f -> "✓"
                progress > 0f -> "●"
                else -> "○"
            }

        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * Draw season segment on canvas
 */
private fun DrawScope.drawSeasonSegment(
    seasonProgress: SeasonProgress,
    segmentIndex: Int,
    segmentWidth: Float,
    totalHeight: Float,
    animationEnabled: Boolean,
) {
    val startX = segmentIndex * segmentWidth
    val progressWidth = segmentWidth * seasonProgress.progress

    // Season segment color based on progress
    val segmentColor =
        when {
            seasonProgress.progress >= 1f -> Color(0xFF4CAF50) // Green for completed
            seasonProgress.progress > 0f -> Color(0xFF2196F3) // Blue for in progress
            else -> Color(0xFFBDBDBD) // Gray for not started
        }

    // Draw progress fill
    if (progressWidth > 0) {
        drawLine(
            color = segmentColor,
            start = Offset(startX, totalHeight / 2),
            end = Offset(startX + progressWidth, totalHeight / 2),
            strokeWidth = totalHeight,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Data classes and enums
 */
data class SeasonProgress(
    val seasonNumber: Int,
    val watchedEpisodes: Int,
    val totalEpisodes: Int,
    val progress: Float = if (totalEpisodes > 0) watchedEpisodes.toFloat() / totalEpisodes else 0f,
)

enum class EpisodeProgressSize(
    val markerSize: Dp,
    val markerSpacing: Dp,
    val spacing: Dp,
) {
    Mini(4.dp, 2.dp, 4.dp),
    Compact(6.dp, 3.dp, 6.dp),
    Standard(8.dp, 4.dp, 8.dp),
    Large(10.dp, 5.dp, 10.dp),
}

enum class ProgressBadgeStyle {
    Circular,
    Rounded,
    Square,
}

// Preview composables
@Preview(showBackground = true)
@Composable
fun EpisodeProgressBarPreview() {
    RdwatchTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EpisodeProgressBar(
                watchedEpisodes = 5,
                totalEpisodes = 12,
                size = EpisodeProgressSize.Standard,
            )

            EpisodeProgressBar(
                watchedEpisodes = 12,
                totalEpisodes = 12,
                size = EpisodeProgressSize.Compact,
            )

            EpisodeProgressBar(
                watchedEpisodes = 0,
                totalEpisodes = 8,
                size = EpisodeProgressSize.Large,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SeasonProgressIndicatorPreview() {
    RdwatchTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SeasonProgressIndicator(
                seasonProgresses =
                    listOf(
                        SeasonProgress(1, 10, 10),
                        SeasonProgress(2, 8, 12),
                        SeasonProgress(3, 0, 10),
                    ),
                showDetails = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MiniProgressBadgePreview() {
    RdwatchTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MiniProgressBadge(progress = 0f)
            MiniProgressBadge(progress = 0.5f)
            MiniProgressBadge(progress = 1f)
            MiniProgressBadge(
                progress = 0.75f,
                style = ProgressBadgeStyle.Rounded,
                showPercentage = true,
            )
        }
    }
}
