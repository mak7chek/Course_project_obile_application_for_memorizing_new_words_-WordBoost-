package com.example.wordboost.ui.screens

// --- Почищені та потрібні імпорти ---
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
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.ui.components.PracticeCard // Імпорт твоєї картки
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random // Потрібно для isReverse

// --- Імпорти для кнопки Назад та BackHandler ---
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.activity.compose.BackHandler // !!! Додайте цей імпорт !!!


// Залишаємо enum тут або виносимо в окремий файл
enum class DragAnchors {
    Start,
    Know,
    DontKnow
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // Додано ExperimentalFoundationApi
@Composable
fun PracticeScreen(practiceRepo: PracticeRepository, onBack: () -> Unit) { // onBack лямбда вже є
    var words by remember { mutableStateOf<List<Word>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    // Початкове значення isReverse встановлюється в LaunchedEffect
    var isReverse by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope() // Додано, якщо був відсутній

    // --- Стан для свайпу ---
    val density = LocalDensity.current
    val defaultActionSize = 100.dp // Відстань для спрацювання дії свайпу
    val actionSizePx = with(density) { defaultActionSize.toPx() }

    val anchoredDraggableState = remember {
        AnchoredDraggableState<DragAnchors>(
            initialValue = DragAnchors.Start,
            anchors = DraggableAnchors<DragAnchors> { DragAnchors.Start at 0f }, // Починаємо тільки зі Start
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay()
        )
    }

    // --- Обробник результату свайпу та переходу до наступного слова ---
    val currentMoveToNextWord by rememberUpdatedState { rezalt: Int -> // Перейменовано параметр з known на rezalt (0 або 5)
        if (words.isEmpty()) return@rememberUpdatedState
        val wordToUpdate = words.getOrNull(currentIndex) ?: return@rememberUpdatedState
        // Використовуємо rezalt (0 або 5) прямо тут

        practiceRepo.updateWordAfterPractice(wordToUpdate,rezalt) {
            statusMessage = if (rezalt == 5) "Чудово!" else "Не біда, вивчимо!" // Оновлено текст статусу
            val nextIndex = if (words.isNotEmpty()) (currentIndex + 1) % words.size else 0
            currentIndex = nextIndex
            isFlipped = false // Завжди скидаємо перевертання
            isReverse = Random.nextBoolean() // Встановлюємо новий рандомний реверс
            coroutineScope.launch {
                // Скидаємо якорі та позицію для нової картки
                anchoredDraggableState.updateAnchors(DraggableAnchors<DragAnchors> { DragAnchors.Start at 0f })
                anchoredDraggableState.snapTo(DragAnchors.Start)
            }
        }
    }

    // --- Завантаження слів та початкове скидання стану ---
    LaunchedEffect(Unit) {
        practiceRepo.getWordsForPractice { fetchedWords ->
            words = fetchedWords
            currentIndex = 0
            isFlipped = false
            statusMessage = null
            isReverse = Random.nextBoolean() // Початкове значення
            // Скидання стану свайпу
            coroutineScope.launch {
                anchoredDraggableState.updateAnchors(DraggableAnchors<DragAnchors> { DragAnchors.Start at 0f })
                anchoredDraggableState.snapTo(DragAnchors.Start)
            }
        }
    }

    // --- Ефект для оновлення якорів свайпу залежно від isFlipped ---
    LaunchedEffect(isFlipped, actionSizePx) {
        val newAnchors = if (isFlipped) {
            DraggableAnchors<DragAnchors> {
                DragAnchors.Start at 0f
                DragAnchors.Know at actionSizePx
                DragAnchors.DontKnow at -actionSizePx
            }
        } else {
            DraggableAnchors<DragAnchors> {
                DragAnchors.Start at 0f
            }
        }
        anchoredDraggableState.updateAnchors(newAnchors)

        // Якщо картку перевернули назад і вона не в центрі, повернемо її.
        if (!isFlipped && anchoredDraggableState.currentValue != DragAnchors.Start) {
            coroutineScope.launch { anchoredDraggableState.snapTo(DragAnchors.Start) }
        }
    }

    // --- Ефект для реагування на завершення свайпу ---
    // Цей ефект має бути *за межами* будь-яких `if`, щоб завжди слухати зміни стану
    LaunchedEffect(anchoredDraggableState.targetValue) {
        val target = anchoredDraggableState.targetValue
        // Діємо тільки якщо картка була перевернута І свайпнута до кінця
        if (isFlipped) {
            when (target) {
                DragAnchors.Know -> currentMoveToNextWord(5) // Передаємо 5 для "Пам'ятаю"
                DragAnchors.DontKnow -> currentMoveToNextWord(0) // Передаємо 0 для "Не пам'ятаю"
                DragAnchors.Start -> { /* Нічого не робити при поверненні до старту */ }
            }
        }
    }

    // !!! Додаємо обробник системного свайпу назад (або натискання кнопки Назад) !!!
    BackHandler(enabled = true) { // enabled = true означає, що обробник завжди активний
        onBack() // Викликаємо зовнішню лямбду onBack
    }


    Scaffold(
        topBar = {
            TopAppBar( // !!! Змінюємо на TopAppBar для додавання navigationIcon !!!
                title = { Text("Практика слів") }, // Залишаємо заголовок
                navigationIcon = { // !!! Додаємо navigationIcon слот для кнопки Назад !!!
                    IconButton(onClick = onBack) { // Кнопка Назад, яка викликає onBack
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp), // Загальний відступ для Box
            contentAlignment = Alignment.Center
        ) {

            if (words.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) // Краще індикатор по центру
            } else {
                words.getOrNull(currentIndex)?.let { word ->

                    // --- Застосовуємо модифікатори свайпу до PracticeCard ---
                    PracticeCard(
                        word = word,
                        isFlipped = isFlipped,
                        isReverse = isReverse,
                        onFlip = { isFlipped = !isFlipped },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            // Додаємо зміщення на основі стану свайпу
                            .offset {
                                IntOffset(
                                    x = anchoredDraggableState
                                        .requireOffset()
                                        .roundToInt(),
                                    y = 0
                                )
                            }
                            // Додаємо сам обробник свайпів
                            .anchoredDraggable(
                                state = anchoredDraggableState,
                                orientation = Orientation.Horizontal,
                                enabled = isFlipped // Свайп можливий ТІЛЬКИ коли картка перевернута
                            )
                    )
                } ?: Text("Помилка: Не вдалося отримати слово", modifier = Modifier.align(Alignment.Center)) // На випадок проблеми з індексом - по центру

                // Прогрес (з виправленим обчисленням)
                if (words.isNotEmpty()) {
                    // Перемістимо індикатор прогресу і лічильник всередину головного Box,
                    // щоб вони розміщувалися відносно нього, а не відносно картки.
                    LinearProgressIndicator(
                        // --- ВИПРАВЛЕНО: Використовуємо toFloat ---
                        progress = { (currentIndex + 1).toFloat() / words.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter) // Вирівнюємо зверху по центру Box
                            .padding(top = 0.dp) // Можна залишити 0 або додати невеликий відступ
                    )
                    Text( // Додамо лічильник слів
                        text = "${currentIndex + 1} / ${words.size}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.TopEnd) // Вирівнюємо зверху справа Box
                            .padding(top = 4.dp, end = 4.dp) // Відступи справа та зверху
                    )
                }



                // Повідомлення про статус - залишаємо його внизу, але можемо додати відступ
                statusMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter) // Вирівнюємо знизу по центру Box
                            .padding(bottom = 16.dp) // Відступ знизу
                    )
                }
            }

        }
    }
}