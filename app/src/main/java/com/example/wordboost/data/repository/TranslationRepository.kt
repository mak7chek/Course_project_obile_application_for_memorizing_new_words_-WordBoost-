
package com.example.wordboost.data.repository

import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.local.CacheDao
import com.example.wordboost.data.local.CacheEntity
import com.example.wordboost.translation.Translator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log // Для логування

class TranslationRepository(
    private val firebaseRepo: FirebaseRepository,
    private val cacheDao: CacheDao,
    private val realTranslator: Translator
) {

    fun translate(text: String, targetLang: String, onResult: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Перевірка у Firebase (словник)
            firebaseRepo.getTranslation(text) { firebaseResult ->
                if (firebaseResult != null) {
                    Log.d("TranslationRepo", "Знайдено у Firebase: $text -> $firebaseResult")
                    onResult(firebaseResult)
                } else {
                    // 2. Перевірка у Room (кеш)
                    CoroutineScope(Dispatchers.IO).launch {
                        val cachedEntry: CacheEntity? = cacheDao.getCacheEntry(text)

                        val cachedResult: String? = if (cachedEntry != null) {
                            // !!! Оновлюємо timestamp запису, який ми щойно використали !!!
                            val currentTime = System.currentTimeMillis()
                            cacheDao.updateTimestamp(cachedEntry.original, currentTime)
                            Log.d("TranslationRepo", "Знайдено у кеші: ${cachedEntry.original}/${cachedEntry.translated}. Timestamp оновлено.")

                            // Визначаємо, який з полів є перекладом
                            if (cachedEntry.original == text) {
                                cachedEntry.translated
                            } else { // cachedEntry.translated == text
                                cachedEntry.original
                            }
                        } else {
                            null
                        }


                        if (cachedResult != null) {
                            withContext(Dispatchers.Main) {
                                onResult(cachedResult)
                            }
                        } else {
                            // 3. DeepL (реальний перекладач)
                            Log.d("TranslationRepo", "Не знайдено у кеші/Firebase. Використовуємо DeepL для $text")
                            realTranslator.translate(text, targetLang) { deeplResult ->
                                if (deeplResult != null) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        // Вставка в кеш. timestamp встановиться автоматично на поточний час.
                                        cacheDao.insertTranslation(text, deeplResult)
                                        Log.d("TranslationRepo", "Вставлено в кеш: $text -> $deeplResult")
                                    }
                                } else {
                                    Log.w("TranslationRepo", "DeepL не зміг перекласти $text")
                                }
                                onResult(deeplResult)
                            }
                        }
                    }
                }
            }
        }
    }
}
