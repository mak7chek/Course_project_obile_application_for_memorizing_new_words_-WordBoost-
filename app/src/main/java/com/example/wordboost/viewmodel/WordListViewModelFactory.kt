package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.firebase.FirebaseRepository // Імпорт FirebaseRepo
import com.example.wordboost.viewmodel.WordListViewModel // Імпорт WordListViewModel


// Factory для створення WordListViewModel
class WordListViewModelFactory(private val repository: FirebaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Перевіряємо, чи запитуваний клас ViewModel є WordListViewModel
        if (modelClass.isAssignableFrom(WordListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Створюємо екземпляр WordListViewModel, передаючи отриманий репозиторій
            return WordListViewModel(repository) as T
        }
        // Якщо запитують інший ViewModel, кидаємо виняток
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}