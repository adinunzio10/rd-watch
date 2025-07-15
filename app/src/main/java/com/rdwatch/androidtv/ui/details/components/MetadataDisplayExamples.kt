package com.rdwatch.androidtv.ui.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdwatch.androidtv.ui.details.models.advanced.*
import java.util.Date

/**
 * Example implementations and preview components for the enhanced metadata display system
 * Demonstrates different display modes and usage patterns for TV-optimized UI
 */
object MetadataDisplayExamples {
    
    /**
     * Creates sample source metadata for testing and preview
     */
    fun createSampleSourceMetadata(): List<SourceMetadata> {
        return listOf(
            createHighQualitySource(),
            createMediumQualitySource(), 
            createP2PSource(),
            createCachedSource(),
            createStreamingSource()
        )
    }
    
    private fun createHighQualitySource(): SourceMetadata {
        return SourceMetadata(
            id = "source_1",
            provider = SourceProviderInfo(
                id = "provider_1",
                name = "UltraStreaming",
                displayName = "UltraStreaming",
                logoUrl = null,
                type = SourceProviderInfo.ProviderType.DIRECT_STREAM,
                reliability = SourceProviderInfo.ProviderReliability.EXCELLENT
            ),
            quality = QualityInfo(
                resolution = VideoResolution.RESOLUTION_4K,
                bitrate = 45_000_000L, // 45 Mbps
                hdr10 = false,
                hdr10Plus = false,
                dolbyVision = true,
                frameRate = 60
            ),
            codec = CodecInfo(
                type = VideoCodec.HEVC,
                profile = "Main 10",
                level = "5.1"
            ),
            audio = AudioInfo(
                format = AudioFormat.TRUEHD,
                channels = "7.1",
                bitrate = 768,
                language = "en",
                dolbyAtmos = true,
                dtsX = false
            ),
            release = ReleaseInfo(
                type = ReleaseType.BLURAY_REMUX,
                group = "FraMeSToR",
                edition = "Director's Cut",
                year = 2023
            ),
            file = FileInfo(
                name = "Movie.2023.Directors.Cut.2160p.UHD.BluRay.REMUX.HDR.DV.TrueHD.Atmos.7.1-FraMeSToR.mkv",
                sizeInBytes = 78_643_200_000L, // ~78 GB
                extension = "mkv",
                hash = "abc123def456789",
                addedDate = Date(System.currentTimeMillis() - 86400000) // 1 day ago
            ),
            health = HealthInfo(
                seeders = null, // Direct stream
                leechers = null,
                downloadSpeed = null,
                uploadSpeed = null,
                availability = null,
                lastChecked = null
            ),
            features = FeatureInfo(
                subtitles = listOf(
                    SubtitleInfo("English", "en", SubtitleInfo.SubtitleType.EMBEDDED),
                    SubtitleInfo("Spanish", "es", SubtitleInfo.SubtitleType.EMBEDDED),
                    SubtitleInfo("French", "fr", SubtitleInfo.SubtitleType.EMBEDDED)
                ),
                has3D = false,
                hasChapters = true,
                hasMultipleAudioTracks = true,
                isDirectPlay = true,
                requiresTranscoding = false
            ),
            availability = AvailabilityInfo(
                isAvailable = true,
                region = "US",
                expiryDate = null,
                debridService = null,
                cached = false
            )
        )
    }
    
    private fun createMediumQualitySource(): SourceMetadata {
        return SourceMetadata(
            id = "source_2",
            provider = SourceProviderInfo(
                id = "provider_2",
                name = "StreamFlix",
                displayName = "StreamFlix",
                logoUrl = null,
                type = SourceProviderInfo.ProviderType.DIRECT_STREAM,
                reliability = SourceProviderInfo.ProviderReliability.GOOD
            ),
            quality = QualityInfo(
                resolution = VideoResolution.RESOLUTION_1080P,
                bitrate = 8_000_000L, // 8 Mbps
                hdr10 = true,
                hdr10Plus = false,
                dolbyVision = false,
                frameRate = 24
            ),
            codec = CodecInfo(
                type = VideoCodec.H264,
                profile = "High",
                level = "4.1"
            ),
            audio = AudioInfo(
                format = AudioFormat.EAC3,
                channels = "5.1",
                bitrate = 640,
                language = "en",
                dolbyAtmos = false,
                dtsX = false
            ),
            release = ReleaseInfo(
                type = ReleaseType.WEB_DL,
                group = "NTb",
                edition = null,
                year = 2023
            ),
            file = FileInfo(
                name = "Movie.2023.1080p.WEB-DL.DDP5.1.H.264-NTb.mkv",
                sizeInBytes = 4_500_000_000L, // ~4.5 GB
                extension = "mkv",
                hash = "def456ghi789",
                addedDate = Date(System.currentTimeMillis() - 172800000) // 2 days ago
            ),
            health = HealthInfo(),
            features = FeatureInfo(
                subtitles = listOf(
                    SubtitleInfo("English", "en", SubtitleInfo.SubtitleType.EMBEDDED)
                ),
                has3D = false,
                hasChapters = false,
                hasMultipleAudioTracks = false,
                isDirectPlay = true,
                requiresTranscoding = false
            ),
            availability = AvailabilityInfo(
                isAvailable = true,
                region = null,
                expiryDate = null,
                debridService = null,
                cached = false
            )
        )
    }
    
    private fun createP2PSource(): SourceMetadata {
        return SourceMetadata(
            id = "source_3",
            provider = SourceProviderInfo(
                id = "provider_3",
                name = "TorrentHub",
                displayName = "TorrentHub",
                logoUrl = null,
                type = SourceProviderInfo.ProviderType.TORRENT,
                reliability = SourceProviderInfo.ProviderReliability.FAIR
            ),
            quality = QualityInfo(
                resolution = VideoResolution.RESOLUTION_1080P,
                bitrate = 12_000_000L, // 12 Mbps
                hdr10 = false,
                hdr10Plus = false,
                dolbyVision = false,
                frameRate = 24
            ),
            codec = CodecInfo(
                type = VideoCodec.H264,
                profile = "High",
                level = "4.1"
            ),
            audio = AudioInfo(
                format = AudioFormat.AC3,
                channels = "5.1",
                bitrate = 448,
                language = "en",
                dolbyAtmos = false,
                dtsX = false
            ),
            release = ReleaseInfo(
                type = ReleaseType.BLURAY,
                group = "YIFY",
                edition = null,
                year = 2023
            ),
            file = FileInfo(
                name = "Movie.2023.1080p.BluRay.x264.DD5.1-YIFY.mkv",
                sizeInBytes = 2_100_000_000L, // ~2.1 GB
                extension = "mkv",
                hash = "ghi789jkl012",
                addedDate = Date(System.currentTimeMillis() - 259200000) // 3 days ago
            ),
            health = HealthInfo(
                seeders = 847,
                leechers = 23,
                downloadSpeed = 5_242_880L, // 5 MB/s
                uploadSpeed = 1_048_576L, // 1 MB/s
                availability = 0.98f,
                lastChecked = Date(System.currentTimeMillis() - 3600000) // 1 hour ago
            ),
            features = FeatureInfo(
                subtitles = listOf(
                    SubtitleInfo("English", "en", SubtitleInfo.SubtitleType.EXTERNAL)
                ),
                has3D = false,
                hasChapters = false,
                hasMultipleAudioTracks = false,
                isDirectPlay = false,
                requiresTranscoding = true
            ),
            availability = AvailabilityInfo(
                isAvailable = true,
                region = null,
                expiryDate = null,
                debridService = null,
                cached = false
            )
        )
    }
    
    private fun createCachedSource(): SourceMetadata {
        return SourceMetadata(
            id = "source_4",
            provider = SourceProviderInfo(
                id = "provider_4",
                name = "RealDebrid",
                displayName = "Real-Debrid",
                logoUrl = null,
                type = SourceProviderInfo.ProviderType.DEBRID,
                reliability = SourceProviderInfo.ProviderReliability.EXCELLENT
            ),
            quality = QualityInfo(
                resolution = VideoResolution.RESOLUTION_4K,
                bitrate = 25_000_000L, // 25 Mbps
                hdr10 = true,
                hdr10Plus = false,
                dolbyVision = false,
                frameRate = 24
            ),
            codec = CodecInfo(
                type = VideoCodec.HEVC,
                profile = "Main 10",
                level = "5.0"
            ),
            audio = AudioInfo(
                format = AudioFormat.DTS_HD_MA,
                channels = "7.1",
                bitrate = 1536,
                language = "en",
                dolbyAtmos = false,
                dtsX = true
            ),
            release = ReleaseInfo(
                type = ReleaseType.BLURAY,
                group = "SPARKS",
                edition = null,
                year = 2023
            ),
            file = FileInfo(
                name = "Movie.2023.2160p.UHD.BluRay.x265.HDR.DTS-HD.MA.7.1-SPARKS.mkv",
                sizeInBytes = 15_000_000_000L, // ~15 GB
                extension = "mkv",
                hash = "jkl012mno345",
                addedDate = Date(System.currentTimeMillis() - 604800000) // 1 week ago
            ),
            health = HealthInfo(
                seeders = 1205,
                leechers = 45,
                downloadSpeed = null,
                uploadSpeed = null,
                availability = 1.0f,
                lastChecked = Date(System.currentTimeMillis() - 7200000) // 2 hours ago
            ),
            features = FeatureInfo(
                subtitles = listOf(
                    SubtitleInfo("English", "en", SubtitleInfo.SubtitleType.EMBEDDED),
                    SubtitleInfo("Spanish", "es", SubtitleInfo.SubtitleType.EMBEDDED)
                ),
                has3D = false,
                hasChapters = true,
                hasMultipleAudioTracks = true,
                isDirectPlay = true,
                requiresTranscoding = false
            ),
            availability = AvailabilityInfo(
                isAvailable = true,
                region = null,
                expiryDate = Date(System.currentTimeMillis() + 2592000000L), // 30 days
                debridService = "real-debrid",
                cached = true
            )
        )
    }
    
    private fun createStreamingSource(): SourceMetadata {
        return SourceMetadata(
            id = "source_5",
            provider = SourceProviderInfo(
                id = "provider_5",
                name = "StreamVault",
                displayName = "StreamVault",
                logoUrl = null,
                type = SourceProviderInfo.ProviderType.DIRECT_STREAM,
                reliability = SourceProviderInfo.ProviderReliability.GOOD
            ),
            quality = QualityInfo(
                resolution = VideoResolution.RESOLUTION_720P,
                bitrate = 3_500_000L, // 3.5 Mbps
                hdr10 = false,
                hdr10Plus = false,
                dolbyVision = false,
                frameRate = 30
            ),
            codec = CodecInfo(
                type = VideoCodec.H264,
                profile = "Main",
                level = "3.1"
            ),
            audio = AudioInfo(
                format = AudioFormat.AAC,
                channels = "2.0",
                bitrate = 128,
                language = "en",
                dolbyAtmos = false,
                dtsX = false
            ),
            release = ReleaseInfo(
                type = ReleaseType.WEBRIP,
                group = "ION10",
                edition = null,
                year = 2023
            ),
            file = FileInfo(
                name = "Movie.2023.720p.WEBRip.AAC2.0.H.264-ION10.mp4",
                sizeInBytes = 1_200_000_000L, // ~1.2 GB
                extension = "mp4",
                hash = "mno345pqr678",
                addedDate = Date(System.currentTimeMillis() - 432000000) // 5 days ago
            ),
            health = HealthInfo(),
            features = FeatureInfo(
                subtitles = emptyList(),
                has3D = false,
                hasChapters = false,
                hasMultipleAudioTracks = false,
                isDirectPlay = true,
                requiresTranscoding = false
            ),
            availability = AvailabilityInfo(
                isAvailable = true,
                region = "US",
                expiryDate = null,
                debridService = null,
                cached = false
            )
        )
    }
}

/**
 * Preview component showcasing different metadata display modes
 */
@Composable
fun MetadataDisplayPreview(
    modifier: Modifier = Modifier
) {
    val sampleSources = remember { MetadataDisplayExamples.createSampleSourceMetadata() }
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    var displayMode by remember { mutableStateOf(SourceDisplayMode.COMPACT) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title
        Text(
            text = "Enhanced Metadata Display System",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Enhanced source container demo
        EnhancedSourceContainer(
            sources = sampleSources,
            displayMode = displayMode,
            onSourceSelect = { selectedSourceId = it.id },
            onSourcePlay = { /* Handle play */ },
            selectedSourceId = selectedSourceId,
            showMetadataTooltips = true,
            maxDisplayedSources = 5
        )
        
        // Individual component demos
        Text(
            text = "Individual Components",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Expandable metadata demo
        ExpandableSourceMetadata(
            sourceMetadata = sampleSources.first(),
            initiallyExpanded = false,
            showTooltips = true,
            maxCollapsedItems = 3
        )
    }
}

/**
 * Integration guide for developers
 */
object MetadataDisplayIntegration {
    
    /**
     * Example of integrating the enhanced metadata display in a details screen
     */
    @Composable
    fun DetailsScreenExample(
        sources: List<SourceMetadata>,
        onSourceSelect: (SourceMetadata) -> Unit,
        onSourcePlay: (SourceMetadata) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var selectedSourceId by remember { mutableStateOf<String?>(null) }
        
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = "Available Sources",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            item {
                EnhancedSourceContainer(
                    sources = sources,
                    displayMode = SourceDisplayMode.DETAILED,
                    onSourceSelect = { source ->
                        selectedSourceId = source.id
                        onSourceSelect(source)
                    },
                    onSourcePlay = onSourcePlay,
                    selectedSourceId = selectedSourceId,
                    showMetadataTooltips = true
                )
            }
        }
    }
    
    /**
     * Example of using individual metadata components
     */
    @Composable
    fun IndividualComponentsExample(
        sourceMetadata: SourceMetadata,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact badge row for lists
            CompactBadgeRow(
                sourceMetadata = sourceMetadata,
                maxBadges = 5
            )
            
            // Quality score indicator
            QualityScoreIndicator(
                score = sourceMetadata.getQualityScore(),
                modifier = Modifier.size(60.dp)
            )
            
            // Provider type indicator
            ProviderTypeIndicator(
                providerType = sourceMetadata.provider.type,
                reliability = sourceMetadata.provider.reliability
            )
            
            // Enhanced metadata row
            EnhancedMetadataRow(
                sourceMetadata = sourceMetadata,
                compact = false
            )
        }
    }
}

/**
 * Performance considerations and best practices
 */
object MetadataDisplayBestPractices {
    
    /**
     * Guidelines for optimal TV performance:
     * 
     * 1. Use lazy loading for large source lists
     * 2. Limit visible badges to prevent UI clutter
     * 3. Implement progressive disclosure for detailed information
     * 4. Use tooltips sparingly to avoid navigation complexity
     * 5. Ensure all text is readable at 10-foot viewing distance
     * 6. Provide clear focus indicators for remote control navigation
     * 7. Use high contrast colors for badge differentiation
     * 8. Implement caching for expensive metadata calculations
     */
    
    /**
     * Recommended badge limits by display mode:
     * - Compact: 3-5 badges maximum
     * - Detailed: 6-8 badges maximum
     * - Grid: 4-6 badges maximum
     */
    
    /**
     * Color accessibility guidelines:
     * - Use sufficient contrast ratios (4.5:1 minimum)
     * - Avoid red/green only distinctions
     * - Test with color blindness simulators
     * - Provide alternative indicators beyond color
     */
}