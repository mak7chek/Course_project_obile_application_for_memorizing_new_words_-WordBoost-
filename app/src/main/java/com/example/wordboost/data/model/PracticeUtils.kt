package com.example.wordboost.data.model

fun calculateNextReviewTime (knowledgeLevel: Int ): Long {
    val days = when {
        knowledgeLevel < 20 -> 1
        knowledgeLevel < 50 -> 4
        knowledgeLevel < 80 -> 14
        else -> 30
    }
    return System.currentTimeMillis() + days * 24 * 60 * 60 * 1000
}