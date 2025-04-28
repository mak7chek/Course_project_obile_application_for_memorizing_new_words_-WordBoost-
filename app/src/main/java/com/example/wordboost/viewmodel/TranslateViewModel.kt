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

    // Стан UI (використовуємо StateFlow, як обговорювали раніше)
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
        // Встановлюємо початкове значення обраної групи на "Основний словник"
        _selectedGroupId.value = "" // Пустий рядок відповідає "Основному словнику"
    }

    // Функції для зміни стану з UI
    fun setUkText(text: String) {
        _ukText.value = text
        // При зміні тексту скидаємо вибір групи та повідомлення
        _selectedGroupId.value = ""
        _statusMessage.value = null
    }

    fun setEnText(text: String) {
        _enText.value = text
        // При зміні тексту скидаємо вибір групи та повідомлення
        _selectedGroupId.value = ""
        _statusMessage.value = null
    }

    /**
     * Встановлює повідомлення про статус, яке буде відображено в UI (наприклад, в Snackbar).
     * Викликається з ViewModel для асинхронних операцій або з UI для простих валідацій.
     */
    fun setStatusMessage(message: String?) {
        _statusMessage.value = message
    }


    fun translate() {
        if (_isLoading.value) return

        _isLoading.value = true
        _statusMessage.value = null
        _selectedGroupId.value = "" // Скидаємо обрану групу при перекладі нового слова

        val textToTranslate = _ukText.value.ifBlank { _enText.value }
        val lang = if (_ukText.value.isNotBlank()) "EN" else "UK"

        viewModelScope.launch {
            // Припускаємо, що translate - це suspend функція або викликає колбек
            // Якщо це колбек-орієнтована функція, її потрібно обробляти інакше
            // Давайте припустимо, що translationRepository.translate використовує колбек, як у вашому оригінальному коді
            translationRepository.translate(textToTranslate, lang) { result ->
                _isLoading.value = false // Вимикаємо завантаження після отримання результату
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

    // Функції для керування діалогом груп
    fun showGroupDialog() {
        // UI (кнопка) має контролювати, чи можна показати діалог (чи поля заповнені)
        _showGroupDialog.value = true
        _statusMessage.value = null // Очищаємо статус при відкритті діалогу
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
            // Видаляємо "Без групи" тут, оскільки вона більше для фільтрації, а не для вибору куди зберегти.
            // Якщо хочете збереження "Без групи", тоді id="" буде означати "Без групи"
            groupsWithSpecial.addAll(fetched)

            _groups.value = groupsWithSpecial
            // Переконайтесь, що обрана група все ще існує, інакше скиньте вибір
            val currentSelected = _selectedGroupId.value
            if (currentSelected != null && currentSelected.isNotBlank() && groupsWithSpecial.none { it.id == currentSelected }) {
                _selectedGroupId.value = "" // Скидаємо вибір, якщо обрана група зникла
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
                loadGroups() // Оновлюємо список груп
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
                loadGroups() // Оновлюємо список груп
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
                loadGroups() // Оновлюємо список
                // Якщо видалили обрану групу, скидаємо вибір на "Основний словник"
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