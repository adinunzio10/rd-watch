package com.rdwatch.androidtv.ui.details.models

/**
 * Unified sorting options for streaming sources
 * Combines options from both basic UI and advanced filtering systems
 */
enum class SourceSortOption(
    val displayName: String,
    val description: String,
    val relevantTo: Set<SortContext> = setOf(SortContext.BASIC, SortContext.ADVANCED),
) {
    // High-level user-friendly options (from components version)
    PRIORITY(
        displayName = "Priority",
        description = "Best overall sources first (quality + reliability)",
        relevantTo = setOf(SortContext.BASIC, SortContext.ADVANCED),
    ),
    QUALITY(
        displayName = "Quality",
        description = "Highest quality first (4K, HDR, etc.)",
        relevantTo = setOf(SortContext.BASIC, SortContext.ADVANCED),
    ),
    PROVIDER(
        displayName = "Provider",
        description = "Group by provider name",
        relevantTo = setOf(SortContext.BASIC, SortContext.ADVANCED),
    ),
    SEEDERS(
        displayName = "Seeders",
        description = "Most seeders first (P2P sources)",
        relevantTo = setOf(SortContext.BASIC, SortContext.ADVANCED),
    ),
    RELIABILITY(
        displayName = "Reliability",
        description = "Most reliable sources first",
        relevantTo = setOf(SortContext.BASIC, SortContext.ADVANCED),
    ),
    AVAILABILITY(
        displayName = "Availability",
        description = "Currently available sources first",
        relevantTo = setOf(SortContext.BASIC, SortContext.ADVANCED),
    ),

    // Advanced/technical options (from advanced models version)
    QUALITY_SCORE(
        displayName = "Quality Score",
        description = "Advanced quality scoring algorithm",
        relevantTo = setOf(SortContext.ADVANCED),
    ),
    FILE_SIZE(
        displayName = "File Size",
        description = "Largest files first",
        relevantTo = setOf(SortContext.ADVANCED),
    ),
    ADDED_DATE(
        displayName = "Date Added",
        description = "Most recently added sources first",
        relevantTo = setOf(SortContext.ADVANCED),
    ),
    RELEASE_TYPE(
        displayName = "Release Type",
        description = "Group by release type (WEB-DL, BluRay, etc.)",
        relevantTo = setOf(SortContext.ADVANCED),
    ),
    ;

    /**
     * Get options relevant to a specific context
     */
    companion object {
        fun forContext(context: SortContext): List<SourceSortOption> {
            return values().filter { context in it.relevantTo }
        }

        fun basicOptions(): List<SourceSortOption> = forContext(SortContext.BASIC)

        fun advancedOptions(): List<SourceSortOption> = forContext(SortContext.ADVANCED)

        /**
         * Default option for basic UI
         */
        val DEFAULT_BASIC = PRIORITY

        /**
         * Default option for advanced filtering
         */
        val DEFAULT_ADVANCED = QUALITY_SCORE

        /**
         * Migration helpers for backward compatibility
         */
        @Deprecated("Use unified enum values directly")
        fun fromComponentsEnum(old: String): SourceSortOption? {
            return when (old.uppercase()) {
                "PRIORITY" -> PRIORITY
                "QUALITY" -> QUALITY
                "PROVIDER" -> PROVIDER
                "SEEDERS" -> SEEDERS
                "RELIABILITY" -> RELIABILITY
                "AVAILABILITY" -> AVAILABILITY
                else -> null
            }
        }

        @Deprecated("Use unified enum values directly")
        fun fromAdvancedEnum(old: String): SourceSortOption? {
            return when (old.uppercase()) {
                "QUALITY_SCORE" -> QUALITY_SCORE
                "FILE_SIZE" -> FILE_SIZE
                "SEEDERS" -> SEEDERS
                "ADDED_DATE" -> ADDED_DATE
                "PROVIDER" -> PROVIDER
                "RELEASE_TYPE" -> RELEASE_TYPE
                else -> null
            }
        }
    }
}

/**
 * Context for which sorting options are relevant
 */
enum class SortContext {
    /**
     * Basic UI context - simple, user-friendly options
     */
    BASIC,

    /**
     * Advanced filtering context - technical options for power users
     */
    ADVANCED,
}
