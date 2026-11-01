package com.autasktic.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class OnDeviceVoiceRecognizer(
    context: Context,
    private val onStateChanged: (String) -> Unit,
    private val onText: (String) -> Unit,
) {
    private val recognizer: SpeechRecognizer? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            null
        }

    init {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onStateChanged("Listening…")
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                onStateChanged("Processing…")
            }

            override fun onError(error: Int) {
                onStateChanged("Could not understand that. Try again.")
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (text.isNullOrBlank()) {
                    onStateChanged("No speech detected. Try again.")
                } else {
                    onText(text)
                    onStateChanged("Result ready")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    fun isAvailable(): Boolean = recognizer != null

    fun start() {
        val speechRecognizer = recognizer ?: return
        speechRecognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.UK.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            },
        )
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
    }
}
