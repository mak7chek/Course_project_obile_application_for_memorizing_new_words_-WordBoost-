package com.example.wordboost.data.model

enum class DifficultyLevel(val displayName: String, val key: String) {
    EASY("Легкий", "EASY"),
    MEDIUM("Середній", "MEDIUM"),
    HARD("Важкий", "HARD");

    companion object {
        // Допоміжні функції для отримання enum за ключем або назвою
        fun fromKey(key: String?): DifficultyLevel? {
            return values().find { it.key.equals(key, ignoreCase = true) }
        }
        fun fromDisplayName(displayName: String?): DifficultyLevel? {
            return values().find { it.displayName == displayName }
        }
        // Список назв для відображення у UI (наприклад, у випадаючому списку)
        val displayNamesList = values().map { it.displayName }
    }
}