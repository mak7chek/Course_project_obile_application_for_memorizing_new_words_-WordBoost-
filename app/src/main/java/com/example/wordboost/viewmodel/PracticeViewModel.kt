package com.example.wordboost.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.model.Word // Імпортуємо Word
import com.example.wordboost.data.repository.PracticeRepository // Імпортуємо PracticeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random // Імпортуємо Random

class PracticeViewModel(private val practiceRepository: PracticeRepository) : ViewModel() {

    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _isReverse = MutableStateFlow(false)
    val isReverse: StateFlow<Boolean> = _isReverse.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false) // Стан завантаження слів
    val isLoading : StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Завантажуємо слова для практики при ініціалізації ViewModel
        loadWordsForPractice()
    }

    fun loadWordsForPractice() {
        if (_isLoading.value) return

        _isLoading.value = true
        _words.value = emptyList() // Очищаємо список перед завантаженням
        _currentIndex.value = 0
        _isFlipped.value = false
        _statusMessage.value = null
        _isReverse.value = Random.nextBoolean()

        // Викликаємо репозиторій в coroutine
        viewModelScope.launch {
            practiceRepository.getWordsForPractice { fetchedWords ->
                _isLoading.value = false
                _words.value = fetchedWords
                _currentIndex.value = 0 // Скидаємо індекс на початок
                _isFlipped.value = false // Скидаємо перевертання
                _isReverse.value = Random.nextBoolean() // Рандом для першої картки
            }
        }
    }

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
        // Скидаємо повідомлення при перевертанні
        _statusMessage.value = null
    }

    /**
     * Обробляє результат практики для поточного слова та переходить до наступного.
     * @param quality Оцінка якості відповіді (0 або 5 у вашому випадку).
     */
    fun processSwipeResult(quality: Int) {
        // Не робимо нічого, якщо немає слів
        if (_words.value.isEmpty()) return

        val wordToUpdate = _words.value.getOrNull(_currentIndex.value) ?: return

        // Викликаємо репозиторій для оновлення слова в coroutine
        viewModelScope.launch {
            practiceRepository.updateWordAfterPractice(wordToUpdate, quality) {
                _statusMessage.value = when (quality) { // Оновлюємо статус повідомлення
                    5 -> "Чудово!"
                    0 -> "Не біда, вивчимо!"
                    else -> "Результат оброблено."
                }

                // Переходимо до наступного слова після успішного оновлення
                // Тут можна також оновити _words.value, якщо PracticeRepository повертає оновлений список
                // Або просто розрахувати наступний індекс і скинути UI стан

                val nextIndex = if (_words.value.isNotEmpty()) {
                    // Простий перехід до наступного слова по колу (якщо не видаляємо слова з списку)
                    (_currentIndex.value + 1) % _words.value.size
                } else {
                    0 // Або повернутися до 0, якщо список порожній
                }

                _currentIndex.value = nextIndex
                _isFlipped.value = false // Скидаємо перевертання для наступної картки
                _isReverse.value = Random.nextBoolean() // Встановлюємо новий рандомний реверс
                // Скидання стану свайпу в Composable буде зроблено через LaunchedEffect,
                // який реагує на зміну _isFlipped або _currentIndex
            }
        }
    }

    // Функція для очищення повідомлення після показу
    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}