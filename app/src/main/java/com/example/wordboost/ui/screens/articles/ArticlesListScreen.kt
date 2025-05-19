package com.example.wordboost.ui.screens.articles

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.* // Потрібен для getValue, remember, etc.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.viewmodel.ArticleUiModel // Імпорт твоєї моделі
import com.example.wordboost.ui.components.ArticleListItem
import com.example.wordboost.ui.screens.SectionHeader
import com.example.wordboost.viewmodel.ArticleViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesListScreen(
    viewModel: ArticleViewModel,
    onViewArticle: (articleId: String) -> Unit,
    onCreateArticle: () -> Unit,
    onEditArticle: (articleId: String) -> Unit
) {
    val userArticles: List<ArticleUiModel> by viewModel.userArticles.collectAsState()
    val publishedArticles: List<ArticleUiModel> by viewModel.publishedArticles.collectAsState()
    val isLoadingUserArticles: Boolean by viewModel.isLoadingUserArticles.collectAsState()
    val isLoadingPublishedArticles: Boolean by viewModel.isLoadingPublishedArticles.collectAsState()
    val errorMessage: String? by viewModel.errorMessage.collectAsState()
    val showDeleteDialog: Boolean by viewModel.showDeleteArticleConfirmDialog.collectAsState()
    val isUserArticlesExpanded: Boolean by viewModel.isUserArticlesExpanded.collectAsState()
    val isPublishedArticlesExpanded: Boolean by viewModel.isPublishedArticlesExpanded.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { // Завантажуємо дані при першому вході на екран, якщо потрібно
        Log.d("ArticlesListScreen", "LaunchedEffect: Initial data load if needed.")
        // viewModel.loadUserArticles() // ViewModel вже робить це в init, якщо користувач залогінений
        // viewModel.loadPublishedArticles()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Log.d("ArticlesListScreen", "Showing error snackbar: $it")
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long) // Long для кращої видимості
                viewModel.clearErrorMessage()
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteArticle() },
            title = { Text("Підтвердити видалення") },
            text = { Text("Ви впевнені, що хочете видалити статтю '${viewModel.getArticleTitleToDelete() ?: "цю статтю"}'? Цю дію неможливо буде скасувати.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteArticle() }) {
                    Text("Видалити", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteArticle() }) {
                    Text("Скасувати")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateArticle) {
                Icon(Icons.Filled.Add, contentDescription = "Створити статтю")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(all = 16.dp), // Змінив на all для симетрії
            verticalArrangement = Arrangement.spacedBy(12.dp) // Збільшив трохи простір
        ) {
            // Секція "Мої статті"
            item {
                SectionHeader(
                    title = "Мої статті",
                    isExpanded = isUserArticlesExpanded,
                    isLoading = isLoadingUserArticles && userArticles.isEmpty(),
                    onToggleExpand = { viewModel.toggleUserArticlesExpanded() }
                )
            }
            item {
                AnimatedVisibility(visible = isUserArticlesExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isLoadingUserArticles && userArticles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (userArticles.isNotEmpty()) {
                            userArticles.forEach { articleUiModel ->
                                ArticleListItem(
                                    articleUiModel = articleUiModel,
                                    isMyArticle = true,
                                    onViewClick = { onViewArticle(articleUiModel.article.id) },
                                    onEditClick = { onEditArticle(articleUiModel.article.id) },
                                    onDeleteClick = { viewModel.requestDeleteArticle(articleUiModel.article.id, articleUiModel.article.title) }
                                )
                            }
                        } else if (!isLoadingUserArticles) {
                            Text(
                                "У вас ще немає створених статей.",
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                SectionHeader(
                    title = "Публічні статті",
                    isExpanded = isPublishedArticlesExpanded,
                    isLoading = isLoadingPublishedArticles && publishedArticles.isEmpty(),
                    onToggleExpand = { viewModel.togglePublishedArticlesExpanded() }
                )
            }
            item {
                AnimatedVisibility(visible = isPublishedArticlesExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isLoadingPublishedArticles && publishedArticles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (publishedArticles.isNotEmpty()) {
                            publishedArticles.forEach { articleUiModel ->
                                ArticleListItem(
                                    articleUiModel = articleUiModel,
                                    isMyArticle = articleUiModel.isCurrentUserOwner, // Використовуємо поле з UiModel
                                    onViewClick = { onViewArticle(articleUiModel.article.id) },
                                    onEditClick = if (articleUiModel.isCurrentUserOwner) {
                                        { onEditArticle(articleUiModel.article.id) }
                                    } else null,
                                    onDeleteClick = if (articleUiModel.isCurrentUserOwner) {
                                        { viewModel.requestDeleteArticle(articleUiModel.article.id, articleUiModel.article.title) }
                                    } else null
                                )
                            }
                        } else if (!isLoadingPublishedArticles) {
                            Text(
                                "Публічних статей поки що немає.",
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}