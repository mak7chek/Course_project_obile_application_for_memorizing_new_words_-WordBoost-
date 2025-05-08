package com.example.wordboost.data.repository

import android.util.Log
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.model.PracticeUtils
// !!! Import Flow !!!
import kotlinx.coroutines.flow.Flow

class PracticeRepository(private val firebase: FirebaseRepository) {

    fun getWordsNeedingPracticeFlow(): Flow<List<Word>> {
        return firebase.getWordsNeedingPracticeFlow()
    }

    suspend fun getWordById(wordId: String): Word? {
        return firebase.getWordById_Suspend(wordId)
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
        val status = PracticeUtils.determineStatus(rep, interval) // Визначаємо статус
        val updated = word.copy(
            repetition = rep,
            easiness = ef,
            interval = interval,
            lastReviewed = now,
            nextReview = next,
            status = status
        )
        firebase.saveWord(updated) { success ->
            if (success) {
                callback()
            } else {

                Log.e("PracticeRepo", "Failed to save updated word ${updated.id}")
                callback()
            }
        }
    }

    fun saveWord(word: Word, callback: () -> Unit) {
        firebase.saveWord(word) { success ->
            if (success) {
                callback()
            } else {
                Log.e("PracticeRepo", "Failed to save word ${word.id} during undo.")
                callback()
            }
        }
    }


    fun resetWordProgress(word: Word, callback: () -> Unit) {
        val reset = word.copy(
            repetition = 0,
            easiness = 2.5f,
            interval = 0L,
            lastReviewed = 0L,
            nextReview = 0L,
            status = "new"
        )

        firebase.saveWord(reset) { success ->
            if (success) {
                callback()
            } else {
                Log.e("PracticeRepo", "Failed to save reset word ${reset.id}")
                callback()
            }
        }
    }
}