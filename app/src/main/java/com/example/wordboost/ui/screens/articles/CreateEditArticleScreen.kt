package com.example.wordboost.ui.screens.articles

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordboost.viewmodel.ArticleViewModel
import com.example.wordboost.viewmodel.SaveArticleStatus


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditArticleScreen(
    viewModel: ArticleViewModel,
    editingArticleId: String?,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val isEditing = editingArticleId != null
    val screenTitle = if (isEditing) "Редагувати статтю" else "Створити статтю"

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var published by remember { mutableStateOf(false) }

    val articleToEdit by viewModel.currentViewingArticle.collectAsState()
    var initialDataLoaded by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(isEditing) }

    val saveStatus by viewModel.saveArticleStatus.collectAsState()
    val isSaving = saveStatus == SaveArticleStatus.Saving

    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler { onBack() }

    LaunchedEffect(editingArticleId) {
        if (isEditing && editingArticleId != null) {
            initialDataLoaded = false
            isLoadingData = true
            Log.d("CreateEditArticle", "Loading article for editing: $editingArticleId")
            viewModel.loadArticleContent(editingArticleId)
        } else {
            title = ""
            content = ""
            published = false
            isLoadingData = false
            initialDataLoaded = true
            viewModel.clearCurrentViewingArticle()
            Log.d("CreateEditArticle", "Mode: Create New Article. Fields reset.")
        }
    }

    LaunchedEffect(articleToEdit, editingArticleId, isEditing) {
        if (isEditing && articleToEdit != null && articleToEdit?.id == editingArticleId && !initialDataLoaded) {
            title = articleToEdit!!.title
            content = articleToEdit!!.content
            published = articleToEdit!!.published
            isLoadingData = false
            initialDataLoaded = true
            Log.d("CreateEditArticle", "Article data populated: ${articleToEdit!!.title}")
        } else if (!isEditing && !initialDataLoaded) {
            isLoadingData = false
            initialDataLoaded = true
        }
    }

    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            SaveArticleStatus.Success -> {
                Log.d("CreateEditArticle", "Save successful, calling onSaveSuccess callback.")
                snackbarHostState.showSnackbar("Статтю збережено!", duration = SnackbarDuration.Short)
                onSaveSuccess()
                viewModel.resetSaveArticleStatus()
            }
            SaveArticleStatus.Error -> {
                Log.d("CreateEditArticle", "Save error occurred.")
                viewModel.resetSaveArticleStatus()
            }
            else -> { }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            if (saveStatus != SaveArticleStatus.Error) {
                Log.d("CreateEditArticle", "Showing error snackbar: $it")
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            }
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isSaving) onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!isSaving) {
                        Log.d("CreateEditArticle", "Save button clicked. Title: $title, Published: $published")
                        viewModel.createOrUpdateArticle(
                            id = editingArticleId,
                            title = title,
                            content = content,
                            published = published
                        )
                    }
                },
                icon = {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Done, contentDescription = "Зберегти")
                    }
                },
                text = { Text(if (isSaving) "Збереження..." else if (published && !isEditing) "Опублікувати" else "Зберегти") },
                expanded = true,
                modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        if (isLoadingData) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок статті") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                    isError = title.isBlank() && !isLoadingData
                )
                if (title.isBlank() && !isLoadingData && !isSaving) {
                    Text("Заголовок не може бути порожнім", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }


                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Текст статті") },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp),
                    enabled = !isSaving,
                    isError = content.isBlank() && !isLoadingData
                )
                if (content.isBlank() && !isLoadingData && !isSaving) {
                    Text("Текст статті не може бути порожнім", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Зробити публічною", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = published,
                        onCheckedChange = { published = it },
                        enabled = !isSaving
                    )
                }
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}