package com.example.wordboost.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Article(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val authorName: String? = null,
    val title: String = "",
    val content: String = "",
    val languageCode: String = "en",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val isPublished: Boolean = false
) {
    constructor() : this("", "", null, "", "", "en", null, null, false)
}