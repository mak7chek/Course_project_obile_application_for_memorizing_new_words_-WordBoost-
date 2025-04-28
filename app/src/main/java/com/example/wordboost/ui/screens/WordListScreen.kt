package com.example.wordboost.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

// Імпорт репозиторію (для Factory)
import com.example.wordboost.data.firebase.FirebaseRepository
// Імпорт моделей
import com.example.wordboost.data.model.Group
import com.example.wordboost.data.model.Word

// Імпорт ViewModel та Factory з presentation.viewmodel пакета
import com.example.wordboost.viewmodel.WordListViewModel
import com.example.wordboost.viewmodel.WordListViewModelFactory
import com.example.wordboost.viewmodel.WordDisplayItem

// Імпорт компонентів з presentation.ui.components пакета
import com.example.wordboost.ui.components.WordListItem
// Імпорт CustomGroupDialog, якщо він використовується тут
// import com.example.wordboost.presentation.ui.components.CustomGroupDialog


// Імпорти для іконок
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    repository: FirebaseRepository,
    onWordEdit: (wordId: String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: WordListViewModel = viewModel(factory = WordListViewModelFactory(repository = repository))

    // Спостерігаємо за станом з ViewModel
    val displayedWords by viewModel.displayedWords.observeAsState(initial = emptyList()) // Список для відображення
    val groups by viewModel.groups.observeAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val searchQuery by viewModel.searchQuery.observeAsState(initial = "")
    val selectedGroupIdFilter by viewModel.selectedGroupIdFilter.observeAsState()


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
        ) {
            // Поле пошуку
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Пошук слів...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Фільтр за групами
            GroupFilterDropdown( // Припускаємо, що цей Composable визначений нижче в цьому ж файлі
                groups = groups,
                selectedGroupId = selectedGroupIdFilter,
                onGroupSelected = { groupId -> viewModel.setGroupFilter(groupId) }
            )

            // Індикатор завантаження (зверху)
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Відображення списку або повідомлення про порожній стан
            if (displayedWords.isEmpty() && !isLoading) { // Показуємо текст, якщо список порожній і не завантажуємо
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank() || (selectedGroupIdFilter != null && selectedGroupIdFilter != "" && selectedGroupIdFilter != "no_group_filter")) {
                            "Не знайдено слів за цими критеріями"
                        } else {
                            "Список слів порожній"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            } else if (displayedWords.isNotEmpty() && !isLoading) { // <-- ВИПРАВЛЕНО ТУТ: ВИКОРИСТОВУЄМО displayedWords
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = displayedWords,
                        key = { it.word.id }
                    ) { item ->
                        WordListItem(
                            item = item,
                            onItemClick = { word -> onWordEdit(word.id) },
                            onEditClick = { word -> onWordEdit(word.id) },
                            onResetClick = { word -> viewModel.resetWord(word) },
                            onDeleteClick = { word -> viewModel.deleteWord(word.id) },
                            formatDate = { timestamp -> viewModel.formatNextReviewDate(timestamp) }
                        )
                    }
                }
            }
            // Примітка: Якщо isLoading true, LinearProgressIndicator вже показаний зверху.
        }
    }
}

// Composable для вибору групи (DropdownMenu). Можна залишити тут або перенести в components.
// ... (цей код залишається без змін, як у попередньому пості) ...
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
                    maxLines = 1
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Виберіть групу")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Max)
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