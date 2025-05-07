package com.example.wordboost.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.tts.TextToSpeechService
import com.example.wordboost.data.firebase.AuthRepository
class PracticeViewModelFactory(
    private val practiceRepository: PracticeRepository,
    private val ttsServise: TextToSpeechService,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PracticeViewModel(practiceRepository,ttsServise,authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}