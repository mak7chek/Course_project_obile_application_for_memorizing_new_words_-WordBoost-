package com.example.wordboost.data.model

import kotlin.math.max

/**
 * SM‑2 алгоритм для інтервального повторення.
 */
object PracticeUtils {
    /**
     * Розрахунок нового repetition, easiness і next interval згідно SM‑2.
     * @param oldRep   поточні повторення
     * @param oldEF    поточний коефіцієнт легкості
     * @param oldInt   останній інтервал (мс)
     * @param quality  оцінка відповіді 0..5 (5 — ідеально)
     * @return Triple<newRep, newEF, newInterval>
     */
    fun sm2(
        oldRep: Int,
        oldEF: Float,
        oldInt: Long,
        quality: Int
    ): Triple<Int, Float, Long> {
        // мінімальне EF
        val minEF = 1.3f
        // новий EF
        val newEF = max(
            minEF,
            oldEF + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        )
        // новий repetition
        val newRep = if (quality < 3) 0 else oldRep + 1
        // новий інтервал
        val newInterval: Long = when (newRep) {
            0 -> 1 * 60 * 1000L             // 1 хв
            1 -> 1 * 60 * 1000L             // 1 хв
            2 -> 6 * 60 * 1000L             // 6 хв
            else -> (oldInt * newEF).toLong() // далі EF * oldInt
        }
        return Triple(newRep, newEF, newInterval)
    }

    /**
     * Статус за repetition
     */
    fun determineStatus(repetition: Int): String = when {
        repetition == 0      -> "new"
        repetition == 1      -> "learning"
        repetition in 2..4   -> "review"
        else                  -> "mastered"
    }
}