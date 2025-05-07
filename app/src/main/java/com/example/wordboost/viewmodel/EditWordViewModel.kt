package com.example.wordboost.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// !!! SavedStateHandle тут більше не потрібен !!!
// import androidx.lifecycle.SavedStateHandle
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.model.Group

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first


// !!! Ключ для ID слова більше не потрібен у ViewModel, але може бути корисним для навігації !!!
// const val WORD_ID_KEY = "wordId" // Може залишитись константою для навігації


class EditWordViewModel(
    private val repository: FirebaseRepository, // FirebaseRepository для завантаження/збереження слова
    // !!! ViewModel тепер приймає wordId безпосередньо !!!
    private val wordId: String? // ID слова для редагування, переданий з Factory
) : ViewModel() {

    // --- СТАН СЛОВА ДЛЯ РЕДАГУВАННЯ ---
    private val _word = MutableStateFlow<Word?>(null) // Оригінальне слово
    val word: StateFlow<Word?> get() = _word.asStateFlow()

    private val _editedText = MutableStateFlow("") // Редагований текст слова
    val editedText: StateFlow<String> get() = _editedText.asStateFlow()

    private val _editedTranslation = MutableStateFlow("") // Редагований переклад
    val editedTranslation: StateFlow<String> get() = _editedTranslation.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null) // ID вибраної групи (null для "Без групи")
    val selectedGroupId: StateFlow<String?> get() = _selectedGroupId.asStateFlow()


    // --- СТАН ДЛЯ ГРУП ---
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> get() = _groups.asStateFlow()


    // --- СТАН UI ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    private val _saveSuccess = MutableStateFlow<Boolean?>(null) // Результат збереження
    val saveSuccess: StateFlow<Boolean?> get() = _saveSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null) // Повідомлення про помилку
    val errorMessage: StateFlow<String?> get() = _errorMessage.asStateFlow()


    init {
        Log.d("EditWordVM", "ViewModel initialized for word ID: $wordId")
        if (wordId != null) {
            loadWord(wordId) // Завантажуємо слово, якщо ID присутній
            loadGroups() // Завантажуємо групи
        } else {
            // Якщо ID слова відсутній, можливо, це режим додавання нового слова?
            // Наразі ми реалізуємо тільки редагування, тому це помилка.
            _isLoading.value = false
            _errorMessage.value = "ID слова для редагування відсутній."
            Log.e("EditWordVM", "Word ID is null. Cannot load word for editing.")
            // Якщо ви хочете реалізувати ДОДАВАННЯ нового слова на цьому ж екрані:
            // можна ініціалізувати _word = null, _editedText = "", _editedTranslation = "", _selectedGroupId = null
            // А в saveWord() перевіряти, чи _word.value == null, щоб створити нове слово.
        }
    }

    // --- ФУНКЦІЇ ЗАВАНТАЖЕННЯ ---

    private fun loadWord(id: String) {
        _isLoading.value = true
        _errorMessage.value = null
        Log.d("EditWordVM", "Loading word with ID: $id")

        // Використовуємо метод репозиторія для отримання слова за ID
        // Припускаємо, що FirebaseRepository.getWordById(id, callback) існує і шукає за ID
        repository.getWordById(id) { word -> // Припускаємо, що getWordById існує
            if (word != null && word.id == id) {
                _word.value = word
                // Ініціалізуємо редаговані поля даними завантаженого слова
                _editedText.value = word.text
                _editedTranslation.value = word.translation
                _selectedGroupId.value = word.dictionaryId // Може бути null
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
        // Варіант з одноразовим запитом (простіше для Edit):
        repository.getGroups { groups ->
            _groups.value = groups
            Log.d("EditWordVM", "Groups loaded. Count: ${groups.size}")
        }
        // Якщо потрібен Listener, використовуємо його і видаляємо в onCleared()
    }


    // --- ФУНКЦІЇ ДЛЯ ОБРОБКИ ВЗАЄМОДІЇ З UI ---

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


    // --- ФУНКЦІЯ ЗБЕРЕЖЕННЯ ---

    fun saveWord() {
        if (_isLoading.value) return

        // Використовуємо оригінальне слово як основу для ID та статистики
        val originalWord = _word.value
        val editedText = _editedText.value.trim()
        val editedTranslation = _editedTranslation.value.trim()
        val selectedGroupId = _selectedGroupId.value

        // Валідація даних
        if (editedText.isBlank() || editedTranslation.isBlank()) {
            _errorMessage.value = "Поля 'Слово' та 'Переклад' не можуть бути порожніми."
            _saveSuccess.value = false
            return
        }

        // Перевірка, чи завантажено оригінальне слово (для режиму редагування)
        if (originalWord == null) {
            _errorMessage.value = "Не вдалося зберегти зміни: оригінальне слово не завантажено."
            _saveSuccess.value = false
            Log.e("EditWordVM", "Cannot save word: original word is null.")
            return
        }

        // Створюємо оновлений об'єкт Word
        val wordToSave = originalWord.copy(
            text = editedText,
            translation = editedTranslation,
            dictionaryId = selectedGroupId // Присвоюємо вибраний ID групи (може бути null)
            // Статистика (repetition, easiness, interval, lastReviewed, nextReview, status) НЕ ЗМІНЮЄТЬСЯ
        )

        _isLoading.value = true
        _errorMessage.value = null
        _saveSuccess.value = null

        Log.d("EditWordVM", "Saving word ID: ${wordToSave.id}")

        // Викликаємо метод репозиторія для збереження слова
        // repository.saveWord вже вміє оновлювати існуюче слово за його ID
        repository.saveWord(wordToSave) { success -> // repository.saveWord має бути з колбеком (Boolean) -> Unit
            _isLoading.value = false
            _saveSuccess.value = success

            if (success) {
                _errorMessage.value = "Зміни збережено успішно!"
                Log.d("EditWordVM", "Word saved successfully: ${wordToSave.id}")
                // UI/Screen має реагувати на saveSuccess = true і викликати onBack()
            } else {
                _errorMessage.value = "Помилка збереження змін."
                Log.e("EditWordVM", "Error saving word: ${wordToSave.id}")
            }
        }
    }

    // --- ДОДАТКОВІ ФУНКЦІЇ ---

    fun clearStatusMessage() {
        _errorMessage.value = null
        _saveSuccess.value = null
    }


//     override fun onCleared() {
//        super.onCleared()
//        groupsListenerRegistration?.remove()
//     }
}