package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.data.model.Group
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.model.PracticeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID // Імпорт UUID


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

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _showGroupDialog = MutableStateFlow(false)
    val showGroupDialog: StateFlow<Boolean> = _showGroupDialog.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    init {
        loadGroups()
        _selectedGroupId.value = ""
    }

    // Функції для зміни стану з UI
    fun setUkText(text: String) {
        _ukText.value = text

        _selectedGroupId.value = ""
        _statusMessage.value = null
    }

    fun setEnText(text: String) {
        _enText.value = text
        _selectedGroupId.value = ""
        _statusMessage.value = null
    }

    fun setStatusMessage(message: String?) {
        _statusMessage.value = message
    }


    fun translate() {
        if (_isLoading.value) return

        _isLoading.value = true
        _statusMessage.value = null
        _selectedGroupId.value = ""

        val textToTranslate = _ukText.value.ifBlank { _enText.value }
        val lang = if (_ukText.value.isNotBlank()) "EN" else "UK"

        viewModelScope.launch {
            translationRepository.translate(textToTranslate, lang) { result ->
                _isLoading.value = false
                result?.let { translated ->
                    if (_ukText.value.isNotBlank()) {
                        _enText.value = translated
                    } else {
                        _ukText.value = translated
                    }
                    _statusMessage.value = "Переклад успішний" // Повідомлення про успіх
                } ?: run {
                    _statusMessage.value = "Не вдалося перекласти" // Повідомлення про помилку перекладу
                }
            }
        }
    }

    fun saveWord() {
        if (_isLoading.value) return

        val original = _ukText.value.trim()
        val translated = _enText.value.trim()
        val groupId = _selectedGroupId.value.orEmpty()
        val groupName = _groups.value.find { it.id == _selectedGroupId.value }?.name.orEmpty()

        if (original.isBlank() || translated.isBlank()) {
            _statusMessage.value = "Заповніть обидва поля для збереження"
            return
        }

        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            firebaseRepository.getWordObject(original) { existingWord ->
                val isDuplicateInTargetLocation = existingWord != null && existingWord.dictionaryId == groupId

                if (isDuplicateInTargetLocation) {
                    _statusMessage.value = if (groupId.isBlank()) "Слово '$original' вже існує в словнику" else "Слово '$original' вже існує в групі '$groupName'"
                    _isLoading.value = false
                } else {
                    val wordToSave = if (existingWord != null) {
                        existingWord.copy(dictionaryId = groupId)
                    } else {
                        val id = UUID.nameUUIDFromBytes((original + translated + groupId).toByteArray()).toString()
                        Word(
                            id = id,
                            text = original,
                            translation = translated,
                            dictionaryId = groupId,
                            repetition = 0, easiness = 2.5f, interval = 0L, lastReviewed = 0L, nextReview = 0L, status = "new"
                        )
                    }

                    firebaseRepository.saveWord(wordToSave) { success ->
                        _isLoading.value = false
                        if (success) {
                            _statusMessage.value = if (groupId.isBlank()) "Слово '$original' додано до словника" else "Слово '$original' додано до групи '$groupName'"
                            _ukText.value = ""
                            _enText.value = ""
                            _selectedGroupId.value = "" // Скидаємо вибір групи після збереження
                        } else {
                            _statusMessage.value = if (groupId.isBlank()) "Помилка збереження слова" else "Помилка збереження слова в групу"
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
        _selectedGroupId.value = groupId ?: "" // Встановлюємо "" якщо передано null
    }

    // Функції для керування групами
    fun loadGroups() {
        firebaseRepository.getGroups { fetched ->
            val groupsWithSpecial = mutableListOf<Group>()
            groupsWithSpecial.add(Group(id = "", name = "Основний словник"))
            groupsWithSpecial.addAll(fetched)

            _groups.value = groupsWithSpecial
            val currentSelected = _selectedGroupId.value
            if (currentSelected != null && currentSelected.isNotBlank() && groupsWithSpecial.none { it.id == currentSelected }) {
                _selectedGroupId.value = ""
            }
        }
    }

    fun createGroup(name: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        _statusMessage.value = null

        firebaseRepository.createGroup(name) { success ->
            _isLoading.value = false
            if (success) {
                _statusMessage.value = "Група '$name' створена"
                loadGroups()
            } else {
                _statusMessage.value = "Помилка створення групи '$name'"
            }
        }
    }

    fun renameGroup(groupId: String, newName: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        _statusMessage.value = null

        firebaseRepository.updateGroup(groupId, newName) { success ->
            _isLoading.value = false
            if (success) {
                _statusMessage.value = "Група перейменована на '$newName'"
                loadGroups()
            } else {
                _statusMessage.value = "Помилка перейменування групи"
            }
        }
    }

    fun deleteGroup(groupId: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        _statusMessage.value = null

        firebaseRepository.deleteGroup(groupId) { success ->
            _isLoading.value = false
            if (success) {
                _statusMessage.value = "Група видалена"
                loadGroups()
                if (_selectedGroupId.value == groupId) {
                    _selectedGroupId.value = ""
                }
            } else {
                _statusMessage.value = "Помилка видалення групи"
            }
        }
    }


    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}