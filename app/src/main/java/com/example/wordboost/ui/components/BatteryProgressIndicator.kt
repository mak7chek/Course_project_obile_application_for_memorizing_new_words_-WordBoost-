package com.example.wordboost.ui.components // Ваш пакет
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun BatteryProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 3f
) {

    val colorLow = Color.Red
    val colorMedium = Color.Yellow
    val colorHigh = Color.Green

    val thresholdLow = 0.15f
    val thresholdMedium = 0.40f

    val indicatorColor = when {
        progress <= thresholdLow -> colorLow
        progress <= thresholdMedium -> colorMedium
        else -> colorHigh
    }

    val batteryHeight = 48f
    val batteryWidth = 32f
    val headHeight = 8f
    val headWidth = 10f
    val cornerRadius = 3f

    Canvas(modifier = modifier.size(batteryWidth.dp, (headHeight + batteryHeight).dp)) {

        val headTop = 0f
        val headLeft = (size.width - headWidth) / 2
        val headBottom = headTop + headHeight
        val headRight = headLeft + headWidth

        val bodyTop = headBottom
        val bodyLeft = (size.width - batteryWidth) / 2
        drawRoundRect(
            color = Color.Gray,
            topLeft = Offset(headLeft, headTop),
            size = Size(headWidth, headHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth / 2)
        )

        drawRoundRect(
            color = Color.Gray,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(batteryWidth, batteryHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth)
        )
        val indicatorHeight = max(0f, (batteryHeight - strokeWidth * 2) * progress)
        val indicatorWidth = batteryWidth - strokeWidth * 2
        val indicatorLeft = bodyLeft + strokeWidth
        val indicatorTop = bodyTop + strokeWidth + (batteryHeight - strokeWidth * 2 - indicatorHeight)

        drawRoundRect(
            color = indicatorColor,
            topLeft = Offset(indicatorLeft, indicatorTop),
            size = Size(indicatorWidth, indicatorHeight),
            cornerRadius = CornerRadius(cornerRadius - strokeWidth / 2, cornerRadius - strokeWidth / 2)
        )
    }
}