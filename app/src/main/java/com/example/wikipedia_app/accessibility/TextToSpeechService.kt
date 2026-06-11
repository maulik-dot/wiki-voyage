package com.example.wikipedia_app.accessibility

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Text-to-speech wrapper that reads long articles reliably.
 *
 * The text is split at sentence/word boundaries and the chunks are queued up
 * front with QUEUE_ADD, so the engine plays straight through without us having
 * to re-trigger each chunk from a callback (the old approach stalled mid-article
 * whenever a single onDone/onError was missed). [isSpeaking] clears only when the
 * final chunk finishes (or on stop/error of the final chunk).
 */
class TextToSpeechService(
    private val context: Context,
    private val initialRate: Float = 1.0f,
    private val initialPitch: Float = 1.0f
) {
    private var textToSpeech: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    @Volatile private var ready = false
    @Volatile private var lastUtteranceId: String? = null
    // Text requested before the engine finished initialising — spoken on init.
    @Volatile private var pendingText: String? = null

    private var rate = initialRate
    private var pitch = initialPitch

    init {
        initializeTTS()
    }

    fun setSpeechRate(value: Float) {
        rate = value
        textToSpeech?.setSpeechRate(value)
    }

    fun setSpeechPitch(value: Float) {
        pitch = value
        textToSpeech?.setPitch(value)
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e("TTS", "TextToSpeech initialization failed ($status)")
                return@TextToSpeech
            }
            val tts = textToSpeech ?: return@TextToSpeech
            when (tts.setLanguage(Locale.getDefault())) {
                TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                    // Fall back to US English if the device locale isn't available.
                    tts.setLanguage(Locale.US)
                }
            }
            tts.setSpeechRate(rate)
            tts.setPitch(pitch)
            ready = true
            // Honour a read request that arrived before init completed.
            pendingText?.let { text -> pendingText = null; speak(text) }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                // Only the final chunk ending means the whole article is done.
                if (utteranceId == lastUtteranceId) _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e("TTS", "Error speaking utterance: $utteranceId")
                // Don't kill playback for a single chunk error — the engine moves
                // on to the next queued chunk. Only clear state if the last failed.
                if (utteranceId == lastUtteranceId) _isSpeaking.value = false
            }
        })
    }

    fun speak(text: String) {
        val tts = textToSpeech
        if (tts == null || !ready) {
            pendingText = text // speak once the engine is initialised
            return
        }
        val chunks = chunkText(text)
        if (chunks.isEmpty()) return

        lastUtteranceId = "wv_${chunks.size - 1}"
        _isSpeaking.value = true
        chunks.forEachIndexed { index, chunk ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(chunk, mode, null, "wv_$index")
        }
    }

    fun stop() {
        pendingText = null
        lastUtteranceId = null
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        pendingText = null
        lastUtteranceId = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
    }

    /**
     * Splits [text] into chunks no longer than [MAX_CHUNK], breaking at a sentence
     * end where possible and otherwise at a word boundary, so nothing is cut mid-word.
     */
    private fun chunkText(text: String): List<String> {
        val chunks = ArrayList<String>()
        var i = 0
        val n = text.length
        while (i < n) {
            var end = minOf(i + MAX_CHUNK, n)
            if (end < n) {
                val window = text.substring(i, end)
                val sentenceCut = window.lastIndexOfAny(charArrayOf('.', '!', '?', '\n', ';'))
                val cut = when {
                    sentenceCut >= MAX_CHUNK / 3 -> sentenceCut
                    else -> window.lastIndexOf(' ')
                }
                if (cut > 0) end = i + cut + 1
            }
            text.substring(i, end).trim().takeIf { it.isNotEmpty() }?.let { chunks.add(it) }
            i = end
        }
        return chunks
    }

    companion object {
        private const val MAX_CHUNK = 480
    }
}
