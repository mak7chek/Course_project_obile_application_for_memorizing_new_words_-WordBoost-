package com.example.wordboost.translation

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TranslatorService {

    @POST("v2/translate")
    fun translate(
        @Body request: TranslationRequest
    ): Call<TranslationResponse>
}