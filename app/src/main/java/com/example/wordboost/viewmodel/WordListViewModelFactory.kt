package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.viewmodel.WordListViewModel
import com.example.wordboost.data.tts.TextToSpeechService
import com.example.wordboost.data.firebase.*

class WordListViewModelFactory(
    private val repository: FirebaseRepository,
    private val ttsService: TextToSpeechService,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordListViewModel(repository, ttsService,authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}