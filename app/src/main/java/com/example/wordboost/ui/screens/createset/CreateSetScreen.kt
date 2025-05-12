package com.example.wordboost.ui.screens.createset

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.wordboost.viewmodel.CreateSetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSetScreen(
    viewModel: CreateSetViewModel,
    isEditing: Boolean, // <--- Приймаємо прапорець
    onCloseOrNavigateBack: () -> Unit
) {
    val currentStep by viewModel.currentStep
    val isLoading by viewModel.isLoading
    val operationMessage by viewModel.operationMessage
    // Використовуємо isEditingMode з ViewModel, щоб UI реагував на завантаження даних для редагування
    val actualIsEditing = viewModel.isEditingMode // viewModel.isEditingMode стає true після успішного loadSetForEditing

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val successMessagePattern = if (actualIsEditing) "успішно оновлено" else "успішно створено"
    val isSuccessfullySaved = operationMessage?.contains(successMessagePattern, ignoreCase = true) == true

    LaunchedEffect(operationMessage) { /* ... (без змін) ... */ }

    BackHandler(enabled = true) {
        if (currentStep > 1 && !isSuccessfullySaved) {
            viewModel.goBackStep()
        } else {
            onCloseOrNavigateBack() // Викличе resetAllState у ViewModel та скине editingSetId у MainScreen
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (actualIsEditing) { // Динамічний заголовок
                            when (currentStep) {
                                1 -> "Редагування: Назва"
                                2 -> "Редагування: Деталі"
                                3 -> "Редагування: Слова"
                                4 -> if (isSuccessfullySaved) "Набір Оновлено" else "Редагування: Завершення"
                                else -> "Редагування набору"
                            }
                        } else { // Заголовки для створення
                            when (currentStep) {
                                1 -> "Крок 1: Назва набору"
                                2 -> "Крок 2: Деталі"
                                3 -> "Крок 3: Додавання слів"
                                4 -> if (isSuccessfullySaved) "Набір Створено" else "Крок 4: Завершення"
                                else -> "Створення набору"
                            }
                        }
                    )
                },
                navigationIcon = {
                    if (currentStep > 1 && !isSuccessfullySaved) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                when (currentStep) {
                    1 -> CreateSetStep1Content(viewModel, Modifier.weight(1f))
                    2 -> CreateSetStep2Content(viewModel, Modifier.weight(1f))
                    3 -> CreateSetStep3Content(viewModel, Modifier.weight(1f))
                    4 -> CreateSetStep4Content(
                        viewModel = viewModel,
                        setNameUkDisplay = viewModel.setNameUk.value, // Використовуємо state з ViewModel
                        onCloseFlow = onCloseOrNavigateBack,
                        isEditing = actualIsEditing, // <--- Передаємо actualIsEditing
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}