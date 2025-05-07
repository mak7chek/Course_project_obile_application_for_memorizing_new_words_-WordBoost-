package com.example.wordboost.data.firebase

import com.example.wordboost.data.model.Word // Переконайтесь, що імпортовано
import com.example.wordboost.data.model.Group // Переконайтесь, що імпортовано
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore // Явний імпорт для getInstance()
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Query.Direction

class FirebaseRepository(private val authRepository: AuthRepository) {

    private val db = Firebase.firestore
    private fun getUserWordsCollection() = authRepository.getCurrentUser()?.uid?.let { userId ->
        db.collection("users").document(userId).collection("words")
    } ?: run {
        Log.e("FirebaseRepo", "User not logged in or ID is null when trying to access words collection!")
        null
    }

    private fun getUserGroupsCollection() = authRepository.getCurrentUser()?.uid?.let { userId ->
        db.collection("users").document(userId).collection("groups")
    } ?: run {
        Log.e("FirebaseRepo", "User not logged in or ID is null when trying to access groups collection!")
        null // Повертаємо null, якщо користувач не залогінений
    }

    // !!! ПРИБРАНО обчислювані властивості userId, wordsCollection, groupsCollection !!!
    fun getWordsNeedingPracticeFlow(): Flow<List<Word>> {
        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null) {
            Log.w("FirebaseRepo", "User not logged in, cannot get practice words flow.")
            return emptyFlow()
        }

        // Використовуємо callbackFlow для обгортання Firestore real-time listener
        return callbackFlow {
            // Прибираємо статичний фільтр за часом з запиту Firestore
            val query = wordsCollection
                // Фільтруємо 'mastered' на стороні Firebase, якщо є індекс, або повністю покладаємось на клієнтську фільтрацію нижче.
                // Для надійності, давайте залишимо клієнтську фільтрацію як основну.
                // .whereNotEqualTo("status", "mastered") // Опціонально: вимагає індексу
                .orderBy("nextReview", Direction.ASCENDING) // Залишаємо сортування

            Log.d("FirebaseRepo", "Starting real-time listener for practice words (client-side time filter).")

            val listenerRegistration = query.addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("FirebaseRepo", "Error listening for practice words", exception)
                    close(exception)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Отримуємо поточний час *в момент отримання snapshot'а*
                    val now = System.currentTimeMillis()
                    val words = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Word::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseRepo", "Error mapping Firestore document to Word: ${doc.id}", e)
                            null
                        }
                    }
                    val wordsNeedingPractice = words
                        .filter { it.nextReview <= now } // Фільтруємо за часом *зараз*
                        .filter { it.status != "mastered" } // Фільтруємо "mastered"

                    Log.d("FirebaseRepo", "Listener received ${snapshot.size()} documents. Filtered to ${wordsNeedingPractice.size} words needing practice.")
                    trySend(wordsNeedingPractice).isSuccess
                } else {
                    Log.d("FirebaseRepo", "Listener received null snapshot.")
                    trySend(emptyList()).isSuccess
                }
            }

            // Припиняємо слухати, коли Flow скасовується (наприклад, ViewModel очищається)
            awaitClose {
                Log.d("FirebaseRepo", "Stopping real-time listener for practice words.")
                listenerRegistration.remove()
            }
        }
    }




    fun getWordObject(text: String, callback: (Word?) -> Unit) {
        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null) {
            callback(null)
            return
        }
        wordsCollection
            .whereEqualTo("text", text)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                val word = docs.firstOrNull()?.toObject(Word::class.java)
                callback(word)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepo", "Error getting word object by text", e)
                callback(null)
            }
    }

    fun getTranslation(text: String, callback: (String?) -> Unit) {
        val wordsCollection = getUserWordsCollection() // Отримуємо колекцію
        if (wordsCollection == null) { // Перевірка
            callback(null)
            return
        }

        wordsCollection
            .whereEqualTo("text", text)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val word = docs.firstOrNull()?.toObject(Word::class.java)
                    callback(word?.translation)
                } else {
                    wordsCollection
                        .whereEqualTo("translation", text)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { altDocs ->
                            val word = altDocs.firstOrNull()?.toObject(Word::class.java)
                            callback(word?.text) // Знайшли в translation, повертаємо text
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseRepo", "Error getting translation (alt query)", e)
                            callback(null)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepo", "Error getting translation (main query)", e)
                callback(null)
            }
    }

    // saveWord залишається з Callback
    fun saveWord(word: Word, callback: (Boolean) -> Unit = {}) {
        val wordsCollection = getUserWordsCollection() // Отримуємо колекцію
        if (wordsCollection == null || word.id.isBlank()) { // Перевірка + валідність ID
            Log.e("FirebaseRepo", "Cannot save word. User not logged in or word ID is blank.")
            callback(false)
            return
        }
        wordsCollection.document(word.id)
            .set(word, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FirebaseRepo", "Word ${word.id} saved successfully.")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepo", "Error saving word ${word.id}", e)
                callback(false)
            }
    }

    fun deleteWord(wordId: String, callback: (Boolean) -> Unit) {
        val wordsCollection = getUserWordsCollection() // Отримуємо колекцію
        if (wordsCollection == null || wordId.isBlank()) { // Перевірка + валідність ID
            Log.e("FirebaseRepo", "Cannot delete word. User not logged in or word ID is blank.")
            callback(false)
            return
        }
        wordsCollection.document(wordId)
            .delete()
            .addOnSuccessListener {
                Log.d("FirebaseRepo", "Word $wordId deleted successfully.")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepo", "Error deleting word $wordId", e)
                callback(false)
            }
    }
    fun getWordById(wordId: String, callback: (Word?) -> Unit) {
        getUserWordsCollection()?.document(wordId)?.get()
            ?.addOnSuccessListener { document ->
                val word = document.toObject(Word::class.java)
                callback(word)
            }
            ?.addOnFailureListener { e ->
                Log.e("FirebaseRepo", "Error getting word by ID: $wordId", e)
                callback(null)
            }
            ?: callback(null) // Якщо колекція не доступна
    }
    fun getWordsListener(callback: (List<Word>) -> Unit): ListenerRegistration? {
        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null) {
            Log.e("FirebaseRepo", "User not logged in! Cannot attach words listener.")
            callback(emptyList())
            return null
        }
        return wordsCollection
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseRepo", "Listener error getting words", e)
                    callback(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val words = snapshot.toObjects(Word::class.java)
                    Log.d("FirebaseRepo", "Words listener updated. Count: ${words.size}")
                    callback(words) // Повертаємо актуальний список слів
                } else {
                    Log.d("FirebaseRepo", "Words listener snapshot is null.")
                    callback(emptyList()) // Повертаємо порожній список, якщо snapshot null
                }
            }
    }


    fun getAllWords(callback: (List<Word>) -> Unit) {
        val wordsCollection = getUserWordsCollection() // Отримуємо колекцію
        if (wordsCollection == null) { // Перевірка
            callback(emptyList())
            return
        }
        wordsCollection
            .get() // Одноразовий запит
            .addOnSuccessListener { result ->
                // @DocumentId автоматично заповнить поле 'id'
                val words = result.documents.mapNotNull { it.toObject(Word::class.java) }
                Log.d("FirebaseRepo", "All words fetched. Count: ${words.size}")
                callback(words)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepo", "Error getting all words", e)
                callback(emptyList())
            }
    }


    fun getGroups(callback: (List<Group>) -> Unit): ListenerRegistration? {
        val groupsCollection = getUserGroupsCollection()
        if (groupsCollection == null) {
            callback(emptyList())
            return null
        }

        return groupsCollection
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseRepo", "Listener error getting groups", e)
                    callback(emptyList()) // Повертаємо порожній список у разі помилки
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Мапимо документи в об'єкти Group, @DocumentId заповнить id
                    val groups = snapshot.toObjects(Group::class.java)
                    // !!! НЕ ДОДАЄМО "Основний словник" ХАРДКОДОМ ТУТ !!!
                    // Це відповідальність ViewModel, який готує список для діалогу
                    Log.d("FirebaseRepo", "Groups listener updated. Count: ${groups.size}")
                    callback(groups) // Повертаємо список груп з Firebase
                } else {
                    Log.d("FirebaseRepo", "Groups listener snapshot is null.")
                    callback(emptyList()) // Повертаємо порожній список, якщо snapshot null
                }
            }
    }

    suspend fun deleteGroup(groupId: String): Boolean {
        val wordsCollection = getUserWordsCollection() // Отримуємо колекцію слів
        val groupsCollection = getUserGroupsCollection() // Отримуємо колекцію груп
        if (wordsCollection == null || groupsCollection == null || groupId.isBlank()) { // Перевірка
            Log.e("FirebaseRepo", "Cannot delete group. User not logged in or group ID is blank.")
            return false
        }

        try {
            val firestore = FirebaseFirestore.getInstance() // Отримуємо екземпляр Firestore
            val batch = firestore.batch() // Створюємо batch операцію

            // 1. Знаходимо всі слова, що належать до цієї групи
            val wordsToDeleteQuerySnapshot = wordsCollection
                .whereEqualTo("dictionaryId", groupId)
                .get()
                .await() // Чекаємо результату запиту слів у корутині

            // 2. Додаємо операції оновлення для кожного знайденого слова в batch
            // Встановлюємо dictionaryId в null (або порожній рядок)
            for (document in wordsToDeleteQuerySnapshot.documents) {
                val wordRef = wordsCollection.document(document.id) // Посилання на документ слова
                batch.update(wordRef, "dictionaryId", null) // <-- Встановлюємо dictionaryId в null
                // Або якщо ви хочете порожній рядок: batch.update(wordRef, "dictionaryId", "")
            }

            // 3. Додаємо операцію видалення самого документа групи в batch
            val groupRef = groupsCollection.document(groupId) // Посилання на документ групи
            batch.delete(groupRef) // Додаємо операцію видалення

            // 4. Виконуємо batch операцію (оновлення слів + видалення групи)
            batch.commit().await() // Чекаємо завершення виконання batch

            Log.d("FirebaseRepo", "Group $groupId deleted and associated words updated successfully in batch.")
            return true // Операція пройшла успішно
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error deleting group $groupId with associated words.", e)
            // Обробка помилки
            return false // Повертаємо помилку
        }
    }

    suspend fun createGroup(name: String): Boolean {
        val groupsCollection = getUserGroupsCollection() // Отримуємо колекцію груп
        if (groupsCollection == null || name.isBlank()) { // Перевірка
            Log.e("FirebaseRepo", "Cannot create group. User not logged in or group name is blank.")
            return false
        }

        try {
            val groupId = groupsCollection.document().id
            val newGroup = Group(id = groupId, name = name)

            groupsCollection.document(groupId).set(newGroup).await()

            Log.d("FirebaseRepo", "Group ${groupId} '${name}' created successfully.")
            return true
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error creating group with name '${name}'.", e)
            return false
        }
    }

    suspend fun updateGroup(groupId: String, newName: String): Boolean {
        val groupsCollection = getUserGroupsCollection() // Отримуємо колекцію груп
        if (groupsCollection == null || groupId.isBlank() || newName.isBlank()) { // Перевірка
            Log.e("FirebaseRepo", "Cannot update group. User not logged in, group ID is blank, or new name is blank.")
            return false
        }

        try {
            groupsCollection.document(groupId).update("name", newName).await()

            Log.d("FirebaseRepo", "Group $groupId updated name to '${newName}' successfully.")
            return true
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updating group $groupId with name '${newName}'.", e)
            return false
        }
    }
}