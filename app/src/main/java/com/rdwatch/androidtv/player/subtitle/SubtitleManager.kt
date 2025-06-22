package com.rdwatch.androidtv.player.subtitle

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class SubtitleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val _availableSubtitles = MutableStateFlow<List<AvailableSubtitle>>(emptyList())
    val availableSubtitles: StateFlow<List<AvailableSubtitle>> = _availableSubtitles.asStateFlow()
    
    private val _selectedSubtitle = MutableStateFlow<AvailableSubtitle?>(null)
    val selectedSubtitle: StateFlow<AvailableSubtitle?> = _selectedSubtitle.asStateFlow()
    
    private val _subtitleStyle = MutableStateFlow(getDefaultSubtitleStyle())
    val subtitleStyle: StateFlow<SubtitleStyle> = _subtitleStyle.asStateFlow()
    
    fun addSubtitleTracks(subtitles: List<SubtitleTrack>) {
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
        subtitles: List<SubtitleTrack> = emptyList()
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