package com.rdwatch.androidtv.ui.settings.scrapers

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.scraper.ScraperManifestManager
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Scraper Settings Screen
 * Manages scraper operations through ScraperManifestManager
 */
@HiltViewModel
class ScraperSettingsViewModel @Inject constructor(
    private val scraperManager: ScraperManifestManager
) : BaseViewModel<ScraperSettingsUiState>() {
    
    override fun createInitialState(): ScraperSettingsUiState {
        return ScraperSettingsUiState()
    }
    
    init {
        loadScrapers()
        observeScrapers()
    }
    
    /**
     * Load all scrapers from the repository
     */
    private fun loadScrapers() {
        launchSafely {
            updateState { copy(isLoading = true) }
            
            when (val result = scraperManager.getAllManifests()) {
                is ManifestResult.Success -> {
                    updateState {
                        copy(
                            scrapers = result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is ManifestResult.Error -> {
                    updateState {
                        copy(
                            isLoading = false,
                            error = "Failed to load scrapers: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Observe scrapers for real-time updates
     */
    private fun observeScrapers() {
        scraperManager.observeManifests()
            .onEach { result ->
                when (result) {
                    is ManifestResult.Success -> {
                        updateState {
                            copy(
                                scrapers = result.data,
                                error = null
                            )
                        }
                    }
                    is ManifestResult.Error -> {
                        updateState {
                            copy(
                                error = "Error updating scrapers: ${result.exception.message}"
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Add a new scraper from URL
     */
    fun addScraperFromUrl(url: String) {
        if (url.isBlank()) {
            updateState { copy(error = "URL cannot be empty") }
            return
        }
        
        launchSafely {
            updateState { copy(isAddingFromUrl = true, error = null) }
            
            when (val result = scraperManager.addManifestFromUrl(url)) {
                is ManifestResult.Success -> {
                    updateState {
                        copy(
                            isAddingFromUrl = false,
                            showAddDialog = false,
                            successMessage = "Scraper added successfully: ${result.data.displayName}"
                        )
                    }
                    // Clear success message after delay
                    clearSuccessMessage()
                }
                is ManifestResult.Error -> {
                    updateState {
                        copy(
                            isAddingFromUrl = false,
                            error = "Failed to add scraper: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Remove a scraper
     */
    fun removeScraper(scraperId: String) {
        launchSafely {
            updateState { copy(isLoading = true, error = null) }
            
            when (val result = scraperManager.removeManifest(scraperId)) {
                is ManifestResult.Success -> {
                    updateState {
                        copy(
                            isLoading = false,
                            successMessage = "Scraper removed successfully"
                        )
                    }
                    clearSuccessMessage()
                }
                is ManifestResult.Error -> {
                    updateState {
                        copy(
                            isLoading = false,
                            error = "Failed to remove scraper: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Toggle scraper enabled state
     */
    fun toggleScraperEnabled(scraperId: String, enabled: Boolean) {
        launchSafely {
            when (val result = scraperManager.setManifestEnabled(scraperId, enabled)) {
                is ManifestResult.Success -> {
                    val action = if (enabled) "enabled" else "disabled"
                    updateState {
                        copy(successMessage = "Scraper $action successfully")
                    }
                    clearSuccessMessage()
                }
                is ManifestResult.Error -> {
                    updateState {
                        copy(error = "Failed to update scraper: ${result.exception.message}")
                    }
                }
            }
        }
    }
    
    /**
     * Refresh all scrapers from their source URLs
     */
    fun refreshAllScrapers() {
        launchSafely {
            updateState { copy(isRefreshing = true, error = null) }
            
            when (val result = scraperManager.refreshAllManifests()) {
                is ManifestResult.Success -> {
                    val refreshResult = result.data
                    updateState {
                        copy(
                            isRefreshing = false,
                            successMessage = "Refreshed ${refreshResult.successCount} scrapers"
                        )
                    }
                    clearSuccessMessage()
                }
                is ManifestResult.Error -> {
                    updateState {
                        copy(
                            isRefreshing = false,
                            error = "Failed to refresh scrapers: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Refresh a specific scraper
     */
    fun refreshScraper(scraperId: String) {
        launchSafely {
            when (val result = scraperManager.refreshManifest(scraperId)) {
                is ManifestResult.Success -> {
                    updateState {
                        copy(successMessage = "Scraper refreshed successfully")
                    }
                    clearSuccessMessage()
                }
                is ManifestResult.Error -> {
                    updateState {
                        copy(error = "Failed to refresh scraper: ${result.exception.message}")
                    }
                }
            }
        }
    }
    
    /**
     * Update scraper priority order
     */
    fun updateScraperPriority(scraperId: String, priority: Int) {
        launchSafely {
            when (val result = scraperManager.updateManifestPriority(scraperId, priority)) {
                is ManifestResult.Success -> {
                    updateState {
                        copy(successMessage = "Priority updated successfully")
                    }
                    clearSuccessMessage()
                }
                is ManifestResult.Error -> {
                    updateState {
                        copy(error = "Failed to update priority: ${result.exception.message}")
                    }
                }
            }
        }
    }
    
    /**
     * Show add scraper dialog
     */
    fun showAddDialog() {
        updateState { copy(showAddDialog = true, error = null) }
    }
    
    /**
     * Hide add scraper dialog
     */
    fun hideAddDialog() {
        updateState { copy(showAddDialog = false, addUrlText = "") }
    }
    
    /**
     * Update URL text in add dialog
     */
    fun updateAddUrlText(text: String) {
        updateState { copy(addUrlText = text) }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
    
    /**
     * Clear success message after delay
     */
    private fun clearSuccessMessage() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // 3 seconds
            updateState { copy(successMessage = null) }
        }
    }
    
    override fun handleError(exception: Throwable) {
        updateState {
            copy(
                isLoading = false,
                isAddingFromUrl = false,
                isRefreshing = false,
                error = "Unexpected error: ${exception.message}"
            )
        }
    }
}

/**
 * UI State for Scraper Settings Screen
 */
data class ScraperSettingsUiState(
    val scrapers: List<ScraperManifest> = emptyList(),
    val isLoading: Boolean = false,
    val isAddingFromUrl: Boolean = false,
    val isRefreshing: Boolean = false,
    val showAddDialog: Boolean = false,
    val addUrlText: String = "",
    val error: String? = null,
    val successMessage: String? = null
) {
    val enabledScrapers: List<ScraperManifest>
        get() = scrapers.filter { it.isEnabled }
    
    val disabledScrapers: List<ScraperManifest>
        get() = scrapers.filter { !it.isEnabled }
    
    val hasScrapers: Boolean
        get() = scrapers.isNotEmpty()
}