package com.example.wordboost.data.repository

import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.local.CacheDao
import com.example.wordboost.translation.Translator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    onResult(firebaseResult)
                } else {
                    // 2. Перевірка у Room (кеш)
                    CoroutineScope(Dispatchers.IO).launch {
                        val cached = cacheDao.getTranslation(text)
                        if (cached != null) {
                            withContext(Dispatchers.Main) {
                                onResult(cached.translated)
                            }
                        } else {
                            // 3. DeepL
                            realTranslator.translate(text, targetLang) { deeplResult ->
                                if (deeplResult != null) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        cacheDao.insertTranslation(text, deeplResult)
                                    }
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