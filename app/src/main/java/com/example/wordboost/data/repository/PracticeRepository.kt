package com.example.wordboost.data.repository

import android.util.Log
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.util.PracticeUtils
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
        callback: (Boolean) -> Unit
    ) {
        val (rep, ef, interval) = PracticeUtils.sm2(
            word.repetition, word.easiness, word.interval, quality
        )
        val now = System.currentTimeMillis()
        val next = now + interval
        val status = PracticeUtils.determineStatus(rep, interval)
        val updated = word.copy(
            repetition = rep,
            easiness = ef,
            interval = interval,
            lastReviewed = now,
            nextReview = next,
            status = status
        )
        saveWord(updated) { success ->
            if (success) {
                Log.d("PracticeRepo", "Word ${updated.id} updated successfully after practice.")
                callback(true)
            } else {
                Log.e("PracticeRepo", "Failed to save updated word ${updated.id} after practice.")
                callback(false)
            }
        }
    }

    fun saveWord(word: Word, callback: (Boolean) -> Unit) {
        firebase.saveWord(word) { success ->
            if (success) {
                Log.d("PracticeRepo", "Word ${word.id} saved successfully via Firebase.")
                callback(true)
            } else {
                Log.e("PracticeRepo", "Error saving word ${word.id} via Firebase.")
                callback(false)
            }
        }
    }



    fun resetWordProgress(word: Word, callback: (Boolean) -> Unit) {
        val reset = word.copy(
            repetition = 0,
            easiness = 2.5f,
            interval = 0L,
            lastReviewed = 0L,
            nextReview = 0L,
            status = "new"
        )
        saveWord(reset) { success ->
            if (success) {
                Log.d("PracticeRepo", "Word ${reset.id} progress reset successfully.")
                callback(true)
            } else {
                Log.e("PracticeRepo", "Error resetting progress for word ${reset.id}.")
                callback(false)
            }
        }
    }
}