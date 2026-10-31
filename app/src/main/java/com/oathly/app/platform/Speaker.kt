package com.oathly.app.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Reads questions aloud with the phone's built-in, offline text-to-speech. */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Triple<String, Locale, Float>? = null

    var available: Boolean = true
        private set

    private fun ensure() {
        if (tts == null) tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        available = ready
        pending?.let { (text, locale, rate) -> pending = null; speak(text, locale, rate) }
    }

    fun speak(text: String, locale: Locale, rate: Float) {
        ensure()
        val engine = tts ?: return
        if (!ready) { pending = Triple(text, locale, rate); return }
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setSpeechRate(rate)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "oathly")
    }

    fun stop() {
        pending = null
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
