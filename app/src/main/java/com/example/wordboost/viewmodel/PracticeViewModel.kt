package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.repository.PracticeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.wordboost.data.tts.TextToSpeechService
import com.example.wordboost.data.model.PracticeUtils // Імпортуємо PracticeUtils
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.getOrNull
import kotlin.collections.isNotEmpty

class PracticeViewModel(private val practiceRepository: PracticeRepository, private val ttsService:TextToSpeechService) : ViewModel() {

    // --- Стан UI та Дані ---
    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow() // Список слів для поточної ітерації сесії

    private val _currentIndex = MutableStateFlow(0) // Індекс в _words (буде завжди 0 при поточній логіці)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _isReverse = MutableStateFlow(false)
    val isReverse: StateFlow<Boolean> = _isReverse.asStateFlow() // Визначає, що на фронті (text/translation)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading : StateFlow<Boolean> = _isLoading.asStateFlow()

    // !!! НОВИЙ СТАН: Прогрес поточного слова для батарейки !!!
    private val _currentWordProgress = MutableStateFlow(0f)
    val currentWordProgress: StateFlow<Float> = _currentWordProgress.asStateFlow()

    // --- Стан Управління Сесією ---
    private val _allSessionWords = mutableListOf<Word>() // Оригінальний список завантажених слів для сесії

    init {
        loadWordsForPractice()
    }

    fun loadWordsForPractice() {
        if (_isLoading.value) return

        _isLoading.value = true
        _allSessionWords.clear() // Очищаємо список слів для сесії
        _words.value = emptyList() // Очищаємо список UI
        _currentIndex.value = 0
        _isFlipped.value = false
        _statusMessage.value = null
        _currentWordProgress.value = 0f


        viewModelScope.launch {
            practiceRepository.getWordsForPractice { fetchedWords ->
                _isLoading.value = false

                _allSessionWords.addAll(fetchedWords) // Зберігаємо завантажені слова

                // !!! Встановлюємо початковий список для UI (всі завантажені слова) !!!
                _words.value = _allSessionWords.toList() // Копія для UI
                _currentIndex.value = 0 // Індекс завжди 0 для UI списку

                // !!! Розраховуємо початковий реверс та прогрес для першого слова !!!
                if (_words.value.isNotEmpty()) {
                    val firstWord = _words.value.first()
                    _isReverse.value = calculateIsReverse(firstWord)
                    _currentWordProgress.value = PracticeUtils.calculateProgress(firstWord.repetition, firstWord.interval)
                } else {
                    _isReverse.value = false
                    _currentWordProgress.value = 0f
                }


                // !!! ТРИГЕР ПЕРШОГО ЗВУКУ ПІСЛЯ ЗАВАНТАЖЕННЯ СПИСКУ !!!
                playInitialWordSound()

                if (_words.value.isEmpty()) {
                    _statusMessage.value = "Слів для практики поки немає."
                } else {
                    _statusMessage.value = "Завантажено ${_allSessionWords.size} слів для практики."
                }
            }
        }
    }

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
        _statusMessage.value = null
    }

    /**
     * Обробляє результат практики для поточного слова, оновлює його та переходить до наступного.
     * Правильні слова видаляються з UI списку для поточної сесії. Неправильні залишаються.
     * Сесія продовжується з рештою слів.
     * @param quality Оцінка відповіді 0..5.
     */
    fun processSwipeResult(quality: Int) {
        if (_words.value.isEmpty()) {
            _statusMessage.value="Список слів для практики порожній."
            return
        }

        val wordToUpdate = _words.value.getOrNull(_currentIndex.value) // Беремо поточне слово
        if(wordToUpdate == null){
            _statusMessage.value ="Помилка: Не вдалося отримати поточне слово."
            return
        }

        viewModelScope.launch {
            // Оновлюємо статистику слова за SM-2 та зберігаємо у Firebase
            // practiceRepository.updateWordAfterPractice використовує ваш варіант PracticeUtils.sm2
            practiceRepository.updateWordAfterPractice(wordToUpdate, quality) {
                // !!! ЛОГІКА ПІСЛЯ УСПІШНОГО ОНОВЛЕННЯ В РЕПОЗИТОРІЇ !!!

                _statusMessage.value = when (quality) {
                    5 -> "Пам'ятаю чудово!"
                    4 -> "Пам'ятаю добре."
                    3 -> "Пам'ятаю, але з зусиллям."
                    in 0..2 -> "Забув / Не впевнений."
                    else -> "Результат оброблено."
                }

                // !!! УПРАВЛІННЯ СПИСКОМ UI ТА ІНДЕКСОМ !!!
                val listBeforeProcessing = _words.value // Поточний список у ViewModel

                val updatedListForUI = if (quality >= 3) {
                    // Якщо правильна відповідь (quality >= 3), видаляємо слово з поточного UI списку сесії
                    listBeforeProcessing.filter { it.id != wordToUpdate.id }
                } else {
                    // Якщо неправильна відповідь (quality < 3), слово залишається у поточному UI списку сесії
                    listBeforeProcessing
                }

                _words.value = updatedListForUI // Оновлюємо UI список

                // Визначаємо індекс наступного слова
                val nextIndex = if (quality >= 3) {
                    // Якщо слово видалили, індекс не змінюється (наступне слово "зсувається" на місце поточного)
                    // Але якщо видалили останнє слово, переходимо на 0
                    _currentIndex.value.coerceAtMost(updatedListForUI.size - 1) // Забезпечуємо, що індекс не вийде за межі, якщо список не порожній
                } else {
                    // Якщо слово залишилось, просто переходимо до наступного індексу
                    _currentIndex.value + 1
                }


                // !!! ОБРОБКА КІНЦЯ ПОТОЧНОЇ ІТЕРАЦІЇ СПИСКУ !!!
                if (updatedListForUI.isNotEmpty() && nextIndex >= updatedListForUI.size) {
                    // Досягли кінця поточного UI списку, але у списку ще є слова (неправильні відповіді).
                    // Починаємо наступну ітерацію з початку поточного списку.
                    _currentIndex.value = 0
                    _statusMessage.value = "Продовжуємо практику слів, що залишились (${updatedListForUI.size})." // Опціональне повідомлення
                } else if (updatedListForUI.isEmpty()) {
                    // Список UI порожній - всі слова з початкової сесії оброблені правильно.
                    _currentIndex.value = 0 // Індекс не має значення
                    _statusMessage.value = "Практичну сесію завершено!"
                    _currentWordProgress.value = 1.0f // Повний прогрес
                }
                else {
                    // Переходимо до наступного слова в поточній ітерації списку UI
                    _currentIndex.value = nextIndex
                }

                _isFlipped.value = false // Скидаємо перевертання для наступної картки

                // !!! Оновлюємо реверс та прогрес для НАСТУПНОГО слова (якщо воно існує) !!!
                if (_words.value.isNotEmpty()) {
                    val nextWord = _words.value.getOrNull(_currentIndex.value)
                    if (nextWord != null) {
                        _isReverse.value = calculateIsReverse(nextWord)
                        _currentWordProgress.value = PracticeUtils.calculateProgress(nextWord.repetition, nextWord.interval)
                    } else {
                        // Цей випадок теоретично не має статися, якщо _words.value не порожній
                        _isReverse.value = false
                        _currentWordProgress.value = 0f
                    }
                } else {
                    // Список UI порожній
                    _isReverse.value = false
                    _currentWordProgress.value = 0f
                }

                // !!! ТРИГЕР ЗВУКУ ДЛЯ НАСТУПНОГО СЛОВА (якщо воно існує) !!!
                if (_words.value.isNotEmpty()) {
                    playInitialWordSound()
                }
            }
        }
    }

    // !!! ЛОГІКА ВИЗНАЧЕННЯ НАПРЯМКУ КАРТКИ ЗА ВИВЧЕННЯМ !!!
    private fun calculateIsReverse(word: Word): Boolean {
        // Логіка: чергуємо напрямок залежно від Repetition count слова
        // Припускаємо, що Repetition count відображає етап вивчення.
        // Rep = 0: Нове або після помилки -> Англ на фронті (translation), Укр на звороті (text) -> isReverse = false
        // Rep = 1: Перший успіх -> Укр на фронті (text), Англ на звороті (translation) -> isReverse = true
        // Rep = 2: Другий успіх -> Англ на фронті -> isReverse = false
        // Rep = 3: Третій успіх -> Укр на фронті -> isReverse = true
        // ... і так далі. Реверс чергується залежно від парності Repetition count > 0.
        // Якщо Repetition = 0, завжди Англ на фронті.

        if (word.repetition == 0) {
            return false // Англійська (translation) на фронті
        }
        // Чергуємо напрямок: false для парних Rep, true для непарних Rep
        return word.repetition % 2 != 0 // Англійська на фронті, якщо Repetition непарне
    }


    // !!! ФУНКЦІЇ ДЛЯ ОЗВУЧУВАННЯ !!!

    // Приватна функція для відтворення звуку при першому показі слова
    private fun playInitialWordSound() {
        val currentWord = _words.value.firstOrNull() // Беремо поточне слово з UI списку
        if (currentWord != null) {
            // Озвучуємо англійський текст (word.translation assumed)
            val englishText = currentWord.translation
            // !!! ВИПРАВЛЕНО: speaks, якщо текст НЕ порожній/пустий !!!
            if (englishText.isNotBlank()) {
                ttsService.speak(englishText)
            }
        }
    }

    // Публічна функція для відтворення звуку при натисканні на іконку (replay)
    fun replayWordSound(word: Word) {
        // Озвучуємо англійський текст (word.translation assumed) переданого слова
        val englishText = word.translation
        // !!! ВИПРАВЛЕНО: speaks, якщо текст НЕ порожній/пустий !!!
        if (englishText.isNotBlank()) {
            ttsService.speak(englishText)
        }
    }


    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ttsService.stop()
    }
}