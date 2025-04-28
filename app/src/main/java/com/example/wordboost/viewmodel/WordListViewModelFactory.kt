package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.viewmodel.WordListViewModel


class WordListViewModelFactory(private val repository: FirebaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}