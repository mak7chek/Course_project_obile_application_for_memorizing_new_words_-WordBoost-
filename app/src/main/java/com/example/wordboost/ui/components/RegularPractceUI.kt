package com.example.wordboost.ui.components

import com.example.wordboost.data.model.Word
import com.example.wordboost.viewmodel.CardState
import com.example.wordboost.viewmodel.PromptContentType

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.remember
import kotlin.math.roundToInt

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import android.util.Log

// !!! Імпорти для фону !!!
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush



private enum class DragAnchors {
    Start,
    Know,
    DontKnow
}


@OptIn(ExperimentalFoundationApi::class) // Додано OptIn для AnchoredDraggable
@Composable
fun RegularPracticeUI(
    word: Word?,
    cardState: CardState,
    promptContentType: PromptContentType,
    onFlipCard: () -> Unit, // Колбек для перевороту картки (обробляється тут)
    onCardSwipedLeft: () -> Unit,
    onCardSwipedRight: () -> Unit,
    onSpeakTranslationClick: (String) -> Unit, // Колбек для озвучення (передається далі)
    modifier: Modifier = Modifier
) {
    // !!! Стан для жестів свайпу (перенесено з попередньої реалізації RegularPracticeUI) !!!
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val defaultActionSize = 100.dp
    val actionSizePx = with(density) { defaultActionSize.toPx() }

    val anchoredDraggableState = remember {
        AnchoredDraggableState<DragAnchors>(
            initialValue = DragAnchors.Start,
            anchors = DraggableAnchors<DragAnchors> { DragAnchors.Start at 0f }, // Початкові якорі
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay() // Використовуйте повний шлях
        )
    }

    // Оновлюємо якорі для свайпу при зміні стану картки (свайп можливий лише у стані Answer)
    LaunchedEffect(cardState, actionSizePx) {
        Log.d("RegularPracticeUI", "Updating anchors. CardState: $cardState, actionSizePx: $actionSizePx")
        val newAnchors = if (cardState == CardState.Answer) {
            DraggableAnchors<DragAnchors> {
                DragAnchors.Start at 0f
                DragAnchors.Know at actionSizePx // Якір для свайпу вправо (Легко)
                DragAnchors.DontKnow at -actionSizePx // Якір для свайпу вліво (Важко)
            }
        } else {
            DraggableAnchors<DragAnchors> {
                DragAnchors.Start at 0f // Тільки початковий якір у стані Prompt
            }
        }
        anchoredDraggableState.updateAnchors(newAnchors)

        // Миттєво повертаємо картку на початковий якір після зміни якорів (наприклад, при переході з Answer назад в Prompt)
        if (anchoredDraggableState.currentValue != DragAnchors.Start) {
            coroutineScope.launch {
                Log.d("RegularPracticeUI", "Snapping draggable state to Start after anchor update.")
                try {
                    anchoredDraggableState.snapTo(DragAnchors.Start)
                } catch (e: IllegalStateException) {
                    Log.e("RegularPracticeUI", "Failed to snap to Start after anchor update: ${e.message}")
                }
            }
        }
    }

    // Скидаємо стан свайпу (позицію картки) при зміні слова
    LaunchedEffect(word) {
        Log.d("RegularPracticeUI", "Word changed: ${word?.text}. Snapping draggable state to Start.")
        coroutineScope.launch {
            try {
                anchoredDraggableState.snapTo(DragAnchors.Start) // Миттєво повертаємо картку на початкову позицію
            } catch (e: IllegalStateException) {
                Log.e("RegularPracticeUI", "Failed to snap to Start on word change: ${e.message}")
            }
        }
    }

    // Автоматичне озвучення перекладу при переході в стан Answer
    // Цей LaunchedEffect вже був у вашому коді, залишаємо його
    LaunchedEffect(cardState) {
        if (cardState == CardState.Answer && word != null) {
            val translation = word.translation
            if (translation != null && translation.isNotBlank()) {
                Log.d("RegularPracticeUI", "Card state is Answer, triggering automatic TTS for word: ${word.text}, translation: $translation")
                onSpeakTranslationClick(translation) // Викликаємо озвучення перекладу
            } else {
                Log.w("RegularPracticeUI", "Card state is Answer, but translation is null or blank. Not triggering TTS.")
            }
        } else if (cardState == CardState.Prompt) {
            Log.d("RegularPracticeUI", "Card state is Prompt.")
        }
    }

    // Обробка завершення анімації свайпу (коли картка досягла якоря Know або DontKnow)
    // Цей LaunchedEffect вже був у вашому коді, залишаємо його
    LaunchedEffect(anchoredDraggableState.targetValue) {
        val target = anchoredDraggableState.targetValue
        Log.d("RegularPracticeUI", "AnchoredDraggable targetValue changed: $target. CardState: $cardState")

        // Обробляємо свайп лише, якщо ми в стані Answer і досягли якоря Know/DontKnow
        if (cardState == CardState.Answer && (target == DragAnchors.Know || target == DragAnchors.DontKnow)) {
            Log.d("RegularPracticeUI", "Swipe action detected (Know/DontKnow). Handling result.")
            when (target) {
                DragAnchors.Know -> {
                    Log.d("RegularPracticeUI", "Swipe RIGHT detected (Know). Calling onCardSwipedRight().")
                    onCardSwipedRight() // Викликаємо колбек для свайпу вправо
                }
                DragAnchors.DontKnow -> {
                    Log.d("RegularPracticeUI", "Swipe LEFT detected (DontKnow). Calling onCardSwipedLeft().")
                    onCardSwipedLeft() // Викликаємо колбек для свайпу вліво
                }
                DragAnchors.Start -> {
                    // Картка повернулася на початковий якір, нічого не робимо
                }
            }
        } else if (target == DragAnchors.Start) {
            Log.d("RegularPracticeUI", "AnchoredDraggable snapped back to Start.")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize() // Застосовуємо модифікатор ззовні (з паддінгом)
            // !!! ДОДАЄМО ГРАДІЄНТНИЙ ФОН !!!
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh, // Колір зверху
                        MaterialTheme.colorScheme.background // Колір знизу
                        // Ви можете обрати інші кольори з вашої теми
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp), // Внутрішні відступи колонки
        horizontalAlignment = Alignment.CenterHorizontally, // Центруємо вміст по горизонталі
        verticalArrangement = Arrangement.Top // Вирівнюємо вміст колонки зверху, щоб вага працювала
    ) {
        Text(
            "Звичайна Практика",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp) // Додано нижній відступ
        )

        // !!! Spacer для відцентрування картки вертикально !!!
        Spacer(modifier = Modifier.weight(1f)) // Перший Spacer займає 1 частину доступного простору

        if (word != null) {
            // CardContainer для розміщення PracticeCard та обробки жестів
            Box(
                // !!! Надаємо Box вагу для пропорційного розміщення на екрані !!!
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Картка займає 90% ширини батьківського контейнера
                    .aspectRatio(3f / 2f) // Зберігаємо співвідношення сторін картки (3:2)
                    .weight(2f) // Box займає 2 частини доступного простору (разом зі Spacers це 1+2+1=4 частини)
                    .offset {
                        // Зсуваємо картку по горизонталі відповідно до стану anchoredDraggable
                        IntOffset(
                            x = anchoredDraggableState
                                .requireOffset()
                                .roundToInt(),
                            y = 0
                        )
                    }
                    // Модифікатор свайпу
                    .anchoredDraggable(
                        state = anchoredDraggableState, // Використовуємо AnchoredDraggableState
                        orientation = Orientation.Horizontal, // Свайп по горизонталі
                        enabled = cardState == CardState.Answer // Вмикаємо свайп тільки у стані Answer
                    )
                    // Модифікатор кліку для перегортання
                    .clickable(enabled = cardState == CardState.Prompt) { // Клік можливий тільки в стані Prompt
                        Log.d("RegularPracticeUI", "Card clicked to flip. CardState: $cardState. Calling onFlipCard().")
                        onFlipCard() // Викликаємо колбек для перевороту картки
                    },
                contentAlignment = Alignment.Center // Центруємо PracticeCard всередині Box
            ) {
                // Відображаємо саму картку PracticeCard
                PracticeCard(
                    word = word,
                    cardState = cardState,
                    promptContentType = promptContentType,
                    onReplayTranslationClick = { word.translation?.let { onSpeakTranslationClick(it) } }, // Передаємо колбек для озвучення (з іконки)
                    modifier = Modifier.fillMaxSize() // Картка заповнює контейнер Box
                )
            }

            // Spacer(modifier = Modifier.height(8.dp)) // Цей Spacer тепер не потрібен через verticalArrangement та вагові Spacers

            // Текст підказки (клікніть або свайпніть)
            Text(
                text = when(cardState) {
                    CardState.Prompt -> "Клікніть картку для перекладу"
                    // Оновлено текст підказки з балами
                    CardState.Answer -> "Свайпніть картку: Вліво - Важко (2), Вправо - Легко (5)"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // !!! Spacer для відцентрування картки вертикально !!!
            Spacer(modifier = Modifier.weight(1f)) // Другий Spacer займає 1 частину доступного простору


        } else {
            // Відображаємо повідомлення, якщо слів для практики немає
            Text(
                text = "Слів для практики поки немає.\nПеревірте список слів або спробуйте пізніше.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Додаємо Spacer знизу, якщо немає слів, щоб повідомлення було відцентроване
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}