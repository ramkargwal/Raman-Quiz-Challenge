package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class SpeechManager(context: Context, onSpeakingStateChanged: (Boolean) -> Unit = {}) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                val hiLocale = Locale("hi", "IN")
                val result = tts?.setLanguage(hiLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to generic Hindi or default
                    tts?.setLanguage(Locale("hi"))
                }
                tts?.setSpeechRate(0.85f)
                tts?.setPitch(1.0f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingStateChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }
                })
            }
        }
    }

    fun speak(text: String) {
        if (!isInitialized || tts == null) return
        stop()
        // Strip $ signs, LaTeX formatting punctuation, and symbol/emoji characters so TTS doesn't read "dollar"
        val clean = text
            .replace("$", "")
            .replace(Regex("""[\\{}^_*~]"""), " ")
            .replace(Regex("[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Cn}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "QUESTION_UTTERANCE")
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
