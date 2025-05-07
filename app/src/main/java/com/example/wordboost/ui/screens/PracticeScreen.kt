package com.example.wordboost.ui.screens // Переконайтесь, що пакет правильний

import android.util.Log // Keep for BatchDebug
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.wordboost.ui.components.PairingGameUI
import com.example.wordboost.ui.components.RegularPracticeUI

import com.example.wordboost.viewmodel.PracticeViewModel
import com.example.wordboost.viewmodel.PracticeViewModelFactory
import com.example.wordboost.viewmodel.PracticePhase


import kotlinx.coroutines.launch // Keep for snackbar coroutineScope
import androidx.compose.runtime.collectAsState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    factory: PracticeViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: PracticeViewModel = viewModel(factory = factory)

    // --- Спостерігаємо за Станами ViewModel ---
    val currentPracticePhase by viewModel.practicePhase.collectAsState()
    val currentCardState by viewModel.currentCardState.collectAsState()
    // !!! Спостерігаємо за новим станом PromptContentType !!!
    val currentWordPromptContentType by viewModel.currentWordPromptContentType.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Спостерігаємо за поточною партією та індексом
    val currentBatchWords by viewModel.currentBatch.collectAsState()
    val currentWordIndex by viewModel.currentWordIndexInBatch.collectAsState()

    // Обчислюємо поточне слово локально
    val currentWordInBatch = remember(currentBatchWords, currentWordIndex) {
        currentBatchWords.getOrNull(currentWordIndex)
    }

    // Стан для Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScopeSnackbar = rememberCoroutineScope()

    // Логування зміни CardState у PracticeScreen (для налагодження)
    LaunchedEffect(currentCardState) {
        Log.d("CardStateDebug", "PracticeScreen: currentCardState ViewModel змінився на $currentCardState")
    }

    // --- LaunchedEffect для Повідомлень про Помилки (показуємо Snackbar) ---
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            coroutineScopeSnackbar.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearErrorMessage()
            }
        }
    }

    // --- Обробник системної кнопки "Назад" ---
    BackHandler(enabled = currentPracticePhase !is PracticePhase.Loading) {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Практика слів") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = currentPracticePhase !is PracticePhase.Loading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val phase = currentPracticePhase) {
            PracticePhase.Loading -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Завантаження слів для практики...")
                }
            }

            is PracticePhase.BatchPairing -> {
                PairingGameUI(
                    words = phase.wordsInBatch,
                    onPairMatched = { wordId -> viewModel.onPairMatched(wordId) },
                    onPairingFinished = {
                        viewModel.onPairingFinished()
                    },
                    onSpeakTranslationClick = { translationText ->
                        viewModel.speakTranslationText(translationText) // Викликаємо універсальну функцію озвучення
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is PracticePhase.BatchRegular -> {
                Log.d("BatchDebug", "PracticeScreen відображає BatchRegular. Поточний індекс слова: ${currentWordIndex}, Розмір партії: ${currentBatchWords.size}, Слово: $currentWordInBatch")
                Log.d("CardStateDebug", "PracticeScreen відображає BatchRegular. Поточний cardState: $currentCardState, PromptContentType: $currentWordPromptContentType")


                RegularPracticeUI(
                    word = currentWordInBatch,
                    cardState = currentCardState, // Передаємо стан картки (Prompt/Answer)
                    promptContentType = currentWordPromptContentType, // !!! Передаємо тип вмісту для Prompt сторони !!!
                    onFlipCard = { viewModel.flipCard() },
                    onCardSwipedLeft = { viewModel.onCardSwipedLeft() },
                    onCardSwipedRight = { viewModel.onCardSwipedRight() },
                    onSpeakTranslationClick = { translationText ->
                        viewModel.speakTranslationText(translationText) // Викликаємо універсальну функцію озвучення
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is PracticePhase.Finished -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Практична сесія завершена!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ви обробили ${phase.totalPracticedCount} слів.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Повернутись до головного")
                    }
                }
            }

            is PracticePhase.Error -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Помилка:", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(phase.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Повернутись")
                    }
                }
            }

            PracticePhase.Empty -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Наразі немає слів, які потребують практики.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Повернутись")
                    }
                }
            }
        }
    }
}