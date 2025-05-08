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
import com.example.wordboost.ui.components.GroupFilterDropdown
import com.example.wordboost.viewmodel.EditWordViewModel
import com.example.wordboost.viewmodel.EditWordViewModelFactory

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWordScreen(
    wordId: String?,
    factory: EditWordViewModelFactory,
    onBack: () -> Unit
) {

    val viewModel: EditWordViewModel = viewModel(
        factory = factory,
        key = wordId
    )

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val word by viewModel.word.collectAsState()

    val editedText by viewModel.editedText.collectAsState()
    val editedTranslation by viewModel.editedTranslation.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val groups by viewModel.groups.collectAsState()

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
                onBack()
                viewModel.clearStatusMessage()
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (word != null && errorMessage == null) {
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

                GroupFilterDropdown(
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    onGroupSelected = { groupId -> viewModel.onGroupSelected(groupId) }
                )


                Spacer(modifier = Modifier.height(16.dp))

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
                        onClick = onBack,
                        enabled = !isLoading
                    ) {
                        Text("Скасувати")
                    }
                }
            } else if (!isLoading && errorMessage != null) {
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