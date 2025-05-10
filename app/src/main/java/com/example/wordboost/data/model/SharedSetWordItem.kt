package com.example.wordboost.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

data class SharedSetWordItem(
    @DocumentId
    var id: String = UUID.randomUUID().toString(),// ID слова в межах підколекції набору (генерується Firestore)
    var originalText: String = "",    // Англійське слово
    var translationText: String = "", // Український переклад
    var exampleSentence_en: String? = null, // Опціонально
    var exampleSentence_uk: String? = null, // Опціонально
    var authorId: String = "",
    @ServerTimestamp
    var addedAt: Date? = null // Опціонально, час додавання слова до набору
)