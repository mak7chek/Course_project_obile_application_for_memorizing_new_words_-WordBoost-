package com.example.wordboost.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class TextToSpeechService(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // !!! Мова для озвучення - АНГЛІЙСЬКА !!!
    private val desiredLocale: Locale = Locale.ENGLISH // !!! Встановлюємо англійську !!!


    init {
        // Ініціалізуємо TextToSpeech двигун
        Log.d("TTS", "Initializing TTS engine.")
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TTS", "TTS engine initialized successfully.")
            // !!! Спробуємо встановити бажану мову (англійську) !!!
            val result = tts?.setLanguage(desiredLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Мова $desiredLocale не підтримується TTS двигуном або відсутні дані.")
                isTtsReady = false // TTS не готовий, якщо мова не підтримується
            } else {
                isTtsReady = true
                Log.d("TTS", "TTS двигун готовий. Мова встановлена на $desiredLocale.")
            }
        } else {
            Log.e("TTS", "Помилка ініціалізації TTS двигуна. Код помилки: $status")
            isTtsReady = false // TTS не готовий при помилці ініціалізації
        }

        // Налаштовуємо слухача для відстеження завершення озвучення (не обов'язково для цієї проблеми, але корисно)
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
            Log.d("TTS", "Спроба озвучити текст: \"$text\"") // Додано лог
            // Використовуємо QUEUE_FLUSH, щоб зупинити попереднє озвучення і почати нове
            // UtteranceId можна використовувати для відстеження прогресу, але тут не обов'язково
            // Додано перевірку на порожній текст перед speak
            if (text.isNotBlank()) {
                val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text) // Використовуємо текст як utteranceId
                if (result == TextToSpeech.ERROR) {
                    Log.e("TTS", "Помилка виклику speak() для тексту: \"$text\"") // Додано лог
                }
            } else {
                Log.w("TTS", "Надано порожній текст для озвучення.") // Додано лог
            }
        } else {
            Log.w("TTS", "TTS двигун не готовий або мова не підтримується. Не можу озвучити текст: \"$text\"") // Додано лог
        }
    }

    fun stop() {
        // Зупиняємо, тільки якщо TTS ініціалізовано
        if (tts != null) {
            tts?.stop()
            Log.d("TTS", "TTS зупинено.") // Додано лог
        }
    }

    fun shutdown() {
        tts?.apply {
            stop()
            shutdown() // Використовуємо shutdown для коректного звільнення ресурсів
            Log.d("TTS", "TTS вимкнено.") // Додано лог
        }
        tts = null
        isTtsReady = false
    }
}