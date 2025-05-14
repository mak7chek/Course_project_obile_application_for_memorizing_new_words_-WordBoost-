package com.example.wordboost.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SharedCardSetSummary(
    @DocumentId val id: String = "",
    val name_uk: String = "",
    val authorName: String? = null,
    val wordCount: Int = 0,
    val difficultyLevelKey: String? = null,
    val public: Boolean = false,
    @ServerTimestamp var createdAt: Date? = null
)