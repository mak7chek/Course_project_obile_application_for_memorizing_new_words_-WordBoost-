package com.example.wordboost.data.model

import com.google.firebase.firestore.DocumentId

data class Word(
    @DocumentId
    var id: String = "",
    val text: String = "",
    val translation: String = "",
    val dictionaryId: String? = null,
    val repetition: Int = 0,
    val easiness: Float = 2.5f,
    val interval: Long = 0L,
    val lastReviewed: Long = 0L,
    val nextReview: Long = 0L,
    val status: String = "new"
)