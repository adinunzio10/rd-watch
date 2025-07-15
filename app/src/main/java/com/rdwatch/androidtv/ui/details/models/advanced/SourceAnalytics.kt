package com.rdwatch.androidtv.ui.details.models.advanced

import kotlin.math.roundToInt

/**
 * Analytics and insights for source collections
 */
class SourceAnalytics {
    
    /**
     * Analyze a collection of sources and provide insights
     */
    fun analyzeSourceCollection(sources: List<SourceMetadata>): SourceCollectionAnalysis {
        if (sources.isEmpty()) {
            return SourceCollectionAnalysis()
        }
        
        return SourceCollectionAnalysis(
            totalSources = sources.size,
            qualityDistribution = analyzeQualityDistribution(sources),
            providerDistribution = analyzeProviderDistribution(sources),
            healthDistribution = analyzeHealthDistribution(sources),
            codecDistribution = analyzeCodecDistribution(sources),
            releaseTypeDistribution = analyzeReleaseTypeDistribution(sources),
            sizeAnalysis = analyzeSizeDistribution(sources),
            cacheAnalysis = analyzeCacheStatus(sources),
            recommendations = generateRecommendations(sources),
            qualityScore = calculateCollectionQualityScore(sources),
            diversityScore = calculateDiversityScore(sources)
        )
    }
    
    /**
     * Analyze quality distribution
     */
    private fun analyzeQualityDistribution(sources: List<SourceMetadata>): QualityDistribution {
        val resolutionCounts = sources.groupingBy { it.quality.resolution }.eachCount()
        val hdrCount = sources.count { it.quality.hasHDR() }
        val dolbyVisionCount = sources.count { it.quality.dolbyVision }
        val highQualityCount = sources.count { 
            it.quality.resolution.baseScore >= VideoResolution.RESOLUTION_1080P.baseScore 
        }
        
        return QualityDistribution(
            resolutionCounts = resolutionCounts,
            hdrPercentage = (hdrCount * 100f / sources.size),
            dolbyVisionPercentage = (dolbyVisionCount * 100f / sources.size),
            highQualityPercentage = (highQualityCount * 100f / sources.size),
            averageQualityScore = sources.map { it.getQualityScore() }.average()
        )
    }
    
    /**
     * Analyze provider distribution
     */
    private fun analyzeProviderDistribution(sources: List<SourceMetadata>): ProviderDistribution {
        val providerCounts = sources.groupingBy { it.provider.name }.eachCount()
        val typeCounts = sources.groupingBy { it.provider.type }.eachCount()
        val reliabilityDistribution = sources.groupingBy { it.provider.reliability }.eachCount()
        
        return ProviderDistribution(
            providerCounts = providerCounts,
            typeCounts = typeCounts,
            reliabilityDistribution = reliabilityDistribution,
            averageReliability = sources.map { it.provider.reliability.ordinal }.average()
        )
    }
    
    /**
     * Analyze health distribution for P2P sources
     */
    private fun analyzeHealthDistribution(sources: List<SourceMetadata>): HealthDistribution {
        val p2pSources = sources.filter { it.health.seeders != null }
        
        if (p2pSources.isEmpty()) {
            return HealthDistribution()
        }
        
        val seeders = p2pSources.mapNotNull { it.health.seeders }
        val leechers = p2pSources.mapNotNull { it.health.leechers }
        
        val healthCategories = p2pSources.groupingBy { source ->
            when (source.health.seeders ?: 0) {
                in 0..0 -> "Dead"
                in 1..5 -> "Poor"
                in 6..20 -> "Fair"
                in 21..100 -> "Good"
                else -> "Excellent"
            }
        }.eachCount()
        
        return HealthDistribution(
            p2pSourceCount = p2pSources.size,
            averageSeeders = seeders.average(),
            averageLeechers = leechers.average(),
            maxSeeders = seeders.maxOrNull() ?: 0,
            healthCategories = healthCategories,
            healthySourcePercentage = p2pSources.count { (it.health.seeders ?: 0) > 20 } * 100f / p2pSources.size
        )
    }
    
    /**
     * Analyze codec distribution
     */
    private fun analyzeCodecDistribution(sources: List<SourceMetadata>): CodecDistribution {
        val codecCounts = sources.groupingBy { it.codec.type }.eachCount()
        val modernCodecCount = sources.count { 
            it.codec.type in setOf(VideoCodec.HEVC, VideoCodec.AV1, VideoCodec.VP9)
        }
        
        return CodecDistribution(
            codecCounts = codecCounts,
            modernCodecPercentage = modernCodecCount * 100f / sources.size,
            averageEfficiencyScore = sources.map { it.codec.type.efficiencyBonus }.average()
        )
    }
    
    /**
     * Analyze release type distribution
     */
    private fun analyzeReleaseTypeDistribution(sources: List<SourceMetadata>): ReleaseTypeDistribution {
        val typeCounts = sources.groupingBy { it.release.type }.eachCount()
        val highQualityReleases = sources.count {
            it.release.type in setOf(ReleaseType.BLURAY_REMUX, ReleaseType.BLURAY, ReleaseType.WEB_DL)
        }
        
        return ReleaseTypeDistribution(
            typeCounts = typeCounts,
            highQualityPercentage = highQualityReleases * 100f / sources.size,
            averageReleaseScore = sources.map { it.release.type.qualityBonus }.average()
        )
    }
    
    /**
     * Analyze file size distribution
     */
    private fun analyzeSizeDistribution(sources: List<SourceMetadata>): SizeAnalysis {
        val sourcesWithSize = sources.filter { it.file.sizeInBytes != null }
        
        if (sourcesWithSize.isEmpty()) {
            return SizeAnalysis()
        }
        
        val sizesInGB = sourcesWithSize.map { it.file.sizeInBytes!! / (1024.0 * 1024.0 * 1024.0) }
        
        val sizeCategories = sourcesWithSize.groupingBy { source ->
            val sizeGB = source.file.sizeInBytes!! / (1024.0 * 1024.0 * 1024.0)
            when {
                sizeGB < 1.0 -> "< 1GB"
                sizeGB < 5.0 -> "1-5GB"
                sizeGB < 15.0 -> "5-15GB"
                sizeGB < 30.0 -> "15-30GB"
                else -> "30GB+"
            }
        }.eachCount()
        
        return SizeAnalysis(
            sourcesWithSize = sourcesWithSize.size,
            averageSizeGB = sizesInGB.average(),
            medianSizeGB = sizesInGB.sorted().let { sorted ->
                if (sorted.size % 2 == 0) {
                    (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
                } else {
                    sorted[sorted.size / 2]
                }
            },
            minSizeGB = sizesInGB.minOrNull() ?: 0.0,
            maxSizeGB = sizesInGB.maxOrNull() ?: 0.0,
            sizeCategories = sizeCategories
        )
    }
    
    /**
     * Analyze cache status for debrid services
     */
    private fun analyzeCacheStatus(sources: List<SourceMetadata>): CacheAnalysis {
        val debridSources = sources.filter { 
            it.provider.type == SourceProviderInfo.ProviderType.DEBRID 
        }
        
        if (debridSources.isEmpty()) {
            return CacheAnalysis()
        }
        
        val cachedCount = debridSources.count { it.availability.cached }
        val debridServices = debridSources.groupingBy { it.availability.debridService }.eachCount()
        
        return CacheAnalysis(
            debridSourceCount = debridSources.size,
            cachedSourceCount = cachedCount,
            cachePercentage = cachedCount * 100f / debridSources.size,
            debridServices = debridServices.filterKeys { it != null } as Map<String, Int>
        )
    }
    
    /**
     * Generate recommendations based on source analysis
     */
    private fun generateRecommendations(sources: List<SourceMetadata>): List<SourceRecommendation> {
        val recommendations = mutableListOf<SourceRecommendation>()
        
        // Check for quality issues
        val lowQualityCount = sources.count { 
            it.quality.resolution.baseScore < VideoResolution.RESOLUTION_720P.baseScore 
        }
        if (lowQualityCount > sources.size * 0.5) {
            recommendations.add(
                SourceRecommendation(
                    type = RecommendationType.QUALITY_IMPROVEMENT,
                    message = "Consider filtering out low-quality sources (${lowQualityCount} sources below 720p)",
                    priority = RecommendationPriority.MEDIUM
                )
            )
        }
        
        // Check for P2P health
        val deadTorrents = sources.count { (it.health.seeders ?: 1) == 0 }
        if (deadTorrents > 0) {
            recommendations.add(
                SourceRecommendation(
                    type = RecommendationType.HEALTH_WARNING,
                    message = "Found ${deadTorrents} dead torrents that should be filtered out",
                    priority = RecommendationPriority.HIGH
                )
            )
        }
        
        // Check for codec diversity
        val modernCodecs = sources.count { 
            it.codec.type in setOf(VideoCodec.HEVC, VideoCodec.AV1) 
        }
        if (modernCodecs < sources.size * 0.3) {
            recommendations.add(
                SourceRecommendation(
                    type = RecommendationType.CODEC_DIVERSITY,
                    message = "Low modern codec availability (${modernCodecs}/${sources.size}). Consider HEVC/AV1 sources",
                    priority = RecommendationPriority.LOW
                )
            )
        }
        
        // Check for cached availability
        val cachedSources = sources.count { it.availability.cached }
        if (cachedSources > 0) {
            recommendations.add(
                SourceRecommendation(
                    type = RecommendationType.PERFORMANCE_BOOST,
                    message = "${cachedSources} cached sources available for instant streaming",
                    priority = RecommendationPriority.HIGH
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Calculate overall quality score for the collection
     */
    private fun calculateCollectionQualityScore(sources: List<SourceMetadata>): Int {
        if (sources.isEmpty()) return 0
        
        val qualityScores = sources.map { it.getQualityScore() }
        val averageScore = qualityScores.average()
        val consistency = 1.0 - (qualityScores.map { (it - averageScore) * (it - averageScore) }.average() / (averageScore * averageScore))
        
        return (averageScore * consistency).roundToInt()
    }
    
    /**
     * Calculate diversity score (how varied the sources are)
     */
    private fun calculateDiversityScore(sources: List<SourceMetadata>): Int {
        if (sources.size < 2) return 0
        
        val uniqueProviders = sources.map { it.provider.id }.distinct().size
        val uniqueQualities = sources.map { it.quality.resolution }.distinct().size
        val uniqueCodecs = sources.map { it.codec.type }.distinct().size
        val uniqueReleaseTypes = sources.map { it.release.type }.distinct().size
        
        val providerDiversity = uniqueProviders.toFloat() / sources.size
        val qualityDiversity = uniqueQualities.toFloat() / VideoResolution.entries.size
        val codecDiversity = uniqueCodecs.toFloat() / VideoCodec.entries.size
        val releaseDiversity = uniqueReleaseTypes.toFloat() / ReleaseType.entries.size
        
        return ((providerDiversity + qualityDiversity + codecDiversity + releaseDiversity) * 25).roundToInt()
    }
}

/**
 * Complete analysis of a source collection
 */
data class SourceCollectionAnalysis(
    val totalSources: Int = 0,
    val qualityDistribution: QualityDistribution = QualityDistribution(),
    val providerDistribution: ProviderDistribution = ProviderDistribution(),
    val healthDistribution: HealthDistribution = HealthDistribution(),
    val codecDistribution: CodecDistribution = CodecDistribution(),
    val releaseTypeDistribution: ReleaseTypeDistribution = ReleaseTypeDistribution(),
    val sizeAnalysis: SizeAnalysis = SizeAnalysis(),
    val cacheAnalysis: CacheAnalysis = CacheAnalysis(),
    val recommendations: List<SourceRecommendation> = emptyList(),
    val qualityScore: Int = 0,
    val diversityScore: Int = 0
)

data class QualityDistribution(
    val resolutionCounts: Map<VideoResolution, Int> = emptyMap(),
    val hdrPercentage: Float = 0f,
    val dolbyVisionPercentage: Float = 0f,
    val highQualityPercentage: Float = 0f,
    val averageQualityScore: Double = 0.0
)

data class ProviderDistribution(
    val providerCounts: Map<String, Int> = emptyMap(),
    val typeCounts: Map<SourceProviderInfo.ProviderType, Int> = emptyMap(),
    val reliabilityDistribution: Map<SourceProviderInfo.ProviderReliability, Int> = emptyMap(),
    val averageReliability: Double = 0.0
)

data class HealthDistribution(
    val p2pSourceCount: Int = 0,
    val averageSeeders: Double = 0.0,
    val averageLeechers: Double = 0.0,
    val maxSeeders: Int = 0,
    val healthCategories: Map<String, Int> = emptyMap(),
    val healthySourcePercentage: Float = 0f
)

data class CodecDistribution(
    val codecCounts: Map<VideoCodec, Int> = emptyMap(),
    val modernCodecPercentage: Float = 0f,
    val averageEfficiencyScore: Double = 0.0
)

data class ReleaseTypeDistribution(
    val typeCounts: Map<ReleaseType, Int> = emptyMap(),
    val highQualityPercentage: Float = 0f,
    val averageReleaseScore: Double = 0.0
)

data class SizeAnalysis(
    val sourcesWithSize: Int = 0,
    val averageSizeGB: Double = 0.0,
    val medianSizeGB: Double = 0.0,
    val minSizeGB: Double = 0.0,
    val maxSizeGB: Double = 0.0,
    val sizeCategories: Map<String, Int> = emptyMap()
)

data class CacheAnalysis(
    val debridSourceCount: Int = 0,
    val cachedSourceCount: Int = 0,
    val cachePercentage: Float = 0f,
    val debridServices: Map<String, Int> = emptyMap()
)

data class SourceRecommendation(
    val type: RecommendationType,
    val message: String,
    val priority: RecommendationPriority
)

enum class RecommendationType {
    QUALITY_IMPROVEMENT,
    HEALTH_WARNING,
    CODEC_DIVERSITY,
    PERFORMANCE_BOOST,
    SIZE_OPTIMIZATION,
    PROVIDER_DIVERSITY
}

enum class RecommendationPriority {
    HIGH, MEDIUM, LOW
}