package com.example.wordboost.ui.screens // Переконайтесь, що пакет правильний

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
// !!! ВИДАЛИТИ ЦЕЙ ІМПОРТ LiveData !!!
// import androidx.compose.runtime.livedata.observeAsState
// !!! ВИКОРИСТОВУЄМО collectAsState !!!
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

// Переконайтесь, що імпорти репозиторіїв, моделей та сервісів правильні
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.Group // Переконайтесь в імпорті Group
import com.example.wordboost.data.model.Word // Переконайтесь в імпорті Word
import com.example.wordboost.data.tts.TextToSpeechService
// Переконайтесь в імпорті ViewModel та Factory
import com.example.wordboost.viewmodel.WordListViewModel
import com.example.wordboost.viewmodel.WordListViewModelFactory
import com.example.wordboost.viewmodel.WordDisplayItem // Переконайтесь в імпорті WordDisplayItem
import com.example.wordboost.ui.components.WordListItem // Переконайтесь в імпорті WordListItem
// Якщо CustomGroupDialog викликається з цього екрану і потрібен тут
// import com.example.wordboost.presentation.ui.components.CustomGroupDialog


import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    repository: FirebaseRepository, // Потрібен для Factory
    ttsService: TextToSpeechService, // Потрібен для Factory
    authRepository: AuthRepository, // Потрібен для Factory
    onWordEdit: (wordId: String) -> Unit, // Колбек для навігації на редагування
    onBack: () -> Unit // Колбек для повернення назад
) {
    // Отримуємо ViewModel за допомогою Factory
    val viewModel: WordListViewModel = viewModel(
        factory = WordListViewModelFactory(
            repository = repository,
            ttsService = ttsService,
            authRepository = authRepository
        )
    )

    // !!! ВИКОРИСТОВУЄМО collectAsState() ДЛЯ СПОСТЕРЕЖЕННЯ ЗА StateFlow !!!
    // Ці StateFlow визначені у WordListViewModel і оновлюються автоматично
    val displayedWords by viewModel.displayedWords.collectAsState(initial = emptyList())
    val groups by viewModel.groups.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val selectedGroupIdFilter by viewModel.selectedGroupIdFilter.collectAsState()


    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // LaunchedEffect для відображення повідомлень про помилку/статус
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

    // Обробка натискання кнопки "Назад"
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
        ) {
            // Поле пошуку
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Пошук слів...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Фільтр за групами
            GroupFilterDropdown(
                groups = groups,
                selectedGroupId = selectedGroupIdFilter,
                onGroupSelected = { groupId -> viewModel.setGroupFilter(groupId) }
            )

            // Індикатор завантаження
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Відображення списку слів або повідомлення
            when {
                isLoading -> {
                    // Показати індикатор
                }
                // Перевірка, якщо список порожній і НЕ завантажується
                displayedWords.isEmpty() && !isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val message = if (searchQuery.isNotBlank() || (selectedGroupIdFilter != null && selectedGroupIdFilter != "" && selectedGroupIdFilter != "no_group_filter")) {
                            "Не знайдено слів за цими критеріями"
                        } else {
                            "Список слів порожній.\nДодайте перше слово!"
                        }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                // Якщо є слова для відображення і НЕ завантажується
                displayedWords.isNotEmpty() && !isLoading -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = displayedWords, // Список WordDisplayItem
                            key = { it.id } // Унікальний ключ для елементів
                        ) { item -> // item має тип WordDisplayItem
                            WordListItem(
                                item = item, // Передаємо WordDisplayItem

                                // !!! ЯВНО ВКАЗУЄМО ТИП ПАРАМЕТРА ЛЯМБДИ !!!
                                // WordListItem onItemClick очікує (Word) -> Unit
                                onItemClick = { word: Word -> onWordEdit(word.id) }, // Лямбда приймає Word як параметр 'word'

                                // WordListItem onEditClick очікує (Word) -> Unit
                                onEditClick = { word: Word -> onWordEdit(word.id) }, // Лямбда приймає Word як параметр 'word'

                                // WordListItem onResetClick очікує (Word) -> Unit
                                onResetClick = { word: Word -> viewModel.resetWord(word) }, // Лямбда приймає Word як параметр 'word'

                                // WordListItem onDeleteClick очікує (String) -> Unit (бо ViewModel приймає String ID)
                                // Лямбда приймає String ID як параметр 'wordId'
                                onDeleteClick = { word: Word -> viewModel.deleteWord(word.id) },

                                // WordListItem onPlaySound очікує (Word) -> Unit
                                onPlaySound = { word: Word -> viewModel.playWordSound(word) }, // Лямбда приймає Word як параметр 'word'

                                formatDate = { timestamp -> viewModel.formatNextReviewDate(timestamp) },
                                wordProgress = item.progress,
                            )
                        }
                    }
                }
            }
        }
    }
}

// GroupFilterDropdown залишається без змін
@Composable
fun GroupFilterDropdown(
    groups: List<Group>,
    selectedGroupId: String?,
    onGroupSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedGroupName = groups.find { it.id == selectedGroupId }?.name ?: "Виберіть групу"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(56.dp)
            .clickable { expanded = true },
        contentAlignment = Alignment.CenterStart
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedGroupName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Виберіть групу")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(200.dp)
        ) {
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.name, maxLines = 1) },
                    onClick = {
                        onGroupSelected(group.id)
                        expanded = false
                    }
                )
            }
        }
    }
}