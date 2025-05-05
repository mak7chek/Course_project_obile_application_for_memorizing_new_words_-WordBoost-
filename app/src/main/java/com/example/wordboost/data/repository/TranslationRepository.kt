
package com.example.wordboost.data.repository

import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.local.CacheDao
import com.example.wordboost.data.local.CacheEntity
import com.example.wordboost.translation.Translator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class TranslationRepository(
    private val firebaseRepo: FirebaseRepository,
    private val cacheDao: CacheDao,
    private val realTranslator: Translator
) {
    fun translate(text: String, targetLang: String, onResult: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            firebaseRepo.getTranslation(text) { firebaseResult ->
                if (firebaseResult != null) {
                    Log.d("TranslationRepo", "Знайдено у Firebase: $text -> $firebaseResult")
                    onResult(firebaseResult)
                } else {

                    CoroutineScope(Dispatchers.IO).launch {
                        val cachedEntry: CacheEntity? = cacheDao.getCacheEntry(text)

                        val cachedResult: String? = if (cachedEntry != null) {
                            val currentTime = System.currentTimeMillis()
                            cacheDao.updateTimestamp(cachedEntry.original, currentTime)
                            Log.d("TranslationRepo", "Знайдено у кеші: ${cachedEntry.original}/${cachedEntry.translated}. Timestamp оновлено.")

                            if (cachedEntry.original == text) {
                                cachedEntry.translated
                            } else {
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
                            Log.d("TranslationRepo", "Не знайдено у кеші/Firebase. Використовуємо DeepL для $text")
                            realTranslator.translate(text, targetLang) { deeplResult ->
                                if (deeplResult != null) {
                                    CoroutineScope(Dispatchers.IO).launch {
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
