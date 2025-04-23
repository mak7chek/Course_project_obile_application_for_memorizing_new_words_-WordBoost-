package com.example.wordboost.data.firebase

import com.example.wordboost.data.model.Word
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth

/**
 * Репозиторій для роботи зі словами у Firestore
 */
class FirebaseRepository {

    private val db = Firebase.firestore
    private val wordsCollection = db.collection("words")
    private val groupsCollection = db.collection("groups")

    /**
     * Отримати слова для практики (nextReview <= зараз, статус != "mastered")
     */
    fun getUserWordsForPractice(callback: (List<Word>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "testUser"
        val now = System.currentTimeMillis()

        wordsCollection
            .whereEqualTo("userId", "testUser")
//            .whereLessThanOrEqualTo("nextReview", now)
            .get()
            .addOnSuccessListener { result ->
                val words = result.documents.mapNotNull { it.toObject(Word::class.java) }
                    .filter { it.status != "mastered" }
                callback(words)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    /**
     * Отримати Word за текстом
     */
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

    /**
     * Отримати переклад для тексту (через Word.translation)
     */
    fun getTranslation(text: String, callback: (String?) -> Unit) {
        getWordObject(text) { word ->
            callback(word?.translation)
        }
    }

    /**
     * Зберегти або оновити Word у Firestore
     */
    fun saveWord(word: Word, callback: (Boolean) -> Unit = {}) {
        wordsCollection.document(word.id)
            .set(word, SetOptions.merge())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    /**
     * Отримати усі групи
     */
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

    /**
     * Створити нову групу
     */
    fun createGroup(name: String, callback: (Boolean) -> Unit) {
        val group = mapOf("name" to name)
        groupsCollection
            .add(group)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    /**
     * Оновити назву групи
     */
    fun updateGroup(groupId: String, newName: String, callback: (Boolean) -> Unit) {
        groupsCollection.document(groupId)
            .update("name", newName)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    /**
     * Видалити групу
     */
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
