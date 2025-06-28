package com.rdwatch.androidtv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.rdwatch.androidtv.data.preferences.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing application settings using DataStore.
 * Provides type-safe access to preferences with reactive Flow-based observables.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val SETTINGS_DATASTORE_NAME = "app_settings"
        private const val CURRENT_SETTINGS_VERSION = 1
    }
    
    // Create DataStore instance
    private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = SETTINGS_DATASTORE_NAME
    )
    
    private val dataStore: DataStore<Preferences> = context.settingsDataStore
    
    // Playback Settings Flows
    val videoQuality: Flow<VideoQualityPreference> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val value = preferences[PreferenceKeys.VIDEO_QUALITY] ?: PreferenceDefaults.VIDEO_QUALITY
            VideoQualityPreference.fromValue(value)
        }
    
    val playbackSpeed: Flow<PlaybackSpeedPreference> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val value = preferences[PreferenceKeys.PLAYBACK_SPEED] ?: PreferenceDefaults.PLAYBACK_SPEED
            PlaybackSpeedPreference.fromValue(value)
        }
    
    val subtitlesEnabled: Flow<Boolean> = getPreferenceFlow(
        PreferenceKeys.SUBTITLES_ENABLED, 
        PreferenceDefaults.SUBTITLES_ENABLED
    )
    
    val subtitleLanguage: Flow<String> = getPreferenceFlow(
        PreferenceKeys.SUBTITLE_LANGUAGE,
        PreferenceDefaults.SUBTITLE_LANGUAGE
    )
    
    val autoPlay: Flow<Boolean> = getPreferenceFlow(
        PreferenceKeys.AUTO_PLAY,
        PreferenceDefaults.AUTO_PLAY
    )
    
    val autoPlayDelay: Flow<Int> = getPreferenceFlow(
        PreferenceKeys.AUTO_PLAY_DELAY,
        PreferenceDefaults.AUTO_PLAY_DELAY_SECONDS
    )
    
    val externalPlayerPackage: Flow<String> = getPreferenceFlow(
        PreferenceKeys.EXTERNAL_PLAYER_PACKAGE,
        PreferenceDefaults.EXTERNAL_PLAYER_PACKAGE
    )
    
    // Display Settings Flows
    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val value = preferences[PreferenceKeys.THEME_MODE] ?: PreferenceDefaults.THEME_MODE
            ThemeMode.fromValue(value)
        }
    
    val contentLayout: Flow<ContentLayout> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val value = preferences[PreferenceKeys.CONTENT_LAYOUT] ?: PreferenceDefaults.CONTENT_LAYOUT
            ContentLayout.fromValue(value)
        }
    
    val showQualityBadges: Flow<Boolean> = getPreferenceFlow(
        PreferenceKeys.SHOW_QUALITY_BADGES,
        PreferenceDefaults.SHOW_QUALITY_BADGES
    )
    
    // Network Settings Flows
    val bandwidthLimit: Flow<BandwidthLimit> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val mbps = preferences[PreferenceKeys.BANDWIDTH_LIMIT_MBPS] ?: PreferenceDefaults.BANDWIDTH_LIMIT_MBPS
            BandwidthLimit.fromMbps(mbps)
        }
    
    val connectionTimeout: Flow<Int> = getPreferenceFlow(
        PreferenceKeys.CONNECTION_TIMEOUT_SECONDS,
        PreferenceDefaults.CONNECTION_TIMEOUT_SECONDS
    )
    
    // Storage Settings Flows
    val cacheSizeLimit: Flow<Long> = getPreferenceFlow(
        PreferenceKeys.CACHE_SIZE_LIMIT_MB,
        PreferenceDefaults.CACHE_SIZE_LIMIT_MB
    )
    
    val autoDeleteWatched: Flow<Boolean> = getPreferenceFlow(
        PreferenceKeys.AUTO_DELETE_WATCHED,
        PreferenceDefaults.AUTO_DELETE_WATCHED
    )
    
    // Account Settings Flows
    val showPremiumDays: Flow<Boolean> = getPreferenceFlow(
        PreferenceKeys.SHOW_PREMIUM_DAYS,
        PreferenceDefaults.SHOW_PREMIUM_DAYS
    )
    
    val deviceName: Flow<String> = getPreferenceFlow(
        PreferenceKeys.DEVICE_NAME,
        PreferenceDefaults.DEVICE_NAME
    )
    
    // Parental Control Flows
    val parentalControlsEnabled: Flow<Boolean> = getPreferenceFlow(
        PreferenceKeys.PARENTAL_CONTROLS_ENABLED,
        PreferenceDefaults.PARENTAL_CONTROLS_ENABLED
    )
    
    val parentalMaxRating: Flow<ParentalRating> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val value = preferences[PreferenceKeys.PARENTAL_MAX_RATING] ?: PreferenceDefaults.PARENTAL_MAX_RATING
            ParentalRating.fromValue(value)
        }
    
    // Notification Settings Flows
    val notificationsEnabled: Flow<Boolean> = getPreferenceFlow(
        PreferenceKeys.NOTIFICATIONS_ENABLED,
        PreferenceDefaults.NOTIFICATIONS_ENABLED
    )
    
    // Update functions
    suspend fun updateVideoQuality(quality: VideoQualityPreference) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.VIDEO_QUALITY] = quality.value
        }
    }
    
    suspend fun updatePlaybackSpeed(speed: PlaybackSpeedPreference) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PLAYBACK_SPEED] = speed.value
        }
    }
    
    suspend fun updateSubtitlesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SUBTITLES_ENABLED] = enabled
        }
    }
    
    suspend fun updateAutoPlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_PLAY] = enabled
        }
    }
    
    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = mode.value
        }
    }
    
    suspend fun updateParentalControlsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PARENTAL_CONTROLS_ENABLED] = enabled
        }
    }
    
    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun updateBandwidthLimit(limit: BandwidthLimit) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BANDWIDTH_LIMIT_MBPS] = limit.mbps
        }
    }
    
    /**
     * Set parental control PIN with secure hashing
     */
    suspend fun setParentalPin(pin: String): Boolean {
        return try {
            val salt = generateSalt()
            val hash = hashPin(pin, salt)
            
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.PARENTAL_PIN_HASH] = hash
                preferences[PreferenceKeys.PARENTAL_PIN_SALT] = salt
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verify parental control PIN
     */
    suspend fun verifyParentalPin(pin: String): Boolean {
        return try {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.map { it }.first()
            val storedHash = prefs[PreferenceKeys.PARENTAL_PIN_HASH]
            val storedSalt = prefs[PreferenceKeys.PARENTAL_PIN_SALT]
            
            if (storedHash == null || storedSalt == null) {
                return false
            }
            
            val hash = hashPin(pin, storedSalt)
            hash == storedHash
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Reset all settings to defaults
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
            // Set version to current
            preferences[PreferenceKeys.SETTINGS_VERSION] = CURRENT_SETTINGS_VERSION
        }
    }
    
    /**
     * Reset settings for sign out
     */
    suspend fun resetForSignOut() {
        dataStore.edit { preferences ->
            PreferenceCategories.getSignOutResetKeys().forEach { key ->
                preferences.remove(key)
            }
        }
    }
    
    /**
     * Export settings for backup
     */
    suspend fun exportSettings(): SettingsBackup {
        return try {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.map { it }.first()
            val exportMap = mutableMapOf<String, Any>()
            
            PreferenceCategories.getBackupKeys().forEach { key ->
                prefs[key]?.let { value ->
                    exportMap[key.name] = value
                }
            }
            
            SettingsBackup(
                version = CURRENT_SETTINGS_VERSION,
                timestamp = System.currentTimeMillis(),
                deviceName = prefs[PreferenceKeys.DEVICE_NAME],
                preferences = exportMap
            )
        } catch (e: Exception) {
            SettingsBackup(CURRENT_SETTINGS_VERSION, System.currentTimeMillis(), null, emptyMap())
        }
    }
    
    /**
     * Import settings from backup
     */
    suspend fun importSettings(backup: SettingsBackup): Boolean {
        return try {
            // Validate version compatibility
            if (backup.version > CURRENT_SETTINGS_VERSION) {
                return false
            }
            
            dataStore.edit { preferences ->
                // Clear existing preferences (except parental controls)
                preferences.asMap().keys
                    .filter { it !in PreferenceCategories.parentalKeys }
                    .forEach { preferences.remove(it) }
                
                // Import backed up preferences
                backup.preferences.forEach { (key, value) ->
                    // Skip parental control keys for security
                    if (key.startsWith("parental_")) return@forEach
                    
                    when (value) {
                        is String -> preferences[stringPreferencesKey(key)] = value
                        is Int -> preferences[intPreferencesKey(key)] = value
                        is Long -> preferences[longPreferencesKey(key)] = value
                        is Float -> preferences[floatPreferencesKey(key)] = value
                        is Boolean -> preferences[booleanPreferencesKey(key)] = value
                    }
                }
                
                // Update metadata
                preferences[PreferenceKeys.LAST_BACKUP_TIMESTAMP] = backup.timestamp
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if this is the first launch
     */
    suspend fun isFirstLaunch(): Boolean {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                !(preferences[PreferenceKeys.FIRST_LAUNCH_COMPLETED] ?: false)
            }
            .first()
    }
    
    /**
     * Mark first launch as completed
     */
    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FIRST_LAUNCH_COMPLETED] = true
            preferences[PreferenceKeys.SETTINGS_VERSION] = CURRENT_SETTINGS_VERSION
        }
    }
    
    // Helper functions
    
    private fun <T> getPreferenceFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }
    }
    
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }
    
    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$pin$salt".toByteArray()
        val hash = md.digest(input)
        return hash.joinToString("") { "%02x".format(it) }
    }
}