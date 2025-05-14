
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import kotlin.coroutines.resume
class TranslationRepository(
    private val firebaseRepo: FirebaseRepository,
    private val cacheDao: CacheDao,
    private val realTranslator: Translator
) {
    suspend fun translateForUserVocabularySuspend(originalText: String, targetLang: String): String? {
        val textToTranslate = originalText.trim()
        if (textToTranslate.isBlank()) return null
        val firebaseResult = firebaseRepo.getTranslationSuspend(textToTranslate)
        if (firebaseResult != null) {
            Log.d("TranslationRepo", "[UserVocabSuspend] Знайдено у Firebase: '$textToTranslate' -> '$firebaseResult'")

            return firebaseResult
        }
        Log.d("TranslationRepo", "[UserVocabSuspend] Не знайдено у Firebase для: '$textToTranslate'")

        val cachedResult = withContext(Dispatchers.IO) {
            val cachedEntry: CacheEntity? = cacheDao.getCacheEntry(textToTranslate)
            if (cachedEntry != null) {
                cacheDao.updateTimestamp(cachedEntry.original, System.currentTimeMillis())
                Log.d("TranslationRepo", "[UserVocabSuspend] Знайдено у кеші: '${cachedEntry.original}' / '${cachedEntry.translated}' для '$textToTranslate'")
                // Визначаємо, що є перекладом для textToTranslate
                if (cachedEntry.original.equals(textToTranslate, ignoreCase = true)) {
                    cachedEntry.translated
                } else if (cachedEntry.translated.equals(textToTranslate, ignoreCase = true)) {
                    cachedEntry.original
                } else { null }
            } else { null }
        }
        if (cachedResult != null) {
            return cachedResult.trim()
        }
        Log.d("TranslationRepo", "[UserVocabSuspend] Не знайдено у кеші для: '$textToTranslate'")

        // 3. Якщо не знайдено ніде, використовуємо DeepL
        return suspendCancellableCoroutine { continuation ->
            realTranslator.translate(textToTranslate, targetLang) { deeplResult ->
                val finalResult = deeplResult?.trim()
                if (finalResult != null) {
                    if (!textToTranslate.equals(finalResult, ignoreCase = true)) {
                        CoroutineScope(Dispatchers.IO).launch {
                            cacheDao.insertTranslation(textToTranslate, finalResult)
                            Log.d("TranslationRepo", "[UserVocabSuspend] Результат DeepL '$finalResult' вставлено в кеш для '$textToTranslate'")
                        }
                    } else {
                        Log.w("TranslationRepo", "[UserVocabSuspend] Оригінал '$textToTranslate' і переклад '$finalResult' однакові. Не кешуємо.")
                    }
                } else {
                    Log.w("TranslationRepo", "[UserVocabSuspend] DeepL не зміг перекласти '$textToTranslate'")
                }
                if (continuation.isActive) {
                    continuation.resume(finalResult)
                }
            }
        }
    }



    suspend fun translateForSetCreationSuspend(text: String, targetLang: String): String? {
        val cachedResult = withContext(Dispatchers.IO) {
            val cachedEntry: CacheEntity? = cacheDao.getCacheEntry(text)
            if (cachedEntry != null) {
                cacheDao.updateTimestamp(cachedEntry.original, System.currentTimeMillis())

                if (cachedEntry.original.equals(text, ignoreCase = true)) {
                    cachedEntry.translated
                } else if (cachedEntry.translated.equals(text, ignoreCase = true)) {
                    cachedEntry.original
                } else {
                    null
                }
            } else {
                null
            }
        }
        if (cachedResult != null) {
            return cachedResult
        }

        return suspendCancellableCoroutine { continuation ->
            realTranslator.translate(text, targetLang) { deeplResult ->
                if (deeplResult != null) {
                    if (!text.equals(deeplResult, ignoreCase = true)) {
                        CoroutineScope(Dispatchers.IO).launch {
                            cacheDao.insertTranslation(
                                text.trim(),
                                deeplResult.trim()
                            )
                        }
                    } else {
                        Log.w(
                            "TranslationRepo",
                            "[TFSC_Suspend] Оригінал і переклад однакові ('$text' -> '$deeplResult'). Не кешуємо."
                        )
                    }
                } else {
                    Log.w("TranslationRepo", "[TFSC_Suspend] DeepL не зміг перекласти '$text'")
                }
                if (continuation.isActive) {
                    continuation.resume(deeplResult?.trim())
                }
            }
        }
    }
}
