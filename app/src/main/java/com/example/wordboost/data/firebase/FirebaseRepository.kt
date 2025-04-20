package com.example.wordboost.data.firebase


import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.wordboost.data.model.Word
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore

class FirebaseRepository {

    private val db = Firebase.firestore
    private val wordsCollection = db.collection("words")
    private val groupsCollection = db.collection("groups")


    fun getTranslation(text: String, callback: (String?) -> Unit) {
        wordsCollection.whereEqualTo("original", text)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                val translated = docs.firstOrNull()?.getString("translated")
                callback(translated)
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

    fun saveTranslation(text: String, translated: String, groupId: String? = null) {
        val data = mutableMapOf<String, Any>(
            "original" to text,
            "translated" to translated,
            "timestamp" to System.currentTimeMillis()
        )
        groupId?.let {
            data["groupId"] = it
        }

        wordsCollection.add(data)
    }

    fun getGroups(callback: (List<Group>) -> Unit) {
        groupsCollection.get()
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
        groupsCollection.add(group)
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
        groupsCollection.document(groupId).delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
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
}

data class Group(
    val id: String,
    val name: String
)