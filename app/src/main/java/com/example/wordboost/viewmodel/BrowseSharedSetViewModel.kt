package com.example.wordboost.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.SharedCardSet
import com.example.wordboost.data.model.SharedSetWordItem
import com.example.wordboost.data.model.UserSharedSetProgress
import com.example.wordboost.data.model.Word // Твоя основна модель Word для особистого словника
import com.example.wordboost.data.tts.TextToSpeechService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID



data class SharedSetDetailsWithWords(
    val setInfo: SharedCardSet,          // Метадані самого набору
    val words: List<SharedSetWordItem> // Список слів у цьому наборі
)
class BrowseSharedSetViewModel(
    private val sharedSetId: String,
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val ttsService: TextToSpeechService
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _setDetailsWithWords = MutableStateFlow<SharedSetDetailsWithWords?>(null)
    val setDetailsWithWords: StateFlow<SharedSetDetailsWithWords?> = _setDetailsWithWords.asStateFlow()

    private val _currentDisplayWordItem = MutableStateFlow<SharedSetWordItem?>(null)
    val currentDisplayWordItem: StateFlow<SharedSetWordItem?> = _currentDisplayWordItem.asStateFlow()

    private val _showTranslation = MutableStateFlow(false)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    private val _isSetCompleted = MutableStateFlow(false)
    val isSetCompleted: StateFlow<Boolean> = _isSetCompleted.asStateFlow()

    private val _currentWordListPosition = MutableStateFlow(0)
    val currentWordListPositionDisplay: StateFlow<Int> = _currentWordListPosition

    val totalWordsInSet: StateFlow<Int> = MutableStateFlow(0)
    val operationStatus = MutableStateFlow<String?>(null) // Для Snackbar

    private var userProgressData: UserSharedSetProgress? = null
    private var translationDisplayJob: Job? = null
    private val translationDelayMs = 2500L

    private var allWordsInCurrentSet: List<SharedSetWordItem> = emptyList()
    private var ignoredWordIdsInThisSet: MutableSet<String> = mutableSetOf()


    init {
        Log.d("BrowseSetVM", "Initializing for setId: $sharedSetId")
        loadSetAndUserProgress()
    }

    private fun loadSetAndUserProgress() {
        viewModelScope.launch {
            _isLoading.value = true
            operationStatus.value = null
            val userId = authRepository.getCurrentUser()?.uid
            if (userId == null) {
                Log.e("BrowseSetVM", "User not logged in!")
                operationStatus.value = "Помилка: користувач не авторизований."
                _isLoading.value = false
                _isSetCompleted.value = true // Показуємо екран завершення/помилки
                return@launch
            }

            userProgressData = firebaseRepository.getUserProgressForSharedSet(userId, sharedSetId)
            _currentWordListPosition.value = userProgressData?.currentWordIndex ?: 0
            ignoredWordIdsInThisSet = userProgressData?.ignoredWordsInSet?.toMutableSet() ?: mutableSetOf()
            val previouslyCompleted = userProgressData?.isCompleted ?: false

            Log.d("BrowseSetVM", "Loaded user progress: Index=${_currentWordListPosition.value}, PreviouslyCompleted=$previouslyCompleted, Ignored count: ${ignoredWordIdsInThisSet.size}")

            val setResult = firebaseRepository.getSharedCardSetWithWords(sharedSetId)
            _isLoading.value = false

            setResult.fold(
                onSuccess = { setWithWords ->
                    allWordsInCurrentSet = setWithWords.words
                    _setDetailsWithWords.value = setWithWords // Зберігаємо для доступу до setInfo
                    (totalWordsInSet as MutableStateFlow).value = allWordsInCurrentSet.size
                    Log.d("BrowseSetVM", "Set '${setWithWords.setInfo.name_uk}' loaded with ${allWordsInCurrentSet.size} words.")

                    if (allWordsInCurrentSet.isEmpty()) {
                        _isSetCompleted.value = true
                        _currentDisplayWordItem.value = null
                        saveUserProgress(completed = true, currentIndex = 0) // Зберігаємо, що порожній набір "пройдено"
                    } else {
                        // Якщо індекс вийшов за межі або набір був пройдений, але слова додалися
                        if (_currentWordListPosition.value >= allWordsInCurrentSet.size) {
                            if (!previouslyCompleted) { // Якщо не був завершений, а індекс за межами (набір зменшився)
                                Log.w("BrowseSetVM", "Current index ${_currentWordListPosition.value} out of bounds (${allWordsInCurrentSet.size}). Resetting to 0.")
                                _currentWordListPosition.value = 0
                                _isSetCompleted.value = false
                            } else { // Був завершений і індекс в кінці або за межами
                                _isSetCompleted.value = true
                            }
                        } else { // Індекс в межах
                            _isSetCompleted.value = previouslyCompleted && (_currentWordListPosition.value >= allWordsInCurrentSet.size)
                        }

                        if (_isSetCompleted.value) {
                            Log.d("BrowseSetVM", "Set is marked as completed. No card to display.")
                            _currentDisplayWordItem.value = null
                        } else {
                            displayNextWord()
                        }
                    }
                },
                onFailure = { exception ->
                    Log.e("BrowseSetVM", "Error loading shared set: ${exception.message}")
                    operationStatus.value = "Помилка завантаження набору: ${exception.message}"
                    _currentDisplayWordItem.value = null
                    _isSetCompleted.value = true
                }
            )
        }
    }

    private fun displayNextWord() {
        translationDisplayJob?.cancel()
        _showTranslation.value = false

        if (allWordsInCurrentSet.isEmpty()) {
            _currentDisplayWordItem.value = null
            _isSetCompleted.value = true
            saveUserProgress(completed = true, currentIndex = _currentWordListPosition.value)
            Log.d("BrowseSetVM", "displayNextWord: No words in set.")
            return
        }

        var nextIndexToShow = _currentWordListPosition.value
        var wordToShow: SharedSetWordItem? = null

        while(nextIndexToShow < allWordsInCurrentSet.size) {
            val potentialWord = allWordsInCurrentSet[nextIndexToShow]
            if (ignoredWordIdsInThisSet.contains(potentialWord.id)) {
                Log.d("BrowseSetVM", "Skipping ignored word at index $nextIndexToShow: ${potentialWord.originalText}")
                nextIndexToShow++
            } else {
                wordToShow = potentialWord
                break
            }
        }

        _currentWordListPosition.value = nextIndexToShow // Оновлюємо позицію до фактично показаного слова або кінця списку

        if (wordToShow != null) {
            _currentDisplayWordItem.value = wordToShow
            _isSetCompleted.value = false // Якщо ми показуємо слово, набір ще не завершено
            Log.d("BrowseSetVM", "Displaying word: '${wordToShow.originalText}' (index in list: $nextIndexToShow)")
            translationDisplayJob = viewModelScope.launch {
                delay(translationDelayMs)
                _showTranslation.value = true
            }
        } else {
            // Всі слова пройдені або проігноровані
            Log.d("BrowseSetVM", "All words processed/ignored. Marking set as completed.")
            _currentDisplayWordItem.value = null
            _isSetCompleted.value = true
            // _currentWordListPosition.value вже буде = allWordsInCurrentSet.size
            saveUserProgress(completed = true, currentIndex = _currentWordListPosition.value)
        }
    }

    fun onWordSwipedUp(sharedWordItem: SharedSetWordItem) {
        Log.d("BrowseSetVM", "Word '${sharedWordItem.originalText}' SWIPED UP (add to personal).")
        processWordAction(sharedWordItem, true)
    }

    fun onWordSwipedDown(sharedWordItem: SharedSetWordItem) {
        Log.d("BrowseSetVM", "Word '${sharedWordItem.originalText}' SWIPED DOWN (ignore).")
        processWordAction(sharedWordItem, false)
    }

    private fun processWordAction(sharedWordItem: SharedSetWordItem, addToVocabulary: Boolean) {
        translationDisplayJob?.cancel()
        _showTranslation.value = false
        _currentDisplayWordItem.value = null // Прибираємо поточну картку, поки обробляється

        viewModelScope.launch {
            if (addToVocabulary) {
                val newPersonalWord = Word(
                    id = UUID.randomUUID().toString(),
                    text = sharedWordItem.translationText,
                    translation = sharedWordItem.originalText,
                    repetition = 0, easiness = 2.5f, interval = 0L, lastReviewed = 0L,
                    nextReview = System.currentTimeMillis(), status = "new"
                )
                // Використовуємо suspend версію для збереження
                val success = firebaseRepository.savePersonalWordSuspend(newPersonalWord)
                operationStatus.value = if (success) {
                    Log.i("BrowseSetVM", "Word '${newPersonalWord.text}' added to user's vocabulary.")
                    "'${newPersonalWord.text}' додано до словника!"
                } else {
                    Log.e("BrowseSetVM", "Failed to add word '${newPersonalWord.text}' to user's vocabulary.")
                    "Помилка додавання '${newPersonalWord.text}'."
                }
            } else {
                // Додаємо до списку ігнорованих
                if (!ignoredWordIdsInThisSet.contains(sharedWordItem.id)) {
                    ignoredWordIdsInThisSet.add(sharedWordItem.id)
                }
                operationStatus.value = "'${sharedWordItem.originalText}' проігноровано."
            }

            // Збільшуємо індекс, щоб вказувати на НАСТУПНЕ слово для displayNextWord
            // І зберігаємо поточний стан (збільшений індекс та оновлений список ігнорованих)
            _currentWordListPosition.value++
            saveUserProgress(
                completed = (_currentWordListPosition.value >= allWordsInCurrentSet.size),
                currentIndex = _currentWordListPosition.value,
                currentIgnoredList = ignoredWordIdsInThisSet.toList()
            )
            displayNextWord() // Показуємо наступне слово або завершуємо
        }
    }

    fun replayWordSound() {
        currentDisplayWordItem.value?.let { // Використовуємо вже оновлений StateFlow
            Log.d("BrowseSetVM", "Replaying sound for '${it.originalText}'")
            ttsService.speak(it.originalText)
        }
    }

    private fun saveUserProgress(
        completed: Boolean,
        currentIndex: Int,
        currentIgnoredList: List<String>? = null // Дозволяємо передати оновлений список ігнорованих
    ) {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            Log.e("BrowseSetVM", "Cannot save progress, user not logged in.")
            return
        }

        val progress = UserSharedSetProgress(
            currentWordIndex = currentIndex,
            isCompleted = completed,
            lastAccessed = null, // Firestore встановить серверний час завдяки @ServerTimestamp
            ignoredWordsInSet = currentIgnoredList ?: userProgressData?.ignoredWordsInSet ?: emptyList()
        )
        // Оновлюємо локальну копію userProgressData для узгодженості
        userProgressData = progress.copy(ignoredWordsInSet = progress.ignoredWordsInSet.toList())


        viewModelScope.launch {
            val success = firebaseRepository.saveUserProgressForSharedSet(userId, sharedSetId, progress)
            if (success) {
                Log.d("BrowseSetVM", "User progress for set $sharedSetId saved. Index: ${progress.currentWordIndex}, Completed: ${progress.isCompleted}, Ignored: ${progress.ignoredWordsInSet.size}")
            } else {
                Log.e("BrowseSetVM", "Failed to save user progress for set $sharedSetId.")
                // Можливо, варто спробувати зберегти пізніше або повідомити користувача
            }
        }
    }

    fun clearOperationStatus() {
        operationStatus.value = null
    }

    override fun onCleared() {
        translationDisplayJob?.cancel()
        Log.d("BrowseSetVM", "ViewModel for $sharedSetId cleared.")
        super.onCleared()
    }
}
