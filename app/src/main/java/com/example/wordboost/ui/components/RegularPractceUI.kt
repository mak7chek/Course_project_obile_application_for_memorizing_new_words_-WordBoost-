// ui.components/RegularPracticeUI.kt
package com.example.wordboost.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Word
import com.example.wordboost.viewmodel.CardState
import com.example.wordboost.viewmodel.PromptContentType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class DragAnchors {
    Start,
    Know,
    DontKnow
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
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val defaultActionSize = 100.dp
    val actionSizePx = with(density) { defaultActionSize.toPx() }

    val anchoredDraggableState = remember {
        AnchoredDraggableState<DragAnchors>(
            initialValue = DragAnchors.Start,
            positionalThreshold = { totalDistance: Float -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween<Float>()
        )
    }

    LaunchedEffect(cardState, actionSizePx, anchoredDraggableState) {
        val newAnchors = if (cardState == CardState.Answer) {
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

        if (anchoredDraggableState.currentValue != DragAnchors.Start && !anchoredDraggableState.offset.isNaN()) {
            if (newAnchors.hasAnchorFor(DragAnchors.Start)) {
                coroutineScope.launch {
                    anchoredDraggableState.snapTo(DragAnchors.Start)
                }
            }
        }
    }

    LaunchedEffect(word) {
        if (anchoredDraggableState.anchors.hasAnchorFor(DragAnchors.Start) && !anchoredDraggableState.offset.isNaN()) {
            coroutineScope.launch {
                try {
                    anchoredDraggableState.snapTo(DragAnchors.Start)
                } catch (e: Exception) {
                }
            }
        }
    }

    LaunchedEffect(cardState, word) {
        if (cardState == CardState.Answer && word != null) {
            word.translation?.takeIf { it.isNotBlank() }?.let { translation ->
                onSpeakTranslationClick(translation)
            }
        }
    }

    LaunchedEffect(anchoredDraggableState.targetValue) {
        val target = anchoredDraggableState.targetValue
        if (anchoredDraggableState.offset.isNaN()) return@LaunchedEffect

        if (cardState == CardState.Answer && (target == DragAnchors.Know || target == DragAnchors.DontKnow)) {
            when (target) {
                DragAnchors.Know -> onCardSwipedRight()
                DragAnchors.DontKnow -> onCardSwipedLeft()
                else -> { }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Звичайна Практика",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Spacer(modifier = Modifier.weight(0.5f))

        if (word != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(3f / 2f)
                    .weight(2f)
                    .offset {
                        if (anchoredDraggableState.offset.isNaN()) {
                            IntOffset.Zero
                        } else {
                            IntOffset(anchoredDraggableState.requireOffset().roundToInt(), 0)
                        }
                    }
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = Orientation.Horizontal,
                        enabled = cardState == CardState.Answer
                    )
                    .clickable(enabled = cardState == CardState.Prompt) {
                        onFlipCard()
                    },
                contentAlignment = Alignment.Center
            ) {
                PracticeCard(
                    word = word,
                    cardState = cardState,
                    promptContentType = promptContentType,
                    onReplayTranslationClick = { word.translation?.takeIf { it.isNotBlank() }?.let { onSpeakTranslationClick(it) } },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = when(cardState) {
                    CardState.Prompt -> "Клікніть картку для перекладу"
                    CardState.Answer -> "Свайпніть: Вліво - Важко (2), Вправо - Легко (5)"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.weight(0.5f))
            Text(
                text = "Слів для практики поки немає.\nПеревірте список слів або спробуйте пізніше.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}