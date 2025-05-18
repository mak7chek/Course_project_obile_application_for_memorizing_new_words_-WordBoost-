package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository

@Suppress("UNCHECKED_CAST")
class ArticleViewModelFactory(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetsViewModel::class.java)) {
            return ArticleViewModel(firebaseRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for SetsViewModelFactory")
    }
}

