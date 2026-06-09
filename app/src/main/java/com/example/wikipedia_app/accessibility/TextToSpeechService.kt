package com.example.wikipedia_app.accessibility

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class TextToSpeechService(
    private val context: Context,
    private val initialRate: Float = 1.0f,
    private val initialPitch: Float = 1.0f
) {
    private var textToSpeech: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking
    private var currentText: String = ""
    private var currentIndex: Int = 0
    private val CHUNK_SIZE = 1000

    init {
        initializeTTS()
    }

    fun setSpeechRate(rate: Float) { textToSpeech?.setSpeechRate(rate) }
    fun setSpeechPitch(pitch: Float) { textToSpeech?.setPitch(pitch) }

    private fun initializeTTS() {
        Log.d("TTS", "Initializing TextToSpeech")
        textToSpeech = TextToSpeech(context) { status ->
            when (status) {
                TextToSpeech.SUCCESS -> {
                    Log.d("TTS", "TextToSpeech initialization successful")
                    val result = textToSpeech?.setLanguage(Locale.getDefault())
                    when (result) {
                        TextToSpeech.LANG_MISSING_DATA -> {
                            Log.e("TTS", "Language data missing")
                        }
                        TextToSpeech.LANG_NOT_SUPPORTED -> {
                            Log.e("TTS", "Language not supported")
                        }
                        else -> {
                            Log.d("TTS", "Language set successfully")
                            textToSpeech?.setSpeechRate(initialRate)
                            textToSpeech?.setPitch(initialPitch)
                        }
                    }
                }
                TextToSpeech.ERROR -> {
                    Log.e("TTS", "TextToSpeech initialization failed")
                }
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TTS", "Started speaking: $utteranceId")
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                Log.d("TTS", "Finished speaking: $utteranceId")
                if (currentIndex < currentText.length) {
                    // Speak next chunk
                    speakNextChunk()
                } else {
                    _isSpeaking.value = false
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e("TTS", "Error while speaking: $utteranceId")
                _isSpeaking.value = false
            }
        })
    }

    private fun speakNextChunk() {
        if (currentIndex >= currentText.length) {
            _isSpeaking.value = false
            return
        }

        val endIndex = minOf(currentIndex + CHUNK_SIZE, currentText.length)
        val chunk = currentText.substring(currentIndex, endIndex)
        currentIndex = endIndex

        Log.d("TTS", "Speaking chunk of length: ${chunk.length}")
        val result = textToSpeech?.speak(
            chunk,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "TTS_UTTERANCE_ID_$currentIndex"
        )

        when (result) {
            TextToSpeech.ERROR -> {
                Log.e("TTS", "Error speaking chunk")
                _isSpeaking.value = false
            }
            TextToSpeech.SUCCESS -> {
                Log.d("TTS", "Successfully queued chunk for speaking")
            }
            else -> {
                Log.e("TTS", "Unknown result code: $result")
                _isSpeaking.value = false
            }
        }
    }

    fun speak(text: String) {
        Log.d("TTS", "Attempting to speak text of length: ${text.length}")
        if (textToSpeech == null) {
            Log.e("TTS", "TextToSpeech is null")
            return
        }

        // Reset state
        currentText = text
        currentIndex = 0
        _isSpeaking.value = true

        // Start speaking first chunk
        speakNextChunk()
    }

    fun stop() {
        Log.d("TTS", "Stopping speech")
        textToSpeech?.stop()
        currentIndex = currentText.length // Prevent further chunks from being spoken
        _isSpeaking.value = false
    }

    fun shutdown() {
        Log.d("TTS", "Shutting down TextToSpeech")
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
} 