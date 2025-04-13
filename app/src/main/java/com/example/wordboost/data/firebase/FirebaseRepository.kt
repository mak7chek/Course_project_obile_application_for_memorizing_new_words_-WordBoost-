package com.example.wordboost.data.firebase

import com.google.firebase.firestore.FirebaseFirestore

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveTranslation(word: String, translated: String) {
        val wordData = hashMapOf(
            "original" to word,
            "translated" to translated,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("translations")
            .add(wordData)
            .addOnSuccessListener {
                println("✅ Translation saved!")
            }
            .addOnFailureListener {
                println("❌ Failed to save: ${it.message}")
            }
    }

    fun getTranslation(word: String, onResult: (String?) -> Unit) {
        db.collection("translations")
            .whereEqualTo("original", word)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val result = documents.firstOrNull()?.getString("translated")
                onResult(result)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}