
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

        // 1. Перевірка в особистому словнику Firebase
        Log.d("TranslationRepo", "[UserVocabSuspend] Перевірка Firebase для: '$textToTranslate'")
        val firebaseResult = firebaseRepo.getTranslationSuspend(textToTranslate)
        if (firebaseResult != null) {
            Log.d("TranslationRepo", "[UserVocabSuspend] Знайдено у Firebase: '$textToTranslate' -> '$firebaseResult'")
            // Якщо знайшли у Firebase, це вже "переклад" (або оригінал, якщо шукали за перекладом).
            // Немає сенсу шукати далі або перекладати через DeepL.
            // Питання: чи потрібно кешувати те, що вже є у Firebase? Ймовірно, ні.
            return firebaseResult // Повертаємо те, що знайшли (це може бути переклад або оригінал)
        }
        Log.d("TranslationRepo", "[UserVocabSuspend] Не знайдено у Firebase для: '$textToTranslate'")

        // 2. Перевірка в локальному кеші (Room)
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
        Log.d("TranslationRepo", "[UserVocabSuspend] Використовуємо DeepL для '$textToTranslate' -> $targetLang")
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
        // 1. Перевірка в кеші (Room) - виконуємо в IO контексті
        val cachedResult = withContext(Dispatchers.IO) {
            val cachedEntry: CacheEntity? = cacheDao.getCacheEntry(text)
            if (cachedEntry != null) {
                cacheDao.updateTimestamp(cachedEntry.original, System.currentTimeMillis())
                Log.d(
                    "TranslationRepo",
                        "[TFSC_Suspend] Знайдено у кеші: ${cachedEntry.original} / ${cachedEntry.translated}"
                    )
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
        // 2. Якщо не знайдено в кеші, використовуємо DeepL
        // 2. DeepL
        Log.d(
            "TranslationRepo",
            "[TFSC_Suspend] Не знайдено у кеші. Використовуємо DeepL для '$text' -> $targetLang"
            )
        return suspendCancellableCoroutine { continuation ->
            realTranslator.translate(text, targetLang) { deeplResult ->
                if (deeplResult != null) {
                    // !!! НОВА ПЕРЕВІРКА ПЕРЕД ЗБЕРЕЖЕННЯМ В КЕШ !!!
                    if (!text.equals(deeplResult, ignoreCase = true)) {
                        CoroutineScope(Dispatchers.IO).launch {
                            cacheDao.insertTranslation(
                                text.trim(),
                                deeplResult.trim()
                            ) // Зберігаємо очищені значення
                            Log.d(
                                "TranslationRepo",
                                "[TFSC_Suspend] Результат DeepL '$deeplResult' вставлено в кеш для '$text'"
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
