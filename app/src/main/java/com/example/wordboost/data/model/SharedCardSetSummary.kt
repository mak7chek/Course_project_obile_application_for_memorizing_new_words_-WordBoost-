package com.example.wordboost.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SharedCardSetSummary(
    @DocumentId val id: String = "",
    val name_uk: String = "",
    // val name_en: String? = null, // Можна додати, якщо потрібно показувати в списку
    val authorName: String? = null, // Або authorId, якщо ім'я будеш завантажувати окремо
    val wordCount: Int = 0,
    val difficultyLevelKey: String? = null, // Ключ з DifficultyLevel enum
    val isPublic: Boolean = false,
    @ServerTimestamp var createdAt: Date? = null // Для сортування "новіші спочатку"
)