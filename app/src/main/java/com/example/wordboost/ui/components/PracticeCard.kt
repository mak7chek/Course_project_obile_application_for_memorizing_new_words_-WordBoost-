// ui.components/PracticeCard.kt
package com.example.wordboost.ui.components

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

@Composable
fun FrontCardFace(
    word: Word,
    isTranslationPrimary: Boolean,
    onSpeakTranslationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryText = if (isTranslationPrimary) word.translation else word.text

    Box(modifier = modifier.padding(16.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
        if (primaryText.isNotBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            }
        } else {
            Text("Немає тексту для відображення", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BackCardFace(
    word: Word,
    onSpeakTranslationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val originalText = word.text
    val translationText = word.translation

    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { rotationY = 180f }
        ) {
            if (originalText.isNotBlank()) {
                Text(
                    text = originalText,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (translationText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val rotation = remember(word.id) { Animatable(0f) }

    LaunchedEffect(word.id) {
        rotation.snapTo(0f)
    }

    LaunchedEffect(cardState) {
        val targetRotation = if (cardState == CardState.Answer) 180f else 0f
        if (rotation.value.equals(targetRotation)) {
            return@LaunchedEffect
        }

        coroutineScope.launch {
            rotation.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(500)
            )
        }
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                this.rotationY = rotation.value
                this.cameraDistance = 12f * density.density
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val currentRotationValue = rotation.value
            val normalizedRotation = currentRotationValue % 360f
            val positiveNormalizedRotation = if (normalizedRotation < 0) normalizedRotation + 360 else normalizedRotation

            val isVisualPromptFace = positiveNormalizedRotation <= 90f || positiveNormalizedRotation >= 270f

            if (isVisualPromptFace) {
                FrontCardFace(
                    word = word,
                    isTranslationPrimary = promptContentType == PromptContentType.Translation,
                    onSpeakTranslationClick = onReplayTranslationClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.rotationY = 0f
                            alpha = if (isVisualPromptFace) 1f else 0f
                        }
                )
            } else {
                BackCardFace(
                    word = word,
                    onSpeakTranslationClick = onReplayTranslationClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (!isVisualPromptFace) 1f else 0f
                        }
                )
            }
        }
    }
}