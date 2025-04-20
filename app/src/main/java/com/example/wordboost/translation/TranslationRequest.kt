package com.example.wordboost.translation

data class TranslationRequest(
    val text: List<String>,
    val target_lang: String
)

