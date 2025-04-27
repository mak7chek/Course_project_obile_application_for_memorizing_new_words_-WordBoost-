
package com.example.wordboost.data.firebase

import com.example.wordboost.data.model.Word
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth

/**
 * Репозиторій для роботи зі словами та групами у Firestore, прив'язаними до користувача
 */
class FirebaseRepository {

    private val db = Firebase.firestore
    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "testUser"

    private val wordsCollection
        get() = db.collection("users").document(userId).collection("words")

    private val groupsCollection
        get() = db.collection("users").document(userId).collection("groups")

    fun getUserWordsForPractice(callback: (List<Word>) -> Unit) {
        val now = System.currentTimeMillis()
        wordsCollection
            .whereLessThanOrEqualTo("nextReview", now) // Додано фільтр на стороні сервера
            .get()
            .addOnSuccessListener { result ->
                val words = result.documents.mapNotNull { it.toObject(Word::class.java) }
                    .filter { it.status != "mastered" } // Фільтр за статусом залишаємо на клієнті, якщо Firestore не підтримує комбінований індекс (або якщо це не велика кількість "mastered" слів). Якщо статус часто "mastered", краще додати його в where clause, якщо це можливо з індексами.
                callback(words)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun getWordObject(text: String, callback: (Word?) -> Unit) {
        wordsCollection
            .whereEqualTo("text", text)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                val word = docs.firstOrNull()?.toObject(Word::class.java)
                callback(word)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun getTranslation(text: String, callback: (String?) -> Unit) {
        wordsCollection
            .whereEqualTo("text", text)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {

                    wordsCollection
                        .whereEqualTo("translation", text)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { altDocs ->
                            val word = altDocs.firstOrNull()?.toObject(Word::class.java)
                            callback(word?.text)
                        }
                        .addOnFailureListener { callback(null) }
                } else {
                    val word = docs.firstOrNull()?.toObject(Word::class.java)
                    callback(word?.translation)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun saveWord(word: Word, callback: (Boolean) -> Unit = {}) {
        wordsCollection.document(word.id)
            .set(word, SetOptions.merge())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun getGroups(callback: (List<Group>) -> Unit) {
        groupsCollection
            .get()
            .addOnSuccessListener { result ->
                val groups = result.mapNotNull { doc ->
                    val id = doc.id
                    val name = doc.getString("name")
                    if (name != null) Group(id, name) else null
                }
                callback(groups)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun createGroup(name: String, callback: (Boolean) -> Unit) {
        val group = mapOf("name" to name)
        groupsCollection
            .add(group)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updateGroup(groupId: String, newName: String, callback: (Boolean) -> Unit) {
        groupsCollection.document(groupId)
            .update("name", newName)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun deleteGroup(groupId: String, callback: (Boolean) -> Unit) {
        groupsCollection.document(groupId)
            .delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}

/**
 * Модель групи
 */
data class Group(
    val id: String = "",
    val name: String = ""
)
