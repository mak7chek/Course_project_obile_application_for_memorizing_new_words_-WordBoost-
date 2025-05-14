package com.example.wordboost.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.model.Group

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first


class EditWordViewModel(
    private val repository: FirebaseRepository,
    private val wordId: String?
) : ViewModel() {

    private val _word = MutableStateFlow<Word?>(null)
    val word: StateFlow<Word?> get() = _word.asStateFlow()

    private val _editedText = MutableStateFlow("")
    val editedText: StateFlow<String> get() = _editedText.asStateFlow()

    private val _editedTranslation = MutableStateFlow("")
    val editedTranslation: StateFlow<String> get() = _editedTranslation.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> get() = _selectedGroupId.asStateFlow()


    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> get() = _groups.asStateFlow()


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> get() = _saveSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage.asStateFlow()


    init {
        Log.d("EditWordVM", "ViewModel initialized for word ID: $wordId")
        if (wordId != null) {
            loadWord(wordId)
            loadGroups()
        } else {
            _isLoading.value = false
            _errorMessage.value = "ID слова для редагування відсутній."
            Log.e("EditWordVM", "Word ID is null. Cannot load word for editing.")
        }
    }


    private fun loadWord(id: String) {
        _isLoading.value = true
        _errorMessage.value = null
        Log.d("EditWordVM", "Loading word with ID: $id")

        repository.getWordById(id) { word ->
            if (word != null && word.id == id) {
                _word.value = word
                _editedText.value = word.text
                _editedTranslation.value = word.translation
                _selectedGroupId.value = word.dictionaryId
                _isLoading.value = false
                Log.d("EditWordVM", "Word loaded: ${word.text}")
            } else {
                _word.value = null
                _isLoading.value = false
                _errorMessage.value = "Не вдалося завантажити слово для редагування."
                Log.e("EditWordVM", "Word with ID $id not found or null.")
            }
        }
    }


    private fun loadGroups() {
        repository.getGroups { groups ->
            _groups.value = groups
            Log.d("EditWordVM", "Groups loaded. Count: ${groups.size}")
        }
    }


    fun onTextChange(text: String) {
        _editedText.value = text
        _saveSuccess.value = null
        _errorMessage.value = null
    }

    fun onTranslationChange(translation: String) {
        _editedTranslation.value = translation
        _saveSuccess.value = null
        _errorMessage.value = null
    }

    fun onGroupSelected(groupId: String?) {
        _selectedGroupId.value = groupId
        _saveSuccess.value = null
        _errorMessage.value = null
        Log.d("EditWordVM", "Selected group ID changed to: $groupId")
    }


    fun saveWord() {
        if (_isLoading.value) return

        val originalWord = _word.value
        val editedText = _editedText.value.trim()
        val editedTranslation = _editedTranslation.value.trim()
        val selectedGroupId = _selectedGroupId.value

        if (editedText.isBlank() || editedTranslation.isBlank()) {
            _errorMessage.value = "Поля 'Слово' та 'Переклад' не можуть бути порожніми."
            _saveSuccess.value = false
            return
        }

        if (originalWord == null) {
            _errorMessage.value = "Не вдалося зберегти зміни: оригінальне слово не завантажено."
            _saveSuccess.value = false
            Log.e("EditWordVM", "Cannot save word: original word is null.")
            return
        }

        val wordToSave = originalWord.copy(
            text = editedText,
            translation = editedTranslation,
            dictionaryId = selectedGroupId
        )

        _isLoading.value = true
        _errorMessage.value = null
        _saveSuccess.value = null

        Log.d("EditWordVM", "Saving word ID: ${wordToSave.id}")

        repository.saveWord(wordToSave) { success ->
            _isLoading.value = false
            _saveSuccess.value = success

            if (success) {
                _errorMessage.value = "Зміни збережено успішно!"
                Log.d("EditWordVM", "Word saved successfully: ${wordToSave.id}")
            } else {
                _errorMessage.value = "Помилка збереження змін."
                Log.e("EditWordVM", "Error saving word: ${wordToSave.id}")
            }
        }
    }


    fun clearStatusMessage() {
        _errorMessage.value = null
        _saveSuccess.value = null
    }


}