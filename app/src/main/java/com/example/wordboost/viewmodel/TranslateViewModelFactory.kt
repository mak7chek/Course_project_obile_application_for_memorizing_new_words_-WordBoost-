package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.TranslationRepository


class TranslateViewModelFactory(
    private val firebaseRepository: FirebaseRepository,
    private val translationRepository: TranslationRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TranslateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TranslateViewModel(firebaseRepository, translationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}