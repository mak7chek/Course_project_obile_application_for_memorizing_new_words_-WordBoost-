package com.example.wordboost.ui.screens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordboost.ui.components.GroupSelectionDropdown
// Імпорти ViewModel та Factory
import com.example.wordboost.viewmodel.EditWordViewModel
import com.example.wordboost.viewmodel.EditWordViewModelFactory

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWordScreen(
    wordId: String?, // ID слова для редагування (передається з MainScreen)
    factory: EditWordViewModelFactory, // Factory для створення ViewModel (має бути створена у MainScreen з потрібними залежностями)
    onBack: () -> Unit // Колбек для повернення назад
) {

    val viewModel: EditWordViewModel = viewModel(factory = factory)


    // --- Спостерігаємо за станами ViewModel ---
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val word by viewModel.word.collectAsState()

    val editedText by viewModel.editedText.collectAsState()
    val editedTranslation by viewModel.editedTranslation.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val groups by viewModel.groups.collectAsState() // Список груп для вибору

    // --- Стан UI ---
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(errorMessage, saveSuccess) {
        errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearStatusMessage()
            }
        }
        if (saveSuccess == true) {
            coroutineScope.launch {
                // Можна показати короткий Snackbar про успіх перед поверненням
                // snackbarHostState.showSnackbar("Зміни збережено!", duration = SnackbarDuration.Short)
                // delay(500) // Коротка затримка
                onBack()
                viewModel.clearStatusMessage() // Очищаємо статус збереження після обробки
            }
        }
    }

    BackHandler(enabled = !isLoading) {
        onBack()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (word == null) "Завантаження..." else "Редагувати слово") }, // Заголовок
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // --- Основний контент екрана ---
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // !!! Індикатор завантаження/збереження !!!
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Відображаємо UI редагування тільки якщо слово завантажено і немає помилки
            if (word != null && errorMessage == null) {
                // --- Поля редагування тексту та перекладу ---
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { viewModel.onTextChange(it) },
                    label = { Text("Слово (українською)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editedTranslation,
                    onValueChange = { viewModel.onTranslationChange(it) },
                    label = { Text("Переклад (англійською)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Вибір Групи (Dropdown) ---
                GroupSelectionDropdown(
                    groups = groups, // Список груп з ViewModel
                    selectedGroupId = selectedGroupId, // Вибраний ID групи з ViewModel
                    onGroupSelected = { groupId -> viewModel.onGroupSelected(groupId) } // Оновлюємо ViewModel
                )


                Spacer(modifier = Modifier.height(16.dp))

                // --- Кнопки Зберегти та Скасувати ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.saveWord() }, // Викликаємо збереження
                        // Кнопка активна, якщо не завантажуємо/зберігаємо І поля тексту/перекладу не порожні
                        enabled = !isLoading && editedText.isNotBlank() && editedTranslation.isNotBlank()
                    ) {
                        Text("Зберегти")
                    }

                    OutlinedButton(
                        onClick = onBack, // Викликаємо повернення
                        enabled = !isLoading
                    ) {
                        Text("Скасувати")
                    }
                }
            } else if (!isLoading && errorMessage != null) {
                // Відображаємо повідомлення про помилку, якщо слово не завантажено і є помилка
                Text(
                    text = errorMessage ?: "Невідома помилка",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!isLoading && word == null && errorMessage == null) {

                Text(
                    text = "Слово не знайдено або видалено.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}