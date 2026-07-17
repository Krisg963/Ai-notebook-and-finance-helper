package com.example.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val recognitionIntent: Intent

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableStateFlow("")
    val finalText: StateFlow<String> = _finalText.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "no-NO") // Default to Norwegian
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "no-NO")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "no-NO")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorState.value = "Talegjenkjenning er ikke tilgjengelig på denne enheten."
            return
        }

        stopListening() // Make sure to clean up any prior instance

        _errorState.value = null
        _partialText.value = ""
        _finalText.value = ""

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Lydopptaksfeil."
                        SpeechRecognizer.ERROR_CLIENT -> "Klientfeil."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mangler tillatelse til lydopptak."
                        SpeechRecognizer.ERROR_NETWORK -> "Nettverksfeil under talegjenkjenning."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tidsavbrudd for nettverk."
                        SpeechRecognizer.ERROR_NO_MATCH -> "Fant ingen match for det du sa. Prøv igjen."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Taletjenesten er opptatt."
                        SpeechRecognizer.ERROR_SERVER -> "Serverfeil."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ingen tale hørt. Vennligst snakk tydelig."
                        else -> "Ukjent feil under talegjenkjenning."
                    }
                    _errorState.value = message
                    Log.e("SpeechRecognizerHelper", "Error code: $error - $message")
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val result = matches[0]
                        _finalText.value = result
                        _partialText.value = result
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _partialText.value = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(recognitionIntent)
        }
    }

    fun stopListening() {
        _isListening.value = false
        speechRecognizer?.let {
            it.stopListening()
            it.destroy()
        }
        speechRecognizer = null
    }

    fun cancel() {
        _isListening.value = false
        speechRecognizer?.let {
            it.cancel()
            it.destroy()
        }
        speechRecognizer = null
    }
}
