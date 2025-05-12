package com.example.wordboost.data.firebase

import android.util.Log
import com.example.wordboost.data.model.*
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Query.Direction
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import com.example.wordboost.data.model.UserSharedSetProgress
import com.example.wordboost.viewmodel.SharedSetDetailsWithWords
import com.google.firebase.firestore.QuerySnapshot

class FirebaseRepository(private val authRepository: AuthRepository) {
    private val db = Firebase.firestore
    private val TAG: String = "FirestoreHelper"
    private fun getUserWordsCollectionPath(): String? {
        val userId = authRepository.getCurrentUser()?.uid
        return if (userId != null) "users/$userId/words" else null
    }
    private fun getUserWordsCollection() = authRepository.getCurrentUser()?.uid?.let { userId ->
        Log.i("FirebaseRepo_User", "getUserWordsCollection: Accessing words for userId: $userId")
        db.collection("users").document(userId).collection("words")
    } ?: run {
        Log.e("FirebaseRepo_User", "getUserWordsCollection: User not logged in or ID is null!")
        null
    }

    private fun getUserGroupsCollection() = authRepository.getCurrentUser()?.uid?.let { userId ->
        Log.i("FirebaseRepo_User", "getUserGroupsCollection: Accessing groups for userId: $userId")
        db.collection("users").document(userId).collection("groups")
    } ?: run {
        Log.e("FirebaseRepo_User", "getUserGroupsCollection: User not logged in or ID is null!")
        null
    }

    private fun getSharedSetsCollection() = db.collection("sharedCardSets")
    suspend fun updateSharedCardSetAndWords(
        setWithUpdates: SharedCardSet, // Цей об'єкт вже має містити правильний set.id
        updatedWords: List<SharedSetWordItem>
    ): Result<Unit> {
        val currentUserUid = authRepository.getCurrentUser()?.uid
        if (currentUserUid == null) {
            Log.e("FirebaseRepo", "[updateSet] User not authenticated.")
            return Result.failure(IllegalStateException("User not authenticated."))
        }
        if (setWithUpdates.id.isBlank()) {
            Log.e("FirebaseRepo", "[updateSet] Set ID for update cannot be blank.")
            return Result.failure(IllegalArgumentException("Set ID for update cannot be blank."))
        }
        // Перевірка, чи користувач є автором і не намагається змінити авторство
        if (setWithUpdates.authorId != currentUserUid) {
            Log.e("FirebaseRepo", "[updateSet] Attempt to change authorship or update set not owned by user. Current: $currentUserUid, SetAuthor: ${setWithUpdates.authorId}")
            return Result.failure(SecurityException("Cannot change set authorship or update set not owned by user."))
        }

        Log.d("FirebaseRepo", "[updateSet] Attempting to update shared set with ID: ${setWithUpdates.id} by user: $currentUserUid")
        val setDocRef = getSharedSetsCollection().document(setWithUpdates.id)

        return try {
            // Переконуємося, що оновлюємо wordCount та, можливо, updatedAt (хоча updatedAt оновиться сервером)
            val finalSetData = setWithUpdates.copy(
                wordCount = updatedWords.size,
                updatedAt = null // Встановлюємо null, щоб @ServerTimestamp спрацював для оновлення
            )

            // Крок 1: Отримуємо посилання на всі існуючі слова для їх видалення
            val existingWordDocsSnapshot: QuerySnapshot
            try {
                existingWordDocsSnapshot = setDocRef.collection("words").get().await()
                Log.d("FirebaseRepo", "[updateSet] Found ${existingWordDocsSnapshot.size()} existing words for set ${setWithUpdates.id}")
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "[updateSet] Error fetching existing words for deletion for set ${setWithUpdates.id}", e)
                return Result.failure(e) // Помилка при читанні слів перед оновленням
            }

            // Крок 2: Виконуємо пакетну операцію
            db.runBatch { batch ->
                // 2.1: Додаємо операції видалення для всіх існуючих слів
                if (!existingWordDocsSnapshot.isEmpty) {
                    for (doc in existingWordDocsSnapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    Log.d("FirebaseRepo", "[updateSet] Scheduled deletion of ${existingWordDocsSnapshot.size()} old words.")
                }

                // 2.2: Додаємо нові/оновлені слова
                val wordsSubCollectionRef = setDocRef.collection("words")
                updatedWords.forEach { wordItem ->
                    // Для кожного слова генеруємо новий ID, оскільки ми перезаписуємо підколекцію.
                    // Або, якщо ти хочеш зберегти ID слів, що редагуються, потрібна складніша логіка.
                    // Для простоти - генеруємо нові ID для всіх слів при оновленні.
                    val newWordDocRef = wordsSubCollectionRef.document() // Новий ID для кожного слова

                    // Переконуємося, що authorId є у слова, якщо правила цього вимагають.
                    // Оскільки правила для words/write тепер перевіряють authorId батьківського набору,
                    // authorId у самому слові не є критичним для правил ЗАПИСУ/ОНОВЛЕННЯ/ВИДАЛЕННЯ слів,
                    // але він потрібен для правила СТВОРЕННЯ слів, якщо воно окреме.
                    // Краще його мати для консистентності.
                    val finalWordItem = if (wordItem.authorId.isBlank()) {
                        wordItem.copy(id = newWordDocRef.id, authorId = currentUserUid)
                    } else {
                        wordItem.copy(id = newWordDocRef.id) // Використовуємо новий ID
                    }
                    batch.set(newWordDocRef, finalWordItem)
                }
                Log.d("FirebaseRepo", "[updateSet] Scheduled add/update of ${updatedWords.size} words.")

                // 2.3: Оновлюємо основний документ набору
                // Використовуємо SetOptions.merge(), щоб оновити тільки передані поля,
                // або .set(finalSetData) для повного перезапису (якщо createdAt/updatedAt керуються тільки сервером).
                // Оскільки finalSetData містить всі поля, .set() є нормальним.
                batch.set(setDocRef, finalSetData)
                Log.d("FirebaseRepo", "[updateSet] Scheduled update for main document for set ${setWithUpdates.id}.")

            }.await() // Чекаємо завершення пакетної операції

            Log.i("FirebaseRepo", "[updateSet] SharedCardSet with ID: ${setWithUpdates.id} and its words updated successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "[updateSet] Error updating SharedCardSet with ID: ${setWithUpdates.id}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSharedCardSet(setId: String): Result<Unit> {
        val currentUserUid = authRepository.getCurrentUser()?.uid
        if (currentUserUid == null) {
            Log.e("FirebaseRepo", "deleteSharedCardSet: User not authenticated.")
            return Result.failure(IllegalStateException("User not authenticated."))
        }
        if (setId.isBlank()) {
            Log.e("FirebaseRepo", "deleteSharedCardSet: Set ID is blank.")
            return Result.failure(IllegalArgumentException("Set ID cannot be blank."))
        }

        Log.d("FirebaseRepo", "Attempting to delete shared set with ID: $setId by user: $currentUserUid")

        return try {
            val setDocRef = getSharedSetsCollection().document(setId)

            val setToDeleteSnapshot = setDocRef.get().await()
            if (!setToDeleteSnapshot.exists()) {
                Log.w("FirebaseRepo", "deleteSharedCardSet: Set with ID $setId does not exist. Nothing to delete.")
                return Result.success(Unit)
            }
            val setToDelete = setToDeleteSnapshot.toObject(SharedCardSet::class.java)
            if (setToDelete?.authorId != currentUserUid) {
                Log.w("FirebaseRepo", "deleteSharedCardSet: User $currentUserUid is not the author of set $setId (author: ${setToDelete?.authorId}). This action should be denied by security rules.")
                return Result.failure(SecurityException("User is not the author of the set."))
            }

            val wordsQuerySnapshot = setDocRef.collection("words").get().await()
            Log.d("FirebaseRepo", "Found ${wordsQuerySnapshot.size()} words in subcollection of set $setId to delete.")

            db.runBatch { batch ->
                if (!wordsQuerySnapshot.isEmpty) {
                    for (wordDoc in wordsQuerySnapshot.documents) {
                        batch.delete(wordDoc.reference)
                    }
                }
                batch.delete(setDocRef)
                Log.d("FirebaseRepo", "Main document for set $setId and its ${wordsQuerySnapshot.size()} words added to delete batch.")

            }.await()

            Log.i("FirebaseRepo", "SharedCardSet with ID: $setId and its words deleted successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error deleting SharedCardSet with ID: $setId", e)
            Result.failure(e)
        }
    }

    suspend fun getMySharedSets(userId: String): Result<List<SharedCardSetSummary>> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        return try {
            val snapshot = getSharedSetsCollection()
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING) // Новіші спочатку
                .get()
                .await()

            val summaries = snapshot.documents.mapNotNull { doc ->
                // Мапимо документ на SharedCardSet, потім на SharedCardSetSummary
                // Або напряму на SharedCardSetSummary, якщо поля співпадають
                val set = doc.toObject(SharedCardSet::class.java) // Повна модель
                set?.let {
                    SharedCardSetSummary( // Створюємо summary з повної моделі
                        id = it.id,
                        name_uk = it.name_uk,
                        authorName = it.authorName, // Або просто userId, якщо ім'я не зберігається тут
                        wordCount = it.wordCount,
                        difficultyLevelKey = it.difficultyLevel,
                        public = it.public,
                        createdAt = it.createdAt
                    )
                }
            }
            Log.d("FirebaseRepo", "Fetched ${summaries.size} of my shared sets for user $userId")
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error fetching my shared sets for user $userId", e)
            Result.failure(e)
        }
    }
    suspend fun getUserProgressForSharedSet(userId: String, sharedSetId: String): UserSharedSetProgress? {
        if (userId.isBlank() || sharedSetId.isBlank()) return null
        Log.d("FirebaseRepo", "Fetching user progress for userId: $userId, setId: $sharedSetId")
        return try {
            db.collection("users").document(userId)
                .collection("browsedSharedSetsProgress").document(sharedSetId)
                .get().await().toObject(UserSharedSetProgress::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error fetching user progress for userId: $userId, setId: $sharedSetId", e)
            null
        }
    }

    suspend fun savePersonalWordSuspend(word: Word): Boolean {
        if (word.id.isBlank()) {
            // Якщо ID порожній, це може бути помилкою або Firestore сам згенерує,
            // але краще мати ID з ViewModel (наприклад, UUID.randomUUID().toString())
            Log.e("FirebaseRepo", "[savePersonalWordSuspend] Word ID is blank for word: ${word.text}")
            // return false // Можна повернути помилку, якщо ID обов'язковий
        }
        Log.d("FirebaseRepo", "[savePersonalWordSuspend] Saving personal word '${word.text}' with ID '${word.id}'")
        return suspendCancellableCoroutine { continuation ->
            saveWord(word) { success -> // Викликаємо твій існуючий saveWord з колбеком
                if (continuation.isActive) {
                    continuation.resume(success)
                }
            }
        }
    }
    suspend fun saveUserProgressForSharedSet(userId: String, sharedSetId: String, progress: UserSharedSetProgress): Boolean {
        if (userId.isBlank() || sharedSetId.isBlank()) return false
        Log.d("FirebaseRepo", "Saving user progress for userId: $userId, setId: $sharedSetId. Index: ${progress.currentWordIndex}, Completed: ${progress.isCompleted}")
        return try {
            db.collection("users").document(userId)
                .collection("browsedSharedSetsProgress").document(sharedSetId)
                .set(progress, SetOptions.merge()) // SetOptions.merge для оновлення або створення
                .await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error saving user progress for userId: $userId, setId: $sharedSetId", e)
            false
        }
    }
    suspend fun getPublicSharedSets(currentUserId: String): Result<List<SharedCardSetSummary>> {
        return try {
            val query = getSharedSetsCollection()
                .whereEqualTo("public", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50) // Обмежимо кількість для початку

            val snapshot = query.get().await()
            val summaries = snapshot.documents.mapNotNull { doc ->
                val set = doc.toObject(SharedCardSet::class.java)
                // Відфільтровуємо набори поточного користувача, якщо не використовували whereNotEqualTo
                if (set?.authorId == currentUserId) return@mapNotNull null
                set?.let {
                    SharedCardSetSummary(
                        id = it.id,
                        name_uk = it.name_uk,
                        authorName = it.authorName,
                        wordCount = it.wordCount,
                        difficultyLevelKey = it.difficultyLevel,
                        public = it.public,
                        createdAt = it.createdAt
                    )
                }
            }
            Log.d("FirebaseRepo", "Fetched ${summaries.size} public shared sets (excluding user's own if filtered).")
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error fetching public shared sets", e)
            Result.failure(e)
        }
    }
    suspend fun getSharedCardSetWithWords(setId: String): Result<SharedSetDetailsWithWords> {
        if (setId.isBlank()) return Result.failure(IllegalArgumentException("Set ID cannot be blank."))
        Log.d("FirebaseRepo", "Fetching shared set with words for ID: $setId")
        return try {
            val setDocRef = getSharedSetsCollection().document(setId)
            val setSnapshot = setDocRef.get().await()
            val sharedSet = setSnapshot.toObject(SharedCardSet::class.java)

            if (sharedSet == null) {
                Log.w("FirebaseRepo", "SharedCardSet with ID $setId not found.")
                return Result.failure(NoSuchElementException("SharedCardSet not found."))
            }

            val wordsSnapshot = setDocRef.collection("words")
                // .orderBy("addedAt", Query.Direction.ASCENDING) // Опціонально, якщо є поле addedAt і потрібне сортування
                .get()
                .await()

            val wordsList = wordsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(SharedSetWordItem::class.java)
            }
            Log.d("FirebaseRepo", "Fetched ${wordsList.size} words for set $setId.")
            Result.success(SharedSetDetailsWithWords(sharedSet, wordsList))
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error fetching shared set with words for ID $setId", e)
            Result.failure(e)
        }
    }

    suspend fun getTranslationSuspend(text: String): String? {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            Log.d("FirebaseRepo", "[getTranslationSuspend] Input text is blank, returning null.")
            return null
        }

        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null) {
            Log.w("FirebaseRepo", "[getTranslationSuspend] User words collection is null, cannot get translation.")
            return null
        }
        Log.d("FirebaseRepo", "[getTranslationSuspend] Searching for '$trimmedText' in ${wordsCollection.path}")

        try {
            // 1. Перевіряємо, чи 'trimmedText' є в полі 'text' (оригінальне слово)
            var querySnapshot = wordsCollection
                .whereEqualTo("text", trimmedText)
                .limit(1)
                .get()
                .await() // Використовуємо await() для suspend-взаємодії

            if (!querySnapshot.isEmpty) {
                val word = querySnapshot.documents.firstOrNull()?.toObject(Word::class.java)
                val translation = word?.translation?.trim()
                Log.d("FirebaseRepo", "[getTranslationSuspend] Found as 'text': '$trimmedText' -> '$translation'.")
                return translation
            }

            // 2. Якщо не знайдено, перевіряємо, чи 'trimmedText' є в полі 'translation'
            querySnapshot = wordsCollection
                .whereEqualTo("translation", trimmedText)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val word = querySnapshot.documents.firstOrNull()?.toObject(Word::class.java)
                val originalText = word?.text?.trim()
                Log.d("FirebaseRepo", "[getTranslationSuspend] Found as 'translation': '$originalText' -> '$trimmedText'. Returning original: '$originalText'")
                return originalText // Повертаємо оригінальне слово
            }

            Log.d("FirebaseRepo", "[getTranslationSuspend] Word '$trimmedText' not found in user's Firebase dictionary as original or translation.")
            return null // Не знайдено в жодному з полів
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "[getTranslationSuspend] Error getting translation for '$trimmedText' from Firestore", e)
            return null // Помилка під час запиту
        }
    }

    @Deprecated("Use getTranslationSuspend instead for better coroutine integration.", ReplaceWith("getTranslationSuspend(text)"))
    fun getTranslation(text: String, callback: (String?) -> Unit) {
        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null) {
            callback(null)
            return
        }
        val trimmedText = text.trim()

        wordsCollection
            .whereEqualTo("text", trimmedText)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val word = docs.firstOrNull()?.toObject(Word::class.java)
                    callback(word?.translation?.trim())
                } else {
                    wordsCollection
                        .whereEqualTo("translation", trimmedText)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { altDocs ->
                            val word = altDocs.firstOrNull()?.toObject(Word::class.java)
                            callback(word?.text?.trim())
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseRepo", "[getTranslation_OLD] Error (alt query for '$trimmedText')", e)
                            callback(null)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepo", "[getTranslation_OLD] Error (main query for '$trimmedText')", e)
                callback(null)
            }
    }

    suspend fun createSharedCardSetInFirestore(
        sharedSet: SharedCardSet,
        wordsToAdd: List<SharedSetWordItem>
    ): Result<String> {
        val currentUserUid = authRepository.getCurrentUser()?.uid
        if (currentUserUid == null || currentUserUid != sharedSet.authorId) {
            Log.e("FirebaseRepo", "User not logged in or authorId mismatch for creating shared set.")
            return Result.failure(IllegalStateException("User not authorized or authorId mismatch."))
        }

        val newSetDocumentRef = getSharedSetsCollection().document()
        val finalSetData = sharedSet.copy(
            id = newSetDocumentRef.id,
            wordCount = wordsToAdd.size
        )

        return try {
            db.runBatch { batch ->
                batch.set(newSetDocumentRef, finalSetData)
                val wordsSubCollectionRef = newSetDocumentRef.collection("words")
                wordsToAdd.forEach { wordItem ->
                    val newWordDocRef = wordsSubCollectionRef.document()
                    batch.set(newWordDocRef, wordItem)
                }
            }.await()
            Log.d("FirebaseRepo", "SharedCardSet '${finalSetData.name_uk}' (ID: ${finalSetData.id}) created with ${wordsToAdd.size} words.")
            Result.success(finalSetData.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error creating SharedCardSet '${finalSetData.name_uk}' in Firestore", e)
            Result.failure(e)
        }
    }


    // --- Методи для слів користувача (Practice, WordList, etc.) ---
    fun getWordsNeedingPracticeFlow(): Flow<List<Word>> {
        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null) {
            Log.w("FirebaseRepo_Practice", "[getWNPFlow] User collection null. Returning emptyFlow().")
            return emptyFlow()
        }
        return callbackFlow {
            val query = wordsCollection.orderBy("nextReview", Direction.ASCENDING)
            Log.i("FirebaseRepo_Practice", "[getWNPFlow] Setting up listener. Path: ${wordsCollection.path}")
            val listenerReg = query.addSnapshotListener { snapshot, e ->
                if (e != null) { Log.e("FirebaseRepo_Practice", "[getWNPFlow] Listener ERROR", e); close(e); return@addSnapshotListener }
                if (snapshot != null) {
                    val now = System.currentTimeMillis()
                    val words = snapshot.toObjects(Word::class.java) // Простіше мапити так
                    val due = words.filter { it.nextReview <= now && it.status != "mastered" }
                    Log.i("FirebaseRepo_Practice", "[getWNPFlow] Received ${snapshot.size()} docs. Filtered to ${due.size} due words.")
                    trySend(due).isSuccess
                } else { Log.d("FirebaseRepo_Practice", "[getWNPFlow] Listener received NULL snapshot.") }
            }
            awaitClose { Log.i("FirebaseRepo_Practice", "[getWNPFlow] Stopping listener."); listenerReg.remove() }
        }
    }

    fun saveWord(word: Word, callback: (Boolean) -> Unit = {}) {
        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null || word.id.isBlank()) {
            Log.e("FirebaseRepo", "Cannot save word. User null or word ID blank. Word ID: '${word.id}'")
            callback(false); return
        }
        Log.d("FirebaseRepo", "Saving word '${word.text}' (ID: ${word.id}) to ${wordsCollection.path}")
        wordsCollection.document(word.id).set(word, SetOptions.merge())
            .addOnSuccessListener { Log.d("FirebaseRepo", "Word ${word.id} saved."); callback(true) }
            .addOnFailureListener { e -> Log.e("FirebaseRepo", "Error saving word ${word.id}", e); callback(false) }
    }

    fun getWordObject(text: String, callback: (Word?) -> Unit) {
        val userWordsCollection = getUserWordsCollection()
        if (userWordsCollection == null) {
            Log.w(TAG, "User words collection is null. Cannot get word object.")
            callback(null)
            return
        }

        userWordsCollection.whereEqualTo("text", text)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val word = querySnapshot.documents.firstOrNull()?.toObject(Word::class.java)
                callback(word)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting word object by text: $text", exception)
                callback(null)
            }
    }

    /**
     * Deletes a Word document from Firestore by its ID.
     *
     * @param wordId The ID of the word document to delete.
     * @param callback A callback function to indicate success (true) or failure (false).
     */
    fun deleteWord(wordId: String, callback: (Boolean) -> Unit) {
        val userWordsCollection = getUserWordsCollection()
        if (userWordsCollection == null) {
            Log.w(TAG, "User words collection is null. Cannot delete word.")
            callback(false)
            return
        }

        if (wordId.isBlank()) {
            Log.w(TAG, "Word ID is blank. Cannot delete word.")
            callback(false)
            return
        }

        userWordsCollection.document(wordId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Word deleted successfully with ID: $wordId")
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting word with ID: $wordId", exception)
                callback(false)
            }
    }

    /**
     * Retrieves a Word object from Firestore by its document ID.
     *
     * @param wordId The ID of the word document.
     * @param callback A callback function to receive the Word object or null if not found or an error occurs.
     */
    fun getWordById(wordId: String, callback: (Word?) -> Unit) {
        val userWordsCollection = getUserWordsCollection()
        if (userWordsCollection == null) {
            Log.w(TAG, "User words collection is null. Cannot get word by ID.")
            callback(null)
            return
        }

        userWordsCollection.document(wordId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val word = documentSnapshot.toObject(Word::class.java)
                callback(word)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting word by ID: $wordId", exception)
                callback(null)
            }
    }

    /**
     * Retrieves a Word object from Firestore by its document ID using Kotlin Coroutines.
     *
     * @param wordId The ID of the word document.
     * @return The Word object or null if not found or an error occurs.
     */
    suspend fun getWordById_Suspend(wordId: String): Word? {
        val userWordsCollection = getUserWordsCollection()
        if (userWordsCollection == null) {
            Log.w(TAG, "User words collection is null. Cannot get word by ID (suspend).")
            return null
        }

        return try {
            val documentSnapshot = userWordsCollection.document(wordId).get().await()
            documentSnapshot.toObject(Word::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting word by ID (suspend): $wordId", e)
            null
        }
    }

    /**
     * Sets up a real-time listener for changes to the user's words collection.
     *
     * @param callback A callback function to receive the list of Word objects whenever the data changes.
     * @return A ListenerRegistration object that can be used to remove the listener.
     */
    fun getWordsListener(callback: (List<Word>) -> Unit): ListenerRegistration? {
        val userWordsCollection = getUserWordsCollection()
        if (userWordsCollection == null) {
            Log.w(TAG, "User words collection is null. Cannot set up words listener.")
            callback(emptyList())
            return null
        }

        return userWordsCollection.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                Log.e(TAG, "Error listening for words changes", exception)
                callback(emptyList())
                return@addSnapshotListener
            }

            val words = querySnapshot?.toObjects(Word::class.java) ?: emptyList()
            callback(words)
        }
    }


    // --- Методи для груп (Groups) ---
    // getGroups, deleteGroup, createGroup, updateGroup - без змін від твого коду
    // ... (скопіюй їх сюди)
    fun getGroups(callback: (List<Group>) -> Unit): ListenerRegistration? { val gc=getUserGroupsCollection();if(gc==null){callback(emptyList());return null};return gc.addSnapshotListener{snap,e->if(e!=null){Log.e("FR","Err groupsListen",e);callback(emptyList());return@addSnapshotListener};callback(snap?.toObjects(Group::class.java)?:emptyList())}}
    suspend fun deleteGroup(groupId: String): Boolean {
        val wc=getUserWordsCollection()
        val gc=getUserGroupsCollection()
        if(wc==null||gc==null||groupId.isBlank()){
            return false
        }
        return try{
            val batch=db.batch()
            val wordsInGrp=wc.whereEqualTo("dictionaryId",groupId).get().await()
            for(doc in wordsInGrp.documents){batch.update(wc.document(doc.id),"dictionaryId",null)}
            batch.delete(gc.document(groupId))
            batch.commit().await()
            true
        }catch(e:Exception){
            Log.e("FR","Err delGrp",e)
            false}
    }
    suspend fun createGroup(name: String): Boolean {
        val gc=getUserGroupsCollection()
        if(gc==null||name.isBlank()){
            return false
        }
        return try
        {val grpId=gc.document().id
            gc.document(grpId).set(Group(id=grpId,name=name)).await()
            true
        }catch(e:Exception){Log.e("FR","Err createGrp",e)
            false
        }
    }
    suspend fun updateGroup(groupId: String, newName: String): Boolean { val gc=getUserGroupsCollection();if(gc==null||groupId.isBlank()||newName.isBlank()){return false};return try{gc.document(groupId).update("name",newName).await();true}catch(e:Exception){Log.e("FR","Err updateGrp",e);false}}
}