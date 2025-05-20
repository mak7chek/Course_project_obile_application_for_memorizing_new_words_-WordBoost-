package com.example.wordboost.ui.screens

import android.util.Log
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

import androidx.compose.ui.res.painterResource
import com.example.wordboost.R


import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    factory: PracticeViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: PracticeViewModel = viewModel(factory = factory)

    val currentPracticePhase by viewModel.practicePhase.collectAsState()
    val currentCardState by viewModel.currentCardState.collectAsState()
    val currentWordPromptContentType by viewModel.currentWordPromptContentType.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val currentBatchWords by viewModel.currentBatch.collectAsState()
    val currentWordIndex by viewModel.currentWordIndexInBatch.collectAsState()

    val canUndo by viewModel.canUndo.collectAsState()


    val currentWordInBatch = remember(currentBatchWords, currentWordIndex) {
        currentBatchWords.getOrNull(currentWordIndex)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScopeSnackbar = rememberCoroutineScope()

    LaunchedEffect(key1 = factory) {
        Log.d("PracticeScreen", "LaunchedEffect: Calling startOrRefreshSession.")
        viewModel.startOrRefreshSession()
    }
    LaunchedEffect(currentCardState) {
        Log.d("CardStateDebug", "PracticeScreen: currentCardState ViewModel змінився на $currentCardState")
    }

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
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undoLastAction() },
                        enabled = canUndo
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.left_arrow_return_svgrepo_com),
                            contentDescription = "Скасувати останню дію"
                        )
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
                        viewModel.speakTranslationText(translationText)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is PracticePhase.BatchRegular -> {
                Log.d("BatchDebug", "PracticeScreen відображає BatchRegular. Поточний індекс слова: ${currentWordIndex}, Розмір партії: ${currentBatchWords.size}, Слово: ${currentWordInBatch?.text}")
                Log.d("CardStateDebug", "PracticeScreen відображає BatchRegular. Поточний cardState: $currentCardState, PromptContentType: $currentWordPromptContentType")


                RegularPracticeUI(
                    word = currentWordInBatch,
                    cardState = currentCardState,
                    promptContentType = currentWordPromptContentType,
                    onFlipCard = { viewModel.flipCard() },
                    onCardSwipedLeft = { viewModel.onCardSwipedLeft() },
                    onCardSwipedRight = { viewModel.onCardSwipedRight() },
                    onSpeakTranslationClick = { translationText ->
                        viewModel.speakTranslationText(translationText)
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
                    Text("Практичну сесію завершено!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Загалом слів опрацьовано: ${phase.totalPracticedCount}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Правильні відповіді: ${phase.correctAnswers}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Неправильні відповіді: ${phase.incorrectAnswers}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    val answeredCount = phase.correctAnswers + phase.incorrectAnswers
                    val accuracy = if (answeredCount > 0) {
                        (phase.correctAnswers.toDouble() / answeredCount.toDouble() * 100)
                    } else {
                        0.0
                    }
                    Text("Точність відповідей: ${String.format(java.util.Locale.US, "%.1f", accuracy)}%", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Слів перейшло у вивчені/повторення: ${phase.newlyLearnedCount}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = { viewModel.startOrRefreshSession() }) {
                        Text("Почати нову сесію")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onBack) {
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