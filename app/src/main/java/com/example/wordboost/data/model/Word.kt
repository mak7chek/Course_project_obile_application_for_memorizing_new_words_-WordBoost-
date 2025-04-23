package com.example.wordboost.data.model

data class Word(
    val userId: String = "",
    val id: String = "",
    val text: String = "",
    val translation: String = "",
    val dictionaryId: String = "",
    // SM‑2 fields:
    val repetition: Int = 0,        // кількість послідовних успіхів
    val easiness: Float = 2.5f,     // коефіцієнт легкості (EF)
    val interval: Long = 0L,        // останній інтервал (мс)
    val lastReviewed: Long = 0L,
    val nextReview: Long = 0L,
    val status: String = "new"
)