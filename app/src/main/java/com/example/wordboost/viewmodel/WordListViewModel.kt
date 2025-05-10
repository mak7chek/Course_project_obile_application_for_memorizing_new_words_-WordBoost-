package com.example.wordboost.viewmodel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.*
import com.example.wordboost.data.model.Group
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.util.PracticeUtils

import com.example.wordboost.data.tts.TextToSpeechService
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit

data class WordDisplayItem(
    val word: Word,
    val groupName: String?,
    val progress: Float
) {
    val id: String = word.id
}

class WordListViewModel(
    private val repository: FirebaseRepository,
    private val ttsService: TextToSpeechService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _allWords = MutableStateFlow<List<Word>>(emptyList())
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    private val _searchQuery = MutableStateFlow<String>("")
    private val _selectedGroupIdFilter = MutableStateFlow<String?>(null)

    // !!! displayedWords ТЕПЕР БЕЗПЕЧНО ВИКОРИСТОВУЄ combine !!!
    val displayedWords: StateFlow<List<WordDisplayItem>> = combine(
        _allWords, // <<< Тепер оголошено вище
        searchQuery,
        selectedGroupIdFilter,
        _groups
    ) { allWords, currentSearchQuery, currentGroupFilterId, allGroups ->
        Log.d("WordListVM", "Recalculating displayedWords...")
        applyFiltersAndSearch(allWords, currentSearchQuery, currentGroupFilterId, allGroups)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    val groups: StateFlow<List<Group>> get() = _groups.asStateFlow()
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()
    val errorMessage: StateFlow<String?> get() = _errorMessage.asStateFlow()
    val searchQuery: StateFlow<String> get() = _searchQuery.asStateFlow()
    val selectedGroupIdFilter: StateFlow<String?> get() = _selectedGroupIdFilter.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    private val _errorMessage = MutableStateFlow<String?>(null)


    private var wordsListenerRegistration: ListenerRegistration? = null
    private var groupsListenerRegistration: ListenerRegistration? = null


    val wordsToLearnNowCount: StateFlow<Int> = _allWords.map { words ->
        val currentTime = System.currentTimeMillis()
        val count = words.count { it.nextReview <= currentTime && it.status != "mastered" }
        Log.d("WordListVM_Counts", "wordsToLearnNowCount: allWords.size=${words.size}, dueCount=$count") // Додай логування
        count
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L), // Змінено з Lazily
        initialValue = 0
    )

    val wordsInShortTermMemoryCount: StateFlow<Int> = _allWords.map { words ->
        val currentTime = System.currentTimeMillis()
        val count = words.count { it.nextReview > currentTime && it.status != "mastered" && it.repetition > 0 }
        Log.d("WordListVM_Counts", "wordsInShortTermMemoryCount: allWords.size=${words.size}, count=$count") // Додай логування
        count
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L), // Змінено з Lazily
        initialValue = 0
    )
    val learnedWordsCount: StateFlow<Int> = _allWords.map { words ->
        val count = words.count { it.status == "mastered" }
        Log.d("WordListVM_Counts", "learnedWordsCount: allWords.size=${words.size}, count=$count") // Додай логування
        count
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L), // Змінено з Lazily
        initialValue = 0
    )


    init {
        _selectedGroupIdFilter.value = ""
        loadWords()
        loadGroups()
    }

    fun loadWords() {
        _isLoading.value = true
        wordsListenerRegistration?.remove()
        wordsListenerRegistration = repository.getWordsListener { fetchedWords ->
            _allWords.value = fetchedWords
            _isLoading.value = false
            Log.d("WordListVM", "Words list updated from listener. Count: ${fetchedWords.size}")
        }
        if (wordsListenerRegistration == null) {
            _isLoading.value = false
            _errorMessage.value = "Не вдалося підписатись на оновлення слів. Користувач не авторизований?"
            _allWords.value = emptyList()
        }
    }

    fun loadGroups() {
        groupsListenerRegistration?.remove()
        groupsListenerRegistration = repository.getGroups { fetchedGroups ->
            val groupsWithFilters = mutableListOf<Group>()
            groupsWithFilters.add(Group(id = "no_group_filter", name = "Без групи"))
            groupsWithFilters.addAll(fetchedGroups)
            _groups.value = groupsWithFilters

            val currentSelected = _selectedGroupIdFilter.value
            if (currentSelected != null && currentSelected.isNotBlank() && currentSelected != "no_group_filter" && fetchedGroups.none { it.id == currentSelected }) {
                _selectedGroupIdFilter.value = ""
            }
        }
        if (groupsListenerRegistration == null) {
            _errorMessage.value = "Не вдалося підписатись на оновлення груп."
            _groups.value = emptyList()
        }
    }

    private fun applyFiltersAndSearch(
        allWords: List<Word>,
        currentSearchQuery: String,
        currentGroupFilterId: String?,
        allGroups: List<Group>
    ): List<WordDisplayItem> {
        val filteredWords = allWords.filter { word ->
            val matchesSearch = currentSearchQuery.isBlank() ||
                    word.text.contains(currentSearchQuery, ignoreCase = true) ||
                    word.translation.contains(currentSearchQuery, ignoreCase = true)

            val matchesGroup = when (currentGroupFilterId) {
                null, "" -> true
                "no_group_filter" -> word.dictionaryId.isNullOrBlank()
                else -> word.dictionaryId == currentGroupFilterId
            }
            matchesSearch && matchesGroup
        }
            .sortedBy { it.nextReview }

        val displayedItems = filteredWords.map { word ->
            val groupName = allGroups.firstOrNull { it.id == word.dictionaryId }?.name
            val progress = PracticeUtils.calculateProgress(word.repetition, word.interval)
            WordDisplayItem(word, groupName, progress)
        }

        return displayedItems
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }


    fun setGroupFilter(groupId: String?) {
        _selectedGroupIdFilter.value = groupId
    }
    fun deleteWord(wordId: String) {
        _isLoading.value = true
        repository.deleteWord(wordId) { success ->
            _isLoading.value = false
            _errorMessage.value = if (success) "Слово видалено." else "Помилка видалення слова."
        }
    }

    fun resetWord(word: Word) {
        val resetWord = word.copy(
            repetition = 0,
            easiness = 2.5f,
            interval = 0L,
            lastReviewed = 0,
            nextReview = System.currentTimeMillis(),
            status = "learning"
        )
        _isLoading.value = true
        repository.saveWord(resetWord) { success ->
            _isLoading.value = false
            _errorMessage.value = if (success) {
                "Статистику слова '${word.text}' скинуто."
            } else {
                "Помилка скидання статистики слова '${word.text}'."
            }
        }
    }

    fun onEditWordClicked(word: Word) {
        Log.d("WordListVM", "Edit clicked for word: ${word.id}")
    }

    fun formatNextReviewDate(timestamp: Long): String {
        if (timestamp == 0L) {
            return "Не вивчалося"
        }

        val now = System.currentTimeMillis()

        if (timestamp < now) {
            return "Не вивчалося"
        }

        if (timestamp == now) {
            return "На повторенні зараз"
        }
        val diff = timestamp - now

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val remainingDiffAfterDays = diff - TimeUnit.DAYS.toMillis(days)
        val hours = TimeUnit.MILLISECONDS.toHours(remainingDiffAfterDays)
        val remainingDiffAfterHours = remainingDiffAfterDays - TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingDiffAfterHours)


        val parts = mutableListOf<String>()

        // Додаємо дні, якщо вони є
        if (days > 0L) {
            parts.add("$days ${getDaysString(days)}")
        }
        // Додаємо години, якщо вони є
        if (hours > 0L) {
            parts.add("$hours ${getHoursString(hours)}")
        }
        // Додаємо хвилини, якщо вони є
        if (minutes > 0L) {
            parts.add("$minutes ${getMinutesString(minutes)}")
        }

        return when {
            parts.isEmpty() -> {
                "менше хвилини"
            }
            else -> "через ${parts.joinToString(" ")}"
        }
    }

    private fun getDaysString(days: Long): String {
        return when {
            days == 1L -> "день"
            days % 10L == 1L && days % 100L != 11L -> "день"
            days % 10L in 2L..4L && days % 100L !in 12L..14L -> "дні"
            else -> "днів"
        }
    }

    private   fun getHoursString(hours: Long): String {
        return when {
            hours == 1L -> "годину"
            hours % 10L == 1L && hours % 100L != 11L -> "годину"
            hours % 10L in 2L..4L && hours % 100L !in 12L..14L -> "години"
            else -> "годин"
        }
    }

    private fun getMinutesString(minutes: Long): String {
        return when {
            minutes == 1L -> "хвилину"
            minutes % 10L == 1L && minutes % 100L != 11L -> "хвилину"
            minutes % 10L in 2L..4L && minutes % 100L !in 12L..14L -> "хвилини"
            else -> "хвилин"
        }
    }

    fun playWordSound(word: Word) {
        if (word.translation.isNotBlank()) {
            ttsService.speak(word.translation)
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        wordsListenerRegistration?.remove()
        groupsListenerRegistration?.remove()
        ttsService.stop()
    }
}