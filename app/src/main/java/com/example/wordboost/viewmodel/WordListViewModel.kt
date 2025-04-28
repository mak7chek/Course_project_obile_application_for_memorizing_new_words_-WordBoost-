package com.example.wordboost.viewmodel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Group
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.model.PracticeUtils
import java.text.SimpleDateFormat // Імпорт SimpleDateFormat
import java.util.* // Імпорт Date, Locale


// Дата-клас для відображення слова у списку
data class WordDisplayItem(
    val word: Word,
    val groupName: String?
)

class WordListViewModel(private val repository: FirebaseRepository) : ViewModel() {

    // Приватні LiveData для зберігання вихідних даних
    private val _allWords = MutableLiveData<List<Word>>(emptyList())

    // Публічні LiveData для стану UI (спостерігаються в Compose)
    private val _displayedWords = MutableLiveData<List<WordDisplayItem>>(emptyList())
    val displayedWords: LiveData<List<WordDisplayItem>> get() = _displayedWords

    private val _groups = MutableLiveData<List<Group>>(emptyList())
    val groups: LiveData<List<Group>> get() = _groups

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> get() = _errorMessage


    // Стан фільтрації/пошуку - ПРИВАТНА ЗМІННА
    private var currentSearchQuery: String = "" // <-- ПЕРЕКОНАЙТЕСЬ, ЩО ЦЕЙ РЯДОК Є У ВАШОМУ КОДІ


    // Приватна LiveData для внутрішнього стану фільтра групи
    private val _selectedGroupIdFilter = MutableLiveData<String?>(null)

    // Публічна LiveData для поточного обраного фільтра групи (щоб UI міг її спостерігати)
    val selectedGroupIdFilter: LiveData<String?> get() = _selectedGroupIdFilter


    // LiveData для поточного пошукового запиту (щоб поле пошуку в UI оновлювалося)
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> get() = _searchQuery


    init {
        loadWords()
        loadGroups()
    }

    // Завантажує всі слова користувача
    fun loadWords() {
        _isLoading.value = true
        repository.getAllWords { words ->
            _allWords.value = words
            applyFiltersAndSearch() // Застосувати фільтри після завантаження
            _isLoading.value = false
        }
    }

    // Завантажує групи користувача
    fun loadGroups() {
        repository.getGroups { groups ->
            val groupsWithFilters = mutableListOf<Group>()
            groupsWithFilters.add(Group(id = "", name = "Всі групи"))
            groupsWithFilters.add(Group(id = "no_group_filter", name = "Без групи"))
            groupsWithFilters.addAll(groups)

            _groups.value = groupsWithFilters

            // Встановлюємо початковий фільтр на "Всі групи" якщо ще не встановлено
            if (_selectedGroupIdFilter.value == null) {
                _selectedGroupIdFilter.value = ""
            }

            applyFiltersAndSearch()
        }
    }


    /**
     * Застосовує поточні фільтри та пошуковий запит до списку слів.
     */
    private fun applyFiltersAndSearch() {
        val words = _allWords.value ?: emptyList()
        val groupsById = _groups.value.orEmpty().associateBy { it.id }

        val filteredWords = words.filter { word ->
            // Фільтрація за групою
            val groupMatch = when (_selectedGroupIdFilter.value) {
                null, "" -> true
                "no_group_filter" -> word.dictionaryId.isEmpty()
                else -> word.dictionaryId == _selectedGroupIdFilter.value
            }

            // Фільтрація за пошуковим запитом
            val queryMatch = if (currentSearchQuery.isBlank()) { // <-- Використання currentSearchQuery
                true
            } else {
                val lowerCaseQuery = currentSearchQuery.toLowerCase(Locale.ROOT)
                // Виправлення неоднозначності: Явно приводимо lowerCaseQuery до CharSequence
                word.text.toLowerCase(Locale.ROOT).contains(lowerCaseQuery as CharSequence) ||
                        word.translation.toLowerCase(Locale.ROOT).contains(lowerCaseQuery as CharSequence)
            }
            groupMatch && queryMatch
        }

        val displayItems = filteredWords.map { word ->
            val groupName = groupsById[word.dictionaryId]?.name
            WordDisplayItem(word = word, groupName = groupName)
        }

        _displayedWords.value = displayItems
    }

    /**
     * Оновлює пошуковий запит та застосовує фільтри. Викликається з UI.
     */
    fun setSearchQuery(query: String) {
        currentSearchQuery = query // <-- Використання currentSearchQuery
        _searchQuery.value = query
        applyFiltersAndSearch()
    }

    /**
     * Оновлює фільтр за групою та застосовує фільтри. Викликається з UI.
     * @param groupId ID обраної групи, або null/"" для "Всі групи", або "no_group_filter" для "Без групи".
     */
    fun setGroupFilter(groupId: String?) {
        _selectedGroupIdFilter.value = groupId
        applyFiltersAndSearch()
    }


    /**
     * Видалити слово.
     */
    fun deleteWord(wordId: String) {
        _isLoading.value = true
        repository.deleteWord(wordId) { success ->
            _isLoading.value = false
            if (success) {
                _allWords.value = _allWords.value?.filter { it.id != wordId }
                applyFiltersAndSearch()
                _errorMessage.value = "Слово видалено"
            } else {
                _errorMessage.value = "Помилка видалення слова"
            }
        }
    }

    /**
     * Скинути статистику слова.
     */
    fun resetWord(word: Word) {
        val resetWord = word.copy(
            repetition = 0, easiness = 2.5f, interval = 0L, lastReviewed = 0L, nextReview = System.currentTimeMillis(), status = PracticeUtils.determineStatus(0)
        )

        _isLoading.value = true
        repository.updateWord(resetWord) { success ->
            _isLoading.value = false
            if (success) {
                _allWords.value = _allWords.value?.map { if (it.id == resetWord.id) resetWord else it }
                applyFiltersAndSearch()
                _errorMessage.value = "Статистику слова скинуто"
            } else {
                _errorMessage.value = "Помилка скидання статистики слова"
            }
        }
    }

    // Функція-заглушка для дії "Редагувати". UI обробляє навігацію.
    fun onEditWordClicked(word: Word) {
        // UI має обробити навігацію
    }


    // Допоміжна функція для форматування дати наступного повторення (доступна для UI)
    fun formatNextReviewDate(timestamp: Long): String {
        if (timestamp == 0L) return "Не вивчалося"
        val now = System.currentTimeMillis()
        return if (timestamp <= now) {
            "На повторенні зараз"
        } else {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            "Наступне: ${formatter.format(date)}"
        }
    }

    // Очищає повідомлення про помилку/статус
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}