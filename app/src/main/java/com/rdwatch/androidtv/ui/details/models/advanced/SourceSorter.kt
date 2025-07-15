package com.rdwatch.androidtv.ui.details.models.advanced

import kotlin.math.ln

/**
 * Smart sorting algorithm for streaming sources
 * Prioritizes: Cached -> Quality -> Health -> Provider Reliability -> File Size
 */
class SourceSorter(
    private val configuration: SortingConfiguration = SortingConfiguration()
) {
    
    /**
     * Sort sources using the smart algorithm
     */
    fun sortSources(
        sources: List<SourceMetadata>,
        userPreferences: UserSortingPreferences = UserSortingPreferences()
    ): List<SourceMetadata> {
        return sources.sortedWith(createSmartComparator(userPreferences))
    }
    
    /**
     * Create the main smart comparator
     */
    private fun createSmartComparator(preferences: UserSortingPreferences): Comparator<SourceMetadata> {
        return compareBy<SourceMetadata> { source ->
            // Primary: Cached status (highest priority)
            if (source.availability.cached) 0 else 1
        }.thenByDescending { source ->
            // Secondary: Smart quality score
            calculateSmartQualityScore(source, preferences)
        }.thenByDescending { source ->
            // Tertiary: Health score for P2P sources
            calculateHealthScore(source)
        }.thenByDescending { source ->
            // Quaternary: Provider reliability
            source.provider.reliability.ordinal
        }.thenBy { source ->
            // Quinary: Release group reputation
            getReleaseGroupPenalty(source.release.group)
        }.thenBy { source ->
            // Senary: File size preference (closer to preferred size is better)
            calculateFileSizeScore(source, preferences)
        }.thenBy { source ->
            // Final tiebreaker: Source ID for consistent ordering
            source.id
        }
    }
    
    /**
     * Calculate smart quality score considering user preferences
     */
    private fun calculateSmartQualityScore(
        source: SourceMetadata,
        preferences: UserSortingPreferences
    ): Int {
        var score = 0
        
        // Base quality score from resolution
        score += source.quality.resolution.baseScore
        
        // Apply user resolution preference boost
        if (source.quality.resolution == preferences.preferredResolution) {
            score += configuration.resolutionPreferenceBonus
        }
        
        // HDR bonuses with user preference consideration
        val hdrMultiplier = if (preferences.preferHDR) 1.5f else 1.0f
        if (source.quality.dolbyVision) score += (30 * hdrMultiplier).toInt()
        if (source.quality.hdr10Plus) score += (25 * hdrMultiplier).toInt()
        if (source.quality.hdr10) score += (20 * hdrMultiplier).toInt()
        
        // Codec efficiency with user preference
        score += source.codec.type.efficiencyBonus
        if (preferences.preferredCodecs.contains(source.codec.type)) {
            score += configuration.codecPreferenceBonus
        }
        
        // Audio quality with user preference
        score += source.audio.format.qualityBonus
        val audioMultiplier = if (preferences.preferHighQualityAudio) 1.3f else 1.0f
        if (source.audio.dolbyAtmos) score += (15 * audioMultiplier).toInt()
        if (source.audio.dtsX) score += (10 * audioMultiplier).toInt()
        
        // Release type bonus with user preference
        score += source.release.type.qualityBonus
        if (preferences.preferredReleaseTypes.contains(source.release.type)) {
            score += configuration.releaseTypePreferenceBonus
        }
        
        // Provider type preference
        when (source.provider.type) {
            SourceProviderInfo.ProviderType.DEBRID -> 
                if (preferences.preferDebrid) score += configuration.debridPreferenceBonus
            SourceProviderInfo.ProviderType.DIRECT_STREAM -> 
                if (preferences.preferDirect) score += configuration.directPreferenceBonus
            SourceProviderInfo.ProviderType.TORRENT -> 
                if (preferences.preferP2P) score += configuration.p2pPreferenceBonus
            else -> {}
        }
        
        return score
    }
    
    /**
     * Calculate health score for P2P sources
     */
    private fun calculateHealthScore(source: SourceMetadata): Int {
        val seeders = source.health.seeders ?: return 0
        val leechers = source.health.leechers ?: 0
        
        if (seeders == 0) return 0
        
        // Base seeder score with logarithmic scaling for large numbers
        val seederScore = when {
            seeders >= 1000 -> 100 + (ln(seeders / 1000.0) * 20).toInt()
            seeders >= 100 -> 80 + (seeders - 100) / 45 // Scale from 80-100
            seeders >= 50 -> 60 + (seeders - 50) / 25  // Scale from 60-80
            seeders >= 20 -> 40 + (seeders - 20) / 15  // Scale from 40-60
            seeders >= 10 -> 20 + (seeders - 10) / 5   // Scale from 20-40
            seeders >= 5 -> 10 + (seeders - 5) * 2     // Scale from 10-20
            else -> seeders * 2                        // Scale from 0-10
        }
        
        // Ratio bonus (seeder/leecher ratio)
        val ratioBonus = if (leechers > 0) {
            val ratio = seeders.toFloat() / leechers
            when {
                ratio >= 10.0f -> 20
                ratio >= 5.0f -> 15
                ratio >= 2.0f -> 10
                ratio >= 1.0f -> 5
                else -> 0
            }
        } else {
            // No leechers is good for older/complete torrents
            if (seeders > 50) 15 else 10
        }
        
        // Availability bonus
        val availabilityBonus = source.health.availability?.let { avail ->
            (avail * 10).toInt()
        } ?: 0
        
        return seederScore + ratioBonus + availabilityBonus
    }
    
    /**
     * Get penalty for known bad release groups
     */
    private fun getReleaseGroupPenalty(releaseGroup: String?): Int {
        if (releaseGroup == null) return 0
        
        val group = releaseGroup.uppercase()
        
        return when {
            // Known good groups get negative penalty (boost)
            group in configuration.trustedReleaseGroups -> -50
            group in configuration.goodReleaseGroups -> -20
            
            // Known bad groups get positive penalty
            group in configuration.bannedReleaseGroups -> 100
            group in configuration.poorReleaseGroups -> 50
            
            else -> 0
        }
    }
    
    /**
     * Calculate file size score based on user preferences
     */
    private fun calculateFileSizeScore(
        source: SourceMetadata,
        preferences: UserSortingPreferences
    ): Float {
        val sizeInGB = source.file.sizeInBytes?.let { it / (1024.0 * 1024.0 * 1024.0) } ?: return 0f
        val preferredSize = preferences.preferredFileSizeGB
        
        return when (preferences.fileSizePreference) {
            FileSizePreference.SMALLEST -> sizeInGB.toFloat()
            FileSizePreference.LARGEST -> -sizeInGB.toFloat()
            FileSizePreference.OPTIMAL -> {
                // Prefer sizes close to the preferred size
                kotlin.math.abs(sizeInGB - preferredSize).toFloat()
            }
            FileSizePreference.NONE -> 0f
        }
    }
    
    /**
     * Group sources by release type and apply group-specific sorting
     */
    fun groupAndSortSources(
        sources: List<SourceMetadata>,
        preferences: UserSortingPreferences = UserSortingPreferences()
    ): Map<ReleaseGroup, List<SourceMetadata>> {
        val grouped = sources.groupBy { source ->
            when (source.release.type) {
                ReleaseType.BLURAY_REMUX, ReleaseType.BLURAY -> ReleaseGroup.BLURAY
                ReleaseType.WEB_DL, ReleaseType.WEBRIP -> ReleaseGroup.WEB
                ReleaseType.HDTV -> ReleaseGroup.TV
                ReleaseType.DVDRIP -> ReleaseGroup.DVD
                ReleaseType.CAM, ReleaseType.HDCAM, ReleaseType.SCREENER -> ReleaseGroup.CAM
                else -> ReleaseGroup.OTHER
            }
        }
        
        return grouped.mapValues { (_, groupSources) ->
            sortSources(groupSources, preferences)
        }.toSortedMap(compareByDescending { it.priority })
    }
    
    /**
     * Get top sources efficiently for large lists
     */
    fun getTopSources(
        sources: List<SourceMetadata>,
        count: Int,
        preferences: UserSortingPreferences = UserSortingPreferences()
    ): List<SourceMetadata> {
        if (sources.size <= count) {
            return sortSources(sources, preferences)
        }
        
        // Use partial sorting for better performance
        return sources.sortedWith(createSmartComparator(preferences))
            .take(count)
    }
    
    /**
     * Sort sources with priority to cached sources for Android TV performance
     */
    fun sortForAndroidTV(
        sources: List<SourceMetadata>,
        preferences: UserSortingPreferences = UserSortingPreferences()
    ): List<SourceMetadata> {
        // Split into cached and non-cached for optimal TV experience
        val (cached, nonCached) = sources.partition { it.availability.cached }
        
        // Sort each group separately
        val sortedCached = sortSources(cached, preferences)
        val sortedNonCached = sortSources(nonCached, preferences)
        
        // Return cached first for immediate playability
        return sortedCached + sortedNonCached
    }
}

/**
 * Release group categories for grouping
 */
enum class ReleaseGroup(val displayName: String, val priority: Int) {
    BLURAY("BluRay/Remux", 100),
    WEB("Web-DL/WebRip", 90),
    TV("HDTV", 80),
    DVD("DVD", 70),
    CAM("Cam/Screener", 60),
    OTHER("Other", 50)
}

/**
 * User preferences for sorting
 */
data class UserSortingPreferences(
    val preferredResolution: VideoResolution = VideoResolution.RESOLUTION_1080P,
    val preferredCodecs: Set<VideoCodec> = setOf(VideoCodec.HEVC, VideoCodec.AV1),
    val preferredReleaseTypes: Set<ReleaseType> = setOf(ReleaseType.WEB_DL, ReleaseType.BLURAY),
    val preferHDR: Boolean = false,
    val preferHighQualityAudio: Boolean = false,
    val preferDebrid: Boolean = true,
    val preferDirect: Boolean = false,
    val preferP2P: Boolean = false,
    val fileSizePreference: FileSizePreference = FileSizePreference.OPTIMAL,
    val preferredFileSizeGB: Double = 8.0, // For 1080p movies
    val prioritizeCached: Boolean = true
)

/**
 * File size preferences
 */
enum class FileSizePreference {
    SMALLEST,   // Prefer smallest files
    LARGEST,    // Prefer largest files
    OPTIMAL,    // Prefer files close to preferred size
    NONE        // No file size preference
}

/**
 * Configuration for sorting algorithm
 */
data class SortingConfiguration(
    val resolutionPreferenceBonus: Int = 100,
    val codecPreferenceBonus: Int = 50,
    val releaseTypePreferenceBonus: Int = 30,
    val debridPreferenceBonus: Int = 200,
    val directPreferenceBonus: Int = 150,
    val p2pPreferenceBonus: Int = 100,
    
    // Release group reputation lists
    val trustedReleaseGroups: Set<String> = setOf(
        "RARBG", "YTS", "EZTV", "ETTV", "GLHF", "SPARKS", "SHORTBREHD",
        "GECKOS", "TEPES", "ROVERS", "TOMMY", "WAYNE", "MZABI"
    ),
    
    val goodReleaseGroups: Set<String> = setOf(
        "YIFY", "1XBET", "RARBG", "EZTV", "ETTV", "TGX"
    ),
    
    val poorReleaseGroups: Set<String> = setOf(
        "KORSUB", "HC", "HDCAM", "HDTS"
    ),
    
    val bannedReleaseGroups: Set<String> = setOf(
        "YIFY-SUBS", "FAKE", "VIRUS", "SCAM"
    )
)