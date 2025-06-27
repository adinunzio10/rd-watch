package com.rdwatch.androidtv.player.subtitle

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.rdwatch.androidtv.player.MediaSourceFactory
import com.rdwatch.androidtv.player.subtitle.parser.SubtitleParserFactory
import com.rdwatch.androidtv.player.subtitle.parser.SubtitleParseResult
import com.rdwatch.androidtv.player.subtitle.parser.SubtitleLoadConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple subtitle track data class for external use
 */
data class ExternalSubtitleTrack(
    val language: String,
    val label: String,
    val url: String,
    val mimeType: String
)

@UnstableApi
@Singleton
class SubtitleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subtitleParserFactory: SubtitleParserFactory,
    private val subtitleSynchronizer: SubtitleSynchronizer,
    private val styleRepository: SubtitleStyleRepository,
    private val errorHandler: SubtitleErrorHandler
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val loadedSubtitleTracks = mutableMapOf<String, SubtitleTrackData>()
    
    private val _availableSubtitles = MutableStateFlow<List<AvailableSubtitle>>(emptyList())
    val availableSubtitles: StateFlow<List<AvailableSubtitle>> = _availableSubtitles.asStateFlow()
    
    private val _selectedSubtitle = MutableStateFlow<AvailableSubtitle?>(null)
    val selectedSubtitle: StateFlow<AvailableSubtitle?> = _selectedSubtitle.asStateFlow()
    
    private val _subtitleStyle = MutableStateFlow(getDefaultSubtitleStyle())
    val subtitleStyle: StateFlow<SubtitleStyle> = _subtitleStyle.asStateFlow()
    
    // Enhanced styling
    val styleConfig: StateFlow<SubtitleStyleConfig> = styleRepository.styleConfig.stateIn(
        scope = scope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = SubtitleStyleConfig.forAndroidTV()
    )
    
    private val _loadingState = MutableStateFlow<SubtitleLoadingState>(SubtitleLoadingState.Idle)
    val loadingState: StateFlow<SubtitleLoadingState> = _loadingState.asStateFlow()
    
    private val _activeSubtitleCues = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val activeSubtitleCues: StateFlow<List<SubtitleCue>> = _activeSubtitleCues.asStateFlow()
    
    // Error handling
    val errors: StateFlow<List<SubtitleError>> = errorHandler.errors
    
    /**
     * Initialize with ExoPlayer for synchronization
     */
    fun initialize(exoPlayer: androidx.media3.exoplayer.ExoPlayer) {
        subtitleSynchronizer.initialize(exoPlayer, this)
    }
    
    fun addSubtitleTracks(subtitles: List<ExternalSubtitleTrack>) {
        val available = subtitles.mapIndexed { index, track ->
            AvailableSubtitle(
                id = index,
                language = track.language,
                label = track.label,
                url = track.url,
                mimeType = track.mimeType,
                isEmbedded = false
            )
        }
        _availableSubtitles.value = available
        
        // Auto-select first subtitle if none selected
        if (_selectedSubtitle.value == null && available.isNotEmpty()) {
            selectSubtitle(available.first())
        }
    }
    
    /**
     * Load external subtitle from URL
     */
    suspend fun loadExternalSubtitle(config: SubtitleLoadConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _loadingState.value = SubtitleLoadingState.Loading(config.url)
                
                // Clear previous errors for this URL
                errorHandler.clearErrorsForUrl(config.url)
                
                val result = subtitleParserFactory.parseSubtitleFromUrl(
                    url = config.url,
                    format = config.format,
                    encoding = config.encoding
                )
                
                when (result) {
                    is SubtitleParseResult.Success -> {
                        val trackData = result.trackData
                        loadedSubtitleTracks[config.url] = trackData
                        
                        // Add to available subtitles
                        val newSubtitle = AvailableSubtitle(
                            id = _availableSubtitles.value.size,
                            language = config.language ?: trackData.language ?: "unknown",
                            label = config.label ?: "External Subtitle",
                            url = config.url,
                            mimeType = trackData.format.mimeType,
                            isEmbedded = false
                        )
                        
                        val currentSubtitles = _availableSubtitles.value.toMutableList()
                        currentSubtitles.add(newSubtitle)
                        _availableSubtitles.value = currentSubtitles
                        
                        // Auto-select if specified
                        if (config.autoSelect) {
                            selectSubtitle(newSubtitle)
                        }
                        
                        _loadingState.value = SubtitleLoadingState.Success(config.url)
                        true
                    }
                    is SubtitleParseResult.Error -> {
                        val error = errorHandler.handleParsingError(config.url, result.exception)
                        _loadingState.value = SubtitleLoadingState.Error(
                            config.url, 
                            error.message
                        )
                        false
                    }
                }
            } catch (e: Exception) {
                val error = errorHandler.handleError(config.url, e, SubtitleErrorContext.LOADING)
                _loadingState.value = SubtitleLoadingState.Error(
                    config.url, 
                    error.message
                )
                false
            }
        }
    }
    
    /**
     * Load multiple external subtitles
     */
    fun loadExternalSubtitles(configs: List<SubtitleLoadConfig>) {
        scope.launch {
            configs.forEach { config ->
                loadExternalSubtitle(config)
            }
        }
    }
    
    fun addEmbeddedSubtitles(trackCount: Int, trackInfoProvider: (Int) -> Pair<String, String>) {
        val embedded = (0 until trackCount).map { index ->
            val (language, label) = trackInfoProvider(index)
            AvailableSubtitle(
                id = index,
                language = language,
                label = label,
                url = "",
                mimeType = "",
                isEmbedded = true
            )
        }
        
        val current = _availableSubtitles.value
        _availableSubtitles.value = current + embedded
    }
    
    fun selectSubtitle(subtitle: AvailableSubtitle?) {
        _selectedSubtitle.value = subtitle
        
        // Clear active cues when changing subtitle
        _activeSubtitleCues.value = emptyList()
        
        // Load subtitle data if external
        subtitle?.let { sub ->
            if (!sub.isEmbedded && sub.url.isNotEmpty()) {
                scope.launch {
                    // Ensure subtitle is loaded
                    if (!loadedSubtitleTracks.containsKey(sub.url)) {
                        val config = SubtitleLoadConfig(
                            url = sub.url,
                            format = SubtitleFormat.fromMimeType(sub.mimeType),
                            language = sub.language,
                            label = sub.label
                        )
                        loadExternalSubtitle(config)
                    }
                }
            }
        }
    }
    
    /**
     * Update active subtitle cues based on current playback position
     */
    fun updateSubtitlesForPosition(positionMs: Long) {
        val selectedSub = _selectedSubtitle.value ?: return
        
        if (!selectedSub.isEmbedded) {
            // Handle external subtitles
            val trackData = loadedSubtitleTracks[selectedSub.url]
            if (trackData != null) {
                val activeCues = trackData.getCuesAt(positionMs)
                _activeSubtitleCues.value = activeCues
            }
        }
        // For embedded subtitles, ExoPlayer handles this automatically
    }
    
    /**
     * Get subtitle track data for external subtitles
     */
    fun getSubtitleTrackData(url: String): SubtitleTrackData? {
        return loadedSubtitleTracks[url]
    }
    
    /**
     * Remove loaded subtitle track
     */
    fun removeSubtitleTrack(url: String) {
        loadedSubtitleTracks.remove(url)
        
        // Remove from available subtitles
        val updated = _availableSubtitles.value.filter { it.url != url }
        _availableSubtitles.value = updated
        
        // Deselect if currently selected
        val selected = _selectedSubtitle.value
        if (selected?.url == url) {
            _selectedSubtitle.value = null
            _activeSubtitleCues.value = emptyList()
        }
    }
    
    /**
     * Clear all loaded subtitles
     */
    fun clearAllSubtitles() {
        loadedSubtitleTracks.clear()
        _availableSubtitles.value = emptyList()
        _selectedSubtitle.value = null
        _activeSubtitleCues.value = emptyList()
        _loadingState.value = SubtitleLoadingState.Idle
    }
    
    fun updateSubtitleStyle(style: SubtitleStyle) {
        _subtitleStyle.value = style
    }
    
    fun configureSubtitleView(subtitleView: SubtitleView) {
        val style = _subtitleStyle.value
        
        subtitleView.setStyle(
            CaptionStyleCompat(
                style.foregroundColor,
                style.backgroundColor,
                style.windowColor,
                style.edgeType,
                style.edgeColor,
                style.typeface
            )
        )
        
        subtitleView.setFractionalTextSize(style.textSize)
        subtitleView.setBottomPaddingFraction(style.bottomPadding)
    }
    
    private fun getDefaultSubtitleStyle(): SubtitleStyle {
        return SubtitleStyle(
            textSize = 0.08f, // 8% of screen height - good for TV
            foregroundColor = android.graphics.Color.WHITE,
            backgroundColor = android.graphics.Color.TRANSPARENT,
            windowColor = android.graphics.Color.parseColor("#80000000"), // Semi-transparent black
            edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            edgeColor = android.graphics.Color.BLACK,
            typeface = android.graphics.Typeface.DEFAULT_BOLD,
            bottomPadding = 0.1f // 10% from bottom for TV safe area
        )
    }
    
    fun createMediaItemWithSubtitles(
        videoUrl: String,
        title: String? = null,
        subtitles: List<ExternalSubtitleTrack> = emptyList()
    ): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(videoUrl)
            .apply {
                title?.let {
                    setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(it)
                            .build()
                    )
                }
            }
        
        // Add subtitle tracks
        subtitles.forEach { subtitle ->
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitle.url))
                        .setMimeType(subtitle.mimeType)
                        .setLanguage(subtitle.language)
                        .setLabel(subtitle.label)
                        .build()
                )
            )
        }
        
        return builder.build()
    }
    
    /**
     * Get supported subtitle formats
     */
    fun getSupportedFormats(): List<SubtitleFormat> {
        return subtitleParserFactory.getSupportedFormats()
    }
    
    /**
     * Check if format is supported
     */
    fun isFormatSupported(format: SubtitleFormat): Boolean {
        return subtitleParserFactory.isFormatSupported(format)
    }
    
    /**
     * Get current subtitle text for display (for external subtitles)
     */
    fun getCurrentSubtitleText(positionMs: Long): String? {
        val selectedSub = _selectedSubtitle.value ?: return null
        
        if (!selectedSub.isEmbedded) {
            val trackData = loadedSubtitleTracks[selectedSub.url] ?: return null
            val activeCues = trackData.getCuesAt(positionMs)
            return if (activeCues.isNotEmpty()) {
                activeCues.joinToString("\n") { it.text }
            } else null
        }
        
        return null
    }
    
    /**
     * Set subtitle timing offset
     */
    fun setSubtitleOffset(offsetMs: Long) {
        subtitleSynchronizer.setSubtitleOffset(offsetMs)
    }
    
    /**
     * Get current subtitle timing offset
     */
    fun getSubtitleOffset(): Long {
        return subtitleSynchronizer.getSubtitleOffset()
    }
    
    /**
     * Start subtitle synchronization
     */
    fun startSynchronization() {
        subtitleSynchronizer.startSynchronization()
    }
    
    /**
     * Stop subtitle synchronization
     */
    fun stopSynchronization() {
        subtitleSynchronizer.stopSynchronization()
    }
    
    /**
     * Get synchronization state
     */
    fun getSynchronizationState() = subtitleSynchronizer.synchronizationState
    
    /**
     * Get timing statistics
     */
    fun getTimingStats() = subtitleSynchronizer.getTimingStats()
    
    /**
     * Update subtitle style configuration
     */
    suspend fun updateStyleConfig(config: SubtitleStyleConfig) {
        styleRepository.saveStyleConfig(config)
    }
    
    /**
     * Apply a preset style
     */
    suspend fun applyStylePreset(presetName: String) {
        styleRepository.applyPreset(presetName)
    }
    
    /**
     * Reset style to default
     */
    suspend fun resetStyleToDefault() {
        styleRepository.resetToDefault()
    }
    
    /**
     * Get available style presets
     */
    fun getStylePresets(): Map<String, SubtitleStyleConfig> {
        return SubtitleStyleConfig.getPresets()
    }
    
    // Error Handling Methods
    
    /**
     * Get errors for specific URL
     */
    fun getErrorsForUrl(url: String): List<SubtitleError> {
        return errorHandler.getErrorsForUrl(url)
    }
    
    /**
     * Check if URL has critical errors
     */
    fun hasCriticalErrors(url: String): Boolean {
        return errorHandler.hasCriticalErrors(url)
    }
    
    /**
     * Clear all subtitle errors
     */
    fun clearErrors() {
        errorHandler.clearErrors()
    }
    
    /**
     * Clear errors for specific URL
     */
    fun clearErrorsForUrl(url: String) {
        errorHandler.clearErrorsForUrl(url)
    }
    
    /**
     * Retry loading subtitle for URL
     */
    fun retrySubtitle(url: String) {
        errorHandler.retryUrl(url) { retryUrl ->
            // Find the config for this URL and retry
            val config = com.rdwatch.androidtv.player.subtitle.parser.SubtitleLoadConfig(url = retryUrl)
            loadExternalSubtitle(config)
        }
    }
    
    /**
     * Get retry count for URL
     */
    fun getRetryCount(url: String): Int {
        return errorHandler.getRetryCount(url)
    }
    
    /**
     * Dispose resources
     */
    fun dispose() {
        subtitleSynchronizer.dispose()
    }
}

data class AvailableSubtitle(
    val id: Int,
    val language: String,
    val label: String,
    val url: String,
    val mimeType: String,
    val isEmbedded: Boolean
)

data class SubtitleStyle(
    val textSize: Float,
    val foregroundColor: Int,
    val backgroundColor: Int,
    val windowColor: Int,
    val edgeType: Int,
    val edgeColor: Int,
    val typeface: android.graphics.Typeface,
    val bottomPadding: Float
)

/**
 * State of subtitle loading operations
 */
sealed class SubtitleLoadingState {
    object Idle : SubtitleLoadingState()
    data class Loading(val url: String) : SubtitleLoadingState()
    data class Success(val url: String) : SubtitleLoadingState()
    data class Error(val url: String, val message: String) : SubtitleLoadingState()
}