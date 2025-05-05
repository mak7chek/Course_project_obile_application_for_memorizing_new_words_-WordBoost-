package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.viewmodel.WordListViewModel
import com.example.wordboost.data.tts.TextToSpeechService


class WordListViewModelFactory(
    private val repository: FirebaseRepository,
    private val ttsService: TextToSpeechService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordListViewModel(repository, ttsService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}