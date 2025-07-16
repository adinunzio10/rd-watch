package com.rdwatch.androidtv.ui.details.models.advanced

import kotlin.math.*
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced health prediction algorithms
 * Provides reliability prediction, download time estimation, and risk assessment
 */
class HealthPredictor {
    
    // Historical data storage
    private val providerHistory = ConcurrentHashMap<String, ProviderHistoryData>()
    private val globalStatistics = GlobalStatistics()
    
    /**
     * Predict source reliability based on patterns and historical data
     */
    fun predictReliability(
        sourceMetadata: SourceMetadata,
        healthData: HealthData,
        historicalData: List<HealthData> = emptyList()
    ): ReliabilityPrediction {
        
        val baseReliability = calculateBaseReliability(sourceMetadata, healthData)
        val trendAdjustment = calculateTrendAdjustment(historicalData)
        val providerFactor = getProviderReliabilityFactor(sourceMetadata.provider.id)
        val seasonalFactor = calculateSeasonalFactor(sourceMetadata)
        val sizeFactor = calculateSizeFactor(sourceMetadata.file.sizeInBytes)
        
        val predictedScore = combineReliabilityFactors(
            baseReliability,
            trendAdjustment,
            providerFactor,
            seasonalFactor,
            sizeFactor
        )
        
        val confidence = calculatePredictionConfidence(historicalData, sourceMetadata)
        val factors = buildReliabilityFactors(
            baseReliability, trendAdjustment, providerFactor, 
            seasonalFactor, sizeFactor
        )
        
        return ReliabilityPrediction(
            predictedScore = predictedScore.coerceIn(0, 100),
            confidence = confidence,
            timeframe = PredictionTimeframe.NEXT_24_HOURS,
            factors = factors,
            riskLevel = calculatePredictedRiskLevel(predictedScore),
            lastUpdated = Date()
        )
    }
    
    /**
     * Estimate download completion time with accuracy assessment
     */
    fun estimateDownloadTime(
        sourceMetadata: SourceMetadata,
        healthData: HealthData,
        userConnectionSpeedMbps: Int? = null
    ): DownloadTimeEstimation {
        
        val fileSize = sourceMetadata.file.sizeInBytes ?: return createUnknownEstimation()
        
        // Multiple estimation methods for better accuracy
        val p2pEstimation = estimateP2PDownloadTime(fileSize, healthData)
        val networkEstimation = estimateNetworkDownloadTime(fileSize, userConnectionSpeedMbps)
        val historicalEstimation = estimateFromHistoricalData(sourceMetadata.provider.id, fileSize)
        
        // Combine estimations with weights
        val combinedEstimation = combineTimeEstimations(
            p2pEstimation, networkEstimation, historicalEstimation
        )
        
        val confidence = calculateTimeEstimationConfidence(
            sourceMetadata, healthData, listOfNotNull(p2pEstimation, networkEstimation, historicalEstimation)
        )
        
        return DownloadTimeEstimation(
            estimatedMinutes = combinedEstimation.minutes,
            rangeMinutes = combinedEstimation.range,
            confidence = confidence,
            method = combinedEstimation.method,
            factors = combinedEstimation.factors,
            assumptions = combinedEstimation.assumptions,
            lastUpdated = Date()
        )
    }
    
    /**
     * Assess download risk factors and success probability
     */
    fun assessDownloadRisk(
        sourceMetadata: SourceMetadata,
        healthData: HealthData,
        userProfile: UserProfile? = null
    ): DownloadRiskAssessment {
        
        val riskFactors = mutableListOf<RiskFactor>()
        var totalRiskScore = 0
        
        // P2P specific risks
        if (sourceMetadata.provider.type == SourceProviderInfo.ProviderType.TORRENT) {
            val p2pRisks = assessP2PRisks(healthData.p2pHealth)
            riskFactors.addAll(p2pRisks.factors)
            totalRiskScore += p2pRisks.score
        }
        
        // Provider reliability risks
        val providerRisks = assessProviderRisks(sourceMetadata.provider)
        riskFactors.addAll(providerRisks.factors)
        totalRiskScore += providerRisks.score
        
        // File size risks
        val sizeRisks = assessFileSizeRisks(sourceMetadata.file.sizeInBytes)
        riskFactors.addAll(sizeRisks.factors)
        totalRiskScore += sizeRisks.score
        
        // Quality/complexity risks
        val qualityRisks = assessQualityRisks(sourceMetadata.quality, sourceMetadata.codec)
        riskFactors.addAll(qualityRisks.factors)
        totalRiskScore += qualityRisks.score
        
        // User-specific risks
        userProfile?.let { profile ->
            val userRisks = assessUserSpecificRisks(sourceMetadata, profile)
            riskFactors.addAll(userRisks.factors)
            totalRiskScore += userRisks.score
        }
        
        val successProbability = calculateSuccessProbability(totalRiskScore, riskFactors)
        val riskLevel = determineOverallRiskLevel(totalRiskScore)
        
        return DownloadRiskAssessment(
            overallRiskLevel = riskLevel,
            riskScore = totalRiskScore.coerceIn(0, 100),
            successProbability = successProbability,
            riskFactors = riskFactors,
            mitigationSuggestions = generateMitigationSuggestions(riskFactors),
            lastUpdated = Date()
        )
    }
    
    /**
     * Update historical data for improved predictions
     */
    fun updateHistoricalData(
        providerId: String,
        actualDownloadTime: Long,
        actualSuccess: Boolean,
        sourceMetadata: SourceMetadata
    ) {
        val historyData = providerHistory.getOrPut(providerId) { ProviderHistoryData() }
        
        historyData.downloads.add(DownloadRecord(
            timestamp = Date(),
            downloadTimeMinutes = (actualDownloadTime / 60_000L).toInt(),
            success = actualSuccess,
            fileSizeBytes = sourceMetadata.file.sizeInBytes ?: 0L,
            quality = sourceMetadata.quality.resolution,
            releaseType = sourceMetadata.release.type
        ))
        
        // Keep only recent records (last 1000)
        if (historyData.downloads.size > 1000) {
            historyData.downloads.removeAt(0)
        }
        
        // Update global statistics
        globalStatistics.updateWithDownload(actualDownloadTime, actualSuccess, sourceMetadata)
    }
    
    // MARK: - Private Implementation Methods
    
    private fun calculateBaseReliability(sourceMetadata: SourceMetadata, healthData: HealthData): Int {
        var reliability = healthData.overallScore
        
        // Adjust based on provider type
        reliability += when (sourceMetadata.provider.type) {
            SourceProviderInfo.ProviderType.DEBRID -> 15
            SourceProviderInfo.ProviderType.DIRECT_STREAM -> 10
            SourceProviderInfo.ProviderType.TORRENT -> 0
            else -> -5
        }
        
        // Adjust based on release quality
        reliability += when (sourceMetadata.release.type) {
            ReleaseType.BLURAY_REMUX, ReleaseType.BLURAY -> 10
            ReleaseType.WEB_DL -> 8
            ReleaseType.WEBRIP -> 5
            ReleaseType.HDTV -> 3
            else -> 0
        }
        
        return reliability.coerceIn(0, 100)
    }
    
    private fun calculateTrendAdjustment(historicalData: List<HealthData>): Int {
        if (historicalData.size < 3) return 0
        
        val recentScores = historicalData.takeLast(5).map { it.overallScore }
        val trend = calculateLinearTrend(recentScores.map { it.toFloat() })
        
        return (trend * 2).roundToInt().coerceIn(-20, 20)
    }
    
    private fun getProviderReliabilityFactor(providerId: String): Float {
        val history = providerHistory[providerId] ?: return 1.0f
        
        if (history.downloads.size < 5) return 1.0f
        
        val successRate = history.downloads.count { it.success }.toFloat() / history.downloads.size
        return when {
            successRate >= 0.95f -> 1.2f
            successRate >= 0.85f -> 1.1f
            successRate >= 0.75f -> 1.0f
            successRate >= 0.60f -> 0.9f
            else -> 0.8f
        }
    }
    
    private fun calculateSeasonalFactor(sourceMetadata: SourceMetadata): Float {
        // Newer releases might be more popular and thus more reliable
        val releaseYear = sourceMetadata.release.year ?: return 1.0f
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val age = currentYear - releaseYear
        
        return when {
            age <= 1 -> 1.1f // Very recent
            age <= 3 -> 1.05f // Recent
            age <= 10 -> 1.0f // Moderate
            else -> 0.95f // Older content
        }
    }
    
    private fun calculateSizeFactor(sizeBytes: Long?): Float {
        if (sizeBytes == null) return 1.0f
        
        val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
        return when {
            sizeGB > 100 -> 0.8f // Very large files are riskier
            sizeGB > 50 -> 0.9f // Large files
            sizeGB > 10 -> 1.0f // Normal size
            sizeGB > 1 -> 1.05f // Small files often more reliable
            else -> 0.9f // Very small files might be low quality
        }
    }
    
    private fun combineReliabilityFactors(
        base: Int, trend: Int, provider: Float, seasonal: Float, size: Float
    ): Int {
        val adjustedBase = base + trend
        val factorMultiplier = provider * seasonal * size
        return (adjustedBase * factorMultiplier).roundToInt()
    }
    
    private fun calculatePredictionConfidence(
        historicalData: List<HealthData>,
        sourceMetadata: SourceMetadata
    ): Float {
        var confidence = 50.0f // Base confidence
        
        // More historical data = higher confidence
        confidence += min(historicalData.size * 5.0f, 30.0f)
        
        // Provider reliability boosts confidence
        when (sourceMetadata.provider.reliability) {
            SourceProviderInfo.ProviderReliability.EXCELLENT -> confidence += 20.0f
            SourceProviderInfo.ProviderReliability.GOOD -> confidence += 15.0f
            SourceProviderInfo.ProviderReliability.FAIR -> confidence += 5.0f
            SourceProviderInfo.ProviderReliability.POOR -> confidence -= 10.0f
            SourceProviderInfo.ProviderReliability.UNKNOWN -> confidence -= 5.0f
        }
        
        return confidence.coerceIn(0.0f, 100.0f)
    }
    
    private fun estimateP2PDownloadTime(fileSize: Long, healthData: HealthData): TimeEstimation? {
        val downloadSpeed = healthData.p2pHealth.downloadSpeedBytesPerSec
        if (downloadSpeed <= 0) return null
        
        val estimatedSeconds = fileSize / downloadSpeed
        val estimatedMinutes = (estimatedSeconds / 60).toInt()
        
        // Add variance based on seeder count
        val variance = when {
            healthData.p2pHealth.seeders > 100 -> 0.2f // ±20%
            healthData.p2pHealth.seeders > 50 -> 0.3f // ±30%
            healthData.p2pHealth.seeders > 10 -> 0.5f // ±50%
            else -> 1.0f // ±100%
        }
        
        val rangeMinutes = (estimatedMinutes * variance).toInt()
        
        return TimeEstimation(
            minutes = estimatedMinutes,
            range = rangeMinutes,
            method = "P2P Speed Analysis",
            factors = listOf("Current download speed: ${downloadSpeed / 1_000_000} MB/s", 
                           "Seeders: ${healthData.p2pHealth.seeders}"),
            assumptions = listOf("Constant download speed", "No connection interruptions")
        )
    }
    
    private fun estimateNetworkDownloadTime(fileSize: Long, userSpeedMbps: Int?): TimeEstimation? {
        val speedMbps = userSpeedMbps ?: return null
        val speedBytesPerSec = (speedMbps * 1_000_000 / 8) * 0.8f // 80% efficiency
        
        val estimatedSeconds = fileSize / speedBytesPerSec
        val estimatedMinutes = (estimatedSeconds / 60).toInt()
        
        return TimeEstimation(
            minutes = estimatedMinutes,
            range = (estimatedMinutes * 0.3f).toInt(), // ±30%
            method = "Network Speed Estimation",
            factors = listOf("User connection: ${userSpeedMbps} Mbps", "80% efficiency assumed"),
            assumptions = listOf("Stable network connection", "No throttling")
        )
    }
    
    private fun estimateFromHistoricalData(providerId: String, fileSize: Long): TimeEstimation? {
        val history = providerHistory[providerId] ?: return null
        val similarDownloads = history.downloads.filter { 
            abs(it.fileSizeBytes - fileSize) < fileSize * 0.5 // Within 50% of file size
        }
        
        if (similarDownloads.size < 3) return null
        
        val avgTime = similarDownloads.map { it.downloadTimeMinutes }.average().toInt()
        val variance = calculateVariance(similarDownloads.map { it.downloadTimeMinutes.toFloat() })
        
        return TimeEstimation(
            minutes = avgTime,
            range = sqrt(variance).toInt(),
            method = "Historical Data Analysis",
            factors = listOf("Based on ${similarDownloads.size} similar downloads"),
            assumptions = listOf("Similar network conditions", "Provider performance consistency")
        )
    }
    
    private fun combineTimeEstimations(vararg estimations: TimeEstimation?): TimeEstimation {
        val validEstimations = estimations.filterNotNull()
        
        if (validEstimations.isEmpty()) {
            return TimeEstimation(
                minutes = -1,
                range = 0,
                method = "Unable to estimate",
                factors = listOf("Insufficient data"),
                assumptions = emptyList()
            )
        }
        
        val weightedAvg = validEstimations.map { it.minutes }.average().toInt()
        val maxRange = validEstimations.maxOf { it.range }
        val combinedFactors = validEstimations.flatMap { it.factors }
        val combinedAssumptions = validEstimations.flatMap { it.assumptions }.distinct()
        
        return TimeEstimation(
            minutes = weightedAvg,
            range = maxRange,
            method = "Combined Analysis",
            factors = combinedFactors,
            assumptions = combinedAssumptions
        )
    }
    
    private fun calculateTimeEstimationConfidence(
        sourceMetadata: SourceMetadata,
        healthData: HealthData,
        estimations: List<TimeEstimation>
    ): Float {
        var confidence = 30.0f // Base confidence
        
        // More estimation methods = higher confidence
        confidence += estimations.size * 15.0f
        
        // Recent health data boosts confidence
        if (!healthData.isStale) confidence += 20.0f
        
        // Provider type affects confidence
        confidence += when (sourceMetadata.provider.type) {
            SourceProviderInfo.ProviderType.DEBRID -> 20.0f
            SourceProviderInfo.ProviderType.DIRECT_STREAM -> 15.0f
            SourceProviderInfo.ProviderType.TORRENT -> 5.0f
            else -> 0.0f
        }
        
        return confidence.coerceIn(0.0f, 100.0f)
    }
    
    private fun createUnknownEstimation(): DownloadTimeEstimation {
        return DownloadTimeEstimation(
            estimatedMinutes = -1,
            rangeMinutes = 0,
            confidence = 0.0f,
            method = "Unknown file size",
            factors = listOf("File size not available"),
            assumptions = emptyList(),
            lastUpdated = Date()
        )
    }
    
    // Risk assessment helpers
    private fun assessP2PRisks(p2pHealth: P2PHealthData): RiskAssessmentResult {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        if (p2pHealth.seeders == 0) {
            factors.add(RiskFactor("Dead torrent", RiskSeverity.CRITICAL, 40))
            score += 40
        } else if (p2pHealth.seeders < 5) {
            factors.add(RiskFactor("Very low seeders", RiskSeverity.HIGH, 25))
            score += 25
        } else if (p2pHealth.seeders < 10) {
            factors.add(RiskFactor("Low seeders", RiskSeverity.MEDIUM, 15))
            score += 15
        }
        
        if (p2pHealth.ratio < 0.5f) {
            factors.add(RiskFactor("Poor seeder/leecher ratio", RiskSeverity.MEDIUM, 10))
            score += 10
        }
        
        return RiskAssessmentResult(factors, score)
    }
    
    private fun assessProviderRisks(provider: SourceProviderInfo): RiskAssessmentResult {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        when (provider.reliability) {
            SourceProviderInfo.ProviderReliability.POOR -> {
                factors.add(RiskFactor("Poor provider reliability", RiskSeverity.HIGH, 25))
                score += 25
            }
            SourceProviderInfo.ProviderReliability.UNKNOWN -> {
                factors.add(RiskFactor("Unknown provider reliability", RiskSeverity.MEDIUM, 15))
                score += 15
            }
            SourceProviderInfo.ProviderReliability.FAIR -> {
                factors.add(RiskFactor("Fair provider reliability", RiskSeverity.LOW, 5))
                score += 5
            }
            else -> {} // Good/Excellent providers add no risk
        }
        
        return RiskAssessmentResult(factors, score)
    }
    
    private fun assessFileSizeRisks(sizeBytes: Long?): RiskAssessmentResult {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        if (sizeBytes != null) {
            val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
            when {
                sizeGB > 100 -> {
                    factors.add(RiskFactor("Very large file size", RiskSeverity.MEDIUM, 15))
                    score += 15
                }
                sizeGB > 50 -> {
                    factors.add(RiskFactor("Large file size", RiskSeverity.LOW, 8))
                    score += 8
                }
                sizeGB < 0.5 -> {
                    factors.add(RiskFactor("Suspiciously small file", RiskSeverity.MEDIUM, 12))
                    score += 12
                }
            }
        }
        
        return RiskAssessmentResult(factors, score)
    }
    
    private fun assessQualityRisks(quality: QualityInfo, codec: CodecInfo): RiskAssessmentResult {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        // Higher quality files might be harder to find reliable sources
        if (quality.resolution == VideoResolution.RESOLUTION_4K || quality.resolution == VideoResolution.RESOLUTION_8K) {
            factors.add(RiskFactor("High resolution may have fewer sources", RiskSeverity.LOW, 5))
            score += 5
        }
        
        // Newer codecs might have compatibility issues
        if (codec.type == VideoCodec.AV1) {
            factors.add(RiskFactor("AV1 codec may have compatibility issues", RiskSeverity.LOW, 3))
            score += 3
        }
        
        return RiskAssessmentResult(factors, score)
    }
    
    private fun assessUserSpecificRisks(sourceMetadata: SourceMetadata, userProfile: UserProfile): RiskAssessmentResult {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        // Check if user's connection can handle the file size
        val fileSize = sourceMetadata.file.sizeInBytes
        if (fileSize != null && userProfile.connectionSpeedMbps != null) {
            val estimatedHours = fileSize / (userProfile.connectionSpeedMbps * 1_000_000 / 8) / 3600
            if (estimatedHours > 12) {
                factors.add(RiskFactor("Very long download time for user's connection", RiskSeverity.MEDIUM, 10))
                score += 10
            }
        }
        
        return RiskAssessmentResult(factors, score)
    }
    
    private fun calculateSuccessProbability(riskScore: Int, riskFactors: List<RiskFactor>): Float {
        val baseSuccess = 90.0f // Start with 90% base success rate
        val riskPenalty = riskScore * 0.8f // Each risk point reduces success by 0.8%
        
        return (baseSuccess - riskPenalty).coerceIn(10.0f, 100.0f)
    }
    
    private fun determineOverallRiskLevel(riskScore: Int): RiskLevel {
        return when {
            riskScore >= 60 -> RiskLevel.HIGH
            riskScore >= 35 -> RiskLevel.MEDIUM
            riskScore >= 15 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }
    
    private fun generateMitigationSuggestions(riskFactors: List<RiskFactor>): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (riskFactors.any { it.description.contains("seeder", ignoreCase = true) }) {
            suggestions.add("Consider waiting for more seeders or choosing a different source")
        }
        
        if (riskFactors.any { it.description.contains("provider", ignoreCase = true) }) {
            suggestions.add("Look for sources from more reliable providers")
        }
        
        if (riskFactors.any { it.description.contains("large", ignoreCase = true) }) {
            suggestions.add("Ensure stable internet connection for large downloads")
            suggestions.add("Consider using a download manager with resume capability")
        }
        
        if (riskFactors.any { it.description.contains("connection", ignoreCase = true) }) {
            suggestions.add("Download during off-peak hours for better performance")
        }
        
        return suggestions
    }
    
    // Utility functions
    private fun calculateLinearTrend(values: List<Float>): Float {
        if (values.size < 2) return 0f
        
        val n = values.size
        val xSum = (1..n).sum()
        val ySum = values.sum()
        val xySum = values.withIndex().sumOf { (i, y) -> (i + 1) * y.toDouble() }
        val xSquareSum = (1..n).sumOf { it * it }
        
        return ((n * xySum - xSum * ySum) / (n * xSquareSum - xSum * xSum)).toFloat()
    }
    
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        return values.map { (it - mean).pow(2) }.average().toFloat()
    }
    
    private fun calculatePredictedRiskLevel(predictedScore: Int): RiskLevel {
        return when {
            predictedScore >= 80 -> RiskLevel.MINIMAL
            predictedScore >= 60 -> RiskLevel.LOW
            predictedScore >= 40 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }
    }
    
    private fun buildReliabilityFactors(
        base: Int, trend: Int, provider: Float, seasonal: Float, size: Float
    ): List<String> {
        return listOf(
            "Base reliability: $base",
            "Trend adjustment: ${if (trend >= 0) "+" else ""}$trend",
            "Provider factor: ${(provider * 100).roundToInt()}%",
            "Seasonal factor: ${(seasonal * 100).roundToInt()}%",
            "Size factor: ${(size * 100).roundToInt()}%"
        )
    }
}

// Data classes for predictions
data class ReliabilityPrediction(
    val predictedScore: Int, // 0-100
    val confidence: Float, // 0-100%
    val timeframe: PredictionTimeframe,
    val factors: List<String>,
    val riskLevel: RiskLevel,
    val lastUpdated: Date
)

data class DownloadTimeEstimation(
    val estimatedMinutes: Int, // -1 if unknown
    val rangeMinutes: Int, // ± range
    val confidence: Float, // 0-100%
    val method: String,
    val factors: List<String>,
    val assumptions: List<String>,
    val lastUpdated: Date
)

data class DownloadRiskAssessment(
    val overallRiskLevel: RiskLevel,
    val riskScore: Int, // 0-100
    val successProbability: Float, // 0-100%
    val riskFactors: List<RiskFactor>,
    val mitigationSuggestions: List<String>,
    val lastUpdated: Date
)

data class RiskFactor(
    val description: String,
    val severity: RiskSeverity,
    val impact: Int // 0-100
)

enum class RiskSeverity { LOW, MEDIUM, HIGH, CRITICAL }
enum class PredictionTimeframe { NEXT_HOUR, NEXT_6_HOURS, NEXT_24_HOURS, NEXT_WEEK }

// Helper data classes
private data class TimeEstimation(
    val minutes: Int,
    val range: Int,
    val method: String,
    val factors: List<String>,
    val assumptions: List<String>
)

private data class RiskAssessmentResult(
    val factors: List<RiskFactor>,
    val score: Int
)

data class UserProfile(
    val connectionSpeedMbps: Int?,
    val preferredQuality: VideoResolution?,
    val storageSpaceGB: Int?
)

// Historical data structures
private data class ProviderHistoryData(
    val downloads: MutableList<DownloadRecord> = mutableListOf()
)

private data class DownloadRecord(
    val timestamp: Date,
    val downloadTimeMinutes: Int,
    val success: Boolean,
    val fileSizeBytes: Long,
    val quality: VideoResolution,
    val releaseType: ReleaseType
)

private class GlobalStatistics {
    fun updateWithDownload(downloadTime: Long, success: Boolean, sourceMetadata: SourceMetadata) {
        // Implementation for global statistics tracking
    }
}