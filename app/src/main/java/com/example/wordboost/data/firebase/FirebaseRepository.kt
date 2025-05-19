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
import com.example.wordboost.viewmodel.SharedSetDetailsWithWords
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.flowOf

class FirebaseRepository(private val authRepository: AuthRepository) {
    private val db = Firebase.firestore
    private val TAG: String = "FirebaseRepo"

    private fun getUserWordsCollection() = authRepository.getCurrentUser()?.uid?.let { userId ->
        db.collection("users").document(userId).collection("words")
    } ?: run {
        Log.e(TAG, "getUserWordsCollection: User not logged in or ID is null!")
        null
    }

    private fun getUserGroupsCollection() = authRepository.getCurrentUser()?.uid?.let { userId ->
        db.collection("users").document(userId).collection("groups")
    } ?: run {
        Log.e(TAG, "getUserGroupsCollection: User not logged in or ID is null!")
        null
    }

    private fun getSharedSetsCollection() = db.collection("sharedCardSets")
    private fun getArticlesCollection() = db.collection("articles")
    private fun getUserArticleInteractionsCollection(userId: String) =
        db.collection("users").document(userId).collection("articleInteractions")


    suspend fun addArticle(article: Article): Result<String> {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null || currentUser.uid.isBlank()) {
            Log.e(TAG, "addArticle: User not authenticated.")
            return Result.failure(IllegalStateException("User not authenticated."))
        }
        val articleData = article.copy(
            id = "",
            userId = currentUser.uid,
            authorName = currentUser.displayName ?: article.authorName
        )

        return try {
            val documentReference = getArticlesCollection().add(articleData).await()
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding article '${articleData.title}'", e)
            Result.failure(e)
        }
    }

    suspend fun updateArticle(articleId: String, updates: Map<String, Any>): Result<Unit> {
        val currentUserUid = authRepository.getCurrentUser()?.uid
        if (currentUserUid == null) {
            Log.e(TAG, "updateArticle: User not authenticated.")
            return Result.failure(IllegalStateException("User not authenticated."))
        }
        if (articleId.isBlank()) {
            Log.e(TAG, "updateArticle: Article ID is blank.")
            return Result.failure(IllegalArgumentException("Article ID cannot be blank."))
        }

        return try {
            val finalUpdates = updates.toMutableMap()
            if (!finalUpdates.containsKey("updatedAt")) {
                finalUpdates["updatedAt"] = FieldValue.serverTimestamp()
            }
            getArticlesCollection().document(articleId).update(finalUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating article with ID: $articleId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteArticle(articleId: String): Result<Unit> {
        val currentUserUid = authRepository.getCurrentUser()?.uid
        if (currentUserUid == null) {
            Log.e(TAG, "deleteArticle: User not authenticated.")
            return Result.failure(IllegalStateException("User not authenticated for delete operation"))
        }
        if (articleId.isBlank()) {
            Log.e(TAG, "deleteArticle: Article ID is blank.")
            return Result.failure(IllegalArgumentException("Article ID cannot be blank for delete operation"))
        }

        try {
            val articleDoc = getArticlesCollection().document(articleId).get().await()
            if (!articleDoc.exists()) {
                Log.w(TAG, "deleteArticle: Article $articleId not found.")
                return Result.failure(NoSuchElementException("Article not found."))
            }
            if (articleDoc.getString("userId") != currentUserUid) {
                Log.w(TAG, "deleteArticle: User $currentUserUid is not the author of article $articleId.")
                return Result.failure(SecurityException("User is not the author of the article and cannot delete it."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking article $articleId authorship before delete", e)
            return Result.failure(e)
        }

        return try {
            val batch = db.batch()
            batch.delete(getArticlesCollection().document(articleId))

            Log.d(TAG, "Preparing to delete interaction for user $currentUserUid, article $articleId")
            batch.delete(getUserArticleInteractionsCollection(currentUserUid).document(articleId))

            batch.commit().await()
            Log.i(TAG, "Article $articleId and its interaction for user $currentUserUid deleted successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during batch delete for article $articleId", e)
            Result.failure(e)
        }
    }

    fun getArticleByIdFlow(articleId: String): Flow<Article?> {
        if (articleId.isBlank()) return emptyFlow()
        return callbackFlow {
            val listenerRegistration = getArticlesCollection().document(articleId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for article $articleId.", error)
                        trySend(null).isSuccess
                        return@addSnapshotListener
                    }
                    val article = snapshot?.toObject(Article::class.java)
                    trySend(article).isSuccess
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    fun getUserArticlesFlow(userId: String): Flow<List<Article>> {
        if (userId.isBlank()) return emptyFlow()
        return callbackFlow {
            val listenerRegistration = getArticlesCollection()
                .whereEqualTo("userId", userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user articles, userId: $userId.", error)
                        trySend(emptyList()).isSuccess
                        return@addSnapshotListener
                    }
                    val articles = snapshot?.toObjects(Article::class.java) ?: emptyList()
                    trySend(articles).isSuccess
                }
            awaitClose { listenerRegistration.remove() }
        }
    }
    fun getPublishedArticlesFlow(currentUserIdToExcludeFilterClientSide: String? = null): Flow<List<Article>> {
        val query = getArticlesCollection()
            .whereEqualTo("published", true)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50) // Додав ліміт для безпеки та продуктивності

        return callbackFlow {
            val listenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for published articles.", error)
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                var articles = snapshot?.toObjects(Article::class.java) ?: emptyList()
                if (currentUserIdToExcludeFilterClientSide != null) {
                    articles = articles.filter { it.userId != currentUserIdToExcludeFilterClientSide }
                }
                trySend(articles).isSuccess
            }
            awaitClose { listenerRegistration.remove() }
        }
    }

    fun getUserArticleInteractionFlow(userId: String, articleId: String): Flow<UserArticleInteraction?> {
        if (userId.isBlank() || articleId.isBlank()) return flowOf(null)
        return callbackFlow {
            val listenerRegistration = getUserArticleInteractionsCollection(userId).document(articleId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user article interaction, userId: $userId, articleId: $articleId.", error)
                        trySend(null).isSuccess // Або close(error) якщо хочете перервати Flow помилкою
                        return@addSnapshotListener
                    }
                    val interaction = snapshot?.toObject(UserArticleInteraction::class.java)
                    trySend(interaction).isSuccess
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    suspend fun markArticleInteraction(userId: String, articleId: String, isRead: Boolean): Result<Unit> {
        if (userId.isBlank() || articleId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID or Article ID cannot be blank."))
        }


        val interactionObject = UserArticleInteraction(
            id = articleId, // ID документа = ID статті
            userId = userId,
            articleId = articleId,
            isRead = isRead
        )

        return try {
            getUserArticleInteractionsCollection(userId).document(articleId)
                .set(interactionObject, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking article interaction for user $userId, article $articleId", e)
            Result.failure(e)
        }
    }

    suspend fun updateSharedCardSetAndWords(
        setWithUpdates: SharedCardSet,
        updatedWords: List<SharedSetWordItem>
    ): Result<Unit> {
        val currentUserUid = authRepository.getCurrentUser()?.uid
        if (currentUserUid == null) {
            Log.e(TAG, "[updateSet] User not authenticated.")
            return Result.failure(IllegalStateException("User not authenticated."))
        }
        if (setWithUpdates.id.isBlank()) {
            Log.e(TAG, "[updateSet] Set ID for update cannot be blank.")
            return Result.failure(IllegalArgumentException("Set ID for update cannot be blank."))
        }
        if (setWithUpdates.authorId != currentUserUid) {
            Log.e(TAG, "[updateSet] Attempt to change authorship or update set not owned by user. Current: $currentUserUid, SetAuthor: ${setWithUpdates.authorId}")
            return Result.failure(SecurityException("Cannot change set authorship or update set not owned by user."))
        }

        val setDocRef = getSharedSetsCollection().document(setWithUpdates.id)

        return try {
            val finalSetData = setWithUpdates.copy(
                wordCount = updatedWords.size,
                updatedAt = null // Встановлюємо null, щоб @ServerTimestamp спрацював для оновлення
            )

            val existingWordDocsSnapshot: QuerySnapshot
            try {
                existingWordDocsSnapshot = setDocRef.collection("words").get().await()
            } catch (e: Exception) {
                Log.e(TAG, "[updateSet] Error fetching existing words for deletion for set ${setWithUpdates.id}", e)
                return Result.failure(e)
            }

            db.runBatch { batch ->
                if (!existingWordDocsSnapshot.isEmpty) {
                    for (doc in existingWordDocsSnapshot.documents) {
                        batch.delete(doc.reference)
                    }
                }

                val wordsSubCollectionRef = setDocRef.collection("words")
                updatedWords.forEach { wordItem ->
                    val newWordDocRef = wordsSubCollectionRef.document()
                    val finalWordItem = if (wordItem.authorId.isBlank()) {
                        wordItem.copy(id = newWordDocRef.id, authorId = currentUserUid)
                    } else {
                        wordItem.copy(id = newWordDocRef.id)
                    }
                    batch.set(newWordDocRef, finalWordItem)
                }
                batch.set(setDocRef, finalSetData)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[updateSet] Error updating SharedCardSet with ID: ${setWithUpdates.id}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSharedCardSet(setId: String): Result<Unit> {
        val currentUserUid = authRepository.getCurrentUser()?.uid
        if (currentUserUid == null) {
            Log.e(TAG, "deleteSharedCardSet: User not authenticated.")
            return Result.failure(IllegalStateException("User not authenticated."))
        }
        if (setId.isBlank()) {
            Log.e(TAG, "deleteSharedCardSet: Set ID is blank.")
            return Result.failure(IllegalArgumentException("Set ID cannot be blank."))
        }

        return try {
            val setDocRef = getSharedSetsCollection().document(setId)

            val setToDeleteSnapshot = setDocRef.get().await()
            if (!setToDeleteSnapshot.exists()) {
                Log.w(TAG, "deleteSharedCardSet: Set with ID $setId does not exist. Nothing to delete.")
                return Result.success(Unit)
            }
            val setToDelete = setToDeleteSnapshot.toObject(SharedCardSet::class.java)
            if (setToDelete?.authorId != currentUserUid) {
                Log.w(TAG, "deleteSharedCardSet: User $currentUserUid is not the author of set $setId (author: ${setToDelete?.authorId}). This action should be denied by security rules.")
                return Result.failure(SecurityException("User is not the author of the set."))
            }

            val wordsQuerySnapshot = setDocRef.collection("words").get().await()

            db.runBatch { batch ->
                if (!wordsQuerySnapshot.isEmpty) {
                    for (wordDoc in wordsQuerySnapshot.documents) {
                        batch.delete(wordDoc.reference)
                    }
                }
                batch.delete(setDocRef)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting SharedCardSet with ID: $setId", e)
            Result.failure(e)
        }
    }

    suspend fun getMySharedSets(userId: String): Result<List<SharedCardSetSummary>> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        return try {
            val snapshot = getSharedSetsCollection()
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val summaries = snapshot.documents.mapNotNull { doc ->
                val set = doc.toObject(SharedCardSet::class.java)
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
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching my shared sets for user $userId", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProgressForSharedSet(userId: String, sharedSetId: String): UserSharedSetProgress? {
        if (userId.isBlank() || sharedSetId.isBlank()) return null
        return try {
            db.collection("users").document(userId)
                .collection("browsedSharedSetsProgress").document(sharedSetId)
                .get().await().toObject(UserSharedSetProgress::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user progress for userId: $userId, setId: $sharedSetId", e)
            null
        }
    }

    suspend fun savePersonalWordSuspend(word: Word): Boolean {
        if (word.id.isBlank()) {
            Log.e(TAG, "[savePersonalWordSuspend] Word ID is blank for word: ${word.text}")
            // return false // Розгляньте можливість повернення false, якщо ID обов'язковий
        }
        return suspendCancellableCoroutine { continuation ->
            saveWord(word) { success ->
                if (continuation.isActive) {
                    continuation.resume(success)
                }
            }
        }
    }

    suspend fun saveUserProgressForSharedSet(userId: String, sharedSetId: String, progress: UserSharedSetProgress): Boolean {
        if (userId.isBlank() || sharedSetId.isBlank()) return false
        return try {
            db.collection("users").document(userId)
                .collection("browsedSharedSetsProgress").document(sharedSetId)
                .set(progress, SetOptions.merge()) // SetOptions.merge для оновлення або створення
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user progress for userId: $userId, setId: $sharedSetId", e)
            false
        }
    }

    suspend fun getPublicSharedSets(currentUserId: String): Result<List<SharedCardSetSummary>> {
        return try {
            val query = getSharedSetsCollection()
                .whereEqualTo("public", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)

            val snapshot = query.get().await()
            val summaries = snapshot.documents.mapNotNull { doc ->
                val set = doc.toObject(SharedCardSet::class.java)
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
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching public shared sets", e)
            Result.failure(e)
        }
    }

    suspend fun getSharedCardSetWithWords(setId: String): Result<SharedSetDetailsWithWords> {
        if (setId.isBlank()) return Result.failure(IllegalArgumentException("Set ID cannot be blank."))
        return try {
            val setDocRef = getSharedSetsCollection().document(setId)
            val setSnapshot = setDocRef.get().await()
            val sharedSet = setSnapshot.toObject(SharedCardSet::class.java)

            if (sharedSet == null) {
                Log.w(TAG, "SharedCardSet with ID $setId not found.")
                return Result.failure(NoSuchElementException("SharedCardSet not found."))
            }

            val wordsSnapshot = setDocRef.collection("words")
                // .orderBy("addedAt", Query.Direction.ASCENDING) // Опціонально, якщо є поле addedAt і потрібне сортування
                .get()
                .await()

            val wordsList = wordsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(SharedSetWordItem::class.java)
            }
            Result.success(SharedSetDetailsWithWords(sharedSet, wordsList))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching shared set with words for ID $setId", e)
            Result.failure(e)
        }
    }

    suspend fun getTranslationSuspend(text: String): String? {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            return null
        }

        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null) {
            Log.w(TAG, "[getTranslationSuspend] User words collection is null, cannot get translation.")
            return null
        }

        try {
            var querySnapshot = wordsCollection
                .whereEqualTo("text", trimmedText)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val word = querySnapshot.documents.firstOrNull()?.toObject(Word::class.java)
                return word?.translation?.trim()
            }

            querySnapshot = wordsCollection
                .whereEqualTo("translation", trimmedText)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val word = querySnapshot.documents.firstOrNull()?.toObject(Word::class.java)
                return word?.text?.trim()
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "[getTranslationSuspend] Error getting translation for '$trimmedText' from Firestore", e)
            return null
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
                            Log.e(TAG, "[getTranslation_OLD] Error (alt query for '$trimmedText')", e)
                            callback(null)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "[getTranslation_OLD] Error (main query for '$trimmedText')", e)
                callback(null)
            }
    }

    suspend fun createSharedCardSetInFirestore(
        sharedSet: SharedCardSet,
        wordsToAdd: List<SharedSetWordItem>
    ): Result<String> {
        val currentUserUid = authRepository.getCurrentUser()?.uid
        if (currentUserUid == null || currentUserUid != sharedSet.authorId) {
            Log.e(TAG, "User not logged in or authorId mismatch for creating shared set.")
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
            Result.success(finalSetData.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SharedCardSet '${finalSetData.name_uk}' in Firestore", e)
            Result.failure(e)
        }
    }

    fun getWordsNeedingPracticeFlow(): Flow<List<Word>> {
        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null) {
            Log.w("FirebaseRepo_Practice", "[getWNPFlow] User collection null. Returning emptyFlow().")
            return emptyFlow()
        }
        return callbackFlow {
            val query = wordsCollection.orderBy("nextReview", Direction.ASCENDING)
            val listenerReg = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseRepo_Practice", "[getWNPFlow] Listener ERROR", e); close(e); return@addSnapshotListener
                }
                if (snapshot != null) {
                    val now = System.currentTimeMillis()
                    val words = snapshot.toObjects(Word::class.java)
                    val due = words.filter { it.nextReview <= now && it.status != "mastered" }
                    trySend(due).isSuccess
                }
            }
            awaitClose { listenerReg.remove() }
        }
    }

    fun saveWord(word: Word, callback: (Boolean) -> Unit = {}) {
        val wordsCollection = getUserWordsCollection()
        if (wordsCollection == null || word.id.isBlank()) {
            Log.e(TAG, "Cannot save word. User null or word ID blank. Word ID: '${word.id}'")
            callback(false); return
        }
        wordsCollection.document(word.id).set(word, SetOptions.merge())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e -> Log.e(TAG, "Error saving word ${word.id}", e); callback(false) }
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
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting word with ID: $wordId", exception)
                callback(false)
            }
    }


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

    fun getGroups(callback: (List<Group>) -> Unit): ListenerRegistration? {
        val gc = getUserGroupsCollection()
        if (gc == null) {
            callback(emptyList()); return null
        }
        return gc.addSnapshotListener { snap, e ->
            if (e != null) {
                Log.e(TAG, "Error groupsListen", e); callback(emptyList()); return@addSnapshotListener
            }
            callback(snap?.toObjects(Group::class.java) ?: emptyList())
        }
    }

    suspend fun deleteGroup(groupId: String): Boolean {
        val wc = getUserWordsCollection()
        val gc = getUserGroupsCollection()
        if (wc == null || gc == null || groupId.isBlank()) {
            return false
        }
        return try {
            val batch = db.batch()
            val wordsInGrp = wc.whereEqualTo("dictionaryId", groupId).get().await()
            for (doc in wordsInGrp.documents) {
                batch.update(wc.document(doc.id), "dictionaryId", null)
            }
            batch.delete(gc.document(groupId))
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleteGroup", e)
            false
        }
    }

    suspend fun createGroup(name: String): Boolean {
        val gc = getUserGroupsCollection()
        if (gc == null || name.isBlank()) {
            return false
        }
        return try {
            val grpId = gc.document().id
            gc.document(grpId).set(Group(id = grpId, name = name)).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error createGroup", e)
            false
        }
    }

    suspend fun updateGroup(groupId: String, newName: String): Boolean {
        val gc = getUserGroupsCollection()
        if (gc == null || groupId.isBlank() || newName.isBlank()) {
            return false
        }
        return try {
            gc.document(groupId).update("name", newName).await(); true
        } catch (e: Exception) {
            Log.e(TAG, "Error updateGroup", e); false
        }
    }
}