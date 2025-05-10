package com.example.wordboost.ui.components // Або твій правильний пакет

import android.util.Log
import androidx.compose.animation.core.AnimationSpec // Потрібен для AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.* // Імпортуємо все з gestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Для MaterialTheme та інших M3 компонентів
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Word // Переконайся, що імпорт правильний
import com.example.wordboost.viewmodel.CardState // Переконайся, що імпорт правильний
import com.example.wordboost.viewmodel.PromptContentType // Переконайся, що імпорт правильний
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Enum для визначення станів перетягування (якорів)
private enum class DragAnchors {
    Start,    // Початкова позиція
    Know,     // Свайп вправо (слово відоме)
    DontKnow  // Свайп вліво (слово невідоме/важке)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RegularPracticeUI(
    word: Word?,
    cardState: CardState,
    promptContentType: PromptContentType,
    onFlipCard: () -> Unit,
    onCardSwipedLeft: () -> Unit,
    onCardSwipedRight: () -> Unit,
    onSpeakTranslationClick: (String) -> Unit,
    modifier: Modifier = Modifier // Modifier, переданий ззовні (наприклад, для padding)
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val defaultActionSize = 100.dp // Відстань, на яку треба свайпнути для спрацювання
    val actionSizePx = with(density) { defaultActionSize.toPx() }

    // Ініціалізація стану для anchoredDraggable
    // Тип <DragAnchors> вказується для AnchoredDraggableState
    val anchoredDraggableState = remember {
        AnchoredDraggableState<DragAnchors>(
            initialValue = DragAnchors.Start,
            positionalThreshold = { totalDistance: Float -> totalDistance * 0.5f }, // Поріг для переходу між якорями
            velocityThreshold = { with(density) { 100.dp.toPx() } }, // Поріг швидкості для флінгу
            animationSpec = tween<Float>() // Анімація притягування до якоря
            // confirmValueChange можна залишити за замовчуванням { true }
        )
    }

    // Динамічне оновлення якорів залежно від стану картки (Prompt або Answer)
    LaunchedEffect(cardState, actionSizePx, anchoredDraggableState) {
        Log.d("RegularPracticeUI", "Updating anchors. CardState: $cardState, actionSizePx: $actionSizePx")
        val newAnchors = if (cardState == CardState.Answer) {
            // Якщо картка показує відповідь, дозволяємо свайпи вліво/вправо
            DraggableAnchors<DragAnchors> { // Явно вказуємо тип <DragAnchors>
                DragAnchors.Start at 0f
                DragAnchors.Know at actionSizePx       // Свайп вправо (позитивне зміщення)
                DragAnchors.DontKnow at -actionSizePx  // Свайп вліво (негативне зміщення)
            }
        } else {
            // Якщо картка показує питання, дозволено тільки початковий стан (без свайпів)
            DraggableAnchors<DragAnchors> { // Явно вказуємо тип <DragAnchors>
                DragAnchors.Start at 0f
            }
        }
        anchoredDraggableState.updateAnchors(newAnchors)

        // Якщо після оновлення якорів поточне значення стану не "Start" і зміщення визначено,
        // безпечно повертаємо картку в початкове положення.
        if (anchoredDraggableState.currentValue != DragAnchors.Start && !anchoredDraggableState.offset.isNaN()) {
            if (newAnchors.hasAnchorFor(DragAnchors.Start)) {
                coroutineScope.launch {
                    Log.d("RegularPracticeUI", "Snapping to Start after anchor update. Current offset: ${anchoredDraggableState.offset}")
                    anchoredDraggableState.snapTo(DragAnchors.Start)
                }
            } else {
                Log.w("RegularPracticeUI", "Cannot snap to Start after anchor update, Start anchor not in newAnchors. Offset: ${anchoredDraggableState.offset}")
            }
        }
    }

    // Скидання позиції картки (snapTo Start) при зміні слова
    LaunchedEffect(word) {
        Log.d("RegularPracticeUI", "Word changed to: ${word?.text}. Snapping to Start.")
        // Переконуємося, що якір Start існує в поточному наборі якорів
        if (anchoredDraggableState.anchors.hasAnchorFor(DragAnchors.Start) && !anchoredDraggableState.offset.isNaN()) {
            coroutineScope.launch {
                try {
                    anchoredDraggableState.snapTo(DragAnchors.Start)
                } catch (e: Exception) {
                    Log.e("RegularPracticeUI", "Error snapping to Start on word change: ${e.message}")
                }
            }
        } else if (!anchoredDraggableState.anchors.hasAnchorFor(DragAnchors.Start)) {
            Log.w("RegularPracticeUI", "Cannot snap to Start on word change, Start anchor not defined in current anchors.")
        }
    }

    // Автоматичне озвучення перекладу, коли картка перевернута (CardState.Answer)
    LaunchedEffect(cardState, word) { // Додав word, щоб перезапускати при зміні слова
        if (cardState == CardState.Answer && word != null) {
            word.translation?.takeIf { it.isNotBlank() }?.let { translation ->
                Log.d("RegularPracticeUI", "Card state is Answer. Triggering TTS for word: ${word.text}, translation: $translation")
                onSpeakTranslationClick(translation)
            }
        }
    }

    // Обробка завершення свайпу (коли картка "прилипла" до якоря Know або DontKnow)
    LaunchedEffect(anchoredDraggableState.targetValue) { // Використовуємо targetValue для реакції на завершення жесту
        val target = anchoredDraggableState.targetValue
        // Перевіряємо offset, щоб уникнути дій при початковому NaN або якщо він не змінився
        if (anchoredDraggableState.offset.isNaN()) return@LaunchedEffect

        Log.d("RegularPracticeUI", "targetValue changed: $target. Current CardState: $cardState. Offset: ${anchoredDraggableState.offset}")

        if (cardState == CardState.Answer && (target == DragAnchors.Know || target == DragAnchors.DontKnow)) {
            Log.d("RegularPracticeUI", "Swipe action fully settled at $target. Handling result.")
            when (target) {
                DragAnchors.Know -> onCardSwipedRight()
                DragAnchors.DontKnow -> onCardSwipedLeft()
                else -> { /* Ігноруємо, якщо ціль Start або інша */ }
            }
            // Після того, як ViewModel обробить свайп і оновить 'word' (на null або наступне слово),
            // LaunchedEffect(word) має скинути позицію картки (snapTo Start) для нової картки.
            // Якщо ж ViewModel не змінює 'word' (наприклад, помилка), то картка залишиться на місці.
            // Можна додати явний snapTo(DragAnchors.Start) сюди, якщо потрібно примусово повертати
            // навіть якщо слово не змінилося, але це може конфліктувати з LaunchedEffect(word).
            // Краще покладатися на зміну 'word' для скидання.
        }
    }

    Column(
        modifier = modifier // Застосовуємо зовнішній modifier (який містить paddingValues від Scaffold)
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp), // Внутрішні відступи для контенту Column
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Основний контент вирівнюється зверху
    ) {
        Text(
            "Звичайна Практика",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Spacer(modifier = Modifier.weight(0.5f)) // Менший спейсер зверху

        if (word != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(3f / 2f) // Співвідношення сторін картки
                    .weight(2f) // Основна вага для картки
                    .offset {
                        // Перевіряємо, чи offset не NaN перед використанням
                        if (anchoredDraggableState.offset.isNaN()) {
                            IntOffset.Zero
                        } else {
                            // Використовуємо requireOffset(), бо якщо не NaN, він має бути валідним
                            IntOffset(anchoredDraggableState.requireOffset().roundToInt(), 0)
                        }
                    }
                    .anchoredDraggable( // Модифікатор для обробки свайпів
                        state = anchoredDraggableState,
                        orientation = Orientation.Horizontal,
                        enabled = cardState == CardState.Answer // Свайп можливий тільки у стані відповіді
                    )
                    .clickable(enabled = cardState == CardState.Prompt) { // Клік для перевороту
                        Log.d("RegularPracticeUI", "Card clicked to flip. Current CardState: $cardState. Calling onFlipCard().")
                        onFlipCard()
                    },
                contentAlignment = Alignment.Center
            ) {
                PracticeCard( // Твій Composable для відображення самої картки
                    word = word,
                    cardState = cardState,
                    promptContentType = promptContentType,
                    onReplayTranslationClick = { word.translation?.takeIf { it.isNotBlank() }?.let { onSpeakTranslationClick(it) } },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text( // Підказка для користувача
                text = when(cardState) {
                    CardState.Prompt -> "Клікніть картку для перекладу"
                    CardState.Answer -> "Свайпніть: Вліво - Важко (2), Вправо - Легко (5)"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp) // Збільшено вертикальний відступ
            )
        } else {
            // Якщо слів для практики немає
            Spacer(modifier = Modifier.weight(0.5f)) // Щоб відцентрувати текст "немає слів"
            Text(
                text = "Слів для практики поки немає.\nПеревірте список слів або спробуйте пізніше.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.weight(1f)) // Більший спейсер знизу
    }
}