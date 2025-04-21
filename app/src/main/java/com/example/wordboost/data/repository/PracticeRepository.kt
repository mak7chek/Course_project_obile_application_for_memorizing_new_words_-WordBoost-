package com.example.wordboost.data.repository

import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.model.PracticeUtils

class PracticeRepository(private val firebaseRepo: FirebaseRepository) {

    fun getWordsForPractice(callback: (List<Word>) -> Unit) {
        firebaseRepo.getUserWordsForPractice { words ->
            callback(words)
        }
    }

    fun updateWordAfterPractice(word: Word, success: Boolean, callback: () -> Unit) {
        val newKnowledgeLevel = when {
            success && word.knowledgeLevel < 5 -> word.knowledgeLevel + 1
            !success && word.knowledgeLevel > 0 -> word.knowledgeLevel - 1
            else -> word.knowledgeLevel
        }

        val nextReview = PracticeUtils.calculateNextReviewTime(newKnowledgeLevel)
        val updatedWord = word.copy(
            knowledgeLevel = newKnowledgeLevel,
            lastReviewed = System.currentTimeMillis(),
            nextReview = nextReview,
            status = if (newKnowledgeLevel >= 5) "learned" else "learning"
        )

        firebaseRepo.saveWord(updatedWord) {
            callback()
        }
    }

    fun resetWordProgress(word: Word, callback: () -> Unit) {
        val resetWord = word.copy(
            knowledgeLevel = 0,
            lastReviewed = 0L,
            nextReview = 0L,
            status = "new"
        )
        firebaseRepo.saveWord(resetWord) {
            callback()
        }
    }
}
