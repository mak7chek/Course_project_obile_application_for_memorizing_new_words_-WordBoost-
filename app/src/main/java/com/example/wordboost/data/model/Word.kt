package com.example.wordboost.data.model

data class Word(
    val id: String = "",
    val text: String = "",
    val translation: String = "",
    val dictionaryId: String = "",
    val knowledgeLevel: Int = 0,
    val status: String = "new",
    val lastReviewed: Long = 0L,
    val nextReview: Long = 0L
)