package com.example.wordboost.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SharedCardSet(
    @DocumentId // Firestore автоматично заповнить це поле ID документа
    var id: String = "",
    var name_uk: String = "", // Назва українською (Крок 1)
    var name_en: String? = null, // Назва англійською (Крок 2, може бути авто-перекладена та редагована)
    var description: String? = null, // Опис (опціонально)
    var authorId: String = "",       // UID користувача-автора
    var authorName: String? = null,  // Ім'я користувача-автора (для відображення)
    var difficultyLevel: String = DifficultyLevel.MEDIUM.key, // Рівень складності (Крок 2), зберігаємо ключ enum
    var isPublic: Boolean = true,    // Видимість набору (Крок 4)
    val languageOriginal: String = "en", // Зафіксовано для англо-українських наборів
    val languageTranslation: String = "uk", // Зафіксовано
    var wordCount: Int = 0,          // Кількість слів, оновлюється при збереженні
    @ServerTimestamp // Firestore автоматично встановлює час створення
    var createdAt: Date? = null,
    @ServerTimestamp // Firestore автоматично встановлює час останнього оновлення
    var updatedAt: Date? = null
    // Самі слова будуть зберігатися у підколекції "words" всередині цього документа
)