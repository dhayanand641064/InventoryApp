package com.example.shan_inventory.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AudioManager handles speech recognition functionality
 * Provides methods to start/stop speech recognition and convert audio to text
 */
class AudioManager(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _errorMessage.value = null
        }
        
        override fun onBeginningOfSpeech() {
            // Speech started
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }
        
        override fun onEndOfSpeech() {
            _isListening.value = false
        }
        
        override fun onError(error: Int) {
            _isListening.value = false
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                SpeechRecognizer.ERROR_SERVER -> "Server sends error status"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error occurred"
            }
            _errorMessage.value = errorMessage
        }
        
        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _recognizedText.value = matches[0]
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _recognizedText.value = matches[0]
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            // Event occurred
        }
    }
    
    /**
     * Check if audio recording permission is granted
     */
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if speech recognition is available on the device
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Start speech recognition
     */
    fun startListening() {
        if (!hasAudioPermission()) {
            _errorMessage.value = "Audio recording permission is required"
            return
        }
        
        if (!isSpeechRecognitionAvailable()) {
            _errorMessage.value = "Speech recognition is not available on this device"
            return
        }
        
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to start speech recognition: ${e.message}"
        }
    }
    
    /**
     * Stop speech recognition
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }
    
    /**
     * Cancel speech recognition
     */
    fun cancelListening() {
        speechRecognizer?.cancel()
        _isListening.value = false
    }
    
    /**
     * Clear recognized text
     */
    fun clearRecognizedText() {
        _recognizedText.value = ""
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

/**
 * Composable function to handle speech recognition
 */
@Composable
fun rememberAudioManager(
    context: Context
): AudioManager {
    val audioManager = remember { AudioManager(context) }
    
    DisposableEffect(audioManager) {
        onDispose {
            audioManager.cleanup()
        }
    }
    
    return audioManager
}
