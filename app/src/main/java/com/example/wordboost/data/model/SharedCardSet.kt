package com.example.wordboost.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SharedCardSet(
    @DocumentId
    var id: String = "",
    var name_uk: String = "",
    var name_en: String? = null,
    var description: String? = null,
    var authorId: String = "",
    var authorName: String? = null,
    var difficultyLevel: String = DifficultyLevel.MEDIUM.key,
    var public: Boolean = true,
    val languageOriginal: String = "en",
    val languageTranslation: String = "uk",
    var wordCount: Int = 0,
    @ServerTimestamp
    var createdAt: Date? = null,
    @ServerTimestamp
    var updatedAt: Date? = null
)