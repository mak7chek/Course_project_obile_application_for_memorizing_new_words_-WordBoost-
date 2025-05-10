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
    onCloseOrNavigateBack: () -> Unit
) {
    val currentStep by viewModel.currentStep
    val isLoading by viewModel.isLoading
    val operationMessage by viewModel.operationMessage

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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

    val isSuccessfullyCreated = operationMessage?.contains("успішно створено", ignoreCase = true) == true

    BackHandler(enabled = true) {
        if (currentStep > 1 && !isSuccessfullyCreated) {
            // Дозволяємо крок назад, тільки якщо набір ще не створено успішно
            // або якщо ми не на першому кроці.
            viewModel.goBackStep()
        } else {
            // Якщо набір успішно створено, або ми на першому кроці, системна кнопка "назад" закриває екран
            onCloseOrNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            1 -> "Крок 1: Назва набору"
                            2 -> "Крок 2: Деталі"
                            3 -> "Крок 3: Додавання слів"
                            4 -> if(isSuccessfullyCreated) "Набір Створено" else "Крок 4: Завершення"
                            else -> "Створення набору"
                        }
                    )
                },
                navigationIcon = {
                    // Показуємо "Назад" якщо можна повернутися на попередній крок І набір ще не створений
                    if (currentStep > 1 && !isSuccessfullyCreated) {
                        IconButton(onClick = { viewModel.goBackStep() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    } else { // В іншому випадку (перший крок АБО набір вже створений) показуємо "Закрити"
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
            Column(modifier = Modifier.fillMaxSize()) { // Modifier.weight(1f) для вмісту кроку
                when (currentStep) {
                    1 -> CreateSetStep1Content(viewModel, Modifier.weight(1f))
                    2 -> CreateSetStep2Content(viewModel, Modifier.weight(1f))
                    3 -> CreateSetStep3Content(viewModel, Modifier.weight(1f))
                    4 -> CreateSetStep4Content(
                        viewModel = viewModel,
                        setNameUkDisplay = viewModel.setNameUk.value, // Передаємо актуальну назву
                        onCloseFlow = onCloseOrNavigateBack,
                        modifier = Modifier.weight(1f)
                    )
                    // Можна додати окремий крок/стан для "Успішно створено", якщо потрібно
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}