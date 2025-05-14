package com.example.wordboost.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class TextToSpeechService(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val desiredLocale: Locale = Locale.ENGLISH


    init {
        Log.d("TTS", "Initializing TTS engine.")
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TTS", "TTS engine initialized successfully.")
            val result = tts?.setLanguage(desiredLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Мова $desiredLocale не підтримується TTS двигуном або відсутні дані.")
                isTtsReady = false
            } else {
                isTtsReady = true
            }
        } else {
            isTtsReady = false
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TTS", "Початок озвучення: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d("TTS", "Завершення озвучення: $utteranceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e("TTS", "Помилка озвучення (стара версія): $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e("TTS", "Помилка озвучення: $utteranceId, код: $errorCode")
            }
        })
    }

    fun speak(text: String) {
        if (isTtsReady) {
            Log.d("TTS", "Спроба озвучити текст: \"$text\"")
            if (text.isNotBlank()) {
                val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
                if (result == TextToSpeech.ERROR) {
                    Log.e("TTS", "Помилка виклику speak() для тексту: \"$text\"")
                }
            } else {
                Log.w("TTS", "Надано порожній текст для озвучення.")
            }
        } else {
            Log.w("TTS", "TTS двигун не готовий або мова не підтримується. Не можу озвучити текст: \"$text\"")
        }
    }

    fun stop() {
        if (tts != null) {
            tts?.stop()
            Log.d("TTS", "TTS зупинено.")
        }
    }

    fun shutdown() {
        tts?.apply {
            stop()
            shutdown()
            Log.d("TTS", "TTS вимкнено.")
        }
        tts = null
        isTtsReady = false
    }
}