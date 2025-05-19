package com.example.wordboost.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.model.Article
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.model.UserArticleInteraction // Додай, якщо ще не додав
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.data.tts.TextToSpeechService
import com.google.firebase.firestore.FieldValue // Важливий імпорт
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ArticleUiModel(
    val article: Article,
    val isRead: Boolean,
    val isCurrentUserOwner: Boolean
)

enum class SaveArticleStatus {
    Idle,    // Немає операції
    Saving,  // Іде збереження
    Success, // Успішно збережено
    Error    // Сталася помилка
}

class ArticleViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val translationRepository: TranslationRepository,
    private val ttsService: TextToSpeechService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserIdInternal: String?
        get() = authRepository.getCurrentUser()?.uid

    private val _autoTranslatedText = MutableStateFlow<String?>(null)
    val autoTranslatedText: StateFlow<String?> = _autoTranslatedText.asStateFlow()

    private val _isLoadingAutomaticTranslation = MutableStateFlow(false)
    val isLoadingAutomaticTranslation: StateFlow<Boolean> = _isLoadingAutomaticTranslation.asStateFlow()

    private var autoTranslationJob: Job? = null
    // --- StateFlows для UI ---
    private val _userArticles = MutableStateFlow<List<ArticleUiModel>>(emptyList())
    val userArticles: StateFlow<List<ArticleUiModel>> = _userArticles.asStateFlow()

    private val _publishedArticles = MutableStateFlow<List<ArticleUiModel>>(emptyList())
    val publishedArticles: StateFlow<List<ArticleUiModel>> = _publishedArticles.asStateFlow()

    private val _isLoadingUserArticles = MutableStateFlow(false)
    val isLoadingUserArticles: StateFlow<Boolean> = _isLoadingUserArticles.asStateFlow()

    private val _isLoadingPublishedArticles = MutableStateFlow(false)
    val isLoadingPublishedArticles: StateFlow<Boolean> = _isLoadingPublishedArticles.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentViewingArticle = MutableStateFlow<Article?>(null)
    val currentViewingArticle: StateFlow<Article?> = _currentViewingArticle.asStateFlow()

    private val _translatedText = MutableStateFlow<String?>(null)
    val translatedText: StateFlow<String?> = _translatedText.asStateFlow()

    private val _isTranslationDialogVisible = MutableStateFlow(false)
    val isTranslationDialogVisible: StateFlow<Boolean> = _isTranslationDialogVisible.asStateFlow()

    private val _isUserArticlesExpanded = MutableStateFlow(true)
    val isUserArticlesExpanded: StateFlow<Boolean> = _isUserArticlesExpanded.asStateFlow()

    private val _isPublishedArticlesExpanded = MutableStateFlow(true)
    val isPublishedArticlesExpanded: StateFlow<Boolean> = _isPublishedArticlesExpanded.asStateFlow()

    // --- Для діалогу видалення ---
    private val _showDeleteArticleConfirmDialog = MutableStateFlow(false)
    val showDeleteArticleConfirmDialog: StateFlow<Boolean> =
        _showDeleteArticleConfirmDialog.asStateFlow()

    private var articleIdToDelete: String? = null
    private var articleTitleToDelete: String? = null
    private val _isLoadingCurrentArticle = MutableStateFlow(false)
    val isLoadingCurrentArticle: StateFlow<Boolean> = _isLoadingCurrentArticle.asStateFlow()
    // --- Для статусу збереження статті ---
    private val _saveArticleStatus = MutableStateFlow<SaveArticleStatus>(SaveArticleStatus.Idle)
    val saveArticleStatus: StateFlow<SaveArticleStatus> = _saveArticleStatus.asStateFlow()

    init {
        Log.d("ArticleViewModel", "ViewModel initialized. CurrentUser ID: $currentUserIdInternal")
        if (currentUserIdInternal != null) {
            loadUserArticles()
            loadPublishedArticles()
        } else {
            Log.w(
                "ArticleViewModel",
                "User not logged in at ViewModel init. Articles will not be loaded."
            )
            _userArticles.value = emptyList()
            _publishedArticles.value = emptyList()
        }
    }
    fun requestAutomaticTranslation(text: String, articleLanguageCode: String) {
        autoTranslationJob?.cancel() // Скасовуємо попередній запит, якщо він ще виконується

        if (text.isBlank()) {
            _autoTranslatedText.value = null
            _isLoadingAutomaticTranslation.value = false
            return
        }

        val MAX_CHARS_FOR_AUTO_TRANSLATE = 300
        if (text.length > MAX_CHARS_FOR_AUTO_TRANSLATE) {
            Log.w("ArticleVM", "Текст '$text' занадто довгий для автоматичного перекладу (${text.length} символів).")
            _autoTranslatedText.value = "[Виділений текст занадто довгий для авто-перекладу]" // Або null і показати Snackbar
            _isLoadingAutomaticTranslation.value = false
            return
        }

        val targetLanguage = if (articleLanguageCode.startsWith("en", ignoreCase = true)) "uk" else "en"

        autoTranslationJob = viewModelScope.launch { // Або інший scope, якщо потрібно
            delay(750L)
            _isLoadingAutomaticTranslation.value = true
            _autoTranslatedText.value = null // Скидаємо попередній результат на час завантаження
            Log.d("ArticleVM", "Автоматичний переклад для: '$text' -> $targetLanguage")
            try {
                val translation = translationRepository.translateForSetCreationSuspend(text, targetLanguage)
                _autoTranslatedText.value = translation
                Log.d("ArticleVM", "Результат авто-перекладу: '$translation'")
            } catch (e: Exception) {
                Log.e("ArticleVM", "Помилка автоматичного перекладу", e)
                _autoTranslatedText.value = null // Або спеціальне значення для помилки
                // _errorMessage.value = "Помилка автоматичного перекладу." // Може бути занадто нав'язливо
            } finally {
                _isLoadingAutomaticTranslation.value = false
            }
        }
    }

    fun clearAutomaticTranslation() {
        autoTranslationJob?.cancel()
        _autoTranslatedText.value = null
        _isLoadingAutomaticTranslation.value = false
    }
    fun loadUserArticles() {
        currentUserIdInternal?.let { userId ->
            viewModelScope.launch {
                _isLoadingUserArticles.value = true
                Log.d("ArticleViewModel", "Loading user articles for userId: $userId")
                firebaseRepository.getUserArticlesFlow(userId)
                    .flatMapLatest { articles ->
                        if (articles.isEmpty()) {
                            Log.d("ArticleViewModel", "No user articles found for userId: $userId")
                            flowOf(emptyList<ArticleUiModel>())
                        } else {
                            Log.d(
                                "ArticleViewModel",
                                "Found ${articles.size} user articles, fetching interactions for userId: $userId"
                            )
                            val uiModelsFlows: List<Flow<ArticleUiModel>> =
                                articles.map { article ->
                                    firebaseRepository.getUserArticleInteractionFlow(
                                        userId,
                                        article.id
                                    )
                                        .map { interaction ->
                                            ArticleUiModel(
                                                article = article,
                                                isRead = interaction?.isRead ?: false,
                                                isCurrentUserOwner = (article.userId == userId)
                                            )
                                        }
                                }
                            combine(uiModelsFlows) { it.toList() }
                        }
                    }
                    .catch { e ->
                        Log.e(
                            "ArticleViewModel",
                            "Error loading user articles for userId: $userId",
                            e
                        )
                        _errorMessage.value = "Помилка завантаження ваших статей: ${e.message}"
                        _userArticles.value = emptyList()
                        _isLoadingUserArticles.value = false
                    }
                    .collect { articleUiModels ->
                        _userArticles.value = articleUiModels
                        _isLoadingUserArticles.value = false
                        Log.d(
                            "ArticleViewModel",
                            "User articles updated for userId: $userId. Count: ${articleUiModels.size}"
                        )
                    }
            }
        } ?: run {
            Log.w(
                "ArticleViewModel",
                "loadUserArticles: currentUserId is null. Clearing user articles."
            )
            _userArticles.value = emptyList()
            _isLoadingUserArticles.value = false
        }
    }

    fun loadPublishedArticles() {
        val localCurrentUserId = currentUserIdInternal
        viewModelScope.launch {
            _isLoadingPublishedArticles.value = true
            Log.d(
                "ArticleViewModel",
                "Loading published articles. Current userId for interactions: $localCurrentUserId"
            )
            firebaseRepository.getPublishedArticlesFlow(null)
                .flatMapLatest { articles ->
                    val articlesNotOwnedByUser = articles.filter { it.userId != localCurrentUserId }
                    if (articlesNotOwnedByUser.isEmpty()) {
                        Log.d(
                            "ArticleViewModel",
                            "No published articles found (not owned by current user)."
                        )
                        flowOf(emptyList<ArticleUiModel>())
                    } else {
                        Log.d(
                            "ArticleViewModel",
                            "Found ${articlesNotOwnedByUser.size} published articles (not owned by user), fetching interactions."
                        )
                        val uiModelsFlows: List<Flow<ArticleUiModel>> =
                            articlesNotOwnedByUser.map { article ->
                                val isOwner =
                                    article.userId == localCurrentUserId // Завжди false для цього відфільтрованого списку
                                if (localCurrentUserId != null) {
                                    firebaseRepository.getUserArticleInteractionFlow(
                                        localCurrentUserId,
                                        article.id
                                    )
                                        .map { interaction ->
                                            ArticleUiModel(
                                                article = article,
                                                isRead = interaction?.isRead ?: false,
                                                isCurrentUserOwner = isOwner
                                            )
                                        }
                                } else {
                                    flowOf(
                                        ArticleUiModel(
                                            article,
                                            isRead = false,
                                            isCurrentUserOwner = false
                                        )
                                    )
                                }
                            }
                        combine(uiModelsFlows) { it.toList() }
                    }
                }
                .catch { e ->
                    Log.e("ArticleViewModel", "Error loading published articles", e)
                    _errorMessage.value = "Помилка завантаження публічних статей: ${e.message}"
                    _publishedArticles.value = emptyList()
                    _isLoadingPublishedArticles.value = false
                }
                .collect { articleUiModels ->
                    _publishedArticles.value = articleUiModels
                    _isLoadingPublishedArticles.value = false
                    Log.d(
                        "ArticleViewModel",
                        "Published articles updated. Count: ${articleUiModels.size}"
                    )
                }
        }
    }

    fun loadArticleContent(articleId: String) {
        viewModelScope.launch {
            _isLoadingCurrentArticle.value = true // Починаємо завантаження
            Log.d("ArticleViewModel", "Loading content for articleId: $articleId")
            firebaseRepository.getArticleByIdFlow(articleId)
                .catch { e ->
                    _errorMessage.value = "Помилка завантаження статті: ${e.message}"
                    Log.e("ArticleViewModel", "Error loading article $articleId content", e)
                    _isLoadingCurrentArticle.value = false
                }
                .collect { article ->
                    _currentViewingArticle.value = article
                    _isLoadingCurrentArticle.value = false // Завершуємо завантаження
                    if (article != null) {
                        Log.d("ArticleViewModel", "Article $articleId content loaded: ${article.title}")

                    } else {
                        _errorMessage.value = "Не вдалося завантажити статтю."
                        Log.w("ArticleViewModel", "Article $articleId content is null after loading.")
                    }
                }
        }
    }

    fun createOrUpdateArticle(
        id: String? = null,
        title: String,
        content: String,
        published: Boolean
    ) {
        val localCurrentUserId = currentUserIdInternal
        val userDisplayName = authRepository.getCurrentUser()?.displayName

        if (localCurrentUserId == null) {
            _errorMessage.value = "Будь ласка, увійдіть, щоб зберегти статтю."
            Log.w("ArticleViewModel", "User not logged in. Cannot save article.")
            _saveArticleStatus.value = SaveArticleStatus.Error
            return
        }
        if (title.isBlank() || content.isBlank()) {
            _errorMessage.value = "Заголовок та текст статті не можуть бути порожніми."
            Log.w("ArticleViewModel", "Title or content is blank.")
            _saveArticleStatus.value = SaveArticleStatus.Error
            return
        }


        _saveArticleStatus.value = SaveArticleStatus.Saving
        viewModelScope.launch {
            val articleData = Article(
                id = id ?: "",
                userId = localCurrentUserId,
                authorName = userDisplayName,
                title = title.trim(),
                content = content.trim(),
                languageCode = "en",
                createdAt = if (id == null) null else _currentViewingArticle.value?.createdAt,
                updatedAt = null,
                published = published
            )

            val result: Result<String?> = if (id == null) { // Створення нової статті
                firebaseRepository.addArticle(articleData) // Повертає Result<String> з ID нової статті
            } else { // Оновлення існуючої
                val updates = mutableMapOf<String, Any>(
                    "title" to articleData.title,
                    "content" to articleData.content,
                    "published" to articleData.published,
                    "languageCode" to articleData.languageCode,
                    "updatedAt" to FieldValue.serverTimestamp() // Завжди оновлюємо час
                )
                firebaseRepository.updateArticle(id, updates)
                    .map { id } // Перетворюємо Result<Unit> на Result<String?>
            }

            result.onSuccess { newOrUpdatedId -> // newOrUpdatedId може бути null при оновленні, якщо updateArticle не повертає ID
                Log.i("ArticleViewModel", "Article save success. ID: ${newOrUpdatedId ?: id}")
                _saveArticleStatus.value = SaveArticleStatus.Success
                loadUserArticles()
                if (published || (id != null && _publishedArticles.value.any { it.article.id == id })) {
                    loadPublishedArticles()
                }
            }.onFailure { e ->
                Log.e("ArticleViewModel", "Error saving article (id: $id)", e)
                _errorMessage.value = "Помилка збереження статті: ${e.message}"
                _saveArticleStatus.value = SaveArticleStatus.Error
            }
        }
    }

    fun resetSaveArticleStatus() {
        _saveArticleStatus.value = SaveArticleStatus.Idle
    }

    fun clearCurrentViewingArticle() {
        _currentViewingArticle.value = null
        Log.d("ArticleViewModel", "Cleared current viewing article.")
    }


    fun deleteArticle(articleId: String) {
        viewModelScope.launch {
            Log.d("ArticleViewModel", "Attempting to delete article: $articleId")
            firebaseRepository.deleteArticle(articleId)
                .onSuccess {
                    Log.i("ArticleViewModel", "Article $articleId deleted successfully.")
                    if (_currentViewingArticle.value?.id == articleId) {
                        _currentViewingArticle.value = null
                    }
                    // Списки оновляться через Firestore listeners, але для миттєвого ефекту можна:
                    loadUserArticles()
                    loadPublishedArticles()
                }
                .onFailure { e ->
                    Log.e("ArticleViewModel", "Error deleting article $articleId", e)
                    _errorMessage.value = "Помилка видалення статті: ${e.message}"
                }
        }
    }

    fun requestDeleteArticle(articleId: String, articleTitle: String) {
        this.articleIdToDelete = articleId
        this.articleTitleToDelete = articleTitle
        _showDeleteArticleConfirmDialog.value = true
        Log.d(
            "ArticleViewModel",
            "Requested delete for articleId: $articleId, title: $articleTitle"
        )
    }

    fun confirmDeleteArticle() {
        Log.d("ArticleViewModel", "Confirming delete for articleId: $articleIdToDelete")
        articleIdToDelete?.let {
            deleteArticle(it)
        }
        _showDeleteArticleConfirmDialog.value = false
        articleIdToDelete = null
        articleTitleToDelete = null
    }

    fun cancelDeleteArticle() {
        Log.d("ArticleViewModel", "Cancelled delete for articleId: $articleIdToDelete")
        _showDeleteArticleConfirmDialog.value = false
        articleIdToDelete = null
        articleTitleToDelete = null
    }

    fun getArticleTitleToDelete(): String? {
        return articleTitleToDelete
    }

    fun translateText(text: String, targetLanguage: String = "uk") {
        if (text.isBlank()) {
            _translatedText.value = ""
            _isTranslationDialogVisible.value = false
            return
        }
        viewModelScope.launch {
            Log.d("ArticleViewModel", "Translating text (using forSetCreation): \"$text\" to $targetLanguage")
            val translation =
                translationRepository.translateForSetCreationSuspend(text, targetLanguage)
            _translatedText.value = translation
            _isTranslationDialogVisible.value = !translation.isNullOrBlank()
            Log.d("ArticleViewModel", "Translation result: \"$translation\"")
        }
    }

    fun dismissTranslationDialog() {
        _isTranslationDialogVisible.value = false
    }

    fun speakText(text: String, languageCode: String = "en") {
        Log.d("ArticleViewModel", "Speaking text: \"$text\" in language: $languageCode")
        if (languageCode == "en") {
            ttsService.speak(text)
        } else {
            _errorMessage.value = "Озвучування для мови '$languageCode' поки не підтримується."
            Log.w("ArticleViewModel", "TTS for language '$languageCode' not supported yet.")
        }
    }
    suspend fun getTranslationForSelectedText(text: String, targetLanguage: String): String? {
        if (text.isBlank()) return null
        return translationRepository.translateForSetCreationSuspend(text, targetLanguage)
    }
    fun markArticleAsReadExplicitly(articleId: String?) {
        if (articleId == null) return
        currentUserIdInternal?.let { userId ->
            viewModelScope.launch {
                // Перевіримо, чи стаття вже не позначена як прочитана, щоб уникнути зайвих записів
                // і повторного оновлення UI, якщо статус не змінився.
                val isAlreadyRead = _userArticles.value.find { it.article.id == articleId }?.isRead == true ||
                        _publishedArticles.value.find { it.article.id == articleId }?.isRead == true ||
                        // Якщо стаття відкрита вперше і ще немає в списках, вона точно не прочитана
                        (_currentViewingArticle.value?.id == articleId &&
                                !(_userArticles.value.any{it.article.id == articleId} || _publishedArticles.value.any{it.article.id == articleId}))


                // Якщо стаття не була позначена як прочитана або це перше відкриття
                // (в цьому випадку isAlreadyRead може бути неточним до першого завантаження списків,
                // тому краще покладатися на те, що markArticleInteraction сама впорається з merge)
                Log.d("ArticleViewModel", "Explicitly marking article $articleId as read for userId: $userId. Was already read: $isAlreadyRead")

                firebaseRepository.markArticleInteraction(userId, articleId, isRead = true)
                    .onSuccess {
                        Log.i("ArticleViewModel", "Article $articleId EXPLICITLY marked as read successfully for user $userId.")
                        // Оновлюємо локальні списки для негайного відображення зміни статусу
                        _userArticles.update { list ->
                            list.map { if (it.article.id == articleId) it.copy(isRead = true) else it }
                        }
                        _publishedArticles.update { list ->
                            list.map { if (it.article.id == articleId) it.copy(isRead = true) else it }
                        }
                    }
                    .onFailure { e ->
                        _errorMessage.value = "Не вдалося оновити статус прочитання: ${e.message}"
                        Log.e("ArticleViewModel", "Error explicitly marking article $articleId as read for user $userId", e)
                    }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun toggleUserArticlesExpanded() {
        _isUserArticlesExpanded.value = !_isUserArticlesExpanded.value
    }

    fun togglePublishedArticlesExpanded() {
        _isPublishedArticlesExpanded.value = !_isPublishedArticlesExpanded.value
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ArticleViewModel", "ViewModel cleared.")
    }
}