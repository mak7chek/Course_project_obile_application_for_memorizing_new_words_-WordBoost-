// com.example.wordboost.ui.screens.CreateSetScreen.kt
package com.example.wordboost.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Важливий імпорт для items з key
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.DifficultyLevel
import com.example.wordboost.data.model.SharedSetWordItem // Використовуємо основну модель Word
import com.example.wordboost.viewmodel.CreateSetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSetScreen(
    viewModel: CreateSetViewModel,
    onCloseOrNavigateBack: () -> Unit
) {
    val currentStep by viewModel.currentStep
    val isLoading by viewModel.isLoading
    val operationMessage by viewModel.operationMessage

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Показ Snackbar при зміні operationMessage
    LaunchedEffect(operationMessage) {
        operationMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.operationMessage.value = null // Очищаємо повідомлення після показу
            }
        }
    }

    BackHandler(enabled = true) {
        if (currentStep > 1 && currentStep < 5) { // Припускаємо, 5 - це не крок, а індикатор завершення
            viewModel.goBackStep()
        } else {
            onCloseOrNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Додано SnackbarHost
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            1 -> "Крок 1: Назва набору"
                            2 -> "Крок 2: Деталі"
                            3 -> "Крок 3: Додавання слів"
                            4 -> "Крок 4: Завершення"
                            else -> "Створення набору"
                        }
                    )
                },
                navigationIcon = {
                    if (currentStep > 1 && currentStep < 5) { // Дозволяємо назад до кроку 4 включно
                        IconButton(onClick = { viewModel.goBackStep() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    } else {
                        IconButton(onClick = onCloseOrNavigateBack) {
                            Icon(Icons.Filled.Close, contentDescription = "Закрити")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box( // Box для розміщення CircularProgressIndicator по центру
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Основний вміст кроків тепер в Column, щоб уникнути проблем з Modifier.weight для Spacer
            Column(modifier = Modifier.fillMaxSize()) {
                when (currentStep) {
                    1 -> CreateSetStep1Content(viewModel, Modifier.weight(1f)) // Передаємо вагу
                    2 -> CreateSetStep2Content(viewModel, Modifier.weight(1f))
                    3 -> CreateSetStep3Content(viewModel, Modifier.weight(1f)) // LazyColumn вже має вагу всередині
                    4 -> CreateSetStep4Content(viewModel, viewModel.setNameUk.value, Modifier.weight(1f))
                    // 5 -> // Можливий екран успіху, якщо operationMessage обробляється інакше
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) // align працює в BoxScope
            }
        }
    }
}

// --- КРОК 1 ---
@Composable
fun CreateSetStep1Content(viewModel: CreateSetViewModel, modifier: Modifier = Modifier) {
    val setNameUk by viewModel.setNameUk
    val setNameUkError by viewModel.setNameUkError

    Column(
        modifier = modifier // Застосовуємо переданий Modifier (з вагою)
            .fillMaxSize() // Заповнюємо доступний простір (наданий вагою)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Центруємо вміст вертикально
    ) {
        Text("Введіть назву для вашого нового набору карток українською мовою.", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = setNameUk,
            onValueChange = { viewModel.onSetNameUkChanged(it) },
            label = { Text("Назва набору (українською)") },
            isError = setNameUkError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (setNameUkError != null) {
            Text(text = setNameUkError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.weight(1f)) // Гнучкий Spacer, щоб кнопка була внизу
        Button(
            onClick = { viewModel.proceedToStep2() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading.value
        ) {
            Text("Продовжити")
        }
    }
}

// --- КРОК 2 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSetStep2Content(viewModel: CreateSetViewModel, modifier: Modifier = Modifier) {
    val setNameEn by viewModel.setNameEn
    val selectedDifficulty by viewModel.selectedDifficulty
    val isLoadingNameTranslation by viewModel.isLoading

    Column(
        modifier = modifier // Застосовуємо переданий Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Введіть англійську назву (або залиште авто-переклад) та виберіть рівень складності.", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = setNameEn,
            onValueChange = { viewModel.onSetNameEnChanged(it) },
            label = { Text("Назва набору (англійською)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoadingNameTranslation,
            trailingIcon = {
                // Показуємо індикатор, якщо йде переклад назви (viewModel.isLoading і ми на етапі переходу з 1 на 2)
                // Цю логіку краще інкапсулювати у ViewModel, якщо вона специфічна для цього поля
                if (isLoadingNameTranslation && viewModel.currentStep.value == 2 && setNameEn.isBlank()) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        )

        Text("Рівень складності:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DifficultyLevel.values().forEach { level ->
                FilterChip(
                    selected = selectedDifficulty == level,
                    onClick = { viewModel.onDifficultyChanged(level) },
                    label = { Text(level.displayName) }
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f)) // Гнучкий Spacer
        Button(
            onClick = { viewModel.proceedToStep3() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoadingNameTranslation
        ) {
            Text("Продовжити до додавання слів")
        }
    }
}

// --- КРОК 3: Додавання слів (ОНОВЛЕНИЙ для SharedSetWordItem та однієї кнопки "Перекласти") ---
@Composable
fun CreateSetStep3Content(viewModel: CreateSetViewModel, modifier: Modifier = Modifier) {
    val currentOriginalWord by viewModel.currentOriginalWord // Англійське
    val currentTranslationWord by viewModel.currentTranslationWord // Українське
    val editingWordUiId by viewModel.editingWordUiId
    val isLoadingWordTranslation by viewModel.isLoading
    // operationMessage тепер обробляється через Snackbar у батьківському CreateSetScreen

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Блок введення та перекладу слова ---
        OutlinedTextField(
            value = currentOriginalWord,
            onValueChange = { viewModel.onOriginalWordChanged(it) },
            label = { Text("Англійське слово (Оригінал)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = currentTranslationWord,
            onValueChange = { viewModel.onTranslationWordChanged(it) },
            label = { Text("Український переклад") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                viewModel.addOrUpdateTemporaryWord()
                focusManager.clearFocus()
            }),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // ОДНА КНОПКА "ПЕРЕКЛАСТИ" (логіка визначення напрямку у ViewModel)
        Button(
            onClick = { viewModel.translateSetCreationInputFields() },
            modifier = Modifier.fillMaxWidth(),
            // Кнопка активна, якщо тільки одне з полів заповнене і не йде завантаження
            enabled = (currentOriginalWord.isNotBlank() xor currentTranslationWord.isNotBlank()) && !isLoadingWordTranslation
        ) {
            if (isLoadingWordTranslation && (currentOriginalWord.isNotBlank() xor currentTranslationWord.isNotBlank())) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Перекласти")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button( // Кнопка "Додати слово" / "Зберегти зміни"
            onClick = {
                viewModel.addOrUpdateTemporaryWord()
                focusManager.clearFocus()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = currentOriginalWord.isNotBlank() && currentTranslationWord.isNotBlank() && !isLoadingWordTranslation
        ) {
            Text(if (editingWordUiId == null) "Додати слово до набору" else "Зберегти зміни")
        }
        // --- Кінець блоку введення та перекладу ---

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.temporaryWordsList.isNotEmpty()) {
            Text("Додані слова (${viewModel.temporaryWordsList.size}):", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(
                    items = viewModel.temporaryWordsList.toList(),
                    key = { wordItem -> wordItem.id } // Використовуємо id з SharedSetWordItem
                ) { wordItem -> // wordItem тепер типу SharedSetWordItem
                    TemporaryWordListItem( // Цей Composable тепер приймає SharedSetWordItem
                        wordItem = wordItem,
                        onEdit = { viewModel.startEditTemporaryWord(wordItem) },
                        onDelete = { viewModel.deleteTemporaryWord(wordItem) },
                        isBeingEdited = editingWordUiId == wordItem.id
                    )
                    Divider()
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Ще не додано жодного слова.", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.proceedToStep4() },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.temporaryWordsList.isNotEmpty() && !isLoadingWordTranslation
        ) {
            Text("Далі до налаштувань видимості")
        }
    }
}

@Composable
fun TemporaryWordListItem(
    wordItem: SharedSetWordItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isBeingEdited: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isBeingEdited) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(wordItem.originalText, style = MaterialTheme.typography.titleMedium)
            Text(wordItem.translationText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Видалити", tint = MaterialTheme.colorScheme.error)
            }
        }
}


// --- КРОК 4 ---
@Composable
fun CreateSetStep4Content(viewModel: CreateSetViewModel, setNameUkDisplay: String, modifier: Modifier = Modifier) { // Додано setNameUkDisplay
    val isSetPublic by viewModel.isSetPublic
    val isLoadingSaveSet by viewModel.isLoading
    // operationMessage обробляється через Snackbar

    Column(
        modifier = modifier // Застосовуємо переданий Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Налаштуйте видимість для набору '$setNameUkDisplay' та збережіть його.", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Зробити набір публічним?", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = isSetPublic,
                onCheckedChange = { viewModel.onVisibilityChanged(it) },
                enabled = !isLoadingSaveSet
            )
        }
        Text(
            if (isSetPublic) "Цей набір буде видно іншим користувачам."
            else "Цей набір буде видно тільки вам у вашій бібліотеці.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f)) // Гнучкий Spacer

        val currentOperationMessage by viewModel.operationMessage
        if (currentOperationMessage?.contains("успішно створено") == true) {
            Button(
                onClick = { viewModel.resetAllState() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Створити ще один набір")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.saveFullSet() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoadingSaveSet && currentOperationMessage?.contains("успішно створено") != true
        ) {
            Text("Зберегти набір карток")
        }
    }
}