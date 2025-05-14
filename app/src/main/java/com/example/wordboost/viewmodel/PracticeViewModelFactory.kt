package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.tts.TextToSpeechService

class PracticeViewModelFactory(
    private val repository: PracticeRepository,
    private val ttsService: TextToSpeechService,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeViewModel::class.java)) {
            return PracticeViewModel(repository, ttsService, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}