package com.example.wordboost.ui.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordboost.data.model.SharedSetWordItem
import kotlin.math.abs
import kotlin.math.roundToInt

fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt()) // Краще використовувати roundToInt
@Composable
fun SharedWordDisplayCard(
    wordItem: SharedSetWordItem,
    showTranslation: Boolean,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onReplaySound: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetY by remember(wordItem.id) { mutableStateOf(0f) }
    val dragProgress = remember(wordItem.id) { mutableStateOf(0f) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val swipeThresholdPx = screenHeightPx / 4.5f // Поріг свайпу - чверть висоти екрану

    Card(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1.6f)
            // !!! ВИПРАВЛЕНО MODIFIER.OFFSET !!!
            .offset { IntOffset(x = 0, y = offsetY.roundToInt()) }
            .pointerInput(wordItem.id) {
                detectVerticalDragGestures(
                    onDragStart = {
                        offsetY = 0f // Скидаємо для нового жесту на цій картці
                        dragProgress.value = 0f
                        Log.d("SharedWordDisplayCard", "onDragStart, offsetY reset to 0")
                    },
                    onDragEnd = {
                        val currentOffsetY = offsetY
                        Log.d("SharedWordDisplayCard", "onDragEnd, currentOffsetY: $currentOffsetY, threshold: $swipeThresholdPx")
                        val swipedUp = currentOffsetY < -swipeThresholdPx
                        val swipedDown = currentOffsetY > swipeThresholdPx

                        if (swipedUp) {
                            Log.d("SharedWordDisplayCard", "Swipe UP detected")
                            onSwipeUp()
                        } else if (swipedDown) {
                            Log.d("SharedWordDisplayCard", "Swipe DOWN detected")
                            onSwipeDown()
                        } else {
                            // Свайп недостатньо сильний, повертаємо картку плавно (або миттєво)
                            Log.d("SharedWordDisplayCard", "Swipe not strong enough, snapping back.")
                            offsetY = 0f // Миттєве повернення
                            dragProgress.value = 0f
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount
                        dragProgress.value = (offsetY / swipeThresholdPx).coerceIn(-1.5f, 1.5f)
                    }
                )
            }
            .graphicsLayer(
                alpha = 1f - (abs(dragProgress.value * 0.6f)).coerceAtMost(0.6f),
                rotationZ = dragProgress.value * 10f
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        // Переконайся, що MaterialTheme.shapes.large існує і правильно визначений
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = wordItem.originalText,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp), // Тепер sp має працювати
                        textAlign = TextAlign.Center,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onReplaySound, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.VolumeUp, "Озвучити слово", modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                val translationAlpha by animateFloatAsState(
                    targetValue = if (showTranslation) 1f else 0f,
                    animationSpec = tween(durationMillis = 300), label = "translationAlpha"
                )
                Text(
                    text = if (showTranslation || wordItem.translationText.isBlank()) wordItem.translationText else "...",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp), // Тепер sp має працювати
                    color = if (showTranslation) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.alpha(translationAlpha),
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
            }
        }
    }
}
