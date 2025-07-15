package com.rdwatch.androidtv.ui.details.models.advanced

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Comprehensive manager for advanced source selection features
 * Integrates health monitoring, season pack detection, prediction, and caching
 */
class AdvancedSourceManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Core components
    private val healthMonitor = HealthMonitor()
    private val seasonPackDetector = SeasonPackDetector()
    private val healthPredictor = HealthPredictor()
    private val cacheManager = HealthCacheManager(context)
    
    // Service integration
    private var monitoringService: HealthMonitoringService? = null
    
    // State flows
    private val _sourceUpdates = MutableSharedFlow<SourceUpdate>()
    val sourceUpdates: SharedFlow<SourceUpdate> = _sourceUpdates.asSharedFlow()
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    init {
        setupHealthMonitoring()
    }
    
    /**
     * Process a source with comprehensive analysis
     */
    suspend fun processSource(sourceMetadata: SourceMetadata): ProcessedSourceData {
        val startTime = System.currentTimeMillis()
        
        try {
            // Get or calculate health data
            val healthData = getOrCalculateHealthData(sourceMetadata)
            
            // Analyze season pack information
            val seasonPackInfo = analyzeSeasonPack(sourceMetadata)
            
            // Generate predictions
            val reliabilityPrediction = generateReliabilityPrediction(sourceMetadata, healthData)
            val downloadTimeEstimation = generateDownloadTimeEstimation(sourceMetadata, healthData)
            val riskAssessment = generateRiskAssessment(sourceMetadata, healthData)
            
            // Calculate enhanced quality score
            val enhancedQualityScore = sourceMetadata.getQualityScore(healthData, seasonPackInfo)
            
            // Generate comprehensive badges
            val badges = sourceMetadata.getQualityBadges(healthData, seasonPackInfo)
            
            // Start monitoring if not already monitored
            startMonitoringIfNeeded(sourceMetadata)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            val processedData = ProcessedSourceData(
                sourceMetadata = sourceMetadata,
                healthData = healthData,
                seasonPackInfo = seasonPackInfo,
                reliabilityPrediction = reliabilityPrediction,
                downloadTimeEstimation = downloadTimeEstimation,
                riskAssessment = riskAssessment,
                enhancedQualityScore = enhancedQualityScore,
                qualityBadges = badges,
                processingTimeMs = processingTime,
                lastUpdated = java.util.Date()
            )
            
            // Emit update
            _sourceUpdates.emit(SourceUpdate.Processed(sourceMetadata.id, processedData))
            
            return processedData
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            _sourceUpdates.emit(SourceUpdate.Error(sourceMetadata.id, e.message ?: "Unknown error"))
            throw e
        }
    }
    
    /**
     * Batch process multiple sources for optimal performance
     */
    suspend fun batchProcessSources(sources: List<SourceMetadata>): List<ProcessedSourceData> {
        val startTime = System.currentTimeMillis()
        
        // Preload cached health data
        val sourceIds = sources.map { it.id }
        val cachedHealthData = cacheManager.preloadHealthData(sourceIds)
        
        // Process sources in parallel with limited concurrency
        val processedSources = sources.chunked(10).flatMap { chunk: List<SourceMetadata> ->
            coroutineScope {
                chunk.map { source: SourceMetadata ->
                    async {
                        try {
                            processSource(source)
                        } catch (e: Exception) {
                            // Create minimal processed data for failed sources
                            ProcessedSourceData(
                                sourceMetadata = source,
                                healthData = null,
                                seasonPackInfo = seasonPackDetector.analyzeSeasonPack(
                                    source.file.name ?: "", 
                                    source.file.sizeInBytes
                                ),
                                reliabilityPrediction = null,
                                downloadTimeEstimation = null,
                                riskAssessment = null,
                                enhancedQualityScore = source.getQualityScore(),
                                qualityBadges = source.getQualityBadges(),
                                processingTimeMs = 0L,
                                lastUpdated = java.util.Date(),
                                hasError = true,
                                errorMessage = e.message
                            )
                        }
                    }
                }.awaitAll()
            }
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        _sourceUpdates.emit(SourceUpdate.BatchProcessed(sources.size, processedSources.size, totalTime))
        
        return processedSources
    }
    
    /**
     * Get optimized source recommendations based on user preferences
     */
    suspend fun getRecommendedSources(
        sources: List<SourceMetadata>,
        userProfile: UserProfile? = null,
        preferences: SourcePreferences = SourcePreferences()
    ): List<SourceRecommendation> {
        
        val processedSources = batchProcessSources(sources)
        
        return processedSources.map { processed ->
            val recommendationScore = calculateRecommendationScore(processed, userProfile, preferences)
            val reasoning = generateRecommendationReasoning(processed, recommendationScore)
            
            SourceRecommendation(
                type = com.rdwatch.androidtv.ui.details.models.advanced.RecommendationType.QUALITY_IMPROVEMENT,
                message = reasoning.firstOrNull() ?: "Recommended source",
                priority = when {
                    recommendationScore >= 80 -> com.rdwatch.androidtv.ui.details.models.advanced.RecommendationPriority.HIGH
                    recommendationScore >= 60 -> com.rdwatch.androidtv.ui.details.models.advanced.RecommendationPriority.MEDIUM
                    else -> com.rdwatch.androidtv.ui.details.models.advanced.RecommendationPriority.LOW
                }
            )
        }
    }
    
    /**
     * Update download result for machine learning
     */
    suspend fun updateDownloadResult(
        sourceId: String,
        success: Boolean,
        actualDownloadTimeMs: Long,
        sourceMetadata: SourceMetadata
    ) {
        healthPredictor.updateHistoricalData(
            sourceMetadata.provider.id,
            actualDownloadTimeMs,
            success,
            sourceMetadata
        )
        
        _sourceUpdates.emit(SourceUpdate.DownloadResultUpdated(sourceId, success))
    }
    
    /**
     * Get comprehensive analytics
     */
    suspend fun getAnalytics(): SourceManagerAnalytics {
        val cacheStats = cacheManager.getCacheStatistics()
        val monitoringStats = monitoringService?.getPerformanceMetrics()?.getMetricsSummary()
        
        return SourceManagerAnalytics(
            cacheStatistics = cacheStats,
            monitoringStatistics = monitoringStats,
            totalSourcesProcessed = 0, // TODO: Track this
            averageProcessingTimeMs = 0L, // TODO: Track this
            healthPredictionAccuracy = 0.85f, // TODO: Calculate from historical data
            recommendationEffectiveness = 0.78f // TODO: Calculate from user feedback
        )
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        healthMonitor.cleanup()
        cacheManager.cleanup()
        monitoringService = null
    }
    
    // MARK: - Private Implementation
    
    private suspend fun getOrCalculateHealthData(sourceMetadata: SourceMetadata): HealthData {
        // Try cache first
        val cached = cacheManager.getHealthData(sourceMetadata.id)
        if (cached != null && !cached.needsRefresh) {
            return cached
        }
        
        // Calculate new health data
        val healthData = healthMonitor.calculateHealthScore(
            sourceMetadata.health,
            sourceMetadata.provider
        )
        
        // Cache the result
        cacheManager.storeHealthData(sourceMetadata.id, healthData)
        
        return healthData
    }
    
    private fun analyzeSeasonPack(sourceMetadata: SourceMetadata): SeasonPackInfo {
        val filename = sourceMetadata.file.name ?: ""
        return seasonPackDetector.analyzeSeasonPack(filename, sourceMetadata.file.sizeInBytes)
    }
    
    private suspend fun generateReliabilityPrediction(
        sourceMetadata: SourceMetadata,
        healthData: HealthData
    ): ReliabilityPrediction {
        return healthPredictor.predictReliability(sourceMetadata, healthData)
    }
    
    private suspend fun generateDownloadTimeEstimation(
        sourceMetadata: SourceMetadata,
        healthData: HealthData
    ): DownloadTimeEstimation {
        return healthPredictor.estimateDownloadTime(sourceMetadata, healthData)
    }
    
    private suspend fun generateRiskAssessment(
        sourceMetadata: SourceMetadata,
        healthData: HealthData
    ): DownloadRiskAssessment {
        return healthPredictor.assessDownloadRisk(sourceMetadata, healthData)
    }
    
    private fun startMonitoringIfNeeded(sourceMetadata: SourceMetadata) {
        // Only monitor high-value sources to avoid resource waste
        val shouldMonitor = sourceMetadata.quality.resolution.ordinal >= VideoResolution.RESOLUTION_1080P.ordinal ||
                           sourceMetadata.provider.reliability == SourceProviderInfo.ProviderReliability.EXCELLENT ||
                           (sourceMetadata.health.seeders ?: 0) > 50
        
        if (shouldMonitor) {
            monitoringService?.startMonitoringSource(sourceMetadata)
        }
    }
    
    private fun calculateRecommendationScore(
        processed: ProcessedSourceData,
        userProfile: UserProfile?,
        preferences: SourcePreferences
    ): Int {
        var score = processed.enhancedQualityScore
        
        // Health-based adjustments
        processed.healthData?.let { health ->
            score += when (health.riskLevel) {
                RiskLevel.MINIMAL -> 50
                RiskLevel.LOW -> 30
                RiskLevel.MEDIUM -> 0
                RiskLevel.HIGH -> -50
            }
            
            // Predicted reliability bonus
            score += (health.predictedReliability * 0.5).toInt()
        }
        
        // Season pack preferences
        if (preferences.preferSeasonPacks && processed.seasonPackInfo.isSeasonPack) {
            score += 100
        }
        
        // User profile compatibility
        userProfile?.let { profile ->
            // Quality preference matching
            if (profile.preferredQuality == processed.sourceMetadata.quality.resolution) {
                score += 75
            }
            
            // Connection speed compatibility
            profile.connectionSpeedMbps?.let { speed ->
                val fileSize = processed.sourceMetadata.file.sizeInBytes ?: return@let
                val estimatedHours = fileSize / (speed * 1_000_000 / 8) / 3600
                if (estimatedHours <= 2) score += 50 // Fast download
                else if (estimatedHours <= 6) score += 25 // Reasonable download
                else score -= 25 // Slow download
            }
        }
        
        return score.coerceIn(0, 2000)
    }
    
    private fun generateRecommendationReasoning(
        processed: ProcessedSourceData,
        score: Int
    ): List<String> {
        val reasons = mutableListOf<String>()
        
        // Quality reasons
        reasons.add("${processed.sourceMetadata.quality.resolution.displayName} quality")
        
        // Health reasons
        processed.healthData?.let { health ->
            when (health.riskLevel) {
                RiskLevel.MINIMAL -> reasons.add("Minimal download risk")
                RiskLevel.LOW -> reasons.add("Low download risk")
                RiskLevel.MEDIUM -> reasons.add("Moderate download risk")
                RiskLevel.HIGH -> reasons.add("High download risk - consider alternatives")
            }
            
            if (health.predictedReliability >= 90) {
                reasons.add("Highly reliable source")
            }
            
            if (health.p2pHealth.seeders > 100) {
                reasons.add("Excellent peer availability")
            }
        }
        
        // Season pack reasons
        if (processed.seasonPackInfo.isSeasonPack) {
            reasons.add(processed.seasonPackInfo.getDisplayText())
        }
        
        // Performance reasons
        if (score >= 1500) reasons.add("Excellent overall quality")
        else if (score >= 1200) reasons.add("Very good quality")
        else if (score >= 900) reasons.add("Good quality")
        else if (score >= 600) reasons.add("Fair quality")
        else reasons.add("Consider alternative sources")
        
        return reasons
    }
    
    private fun calculateUserCompatibility(
        processed: ProcessedSourceData,
        userProfile: UserProfile?
    ): Float {
        if (userProfile == null) return 0.5f // Neutral compatibility
        
        var compatibility = 0.5f
        
        // Quality preference compatibility
        userProfile.preferredQuality?.let { preferred ->
            compatibility += when {
                processed.sourceMetadata.quality.resolution == preferred -> 0.3f
                processed.sourceMetadata.quality.resolution.ordinal >= preferred.ordinal -> 0.2f
                else -> -0.1f
            }
        }
        
        // Storage space compatibility
        userProfile.storageSpaceGB?.let { available ->
            val fileSizeGB = processed.sourceMetadata.file.sizeInBytes?.let { 
                it / (1024.0 * 1024.0 * 1024.0) 
            } ?: 0.0
            
            compatibility += when {
                fileSizeGB <= available * 0.1 -> 0.2f // Uses < 10% of space
                fileSizeGB <= available * 0.3 -> 0.1f // Uses < 30% of space
                fileSizeGB <= available * 0.7 -> 0.0f // Uses < 70% of space
                else -> -0.3f // Uses > 70% of space
            }
        }
        
        return compatibility.coerceIn(0.0f, 1.0f)
    }
    
    private fun calculateDownloadPriority(
        processed: ProcessedSourceData,
        preferences: SourcePreferences
    ): DownloadPriority {
        val healthScore = processed.healthData?.overallScore ?: 50
        val riskLevel = processed.healthData?.riskLevel ?: RiskLevel.MEDIUM
        
        return when {
            healthScore >= 90 && riskLevel == RiskLevel.MINIMAL -> DownloadPriority.URGENT
            healthScore >= 75 && riskLevel <= RiskLevel.LOW -> DownloadPriority.HIGH
            healthScore >= 60 && riskLevel <= RiskLevel.MEDIUM -> DownloadPriority.NORMAL
            healthScore >= 40 -> DownloadPriority.LOW
            else -> DownloadPriority.AVOID
        }
    }
    
    private fun setupHealthMonitoring() {
        // Observe health updates and cache them
        scope.launch {
            healthMonitor.healthUpdates.collect { healthUpdates ->
                healthUpdates.forEach { (sourceId, healthData) ->
                    cacheManager.storeHealthData(sourceId, healthData)
                }
            }
        }
    }
    
    /**
     * Bind to monitoring service
     */
    fun bindMonitoringService(service: HealthMonitoringService) {
        monitoringService = service
        
        // Observe health alerts
        scope.launch {
            service.healthAlerts.collect { alert ->
                _sourceUpdates.emit(SourceUpdate.HealthAlert(alert))
            }
        }
    }
}

/**
 * Processed source data with all analysis results
 */
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
    val lastUpdated: java.util.Date,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)


/**
 * User preferences for source recommendations
 */
data class SourcePreferences(
    val preferSeasonPacks: Boolean = false,
    val maxAcceptableRisk: RiskLevel = RiskLevel.MEDIUM,
    val prioritizeSpeed: Boolean = true,
    val prioritizeReliability: Boolean = true,
    val acceptableDownloadTimeHours: Int = 12
)

/**
 * Download priority levels
 */
enum class DownloadPriority {
    URGENT,
    HIGH,
    NORMAL,
    LOW,
    AVOID
}

/**
 * Source update events
 */
sealed class SourceUpdate {
    data class Processed(val sourceId: String, val data: ProcessedSourceData) : SourceUpdate()
    data class BatchProcessed(val requested: Int, val successful: Int, val timeMs: Long) : SourceUpdate()
    data class Error(val sourceId: String, val error: String) : SourceUpdate()
    data class HealthAlert(val alert: com.rdwatch.androidtv.ui.details.models.advanced.HealthAlert) : SourceUpdate()
    data class DownloadResultUpdated(val sourceId: String, val success: Boolean) : SourceUpdate()
}


