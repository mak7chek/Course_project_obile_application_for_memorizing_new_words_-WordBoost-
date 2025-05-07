package com.example.wordboost.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordboost.data.firebase.FirebaseRepository


class EditWordViewModelFactory(
    private val repository: FirebaseRepository,
    private val wordId: String?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditWordViewModel::class.java)) {
            return EditWordViewModel(repository, wordId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}