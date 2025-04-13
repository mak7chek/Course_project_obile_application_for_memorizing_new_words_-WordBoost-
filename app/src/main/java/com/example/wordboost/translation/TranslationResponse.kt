package com.example.wordboost.translation

data class TranslationResponse(
    val translations: List<TranslatedText>
)

data class TranslatedText(
    val text: String
)