// ui.components/BatteryProgressIndicator.kt
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
    progress: Float, // Прогрес від 0.0 до 1.0
    modifier: Modifier = Modifier,
    strokeWidth: Float = 3f // Товщина ліній індикатора
) {
    // Визначаємо кольори індикатора залежно від прогресу
    val colorLow = Color.Red
    val colorMedium = Color.Yellow
    val colorHigh = Color.Green

    val thresholdLow = 0.15f // Поріг для низького прогресу
    val thresholdMedium = 0.40f // Поріг для середнього прогресу

    val indicatorColor = when {
        progress <= thresholdLow -> colorLow
        progress <= thresholdMedium -> colorMedium
        else -> colorHigh
    }

    // Розміри батареї (якщо потрібно змінити загальний розмір компонента, використовуйте Modifier.size ззовні)
    val batteryHeight = 48f
    val batteryWidth = 32f
    val headHeight = 8f
    val headWidth = 10f
    val cornerRadius = 3f


    Canvas(modifier = modifier.size(batteryWidth.dp, (headHeight + batteryHeight).dp)) {
        // Розміри елементів батареї відносно Canvas
        val headTop = 0f
        val headLeft = (size.width - headWidth) / 2
        val headBottom = headTop + headHeight
        val headRight = headLeft + headWidth

        val bodyTop = headBottom
        val bodyLeft = (size.width - batteryWidth) / 2
        val bodyRight = bodyLeft + batteryWidth
        val bodyBottom = bodyTop + batteryHeight

        // Малюємо голову батареї (обрис)
        drawRoundRect(
            color = Color.Gray, // Колір обрису голови
            topLeft = Offset(headLeft, headTop),
            size = Size(headWidth, headHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth / 2) // Товщина обрису
        )

        // Малюємо тіло батареї (обрис)
        drawRoundRect(
            color = Color.Gray, // Колір обрису тіла
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(batteryWidth, batteryHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth) // Товщина обрису
        )

        // Розраховуємо розміри та позицію індикатора прогресу всередині тіла батареї
        val indicatorHeight = max(0f, (batteryHeight - strokeWidth * 2) * progress) // Висота індикатора залежить від прогресу
        val indicatorWidth = batteryWidth - strokeWidth * 2 // Ширина індикатора
        val indicatorLeft = bodyLeft + strokeWidth // Ліва координата індикатора
        // Верхня координата індикатора (зміщуємо вниз зі збільшенням прогресу)
        val indicatorTop = bodyTop + strokeWidth + (batteryHeight - strokeWidth * 2 - indicatorHeight)

        // Малюємо індикатор прогресу
        drawRoundRect(
            color = indicatorColor, // Колір індикатора (залежить від прогресу)
            topLeft = Offset(indicatorLeft, indicatorTop),
            size = Size(indicatorWidth, indicatorHeight),
            // Закруглення кутів індикатора (трохи менше, ніж у обрису)
            cornerRadius = CornerRadius(cornerRadius - strokeWidth / 2, cornerRadius - strokeWidth / 2)
        )
    }
}