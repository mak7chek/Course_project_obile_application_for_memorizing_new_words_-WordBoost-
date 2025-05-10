package com.example.wordboost.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserSharedSetProgress(
    val sharedSetId: String = "",
    var currentWordIndex: Int = 0,
    var isCompleted: Boolean = false,
    @ServerTimestamp
    var lastAccessed: Date? = null,
    val ignoredWordsInSet: List<String> = emptyList()
)