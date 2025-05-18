package com.example.wordboost.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserArticleInteraction(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val articleId: String = "",
    var isRead: Boolean = false,
    @ServerTimestamp
    var lastReadTimestamp: Date? = null
) {
    constructor() : this("", "", "", false, null)
}