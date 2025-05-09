package com.example.wordboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.model.PracticeUtils
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.tts.TextToSpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

import java.util.*
import kotlin.math.min
import android.util.Log
import kotlin.random.Random

import com.example.wordboost.data.util.Stack




enum class PromptContentType {
    Original,
    Translation
}

enum class CardState {
    Prompt,
    Answer
}

data class WordMemento(
    val wordId: String,
    val repetition: Int,
    val easiness: Float,
    val interval: Long,
    val lastReviewed: Long,
    val nextReview: Long,
    val status: String
)

data class UndoState(
    val phase: PracticePhase,
    val batch: List<Word>,
    val wordIndexInBatch: Int,
    val cardState: CardState,
    val wordBeforeAction: WordMemento? = null
)


class PracticeViewModel(
    private val practiceRepository: PracticeRepository,
    private val ttsService: TextToSpeechService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _allWordsForSession = MutableStateFlow<List<Word>>(emptyList())

    private val _currentBatch = MutableStateFlow<List<Word>>(emptyList())
    val currentBatch: StateFlow<List<Word>> get() = _currentBatch.asStateFlow()

    private val _practicePhase = MutableStateFlow<PracticePhase>(PracticePhase.Loading)
    val practicePhase: StateFlow<PracticePhase> get() = _practicePhase.asStateFlow()

    private val _currentWordIndexInBatch = MutableStateFlow(0)
    val currentWordIndexInBatch: StateFlow<Int> get() = _currentWordIndexInBatch.asStateFlow()

    val currentWordInBatch: StateFlow<Word?> = _currentBatch.map { batchWords ->
        batchWords.getOrNull(_currentWordIndexInBatch.value)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    private val _currentWordPromptContentType = MutableStateFlow<PromptContentType>(PromptContentType.Original)
    val currentWordPromptContentType: StateFlow<PromptContentType> get() = _currentWordPromptContentType.asStateFlow()

    private val _currentCardState = MutableStateFlow<CardState>(CardState.Prompt)
    val currentCardState: StateFlow<CardState> get() = _currentCardState.asStateFlow()


    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage.asStateFlow()

    private var totalPracticedCount: Int = 0

    private val batchSize = 5

    private val undoStack = Stack<UndoState>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()


    init {
        collectWordsForPractice()
    }

    private fun setRandomPromptContentType() {
        _currentWordPromptContentType.value = if (Random.nextBoolean()) {
            Log.d("PracticeVM", "setRandomPromptContentType: Setting PromptContentType to Original.")
            PromptContentType.Original
        } else {
            Log.d("PracticeVM", "setRandomPromptContentType: Setting PromptContentType to Translation.")
            PromptContentType.Translation
        }
    }

    private fun collectWordsForPractice() {
        _practicePhase.value = PracticePhase.Loading
        _errorMessage.value = null
        totalPracticedCount = 0
        undoStack.clear()
        _canUndo.value = false

        viewModelScope.launch {
            try {
                practiceRepository.getWordsNeedingPracticeFlow()
                    .collect { words ->
                        Log.d("PracticeVM", "Received ${words.size} words from practice Flow.")
                        _allWordsForSession.value = words

                        val currentPhase = _practicePhase.value
                        if (words.isNotEmpty() && (currentPhase == PracticePhase.Loading ||
                                    currentPhase is PracticePhase.Finished ||
                                    currentPhase is PracticePhase.Empty ||
                                    (currentPhase is PracticePhase.BatchPairing && _currentBatch.value.isEmpty()) ||
                                    (currentPhase is PracticePhase.BatchRegular && _currentBatch.value.isEmpty())))
                        {
                            if (!(currentPhase == PracticePhase.Loading && words.isEmpty())) {
                                pushStateForUndo(null)
                                Log.d("UndoDebug", "Pushing phase state before starting next batch/phase transition (Flow collect).")
                            } else {
                                Log.d("UndoDebug", "Not pushing state from Loading to Empty.")
                            }
                            startNextPracticeBatch()
                        } else if (words.isEmpty() && (currentPhase == PracticePhase.Loading || currentPhase is PracticePhase.BatchPairing || currentPhase is PracticePhase.BatchRegular)) {
                            if (_currentBatch.value.isEmpty()) {
                                pushStateForUndo(null)
                                Log.d("UndoDebug", "Pushing phase state before transitioning to Empty/Finished (Flow collect).")
                                _practicePhase.value = if (totalPracticedCount > 0) PracticePhase.Finished(totalPracticedCount) else PracticePhase.Empty
                            }
                        } else {
                            if (currentPhase == PracticePhase.Loading && words.isEmpty()) {
                                Log.d("PracticeVM", "Loading finished, no words in Flow. Transitioning to Empty phase.")
                                pushStateForUndo(null)
                                Log.d("UndoDebug", "Pushing phase state before transitioning from Loading to Empty.")
                                _practicePhase.value = PracticePhase.Empty
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("PracticeVM", "Error collecting words for practice: ${e.message}", e)
                _practicePhase.value = PracticePhase.Error("Помилка завантаження слів: ${e.message}")
                undoStack.clear()
                _canUndo.value = false
            }
        }
    }


    fun flipCard() {
        Log.d("PracticeVM", "flipCard: Called. Current CardState: ${_currentCardState.value}")
        ttsService.stop()
        Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з flipCard().")

        _currentCardState.value = when (_currentCardState.value) {
            CardState.Prompt -> CardState.Answer
            CardState.Answer -> CardState.Prompt
        }
        Log.d("PracticeVM", "flipCard: CardState changed to ${_currentCardState.value}")
    }


    private fun startNextPracticeBatch() {
        Log.d("PracticeVM", "startNextPracticeBatch: Called.")
        val remainingWords = _allWordsForSession.value

        if (remainingWords.isEmpty()) {

            _currentBatch.value = emptyList()
            Log.d("PracticeVM", "startNextPracticeBatch: No remaining words in _allWordsForSession. Flow collector should handle phase.")
            ttsService.stop()
            Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з startNextPracticeBatch() (empty).")
            return
        }

        val currentBatchSize = min(remainingWords.size, batchSize)
        val nextBatch = remainingWords.take(currentBatchSize)

        _currentBatch.value = nextBatch
        _currentWordIndexInBatch.value = 0

        if (nextBatch.isNotEmpty()) {
            val currentPhase = _practicePhase.value
            if (currentPhase == PracticePhase.Loading || currentPhase is PracticePhase.BatchRegular) {
                _practicePhase.value = PracticePhase.BatchPairing(nextBatch)
                Log.d("PracticeVM", "startNextPracticeBatch: Starting BatchPairing with ${nextBatch.size} words.")
            }

            _currentCardState.value = CardState.Prompt
            setRandomPromptContentType()

            Log.d("PracticeVM", "startNextPracticeBatch: Initial CardState set to Prompt. PromptContentType: ${_currentWordPromptContentType.value}.")

        } else {
            Log.w("PracticeVM", "startNextPracticeBatch: Logic error, batch was empty despite words in _allWordsForSession. Flow collect should handle phase.")
            _currentBatch.value = emptyList()
        }

        ttsService.stop()
        Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з startNextPracticeBatch().")
    }


    fun onPairingFinished() {
        Log.d("PracticeVM", "onPairingFinished: Pairing finished for current batch. Moving to Regular phase.")
        pushStateForUndo(null)
        Log.d("UndoDebug", "Pushing phase state before onPairingFinished() transition to Regular.")
        if (_currentBatch.value.isNotEmpty()) {
            _practicePhase.value = PracticePhase.BatchRegular(_currentBatch.value)
            _currentWordIndexInBatch.value = 0

            _currentCardState.value = CardState.Prompt
            setRandomPromptContentType()

            ttsService.stop()
            Log.d("TTS_DEBUG", "Manual Stop: Озвучуємо зупинено з onPairingFinished().")
            Log.d("PracticeVM", "onPairingFinished: Transitioned to BatchRegular.")
        } else {
            Log.w("PracticeVM", "onPairingFinished: Current batch is empty, cannot transition to Regular. Checking for next batch.")

        }
    }

    fun onPairMatched(wordId: String) {
        Log.d("PracticeVM", "onPairMatched: Pair matched for word ID $wordId.")
        val matchedWord = _currentBatch.value.firstOrNull { it.id == wordId }
        if (matchedWord != null) {
            Log.d("PracticeVM", "Matched word found: ${matchedWord.text}. Processing answer.")
            val quality = 4
            processAnswerAndUpdateWord(matchedWord, quality)
        } else {
            Log.e("PracticeVM", "onPairMatched: Matched word with ID $wordId not found in current batch.")
        }
    }


    fun onCardSwipedLeft() {
        Log.d("PracticeVM", "onCardSwipedLeft: Card swiped LEFT.")
        val currentWord = _currentBatch.value.getOrNull(_currentWordIndexInBatch.value)
        if (currentWord != null) {
            pushStateForUndo(createWordMemento(currentWord))
            Log.d("UndoDebug", "Pushing state before onCardSwipedLeft() for word ${currentWord.text}.")
            val quality = 2
            processAnswerAndUpdateWord(currentWord, quality)
        } else {
            Log.w("PracticeVM", "onCardSwipedLeft: No current word to process.")
        }
    }

    fun onCardSwipedRight() {
        Log.d("PracticeVM", "onCardSwipedRight: Card swiped RIGHT.")
        val currentWord = _currentBatch.value.getOrNull(_currentWordIndexInBatch.value)
        if (currentWord != null) {
            pushStateForUndo(createWordMemento(currentWord))
            Log.d("UndoDebug", "Pushing state before onCardSwipedRight() for word ${currentWord.text}.")
            val quality = 5
            processAnswerAndUpdateWord(currentWord, quality)
        } else {
            Log.w("PracticeVM", "onCardSwipedRight: No current word to process.")
        }
    }

    private fun createWordMemento(word: Word): WordMemento {
        return WordMemento(
            wordId = word.id,
            repetition = word.repetition,
            easiness = word.easiness,
            interval = word.interval,
            lastReviewed = word.lastReviewed,
            nextReview = word.nextReview,
            status = word.status
        )
    }

    private fun restoreWordFromMemento(word: Word, memento: WordMemento): Word {
        return word.copy(
            repetition = memento.repetition,
            easiness = memento.easiness,
            interval = memento.interval,
            lastReviewed = memento.lastReviewed,
            nextReview = memento.nextReview,
            status = memento.status
        )
    }


    private fun processAnswerAndUpdateWord(word: Word, quality: Int) {
        Log.d("PracticeVM", "processAnswerAndUpdateWord: Processing answer for word ${word.text} with quality $quality.")
        val indexBeforeProcessing = _currentWordIndexInBatch.value
        val currentBatchSizeBeforeProcessing = _currentBatch.value.size
        val currentPhaseBeforeProcessing = _practicePhase.value

        Log.d("BatchDebug", "processAnswerAndUpdateWord: Called for word ${word.text} with quality $quality. Index before: $indexBeforeProcessing, Batch size before: $currentBatchSizeBeforeProcessing, Phase before: $currentPhaseBeforeProcessing")

        val (rep, ef, interval) = PracticeUtils.sm2(
            word.repetition, word.easiness, word.interval, quality
        )
        val now = System.currentTimeMillis()
        val next = now + interval
        val status = PracticeUtils.determineStatus(rep, interval)
        val updatedWord = word.copy(
            repetition = rep,
            easiness = ef,
            interval = interval,
            lastReviewed = now,
            nextReview = next,
            status = status
        )


        viewModelScope.launch {
            practiceRepository.saveWord(updatedWord) {
                Log.d("PracticeVM", "Word ${updatedWord.text} updated successfully in Firebase with quality $quality via saveWord.")

                if (currentPhaseBeforeProcessing is PracticePhase.BatchRegular) {
                    val nextIndexInBatch = indexBeforeProcessing + 1

                    if (nextIndexInBatch >= currentBatchSizeBeforeProcessing) {
                        Log.d("PracticeVM", "processAnswerAndUpdateWord (Callback): Finished current batch in Regular phase. Index $nextIndexInBatch vs Batch size $currentBatchSizeBeforeProcessing. Starting next batch via Flow update.")
                        _currentBatch.value = emptyList()
                        _currentWordIndexInBatch.value = 0

                        ttsService.stop()
                        Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з processAnswerAndUpdateWord() (End of Regular Batch).")

                    } else {
                        Log.d("PracticeVM", "processAnswerAndUpdateWord (Callback): Moving to next word in batch in Regular phase. Index: $nextIndexInBatch.")
                        _currentWordIndexInBatch.value = nextIndexInBatch
                        _currentCardState.value = CardState.Prompt
                        setRandomPromptContentType()

                        ttsService.stop()
                        Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з processAnswerAndUpdateWord() (Next Regular Word).")
                    }
                    totalPracticedCount++
                } else if (currentPhaseBeforeProcessing is PracticePhase.BatchPairing) {
                    Log.d("PracticeVM", "processAnswerAndUpdateWord (Callback): Processed word in Pairing phase. Not moving to next word here.")
                    totalPracticedCount++
                } else {
                    Log.d("PracticeVM", "processAnswerAndUpdateWord (Callback): Processed word in unexpected phase: $currentPhaseBeforeProcessing.")
                }
            }
        }
    }

    private fun pushStateForUndo(wordMemento: WordMemento?) {
        val stateToPush = UndoState(
            phase = _practicePhase.value,
            batch = _currentBatch.value,
            wordIndexInBatch = _currentWordIndexInBatch.value,
            cardState = _currentCardState.value,
            wordBeforeAction = wordMemento
        )
        undoStack.push(stateToPush)
        _canUndo.value = undoStack.isNotEmpty()
        Log.d("UndoDebug", "State pushed to stack. Stack size: ${undoStack.size()}")
        Log.d("UndoDebug", "Pushed State: Phase=${stateToPush.phase}, BatchSize=${stateToPush.batch.size}, Index=${stateToPush.wordIndexInBatch}, CardState=${stateToPush.cardState}, WordMemento=${stateToPush.wordBeforeAction?.wordId}")
    }


    fun undoLastAction() {
        val lastState = undoStack.pop()
        if (lastState != null) {
            Log.d("UndoDebug", "Popped state from stack. Restoring...")
            Log.d("UndoDebug", "Restoring State: Phase=${lastState.phase}, BatchSize=${lastState.batch.size}, Index=${lastState.wordIndexInBatch}, CardState=${lastState.cardState}, WordMemento=${lastState.wordBeforeAction?.wordId}")

            _practicePhase.value = lastState.phase
            _currentBatch.value = lastState.batch
            _currentWordIndexInBatch.value = lastState.wordIndexInBatch

            if (lastState.phase is PracticePhase.BatchRegular) {
                _currentCardState.value = CardState.Answer
                setRandomPromptContentType()
            } else {
                _currentCardState.value = lastState.cardState
                if (lastState.cardState == CardState.Prompt) {
                    setRandomPromptContentType()
                }
            }

            lastState.wordBeforeAction?.let { memento ->
                viewModelScope.launch {
                    val wordToRestore = _allWordsForSession.value.find { it.id == memento.wordId }
                        ?: _currentBatch.value.find { it.id == memento.wordId }
                        ?: practiceRepository.getWordById(memento.wordId)

                    if (wordToRestore != null) {
                        val restoredWord = restoreWordFromMemento(wordToRestore, memento)
                        Log.d("UndoDebug", "Attempting to restore word ${restoredWord.text} with state: Rep=${restoredWord.repetition}, EF=${restoredWord.easiness}, Interval=${restoredWord.interval}, Status=${restoredWord.status}")

                        practiceRepository.saveWord(restoredWord) {
                            Log.d("UndoDebug", "Word ${restoredWord.text} state restored in Firebase successfully.")
                        }
                    } else {
                        Log.e("UndoDebug", "Word with ID ${memento.wordId} not found in current session words, batch, or repository for restoring.")
                        _errorMessage.value = "Не вдалося скасувати дію: слово для відновлення не знайдено."
                    }
                }
            }


            _canUndo.value = undoStack.isNotEmpty()
            ttsService.stop()
            Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з undoLastAction().")


        } else {
            Log.d("UndoDebug", "Undo stack is empty. No action to undo.")
            _errorMessage.value = "Немає дій для скасування."
        }
    }


    fun speakTranslationText(translationText: String) {
        Log.d("TTS_DEBUG", "speakTranslationText: ВИКЛИК З ТЕКСТОМ: '$translationText'")
        ttsService.stop()
        Log.d("TTS_DEBUG", "speakTranslationText: Попереднє озвучення зупинено.")

        if (translationText.isNotBlank()) {
            ttsService.speak(translationText)
            Log.d("TTS_DEBUG", "speakTranslationText: ПОЧИНАЄМО ОЗВУЧЕННЯ: '$translationText'")
        } else {
            Log.d("TTS_DEBUG", "speakTranslationText: Текст для озвучення порожній.")
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
        Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з onCleared(). TTS shutdown.")
        Log.d("PracticeVM", "ViewModel onCleared, TTS shutdown.")
    }
}

sealed class PracticePhase {
    object Loading : PracticePhase()
    data class BatchPairing(val wordsInBatch: List<Word>) : PracticePhase()
    data class BatchRegular(val wordsInBatch: List<Word>) : PracticePhase()
    data class Finished(val totalPracticedCount: Int) : PracticePhase()
    data class Error(val message: String) : PracticePhase()
    object Empty : PracticePhase()
}
