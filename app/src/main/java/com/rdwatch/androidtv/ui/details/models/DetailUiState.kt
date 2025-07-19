package com.rdwatch.androidtv.ui.details.models

import com.rdwatch.androidtv.ui.common.UiState

/**
 * UI State for Content Details Screen
 * Generic state that works with any ContentDetail implementation
 */
data class DetailUiState(
    val content: ContentDetail? = null,
    val relatedContent: List<ContentDetail> = emptyList(),
    val progress: ContentProgress? = null,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null,
    val defaultLayoutConfig: DetailLayoutConfig = DetailLayoutConfig(),
    val focusedSection: DetailSection = DetailSection.HERO,
) {
    val hasContent: Boolean get() = content != null
    val canPlay: Boolean get() = content?.isPlayable() == true
    val hasRelatedContent: Boolean get() = relatedContent.isNotEmpty()
    val hasError: Boolean get() = error != null

    /**
     * Get the layout configuration for the current content type
     */
    fun getLayoutConfig(): DetailLayoutConfig {
        return content?.let { DetailLayoutConfig.forContentType(it.contentType) } ?: defaultLayoutConfig
    }

    /**
     * Check if a specific section should be shown
     */
    fun shouldShowSection(section: DetailSection): Boolean {
        val config = getLayoutConfig()
        return when (section) {
            DetailSection.HERO -> config.showHeroSection
            DetailSection.INFO -> config.showInfoSection && hasContent
            DetailSection.ACTIONS -> config.showActionButtons && hasContent
            DetailSection.RELATED -> config.showRelatedContent && hasRelatedContent
            DetailSection.SEASON_EPISODE_GRID -> config.showSeasonEpisodeGrid
            DetailSection.CAST_CREW -> config.showCastCrew
            DetailSection.CUSTOM -> config.customSections.isNotEmpty()
        }
    }

    /**
     * Get visible sections in order
     */
    fun getVisibleSections(): List<DetailSection> {
        return DetailSection.values().filter { shouldShowSection(it) }
    }
}

/**
 * Sections that can be displayed in a detail screen
 */
enum class DetailSection {
    HERO,
    INFO,
    ACTIONS,
    RELATED,
    SEASON_EPISODE_GRID,
    CAST_CREW,
    CUSTOM,
}

/**
 * State for managing related content
 */
data class RelatedContentState(
    val movies: UiState<List<ContentDetail>> = UiState.Loading,
    val tvShows: UiState<List<ContentDetail>> = UiState.Loading,
    val similar: UiState<List<ContentDetail>> = UiState.Loading,
    val recommended: UiState<List<ContentDetail>> = UiState.Loading,
) {
    /**
     * Get all related content as a single list
     */
    fun getAllRelatedContent(): List<ContentDetail> {
        val allContent = mutableListOf<ContentDetail>()

        movies.dataOrNull?.let { allContent.addAll(it) }
        tvShows.dataOrNull?.let { allContent.addAll(it) }
        similar.dataOrNull?.let { allContent.addAll(it) }
        recommended.dataOrNull?.let { allContent.addAll(it) }

        return allContent.distinctBy { it.id }
    }

    /**
     * Check if any related content is available
     */
    fun hasAnyContent(): Boolean {
        return movies.dataOrNull?.isNotEmpty() == true ||
            tvShows.dataOrNull?.isNotEmpty() == true ||
            similar.dataOrNull?.isNotEmpty() == true ||
            recommended.dataOrNull?.isNotEmpty() == true
    }

    /**
     * Check if any related content is loading
     */
    fun isAnyLoading(): Boolean {
        return movies.isLoading || tvShows.isLoading || similar.isLoading || recommended.isLoading
    }

    /**
     * Check if any related content has errors
     */
    fun hasAnyErrors(): Boolean {
        return movies.isError || tvShows.isError || similar.isError || recommended.isError
    }
}

/**
 * State for managing focus within detail screen
 */
data class DetailFocusState(
    val currentSection: DetailSection = DetailSection.HERO,
    val currentItemIndex: Int = 0,
    val canNavigateUp: Boolean = false,
    val canNavigateDown: Boolean = true,
    val canNavigateLeft: Boolean = false,
    val canNavigateRight: Boolean = false,
    val focusRestorePoint: String? = null,
) {
    /**
     * Get the next section for navigation
     */
    fun getNextSection(direction: NavigationDirection): DetailSection? {
        val sections = DetailSection.values()
        val currentIndex = sections.indexOf(currentSection)

        return when (direction) {
            NavigationDirection.UP -> if (currentIndex > 0) sections[currentIndex - 1] else null
            NavigationDirection.DOWN -> if (currentIndex < sections.size - 1) sections[currentIndex + 1] else null
            NavigationDirection.LEFT, NavigationDirection.RIGHT -> currentSection
        }
    }

    /**
     * Update focus state for navigation
     */
    fun navigateToSection(
        section: DetailSection,
        itemIndex: Int = 0,
    ): DetailFocusState {
        return copy(
            currentSection = section,
            currentItemIndex = itemIndex,
            canNavigateUp = section != DetailSection.HERO,
            canNavigateDown = section != DetailSection.values().last(),
            canNavigateLeft = itemIndex > 0,
            canNavigateRight = true, // Will be determined by actual content
        )
    }
}

/**
 * Navigation directions for focus management
 */
enum class NavigationDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}
