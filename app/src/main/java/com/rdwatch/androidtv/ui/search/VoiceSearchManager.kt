package com.rdwatch.androidtv.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages voice recognition for search functionality
 * Integrates with Android SpeechRecognizer API
 */
@Singleton
class VoiceSearchManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    private val _voiceSearchState = MutableStateFlow(VoiceSearchState.IDLE)
    val voiceSearchState: StateFlow<VoiceSearchState> = _voiceSearchState.asStateFlow()
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _partialResults = MutableStateFlow("")
    val partialResults: StateFlow<String> = _partialResults.asStateFlow()
    
    /**
     * Check if voice search is available on the device
     */
    fun isVoiceSearchAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Start voice recognition with optimized settings for TV
     */
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startVoiceRecognition(
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {},
        language: String = Locale.getDefault().language
    ) {
        if (isListening) {
            stopVoiceRecognition()
        }
        
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            if (speechRecognizer == null) {
                val errorMsg = "Speech recognition not available"
                _error.value = errorMsg
                onError(errorMsg)
                return
            }
            
            val intent = createRecognitionIntent(language)
            val listener = createRecognitionListener(onResult, onError)
            
            speechRecognizer?.setRecognitionListener(listener)
            speechRecognizer?.startListening(intent)
            
            isListening = true
            _voiceSearchState.value = VoiceSearchState.LISTENING
            _error.value = null
            _partialResults.value = ""
            _recognizedText.value = ""
            
        } catch (e: SecurityException) {
            val errorMsg = "Microphone permission required"
            _error.value = errorMsg
            _voiceSearchState.value = VoiceSearchState.ERROR
            onError(errorMsg)
        } catch (e: Exception) {
            val errorMsg = "Failed to start voice recognition: ${e.message}"
            _error.value = errorMsg
            _voiceSearchState.value = VoiceSearchState.ERROR
            onError(errorMsg)
        }
    }
    
    /**
     * Stop voice recognition
     */
    fun stopVoiceRecognition() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            _voiceSearchState.value = VoiceSearchState.IDLE
            _partialResults.value = ""
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Cancel voice recognition
     */
    fun cancelVoiceRecognition() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            _voiceSearchState.value = VoiceSearchState.IDLE
            _partialResults.value = ""
            _recognizedText.value = ""
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Create optimized recognition intent for TV search
     */
    private fun createRecognitionIntent(language: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Use web search model for better accuracy with movie/show names
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            
            // Set language
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            
            // Request partial results for real-time feedback
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            // Optimize for single phrase (movie/show titles)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            
            // Prompt for TV context
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the name of a movie or TV show")
            
            // Disable offensive words filtering for movie titles
            putExtra("android.speech.extra.DICTATION_MODE", false)
            
            // Enable confidence scores
            putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
            
            // Prefer offline recognition if available
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            
            // Set recognition timeout (TV users may speak slowly)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }
    }
    
    /**
     * Create recognition listener with TV-optimized callbacks
     */
    private fun createRecognitionListener(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ): RecognitionListener {
        return object : RecognitionListener {
            
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceSearchState.value = VoiceSearchState.LISTENING
            }
            
            override fun onBeginningOfSpeech() {
                _voiceSearchState.value = VoiceSearchState.PROCESSING
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Could be used for visual feedback (volume level indicator)
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received - could be used for waveform visualization
            }
            
            override fun onEndOfSpeech() {
                _voiceSearchState.value = VoiceSearchState.PROCESSING
            }
            
            override fun onError(error: Int) {
                isListening = false
                _voiceSearchState.value = VoiceSearchState.ERROR
                
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found - try speaking more clearly"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected - try speaking louder"
                    else -> "Voice recognition error"
                }
                
                _error.value = errorMessage
                onError(errorMessage)
                
                // Auto-cleanup
                stopVoiceRecognition()
            }
            
            override fun onResults(results: Bundle?) {
                isListening = false
                _voiceSearchState.value = VoiceSearchState.COMPLETED
                
                try {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    
                    if (!matches.isNullOrEmpty()) {
                        // Get the best result with highest confidence
                        val bestResult = if (confidenceScores != null && confidenceScores.isNotEmpty()) {
                            val bestIndex = confidenceScores.indices.maxByOrNull { confidenceScores[it] } ?: 0
                            matches[bestIndex]
                        } else {
                            matches[0]
                        }
                        
                        _recognizedText.value = bestResult
                        onResult(bestResult)
                    } else {
                        val errorMsg = "No speech recognized"
                        _error.value = errorMsg
                        onError(errorMsg)
                    }
                } catch (e: Exception) {
                    val errorMsg = "Error processing speech results: ${e.message}"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
                
                // Auto-cleanup
                stopVoiceRecognition()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                try {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _partialResults.value = matches[0]
                    }
                } catch (e: Exception) {
                    // Ignore partial result errors
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle any additional events if needed
            }
        }
    }
    
    /**
     * Get suggestions for voice search prompts based on context
     */
    fun getVoiceSearchPrompts(): List<String> {
        return listOf(
            "Try saying: \"The Matrix\"",
            "Try saying: \"Breaking Bad\"", 
            "Try saying: \"Inception\"",
            "Try saying: \"Game of Thrones\"",
            "Try saying: \"Marvel movies\"",
            "Try saying: \"Comedy movies\""
        )
    }
    
    /**
     * Check if device has a microphone
     */
    fun hasMicrophone(): Boolean {
        return try {
            context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_MICROPHONE)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopVoiceRecognition()
    }
}

/**
 * States for voice search process
 */
enum class VoiceSearchState {
    IDLE,           // Not active
    LISTENING,      // Actively listening for speech
    PROCESSING,     // Processing speech input
    COMPLETED,      // Recognition completed successfully
    ERROR           // Error occurred
}

/**
 * Voice search configuration for different contexts
 */
data class VoiceSearchConfig(
    val language: String = Locale.getDefault().language,
    val maxResults: Int = 5,
    val timeoutMs: Long = 10000L,
    val partialResults: Boolean = true,
    val offlinePreferred: Boolean = false,
    val profanityFilter: Boolean = false
)