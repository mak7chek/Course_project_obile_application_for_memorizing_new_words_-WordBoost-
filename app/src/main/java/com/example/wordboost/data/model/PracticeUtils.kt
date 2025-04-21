package com.example.wordboost.data.model


object PracticeUtils {
    fun calculateNextReviewTime(knowledgeLevel: Int): Long {
        val days = when {
            knowledgeLevel < 20 -> 1
            knowledgeLevel < 50 -> 3
            knowledgeLevel < 80 -> 7
            else                 -> 14
        }
        return System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
    }
}