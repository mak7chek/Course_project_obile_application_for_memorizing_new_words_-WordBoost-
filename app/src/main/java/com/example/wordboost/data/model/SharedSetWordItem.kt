package com.example.wordboost.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

data class SharedSetWordItem(
    @DocumentId
    var id: String = UUID.randomUUID().toString(),
    var originalText: String = "",
    var translationText: String = "",
    var exampleSentence_en: String? = null,
    var exampleSentence_uk: String? = null,
    var authorId: String = "",
    @ServerTimestamp
    var addedAt: Date? = null
)