package com.example.wordboost.translation

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

interface Translator {
    fun translate(text: String, targetLang: String, callback: (String?) -> Unit)
}

class RealTranslator : Translator {
    override fun translate(text: String, targetLang: String, callback: (String?) -> Unit) {
        val request = TranslationRequest(listOf(text), targetLang)
        val call = RetrofitInstance.api.translate(request)

        call.enqueue(object : Callback<TranslationResponse> {
            override fun onResponse(
                call: Call<TranslationResponse>,
                response: Response<TranslationResponse>
            ) {
                if (response.isSuccessful) {
                    val translated = response.body()?.translations?.firstOrNull()?.text
                    callback(translated)
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<TranslationResponse>, t: Throwable) {
                callback(null)
            }
        })
    }
}