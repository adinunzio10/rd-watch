package com.rdwatch.androidtv.ui.details.models

/**
 * Represents a streaming provider/service
 */
data class SourceProvider(
    val id: String,
    val name: String,
    val displayName: String,
    val logoUrl: String?,
    val logoResource: Int? = null, // For bundled provider logos
    val isAvailable: Boolean = true,
    val requiresSubscription: Boolean = false,
    val subscriptionTier: String? = null,
    val color: String? = null, // Provider brand color
    val priority: Int = 0 // Higher priority sources appear first
) {
    companion object {
        // Common streaming providers
        val NETFLIX = SourceProvider(
            id = "netflix",
            name = "Netflix",
            displayName = "Netflix",
            logoUrl = null,
            logoResource = null, // TODO: Add logo resource when available
            requiresSubscription = true,
            color = "#E50914",
            priority = 100
        )
        
        val AMAZON_PRIME = SourceProvider(
            id = "amazon_prime",
            name = "Amazon Prime Video",
            displayName = "Prime Video",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = true,
            color = "#00A8E1",
            priority = 90
        )
        
        val DISNEY_PLUS = SourceProvider(
            id = "disney_plus",
            name = "Disney+",
            displayName = "Disney+",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = true,
            color = "#113CCF",
            priority = 85
        )
        
        val HULU = SourceProvider(
            id = "hulu",
            name = "Hulu",
            displayName = "Hulu",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = true,
            color = "#1CE783",
            priority = 80
        )
        
        val HBO_MAX = SourceProvider(
            id = "hbo_max",
            name = "HBO Max",
            displayName = "HBO Max",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = true,
            color = "#8B5CF6",
            priority = 85
        )
        
        val APPLE_TV = SourceProvider(
            id = "apple_tv",
            name = "Apple TV+",
            displayName = "Apple TV+",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = true,
            color = "#000000",
            priority = 75
        )
        
        val PARAMOUNT_PLUS = SourceProvider(
            id = "paramount_plus",
            name = "Paramount+",
            displayName = "Paramount+",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = true,
            color = "#0052CC",
            priority = 70
        )
        
        val PEACOCK = SourceProvider(
            id = "peacock",
            name = "Peacock",
            displayName = "Peacock",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = true,
            color = "#4F46E5",
            priority = 65
        )
        
        // Free/Ad-supported providers
        val TUBI = SourceProvider(
            id = "tubi",
            name = "Tubi",
            displayName = "Tubi",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = false,
            color = "#F59E0B",
            priority = 50
        )
        
        val CRACKLE = SourceProvider(
            id = "crackle",
            name = "Crackle",
            displayName = "Crackle",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = false,
            color = "#F97316",
            priority = 45
        )
        
        val PLUTO_TV = SourceProvider(
            id = "pluto_tv",
            name = "Pluto TV",
            displayName = "Pluto TV",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = false,
            color = "#FBBF24",
            priority = 40
        )
        
        // Rental/Purchase providers
        val GOOGLE_PLAY = SourceProvider(
            id = "google_play",
            name = "Google Play Movies & TV",
            displayName = "Google Play",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = false,
            color = "#4285F4",
            priority = 60
        )
        
        val APPLE_ITUNES = SourceProvider(
            id = "apple_itunes",
            name = "Apple iTunes",
            displayName = "iTunes",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = false,
            color = "#000000",
            priority = 55
        )
        
        val VUDU = SourceProvider(
            id = "vudu",
            name = "Vudu",
            displayName = "Vudu",
            logoUrl = null,
            logoResource = null,
            requiresSubscription = false,
            color = "#2563EB",
            priority = 55
        )
        
        // Get all predefined providers
        fun getAllProviders(): List<SourceProvider> = listOf(
            NETFLIX, AMAZON_PRIME, DISNEY_PLUS, HULU, HBO_MAX, APPLE_TV,
            PARAMOUNT_PLUS, PEACOCK, TUBI, CRACKLE, PLUTO_TV,
            GOOGLE_PLAY, APPLE_ITUNES, VUDU
        )
    }
}

/**
 * Video quality levels for streaming sources
 */
enum class SourceQuality(
    val displayName: String,
    val shortName: String,
    val priority: Int, // Higher priority qualities appear first
    val isHighQuality: Boolean = false
) {
    QUALITY_8K("8K Ultra HD", "8K", 100, true),
    QUALITY_4K("4K Ultra HD", "4K", 90, true),
    QUALITY_4K_HDR("4K HDR", "4K HDR", 95, true),
    QUALITY_1080P("Full HD", "1080p", 80),
    QUALITY_1080P_HDR("Full HD HDR", "1080p HDR", 85, true),
    QUALITY_720P("HD", "720p", 70),
    QUALITY_720P_HDR("HD HDR", "720p HDR", 75, true),
    QUALITY_480P("Standard", "480p", 60),
    QUALITY_360P("Low", "360p", 50),
    QUALITY_240P("Very Low", "240p", 40),
    QUALITY_AUTO("Auto", "Auto", 30);
    
    companion object {
        fun fromString(quality: String?): SourceQuality? {
            if (quality == null) return null
            return entries.find { 
                it.displayName.equals(quality, ignoreCase = true) ||
                it.shortName.equals(quality, ignoreCase = true) ||
                it.name.equals(quality, ignoreCase = true)
            }
        }
        
        fun getHighQualityOptions(): List<SourceQuality> {
            return entries.filter { it.isHighQuality }.sortedByDescending { it.priority }
        }
        
        fun getStandardQualityOptions(): List<SourceQuality> {
            return entries.filter { !it.isHighQuality }.sortedByDescending { it.priority }
        }
    }
}

/**
 * Additional streaming source features
 */
data class SourceFeatures(
    val supportsDolbyVision: Boolean = false,
    val supportsDolbyAtmos: Boolean = false,
    val supportsDownload: Boolean = false,
    val supportsOfflineViewing: Boolean = false,
    val maxDevices: Int? = null,
    val simultaneousStreams: Int? = null,
    val hasAds: Boolean = false,
    val isLiveContent: Boolean = false,
    val hasSubtitles: Boolean = true,
    val hasClosedCaptions: Boolean = true,
    val supportedLanguages: List<String> = emptyList()
)

/**
 * Pricing information for streaming sources
 */
data class SourcePricing(
    val type: PricingType,
    val price: Double? = null,
    val currency: String = "USD",
    val period: String? = null, // "monthly", "yearly", "one-time"
    val isPromotional: Boolean = false,
    val promotionalPrice: Double? = null,
    val promotionalPeriod: String? = null,
    val hasFreeTrial: Boolean = false,
    val freeTrialDuration: String? = null
) {
    enum class PricingType {
        FREE,
        SUBSCRIPTION,
        RENTAL,
        PURCHASE,
        FREE_WITH_ADS
    }
    
    fun getDisplayPrice(): String {
        return when (type) {
            PricingType.FREE -> "Free"
            PricingType.FREE_WITH_ADS -> "Free with ads"
            PricingType.SUBSCRIPTION -> {
                val displayPrice = if (isPromotional && promotionalPrice != null) {
                    "$${promotionalPrice}${period?.let { "/$it" } ?: ""}"
                } else {
                    price?.let { "$${it}${period?.let { "/$it" } ?: ""}" } ?: "Subscription"
                }
                displayPrice + if (hasFreeTrial) " (Free trial)" else ""
            }
            PricingType.RENTAL -> price?.let { "Rent $${it}" } ?: "Rent"
            PricingType.PURCHASE -> price?.let { "Buy $${it}" } ?: "Buy"
        }
    }
}

/**
 * Represents a streaming source with provider, quality, and metadata
 */
data class StreamingSource(
    val id: String,
    val provider: SourceProvider,
    val quality: SourceQuality,
    val url: String,
    val isAvailable: Boolean = true,
    val features: SourceFeatures = SourceFeatures(),
    val pricing: SourcePricing,
    val region: String? = null,
    val expirationDate: String? = null, // ISO date string
    val addedDate: String? = null, // ISO date string
    val lastUpdated: String? = null, // ISO date string
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Check if this source is currently available
     */
    fun isCurrentlyAvailable(): Boolean {
        return isAvailable && provider.isAvailable
    }
    
    /**
     * Get display text for availability
     */
    fun getAvailabilityText(): String {
        return when {
            !isAvailable -> "Not available"
            !provider.isAvailable -> "Provider unavailable"
            expirationDate != null -> "Expires $expirationDate"
            else -> "Available"
        }
    }
    
    /**
     * Get priority score for sorting sources
     */
    fun getPriorityScore(): Int {
        var score = provider.priority + quality.priority
        
        // Bonus for high-quality features
        if (features.supportsDolbyVision) score += 10
        if (features.supportsDolbyAtmos) score += 5
        if (features.supportsDownload) score += 5
        
        // Penalty for ads
        if (features.hasAds) score -= 10
        
        // Bonus for free content
        if (pricing.type == SourcePricing.PricingType.FREE) score += 20
        
        return score
    }
    
    /**
     * Check if this source requires payment
     */
    fun requiresPayment(): Boolean {
        return pricing.type in listOf(
            SourcePricing.PricingType.SUBSCRIPTION,
            SourcePricing.PricingType.RENTAL,
            SourcePricing.PricingType.PURCHASE
        )
    }
    
    /**
     * Get all quality badges for this source
     */
    fun getQualityBadges(): List<String> {
        val badges = mutableListOf<String>()
        
        // Main quality badge
        badges.add(quality.shortName)
        
        // Additional quality features
        if (features.supportsDolbyVision) badges.add("Dolby Vision")
        if (features.supportsDolbyAtmos) badges.add("Dolby Atmos")
        
        return badges
    }
    
    companion object {
        /**
         * Create a sample streaming source for testing
         */
        fun createSample(
            provider: SourceProvider = SourceProvider.NETFLIX,
            quality: SourceQuality = SourceQuality.QUALITY_4K,
            pricing: SourcePricing = SourcePricing(SourcePricing.PricingType.SUBSCRIPTION)
        ): StreamingSource {
            return StreamingSource(
                id = "${provider.id}_${quality.name}",
                provider = provider,
                quality = quality,
                url = "https://example.com/stream",
                pricing = pricing,
                features = SourceFeatures(
                    supportsDolbyVision = quality.isHighQuality,
                    supportsDolbyAtmos = quality.isHighQuality,
                    supportsDownload = provider.requiresSubscription,
                    hasSubtitles = true,
                    hasClosedCaptions = true
                )
            )
        }
        
        /**
         * Create sample sources for testing
         */
        fun createSampleSources(): List<StreamingSource> {
            return listOf(
                createSample(
                    SourceProvider.NETFLIX,
                    SourceQuality.QUALITY_4K_HDR,
                    SourcePricing(SourcePricing.PricingType.SUBSCRIPTION)
                ),
                createSample(
                    SourceProvider.AMAZON_PRIME,
                    SourceQuality.QUALITY_4K,
                    SourcePricing(SourcePricing.PricingType.SUBSCRIPTION)
                ),
                createSample(
                    SourceProvider.DISNEY_PLUS,
                    SourceQuality.QUALITY_1080P_HDR,
                    SourcePricing(SourcePricing.PricingType.SUBSCRIPTION)
                ),
                createSample(
                    SourceProvider.GOOGLE_PLAY,
                    SourceQuality.QUALITY_4K,
                    SourcePricing(SourcePricing.PricingType.RENTAL, price = 5.99)
                ),
                createSample(
                    SourceProvider.TUBI,
                    SourceQuality.QUALITY_1080P,
                    SourcePricing(SourcePricing.PricingType.FREE_WITH_ADS)
                )
            )
        }
    }
}