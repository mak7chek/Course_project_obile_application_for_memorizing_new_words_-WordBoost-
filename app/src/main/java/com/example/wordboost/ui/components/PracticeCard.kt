package com.example.wordboost.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Word
// Імпорт noRippleClickable з того ж пакету components
// import com.example.wordboost.ui.components.noRippleClickable
// Імпорт CardContent з того ж пакету components
// import com.example.wordboost.ui.components.CardContent
import kotlin.math.absoluteValue

@Composable
fun PracticeCard(
    word: Word,
    isFlipped: Boolean,
    isReverse: Boolean,
    onFlip: () -> Unit,
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            val frontText = if (!isReverse) word.text else word.translation
            val backTextPrimary = if (!isReverse) word.text else word.translation
            val backTextSecondary = if (!isReverse) word.translation else word.text

            if (rotationY.absoluteValue % 360f <= 90f) {
                CardContent(
                    title = frontText,
                    modifier = Modifier.graphicsLayer { alpha = frontOpacity }
                )
            }
            else {
                CardContent(
                    title = backTextPrimary,
                    subtitle = backTextSecondary,
                    modifier = Modifier.graphicsLayer {
                        this.rotationY = 180f
                        alpha = backOpacity
                    }
                )
            }
        }
    }
}