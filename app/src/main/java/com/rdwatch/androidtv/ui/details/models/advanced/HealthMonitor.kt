package com.rdwatch.androidtv.ui.details.models.advanced

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * Comprehensive health monitoring system for sources
 * Provides real-time health tracking, prediction, and caching
 */
class HealthMonitor {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val healthCache = ConcurrentHashMap<String, CachedHealthData>()
    private val _healthUpdates = MutableStateFlow<Map<String, HealthData>>(emptyMap())
    val healthUpdates: StateFlow<Map<String, HealthData>> = _healthUpdates.asStateFlow()

    // Configuration
    private val cacheExpirationMs = 5 * 60 * 1000L // 5 minutes
    private val staleDataThresholdMs = 15 * 60 * 1000L // 15 minutes
    private val healthUpdateIntervalMs = 30 * 1000L // 30 seconds

    // Health history for prediction
    private val healthHistory = ConcurrentHashMap<String, MutableList<HealthSnapshot>>()

    init {
        startHealthMonitoring()
    }

    /**
     * Calculate comprehensive health score for a source
     */
    fun calculateHealthScore(
        healthInfo: HealthInfo,
        provider: SourceProviderInfo,
    ): HealthData {
        val p2pHealth = calculateP2PHealth(healthInfo)
        val availabilityScore = calculateAvailabilityScore(healthInfo)
        val providerReliability = calculateProviderReliability(provider)
        val freshnessScore = calculateFreshnessScore(healthInfo.lastChecked)
        val successRate = getDownloadSuccessRate(provider.id)

        val overallScore =
            combineHealthScores(
                p2pHealth,
                availabilityScore,
                providerReliability,
                freshnessScore,
                successRate,
            )

        val riskAssessment = calculateRiskAssessment(healthInfo, provider)
        val estimatedDownloadTime = estimateDownloadTime(healthInfo)

        return HealthData(
            sourceId = "${provider.id}_${healthInfo.hashCode()}",
            overallScore = overallScore,
            p2pHealth = p2pHealth,
            availabilityPercentage = availabilityScore,
            providerReliability = providerReliability,
            sourceAuthority = calculateSourceAuthority(provider),
            freshnessIndicator = freshnessScore,
            downloadSuccessRate = successRate,
            riskLevel = riskAssessment.level,
            riskFactors = riskAssessment.factors,
            estimatedDownloadTimeMinutes = estimatedDownloadTime,
            predictedReliability = predictReliability(provider.id),
            healthTrend = calculateHealthTrend(provider.id),
            lastUpdated = Date(),
            isStale = isDataStale(healthInfo.lastChecked),
            needsRefresh = shouldRefreshHealth(provider.id),
        )
    }

    /**
     * Calculate P2P health score (seeders, leechers, ratio analysis)
     */
    private fun calculateP2PHealth(healthInfo: HealthInfo): P2PHealthData {
        val seeders = healthInfo.seeders ?: 0
        val leechers = healthInfo.leechers ?: 0

        // Seeder score (0-100)
        val seederScore =
            when {
                seeders >= 1000 -> 100
                seeders >= 500 -> 90
                seeders >= 100 -> 80
                seeders >= 50 -> 70
                seeders >= 20 -> 60
                seeders >= 10 -> 50
                seeders >= 5 -> 40
                seeders >= 1 -> 25
                else -> 0
            }

        // Ratio analysis
        val ratio = if (leechers > 0) seeders.toFloat() / leechers else Float.MAX_VALUE
        val ratioScore =
            when {
                ratio >= 10.0f -> 100
                ratio >= 5.0f -> 90
                ratio >= 2.0f -> 80
                ratio >= 1.0f -> 70
                ratio >= 0.5f -> 60
                ratio >= 0.2f -> 40
                ratio > 0.0f -> 20
                else -> 0
            }

        // Activity score (combined seeders + leechers)
        val totalPeers = seeders + leechers
        val activityScore =
            when {
                totalPeers >= 2000 -> 100
                totalPeers >= 1000 -> 90
                totalPeers >= 500 -> 80
                totalPeers >= 200 -> 70
                totalPeers >= 100 -> 60
                totalPeers >= 50 -> 50
                totalPeers >= 20 -> 40
                totalPeers >= 10 -> 30
                totalPeers >= 5 -> 20
                totalPeers > 0 -> 10
                else -> 0
            }

        // Speed analysis
        val downloadSpeed = healthInfo.downloadSpeed ?: 0L
        val uploadSpeed = healthInfo.uploadSpeed ?: 0L
        val speedScore = calculateSpeedScore(downloadSpeed, uploadSpeed)

        // Combined P2P score
        val overallP2PScore =
            (
                seederScore * 0.4f +
                    ratioScore * 0.3f +
                    activityScore * 0.2f +
                    speedScore * 0.1f
            ).toInt()

        return P2PHealthData(
            seeders = seeders,
            leechers = leechers,
            ratio = ratio,
            totalPeers = totalPeers,
            seederScore = seederScore,
            ratioScore = ratioScore,
            activityScore = activityScore,
            speedScore = speedScore,
            overallScore = overallP2PScore,
            downloadSpeedBytesPerSec = downloadSpeed,
            uploadSpeedBytesPerSec = uploadSpeed,
            healthStatus = determineP2PHealthStatus(overallP2PScore),
        )
    }

    /**
     * Calculate availability percentage
     */
    private fun calculateAvailabilityScore(healthInfo: HealthInfo): Float {
        val baseAvailability = healthInfo.availability ?: 1.0f

        // Adjust based on seeders for P2P sources
        val seederAdjustment =
            when (val seeders = healthInfo.seeders) {
                null -> 0.0f // Not P2P, use base availability
                0 -> -0.5f // Dead torrent
                in 1..4 -> -0.2f // Low seeders
                in 5..19 -> -0.1f // Moderate seeders
                else -> 0.0f // Good seeders
            }

        return (baseAvailability + seederAdjustment).coerceIn(0.0f, 1.0f)
    }

    /**
     * Calculate provider reliability score
     */
    private fun calculateProviderReliability(provider: SourceProviderInfo): Int {
        val baseScore =
            when (provider.reliability) {
                SourceProviderInfo.ProviderReliability.EXCELLENT -> 95
                SourceProviderInfo.ProviderReliability.GOOD -> 80
                SourceProviderInfo.ProviderReliability.FAIR -> 60
                SourceProviderInfo.ProviderReliability.POOR -> 30
                SourceProviderInfo.ProviderReliability.UNKNOWN -> 50
            }

        // Adjust based on provider type
        val typeAdjustment =
            when (provider.type) {
                SourceProviderInfo.ProviderType.DEBRID -> 10 // Debrid services are generally more reliable
                SourceProviderInfo.ProviderType.DIRECT_STREAM -> 5
                SourceProviderInfo.ProviderType.TORRENT -> 0
                else -> 0
            }

        return (baseScore + typeAdjustment).coerceIn(0, 100)
    }

    /**
     * Calculate source authority score
     */
    private fun calculateSourceAuthority(provider: SourceProviderInfo): Int {
        // Authority based on provider capabilities and type
        var authorityScore = 50

        // Provider type authority
        authorityScore +=
            when (provider.type) {
                SourceProviderInfo.ProviderType.DEBRID -> 30
                SourceProviderInfo.ProviderType.DIRECT_STREAM -> 20
                SourceProviderInfo.ProviderType.TORRENT -> 10
                else -> 0
            }

        // Capability bonuses
        if ("hdr" in provider.capabilities) authorityScore += 5
        if ("4k" in provider.capabilities) authorityScore += 5
        if ("surround_sound" in provider.capabilities) authorityScore += 5
        if ("fast_indexing" in provider.capabilities) authorityScore += 10

        return authorityScore.coerceIn(0, 100)
    }

    /**
     * Calculate freshness score based on last checked time
     */
    private fun calculateFreshnessScore(lastChecked: Date?): Int {
        if (lastChecked == null) return 0

        val ageMs = System.currentTimeMillis() - lastChecked.time
        return when {
            ageMs < 5 * 60 * 1000L -> 100 // < 5 minutes
            ageMs < 15 * 60 * 1000L -> 90 // < 15 minutes
            ageMs < 30 * 60 * 1000L -> 80 // < 30 minutes
            ageMs < 60 * 60 * 1000L -> 70 // < 1 hour
            ageMs < 6 * 60 * 60 * 1000L -> 50 // < 6 hours
            ageMs < 24 * 60 * 60 * 1000L -> 30 // < 24 hours
            else -> 10 // > 24 hours
        }
    }

    /**
     * Get download success rate for provider
     */
    private fun getDownloadSuccessRate(providerId: String): Float {
        // TODO: Implement actual tracking
        // For now, return estimated based on provider type
        return 0.85f // 85% default success rate
    }

    /**
     * Combine individual health scores into overall score
     */
    private fun combineHealthScores(
        p2pHealth: P2PHealthData,
        availability: Float,
        providerReliability: Int,
        freshness: Int,
        successRate: Float,
    ): Int {
        val p2pWeight = 0.35f
        val availabilityWeight = 0.25f
        val reliabilityWeight = 0.20f
        val freshnessWeight = 0.10f
        val successWeight = 0.10f

        val combinedScore =
            (
                p2pHealth.overallScore * p2pWeight +
                    availability * 100 * availabilityWeight +
                    providerReliability * reliabilityWeight +
                    freshness * freshnessWeight +
                    successRate * 100 * successWeight
            ).roundToInt()

        return combinedScore.coerceIn(0, 100)
    }

    /**
     * Calculate risk assessment
     */
    private fun calculateRiskAssessment(
        healthInfo: HealthInfo,
        provider: SourceProviderInfo,
    ): RiskAssessment {
        val factors = mutableListOf<String>()
        var riskScore = 0

        // P2P risks
        healthInfo.seeders?.let { seeders ->
            when {
                seeders == 0 -> {
                    factors.add("Dead torrent (0 seeders)")
                    riskScore += 50
                }
                seeders < 5 -> {
                    factors.add("Very low seeders ($seeders)")
                    riskScore += 30
                }
                seeders < 10 -> {
                    factors.add("Low seeders ($seeders)")
                    riskScore += 15
                }
            }
        }

        // Provider risks
        when (provider.reliability) {
            SourceProviderInfo.ProviderReliability.POOR -> {
                factors.add("Poor provider reliability")
                riskScore += 25
            }
            SourceProviderInfo.ProviderReliability.UNKNOWN -> {
                factors.add("Unknown provider reliability")
                riskScore += 15
            }
            else -> {}
        }

        // Freshness risks
        healthInfo.lastChecked?.let { lastChecked ->
            val ageMs = System.currentTimeMillis() - lastChecked.time
            if (ageMs > staleDataThresholdMs) {
                factors.add("Stale health data")
                riskScore += 20
            }
        } ?: run {
            factors.add("No health data available")
            riskScore += 30
        }

        val level =
            when {
                riskScore >= 70 -> RiskLevel.HIGH
                riskScore >= 40 -> RiskLevel.MEDIUM
                riskScore >= 20 -> RiskLevel.LOW
                else -> RiskLevel.MINIMAL
            }

        return RiskAssessment(level, factors)
    }

    /**
     * Estimate download completion time
     */
    private fun estimateDownloadTime(healthInfo: HealthInfo): Int {
        val downloadSpeed = healthInfo.downloadSpeed ?: return -1 // Unknown
        if (downloadSpeed <= 0) return -1

        // Estimate based on average file sizes for different qualities
        val estimatedSizeBytes = 4_000_000_000L // 4GB average
        val timeSeconds = estimatedSizeBytes / downloadSpeed

        return (timeSeconds / 60).toInt() // Convert to minutes
    }

    /**
     * Predict source reliability based on patterns
     */
    private fun predictReliability(providerId: String): Int {
        val history = healthHistory[providerId] ?: return 50

        if (history.size < 3) return 50 // Not enough data

        val recentSnapshots = history.takeLast(10)
        val avgReliability = recentSnapshots.map { it.overallScore }.average()
        val trend = calculateTrend(recentSnapshots.map { it.overallScore.toFloat() })

        // Adjust prediction based on trend
        val prediction = avgReliability + (trend * 10)
        return prediction.roundToInt().coerceIn(0, 100)
    }

    /**
     * Calculate health trend
     */
    private fun calculateHealthTrend(providerId: String): HealthTrend {
        val history = healthHistory[providerId] ?: return HealthTrend.STABLE

        if (history.size < 3) return HealthTrend.STABLE

        val recentScores = history.takeLast(5).map { it.overallScore.toFloat() }
        val trend = calculateTrend(recentScores)

        return when {
            trend > 5 -> HealthTrend.IMPROVING
            trend < -5 -> HealthTrend.DECLINING
            else -> HealthTrend.STABLE
        }
    }

    /**
     * Calculate numerical trend from data points
     */
    private fun calculateTrend(values: List<Float>): Float {
        if (values.size < 2) return 0f

        val n = values.size
        val xSum = (1..n).sum()
        val ySum = values.sum()
        val xySum = values.withIndex().sumOf { (i: Int, y: Float) -> (i + 1) * y.toDouble() }
        val xSquareSum = (1..n).sumOf { it * it }

        val slope = (n * xySum - xSum * ySum) / (n * xSquareSum - xSum * xSum)
        return slope.toFloat()
    }

    /**
     * Check if health data is stale
     */
    private fun isDataStale(lastChecked: Date?): Boolean {
        if (lastChecked == null) return true
        val ageMs = System.currentTimeMillis() - lastChecked.time
        return ageMs > staleDataThresholdMs
    }

    /**
     * Check if health should be refreshed
     */
    private fun shouldRefreshHealth(providerId: String): Boolean {
        val cached = healthCache[providerId] ?: return true
        val ageMs = System.currentTimeMillis() - cached.timestamp
        return ageMs > cacheExpirationMs
    }

    /**
     * Calculate speed score from download/upload speeds
     */
    private fun calculateSpeedScore(
        downloadSpeed: Long,
        uploadSpeed: Long,
    ): Int {
        val totalSpeed = downloadSpeed + uploadSpeed
        return when {
            totalSpeed >= 100_000_000L -> 100 // >= 100 MB/s
            totalSpeed >= 50_000_000L -> 90 // >= 50 MB/s
            totalSpeed >= 20_000_000L -> 80 // >= 20 MB/s
            totalSpeed >= 10_000_000L -> 70 // >= 10 MB/s
            totalSpeed >= 5_000_000L -> 60 // >= 5 MB/s
            totalSpeed >= 2_000_000L -> 50 // >= 2 MB/s
            totalSpeed >= 1_000_000L -> 40 // >= 1 MB/s
            totalSpeed >= 500_000L -> 30 // >= 500 KB/s
            totalSpeed > 0L -> 20 // > 0
            else -> 0
        }
    }

    /**
     * Determine P2P health status
     */
    private fun determineP2PHealthStatus(score: Int): P2PHealthStatus {
        return when {
            score >= 90 -> P2PHealthStatus.EXCELLENT
            score >= 75 -> P2PHealthStatus.VERY_GOOD
            score >= 60 -> P2PHealthStatus.GOOD
            score >= 40 -> P2PHealthStatus.FAIR
            score >= 20 -> P2PHealthStatus.POOR
            score > 0 -> P2PHealthStatus.VERY_POOR
            else -> P2PHealthStatus.DEAD
        }
    }

    /**
     * Start background health monitoring
     */
    private fun startHealthMonitoring() {
        scope.launch {
            while (true) {
                try {
                    updateHealthData()
                    delay(healthUpdateIntervalMs)
                } catch (e: Exception) {
                    // Log error but continue monitoring
                    delay(healthUpdateIntervalMs)
                }
            }
        }
    }

    /**
     * Update health data for all cached sources
     */
    private suspend fun updateHealthData() {
        val currentTime = System.currentTimeMillis()
        val staleEntries =
            healthCache.filter { (_, cached) ->
                currentTime - cached.timestamp > cacheExpirationMs
            }

        // Remove stale entries
        staleEntries.keys.forEach { healthCache.remove(it) }

        // Update state flow
        val currentHealthData =
            healthCache.values.associate {
                it.sourceId to it.healthData
            }
        _healthUpdates.value = currentHealthData
    }

    /**
     * Store health data in cache
     */
    fun cacheHealthData(
        sourceId: String,
        healthData: HealthData,
    ) {
        healthCache[sourceId] =
            CachedHealthData(
                sourceId = sourceId,
                healthData = healthData,
                timestamp = System.currentTimeMillis(),
            )

        // Store in history for predictions
        val history = healthHistory.getOrPut(sourceId) { mutableListOf() }
        history.add(
            HealthSnapshot(
                timestamp = Date(),
                overallScore = healthData.overallScore,
                p2pScore = healthData.p2pHealth.overallScore,
                availability = healthData.availabilityPercentage,
            ),
        )

        // Keep only recent history (last 50 entries)
        if (history.size > 50) {
            history.removeAt(0)
        }

        // Update state flow
        _healthUpdates.value =
            healthCache.values.associate {
                it.sourceId to it.healthData
            }
    }

    /**
     * Get cached health data
     */
    fun getCachedHealthData(sourceId: String): HealthData? {
        return healthCache[sourceId]?.healthData
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        healthCache.clear()
        healthHistory.clear()
    }
}

/**
 * Comprehensive health data for a source
 */
@Serializable
data class HealthData(
    val sourceId: String,
    val overallScore: Int, // 0-100
    val p2pHealth: P2PHealthData,
    val availabilityPercentage: Float, // 0.0-1.0
    val providerReliability: Int, // 0-100
    val sourceAuthority: Int, // 0-100
    val freshnessIndicator: Int, // 0-100
    val downloadSuccessRate: Float, // 0.0-1.0
    val riskLevel: RiskLevel,
    val riskFactors: List<String>,
    val estimatedDownloadTimeMinutes: Int, // -1 if unknown
    val predictedReliability: Int, // 0-100
    val healthTrend: HealthTrend,
    @Contextual val lastUpdated: Date,
    val isStale: Boolean,
    val needsRefresh: Boolean,
) {
    fun getHealthBadge(): QualityBadge {
        val healthText =
            when {
                overallScore >= 90 -> "Excellent"
                overallScore >= 75 -> "Very Good"
                overallScore >= 60 -> "Good"
                overallScore >= 40 -> "Fair"
                overallScore >= 20 -> "Poor"
                else -> "Bad"
            }

        return QualityBadge(
            text = healthText,
            type = QualityBadge.Type.HEALTH,
            priority = 45,
        )
    }
}

/**
 * P2P specific health data
 */
@Serializable
data class P2PHealthData(
    val seeders: Int,
    val leechers: Int,
    val ratio: Float,
    val totalPeers: Int,
    val seederScore: Int,
    val ratioScore: Int,
    val activityScore: Int,
    val speedScore: Int,
    val overallScore: Int,
    val downloadSpeedBytesPerSec: Long,
    val uploadSpeedBytesPerSec: Long,
    val healthStatus: P2PHealthStatus,
)

/**
 * Risk assessment data
 */
@Serializable
data class RiskAssessment(
    val level: RiskLevel,
    val factors: List<String>,
)

/**
 * Risk levels
 */
@Serializable
enum class RiskLevel {
    MINIMAL,
    LOW,
    MEDIUM,
    HIGH,
}

/**
 * Health trends
 */
@Serializable
enum class HealthTrend {
    IMPROVING,
    STABLE,
    DECLINING,
}

/**
 * P2P health status
 */
@Serializable
enum class P2PHealthStatus {
    EXCELLENT,
    VERY_GOOD,
    GOOD,
    FAIR,
    POOR,
    VERY_POOR,
    DEAD,
}

/**
 * Cached health data
 */
private data class CachedHealthData(
    val sourceId: String,
    val healthData: HealthData,
    val timestamp: Long,
)

/**
 * Health snapshot for historical tracking
 */
private data class HealthSnapshot(
    val timestamp: Date,
    val overallScore: Int,
    val p2pScore: Int,
    val availability: Float,
)
