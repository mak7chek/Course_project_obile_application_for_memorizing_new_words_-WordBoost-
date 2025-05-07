// Corrected PracticeViewModel.kt
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

sealed class PracticePhase {
    object Loading : PracticePhase()
    data class BatchPairing(val wordsInBatch: List<Word>) : PracticePhase()
    data class BatchRegular(val wordsInBatch: List<Word>) : PracticePhase()
    data class Finished(val totalPracticedCount: Int) : PracticePhase()
    data class Error(val message: String) : PracticePhase()
    object Empty : PracticePhase()
}

enum class PromptContentType {
    Original,
    Translation
}

enum class CardState {
    Prompt,
    Answer
}

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
                            Log.d("PracticeVM", "Starting next batch after Flow update.")
                            startNextPracticeBatch()
                        } else if (words.isEmpty() && (currentPhase == PracticePhase.Loading || currentPhase is PracticePhase.BatchPairing || currentPhase is PracticePhase.BatchRegular)) {
                            if (_currentBatch.value.isEmpty()) {
                                Log.d("PracticeVM", "No words in Flow, and current batch empty. Transitioning to Empty phase.")
                                _practicePhase.value = PracticePhase.Empty
                            } else {
                                Log.d("PracticeVM", "No words in Flow, but current batch still has items (${_currentBatch.value.size}). Staying in current phase.")
                            }
                        } else {
                            Log.d("PracticeVM", "Flow update received, but not triggering batch start. Current phase: $currentPhase, words in flow: ${words.size}, words in current batch: ${_currentBatch.value.size}")
                            if (currentPhase == PracticePhase.Loading && words.isEmpty()) {
                                Log.d("PracticeVM", "Loading finished, no words in Flow. Transitioning to Empty phase.")
                                _practicePhase.value = PracticePhase.Empty
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("PracticeVM", "Error collecting words for practice: ${e.message}", e)
                _practicePhase.value = PracticePhase.Error("Помилка завантаження слів: ${e.message}")
            }
        }
    }


    fun flipCard() {
        Log.d("PracticeVM", "flipCard: Called. Current CardState: ${_currentCardState.value}")
        ttsService.stop() // Stop TTS when flipping
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
            _practicePhase.value = PracticePhase.Empty
            Log.d("PracticeVM", "startNextPracticeBatch: No remaining words in _allWordsForSession. Transitioning to Empty.")
            ttsService.stop() // Stop TTS when batch starts and list is empty
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
            } // else if currentPhase is BatchPairing, transition happens in onPairingFinished


            _currentCardState.value = CardState.Prompt
            setRandomPromptContentType()

            Log.d("PracticeVM", "startNextPracticeBatch: Initial CardState set to Prompt. PromptContentType: ${_currentWordPromptContentType.value}.")

        } else {
            _currentBatch.value = emptyList()
            _practicePhase.value = PracticePhase.Empty
            Log.w("PracticeVM", "startNextPracticeBatch: Logic error, batch was empty despite words in _allWordsForSession. Transitioning to Empty.")
        }

        ttsService.stop() // Stop TTS when batch starts (even if not empty)
        Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з startNextPracticeBatch().")
    }


    fun onPairingFinished() {
        Log.d("PracticeVM", "onPairingFinished: Pairing finished for current batch. Moving to Regular phase.")
        if (_currentBatch.value.isNotEmpty()) {
            _practicePhase.value = PracticePhase.BatchRegular(_currentBatch.value)
            _currentWordIndexInBatch.value = 0

            _currentCardState.value = CardState.Prompt
            setRandomPromptContentType()

            ttsService.stop() // Stop TTS when transitioning to Regular phase
            Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з onPairingFinished().")
            Log.d("PracticeVM", "onPairingFinished: Transitioned to BatchRegular.")
        } else {
            Log.w("PracticeVM", "onPairingFinished: Current batch is empty, cannot transition to Regular. Checking for next batch.")
            startNextPracticeBatch()
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
        // !!! Do NOT call speakTranslationText or stop() here automatically on match !!!
        // The UI (PairingGameUI) might call speakTranslationText when a word is clicked for preview.
    }


    fun onCardSwipedLeft() {
        Log.d("PracticeVM", "onCardSwipedLeft: Card swiped LEFT.")
        val currentWord = _currentBatch.value.getOrNull(_currentWordIndexInBatch.value) ?: return
        val quality = 2
        processAnswerAndUpdateWord(currentWord, quality)
    }

    fun onCardSwipedRight() {
        Log.d("PracticeVM", "onCardSwipedRight: Card swiped RIGHT.")
        val currentWord = _currentBatch.value.getOrNull(_currentWordIndexInBatch.value) ?: return
        val quality = 5
        processAnswerAndUpdateWord(currentWord, quality)
    }

    private fun processAnswerAndUpdateWord(word: Word, quality: Int) {
        Log.d("PracticeVM", "processAnswerAndUpdateWord: Processing answer for word ${word.text} with quality $quality.")
        val indexBeforeProcessing = _currentWordIndexInBatch.value
        val currentBatchSizeBeforeProcessing = _currentBatch.value.size
        val currentPhaseBeforeProcessing = _practicePhase.value

        Log.d("BatchDebug", "processAnswerAndUpdateWord: Called for word ${word.text} with quality $quality. Index before: $indexBeforeProcessing, Batch size before: $currentBatchSizeBeforeProcessing, Phase before: $currentPhaseBeforeProcessing")


        viewModelScope.launch {
            practiceRepository.updateWordAfterPractice(word, quality) { // Callback after Firebase save
                Log.d("PracticeVM", "Word ${word.text} updated successfully in Firebase with quality $quality.")


                if (currentPhaseBeforeProcessing is PracticePhase.BatchRegular) {
                    val nextIndexInBatch = indexBeforeProcessing + 1

                    if (nextIndexInBatch >= currentBatchSizeBeforeProcessing) {
                        Log.d("PracticeVM", "processAnswerAndUpdateWord (Callback): Finished current batch in Regular phase. Index $nextIndexInBatch vs Batch size $currentBatchSizeBeforeProcessing. Starting next batch.")
                        _currentBatch.value = emptyList()
                        ttsService.stop()
                        Log.d("TTS_DEBUG", "Manual Stop: Озвучення зупинено з processAnswerAndUpdateWord() (End of Regular Batch).")
                    } else {
                        _currentWordIndexInBatch.value = nextIndexInBatch
                        _currentCardState.value = CardState.Prompt
                        setRandomPromptContentType()

                        Log.d("PracticeVM", "processAnswerAndUpdateWord (Callback): Moving to next word in batch in Regular phase. Index: $nextIndexInBatch. CardState set to: ${_currentCardState.value}. PromptContentType: ${_currentWordPromptContentType.value}.")
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

    fun speakTranslationText(translationText: String) {
        Log.d("TTS_DEBUG", "speakTranslationText: ВИКЛИК З ТЕКСТОМ: '$translationText'")
        ttsService.stop() // Always stop previous speech before starting new
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