package com.example.wordboost.viewmodel // Переконайтесь, що пакет правильний

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.repository.PracticeRepository // Імпорт репозиторію
import com.example.wordboost.data.firebase.AuthRepository // Імпорт репозиторію
import com.example.wordboost.data.tts.TextToSpeechService // Імпорт сервісу

class PracticeViewModelFactory(
    private val repository: PracticeRepository, // Приймаємо PracticeRepository
    private val ttsService: TextToSpeechService, // Приймаємо TextToSpeechService
    private val authRepository: AuthRepository // Приймаємо AuthRepository
) : ViewModelProvider.Factory {

    // Перевизначаємо метод create для створення ViewModel
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Перевіряємо, чи запитується саме PracticeViewModel
        if (modelClass.isAssignableFrom(PracticeViewModel::class.java)) {
            // Створюємо PracticeViewModel, передаючи необхідні залежності
            return PracticeViewModel(repository, ttsService, authRepository) as T
        }
        // Викидаємо виняток, якщо запитується ViewModel іншого типу
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}