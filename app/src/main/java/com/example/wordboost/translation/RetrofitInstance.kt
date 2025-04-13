package com.example.wordboost.translation

import com.example.wordboost.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://api-free.deepl.com/"

    // Створюємо клієнт з авторизаційним заголовком
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "DeepL-Auth-Key ${BuildConfig.DEEPL_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    // Створюємо Retrofit з цим клієнтом
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // додаємо клієнт
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Створюємо API
    val api: TranslatorService by lazy {
        retrofit.create(TranslatorService::class.java)
    }
}