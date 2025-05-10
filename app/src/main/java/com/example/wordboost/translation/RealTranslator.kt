package com.example.wordboost.translation

import android.util.Log // Потрібен імпорт
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

interface Translator {
    fun translate(text: String, targetLang: String, callback: (String?) -> Unit)
}

class RealTranslator : Translator {
    override fun translate(text: String, targetLang: String, callback: (String?) -> Unit) {
        val request = TranslationRequest(listOf(text), targetLang)
        Log.d("RealTranslator", "Requesting DeepL: Text='$text', TargetLang='$targetLang'") // Лог запиту
        val call = RetrofitInstance.api.translate(request)

        call.enqueue(object : Callback<TranslationResponse> {
            override fun onResponse(
                call: Call<TranslationResponse>,
                response: Response<TranslationResponse>
            ) {
                if (response.isSuccessful) {
                    val translated = response.body()?.translations?.firstOrNull()?.text
                    Log.d("RealTranslator", "DeepL Response SUCCESS: Code=${response.code()}, Translated='$translated'") // Лог успіху
                    callback(translated)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("RealTranslator", "DeepL Response ERROR: Code=${response.code()}, Message='${response.message()}', ErrorBody='$errorBody'") // Лог помилки відповіді
                    callback(null) // Повертаємо null при помилці API
                }
            }

            override fun onFailure(call: Call<TranslationResponse>, t: Throwable) {
                Log.e("RealTranslator", "DeepL Request FAILURE: Message='${t.message}'", t) // Лог помилки запиту (наприклад, мережева)
                callback(null)
            }
        })
    }
}