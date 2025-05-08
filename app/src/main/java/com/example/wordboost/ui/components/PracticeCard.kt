// ui.components/PracticeCard.kt
package com.example.wordboost.ui.components

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.R
import com.example.wordboost.data.model.Word
import com.example.wordboost.viewmodel.CardState
import com.example.wordboost.viewmodel.PromptContentType
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// Припускаємо, що ці Composable визначені в цьому ж файлі

// --- Допоміжна функція для вмісту Prompt сторони картки (Оновлено: Тільки один текст) ---
@Composable
fun FrontCardFace(
    word: Word,
    isTranslationPrimary: Boolean, // Визначає, що показувати: Переклад чи Оригінал
    onSpeakTranslationClick: () -> Unit, // Колбек для озвучення (активний лише якщо primary - переклад)
    modifier: Modifier = Modifier // Apply modifier from parent
) {
    // Вибираємо лише один текст для показу на лицьовій стороні
    val primaryText = if (isTranslationPrimary) word.translation else word.text

    Log.d("CardStateDebug", "FrontCardFace recomposing. Word: ${word.text}, isTranslationPrimary: $isTranslationPrimary. Showing only primary text: '$primaryText'")

    Box(modifier = modifier.padding(16.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
        if (primaryText.isNotBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { // Вирівнюємо по центру, якщо текст з іконкою
                if (isTranslationPrimary) {
                    // Якщо primary - це переклад, показуємо його з іконкою озвучення
                    Row(
                        modifier = Modifier.fillMaxWidth(), // Рядок займає всю ширину
                        verticalAlignment = Alignment.CenterVertically, // Вирівнювання елементів в рядку
                        horizontalArrangement = Arrangement.Center // Розподіл елементів в рядку
                    ) {
                        IconButton(
                            onClick = onSpeakTranslationClick, // Озвучуємо цей текст
                            modifier = Modifier.size(48.dp) // Розмір іконки
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.volume_up_svgrepo_com),
                                contentDescription = "Прослухати переклад",
                                tint = MaterialTheme.colorScheme.primary // Колір іконки з теми
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp)) // Відступ між іконкою та текстом
                        Text(
                            text = primaryText, // Тільки переклад
                            style = MaterialTheme.typography.displaySmall, // Стиль тексту з теми
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false) // Текст займає доступний простір без розтягування
                        )
                    }
                } else {
                    // Якщо primary - це оригінал, показуємо тільки текст
                    Text(
                        text = primaryText, // Тільки оригінал
                        style = MaterialTheme.typography.displaySmall, // Стиль тексту з теми
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Log.w("CardStateDebug", "FrontCardFace: Primary text is blank for word ${word.text}")
            Text("Немає тексту для відображення", style = MaterialTheme.typography.bodySmall) // Менший стиль для повідомлення
        }
    }
}

// --- Допоміжна функція для вмісту Answer сторони картки (ЗАВЖДИ Обидва Тексти) ---
// Цей Composable залишається БЕЗ ЗМІН
@Composable
fun BackCardFace(
    word: Word,
    onSpeakTranslationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val originalText = word.text
    val translationText = word.translation

    Log.d("CardStateDebug", "BackCardFace recomposing. Word: ${word.text}, showing Both Texts (Answer)")

    Box(
        modifier = modifier.padding(16.dp), // Внутрішній відступ картки
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth() // Заповнюємо всю доступну ширину
                .graphicsLayer { rotationY = 180f } // Компенсація повороту вмісту зворотньої сторони
        ) {
            if (originalText.isNotBlank()) {
                Text(
                    text = originalText, // Оригінал
                    style = MaterialTheme.typography.headlineMedium, // Стиль для оригіналу на зворотній стороні
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface // Колір тексту з теми
                )
                if (translationText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp)) // Відступ між текстами
                }
            } else {
                Log.w("CardStateDebug", "BackCardFace: Original text is blank for word ${word.text}")
            }


            if (translationText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onSpeakTranslationClick, // Озвучення перекладу
                        modifier = Modifier.size(48.dp) // Розмір іконки
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.volume_up_svgrepo_com),
                            contentDescription = "Прослухати переклад",
                            tint = MaterialTheme.colorScheme.primary // Колір іконки з теми
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp)) // Відступ між іконкою та текстом
                    Text(
                        text = translationText, // Переклад
                        style = MaterialTheme.typography.headlineMedium, // Стиль для перекладу на зворотній стороні
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // Колір з теми (можна використати primary)
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            } else {
                Log.w("CardStateDebug", "BackCardFace: Translation text is blank for word ${word.text}")
                if (originalText.isBlank()) {
                    Text("Немає тексту для відображення", style = MaterialTheme.typography.bodySmall) // Менший стиль
                }
            }
        }
    }
}


@Composable
fun PracticeCard(
    word: Word,
    cardState: CardState,
    promptContentType: PromptContentType,
    onReplayTranslationClick: () -> Unit, // Колбек для озвучення
    modifier: Modifier = Modifier
) {
    Log.d("CardStateDebug", "PracticeCard перекомпоновується. Слово: ${word.text}, cardState: $cardState, promptContentType: $promptContentType")

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val rotation = remember(word.id) { Animatable(0f) }


    LaunchedEffect(word.id) {
        Log.d("CardStateDebug", "PracticeCard: Слово змінилось (ID: ${word.id}). Миттєво скидаємо поворот до 0.")
        rotation.snapTo(0f)
    }


    LaunchedEffect(cardState) {
        val targetRotation = if (cardState == CardState.Answer) 180f else 0f
        if (rotation.value.equals(targetRotation)) {
            Log.d("CardStateDebug", "PracticeCard: CardState змінився на $cardState, але вже на цільовому куті ($targetRotation). Не анімуємо.")
            return@LaunchedEffect
        }

        Log.d("CardStateDebug", "PracticeCard: CardState змінився на $cardState. Анімуємо до $targetRotation.")
        coroutineScope.launch {
            rotation.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(500)
            )
            Log.d("CardStateDebug", "PracticeCard: Анімація до $targetRotation завершена.")
        }
    }


    Card(
        modifier = modifier
            .graphicsLayer {
                this.rotationY = rotation.value
                this.cameraDistance = 12f * density.density
                Log.d("CardStateDebug", "graphicsLayer: rotationY=${this.rotationY}, cameraDistance=${this.cameraDistance}")
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        // !!! Змінено колір фону картки на surfaceBright !!!
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val currentRotationValue = rotation.value
            val normalizedRotation = currentRotationValue % 360f
            val positiveNormalizedRotation = if (normalizedRotation < 0) normalizedRotation + 360 else normalizedRotation

            // Визначаємо, яка сторона "видима" на основі кута повороту
            // Кут від 0 до 90 (або від 270 до 360) показує передню сторону
            val isVisualPromptFace = positiveNormalizedRotation <= 90f || positiveNormalizedRotation >= 270f


            if (isVisualPromptFace) {
                Log.d("CardStateDebug", "Showing FrontCardFace. Current Rotation: $currentRotationValue, Normalized: $positiveNormalizedRotation, isVisualPromptFace: $isVisualPromptFace")
                FrontCardFace(
                    word = word,
                    isTranslationPrimary = promptContentType == PromptContentType.Translation,
                    onSpeakTranslationClick = onReplayTranslationClick, // Передаємо колбек озвучення
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Зворотній поворот вмісту FrontCardFace
                            this.rotationY = 0f
                            // Керуємо видимістю для уникнення мерехтіння
                            alpha = if (isVisualPromptFace) 1f else 0f
                        }
                )
            } else {
                Log.d("CardStateDebug", "Showing BackCardFace. Current Rotation: $currentRotationValue, Normalized: $positiveNormalizedRotation, isVisualPromptFace: $isVisualPromptFace")
                BackCardFace(
                    word = word,
                    onSpeakTranslationClick = onReplayTranslationClick, // Передаємо колбек озвучення
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Зворотній поворот вмісту BackCardFace (вже є всередині Composable)
                            // graphicsLayer { rotationY = 180f } у BackCardFace
                            // Керуємо видимістю
                            alpha = if (!isVisualPromptFace) 1f else 0f
                        }
                )
            }
        }
    }
}