package com.example.wordboost.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush

import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Group
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.tts.TextToSpeechService
import com.example.wordboost.viewmodel.WordListViewModel
import com.example.wordboost.viewmodel.WordListViewModelFactory
import com.example.wordboost.viewmodel.WordDisplayItem
import com.example.wordboost.ui.components.WordListItem
import com.example.wordboost.ui.components.GroupFilterDropdown


import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    repository: FirebaseRepository,
    ttsService: TextToSpeechService,
    authRepository: AuthRepository,
    onWordEdit: (wordId: String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: WordListViewModel = viewModel(
        factory = WordListViewModelFactory(
            repository = repository,
            ttsService = ttsService,
            authRepository = authRepository
        )
    )

    val displayedWords by viewModel.displayedWords.collectAsState(initial = emptyList())
    val groups by viewModel.groups.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val selectedGroupIdFilter by viewModel.selectedGroupIdFilter.collectAsState()


    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearErrorMessage()
            }
        }
    }

    BackHandler(enabled = true) { onBack() }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мій Словник") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.background
                        )
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Пошук слів...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            GroupFilterDropdown(
                groups = groups,
                selectedGroupId = selectedGroupIdFilter,
                onGroupSelected = { groupId -> viewModel.setGroupFilter(groupId) }
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                isLoading && displayedWords.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                displayedWords.isEmpty() && !isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val message = if (searchQuery.isNotBlank() || (selectedGroupIdFilter != null)) {
                            "Не знайдено слів за цими критеріями"
                        } else {
                            "Список слів порожній.\nДодайте перше слово!"
                        }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                displayedWords.isNotEmpty() && !isLoading -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = displayedWords,
                            key = { it.id }
                        ) { item ->
                            WordListItem(
                                item = item,
                                onItemClick = { word: Word -> onWordEdit(word.id) },
                                onEditClick = { word: Word -> onWordEdit(word.id) },
                                onResetClick = { word: Word -> viewModel.resetWord(word) },
                                onDeleteClick = { wordId: String -> viewModel.deleteWord(wordId) },
                                onPlaySound = { word: Word -> viewModel.playWordSound(word) },
                                formatDate = { timestamp -> viewModel.formatNextReviewDate(timestamp) },
                                wordProgress = item.progress,
                            )
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}