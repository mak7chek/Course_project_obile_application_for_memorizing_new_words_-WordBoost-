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
        allGroups.filter { it.name != "Основний словник" } // Приклад фільтрації, якщо потрібно
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _showGroupDialog = MutableStateFlow(false)
    val showGroupDialog: StateFlow<Boolean> = _showGroupDialog.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private var groupsListenerRegistration: ListenerRegistration? = null

    init {
        _selectedGroupId.value = null // Або ID групи "Основний словник" за замовчуванням
        loadGroups()
    }

    fun setUkText(text: String) {
        _ukText.value = text
        if (text.isBlank() && _enText.value.isBlank()) { // Якщо обидва поля порожні
            // _selectedGroupId.value = null // Можна скидати групу, якщо логіка це передбачає
        }
        _statusMessage.value = null // Скидаємо повідомлення при зміні тексту
    }

    fun setEnText(text: String) {
        _enText.value = text
        if (text.isBlank() && _ukText.value.isBlank()) {
            // _selectedGroupId.value = null
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
            // Якщо вибрана група більше не існує (наприклад, видалена), скидаємо вибір
            if (currentSelected != null && fetched.none { it.id == currentSelected }) {
                _selectedGroupId.value = null
                Log.d("TranslateVM", "Selected group $currentSelected no longer exists, resetting to null.")
            }
        }
    }

    // !!! ОНОВЛЕНИЙ МЕТОД ПЕРЕКЛАДУ !!!
    fun translate() {
        if (_isLoading.value) {
            Log.d("TranslateVM", "Translate called while already loading, ignoring.")
            return
        }

        val ukTextValue = _ukText.value.trim()
        val enTextValue = _enText.value.trim()

        val textToTranslate: String
        val targetLangForApi: String // Мова, НА яку ми хочемо перекласти (для DeepL)
        val sourceFieldIsUk: Boolean   // Чи було українське поле джерелом для перекладу

        if (ukTextValue.isNotBlank() && enTextValue.isBlank()) {
            textToTranslate = ukTextValue    // Текст з українського поля
            targetLangForApi = "en"          // Хочемо отримати англійський переклад
            sourceFieldIsUk = true
        } else if (enTextValue.isNotBlank() && ukTextValue.isBlank()) {
            textToTranslate = enTextValue    // Текст з англійського поля
            targetLangForApi = "uk"          // Хочемо отримати український переклад
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
        _statusMessage.value = null // Очищаємо попередні повідомлення
        Log.i("TranslateVM", "Calling translationRepository.translateForUserVocabularySuspend for '$textToTranslate' -> $targetLangForApi")

        viewModelScope.launch {
            val translationResult: String? = try {
                // Викликаємо нову suspend функцію з TranslationRepository
                translationRepository.translateForUserVocabularySuspend(textToTranslate, targetLangForApi)
            } catch (e: Exception) {
                Log.e("TranslateVM", "Exception during translateForUserVocabularySuspend for '$textToTranslate'", e)
                _statusMessage.value = "Помилка перекладу: ${e.message}" // Встановлюємо повідомлення про помилку
                null // Повертаємо null у разі винятку
            }

            // Цей блок виконається ПІСЛЯ того, як translateForUserVocabularySuspend завершиться
            _isLoading.value = false // Гарантовано скидаємо стан завантаження
            Log.d("TranslateVM", "Translation result for '$textToTranslate': '$translationResult'. isLoading set to false.")

            if (translationResult != null) {
                val cleanTranslationResult = translationResult.trim() // Очищаємо результат перед присвоєнням
                if (sourceFieldIsUk) { // Якщо перекладали з українського поля (_ukText)
                    _enText.value = cleanTranslationResult // Результат (англійський) вставляємо в _enText
                } else { // Якщо перекладали з англійського поля (_enText)
                    _ukText.value = cleanTranslationResult // Результат (український) вставляємо в _ukText
                }
                if (_statusMessage.value == null) { // Показуємо успіх, тільки якщо не було помилки
                    _statusMessage.value = "Переклад отримано."
                }
                Log.d("TranslateVM", "Text fields updated. UK: '${_ukText.value}', EN: '${_enText.value}'")
            } else {
                // Якщо translationResult == null І не було встановлено повідомлення про помилку через виняток
                if (_statusMessage.value == null) {
                    _statusMessage.value = "Не вдалося отримати переклад для '$textToTranslate'."
                }
                Log.w("TranslateVM", "Translation result was null for '$textToTranslate'. Status: ${_statusMessage.value}")
            }
        }
    }

    // Функція збереження слова (залишається як є, її логіка перевірки дублікатів перед збереженням у групу актуальна)
    fun saveWord() {
        if (_isLoading.value) return // Не зберігаємо, якщо йде інша операція (наприклад, переклад)

        val originalToSave = _ukText.value.trim() // Припускаємо, що українське поле - це "оригінал" для збереження
        val translatedToSave = _enText.value.trim() // Англійське - це "переклад"
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
                // Цей колбек від getWordObject може прийти не на Main потоці,
                // але подальші оновлення StateFlow безпечні з будь-якого потоку.
                // Для чистоти, можна було б обгорнути вміст колбеку в launch(Dispatchers.Main) або viewModelScope.launch,
                // але зазвичай оновлення StateFlow з фонових потоків працюють.

                val isDuplicateInTargetGroup = existingWord != null && existingWord.dictionaryId == groupIdToSave

                if (isDuplicateInTargetGroup) {
                    val groupName = if (groupIdToSave.isNullOrBlank()) "Основному словнику" else _groups.value.find { it.id == groupIdToSave }?.name ?: "обраній групі"
                    _statusMessage.value = "Слово '$originalToSave' вже існує в $groupName."
                    _isLoading.value = false
                    Log.w("TranslateVM", "Duplicate word '$originalToSave' in target group '$groupName'.")
                } else {
                    val wordToSave = existingWord?.copy(
                        translation = translatedToSave, // Оновлюємо переклад, якщо слово вже існує
                        dictionaryId = groupIdToSave    // Оновлюємо групу
                    ) ?: Word(
                        id = UUID.randomUUID().toString(),
                        text = originalToSave,       // Українське слово
                        translation = translatedToSave, // Англійський переклад
                        dictionaryId = groupIdToSave,
                        repetition = 0, easiness = 2.5f, interval = 0L, lastReviewed = 0L, nextReview = 0L, status = "new"
                    )

                    firebaseRepository.saveWord(wordToSave) { success ->
                        // Колбек від saveWord (з FirebaseRepository) має бути (Boolean) -> Unit
                        viewModelScope.launch { // Перемикаємось на viewModelScope (зазвичай Main) для оновлення стану
                            _isLoading.value = false
                            if (success) {
                                val groupName = if (groupIdToSave.isNullOrBlank()) "Основний словник" else _groups.value.find { it.id == groupIdToSave }?.name ?: "обрану групу"
                                _statusMessage.value = "Слово '$originalToSave' додано в $groupName."
                                _ukText.value = ""
                                _enText.value = ""
                                // _selectedGroupId.value = null // Можливо, не скидати, якщо користувач хоче додати ще слів у ту ж групу
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