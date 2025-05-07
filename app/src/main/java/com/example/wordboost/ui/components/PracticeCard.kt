package com.example.wordboost.ui.components
import android.util.Log
import androidx.compose.animation.core.Animatable // !!! Імпорт Animatable !!!
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

@Composable
fun FrontCardFace(
    word: Word,
    isTranslationPrimary: Boolean, // Визначає, що показувати: Переклад чи Оригінал
    onSpeakTranslationClick: () -> Unit, // Колбек для озвучення (активний лише для Перекладу)
    modifier: Modifier = Modifier // Apply modifier from parent
) {
    val primaryText = if (isTranslationPrimary) word.translation else word.text

    Log.d("CardStateDebug", "FrontCardFace recomposing. Word: ${word.text}, isTranslationPrimary: $isTranslationPrimary. Showing only primary text.")

    Box(modifier = modifier.padding(16.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
        if (primaryText.isNotBlank()) {
            if (isTranslationPrimary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onSpeakTranslationClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.volume_up_svgrepo_com),
                            contentDescription = "Прослухати переклад",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = primaryText,
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            } else {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Log.w("CardStateDebug", "FrontCardFace: Primary text is blank for word ${word.text}")
            Text("Немає тексту для відображення", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// --- Допоміжний Composable для вмісту Answer сторони картки (ЗАВЖДИ Обидва Тексти) ---
@Composable
fun BackCardFace(
    word: Word,
    onSpeakTranslationClick: () -> Unit,
    modifier: Modifier = Modifier // Apply modifier from parent
) {
    val originalText = word.text
    val translationText = word.translation

    Log.d("CardStateDebug", "BackCardFace recomposing. Word: ${word.text}, showing Both Texts (Answer)")

    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { rotationY = 180f } // Correct compensation here
        ) {
            if (originalText.isNotBlank()) {
                Text(
                    text = originalText,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                )
                if (translationText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                        onClick = onSpeakTranslationClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.volume_up_svgrepo_com),
                            contentDescription = "Прослухати переклад",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = translationText,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            } else {
                Log.w("CardStateDebug", "BackCardFace: Translation text is blank for word ${word.text}")
                if (originalText.isBlank()) {
                    Text("Немає тексту для відображення", style = MaterialTheme.typography.bodySmall)
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
    onReplayTranslationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("CardStateDebug", "PracticeCard перекомпоновується. Слово: ${word.text}, cardState: $cardState, promptContentType: $promptContentType")

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val rotation = remember(word.id) { Animatable(0f) } // Add word.id as key


    LaunchedEffect(word.id) {
        Log.d("CardStateDebug", "PracticeCard: Слово змінилось (ID: ${word.id}). Миттєво скидаємо поворот до 0.")
        // Одразу встановлюємо початкове значення анімації в 0
        rotation.snapTo(0f)
    }


    // Ефект для АНІМАЦІЇ повороту при зміні CardState (клік користувача)
    LaunchedEffect(cardState) {
        val targetRotation = if (cardState == CardState.Answer) 180f else 0f
        // Якщо поточний кут вже дуже близький до цільового, не анімуємо
        if (rotation.value.equals(targetRotation)) {
            Log.d("CardStateDebug", "PracticeCard: CardState змінився на $cardState, але вже на цільовому куті ($targetRotation). Не анімуємо.")
            return@LaunchedEffect
        }

        Log.d("CardStateDebug", "PracticeCard: CardState змінився на $cardState. Анімуємо до $targetRotation.")
        coroutineScope.launch {
            rotation.animateTo( // Анімуємо поточне значення Animatable
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
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Використовуємо поточне значення кута для визначення візуальної сторони
            val currentRotationValue = rotation.value
            val normalizedRotation = currentRotationValue % 360f
            val positiveNormalizedRotation = if (normalizedRotation < 0) normalizedRotation + 360 else normalizedRotation

            val isVisualPromptFace = positiveNormalizedRotation <= 90f || positiveNormalizedRotation >= 270f


            if (isVisualPromptFace) {
                Log.d("CardStateDebug", "Showing FrontCardFace. Current Rotation: $currentRotationValue, Normalized: $positiveNormalizedRotation, isVisualPromptFace: $isVisualPromptFace")
                FrontCardFace(
                    word = word,
                    isTranslationPrimary = promptContentType == PromptContentType.Translation,
                    onSpeakTranslationClick = onReplayTranslationClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.rotationY = 0f // Вміст передньої сторони не обертається відносно картки
                            alpha = if (isVisualPromptFace) 1f else 0f // Керуємо видимістю
                        }
                )
            } else {
                Log.d("CardStateDebug", "Showing BackCardFace. Current Rotation: $currentRotationValue, Normalized: $positiveNormalizedRotation, isVisualPromptFace: $isVisualPromptFace")
                BackCardFace(
                    word = word,
                    onSpeakTranslationClick = onReplayTranslationClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Вміст зворотньої сторони має свою компенсацію повороту всередині BackCardFace
                            alpha = if (!isVisualPromptFace) 1f else 0f // Керуємо видимістю
                        }
                )
            }
        }
    }
}