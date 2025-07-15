# Claude Testing Documentation

This file contains testing strategy, commands, and maintenance procedures for the RD Watch Android TV application. It is automatically maintained by Claude Code as tests are added or modified.

## Current Testing Status

**Status**: Partial test configuration with Android Log mocking enabled

**Target Test Coverage**: 
- Unit Tests: 80%+ for business logic and data models
- Integration Tests: Key user flows and navigation
- UI Tests: Focus management and TV-specific interactions

## Testing Strategy

### Unit Tests
- **Target**: Data models, business logic, utility functions
- **Framework**: JUnit 5 with Mockito
- **Location**: `app/src/test/java/com/rdwatch/androidtv/`

### Integration Tests  
- **Target**: Navigation flows, D-pad interaction, data flow
- **Framework**: Android Instrumentation Tests
- **Location**: `app/src/androidTest/java/com/rdwatch/androidtv/`

### Compose UI Tests
- **Target**: UI component testing with focus simulation
- **Framework**: Compose Testing API
- **Considerations**: TV-specific focus behavior, D-pad navigation

### TV-Specific Tests
- **Target**: Leanback compatibility, remote control simulation
- **Framework**: Espresso with TV extensions
- **Considerations**: 10-foot UI, overscan, focus traversal

### Advanced Source Selection Tests
- **Target**: Health monitoring, filtering, sorting algorithms, performance
- **Framework**: JUnit 5 with coroutines testing
- **Considerations**: Performance benchmarks, caching behavior, machine learning accuracy

## Standard Android Test Commands

```bash
# Unit tests (when configured)
./gradlew test

# Integration tests (when configured)  
./gradlew connectedCheck

# Test specific module
./gradlew app:test

# Test with coverage
./gradlew test jacocoTestReport

# Run tests on specific device
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rdwatch.androidtv.SpecificTest
```

## Test Organization

### Recommended Directory Structure
```
app/src/test/java/com/rdwatch/androidtv/
├── data/
│   ├── MovieTest.kt                 # Data model tests
│   └── MovieListTest.kt             # Data provider tests
├── ui/
│   ├── MainActivityTest.kt          # Main UI tests
│   └── theme/
│       ├── ThemeTest.kt             # Theme configuration tests
│       └── TypeTest.kt              # Typography tests
└── utils/                           # Utility function tests

app/src/androidTest/java/com/rdwatch/androidtv/
├── integration/
│   ├── NavigationFlowTest.kt        # Navigation integration tests
│   └── FocusManagementTest.kt       # Focus behavior tests
├── ui/
│   ├── ComposeUITest.kt             # Compose UI integration tests
│   └── TVSpecificTest.kt            # TV-specific UI tests
└── leanback/                        # Legacy leanback tests
```

## Test Development Guidelines

### Writing Unit Tests

```kotlin
// Example unit test structure
@Test
fun `movie data model should validate correctly`() {
    // Given
    val movie = Movie(
        title = "Test Movie",
        description = "Test Description",
        cardImageUrl = "https://example.com/image.jpg"
    )
    
    // When
    val isValid = movie.isValid()
    
    // Then
    assertTrue(isValid)
}
```

### Writing Compose UI Tests

```kotlin
// Example Compose UI test with focus
@Test
fun `movie card should handle focus correctly`() {
    composeTestRule.setContent {
        RDWatchTheme {
            MovieCard(
                movie = sampleMovie,
                onMovieClick = { },
                modifier = Modifier.testTag("movie_card")
            )
        }
    }
    
    // Test focus behavior
    composeTestRule.onNodeWithTag("movie_card")
        .requestFocus()
        .assertIsFocused()
}
```

### Writing TV-Specific Tests

```kotlin
// Example TV-specific test
@Test
fun `should handle dpad navigation correctly`() {
    // Setup TV emulator or device
    // Test D-pad navigation between focusable elements
    // Verify focus traversal follows expected pattern
}
```

## Test Maintenance Rules

### Auto-Maintenance by Claude Code

When Claude Code makes changes to the codebase:

1. **Add New Tests**: When creating new functions, classes, or UI components
2. **Update Existing Tests**: When modifying existing functionality
3. **Document Test Changes**: Update this file with new test patterns or conventions
4. **Verify Test Coverage**: Ensure new code maintains target coverage levels

### Specific Maintenance Triggers

- **New Data Models**: Add corresponding unit tests
- **New UI Components**: Add Compose UI tests with focus testing
- **New Navigation**: Add integration tests for navigation flows
- **New Business Logic**: Add unit tests with edge cases
- **Bug Fixes**: Add regression tests to prevent reoccurrence

## Test Configuration

### Dependencies to Add

```kotlin
// In app/build.gradle.kts
dependencies {
    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    
    // Android testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    // Compose testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")
    
    // TV-specific testing
    androidTestImplementation("androidx.leanback:leanback-test:1.2.0")
}
```

### Test Runner Configuration

```xml
<!-- In AndroidManifest.xml (test) -->
<instrumentation
    android:name="androidx.test.runner.AndroidJUnitRunner"
    android:targetPackage="com.rdwatch.androidtv" />
```

## TV Testing Considerations

### Focus Testing
- Test focus traversal in all directions (up, down, left, right)
- Verify focus indicators are visible and clear
- Test focus behavior with different screen sizes

### D-Pad Simulation
- Test all D-pad directions and center button
- Verify back button behavior
- Test menu and home button interactions

### Performance Testing
- Test on lower-end TV devices
- Verify smooth scrolling and transitions
- Test memory usage with large content lists

## Continuous Integration

### GitHub Actions Configuration

```yaml
# .github/workflows/test.yml
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      - name: Run tests
        run: ./gradlew test
      - name: Run lint
        run: ./gradlew lint
```

## Test Reporting

### Coverage Reports
- Use JaCoCo for coverage reporting
- Target minimum 80% coverage for new code
- Generate HTML reports for review

### Test Results
- Integrate with CI/CD pipeline
- Fail builds on test failures
- Generate test reports for PR reviews

## Maintenance Notes

*This file is automatically maintained by Claude Code. When adding or modifying tests:*

1. *Update test commands and configuration*
2. *Document new testing patterns*
3. *Keep test organization structure current*
4. *Update CI/CD configurations as needed*
5. *Record any TV-specific testing discoveries*

## Current Test Inventory

**Unit Tests**: 13 (8 passing, 5 failing)
- TMDbTVRepositorySeasonTest: 8 tests (3 passing, 5 failing - async flow issues)  
- TVDetailsViewModelSeasonTest: 5 tests (5 passing)

**Integration Tests**: 0  
**UI Tests**: 0
**TV-Specific Tests**: 0

### Test Configuration

Android Log mocking is now properly configured in `app/build.gradle.kts`:
```kotlin
testOptions {
    unitTests {
        isReturnDefaultValues = true
    }
}
```

This resolves RuntimeExceptions from unmocked Android framework calls in unit tests.

## Advanced Source Selection Testing

### Testing Strategy Overview

The Advanced Source Selection system requires comprehensive testing across multiple dimensions due to its complexity and performance requirements.

#### Test Categories

1. **Unit Tests**: Core algorithms and business logic
2. **Integration Tests**: Component interaction and data flow
3. **Performance Tests**: Android TV optimization and memory usage
4. **Accuracy Tests**: Machine learning and prediction accuracy
5. **UI Tests**: Filter panels and source list interactions

### Unit Testing

#### Filter System Tests
```kotlin
class SourceFilterSystemTest {
    
    @Test
    fun `quality filters should remove sources below resolution threshold`() {
        // Given
        val sources = listOf(
            createSourceWithResolution(VideoResolution.RESOLUTION_720P),
            createSourceWithResolution(VideoResolution.RESOLUTION_1080P),
            createSourceWithResolution(VideoResolution.RESOLUTION_4K)
        )
        val filter = AdvancedSourceFilter(
            qualityFilters = QualityFilters(minResolution = VideoResolution.RESOLUTION_1080P)
        )
        
        // When
        val result = filterSystem.filterSources(sources, filter)
        
        // Then
        assertEquals(2, result.filteredSources.size)
        assertTrue(result.filteredSources.all { 
            it.quality.resolution.ordinal >= VideoResolution.RESOLUTION_1080P.ordinal 
        })
    }
    
    @Test
    fun `conflict resolution should relax constraints when no sources match`() {
        // Given
        val lowQualitySources = listOf(createSourceWithResolution(VideoResolution.RESOLUTION_720P))
        val strictFilter = AdvancedSourceFilter(
            qualityFilters = QualityFilters(minResolution = VideoResolution.RESOLUTION_4K),
            conflictResolution = ConflictResolution(
                enabled = true,
                strategies = listOf(ConflictResolutionStrategy.RELAX_QUALITY)
            )
        )
        
        // When
        val result = filterSystem.filterSources(lowQualitySources, strictFilter)
        
        // Then
        assertTrue(result.filteredSources.isNotEmpty())
        assertTrue(result.appliedFilters.contains("Relaxed quality requirements"))
    }
}
```

#### Health Monitor Tests
```kotlin
class HealthMonitorTest {
    
    @Test
    fun `health score calculation should weight seeders logarithmically`() {
        // Given
        val lowSeeders = SourceHealth(seeders = 10, leechers = 5, availability = 100.0f)
        val highSeeders = SourceHealth(seeders = 1000, leechers = 50, availability = 100.0f)
        val provider = createMockProvider()
        
        // When
        val lowHealth = healthMonitor.calculateHealthScore(lowSeeders, provider)
        val highHealth = healthMonitor.calculateHealthScore(highSeeders, provider)
        
        // Then
        assertTrue(highHealth.p2pHealth.score > lowHealth.p2pHealth.score)
        // Verify logarithmic scaling prevents extreme differences
        assertTrue((highHealth.p2pHealth.score - lowHealth.p2pHealth.score) < 50)
    }
    
    @Test
    fun `risk assessment should identify dead torrents`() {
        // Given
        val deadTorrent = SourceHealth(seeders = 0, leechers = 10, availability = 0.0f)
        val provider = createMockProvider()
        
        // When
        val healthData = healthMonitor.calculateHealthScore(deadTorrent, provider)
        
        // Then
        assertEquals(RiskLevel.HIGH, healthData.riskLevel)
        assertTrue(healthData.overallScore < 20)
    }
}
```

#### Season Pack Detection Tests
```kotlin
class SeasonPackDetectorTest {
    
    @Test
    fun `should detect standard season pack patterns`() {
        val testCases = mapOf(
            "Show.Name.S01.Complete.2160p.BluRay.REMUX" to SeasonPackInfo(
                isSeasonPack = true,
                seasons = listOf(1),
                confidence = 95,
                packType = SeasonPackType.COMPLETE_SEASON
            ),
            "Series.S01-S05.Complete.Collection" to SeasonPackInfo(
                isSeasonPack = true,
                seasons = (1..5).toList(),
                confidence = 90,
                packType = SeasonPackType.MULTI_SEASON
            ),
            "Regular.Episode.S01E05.1080p" to SeasonPackInfo(
                isSeasonPack = false,
                confidence = 0
            )
        )
        
        testCases.forEach { (filename, expected) ->
            // When
            val result = seasonPackDetector.analyzeSeasonPack(filename, 85_000_000_000L)
            
            // Then
            assertEquals(expected.isSeasonPack, result.isSeasonPack, "Failed for: $filename")
            assertEquals(expected.seasons, result.seasons, "Failed for: $filename")
            assertTrue(result.confidence >= expected.confidence - 5, "Confidence too low for: $filename")
        }
    }
}
```

#### Sorting Algorithm Tests
```kotlin
class SourceSorterTest {
    
    @Test
    fun `cached sources should always rank higher than non-cached`() {
        // Given
        val cachedLowQuality = createSource(cached = true, qualityScore = 500)
        val nonCachedHighQuality = createSource(cached = false, qualityScore = 1500)
        val sources = listOf(nonCachedHighQuality, cachedLowQuality)
        val preferences = UserSortingPreferences()
        
        // When
        val sorted = sourceSorter.sortSources(sources, preferences)
        
        // Then
        assertEquals(cachedLowQuality.id, sorted.first().id)
    }
    
    @Test
    fun `sorting should be stable with consistent tiebreaker`() {
        // Given
        val identicalSources = (1..10).map { 
            createIdenticalSource(id = "source_$it")
        }
        val preferences = UserSortingPreferences()
        
        // When
        val sorted1 = sourceSorter.sortSources(identicalSources, preferences)
        val sorted2 = sourceSorter.sortSources(identicalSources.shuffled(), preferences)
        
        // Then
        assertEquals(sorted1.map { it.id }, sorted2.map { it.id })
    }
}
```

### Integration Testing

#### Advanced Source Manager Integration
```kotlin
class AdvancedSourceManagerIntegrationTest {
    
    @Test
    fun `batch processing should handle large source lists efficiently`() = runTest {
        // Given
        val largeSources = createLargeSourceList(200)
        val startTime = System.currentTimeMillis()
        
        // When
        val processed = advancedSourceManager.batchProcessSources(largeSources)
        
        // Then
        val processingTime = System.currentTimeMillis() - startTime
        assertEquals(largeSources.size, processed.size)
        assertTrue(processingTime < 1000, "Processing took ${processingTime}ms, expected <1000ms")
        assertTrue(processed.all { it.hasError.not() || it.errorMessage != null })
    }
    
    @Test
    fun `recommendation engine should personalize results`() = runTest {
        // Given
        val sources = createMixedQualitySources()
        val userProfile = UserProfile(preferredQuality = VideoResolution.RESOLUTION_4K)
        val preferences = SourcePreferences(preferSeasonPacks = true)
        
        // When
        val recommendations = advancedSourceManager.getRecommendedSources(sources, userProfile, preferences)
        
        // Then
        assertTrue(recommendations.isNotEmpty())
        // Verify 4K sources are ranked higher
        val top3 = recommendations.take(3)
        assertTrue(top3.count { it.processedSource.sourceMetadata.quality.resolution == VideoResolution.RESOLUTION_4K } >= 2)
        // Verify season packs get bonus
        val seasonPacks = recommendations.filter { it.processedSource.seasonPackInfo.isSeasonPack }
        if (seasonPacks.isNotEmpty()) {
            assertTrue(seasonPacks.first().recommendationScore > recommendations.find { !it.processedSource.seasonPackInfo.isSeasonPack }?.recommendationScore ?: 0)
        }
    }
}
```

#### Cache Performance Integration
```kotlin
class HealthCacheManagerIntegrationTest {
    
    @Test
    fun `multi-tier cache should fall back correctly`() = runTest {
        // Given
        val sourceId = "test_source_123"
        val healthData = createMockHealthData()
        
        // When - Store in persistent cache only
        cacheManager.storePersistentHealthData(sourceId, healthData)
        
        // Then - Should retrieve from persistent when memory/disk miss
        val retrieved = cacheManager.getHealthData(sourceId)
        assertNotNull(retrieved)
        assertEquals(healthData.overallScore, retrieved!!.overallScore)
    }
    
    @Test
    fun `cache statistics should track hit rates accurately`() = runTest {
        // Given
        val testData = (1..100).map { "source_$it" to createMockHealthData() }
        
        // When - Store all data
        testData.forEach { (id, data) -> cacheManager.storeHealthData(id, data) }
        
        // Access subset multiple times
        repeat(10) {
            testData.take(50).forEach { (id, _) -> cacheManager.getHealthData(id) }
        }
        
        // Then
        val stats = cacheManager.getCacheStatistics()
        assertTrue(stats.getHitRate() > 0.8f, "Hit rate was ${stats.getHitRate()}, expected >0.8")
        assertTrue(stats.memoryHits > 0)
    }
}
```

### Performance Testing

#### Android TV Performance Tests
```kotlin
class AndroidTVPerformanceTest {
    
    @Test
    fun `device capability assessment should categorize correctly`() {
        // Given
        val highEndDevice = DeviceCapabilities(
            tier = DeviceCapabilities.Tier.HIGH_END,
            availableMemoryMB = 4000,
            processorCores = 8,
            isAndroidTV = true
        )
        
        // When
        val config = performanceOptimizer.createPerformanceConfig(highEndDevice)
        
        // Then
        assertEquals(8, config.maxConcurrentThreads)
        assertEquals(50, config.maxBatchSize)
        assertTrue(config.enablePreloading)
    }
    
    @Test
    fun `memory usage should remain within bounds during processing`() {
        // Given
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        val largeSources = createLargeSourceList(500)
        
        // When
        runBlocking {
            advancedSourceManager.batchProcessSources(largeSources)
        }
        
        // Then
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        assertTrue(memoryIncrease < 50 * 1024 * 1024, "Memory increased by ${memoryIncrease / (1024 * 1024)}MB, expected <50MB")
    }
    
    @Test
    fun `chunked processing should maintain performance with large datasets`() = runTest {
        // Given
        val massiveSources = createLargeSourceList(1000)
        
        // When
        val startTime = System.currentTimeMillis()
        val result = performanceOptimizer.processSourcesOptimized(massiveSources, UserSortingPreferences(), maxResults = 50)
        val endTime = System.currentTimeMillis()
        
        // Then
        assertTrue(endTime - startTime < 2000, "Processing took ${endTime - startTime}ms, expected <2000ms")
        assertEquals(50, result.prioritySources.size)
        assertTrue(result.backgroundProcessed.isNotEmpty())
    }
}
```

### Accuracy Testing

#### Machine Learning Accuracy Tests
```kotlin
class HealthPredictorAccuracyTest {
    
    @Test
    fun `reliability predictions should improve with historical data`() = runTest {
        // Given
        val provider = "test_provider"
        val sourceMetadata = createMockSourceMetadata()
        
        // Simulate historical data
        repeat(50) { i ->
            val success = i % 4 != 0 // 75% success rate
            healthPredictor.updateHistoricalData(provider, 1000L + i * 100, success, sourceMetadata)
        }
        
        // When
        val prediction = healthPredictor.predictReliability(sourceMetadata, createMockHealthData())
        
        // Then
        assertTrue(prediction.successProbability > 0.7f && prediction.successProbability < 0.8f)
        assertTrue(prediction.confidence > 0.8f)
    }
    
    @Test
    fun `download time estimation should correlate with file size and health`() = runTest {
        // Given
        val smallFile = createSourceMetadata(sizeBytes = 1_000_000_000L) // 1GB
        val largeFile = createSourceMetadata(sizeBytes = 10_000_000_000L) // 10GB
        val healthData = createMockHealthData(seeders = 100)
        
        // When
        val smallEstimate = healthPredictor.estimateDownloadTime(smallFile, healthData)
        val largeEstimate = healthPredictor.estimateDownloadTime(largeFile, healthData)
        
        // Then
        assertTrue(largeEstimate.estimatedMinutes > smallEstimate.estimatedMinutes)
        assertTrue(largeEstimate.estimatedMinutes < smallEstimate.estimatedMinutes * 15) // Should be sub-linear due to parallel downloading
    }
}
```

### UI Testing

#### Filter Panel UI Tests
```kotlin
@RunWith(AndroidJUnit4::class)
class AdvancedFilterPanelTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `filter preset selection should update filter state`() {
        // Given
        var appliedFilter: AdvancedSourceFilter? = null
        
        composeTestRule.setContent {
            AdvancedFilterPanel(
                currentFilter = AdvancedSourceFilter(),
                onFilterChange = { appliedFilter = it },
                presets = FilterPresets.getAllPresets()
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Quality Focused").performClick()
        
        // Then
        assertNotNull(appliedFilter)
        assertEquals(VideoResolution.RESOLUTION_1080P, appliedFilter!!.qualityFilters.minResolution)
    }
    
    @Test
    fun `quality badge row should handle focus navigation`() {
        val badges = listOf(
            QualityBadge("4K", QualityBadge.Type.RESOLUTION, 100),
            QualityBadge("HDR10", QualityBadge.Type.HDR, 90),
            QualityBadge("H.265", QualityBadge.Type.CODEC, 80)
        )
        
        composeTestRule.setContent {
            QualityBadgeRow(
                badges = badges,
                focusable = true,
                onBadgeClick = { }
            )
        }
        
        // Test focus navigation with D-pad simulation
        composeTestRule.onNodeWithText("4K").requestFocus()
        composeTestRule.onNodeWithText("4K").assertIsFocused()
        
        // Simulate D-pad right
        composeTestRule.onNodeWithText("4K").performKeyPress(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
        composeTestRule.onNodeWithText("HDR10").assertIsFocused()
    }
}
```

### Test Data Helpers

#### Mock Data Creation
```kotlin
object TestDataFactory {
    
    fun createMockSourceMetadata(
        id: String = "test_source",
        resolution: VideoResolution = VideoResolution.RESOLUTION_1080P,
        cached: Boolean = false,
        seeders: Int = 50,
        sizeBytes: Long = 5_000_000_000L
    ): SourceMetadata {
        return SourceMetadata(
            id = id,
            title = "Test Source",
            file = FileInfo(name = "test.mkv", sizeInBytes = sizeBytes),
            quality = QualityInfo(resolution = resolution),
            codec = CodecInfo(type = VideoCodec.H264),
            audio = AudioInfo(format = AudioFormat.AC3),
            release = ReleaseInfo(type = ReleaseType.WEB_DL),
            provider = SourceProviderInfo(id = "test_provider", reliability = SourceProviderInfo.ProviderReliability.GOOD),
            health = SourceHealth(seeders = seeders, leechers = 10, availability = 100.0f),
            availability = AvailabilityInfo(cached = cached),
            url = "http://test.com/source"
        )
    }
    
    fun createLargeSourceList(count: Int): List<SourceMetadata> {
        return (1..count).map { i ->
            createMockSourceMetadata(
                id = "source_$i",
                resolution = VideoResolution.entries.random(),
                cached = i % 5 == 0, // 20% cached
                seeders = (10..500).random()
            )
        }
    }
}
```

### Test Performance Benchmarks

#### Target Performance Metrics
- **Filter Application**: <100ms for 200 sources
- **Health Calculation**: <50ms per source
- **Batch Processing**: <200ms for 50 sources
- **Cache Retrieval**: <1ms
- **UI Responsiveness**: No frame drops during background processing

#### Performance Test Runner
```kotlin
class PerformanceBenchmarkTest {
    
    @Test
    fun `benchmark filter system performance`() {
        val sources = TestDataFactory.createLargeSourceList(200)
        val filter = FilterPresets.QUALITY_FOCUSED
        
        val measurements = (1..10).map {
            measureTimeMillis {
                filterSystem.filterSources(sources, filter)
            }
        }
        
        val averageMs = measurements.average()
        assertTrue(averageMs < 100, "Average filter time was ${averageMs}ms, expected <100ms")
        
        println("Filter Performance: ${averageMs}ms average, ${measurements.min()}ms min, ${measurements.max()}ms max")
    }
}
```

This resolves RuntimeExceptions from unmocked Android framework calls in unit tests.

---

**Last Updated**: Auto-maintained by Claude Code  
**Related Files**: [CLAUDE.md](CLAUDE.md), [CLAUDE-architecture.md](CLAUDE-architecture.md), [docs/advanced-source-selection-technical.md](docs/advanced-source-selection-technical.md)