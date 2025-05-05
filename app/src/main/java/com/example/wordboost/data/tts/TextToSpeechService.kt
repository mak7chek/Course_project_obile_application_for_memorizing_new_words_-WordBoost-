package com.example.wordboost.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechService(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        // Ініціалізуємо TextToSpeech двигун асинхронно
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Мова не підтримується")
                    // TODO: Можливо, повідомити користувача або спробувати іншу мову
                } else {
                    isTtsInitialized = true
                    Log.d("TTS", "TTS успішно ініціалізовано")
                }
            } else {
                Log.e("TTS", "Помилка ініціалізації TTS")
            }
        }
    }

    /**
     * Озвучує наданий текст.
     */
    fun speak(text: String) {
        if (isTtsInitialized && tts != null) {
            // Використовуємо QUEUE_FLUSH, щоб перервати попереднє озвучування, якщо воно було
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d("TTS", "Озвучуємо: $text")
        } else {
            Log.w("TTS", "TTS не ініціалізовано або недоступно")
            // TODO: Можливо, встановити статус повідомлення в ViewModel
        }
    }

    /**
     * Зупиняє озвучування.
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Звільняє ресурси TextToSpeech двигуна. Повинно викликатись, коли сервіс більше не потрібен.
     */
    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
        Log.d("TTS", "TTS звільнено")
    }
}