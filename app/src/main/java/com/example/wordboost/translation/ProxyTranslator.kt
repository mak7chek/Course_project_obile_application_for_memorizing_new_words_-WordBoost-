package com.example.wordboost.translation
import com.example.wordboost.data.firebase.*
// Тут ми припускаємо, що є FirebaseRepository, який займається збереженням і отриманням даних з Firebase.
class ProxyTranslator(
    private val realTranslator: RealTranslator,
    private val repository: FirebaseRepository
) : Translator {

    override fun translate(text: String, targetLang: String, callback: (String?) -> Unit) {
        // Спершу пробуємо отримати кешований переклад
        repository.getTranslation(text) { cached ->
            if (cached != null) {
                callback(cached)
            } else {
                // Якщо немає кешу – робимо реальний запит
                realTranslator.translate(text, targetLang) { result ->
                    if (result != null) {
                        repository.saveTranslation(text, result)
                    }
                    callback(result)
                }
            }
        }
    }
}