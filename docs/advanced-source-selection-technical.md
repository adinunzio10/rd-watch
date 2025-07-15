# Advanced Source Selection System - Technical Documentation

## System Architecture

### Core Design Principles

1. **Performance First**: Optimized for Android TV hardware constraints with limited memory and processing power
2. **Reactive Architecture**: Built on Kotlin Coroutines and Flow for responsive UI updates
3. **Modular Design**: Loosely coupled components that can be tested and maintained independently
4. **Caching Strategy**: Multi-tier caching to minimize repeated computations and network requests
5. **TV-Optimized**: Designed specifically for 10-foot UI with remote control navigation

### Component Hierarchy

```
AdvancedSourceManager (Orchestrator)
├── SourceFilterSystem (Filtering)
├── HealthMonitor (Health Analysis)
├── SeasonPackDetector (Content Analysis)
├── SourceSorter (Intelligent Sorting)
├── HealthPredictor (Machine Learning)
├── HealthCacheManager (Caching)
├── AndroidTVPerformanceOptimizer (Performance)
└── SourceAnalytics (Insights)
```

## Core Components

### AdvancedSourceManager

**Purpose**: Central orchestration of all advanced source selection features

**Key Methods**:
```kotlin
suspend fun processSource(sourceMetadata: SourceMetadata): ProcessedSourceData
suspend fun batchProcessSources(sources: List<SourceMetadata>): List<ProcessedSourceData>
suspend fun getRecommendedSources(sources: List<SourceMetadata>, userProfile: UserProfile?, preferences: SourcePreferences): List<SourceRecommendation>
```

**Features**:
- Unified processing interface with health analysis integration
- Batch processing optimization for Android TV performance
- Recommendation engine with user profiling support
- Real-time source update notifications via SharedFlow
- Machine learning feedback integration
- Performance monitoring and analytics

**Performance Optimizations**:
- Chunked processing for large source lists (>100 sources)
- Concurrent processing with configurable thread limits
- Preloading of cached health data
- Graceful error handling with fallback to basic functionality

### SourceFilterSystem

**Purpose**: Comprehensive multi-criteria filtering with conflict resolution

**Filter Categories**:
```kotlin
data class AdvancedSourceFilter(
    val qualityFilters: QualityFilters = QualityFilters(),
    val sourceTypeFilters: SourceTypeFilters = SourceTypeFilters(),
    val healthFilters: HealthFilters = HealthFilters(),
    val fileSizeFilters: FileSizeFilters = FileSizeFilters(),
    val codecFilters: CodecFilters = CodecFilters(),
    val audioFilters: AudioFilters = AudioFilters(),
    val releaseTypeFilters: ReleaseTypeFilters = ReleaseTypeFilters(),
    val providerFilters: ProviderFilters = ProviderFilters(),
    val ageFilters: AgeFilters = AgeFilters(),
    val customFilters: List<CustomFilter> = emptyList(),
    val conflictResolution: ConflictResolution = ConflictResolution()
)
```

**Conflict Resolution Strategies**:
- `RELAX_QUALITY`: Lower minimum resolution requirements
- `RELAX_HEALTH`: Reduce seeder/health requirements
- `RELAX_SIZE`: Increase file size limits
- `DISABLE_STRICT`: Remove strict requirements

**Performance Features**:
- Short-circuiting filter evaluation
- Early termination on filter failures
- Optimized boolean logic evaluation
- Filter description generation for UI display

### Health Monitoring System

#### HealthMonitor
**Purpose**: Core health calculation and monitoring engine

**Health Score Calculation**:
```kotlin
fun calculateHealthScore(sourceHealth: SourceHealth, provider: SourceProviderInfo): HealthData {
    val p2pScore = calculateP2PHealthScore(sourceHealth)
    val providerScore = calculateProviderReliabilityScore(provider)
    val availabilityScore = calculateAvailabilityScore(sourceHealth)
    val freshnessScore = calculateFreshnessScore(sourceHealth)
    
    return HealthData(
        overallScore = (p2pScore + providerScore + availabilityScore + freshnessScore) / 4,
        p2pHealth = P2PHealthData(sourceHealth.seeders, sourceHealth.leechers, sourceHealth.availability),
        providerReliability = providerScore,
        predictedReliability = predictReliability(sourceHealth, provider),
        riskLevel = assessRiskLevel(p2pScore, providerScore),
        lastUpdated = Date()
    )
}
```

**Health Factors**:
- **P2P Health (0-100)**: Logarithmic scaling for seeders, seeder/leecher ratio
- **Provider Reliability (0-40)**: Historical provider performance
- **Availability (0-30)**: Percentage of file available in swarm
- **Freshness (0-30)**: Age-based scoring with decay function

#### HealthMonitoringService
**Purpose**: Background service for real-time monitoring

**Monitoring Features**:
- 60-second health check intervals for active sources
- Automatic cleanup of inactive sources after 5 minutes
- Health degradation alerts with severity levels
- Performance metrics tracking
- Batch processing for efficiency

**Alert Types**:
```kotlin
sealed class HealthAlert {
    data class LowHealth(val sourceId: String, val score: Int) : HealthAlert()
    data class DegradingTrend(val sourceId: String, val trend: Float) : HealthAlert()
    data class DeadTorrent(val sourceId: String) : HealthAlert()
    data class StaleData(val sourceId: String, val age: Duration) : HealthAlert()
}
```

#### HealthPredictor
**Purpose**: Machine learning prediction algorithms

**Prediction Algorithms**:
- **Reliability Prediction**: Pattern-based future reliability estimation using historical data
- **Download Time Estimation**: Multi-factor time prediction considering file size, connection speed, and P2P health
- **Risk Assessment**: Comprehensive download risk analysis

**Learning Methods**:
```kotlin
suspend fun updateHistoricalData(providerId: String, actualDownloadTime: Long, success: Boolean, sourceMetadata: SourceMetadata)
suspend fun predictReliability(sourceMetadata: SourceMetadata, healthData: HealthData): ReliabilityPrediction
suspend fun estimateDownloadTime(sourceMetadata: SourceMetadata, healthData: HealthData): DownloadTimeEstimation
```

#### HealthCacheManager
**Purpose**: Multi-tier caching and persistence system

**Cache Tiers**:
1. **Memory Cache**: LRU cache with 5-minute expiration, 500-entry limit
2. **Disk Cache**: File-based cache with 1-hour expiration, 50MB limit
3. **Persistent Cache**: SharedPreferences with 24-hour expiration

**Cache Performance**:
```kotlin
// Cache statistics tracking
data class CacheStatistics(
    val memoryHits: Long,
    val diskHits: Long,
    val persistentHits: Long,
    val misses: Long,
    val averageRetrievalTimeMs: Double,
    val cacheSize: Int,
    val evictions: Long
) {
    fun getHitRate(): Float = (memoryHits + diskHits + persistentHits).toFloat() / (memoryHits + diskHits + persistentHits + misses)
}
```

### SeasonPackDetector

**Purpose**: Intelligent identification and analysis of season pack content

**Detection Patterns**:
```kotlin
private val seasonPatterns = listOf(
    Regex("""S(\d{1,2})""", RegexOption.IGNORE_CASE),
    Regex("""Season[\s\.]?(\d{1,2})""", RegexOption.IGNORE_CASE),
    Regex("""Complete[\s\.]?Season[\s\.]?(\d{1,2})""", RegexOption.IGNORE_CASE),
    Regex("""S(\d{1,2})-S(\d{1,2})""", RegexOption.IGNORE_CASE),
    Regex("""Seasons[\s\.]?(\d{1,2})-(\d{1,2})""", RegexOption.IGNORE_CASE),
    Regex("""Complete[\s\.]?Series""", RegexOption.IGNORE_CASE)
)
```

**Analysis Features**:
- Multi-regex pattern recognition for season/episode detection
- Completeness analysis with percentage calculations
- Multi-season and complete series detection
- Episode range parsing and validation
- Quality assessment for sorting prioritization
- Confidence scoring (0-100%) for detection accuracy

### Smart Sorting System

#### SourceSorter
**Purpose**: Main sorting engine with hierarchical criteria

**Sorting Hierarchy**:
```kotlin
private fun createSmartComparator(preferences: UserSortingPreferences): Comparator<SourceMetadata> {
    return compareByDescending<SourceMetadata> { it.availability.cached }  // 1. Cached first
        .thenByDescending { it.getSmartQualityScore(preferences) }          // 2. Quality score
        .thenByDescending { it.health.getHealthScore() }                    // 3. Health score
        .thenByDescending { it.provider.reliability.ordinal }              // 4. Provider reliability
        .thenBy { it.release.group?.let { group -> getReleaseGroupPenalty(group) } ?: 0 } // 5. Release group
        .thenBy { abs(it.file.sizeInBytes?.let { size -> calculateSizeScore(size, preferences) } ?: 0) } // 6. Size preference
        .thenBy { it.id } // 7. Stable tiebreaker
}
```

**Quality Score Calculation**:
```kotlin
fun getSmartQualityScore(preferences: UserSortingPreferences): Int {
    var score = resolution.baseScore          // 300-1000 points
    score += hdrBonus                         // 0-30 points
    score += codec.efficiencyBonus            // 0-50 points
    score += audio.qualityBonus               // 0-50 points
    score += releaseType.qualityBonus         // 0-100 points
    score += healthBonus                      // 0-50 points
    score += providerReliability * 10         // 0-40 points
    
    // Apply user preference bonuses
    if (resolution == preferences.preferredResolution) score += 75
    if (codec.type in preferences.preferredCodecs) score += 25
    if (releaseType in preferences.preferredReleaseTypes) score += 50
    
    return score
}
```

#### Performance Optimization
**AndroidTVPerformanceOptimizer**: Device-specific optimizations

**Device Capability Assessment**:
```kotlin
data class DeviceCapabilities(
    val tier: Tier,
    val availableMemoryMB: Int,
    val processorCores: Int,
    val isAndroidTV: Boolean
) {
    enum class Tier { HIGH_END, MID_RANGE, STANDARD, LOW_END }
}

private fun assessDeviceCapabilities(context: Context): DeviceCapabilities {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    
    val availableMemoryMB = (memoryInfo.availMem / (1024 * 1024)).toInt()
    val cores = Runtime.getRuntime().availableProcessors()
    
    val tier = when {
        availableMemoryMB >= 3000 && cores >= 8 -> DeviceCapabilities.Tier.HIGH_END
        availableMemoryMB >= 2000 && cores >= 6 -> DeviceCapabilities.Tier.MID_RANGE
        availableMemoryMB >= 1000 && cores >= 4 -> DeviceCapabilities.Tier.STANDARD
        else -> DeviceCapabilities.Tier.LOW_END
    }
    
    return DeviceCapabilities(tier, availableMemoryMB, cores, context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
}
```

**Performance Configuration**:
```kotlin
data class PerformanceConfig(
    val maxConcurrentThreads: Int,
    val maxBatchSize: Int,
    val cacheSize: Int,
    val enablePreloading: Boolean,
    val chunkSize: Int,
    val timeoutMs: Long
)

private fun createPerformanceConfig(capabilities: DeviceCapabilities): PerformanceConfig {
    return when (capabilities.tier) {
        DeviceCapabilities.Tier.HIGH_END -> PerformanceConfig(8, 50, 1000, true, 20, 30000)
        DeviceCapabilities.Tier.MID_RANGE -> PerformanceConfig(6, 30, 500, true, 15, 20000)
        DeviceCapabilities.Tier.STANDARD -> PerformanceConfig(4, 20, 250, false, 10, 15000)
        DeviceCapabilities.Tier.LOW_END -> PerformanceConfig(2, 10, 100, false, 5, 10000)
    }
}
```

## Data Models

### Enhanced Source Metadata
```kotlin
data class SourceMetadata(
    val id: String,
    val title: String,
    val file: FileInfo,
    val quality: QualityInfo,
    val codec: CodecInfo,
    val audio: AudioInfo,
    val release: ReleaseInfo,
    val provider: SourceProviderInfo,
    val health: SourceHealth,
    val availability: AvailabilityInfo,
    val url: String?,
    val lastUpdated: Date = Date()
) {
    // Enhanced methods for advanced features
    fun getQualityScore(healthData: HealthData? = null, seasonPackInfo: SeasonPackInfo? = null): Int
    fun getQualityBadges(healthData: HealthData? = null, seasonPackInfo: SeasonPackInfo? = null): List<QualityBadge>
    fun getHealthIndicators(): List<HealthIndicator>
    fun getSeasonPackInfo(): SeasonPackInfo?
}
```

### Processing Results
```kotlin
data class ProcessedSourceData(
    val sourceMetadata: SourceMetadata,
    val healthData: HealthData?,
    val seasonPackInfo: SeasonPackInfo,
    val reliabilityPrediction: ReliabilityPrediction?,
    val downloadTimeEstimation: DownloadTimeEstimation?,
    val riskAssessment: DownloadRiskAssessment?,
    val enhancedQualityScore: Int,
    val qualityBadges: List<QualityBadge>,
    val processingTimeMs: Long,
    val lastUpdated: Date,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)
```

### Recommendation System
```kotlin
data class SourceRecommendation(
    val processedSource: ProcessedSourceData,
    val recommendationScore: Int,
    val reasoning: List<String>,
    val userCompatibility: Float, // 0.0-1.0
    val downloadPriority: DownloadPriority
)

enum class DownloadPriority {
    URGENT,    // >90% health, minimal risk
    HIGH,      // >75% health, low risk
    NORMAL,    // >60% health, medium risk
    LOW,       // >40% health
    AVOID      // <40% health or high risk
}
```

## Integration Points

### Repository Integration
```kotlin
class SourceAggregationRepositoryImpl @Inject constructor(
    private val advancedSourceManager: AdvancedSourceManager,
    private val filterSystem: SourceFilterSystem,
    private val scraperApiClient: ScraperApiClient,
    private val realDebridApi: RealDebridApiService,
    private val performanceOptimizer: AndroidTVPerformanceOptimizer
) : SourceAggregationRepository {
    
    override fun getSources(contentDetail: ContentDetail, forceRefresh: Boolean): Flow<List<SourceMetadata>> = flow {
        try {
            emit(emptyList()) // Loading state
            
            // Get raw sources from scrapers
            val rawSources = scraperApiClient.getSources(contentDetail)
            
            // Apply performance optimization
            val optimizationPlan = performanceOptimizer.optimizeHealthCalculation(rawSources)
            
            // Process with advanced analysis
            val processedSources = when {
                rawSources.size <= optimizationPlan.directProcessingThreshold -> {
                    advancedSourceManager.batchProcessSources(rawSources)
                }
                else -> {
                    optimizationPlan.processInChunks(rawSources) { chunk ->
                        advancedSourceManager.batchProcessSources(chunk)
                    }
                }
            }
            
            emit(processedSources.map { it.sourceMetadata })
            
        } catch (e: Exception) {
            // Fallback to basic functionality
            val basicSources = scraperApiClient.getBasicSources(contentDetail)
            emit(basicSources)
        }
    }
}
```

### ViewModel Integration
```kotlin
class MovieDetailsViewModel @Inject constructor(
    private val sourceRepository: SourceAggregationRepository,
    private val advancedSourceManager: AdvancedSourceManager,
    private val filterSystem: SourceFilterSystem,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {
    
    private val _sourceState = MutableStateFlow(SourceSelectionState())
    val sourceState: StateFlow<SourceSelectionState> = _sourceState.asStateFlow()
    
    private val filterDebouncer = Debouncer(300L) // 300ms debounce
    
    fun loadSources(contentDetail: ContentDetail) {
        viewModelScope.launch {
            sourceRepository.getSources(contentDetail)
                .catch { error ->
                    _sourceState.value = _sourceState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
                .collect { sources ->
                    val preferences = userPreferences.getSourcePreferences()
                    
                    // Get enhanced recommendations
                    val recommendations = advancedSourceManager.getRecommendedSources(
                        sources, 
                        userPreferences.getUserProfile(),
                        preferences
                    )
                    
                    _sourceState.value = _sourceState.value.copy(
                        sources = sources,
                        recommendations = recommendations,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }
    
    fun applyFilter(filter: AdvancedSourceFilter) {
        filterDebouncer.debounce {
            viewModelScope.launch {
                val current = _sourceState.value.sources
                val filterResult = filterSystem.filterSources(current, filter)
                
                _sourceState.value = _sourceState.value.copy(
                    filteredSources = filterResult.filteredSources,
                    appliedFilters = filterResult.appliedFilters,
                    filterProcessingTime = filterResult.processingTimeMs
                )
            }
        }
    }
}
```

### UI Component Integration
```kotlin
@Composable
fun AdvancedSourceSelectionScreen(
    contentDetail: ContentDetail,
    viewModel: MovieDetailsViewModel = hiltViewModel()
) {
    val sourceState by viewModel.sourceState.collectAsState()
    
    LaunchedEffect(contentDetail) {
        viewModel.loadSources(contentDetail)
    }
    
    Column {
        // Filter panel
        AdvancedFilterPanel(
            currentFilter = sourceState.currentFilter,
            onFilterChange = viewModel::applyFilter,
            presets = FilterPresets.getAllPresets()
        )
        
        // Source list
        when {
            sourceState.isLoading -> LoadingIndicator()
            sourceState.error != null -> ErrorMessage(sourceState.error!!)
            else -> AdvancedSourceList(
                sources = sourceState.filteredSources ?: sourceState.sources,
                recommendations = sourceState.recommendations,
                onSourceSelect = viewModel::selectSource
            )
        }
    }
}
```

## Testing Strategy

### Unit Tests
```kotlin
class SourceFilterSystemTest {
    
    @Test
    fun `filter application removes sources below quality threshold`() {
        val sources = createTestSources()
        val filter = AdvancedSourceFilter(
            qualityFilters = QualityFilters(minResolution = VideoResolution.RESOLUTION_1080P)
        )
        
        val result = filterSystem.filterSources(sources, filter)
        
        assertTrue(result.filteredSources.all { it.quality.resolution.ordinal >= VideoResolution.RESOLUTION_1080P.ordinal })
    }
    
    @Test
    fun `conflict resolution relaxes constraints when no sources match`() {
        val sources = createLowQualitySources()
        val strictFilter = AdvancedSourceFilter(
            qualityFilters = QualityFilters(minResolution = VideoResolution.RESOLUTION_4K),
            conflictResolution = ConflictResolution(
                enabled = true,
                strategies = listOf(ConflictResolutionStrategy.RELAX_QUALITY)
            )
        )
        
        val result = filterSystem.filterSources(sources, strictFilter)
        
        assertTrue(result.filteredSources.isNotEmpty())
    }
}
```

### Integration Tests
```kotlin
class AdvancedSourceManagerIntegrationTest {
    
    @Test
    fun `batch processing handles large source lists efficiently`() = runTest {
        val largeSources = createLargeSourceList(200)
        val startTime = System.currentTimeMillis()
        
        val processed = advancedSourceManager.batchProcessSources(largeSources)
        
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime
        
        assertEquals(largeSources.size, processed.size)
        assertTrue(processingTime < 1000) // Should complete in under 1 second
    }
}
```

### Performance Tests
```kotlin
class PerformanceTest {
    
    @Test
    fun `memory usage remains within bounds during batch processing`() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val largeSources = createLargeSourceList(500)
        advancedSourceManager.batchProcessSources(largeSources)
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        assertTrue(memoryIncrease < 50 * 1024 * 1024) // Less than 50MB increase
    }
}
```

## Performance Benchmarks

### Target Performance Metrics
- **Single Source Processing**: <50ms
- **Batch Processing (50 sources)**: <200ms
- **Filter Application (200 sources)**: <100ms
- **Cache Retrieval**: <1ms
- **Memory Usage**: <50MB additional heap during processing
- **UI Responsiveness**: No frame drops during background processing

### Monitoring and Profiling
```kotlin
class PerformanceMonitor {
    private val metrics = mutableMapOf<String, MutableList<Long>>()
    
    inline fun <T> measure(operation: String, block: () -> T): T {
        val startTime = System.nanoTime()
        val result = block()
        val endTime = System.nanoTime()
        
        val durationMs = (endTime - startTime) / 1_000_000
        metrics.getOrPut(operation) { mutableListOf() }.add(durationMs)
        
        return result
    }
    
    fun getStatistics(): Map<String, PerformanceStats> {
        return metrics.mapValues { (_, times) ->
            PerformanceStats(
                averageMs = times.average(),
                medianMs = times.sorted()[times.size / 2].toDouble(),
                maxMs = times.maxOrNull()?.toDouble() ?: 0.0,
                minMs = times.minOrNull()?.toDouble() ?: 0.0,
                count = times.size
            )
        }
    }
}
```

## Error Handling and Resilience

### Error Recovery Strategies
```kotlin
class ErrorHandler {
    suspend fun <T> withFallback(
        primary: suspend () -> T,
        fallback: suspend () -> T,
        onError: (Throwable) -> Unit = {}
    ): T {
        return try {
            primary()
        } catch (e: Exception) {
            onError(e)
            fallback()
        }
    }
    
    suspend fun withRetry(
        maxAttempts: Int = 3,
        delayMs: Long = 1000,
        operation: suspend () -> Unit
    ) {
        repeat(maxAttempts) { attempt ->
            try {
                operation()
                return
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) throw e
                delay(delayMs * (attempt + 1))
            }
        }
    }
}
```

## Deployment and Configuration

### Dependency Injection Setup
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AdvancedSourceModule {
    
    @Provides
    @Singleton
    fun provideAdvancedSourceManager(
        context: Context,
        healthMonitor: HealthMonitor,
        seasonPackDetector: SeasonPackDetector,
        healthPredictor: HealthPredictor,
        cacheManager: HealthCacheManager
    ): AdvancedSourceManager {
        return AdvancedSourceManager(context).apply {
            // Bind monitoring service when available
            bindMonitoringService(healthMonitoringService)
        }
    }
    
    @Provides
    @Singleton
    fun providePerformanceOptimizer(context: Context): AndroidTVPerformanceOptimizer {
        return AndroidTVPerformanceOptimizer(context)
    }
}
```

### Configuration Management
```kotlin
object AdvancedSourceConfig {
    // Health monitoring configuration
    const val HEALTH_MONITORING_INTERVAL_MS = 60_000L
    const val HEALTH_ALERT_THRESHOLD = 30
    const val MAX_MONITORED_SOURCES = 100
    
    // Cache configuration
    const val MEMORY_CACHE_SIZE = 500
    const val DISK_CACHE_SIZE_MB = 50
    const val MEMORY_CACHE_EXPIRATION_MS = 5 * 60 * 1000L // 5 minutes
    const val DISK_CACHE_EXPIRATION_MS = 60 * 60 * 1000L // 1 hour
    
    // Performance configuration
    const val MAX_CONCURRENT_THREADS = 8
    const val MAX_BATCH_SIZE = 50
    const val PROCESSING_TIMEOUT_MS = 30_000L
    
    // Filter configuration
    const val MAX_CUSTOM_FILTERS = 10
    const val FILTER_DEBOUNCE_MS = 300L
}
```

This technical documentation provides developers with comprehensive information about the Advanced Source Selection System's architecture, implementation details, and integration patterns.