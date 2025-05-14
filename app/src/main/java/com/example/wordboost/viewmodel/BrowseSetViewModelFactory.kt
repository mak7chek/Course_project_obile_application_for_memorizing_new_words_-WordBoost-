package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.tts.TextToSpeechService
import com.example.wordboost.viewmodel.BrowseSharedSetViewModel

@Suppress("UNCHECKED_CAST")
class BrowseSetViewModelFactory(
    private val sharedSetId: String,
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val ttsService: TextToSpeechService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowseSharedSetViewModel::class.java)) {
            return BrowseSharedSetViewModel(
                sharedSetId = sharedSetId,
                firebaseRepository = firebaseRepository,
                authRepository = authRepository,
                ttsService = ttsService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for BrowseSetViewModelFactory")
    }
}