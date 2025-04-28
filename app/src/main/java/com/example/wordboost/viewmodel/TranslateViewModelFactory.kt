package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.firebase.FirebaseRepository // <-- Потрібен FirebaseRepo
import com.example.wordboost.data.repository.TranslationRepository // <-- Потрібен TranslationRepo


class TranslateViewModelFactory(
    private val firebaseRepository: FirebaseRepository, // Приймаємо FirebaseRepo
    private val translationRepository: TranslationRepository // Приймаємо TranslationRepo
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TranslateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Створюємо ViewModel, передаючи обидва репозиторії
            return TranslateViewModel(firebaseRepository, translationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}