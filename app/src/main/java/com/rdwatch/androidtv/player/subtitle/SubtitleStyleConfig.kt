package com.rdwatch.androidtv.player.subtitle

import android.graphics.Color
import android.graphics.Typeface
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced subtitle style configuration with user preferences
 */
@UnstableApi
data class SubtitleStyleConfig(
    val textSize: Float = 0.08f, // 8% of screen height
    val textColor: Int = Color.WHITE,
    val backgroundColor: Int = Color.TRANSPARENT,
    val windowColor: Int = Color.parseColor("#80000000"), // Semi-transparent black
    val edgeType: Int = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
    val edgeColor: Int = Color.BLACK,
    val typeface: Typeface = Typeface.DEFAULT_BOLD,
    val bottomPadding: Float = 0.1f, // 10% from bottom
    val opacity: Float = 1.0f,
    val shadowEnabled: Boolean = true,
    val shadowColor: Int = Color.BLACK,
    val shadowOffsetX: Float = 2f,
    val shadowOffsetY: Float = 2f,
    val shadowRadius: Float = 4f,
    val outlineWidth: Float = 2f,
    val fontWeight: FontWeight = FontWeight.BOLD,
    val fontStyle: FontStyle = FontStyle.NORMAL,
    val textAlignment: TextAlignment = TextAlignment.CENTER,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM_CENTER,
) {
    /**
     * Convert to Media3 CaptionStyleCompat
     */
    fun toCaptionStyle(): CaptionStyleCompat {
        return CaptionStyleCompat(
            textColor,
            backgroundColor,
            windowColor,
            edgeType,
            edgeColor,
            typeface,
        )
    }

    /**
     * Create a copy with modified text size for different screen sizes
     */
    fun forScreenSize(
        screenWidthDp: Int,
        screenHeightDp: Int,
    ): SubtitleStyleConfig {
        val scaleFactor =
            when {
                screenWidthDp < 600 -> 0.9f // Phone/small tablet
                screenWidthDp < 1200 -> 1.0f // Tablet
                else -> 1.2f // TV/large screen
            }

        return copy(
            textSize = textSize * scaleFactor,
            outlineWidth = outlineWidth * scaleFactor,
            shadowOffsetX = shadowOffsetX * scaleFactor,
            shadowOffsetY = shadowOffsetY * scaleFactor,
            shadowRadius = shadowRadius * scaleFactor,
        )
    }

    /**
     * Validate configuration values
     */
    fun validate(): SubtitleStyleConfig {
        return copy(
            textSize = textSize.coerceIn(0.04f, 0.2f), // 4% to 20%
            bottomPadding = bottomPadding.coerceIn(0f, 0.3f), // 0% to 30%
            opacity = opacity.coerceIn(0f, 1f),
            shadowOffsetX = shadowOffsetX.coerceIn(-10f, 10f),
            shadowOffsetY = shadowOffsetY.coerceIn(-10f, 10f),
            shadowRadius = shadowRadius.coerceIn(0f, 20f),
            outlineWidth = outlineWidth.coerceIn(0f, 10f),
        )
    }

    companion object {
        /**
         * Predefined style presets for common use cases
         */
        fun getPresets(): Map<String, SubtitleStyleConfig> {
            return mapOf(
                "default" to SubtitleStyleConfig(),
                "large_text" to
                    SubtitleStyleConfig(
                        textSize = 0.12f,
                        fontWeight = FontWeight.BOLD,
                    ),
                "high_contrast" to
                    SubtitleStyleConfig(
                        textColor = Color.YELLOW,
                        backgroundColor = Color.BLACK,
                        windowColor = Color.parseColor("#E0000000"),
                        edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        outlineWidth = 3f,
                    ),
                "minimal" to
                    SubtitleStyleConfig(
                        backgroundColor = Color.TRANSPARENT,
                        windowColor = Color.TRANSPARENT,
                        edgeType = CaptionStyleCompat.EDGE_TYPE_NONE,
                        shadowEnabled = false,
                    ),
                "cinema" to
                    SubtitleStyleConfig(
                        textColor = Color.WHITE,
                        backgroundColor = Color.TRANSPARENT,
                        windowColor = Color.parseColor("#90000000"),
                        edgeType = CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                        shadowEnabled = true,
                        shadowRadius = 8f,
                        bottomPadding = 0.15f,
                    ),
                "accessibility" to
                    SubtitleStyleConfig(
                        textSize = 0.15f,
                        textColor = Color.WHITE,
                        backgroundColor = Color.BLACK,
                        windowColor = Color.parseColor("#F0000000"),
                        edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        outlineWidth = 4f,
                        fontWeight = FontWeight.BOLD,
                    ),
            )
        }

        /**
         * Get style for Android TV (optimized for 10-foot viewing)
         */
        fun forAndroidTV(): SubtitleStyleConfig {
            return SubtitleStyleConfig(
                textSize = 0.1f, // Larger text for TV
                bottomPadding = 0.12f, // More padding for TV safe area
                edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                outlineWidth = 3f,
                shadowEnabled = true,
                shadowRadius = 6f,
            )
        }
    }
}

/**
 * Font weight options
 */
enum class FontWeight(val weight: Int) {
    LIGHT(300),
    NORMAL(400),
    MEDIUM(500),
    BOLD(700),
    BLACK(900),
    ;

    fun toTypeface(): Typeface {
        return when (this) {
            LIGHT -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            NORMAL -> Typeface.DEFAULT
            MEDIUM -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            BOLD -> Typeface.DEFAULT_BOLD
            BLACK -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
}

/**
 * Font style options
 */
enum class FontStyle {
    NORMAL,
    ITALIC,
    ;

    fun toTypefaceStyle(): Int {
        return when (this) {
            NORMAL -> Typeface.NORMAL
            ITALIC -> Typeface.ITALIC
        }
    }
}

/**
 * Text alignment options
 */
enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT,
    ;

    fun toCueAlignment(): Int {
        return when (this) {
            LEFT -> 1 // TEXT_ALIGNMENT_START
            CENTER -> 2 // TEXT_ALIGNMENT_CENTER
            RIGHT -> 3 // TEXT_ALIGNMENT_END
        }
    }
}

/**
 * Subtitle position options
 */
enum class SubtitlePosition(
    val line: Float,
    val position: Float,
    val anchor: Int,
) {
    TOP_LEFT(0.1f, 0.1f, 0), // ANCHOR_TYPE_START
    TOP_CENTER(0.1f, 0.5f, 1), // ANCHOR_TYPE_MIDDLE
    TOP_RIGHT(0.1f, 0.9f, 2), // ANCHOR_TYPE_END
    MIDDLE_LEFT(0.5f, 0.1f, 0), // ANCHOR_TYPE_START
    MIDDLE_CENTER(0.5f, 0.5f, 1), // ANCHOR_TYPE_MIDDLE
    MIDDLE_RIGHT(0.5f, 0.9f, 2), // ANCHOR_TYPE_END
    BOTTOM_LEFT(0.9f, 0.1f, 0), // ANCHOR_TYPE_START
    BOTTOM_CENTER(0.9f, 0.5f, 1), // ANCHOR_TYPE_MIDDLE
    BOTTOM_RIGHT(0.9f, 0.9f, 2), // ANCHOR_TYPE_END
}

/**
 * Repository for managing subtitle style preferences
 */
@UnstableApi
@Singleton
class SubtitleStyleRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private companion object {
            val TEXT_SIZE = floatPreferencesKey("subtitle_text_size")
            val TEXT_COLOR = intPreferencesKey("subtitle_text_color")
            val BACKGROUND_COLOR = intPreferencesKey("subtitle_background_color")
            val WINDOW_COLOR = intPreferencesKey("subtitle_window_color")
            val EDGE_TYPE = intPreferencesKey("subtitle_edge_type")
            val EDGE_COLOR = intPreferencesKey("subtitle_edge_color")
            val BOTTOM_PADDING = floatPreferencesKey("subtitle_bottom_padding")
            val OPACITY = floatPreferencesKey("subtitle_opacity")
            val SHADOW_ENABLED = stringPreferencesKey("subtitle_shadow_enabled")
            val SHADOW_COLOR = intPreferencesKey("subtitle_shadow_color")
            val SHADOW_OFFSET_X = floatPreferencesKey("subtitle_shadow_offset_x")
            val SHADOW_OFFSET_Y = floatPreferencesKey("subtitle_shadow_offset_y")
            val SHADOW_RADIUS = floatPreferencesKey("subtitle_shadow_radius")
            val OUTLINE_WIDTH = floatPreferencesKey("subtitle_outline_width")
            val FONT_WEIGHT = stringPreferencesKey("subtitle_font_weight")
            val FONT_STYLE = stringPreferencesKey("subtitle_font_style")
            val TEXT_ALIGNMENT = stringPreferencesKey("subtitle_text_alignment")
            val POSITION = stringPreferencesKey("subtitle_position")
        }

        /**
         * Get current subtitle style configuration
         */
        val styleConfig: Flow<SubtitleStyleConfig> =
            dataStore.data.map { preferences ->
                SubtitleStyleConfig(
                    textSize = preferences[TEXT_SIZE] ?: 0.08f,
                    textColor = preferences[TEXT_COLOR] ?: Color.WHITE,
                    backgroundColor = preferences[BACKGROUND_COLOR] ?: Color.TRANSPARENT,
                    windowColor = preferences[WINDOW_COLOR] ?: Color.parseColor("#80000000"),
                    edgeType = preferences[EDGE_TYPE] ?: CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    edgeColor = preferences[EDGE_COLOR] ?: Color.BLACK,
                    bottomPadding = preferences[BOTTOM_PADDING] ?: 0.1f,
                    opacity = preferences[OPACITY] ?: 1.0f,
                    shadowEnabled = preferences[SHADOW_ENABLED]?.toBoolean() ?: true,
                    shadowColor = preferences[SHADOW_COLOR] ?: Color.BLACK,
                    shadowOffsetX = preferences[SHADOW_OFFSET_X] ?: 2f,
                    shadowOffsetY = preferences[SHADOW_OFFSET_Y] ?: 2f,
                    shadowRadius = preferences[SHADOW_RADIUS] ?: 4f,
                    outlineWidth = preferences[OUTLINE_WIDTH] ?: 2f,
                    fontWeight = FontWeight.valueOf(preferences[FONT_WEIGHT] ?: "BOLD"),
                    fontStyle = FontStyle.valueOf(preferences[FONT_STYLE] ?: "NORMAL"),
                    textAlignment = TextAlignment.valueOf(preferences[TEXT_ALIGNMENT] ?: "CENTER"),
                    position = SubtitlePosition.valueOf(preferences[POSITION] ?: "BOTTOM_CENTER"),
                ).validate()
            }

        /**
         * Save subtitle style configuration
         */
        suspend fun saveStyleConfig(config: SubtitleStyleConfig) {
            val validatedConfig = config.validate()

            dataStore.edit { preferences ->
                preferences[TEXT_SIZE] = validatedConfig.textSize
                preferences[TEXT_COLOR] = validatedConfig.textColor
                preferences[BACKGROUND_COLOR] = validatedConfig.backgroundColor
                preferences[WINDOW_COLOR] = validatedConfig.windowColor
                preferences[EDGE_TYPE] = validatedConfig.edgeType
                preferences[EDGE_COLOR] = validatedConfig.edgeColor
                preferences[BOTTOM_PADDING] = validatedConfig.bottomPadding
                preferences[OPACITY] = validatedConfig.opacity
                preferences[SHADOW_ENABLED] = validatedConfig.shadowEnabled.toString()
                preferences[SHADOW_COLOR] = validatedConfig.shadowColor
                preferences[SHADOW_OFFSET_X] = validatedConfig.shadowOffsetX
                preferences[SHADOW_OFFSET_Y] = validatedConfig.shadowOffsetY
                preferences[SHADOW_RADIUS] = validatedConfig.shadowRadius
                preferences[OUTLINE_WIDTH] = validatedConfig.outlineWidth
                preferences[FONT_WEIGHT] = validatedConfig.fontWeight.name
                preferences[FONT_STYLE] = validatedConfig.fontStyle.name
                preferences[TEXT_ALIGNMENT] = validatedConfig.textAlignment.name
                preferences[POSITION] = validatedConfig.position.name
            }
        }

        /**
         * Reset to default style
         */
        suspend fun resetToDefault() {
            saveStyleConfig(SubtitleStyleConfig.forAndroidTV())
        }

        /**
         * Apply a preset style
         */
        suspend fun applyPreset(presetName: String) {
            val presets = SubtitleStyleConfig.getPresets()
            val preset = presets[presetName] ?: SubtitleStyleConfig.forAndroidTV()
            saveStyleConfig(preset)
        }
    }
