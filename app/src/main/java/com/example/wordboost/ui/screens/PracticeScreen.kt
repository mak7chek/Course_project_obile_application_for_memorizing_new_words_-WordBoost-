package com.example.wordboost.ui.screens

import androidx.activity.compose.BackHandler // Імпортуємо BackHandler
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Імпортуємо viewModel
import com.example.wordboost.data.model.Word // Імпортуємо Word
import com.example.wordboost.data.repository.PracticeRepository // Імпортуємо PracticeRepository
// Імпортуємо ViewModel та Factory
import com.example.wordboost.viewmodel.PracticeViewModel
import com.example.wordboost.viewmodel.PracticeViewModelFactory
// Імпортуємо компоненти
import com.example.wordboost.ui.components.PracticeCard

import kotlinx.coroutines.launch
import kotlin.math.roundToInt
// import kotlin.random.Random // Random тепер використовується у ViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Для Back button
import androidx.compose.ui.text.style.TextAlign


enum class DragAnchors {
    Start,
    Know,
    DontKnow
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PracticeScreen(
    practiceRepo: PracticeRepository, // Отримуємо як залежність для Factory ViewModel
    onBack: () -> Unit // Колбек для кнопки "Назад"
) {
    // Отримуємо ViewModel за допомогою Factory
    val viewModel: PracticeViewModel = viewModel(factory = PracticeViewModelFactory(practiceRepo))

    // Спостерігаємо за станом з ViewModel
    val words by viewModel.words.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isFlipped by viewModel.isFlipped.collectAsState()
    val isReverse by viewModel.isReverse.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()


    // UI-специфічний стан та логіка для свайпу залишаються тут
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val defaultActionSize = 100.dp // Відстань для спрацювання дії свайпу
    val actionSizePx = with(density) { defaultActionSize.toPx() }

    val anchoredDraggableState = remember {
        AnchoredDraggableState<DragAnchors>(
            initialValue = DragAnchors.Start,
            // Якорі оновлюються в LaunchedEffect
            anchors = DraggableAnchors<DragAnchors> { DragAnchors.Start at 0f },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay()
        )
    }

    // --- Ефект для оновлення якорів свайпу залежно від isFlipped ---
    LaunchedEffect(isFlipped, actionSizePx) {
        val newAnchors = if (isFlipped) {
            DraggableAnchors<DragAnchors> {
                DragAnchors.Start at 0f
                DragAnchors.Know at actionSizePx
                DragAnchors.DontKnow at -actionSizePx // <-- Коректний напрямок для "Не пам'ятаю"
            }
        } else {
            DraggableAnchors<DragAnchors> {
                DragAnchors.Start at 0f
            }
        }
        anchoredDraggableState.updateAnchors(newAnchors)

        // Якщо картку перевернули назад І вона не в центрі, повернемо її.
        // Це також спрацює, коли ViewModel змінює isFlipped на false для нової картки
        if (!isFlipped && anchoredDraggableState.currentValue != DragAnchors.Start) {
            coroutineScope.launch { anchoredDraggableState.snapTo(DragAnchors.Start) }
        }
    }

    // --- Ефект для реагування на завершення свайпу ---
    // Цей ефект викликає функцію ViewModel після завершення анімації свайпу
    LaunchedEffect(anchoredDraggableState.targetValue) {
        val target = anchoredDraggableState.targetValue
        // Діємо тільки якщо картка була перевернута І свайпнута до кінця
        // ViewModel змінює isFlipped, що скидає цей ефект для нової картки
        if (isFlipped) {
            when (target) {
                DragAnchors.Know -> {
                    // Картка свайпнута вправо
                    viewModel.processSwipeResult(5) // Передаємо результат "Пам'ятаю"
                    // Не скидаємо тут UI стан, це зробить ViewModel
                }
                DragAnchors.DontKnow -> {
                    // Картка свайпнута вліво
                    viewModel.processSwipeResult(0) // Передаємо результат "Не пам'ятаю"
                    // Не скидаємо тут UI стан, це зробить ViewModel
                }
                DragAnchors.Start -> { /* Нічого не робити при поверненні до старту */ }
            }
        }
    }

    // Обробник системної кнопки "Назад"
    BackHandler(enabled = true) { onBack() }


    // Показати Snackbar при отриманні повідомлення про статус
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScopeSnackbar = rememberCoroutineScope() // Окремий scope для Snackbar

    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            coroutineScopeSnackbar.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearStatusMessage() // Очищаємо повідомлення після показу
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Практика слів") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // Додаємо SnackbarHost
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {

            if (isLoading && words.isEmpty()) { // Показуємо індикатор, коли завантажуємо і список порожній
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (words.isEmpty() && !isLoading) { // Якщо список порожній після завантаження
                Text("Слів для практики поки немає.\nДодайте слова або змініть статус існуючих.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else { // Якщо слова є
                words.getOrNull(currentIndex)?.let { word ->

                    // --- Застосовуємо модифікатори свайпу до PracticeCard ---
                    PracticeCard(
                        word = word, // Дані з ViewModel
                        isFlipped = isFlipped, // Стан з ViewModel
                        isReverse = isReverse, // Стан з ViewModel
                        onFlip = { viewModel.flipCard() }, // Викликаємо функцію ViewModel
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            // Застосовуємо зміщення від стану свайпу
                            .offset {
                                IntOffset(
                                    x = anchoredDraggableState
                                        .requireOffset()
                                        .roundToInt(),
                                    y = 0
                                )
                            }
                            // Додаємо обробник свайпів
                            .anchoredDraggable(
                                state = anchoredDraggableState,
                                orientation = Orientation.Horizontal,
                                enabled = isFlipped // Свайп можливий ТІЛЬКИ коли картка перевернута
                            )
                    )
                } // Текст "Не вдалося отримати слово" тепер обробляється через statusMessage Snackbar
            }

            // Прогрес та лічильник
            if (words.isNotEmpty() && !isLoading) { // Показуємо, тільки якщо є слова і не завантажуємо
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 0.dp) // Невеликий відступ зверху
                ) {
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / words.size.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.End // Вирівнюємо лічильник справа
                    ) {
                        Text( // Лічильник слів
                            text = "${currentIndex + 1} / ${words.size}",
                            style = MaterialTheme.typography.bodySmall,
                            // modifier = Modifier.align(Alignment.TopEnd) // Не тут, бо ми в Column
                        )
                    }
                }
            }

            // Повідомлення про статус (тепер відображається через Snackbar)
            /* statusMessage?.let { Text(...) } */
        }
    }
}