package com.example.wordboost.data.repository

import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.model.PracticeUtils

class PracticeRepository(private val firebase: FirebaseRepository) {
    fun getWordsForPractice(callback: (List<Word>) -> Unit) {
        firebase.getUserWordsForPractice(callback)
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
        val status = PracticeUtils.determineStatus(rep)
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
            status = "new"
        )
        firebase.saveWord(reset) { callback() }
    }
}