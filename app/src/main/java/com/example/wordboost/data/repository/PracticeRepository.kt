package com.example.wordboost.data.repository

import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.model.PracticeUtils
// !!! Import Flow !!!
import kotlinx.coroutines.flow.Flow

class PracticeRepository(private val firebase: FirebaseRepository) {

    // !!! REMOVED: Old getWordsForPractice !!!
    // fun getWordsForPractice(callback: (List<Word>) -> Unit) { ... }


    // !!! NEW: Get words needing practice as a Flow from FirebaseRepository !!!
    fun getWordsNeedingPracticeFlow(): Flow<List<Word>> {
        return firebase.getWordsNeedingPracticeFlow()
    }


    fun updateWordAfterPractice(
        word: Word,
        quality: Int,
        callback: () -> Unit
    ) {
        val (rep, ef, interval) = PracticeUtils.sm2(
            word.repetition, word.easiness, word.interval, quality
        )
        val now = System.currentTimeMillis()
        val next = now + interval
        // The status logic in PracticeUtils needs to correctly transition
        // words out of the "needs practice" state when quality is high enough
        // (e.g., to "mastered" or just a far future nextReview).
        // Assuming determineStatus handles this.
        val status = PracticeUtils.determineStatus(rep, interval)
        val updated = word.copy(
            repetition = rep,
            easiness = ef,
            interval = interval,
            lastReviewed = now,
            nextReview = next,
            status = status
        )
        firebase.saveWord(updated) { callback() }
    }

    fun resetWordProgress(word: Word, callback: () -> Unit) {
        val reset = word.copy(
            repetition = 0,
            easiness = 2.5f,
            interval = 0L,
            lastReviewed = 0L,
            nextReview = 0L,
            status = "new" // Set status to "new" or similar initial state
        )
        firebase.saveWord(reset) { callback() }
    }
}