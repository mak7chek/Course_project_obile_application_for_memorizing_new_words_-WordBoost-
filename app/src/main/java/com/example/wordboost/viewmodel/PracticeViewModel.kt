package com.example.wordboost.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.util.PracticeUtils
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.tts.TextToSpeechService
import com.example.wordboost.data.util.Stack
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.annotation.VisibleForTesting

enum class PromptContentType { Original, Translation }
enum class CardState { Prompt, Answer }

data class WordMemento(
    val wordId: String, val repetition: Int, val easiness: Float, val interval: Long,
    val lastReviewed: Long, val nextReview: Long, val status: String
)

data class UndoState(
    val phase: PracticePhase, val batch: List<Word>, val wordIndexInBatch: Int,
    val cardState: CardState, val promptContentType: PromptContentType?,
    val wordBeforeAction: WordMemento? = null,
    val qualityOfAction: Int? = null
)

sealed class PracticePhase {
    object Loading : PracticePhase()
    data class BatchPairing(val wordsInBatch: List<Word>) : PracticePhase()
    data class BatchRegular(val wordsInBatch: List<Word>) : PracticePhase()
    data class Finished(
        val totalPracticedCount: Int,
        val correctAnswers: Int,
        val incorrectAnswers: Int,
        val newlyLearnedCount: Int
    ) : PracticePhase()
    data class Error(val message: String) : PracticePhase()
    object Empty : PracticePhase()
}

class PracticeViewModel(
    private val practiceRepository: PracticeRepository,
    private val ttsService: TextToSpeechService,
    private val authRepository: AuthRepository
) : ViewModel() {
    @VisibleForTesting
    internal fun forceSetPhaseForTesting(phase: PracticePhase) {
        _practicePhase.value = phase
    }

    private val _allWordsForSession = MutableStateFlow<List<Word>>(emptyList())
    private val _currentBatch = MutableStateFlow<List<Word>>(emptyList())
    val currentBatch: StateFlow<List<Word>> get() = _currentBatch.asStateFlow()
    private val _practicePhase = MutableStateFlow<PracticePhase>(PracticePhase.Loading)
    val practicePhase: StateFlow<PracticePhase> get() = _practicePhase.asStateFlow()
    private val _currentWordIndexInBatch = MutableStateFlow(0)
    val currentWordIndexInBatch: StateFlow<Int> get() = _currentWordIndexInBatch.asStateFlow()

    val currentWordInBatch: StateFlow<Word?> = combine(_currentBatch, _currentWordIndexInBatch) { batch, index ->
        batch.getOrNull(index)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _currentWordPromptContentType = MutableStateFlow(PromptContentType.Original)
    val currentWordPromptContentType: StateFlow<PromptContentType> get() = _currentWordPromptContentType.asStateFlow()
    private val _currentCardState = MutableStateFlow(CardState.Prompt)
    val currentCardState: StateFlow<CardState> get() = _currentCardState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage.asStateFlow()

    private var totalPracticedCount: Int = 0
    private var currentSessionCorrectAnswers: Int = 0
    private var currentSessionIncorrectAnswers: Int = 0
    private var currentSessionNewlyLearnedCount: Int = 0

    private val batchSize = 5

    private val undoStack = Stack<UndoState>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    internal val allWordsForSessionSnapshotForDebug: List<Word>
        get() = _allWordsForSession.value

    init {
        Log.i("PracticeVM_Lifecycle", "ViewModel initialized (init block). Will call startOrRefreshSession via UI.")
    }

    fun startOrRefreshSession() {
        Log.i("PracticeVM_Lifecycle", "[startOrRefreshSession] Called. Current phase: ${_practicePhase.value}")
        collectWordsForPractice()
    }

    private fun setRandomPromptContentType() {
        _currentWordPromptContentType.value = if (Random.nextBoolean()) PromptContentType.Original else PromptContentType.Translation
        Log.d("PracticeVM_State", "PromptContentType set to: ${_currentWordPromptContentType.value}")
    }

    private fun collectWordsForPractice() {
        Log.i("PracticeVM_Flow", "[collectWordsForPractice] Starting. Current phase before reset: ${_practicePhase.value}")
        _practicePhase.value = PracticePhase.Loading
        _errorMessage.value = null

        totalPracticedCount = 0
        currentSessionCorrectAnswers = 0
        currentSessionIncorrectAnswers = 0
        currentSessionNewlyLearnedCount = 0

        undoStack.clear()
        _canUndo.value = false
        Log.i("PracticeVM_Flow", "[collectWordsForPractice] States reset. Phase set to Loading.")

        viewModelScope.launch {
            Log.d("PracticeVM_Flow", "[collectWordsForPractice] Coroutine launched for collecting words.")
            try {
                practiceRepository.getWordsNeedingPracticeFlow()
                    .collect { wordsFromFlow ->
                        Log.i("PracticeVM_Flow", "[Collector] Received ${wordsFromFlow.size} words from repository. Current VM Phase: ${_practicePhase.value}")
                        _allWordsForSession.value = wordsFromFlow

                        val currentVmPhase = _practicePhase.value
                        if (currentVmPhase == PracticePhase.Loading) {
                            if (wordsFromFlow.isNotEmpty()) {
                                Log.i("PracticeVM_Flow", "[Collector] (Loading Phase): Words available (${wordsFromFlow.size}). Calling startNextPracticeBatch().")
                                startNextPracticeBatch()
                            } else {
                                Log.i("PracticeVM_Flow", "[Collector] (Loading Phase): No words from flow. Transitioning to Empty.")
                                _practicePhase.value = PracticePhase.Empty
                            }
                        } else if (wordsFromFlow.isNotEmpty() && (currentVmPhase is PracticePhase.Empty || currentVmPhase is PracticePhase.Finished)) {
                            Log.i("PracticeVM_Flow", "[Collector] (Phase ${currentVmPhase}): New words appeared (${wordsFromFlow.size}). Starting new session via startNextPracticeBatch().")
                            startNextPracticeBatch()
                        } else if (wordsFromFlow.isEmpty() && _currentBatch.value.isEmpty() &&
                            currentVmPhase !is PracticePhase.Empty && currentVmPhase !is PracticePhase.Finished &&
                            currentVmPhase !is PracticePhase.Error) {
                            Log.i("PracticeVM_Flow", "[Collector] (Active Phase ${currentVmPhase}): Word flow AND current batch are empty. Transitioning to Finished/Empty.")
                            _practicePhase.value = if (totalPracticedCount > 0) {
                                PracticePhase.Finished(
                                    totalPracticedCount,
                                    currentSessionCorrectAnswers,
                                    currentSessionIncorrectAnswers,
                                    currentSessionNewlyLearnedCount
                                )
                            } else PracticePhase.Empty
                        } else {
                            Log.d("PracticeVM_Flow", "[Collector] Words update (${wordsFromFlow.size}), current phase ($currentVmPhase) doesn't require immediate new batch start from here. _allWordsForSession updated.")
                        }
                    }
            } catch (e: Exception) {
                Log.e("PracticeVM_Flow", "[collectWordsForPractice] Exception during collection: ${e.message}", e)
                _practicePhase.value = PracticePhase.Error("Помилка завантаження слів для практики: ${e.localizedMessage}")
            }
        }
    }

    fun flipCard() {
        Log.d("PracticeVM_Action", "flipCard called. Current CardState: ${_currentCardState.value}")
        ttsService.stop()
        _currentCardState.value = if (_currentCardState.value == CardState.Prompt) CardState.Answer else CardState.Prompt
        Log.d("PracticeVM_State", "CardState changed to: ${_currentCardState.value}")
    }

    private fun startNextPracticeBatch() {
        Log.i("PracticeVM_Batch", "[startNextPracticeBatch] Attempting new batch. totalPracticed: $totalPracticedCount")
        ttsService.stop()

        val allAvailableWords = _allWordsForSession.value
        Log.d("PracticeVM_Batch", "_allWordsForSession count: ${allAvailableWords.size}. Words: ${allAvailableWords.joinToString { "'${it.text}'(rev:${it.nextReview},st:${it.status})" }}")

        if (allAvailableWords.isEmpty()) {
            Log.i("PracticeVM_Batch", "No words in _allWordsForSession. Current phase: ${_practicePhase.value}. Transitioning to Finished/Empty.")
            _currentBatch.value = emptyList()
            if (_practicePhase.value !is PracticePhase.Finished && _practicePhase.value !is PracticePhase.Empty) {
                _practicePhase.value = if (totalPracticedCount > 0) {
                    PracticePhase.Finished(
                        totalPracticedCount,
                        currentSessionCorrectAnswers,
                        currentSessionIncorrectAnswers,
                        currentSessionNewlyLearnedCount
                    )
                } else PracticePhase.Empty
                Log.i("PracticeVM_State", "Phase set to: ${_practicePhase.value} (allAvailableWords is empty)")
            }
            return
        }

        val nextBatchWords = allAvailableWords.take(batchSize)
        _currentBatch.value = nextBatchWords
        _currentWordIndexInBatch.value = 0
        Log.i("PracticeVM_Batch", "New current batch: ${nextBatchWords.size} words: ${nextBatchWords.joinToString { it.text }}. Index reset to 0.")

        if (nextBatchWords.isNotEmpty()) {
            _practicePhase.value = PracticePhase.BatchPairing(nextBatchWords)
            _currentCardState.value = CardState.Prompt
            setRandomPromptContentType()
            Log.i("PracticeVM_State", "Phase set to BatchPairing/Regular with ${nextBatchWords.size} words. CardState: Prompt.")
        } else {
            Log.w("PracticeVM_Batch", "allAvailableWords was not empty, but nextBatch is. Unexpected! Forcing Finished/Empty.")
            if (_practicePhase.value !is PracticePhase.Finished && _practicePhase.value !is PracticePhase.Empty) {
                _practicePhase.value = if (totalPracticedCount > 0) {
                    PracticePhase.Finished(
                        totalPracticedCount,
                        currentSessionCorrectAnswers,
                        currentSessionIncorrectAnswers,
                        currentSessionNewlyLearnedCount
                    )
                } else PracticePhase.Empty
                Log.i("PracticeVM_State", "Phase set to: ${_practicePhase.value} (nextBatch unexpectedly empty)")
            }
        }
    }

    fun onPairingFinished() {
        Log.i("PracticeVM_Action", "onPairingFinished. Current batch size: ${_currentBatch.value.size}, words: ${_currentBatch.value.joinToString { it.text }}")
        if (_currentBatch.value.isNotEmpty()) {
            pushStateForUndo(null, qualityOfAction = null)
            _practicePhase.value = PracticePhase.BatchRegular(_currentBatch.value)
            _currentWordIndexInBatch.value = 0
            _currentCardState.value = CardState.Prompt
            setRandomPromptContentType()
            ttsService.stop()
            Log.i("PracticeVM_State", "Transitioned to BatchRegular. Index: 0. CardState: Prompt.")
        } else {
            Log.w("PracticeVM_Action", "onPairingFinished: _currentBatch is EMPTY! This is unexpected. Trying to start next batch.")
            startNextPracticeBatch()
        }
    }

    fun onPairMatched(wordId: String) {
        val matchedWord = _currentBatch.value.firstOrNull { it.id == wordId }
        if (matchedWord != null) {
            Log.d("PracticeVM_Action", "onPairMatched: Word '${matchedWord.text}' (ID: $wordId). Simplified processing.")
            processAnswerAndUpdateWord(matchedWord, 4)
        } else {
            Log.e("PracticeVM_Error", "onPairMatched: Word with ID $wordId not found in current batch: ${_currentBatch.value.joinToString { it.id }}")
        }
    }

    fun onCardSwipedLeft() {
        Log.d("PracticeVM_Action", "onCardSwipedLeft called for: ${currentWordInBatch.value?.text}")
        currentWordInBatch.value?.let {
            pushStateForUndo(createWordMemento(it), qualityOfAction = 2)
            processAnswerAndUpdateWord(it, 2)
        } ?: Log.w("PracticeVM_Action", "onCardSwipedLeft: currentWordInBatch is null, cannot process.")
    }

    fun onCardSwipedRight() {
        Log.d("PracticeVM_Action", "onCardSwipedRight called for: ${currentWordInBatch.value?.text}")
        currentWordInBatch.value?.let {
            pushStateForUndo(createWordMemento(it), qualityOfAction = 5)
            processAnswerAndUpdateWord(it, 5)
        } ?: Log.w("PracticeVM_Action", "onCardSwipedRight: currentWordInBatch is null, cannot process.")
    }

    private fun createWordMemento(word: Word): WordMemento { return WordMemento(word.id,word.repetition,word.easiness,word.interval,word.lastReviewed,word.nextReview,word.status) }
    private fun restoreWordFromMemento(word: Word, memento: WordMemento): Word
    {
        return word.copy(repetition=memento.repetition,easiness=memento.easiness,interval=memento.interval,lastReviewed=memento.lastReviewed,nextReview=memento.nextReview,status=memento.status)
    }

    private fun processAnswerAndUpdateWord(word: Word, quality: Int) {
        Log.i("PracticeVM_Process", "Processing answer for '${word.text}', quality $quality.")

        if (quality >= 3) {
            currentSessionCorrectAnswers++
            Log.d("PracticeVM_Stats", "Correct answer registered. Total correct: $currentSessionCorrectAnswers")
        } else {
            currentSessionIncorrectAnswers++
            Log.d("PracticeVM_Stats", "Incorrect answer registered. Total incorrect: $currentSessionIncorrectAnswers")
        }

        val oldStatus = word.status

        val (rep, ef, interval) = PracticeUtils.sm2(word.repetition, word.easiness, word.interval, quality)
        val now = System.currentTimeMillis()
        val nextReviewTime = now + interval
        val newStatus = PracticeUtils.determineStatus(rep, interval)
        val updatedWord = word.copy(repetition = rep, easiness = ef, interval = interval, lastReviewed = now, nextReview = nextReviewTime, status = newStatus)
        Log.d("PracticeVM_Process", "Updated word local stats: nextReview=${updatedWord.nextReview}, status='${updatedWord.status}' from rep=${rep}, int=${interval/(60000)}min")

        if (PracticeUtils.isWordConsideredLearned(newStatus) && !PracticeUtils.isWordConsideredLearned(oldStatus)) {
            currentSessionNewlyLearnedCount++
            Log.d("PracticeVM_Stats", "Word '${updatedWord.text}' newly learned (status: $newStatus -> $oldStatus). Total newly learned: $currentSessionNewlyLearnedCount")
        }

        viewModelScope.launch {
            practiceRepository.saveWord(updatedWord) { success ->
                Log.i("PracticeVM_Process", "saveWord CALLBACK for '${updatedWord.text}'. Success: $success")
                if (!success) {
                    Log.e("PracticeVM_Process", "Failed to save word ${updatedWord.text} in Firebase.")
                    _errorMessage.value = "Помилка збереження прогресу слова."
                }
                totalPracticedCount++
                Log.d("PracticeVM_Process", "totalPracticedCount is now $totalPracticedCount")


                val currentPhase = _practicePhase.value
                if (currentPhase is PracticePhase.BatchRegular) {
                    val nextWordIndex = _currentWordIndexInBatch.value + 1
                    Log.d("PracticeVM_Process", "In BatchRegular. Batch size: ${_currentBatch.value.size}. Prev index: ${_currentWordIndexInBatch.value}, Next attempt: $nextWordIndex")
                    if (nextWordIndex >= _currentBatch.value.size) {
                        Log.i("PracticeVM_Process", "BatchRegular finished for batch. Calling startNextPracticeBatch().")
                        startNextPracticeBatch()
                    } else {
                        _currentWordIndexInBatch.value = nextWordIndex
                        _currentCardState.value = CardState.Prompt
                        setRandomPromptContentType()
                        ttsService.stop()
                        Log.i("PracticeVM_State", "Moved to next word in BatchRegular. New index: $nextWordIndex. CardState: Prompt.")
                    }
                } else if (currentPhase is PracticePhase.BatchPairing) {
                    Log.d("PracticeVM_Process", "Word processed in BatchPairing for '${updatedWord.text}'. (No index/phase change here)")
                } else {
                    Log.w("PracticeVM_Process", "Word processed in UNEXPECTED phase: $currentPhase.")
                }
            }
        }
    }

    private fun pushStateForUndo(wordMemento: WordMemento?, qualityOfAction: Int? = null) {
        val contentTypeForUndo = if (_currentCardState.value == CardState.Prompt) _currentWordPromptContentType.value else null
        val stateToPush = UndoState(
            phase = _practicePhase.value, batch = _currentBatch.value.toList(),
            wordIndexInBatch = _currentWordIndexInBatch.value, cardState = _currentCardState.value,
            promptContentType = contentTypeForUndo, wordBeforeAction = wordMemento,
            qualityOfAction = qualityOfAction
        )
        undoStack.push(stateToPush)
        _canUndo.value = true
        Log.d("UndoDebug", "Pushed state. Stack size: ${undoStack.size()}. Details: Phase=${stateToPush.phase}, WordBefore=${stateToPush.wordBeforeAction?.wordId}, Quality=${stateToPush.qualityOfAction}")
    }

    fun undoLastAction() {
        if (undoStack.isEmpty()) {
            Log.d("UndoDebug", "Undo stack empty. No action.")
            _errorMessage.value = "Немає дій для скасування."
            _canUndo.value = false; return
        }
        val lastState = undoStack.pop()!!
        Log.i("UndoDebug", "Popped state for restore: Phase=${lastState.phase}, Batch words: ${lastState.batch.map{it.text}}, Index=${lastState.wordIndexInBatch}, CardState=${lastState.cardState}, Quality=${lastState.qualityOfAction}")

        _practicePhase.value = lastState.phase
        _currentBatch.value = lastState.batch.toList()
        _currentWordIndexInBatch.value = lastState.wordIndexInBatch
        _currentCardState.value = lastState.cardState

        if (lastState.cardState == CardState.Prompt && _practicePhase.value is PracticePhase.BatchRegular) {
            if (lastState.promptContentType != null) {
                _currentWordPromptContentType.value = lastState.promptContentType
            } else {
                setRandomPromptContentType()
                Log.w("UndoDebug", "Restored Prompt state for BatchRegular, but promptContentType was null in UndoState. Set to random.")
            }
        }


        lastState.wordBeforeAction?.let { memento ->
            if (totalPracticedCount > 0) {
                totalPracticedCount--
                Log.d("UndoDebug", "totalPracticedCount decremented to $totalPracticedCount for word ${memento.wordId}")
            }

            lastState.qualityOfAction?.let { quality ->
                Log.d("UndoDebug", "Undoing action with quality $quality for stats.")
                if (quality >= 3) {
                    if (currentSessionCorrectAnswers > 0) currentSessionCorrectAnswers--
                    Log.d("UndoDebug", "Decremented correctAnswers. Now: $currentSessionCorrectAnswers")
                } else {
                    if (currentSessionIncorrectAnswers > 0) currentSessionIncorrectAnswers--
                    Log.d("UndoDebug", "Decremented incorrectAnswers. Now: $currentSessionIncorrectAnswers")
                }
            }

            viewModelScope.launch {
                val wordToRestoreFromRepo = practiceRepository.getWordById(memento.wordId)
                    ?: _allWordsForSession.value.find { it.id == memento.wordId }

                if (wordToRestoreFromRepo != null) {
                    val statusAfterUndoneAction = wordToRestoreFromRepo.status
                    val restoredWord = restoreWordFromMemento(wordToRestoreFromRepo, memento)

                    if (PracticeUtils.isWordConsideredLearned(statusAfterUndoneAction) && !PracticeUtils.isWordConsideredLearned(restoredWord.status)) {
                        if (currentSessionNewlyLearnedCount > 0) {
                            currentSessionNewlyLearnedCount--
                            Log.d("UndoDebug", "Decremented newlyLearnedCount due to unlearning. Word: ${restoredWord.text}, Status after action: $statusAfterUndoneAction -> Status in memento: ${restoredWord.status}. Now: $currentSessionNewlyLearnedCount")
                        }
                    }

                    Log.d("UndoDebug", "Restoring word '${restoredWord.text}' (status: ${restoredWord.status}) to Firebase.")
                    practiceRepository.saveWord(restoredWord) { success ->
                        if (success) Log.d("UndoDebug", "Word '${restoredWord.text}' successfully restored in Firebase.")
                        else { Log.e("UndoDebug", "Error restoring word '${restoredWord.text}' in Firebase."); _errorMessage.value = "Помилка відновлення стану слова." }
                    }
                } else { Log.e("UndoDebug", "Word with ID ${memento.wordId} not found for restoration."); _errorMessage.value = "Не вдалося скасувати: слово для відновлення не знайдено." }
            }
        } ?: run {
            Log.d("UndoDebug", "Undoing a phase transition (e.g., from BatchRegular back to BatchPairing via onPairingFinished undo). No word-specific stats or totalPracticedCount changed by this specific undo step.")
        }

        _canUndo.value = !undoStack.isEmpty()
        Log.i("UndoDebug", "_canUndo set to: ${_canUndo.value}. Stack empty: ${undoStack.isEmpty()}. Current restored phase: ${_practicePhase.value}")

        ttsService.stop()
    }

    fun speakTranslationText(translationText: String) { if (translationText.isNotBlank()) ttsService.speak(translationText) }
    fun clearErrorMessage() { _errorMessage.value = null }
    override fun onCleared() { super.onCleared(); ttsService.shutdown(); Log.i("PracticeVM_Lifecycle", "ViewModel onCleared, TTS shutdown.") }
}