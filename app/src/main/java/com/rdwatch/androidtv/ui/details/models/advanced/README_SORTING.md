# Advanced Source Sorting System

## Overview

The Advanced Source Sorting System provides intelligent, multi-criteria sorting of streaming sources optimized for Android TV. The system prioritizes cached sources, quality, health indicators, and user preferences to deliver the best viewing experience.

## Core Algorithm

The smart sorting algorithm uses a hierarchical approach:

1. **Primary: Cached Status** - Cached sources are prioritized for instant playback
2. **Secondary: Smart Quality Score** - Comprehensive quality scoring considering resolution, HDR, codec, audio, and release type
3. **Tertiary: Health Score** - P2P health indicators (seeders, leechers, availability)
4. **Quaternary: Provider Reliability** - Provider reputation and reliability
5. **Quinary: Release Group Reputation** - Known good/bad release groups
6. **Senary: File Size Preference** - User-configurable size preferences
7. **Final: Source ID** - Stable tiebreaker for consistent ordering

## Key Components

### SourceSorter
Main sorting engine that implements the smart algorithm:
- Weighted quality scoring based on user preferences
- Health calculation for P2P sources
- Release group reputation system
- Android TV optimizations

### SourceComparators
Collection of specialized comparators for different scenarios:
- Quality-focused sorting
- Health-focused sorting (P2P)
- Size-based sorting
- Provider reliability
- Android TV optimized

### SourcePerformanceOptimizer
Performance optimizations for Android TV:
- Chunked processing for large source lists (>100 sources)
- Pre-filtering to remove unusable sources
- Cached source prioritization
- Memory-efficient processing

### SourceAnalytics
Analytics and insights for source collections:
- Quality distribution analysis
- Provider distribution
- Health statistics
- Recommendations for optimization

## Sorting Criteria

### Quality Score Calculation
```kotlin
fun getQualityScore(): Int {
    var score = 0
    score += resolution.baseScore          // 300-1000 points
    score += hdrBonus                      // 0-30 points
    score += codec.efficiencyBonus         // 0-50 points
    score += audio.qualityBonus            // 0-50 points
    score += releaseType.qualityBonus      // 0-100 points
    score += healthBonus                   // 0-50 points
    score += providerReliability * 10      // 0-40 points
    return score
}
```

### Health Score (P2P Sources)
- Logarithmic scaling for large seeder counts
- Seeder/leecher ratio consideration
- Availability percentage bonus
- Dead torrent filtering (0 seeders)

### Release Group Reputation
- **Trusted Groups**: RARBG, SPARKS, GECKOS (-50 penalty)
- **Good Groups**: YIFY, EZTV, ETTV (-20 penalty)
- **Poor Groups**: KORSUB, HC, HDCAM (+50 penalty)
- **Banned Groups**: FAKE, VIRUS, SCAM (+100 penalty)

## Performance Optimizations

### Android TV Specific
1. **Cached Sources First**: Immediate playability
2. **Chunked Processing**: Handle 200+ sources efficiently
3. **Memory Management**: Limited heap on Android TV devices
4. **UI Responsiveness**: Background processing with progress

### Processing Thresholds
- **Small Lists (<50)**: Direct sorting
- **Medium Lists (50-200)**: Optimized sorting with pre-filtering
- **Large Lists (>200)**: Chunked processing with top-N selection

### Pre-filtering Rules
- Remove dead torrents (0 seeders)
- Remove extremely low quality (240p, unknown)
- Remove untrusted providers (when many sources available)

## User Preferences

### UserSortingPreferences
```kotlin
data class UserSortingPreferences(
    val preferredResolution: VideoResolution = RESOLUTION_1080P,
    val preferredCodecs: Set<VideoCodec> = setOf(HEVC, AV1),
    val preferredReleaseTypes: Set<ReleaseType> = setOf(WEB_DL, BLURAY),
    val preferHDR: Boolean = false,
    val preferHighQualityAudio: Boolean = false,
    val preferDebrid: Boolean = true,
    val fileSizePreference: FileSizePreference = OPTIMAL,
    val preferredFileSizeGB: Double = 8.0
)
```

### Preset Configurations
- **Quality Focused**: 4K HDR, best codecs, large files
- **Android TV Optimized**: Cached sources, moderate sizes
- **Bandwidth Constrained**: Smallest files, 720p max
- **P2P Optimized**: High seeders, known groups
- **Instant Playback**: Cached/debrid sources only

## Grouping and Display

### Release Groups
Sources are grouped by release type for better organization:
- **BluRay/Remux** (Priority: 100)
- **Web-DL/WebRip** (Priority: 90)
- **HDTV** (Priority: 80)
- **DVD** (Priority: 70)
- **Cam/Screener** (Priority: 60)

### Analytics
Real-time analysis provides:
- Quality distribution charts
- Provider reliability metrics
- Health statistics for P2P sources
- Recommendations for filtering

## Usage Examples

### Basic Smart Sorting
```kotlin
val sorter = SourceSorter()
val sorted = sorter.sortSources(sources, userPreferences)
```

### Android TV Optimization
```kotlin
val optimizer = SourcePerformanceOptimizer()
val result = optimizer.optimizeForAndroidTV(sources, preferences)
val immediatelyAvailable = result.immediatelyAvailable
val backgroundProcessed = result.backgroundProcessed
```

### Custom Configuration
```kotlin
val config = SourceSortingConfiguration.qualityFocused()
val comparator = config.createComparator()
val sorted = sources.sortedWith(comparator)
```

### Reactive Processing
```kotlin
val processor = optimizer.createReactiveProcessor(preferences)
processor.updateSources(newSources) // Debounced processing
```

## Performance Metrics

### Target Performance
- **Processing Time**: <1 second for 200 sources
- **Memory Usage**: <50MB additional heap
- **UI Responsiveness**: Non-blocking operations

### Monitoring
```kotlin
val monitor = optimizer.createPerformanceMonitor()
// Automatic performance tracking and optimization suggestions
```

## Best Practices

1. **Use Android TV Optimization** for TV interfaces
2. **Enable Chunked Processing** for large source lists
3. **Prioritize Cached Sources** for better UX
4. **Configure User Preferences** for personalized sorting
5. **Monitor Performance** and adjust thresholds as needed

## Integration with UI

### State Management
The `SourceSelectionManager` integrates the sorting system:
```kotlin
suspend fun updateSources(sources: List<SourceMetadata>) {
    val processedResult = performanceOptimizer.processSourcesOptimized(
        sources = sources,
        preferences = userPreferences,
        maxResults = 50
    )
    // Update UI state
}
```

### Reactive Updates
Sources are processed reactively with debouncing to prevent excessive computation during rapid updates.

## Future Enhancements

1. **Machine Learning**: Adapt to user selection patterns
2. **Collaborative Filtering**: Learn from community preferences
3. **Dynamic Weights**: Adjust sorting weights based on content type
4. **A/B Testing**: Compare sorting algorithm effectiveness
5. **Caching**: Cache sorting results for identical source sets