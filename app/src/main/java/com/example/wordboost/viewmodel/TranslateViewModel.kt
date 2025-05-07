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


    private val _selectedGroupId = MutableStateFlow<String?>(null) // <-- Правильний тип MutableStateFlow
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()



    private var groupsListenerRegistration: ListenerRegistration? = null


    init {

        _selectedGroupId.value = null
        loadGroups()
    }

    fun setUkText(text: String) {
        _ukText.value = text
        if (ukText.value.isBlank() && enText.value.isBlank()) {
            _selectedGroupId.value = null // <-- Скидаємо в null
        }
        _statusMessage.value = null
    }

    fun setEnText(text: String) {
        _enText.value = text
        if (ukText.value.isBlank() && enText.value.isBlank()) {
            _selectedGroupId.value = null // <-- Скидаємо в null
        }
        _statusMessage.value = null
    }

    fun setStatusMessage(message: String?) {
        _statusMessage.value = message
    }


    // Функція перекладу
    fun translate() {
        if (_isLoading.value) return

        _isLoading.value = true
        _statusMessage.value = null
        // При перекладі вибір групи НЕ має скидатись автоматично!
        // _selectedGroupId.value = null // <-- Цей рядок слід видалити з translate(), як ми вже вирішили

        val textToTranslate = _ukText.value.ifBlank { _enText.value }
        val lang = if (_ukText.value.isNotBlank()) "EN" else "UK" // Приклад визначення мови

        viewModelScope.launch {
            // Assume translationRepository.translate handles callbacks or Flow
            translationRepository.translate(textToTranslate, lang) { result ->
                _isLoading.value = false
                result?.let { translated ->
                    // Определяем, куда поместить перевод
                    if (_ukText.value.isNotBlank()) { // Если переводили УКР -> АНГЛ
                        _enText.value = translated
                    } else { // Если переводили АНГЛ -> УКР
                        _ukText.value = translated
                    }
                    _statusMessage.value = "Переклад успішний" // Повідомлення про успіх
                } ?: run {
                    _statusMessage.value = "Не вдалося перекласти" // Повідомлення про помилку перекладу
                }
            }
        }
    }

    // Функція збереження слова
    fun saveWord() {
        if (_isLoading.value) return

        val original = _ukText.value.trim()
        val translated = _enText.value.trim()
        val groupId = _selectedGroupId.value // Може бути null

        if (original.isBlank() || translated.isBlank()) {
            _statusMessage.value = "Заповніть обидва поля для збереження"
            return
        }

        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            // Перевіряємо, існує ли вже слово (для original текста)
            // firebaseRepository.getWordObject повертає Word? через callback
            firebaseRepository.getWordObject(original) { existingWord ->
                // Перевіряємо, чи є існуюче слово дублікатом в ВИБРАНІЙ групі (dictionaryId може бути null)
                //existingWord?.dictionaryId має тип String? (припустимо після виправлення Word)
                // groupId має тип String?
                val isDuplicateInTargetLocation = existingWord != null && existingWord.dictionaryId == groupId

                if (isDuplicateInTargetLocation) {
                    // Визначаємо назву групи для повідомлення
                    val groupName = if (groupId.isNullOrBlank()) "словнику" else _groups.value.find { it.id == groupId }?.name ?: "обраній групі"
                    _statusMessage.value = "Слово '$original' вже існує в $groupName."
                    _isLoading.value = false
                } else {
                    val wordToSave = if (existingWord != null) {
                        // Якщо слово існує, оновлюємо його, присвоюючи йому вибрану групу
                        // dictionaryId може бути null
                        //existingWord.copy(dictionaryId = groupId) // Цей рядок правильний, якщо Word.dictionaryId: String?
                        existingWord.copy(dictionaryId = groupId)
                    } else {
                        // Якщо слово не існує, створюємо нове з унікальним ID
                        val id = UUID.randomUUID().toString() // Використовуємо UUID.randomUUID()
                        Word(
                            id = id,
                            text = original,
                            translation = translated,
                            dictionaryId = groupId, // <-- Присвоюємо String? полю dictionaryId
                            repetition = 0, easiness = 2.5f, interval = 0L, lastReviewed = 0L, nextReview = 0L, status = "new"
                        )
                    }

                    // Репозиторій зберігає або оновлює слово (використовує callback (Boolean) -> Unit)
                    firebaseRepository.saveWord(wordToSave) { success ->
                        _isLoading.value = false
                        if (success) {
                            val groupName = if (groupId.isNullOrBlank()) "словника" else _groups.value.find { it.id == groupId }?.name ?: "обрану групу"
                            _statusMessage.value = "Слово '$original' додано до $groupName"
                            // Очищаємо поля після успішного збереження
                            _ukText.value = ""
                            _enText.value = ""


                            _selectedGroupId.value = null
                        } else {
                            val groupNameMsg = if (groupId.isNullOrBlank()) "" else "в групу"
                            _statusMessage.value = "Помилка збереження слова $groupNameMsg."
                        }
                    }
                }
            }
        }
    }

    // Функції для управління видимістю діалога груп
    fun showGroupDialog() {
        _showGroupDialog.value = true
        _statusMessage.value = null // Очищаємо повідомлення при відкритті діалога
    }

    fun hideGroupDialog() {
        _showGroupDialog.value = false
    }


    fun setSelectedGroupId(groupId: String?) {
        _selectedGroupId.value = groupId
        Log.d("TranslateVM", "Selected group ID set to: $groupId")
    }
    fun loadGroups() {
        groupsListenerRegistration?.remove()


        groupsListenerRegistration = firebaseRepository.getGroups { fetched ->
            _groups.value = fetched
            Log.d("TranslateVM", "Groups fetched. Count: ${fetched.size}. First: ${fetched.firstOrNull()?.name}") // Лог

            val currentSelected = _selectedGroupId.value
            if (currentSelected != null && fetched.none { it.id == currentSelected }) {
                _selectedGroupId.value = null
                Log.d("TranslateVM", "Selected group $currentSelected no longer exists, resetting to null.") // Лог
            }

        }
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
                // loadGroups() не потрібен, listener сам оновить _groups.value
            } else {
                _statusMessage.value = "Помилка створення групи '$name'."
            }
        }
    }

    fun renameGroup(groupId: String, newName: String) {
        if (_isLoading.value) return // Перевіряємо, не йде ли вже операція
        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val success = firebaseRepository.updateGroup(groupId, newName) // Викликаємо suspend метод Repository
            _isLoading.value = false
            if (success) {
                _statusMessage.value = "Група перейменована на '$newName'."
                // loadGroups() не потрібен
            } else {
                _statusMessage.value = "Помилка перейменування групи."
            }
        }
    }

    fun deleteGroup(groupId: String) {
        if (_isLoading.value) return // Перевіряємо, не йде ли вже операція
        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val success = firebaseRepository.deleteGroup(groupId)
            _isLoading.value = false
            if (success) {
                _statusMessage.value = "Група видалена."

                if (_selectedGroupId.value == groupId) {
                    _selectedGroupId.value = null // <-- Сбрасываем в null
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