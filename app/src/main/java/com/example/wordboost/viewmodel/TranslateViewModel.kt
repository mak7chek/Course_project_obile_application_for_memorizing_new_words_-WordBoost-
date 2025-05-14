package com.example.wordboost.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.data.model.Group
import com.example.wordboost.data.model.Word
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class TranslateViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _ukText = MutableStateFlow("")
    val ukText: StateFlow<String> = _ukText.asStateFlow()

    private val _enText = MutableStateFlow("")
    val enText: StateFlow<String> = _enText.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val editableGroups: StateFlow<List<Group>> = _groups.map { allGroups ->
        allGroups.filter { it.name != "Основний словник" }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _showGroupDialog = MutableStateFlow(false)
    val showGroupDialog: StateFlow<Boolean> = _showGroupDialog.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private var groupsListenerRegistration: ListenerRegistration? = null

    init {
        _selectedGroupId.value = null
        loadGroups()
    }

    fun setUkText(text: String) {
        _ukText.value = text
        if (text.isBlank() && _enText.value.isBlank()) {
        }
        _statusMessage.value = null
    }

    fun setEnText(text: String) {
        _enText.value = text
        if (text.isBlank() && _ukText.value.isBlank()) {
        }
        _statusMessage.value = null
    }

    fun setStatusMessage(message: String?) { _statusMessage.value = message }

    fun loadGroups() {
        groupsListenerRegistration?.remove()
        groupsListenerRegistration = firebaseRepository.getGroups { fetched ->
            _groups.value = fetched
            Log.d("TranslateVM", "Groups fetched. Count: ${fetched.size}. First: ${fetched.firstOrNull()?.name}")
            val currentSelected = _selectedGroupId.value
            if (currentSelected != null && fetched.none { it.id == currentSelected }) {
                _selectedGroupId.value = null
                Log.d("TranslateVM", "Selected group $currentSelected no longer exists, resetting to null.")
            }
        }
    }

    fun translate() {
        if (_isLoading.value) {
            Log.d("TranslateVM", "Translate called while already loading, ignoring.")
            return
        }

        val ukTextValue = _ukText.value.trim()
        val enTextValue = _enText.value.trim()

        val textToTranslate: String
        val targetLangForApi: String
        val sourceFieldIsUk: Boolean

        if (ukTextValue.isNotBlank() && enTextValue.isBlank()) {
            textToTranslate = ukTextValue
            targetLangForApi = "en"
            sourceFieldIsUk = true
        } else if (enTextValue.isNotBlank() && ukTextValue.isBlank()) {
            textToTranslate = enTextValue
            targetLangForApi = "uk"
            sourceFieldIsUk = false
        } else {
            _statusMessage.value = if (ukTextValue.isNotBlank() && enTextValue.isNotBlank()) {
                "Обидва поля заповнені. Очистіть одне для перекладу."
            } else {
                "Введіть слово в одне з полів для перекладу."
            }
            Log.d("TranslateVM", "Translate pre-condition not met: ${statusMessage.value}")
            return
        }

        _isLoading.value = true
        _statusMessage.value = null
        Log.i("TranslateVM", "Calling translationRepository.translateForUserVocabularySuspend for '$textToTranslate' -> $targetLangForApi")

        viewModelScope.launch {
            val translationResult: String? = try {
                translationRepository.translateForUserVocabularySuspend(textToTranslate, targetLangForApi)
            } catch (e: Exception) {
                Log.e("TranslateVM", "Exception during translateForUserVocabularySuspend for '$textToTranslate'", e)
                _statusMessage.value = "Помилка перекладу: ${e.message}"
                null
            }

            _isLoading.value = false
            Log.d("TranslateVM", "Translation result for '$textToTranslate': '$translationResult'. isLoading set to false.")

            if (translationResult != null) {
                val cleanTranslationResult = translationResult.trim()
                if (sourceFieldIsUk) {
                    _enText.value = cleanTranslationResult
                } else {
                    _ukText.value = cleanTranslationResult
                }
                if (_statusMessage.value == null) {
                    _statusMessage.value = "Переклад отримано."
                }
                Log.d("TranslateVM", "Text fields updated. UK: '${_ukText.value}', EN: '${_enText.value}'")
            } else {
                if (_statusMessage.value == null) {
                    _statusMessage.value = "Не вдалося отримати переклад для '$textToTranslate'."
                }
                Log.w("TranslateVM", "Translation result was null for '$textToTranslate'. Status: ${_statusMessage.value}")
            }
        }
    }

    fun saveWord() {
        if (_isLoading.value) return

        val originalToSave = _ukText.value.trim()
        val translatedToSave = _enText.value.trim()
        val groupIdToSave = _selectedGroupId.value

        if (originalToSave.isBlank() || translatedToSave.isBlank()) {
            _statusMessage.value = "Заповніть обидва поля для збереження"
            return
        }

        _isLoading.value = true
        _statusMessage.value = null
        Log.d("TranslateVM", "Attempting to save word: Original='$originalToSave', Translated='$translatedToSave', GroupId='$groupIdToSave'")

        viewModelScope.launch {
            firebaseRepository.getWordObject(originalToSave) { existingWord ->
                val isDuplicateInTargetGroup = existingWord != null && existingWord.dictionaryId == groupIdToSave

                if (isDuplicateInTargetGroup) {
                    val groupName = if (groupIdToSave.isNullOrBlank()) "Основному словнику" else _groups.value.find { it.id == groupIdToSave }?.name ?: "обраній групі"
                    _statusMessage.value = "Слово '$originalToSave' вже існує в $groupName."
                    _isLoading.value = false
                    Log.w("TranslateVM", "Duplicate word '$originalToSave' in target group '$groupName'.")
                } else {
                    val wordToSave = existingWord?.copy(
                        translation = translatedToSave,
                        dictionaryId = groupIdToSave
                    ) ?: Word(
                        id = UUID.randomUUID().toString(),
                        text = originalToSave,
                        translation = translatedToSave,
                        dictionaryId = groupIdToSave,
                        repetition = 0, easiness = 2.5f, interval = 0L, lastReviewed = 0L, nextReview = 0L, status = "new"
                    )

                    firebaseRepository.saveWord(wordToSave) { success ->
                        viewModelScope.launch {
                            _isLoading.value = false
                            if (success) {
                                val groupName = if (groupIdToSave.isNullOrBlank()) "Основний словник" else _groups.value.find { it.id == groupIdToSave }?.name ?: "обрану групу"
                                _statusMessage.value = "Слово '$originalToSave' додано в $groupName."
                                _ukText.value = ""
                                _enText.value = ""
                                Log.i("TranslateVM", "Word '$originalToSave' saved successfully to group '$groupName'.")
                            } else {
                                _statusMessage.value = "Помилка збереження слова."
                                Log.e("TranslateVM", "Failed to save word '$originalToSave'.")
                            }
                        }
                    }
                }
            }
        }
    }
    fun showGroupDialog() {
        _showGroupDialog.value = true
        _statusMessage.value = null
    }

    fun hideGroupDialog() {
        _showGroupDialog.value = false
    }


    fun setSelectedGroupId(groupId: String?) {
        _selectedGroupId.value = groupId
        Log.d("TranslateVM", "Selected group ID set to: $groupId")
    }


    fun createGroup(name: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val success = firebaseRepository.createGroup(name)
            _isLoading.value = false
            if (success) {
                _statusMessage.value = "Група '$name' створена."
            } else {
                _statusMessage.value = "Помилка створення групи '$name'."
            }
        }
    }

    fun renameGroup(groupId: String, newName: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val success = firebaseRepository.updateGroup(groupId, newName)
            _isLoading.value = false
            if (success) {
                _statusMessage.value = "Група перейменована на '$newName'."
            } else {
                _statusMessage.value = "Помилка перейменування групи."
            }
        }
    }

    fun deleteGroup(groupId: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val success = firebaseRepository.deleteGroup(groupId)
            _isLoading.value = false
            if (success) {
                _statusMessage.value = "Група видалена."

                if (_selectedGroupId.value == groupId) {
                    _selectedGroupId.value = null
                }
            } else {
                _statusMessage.value = "Помилка видалення групи."
            }
        }
    }
    fun clearStatusMessage() {

        _statusMessage.value = null

    }
    override fun onCleared() {
        super.onCleared()
        groupsListenerRegistration?.remove()

    }

}