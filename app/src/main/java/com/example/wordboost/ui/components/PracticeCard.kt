package com.example.wordboost.ui.components // Ваш пакет компонентів

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign // Імпортуємо TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.R // Імпортуємо R для ресурсів
import com.example.wordboost.data.model.Word
import kotlin.math.absoluteValue
@Composable
fun PracticeCard(
    word: Word,
    isFlipped: Boolean,
    isReverse: Boolean,
    onFlip: () -> Unit,
    onReplaySound: (Word) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(500),
        label = "PracticeCardFlip"
    )
    val frontOpacity by animateFloatAsState(
        targetValue = if (rotationY.absoluteValue % 360f <= 90f) 1f else 0f,
        animationSpec = tween(100),
        label = "PracticeCardFrontOpacity"
    )
    val backOpacity by animateFloatAsState(
        targetValue = if (rotationY.absoluteValue % 360f > 90f) 1f else 0f,
        animationSpec = tween(100),
        label = "PracticeCardBackOpacity"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = 12f * density
            }
            .noRippleClickable(enabled = !isFlipped) { onFlip() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Визначаємо тексти спереду/ззаду. Припускаємо translation - англійська.
            val frontText = if (!isReverse) word.text else word.translation // Текст на фронті
            val backTextPrimary =
                if (!isReverse) word.translation else word.text // Англійська на звороті, якщо isReverse=false
            val backTextSecondary =
                if (!isReverse) word.text else word.translation // Українська на звороті, якщо isReverse=false

            // Визначаємо, чи англійська сторона зараз видима
            // Англійська (translation) на фронті, якщо !isFlipped І isReverse.
            // Англійська (translation) на звороті, якщо isFlipped І !isReverse.
            val isEnglishSideVisible = (!isFlipped && isReverse) || (isFlipped && !isReverse)

            // Фронтова сторона
            if (rotationY.absoluteValue % 360f <= 90f) {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = frontOpacity }) {
                    // Якщо на фронті англійський текст, розміщуємо його разом з іконкою в Row
                    if (!isFlipped && isEnglishSideVisible) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                                .align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = { onReplaySound(word) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.volume_up_svgrepo_com),
                                    contentDescription = "Прослухати ${word.translation}",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp)) // Відступ
                            Text(
                                text = frontText,
                                style = MaterialTheme.typography.displaySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(
                                    1f,
                                    fill = false
                                ) // Текст займає потрібний простір
                            )
                        }
                    } else {
                        // Якщо на фронті не англійський текст, просто показуємо CardContent для нього
                        CardContent(
                            title = frontText,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                }
            }
            // Зворотна сторона
            else {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                    this.rotationY = 180f // Компенсація обертання
                    alpha = backOpacity
                }) {
                    // Якщо на звороті англійський текст, розміщуємо його разом з іконкою в Row
                    if (isFlipped && isEnglishSideVisible) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                                .align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = { onReplaySound(word) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.volume_up_svgrepo_com),
                                    contentDescription = "Прослухати ${word.translation}",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp)) // Відступ
                            Text(
                                text = backTextPrimary, // Англійський текст
                                style = MaterialTheme.typography.displaySmall, // Стиль основного тексту
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        // Показуємо другий текст (український) під основним англійським
                        Text(
                            text = backTextSecondary, // Український текст
                            style = MaterialTheme.typography.headlineMedium, // Стиль додаткового тексту
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                                .padding(top = 60.dp) // Розміщення під основним текстом
                            // Потрібно скоригувати відступ (60.dp) залежно від розміру тексту та іконки
                        )

                    } else {
                        CardContent(
                            title = backTextPrimary,
                            subtitle = backTextSecondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}