package com.example.wordboost.ui.screens.articles

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Article
import com.example.wordboost.data.util.findSentenceBoundaries
import com.example.wordboost.viewmodel.ArticleViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewArticleScreen(
    viewModel: ArticleViewModel,
    articleId: String,
    onBack: () -> Unit,
    onNavigateToTranslateScreenWithData: (originalText: String, translatedText: String, articleLanguageCode: String) -> Unit
) {
    val articleState: Article? by viewModel.currentViewingArticle.collectAsState()
    val currentArticle = articleState

    val isLoadingInitialLoad = articleState == null && articleId.isNotBlank()

    val errorMessage: String? by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val localCoroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val autoTranslation by viewModel.autoTranslatedText.collectAsState()
    val isLoadingAutoTranslation by viewModel.isLoadingAutomaticTranslation.collectAsState()

    var selectedTextContent by remember { mutableStateOf<String?>(null) }
    var showCustomActions by remember { mutableStateOf(false) }

    var articleContentTfv by remember(articleState?.content) {
        mutableStateOf(TextFieldValue(text = articleState?.content ?: ""))
    }

    LaunchedEffect(articleState?.content) {
        if (articleContentTfv.text != (articleState?.content ?: "")) {
            val currentSelection = articleContentTfv.selection
            val newText = articleState?.content ?: ""
            articleContentTfv = TextFieldValue(
                text = newText,
                selection = if (currentSelection.end <= newText.length) currentSelection else TextRange(newText.length)
            )
        }
    }

    LaunchedEffect(articleId) {
        if (articleId.isNotBlank()) {
            Log.d("ViewArticleScreen", "Loading article content for ID: $articleId")
            viewModel.loadArticleContent(articleId)
        }
    }

    BackHandler {
        Log.d("ViewArticleScreen", "BackHandler triggered")
        viewModel.clearAutomaticTranslation()
        onBack()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Log.d("ViewArticleScreen", "Showing error snackbar: $it")
            localCoroutineScope.launch {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                viewModel.clearErrorMessage()
            }
        }
    }

    LaunchedEffect(articleContentTfv.selection) {
        val selection = articleContentTfv.selection
        val currentSelectedString = if (!selection.collapsed) {
            val start = selection.min
            val end = selection.max
            if (start < end && start >= 0 && end <= articleContentTfv.text.length) {
                articleContentTfv.text.substring(start, end)
            } else { null }
        } else { null }

        selectedTextContent = currentSelectedString

        if (!currentSelectedString.isNullOrBlank() && currentArticle != null) {
            viewModel.requestAutomaticTranslation(currentSelectedString, currentArticle.languageCode)
        } else {
            viewModel.clearAutomaticTranslation()
        }
        showCustomActions = !selectedTextContent.isNullOrEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentArticle?.title ?: if (isLoadingInitialLoad) "Завантаження..." else "Стаття", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearAutomaticTranslation()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoadingInitialLoad && currentArticle == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (currentArticle != null) {
                val article = currentArticle!!
                val scrollState = rememberScrollState()

                LaunchedEffect(scrollState, article.content) {
                    snapshotFlow { scrollState.value > 0 && scrollState.value >= scrollState.maxValue - 20 }
                        .distinctUntilChanged()
                        .collect { isAtEnd ->
                            if (isAtEnd) {
                                viewModel.markArticleAsReadExplicitly(article.id)
                            }
                        }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(article.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
                    article.authorName?.takeIf { it.isNotBlank() }?.let {
                        Text("Автор: $it", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 16.dp))
                    }

                    TextField(
                        value = articleContentTfv,
                        onValueChange = { newValueTfv ->
                            articleContentTfv = newValueTfv
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = LocalContentColor.current,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Start
                        )
                    )
                    Spacer(modifier = Modifier.height(120.dp))
                }

                if (showCustomActions && selectedTextContent != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentAutoTranslation = autoTranslation
                        val currentIsLoadingAutoTranslation = isLoadingAutoTranslation

                        if (currentIsLoadingAutoTranslation && selectedTextContent!!.length <= 300 ) {
                        } else if (!currentAutoTranslation.isNullOrBlank()) {
                            if (currentAutoTranslation == "[Виділений текст занадто довгий для авто-перекладу]") {
                                Surface(shape = MaterialTheme.shapes.small, tonalElevation = 1.dp, color = MaterialTheme.colorScheme.errorContainer) {
                                    Text(
                                        text = "Текст задовгий для перекладу",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            } else {
                                Surface(shape = MaterialTheme.shapes.small, tonalElevation = 1.dp) {
                                    Text(
                                        text = currentAutoTranslation,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        FloatingAppBar(
                            isLoadingTranslation = currentIsLoadingAutoTranslation && selectedTextContent!!.length <= 300 ,
                            onSpeak = {
                                selectedTextContent?.let { textToSpeak ->
                                    if (textToSpeak.isNotBlank()) {
                                        viewModel.speakText(textToSpeak, article.languageCode)
                                    }
                                }
                            },
                            onSelectSentence = {
                                val cursorPosition = articleContentTfv.selection.start
                                val articleText = articleContentTfv.text
                                val locale = Locale(article.languageCode)

                                val sentenceRange = findSentenceBoundaries(articleText, cursorPosition, locale)

                                sentenceRange?.let { range ->
                                    val sentenceText = articleText.substring(range.start, range.end)
                                    articleContentTfv = articleContentTfv.copy(selection = range)
                                } ?: localCoroutineScope.launch {
                                    snackbarHostState.showSnackbar("Не вдалося визначити речення.", duration = SnackbarDuration.Short)
                                }
                            },
                            onCopy = {
                                selectedTextContent?.let { textToCopy ->
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                    localCoroutineScope.launch {
                                        snackbarHostState.showSnackbar("Текст скопійовано!", duration = SnackbarDuration.Short)
                                    }
                                }
                            },
                            onAddToDictionary = {
                                selectedTextContent?.let { original ->
                                    if (!currentAutoTranslation.isNullOrBlank() && currentAutoTranslation != "[Виділений текст занадто довгий для авто-перекладу]") {
                                        onNavigateToTranslateScreenWithData(original, currentAutoTranslation, article.languageCode)
                                    } else {
                                        localCoroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (currentAutoTranslation == "[Виділений текст занадто довгий для авто-перекладу]") "Текст задовгий, неможливо додати."
                                                else "Переклад недоступний для додавання.",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

            } else if (!isLoadingInitialLoad && articleId.isNotBlank()) {
                Text(
                    "Не вдалося завантажити статтю або її не існує.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

        }
    }
}

@Composable
fun FloatingAppBar(
    modifier: Modifier = Modifier,
    onSpeak: () -> Unit,
    onSelectSentence: () -> Unit,
    onCopy: () -> Unit,
    onAddToDictionary: () -> Unit,
    isLoadingTranslation: Boolean
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 4.dp,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = onSpeak, modifier = Modifier.size(38.dp)) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Озвучити")
            }
            IconButton(onClick = onSelectSentence, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.FormatAlignLeft, contentDescription = "Виділити речення")
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Копіювати")
            }
            Box(modifier = Modifier.size(38.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                if (isLoadingTranslation) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onAddToDictionary, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.LibraryAdd, contentDescription = "Додати в словник")
                    }
                }
            }
        }
    }
}