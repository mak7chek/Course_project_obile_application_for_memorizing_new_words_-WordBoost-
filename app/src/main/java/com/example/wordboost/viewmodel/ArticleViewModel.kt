package com.example.wordboost.viewmodel // Або твій пакет для ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.model.Article // Твої моделі
import com.example.wordboost.data.model.ArticleUiModel
import com.example.wordboost.data.repository.ArticleRepository // Твій репозиторій
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.data.tts.TextToSpeechService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class ArticleViewModel(
    private val articleRepository: ArticleRepository,
    private val translationRepository: TranslationRepository, // Твій репозиторій перекладів
    private val ttsService: TextToSpeechService, // Твій TTS сервіс
    private val firebaseAuth: FirebaseAuth // Для отримання ID поточного користувача
) : ViewModel() {

    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    // --- StateFlows для списків статей ---
    private val _userArticles = MutableStateFlow<List<ArticleUiModel>>(emptyList())
    val userArticles: StateFlow<List<ArticleUiModel>> = _userArticles.asStateFlow()

    private val _publishedArticles = MutableStateFlow<List<ArticleUiModel>>(emptyList())
    val publishedArticles: StateFlow<List<ArticleUiModel>> = _publishedArticles.asStateFlow()

    // --- Стани завантаження та помилок ---
    private val _isLoadingUserArticles = MutableStateFlow(false)
    val isLoadingUserArticles: StateFlow<Boolean> = _isLoadingUserArticles.asStateFlow()

    private val _isLoadingPublishedArticles = MutableStateFlow(false)
    val isLoadingPublishedArticles: StateFlow<Boolean> = _isLoadingPublishedArticles.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Стан для поточного перегляду статті ---
    private val _currentViewingArticle = MutableStateFlow<Article?>(null)
    val currentViewingArticle: StateFlow<Article?> = _currentViewingArticle.asStateFlow()

    // --- Стан для перекладу ---
    private val _translatedText = MutableStateFlow<String?>(null)
    val translatedText: StateFlow<String?> = _translatedText.asStateFlow()

    private val _isTranslationDialogVisible = MutableStateFlow(false)
    val isTranslationDialogVisible: StateFlow<Boolean> = _isTranslationDialogVisible.asStateFlow()

    // --- Стани розгорнутих секцій (як у SetsViewModel) ---
    private val _isUserArticlesExpanded = MutableStateFlow(true)
    val isUserArticlesExpanded: StateFlow<Boolean> = _isUserArticlesExpanded.asStateFlow()

    private val _isPublishedArticlesExpanded = MutableStateFlow(true)
    val isPublishedArticlesExpanded: StateFlow<Boolean> = _isPublishedArticlesExpanded.asStateFlow()


    init {
        loadUserArticles()
        loadPublishedArticles()
    }

    fun loadUserArticles() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _isLoadingUserArticles.value = true
                articleRepository.getUserArticlesWithReadStatus(userId)
                    .catch { e ->
                        _errorMessage.value = "Помилка завантаження ваших статей: ${e.message}"
                        _isLoadingUserArticles.value = false
                    }
                    .collect { articles ->
                        _userArticles.value = articles
                        _isLoadingUserArticles.value = false
                    }
            }
        } ?: run {
            _userArticles.value = emptyList() // Якщо користувач не залогінений
            _isLoadingUserArticles.value = false
        }
    }

    fun loadPublishedArticles() {
        currentUserId?.let { userId -> // Потрібен userId для визначення статусу прочитання
            viewModelScope.launch {
                _isLoadingPublishedArticles.value = true
                articleRepository.getPublishedArticlesWithReadStatus(userId)
                    .catch { e ->
                        _errorMessage.value = "Помилка завантаження публічних статей: ${e.message}"
                        _isLoadingPublishedArticles.value = false
                    }
                    .collect { articles ->
                        _publishedArticles.value = articles
                        _isLoadingPublishedArticles.value = false
                    }
            }
        } ?: run { // Якщо користувач не залогінений, можна завантажити без статусу прочитання або не завантажувати
            _publishedArticles.value = emptyList()
            _isLoadingPublishedArticles.value = false
        }
    }

    fun loadArticleContent(articleId: String) {
        viewModelScope.launch {
            // Опціонально: показати індикатор завантаження контенту статті
            articleRepository.getArticleById(articleId)
                .catch { e -> _errorMessage.value = "Помилка завантаження статті: ${e.message}" }
                .collect { article ->
                    _currentViewingArticle.value = article
                    // Позначити статтю як прочитану, якщо вона завантажена успішно
                    if (article != null) {
                        currentUserId?.let { userId ->
                            articleRepository.markArticleAsRead(userId, articleId)
                                .onFailure { e -> _errorMessage.value = "Не вдалося позначити статтю як прочитану: ${e.message}" }
                        }
                    }
                }
        }
    }

    fun createOrUpdateArticle(
        id: String? = null, // null для створення нової статті
        title: String,
        content: String,
        isPublished: Boolean
        // languageCode можна поки що жорстко задати "en"
    ) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                val article = Article(
                    id = id ?: "", // Firebase згенерує ID, якщо id порожній при створенні
                    userId = userId,
                    authorName = firebaseAuth.currentUser?.displayName, // Або інше ім'я автора
                    title = title.trim(),
                    content = content.trim(),
                    languageCode = "en", // Поки що англійська
                    createdAt = if (id == null) Date() else null, // Встановлюємо тільки при створенні, Firebase оновить через @ServerTimestamp
                    updatedAt = Date(), // Firebase оновить через @ServerTimestamp
                    isPublished = isPublished
                )

                val result = if (id == null) {
                    articleRepository.addArticle(article.copy(createdAt = null, updatedAt = null)) // Firebase встановить timestamps
                } else {
                    // При оновленні, ми передаємо тільки ті поля, які змінилися, або весь об'єкт.
                    // createdAt не оновлюємо. updatedAt оновиться автоматично.
                    val updates = mapOf(
                        "title" to article.title,
                        "content" to article.content,
                        "isPublished" to article.isPublished,
                        "updatedAt" to FieldValue.serverTimestamp() // Явно просимо оновити час
                    )
                    articleRepository.updateArticle(id, updates).map { id } // map Result<Unit> to Result<String>
                }

                result.onSuccess { articleId ->
                    // Можна оновити списки або показати повідомлення
                    loadUserArticles() // Оновити список моїх статей
                    if (isPublished) loadPublishedArticles() // Якщо опубліковано, оновити і публічні
                }.onFailure { e ->
                    _errorMessage.value = "Помилка збереження статті: ${e.message}"
                }
            }
        } ?: run {
            _errorMessage.value = "Будь ласка, увійдіть, щоб зберегти статтю."
        }
    }


    fun deleteArticle(articleId: String) {
        viewModelScope.launch {
            articleRepository.deleteArticle(articleId)
                .onSuccess {
                    // Оновити списки, прибрати _currentViewingArticle якщо це видалена стаття
                    if (_currentViewingArticle.value?.id == articleId) {
                        _currentViewingArticle.value = null
                    }
                    loadUserArticles()
                    loadPublishedArticles()
                }
                .onFailure { e ->
                    _errorMessage.value = "Помилка видалення статті: ${e.message}"
                }
        }
    }

    fun translateText(text: String, targetLanguage: String = "uk") {
        if (text.isBlank()) {
            _translatedText.value = ""
            _isTranslationDialogVisible.value = false
            return
        }
        viewModelScope.launch {
            // Тут можна додати індикатор завантаження перекладу
            val translation = translationRepository.translateForUserVocabularySuspend(text, targetLanguage)
            _translatedText.value = translation
            _isTranslationDialogVisible.value = translation != null // Показати діалог, якщо є переклад
        }
    }

    fun dismissTranslationDialog() {
        _isTranslationDialogVisible.value = false
        _translatedText.value = null
    }

    fun speakText(text: String, languageCode: String = "en") { // languageCode для оригіналу
        // Поки що твій TTS сервіс жорстко налаштований на Locale.ENGLISH
        // Якщо буде потрібно динамічно, треба буде модифікувати TTS сервіс
        if (languageCode == "en") {
            ttsService.speak(text)
        } else {
            // Логіка для інших мов, якщо TTS сервіс це підтримує
            _errorMessage.value = "Озвучування для мови '$languageCode' поки не підтримується."
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
        // ttsService.shutdown() // Якщо TTS сервіс має бути живим протягом всього додатку, то shutdown() викликати в Application.onTerminate() або коли життєвий цикл додатку завершується. Якщо він специфічний для ViewModel, то тут.
    }
}

// --- Factory для ArticleViewModel ---
class ArticleViewModelFactory(
    private val articleRepository: ArticleRepository,
    private val translationRepository: TranslationRepository,
    private val ttsService: TextToSpeechService,
    private val firebaseAuth: FirebaseAuth
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArticleViewModel(articleRepository, translationRepository, ttsService, firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}