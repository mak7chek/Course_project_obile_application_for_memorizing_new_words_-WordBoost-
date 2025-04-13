package com.example.wordboost.translation

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface TranslatorService {
    @Headers(
        "Content-Type: application/json",
        // Замініть YOUR_API_KEY на ваш реальний API ключ DeepL.
        "Authorization: DeepL-Auth-Key YOUR_API_KEY"
    )
    @POST("v2/translate")
    fun translate(
        @Body request: TranslationRequest
    ): Call<TranslationResponse>
}