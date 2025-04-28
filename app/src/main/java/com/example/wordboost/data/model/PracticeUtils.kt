package com.example.wordboost.data.model

import kotlin.math.max


object PracticeUtils {

    fun sm2(
        oldRep: Int,
        oldEF: Float,
        oldInt: Long,
        quality: Int
    ): Triple<Int, Float, Long> {

        val minEF = 1.3f
        val newEF = max(
            minEF,
            oldEF + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        )
        val newRep = if (quality < 3) 0 else oldRep + 1
        val newInterval: Long = when (newRep) {
            0 -> 1 * 60 * 1000L             // 1 хв
            1 -> 1 * 60 * 1000L             // 1 хв
            2 -> 6 * 60 * 1000L             // 6 хв
            else -> (oldInt * newEF).toLong() // далі EF * oldInt
        }
        return Triple(newRep, newEF, newInterval)
    }

    fun determineStatus(repetition: Int): String = when {
        repetition == 0      -> "new"
        repetition == 1      -> "learning"
        repetition in 2..4   -> "review"
        else                  -> "mastered"
    }
}