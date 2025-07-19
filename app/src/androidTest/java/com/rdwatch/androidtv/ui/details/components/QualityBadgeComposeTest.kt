package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.ui.details.models.SourceQuality
import com.rdwatch.androidtv.ui.theme.RDWatchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.rdwatch.androidtv.ui.details.models.advanced.QualityBadge as AdvancedQualityBadge

/**
 * UI tests for QualityBadge component
 * Tests rendering, focus behavior, and interaction
 */
@RunWith(AndroidJUnit4::class)
class QualityBadgeComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun qualityBadge_displaysCorrectText() {
        val quality = SourceQuality.UHD_4K

        composeTestRule.setContent {
            RDWatchTheme {
                QualityBadge(quality = quality)
            }
        }

        composeTestRule
            .onNodeWithText("4K")
            .assertIsDisplayed()
    }

    @Test
    fun qualityBadge_showsSelectedState() {
        val quality = SourceQuality.FULL_HD_1080P

        composeTestRule.setContent {
            RDWatchTheme {
                QualityBadge(
                    quality = quality,
                    isSelected = true,
                )
            }
        }

        composeTestRule
            .onNodeWithText("1080p")
            .assertIsDisplayed()
    }

    @Test
    fun qualityBadge_respondsToClick() {
        val quality = SourceQuality.HD_720P
        var clicked = false

        composeTestRule.setContent {
            RDWatchTheme {
                QualityBadge(
                    quality = quality,
                    onClick = { clicked = true },
                )
            }
        }

        composeTestRule
            .onNodeWithText("720p")
            .performClick()

        assert(clicked)
    }

    @Test
    fun qualityBadge_hasFocusableState() {
        val quality = SourceQuality.FULL_HD_1080P

        composeTestRule.setContent {
            RDWatchTheme {
                QualityBadge(
                    quality = quality,
                    focusable = true,
                )
            }
        }

        val badge = composeTestRule.onNodeWithText("1080p")
        badge.assertIsDisplayed()

        // Test that it can receive focus (this is TV-specific behavior)
        badge.requestFocus()
        badge.assertIsFocused()
    }

    @Test
    fun qualityBadge_differentSizes() {
        composeTestRule.setContent {
            RDWatchTheme {
                Column {
                    QualityBadge(
                        quality = SourceQuality.UHD_4K,
                        size = QualityBadgeSize.SMALL,
                        modifier = Modifier.testTag("small"),
                    )
                    QualityBadge(
                        quality = SourceQuality.UHD_4K,
                        size = QualityBadgeSize.MEDIUM,
                        modifier = Modifier.testTag("medium"),
                    )
                    QualityBadge(
                        quality = SourceQuality.UHD_4K,
                        size = QualityBadgeSize.LARGE,
                        modifier = Modifier.testTag("large"),
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("small").assertIsDisplayed()
        composeTestRule.onNodeWithTag("medium").assertIsDisplayed()
        composeTestRule.onNodeWithTag("large").assertIsDisplayed()
    }

    @Test
    fun qualityBadge_handlesLongText() {
        val quality = SourceQuality.UHD_4K // This might have longer text like "4K Ultra HD"

        composeTestRule.setContent {
            RDWatchTheme {
                QualityBadge(
                    quality = quality,
                    modifier = Modifier.width(100.dp),
                )
            }
        }

        // Should still be displayed even with constrained width
        composeTestRule
            .onNode(hasText("4K", substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun advancedQualityBadge_displaysCorrectly() {
        composeTestRule.setContent {
            RDWatchTheme {
                advancedQualityBadgeComposable(
                    badge =
                        AdvancedQualityBadge(
                            text = "HDR10",
                            type = AdvancedQualityBadge.Type.HDR,
                            priority = 95,
                        ),
                )
            }
        }

        composeTestRule
            .onNodeWithText("HDR10")
            .assertIsDisplayed()
    }

    @Test
    fun qualityBadgeRow_displaysMultipleBadges() {
        val badges =
            listOf(
                AdvancedQualityBadge("4K", AdvancedQualityBadge.Type.RESOLUTION, 100),
                AdvancedQualityBadge("HDR10", AdvancedQualityBadge.Type.HDR, 95),
                AdvancedQualityBadge("HEVC", AdvancedQualityBadge.Type.CODEC, 80),
                AdvancedQualityBadge("Atmos", AdvancedQualityBadge.Type.AUDIO, 70),
            )

        composeTestRule.setContent {
            RDWatchTheme {
                qualityBadgeRow(badges = badges)
            }
        }

        composeTestRule.onNodeWithText("4K").assertIsDisplayed()
        composeTestRule.onNodeWithText("HDR10").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEVC").assertIsDisplayed()
        composeTestRule.onNodeWithText("Atmos").assertIsDisplayed()
    }

    @Test
    fun qualityBadgeRow_scrollsWhenOverflow() {
        val manyBadges =
            (1..10).map { index ->
                AdvancedQualityBadge("Badge$index", AdvancedQualityBadge.Type.FEATURE, 50)
            }

        composeTestRule.setContent {
            RDWatchTheme {
                Box(modifier = Modifier.width(200.dp)) { // Constrained width to force scrolling
                    qualityBadgeRow(badges = manyBadges)
                }
            }
        }

        // First badge should be visible
        composeTestRule.onNodeWithText("Badge1").assertIsDisplayed()

        // Scroll to see later badges
        composeTestRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("Badge10"))

        composeTestRule.onNodeWithText("Badge10").assertIsDisplayed()
    }

    @Test
    fun qualityBadge_accessibilitySemantics() {
        val quality = SourceQuality.UHD_4K

        composeTestRule.setContent {
            RDWatchTheme {
                QualityBadge(
                    quality = quality,
                    focusable = true,
                    onClick = { },
                )
            }
        }

        composeTestRule
            .onNodeWithText("4K")
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun qualityBadge_handlesDisabledState() {
        val quality = SourceQuality.HD_720P

        composeTestRule.setContent {
            RDWatchTheme {
                QualityBadge(
                    quality = quality,
                    enabled = false,
                )
            }
        }

        composeTestRule
            .onNodeWithText("720p")
            .assertIsDisplayed()
        // Disabled badges should not be clickable
    }

    @Test
    fun qualityBadge_theming() {
        composeTestRule.setContent {
            RDWatchTheme {
                Column {
                    QualityBadge(
                        quality = SourceQuality.UHD_4K,
                        variant = QualityBadgeVariant.DEFAULT,
                        modifier = Modifier.testTag("default"),
                    )
                    QualityBadge(
                        quality = SourceQuality.UHD_4K,
                        variant = QualityBadgeVariant.OUTLINED,
                        modifier = Modifier.testTag("outlined"),
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("default").assertIsDisplayed()
        composeTestRule.onNodeWithTag("outlined").assertIsDisplayed()
    }
}

// Helper composable for testing advanced quality badges
@Composable
private fun advancedQualityBadgeComposable(
    badge: AdvancedQualityBadge,
    modifier: Modifier = Modifier,
) {
    Text(
        text = badge.text,
        modifier =
            modifier
                .background(
                    color =
                        when (badge.type) {
                            AdvancedQualityBadge.Type.RESOLUTION -> MaterialTheme.colorScheme.primary
                            AdvancedQualityBadge.Type.HDR -> MaterialTheme.colorScheme.secondary
                            AdvancedQualityBadge.Type.CODEC -> MaterialTheme.colorScheme.tertiary
                            AdvancedQualityBadge.Type.AUDIO -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun qualityBadgeRow(
    badges: List<AdvancedQualityBadge>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(badges) { badge ->
            advancedQualityBadgeComposable(badge = badge)
        }
    }
}
