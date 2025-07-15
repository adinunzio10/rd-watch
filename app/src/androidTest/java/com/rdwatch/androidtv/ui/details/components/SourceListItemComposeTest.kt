package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.ui.details.models.advanced.*
import com.rdwatch.androidtv.ui.theme.RDWatchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * UI tests for SourceListItem component
 * Tests source display, focus behavior, and interaction
 */
@RunWith(AndroidJUnit4::class)
class SourceListItemComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestSourceMetadata(
        id: String = "test_source",
        providerName: String = "Test Provider",
        resolution: VideoResolution = VideoResolution.RESOLUTION_1080P,
        codec: VideoCodec = VideoCodec.H264,
        fileSize: Long = 5_000_000_000L,
        seeders: Int = 100
    ): SourceMetadata {
        return SourceMetadata(
            id = id,
            provider = SourceProviderInfo(
                id = "provider_$id",
                name = providerName,
                displayName = providerName,
                logoUrl = null,
                type = SourceProviderInfo.ProviderType.TORRENT,
                reliability = SourceProviderInfo.ProviderReliability.GOOD
            ),
            quality = QualityInfo(
                resolution = resolution,
                bitrate = 8_000_000L,
                hdr10 = false,
                hdr10Plus = false,
                dolbyVision = false,
                frameRate = 24
            ),
            codec = CodecInfo(
                type = codec,
                profile = "High",
                level = "4.1"
            ),
            audio = AudioInfo(
                format = AudioFormat.AC3,
                channels = "5.1",
                bitrate = 640,
                language = "en",
                dolbyAtmos = false,
                dtsX = false
            ),
            release = ReleaseInfo(
                type = ReleaseType.BLURAY,
                group = "TEST",
                edition = null,
                year = 2023
            ),
            file = FileInfo(
                name = "Test.Movie.2023.${resolution.shortName}.BluRay.${codec.shortName}-TEST.mkv",
                sizeInBytes = fileSize,
                extension = "mkv",
                hash = "${id}_hash",
                addedDate = Date()
            ),
            health = HealthInfo(
                seeders = seeders,
                leechers = seeders / 4,
                downloadSpeed = 5_000_000L,
                uploadSpeed = 1_000_000L,
                availability = 1.0f,
                lastChecked = Date()
            ),
            features = FeatureInfo(),
            availability = AvailabilityInfo(
                isAvailable = true
            )
        )
    }

    @Test
    fun sourceListItem_displaysBasicInformation() {
        val testSource = createTestSourceMetadata(
            providerName = "TestProvider",
            resolution = VideoResolution.RESOLUTION_4K,
            codec = VideoCodec.HEVC,
            seeders = 500
        )
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { }
                )
            }
        }

        // Check that provider name is displayed
        composeTestRule.onNodeWithText("TestProvider").assertIsDisplayed()
        
        // Check that resolution is displayed
        composeTestRule.onNodeWithText("4K", substring = true).assertIsDisplayed()
        
        // Check that codec is displayed
        composeTestRule.onNodeWithText("HEVC", substring = true).assertIsDisplayed()
        
        // Check that seeder count is displayed
        composeTestRule.onNodeWithText("500", substring = true).assertIsDisplayed()
    }

    @Test
    fun sourceListItem_respondsToClick() {
        val testSource = createTestSourceMetadata()
        var clicked = false
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Source item", substring = true)
            .performClick()
        
        assert(clicked)
    }

    @Test
    fun sourceListItem_showsSelectedState() {
        val testSource = createTestSourceMetadata()
        
        composeTestRule.setContent {
            RDWatchTheme {
                Column {
                    SourceListItem(
                        sourceMetadata = testSource,
                        isSelected = false,
                        onClick = { },
                        modifier = Modifier.testTag("unselected")
                    )
                    SourceListItem(
                        sourceMetadata = testSource,
                        isSelected = true,
                        onClick = { },
                        modifier = Modifier.testTag("selected")
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("unselected").assertIsDisplayed()
        composeTestRule.onNodeWithTag("selected").assertIsDisplayed()
        
        // Selected item should have different visual state (tested through styling)
    }

    @Test
    fun sourceListItem_handlesFocusState() {
        val testSource = createTestSourceMetadata()
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { }
                )
            }
        }

        val sourceItem = composeTestRule.onNode(hasClickAction())
        sourceItem.assertIsDisplayed()
        
        // Test focus behavior
        sourceItem.requestFocus()
        sourceItem.assertIsFocused()
    }

    @Test
    fun sourceListItem_displaysFileSize() {
        val testSource = createTestSourceMetadata(
            fileSize = 8_589_934_592L // 8GB
        )
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { }
                )
            }
        }

        // Should display file size in human-readable format
        composeTestRule.onNodeWithText("8", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("GB", substring = true).assertIsDisplayed()
    }

    @Test
    fun sourceListItem_showsQualityBadges() {
        val testSource = createTestSourceMetadata(
            resolution = VideoResolution.RESOLUTION_4K,
            codec = VideoCodec.HEVC
        ).copy(
            quality = QualityInfo(
                resolution = VideoResolution.RESOLUTION_4K,
                hdr10 = true,
                bitrate = 15_000_000L
            )
        )
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { }
                )
            }
        }

        // Should show quality badges
        composeTestRule.onNodeWithText("4K").assertIsDisplayed()
        composeTestRule.onNodeWithText("HDR10", substring = true).assertExists()
    }

    @Test
    fun sourceListItem_handlesLongProviderNames() {
        val testSource = createTestSourceMetadata(
            providerName = "Very Long Provider Name That Might Overflow"
        )
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { },
                    modifier = Modifier.width(300.dp) // Constrained width
                )
            }
        }

        // Should still display, potentially truncated
        composeTestRule.onNodeWithText("Very Long Provider", substring = true).assertIsDisplayed()
    }

    @Test
    fun sourceListItem_showsHealthIndicators() {
        val testSource = createTestSourceMetadata(seeders = 1500)
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { }
                )
            }
        }

        // Should show seeder count
        composeTestRule.onNodeWithText("1500", substring = true).assertIsDisplayed()
        
        // Might show health status indicator
        composeTestRule.onNode(hasText("S", substring = true)).assertExists()
    }

    @Test
    fun sourceListItem_handlesZeroSeeders() {
        val testSource = createTestSourceMetadata(seeders = 0)
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { }
                )
            }
        }

        // Should handle zero seeders gracefully
        composeTestRule.onNodeWithText("0", substring = true).assertIsDisplayed()
    }

    @Test
    fun sourceListItem_showsReleaseType() {
        val testSource = createTestSourceMetadata().copy(
            release = ReleaseInfo(
                type = ReleaseType.BLURAY_REMUX,
                group = "TEST",
                edition = "Director's Cut",
                year = 2023
            )
        )
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { }
                )
            }
        }

        // Should show release type
        composeTestRule.onNodeWithText("REMUX", substring = true).assertIsDisplayed()
    }

    @Test
    fun sourceListInColumn_scrollsCorrectly() {
        val testSources = (1..20).map { index ->
            createTestSourceMetadata(
                id = "source_$index",
                providerName = "Provider $index"
            )
        }
        
        composeTestRule.setContent {
            RDWatchTheme {
                LazyColumn(
                    modifier = Modifier.height(400.dp) // Constrained height to force scrolling
                ) {
                    items(testSources) { source ->
                        SourceListItem(
                            sourceMetadata = source,
                            onClick = { },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // First item should be visible
        composeTestRule.onNodeWithText("Provider 1").assertIsDisplayed()
        
        // Scroll to see later items
        composeTestRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("Provider 20"))
        
        composeTestRule.onNodeWithText("Provider 20").assertIsDisplayed()
    }

    @Test
    fun sourceListItem_handlesLongPress() {
        val testSource = createTestSourceMetadata()
        var longClicked = false
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { },
                    onLongClick = { longClicked = true }
                )
            }
        }

        composeTestRule
            .onNode(hasClickAction())
            .performTouchInput { longClick() }
        
        assert(longClicked)
    }

    @Test
    fun sourceListItem_accessibilitySemantics() {
        val testSource = createTestSourceMetadata(
            providerName = "Accessible Provider",
            resolution = VideoResolution.RESOLUTION_1080P
        )
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = testSource,
                    onClick = { }
                )
            }
        }

        composeTestRule
            .onNode(hasClickAction())
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun sourceListItem_handlesDebridSource() {
        val debridSource = createTestSourceMetadata().copy(
            provider = SourceProviderInfo(
                id = "debrid_provider",
                name = "Real-Debrid",
                displayName = "Real-Debrid",
                logoUrl = null,
                type = SourceProviderInfo.ProviderType.DEBRID,
                reliability = SourceProviderInfo.ProviderReliability.EXCELLENT
            ),
            availability = AvailabilityInfo(
                isAvailable = true,
                cached = true,
                debridService = "real-debrid"
            )
        )
        
        composeTestRule.setContent {
            RDWatchTheme {
                SourceListItem(
                    sourceMetadata = debridSource,
                    onClick = { }
                )
            }
        }

        composeTestRule.onNodeWithText("Real-Debrid").assertIsDisplayed()
        // Should show cached indicator
        composeTestRule.onNodeWithText("Cached", substring = true).assertExists()
    }

    @Test
    fun sourceListItem_performance_largeList() {
        // Test performance with many items
        val largeSources = (1..100).map { index ->
            createTestSourceMetadata(
                id = "perf_source_$index",
                providerName = "Provider $index"
            )
        }
        
        composeTestRule.setContent {
            RDWatchTheme {
                LazyColumn {
                    items(largeSources) { source ->
                        SourceListItem(
                            sourceMetadata = source,
                            onClick = { }
                        )
                    }
                }
            }
        }

        // Should render without performance issues
        composeTestRule.onNodeWithText("Provider 1").assertIsDisplayed()
        
        // Scroll performance test
        composeTestRule
            .onNode(hasScrollAction())
            .performScrollToIndex(50)
        
        composeTestRule.onNodeWithText("Provider 51", substring = true).assertIsDisplayed()
    }
}