package com.example.wordboost.data.util

import kotlin.math.max
import java.util.concurrent.TimeUnit

object PracticeUtils {

    private val ONE_MINUTE_MS = TimeUnit.MINUTES.toMillis(1)
    private val SIX_MINUTES_MS = TimeUnit.MINUTES.toMillis(6)
    private val ONE_DAY_MS = TimeUnit.DAYS.toMillis(1)
    private val MIN_EF = 1.3f

    fun sm2(
        oldRep: Int,
        oldEF: Float,
        oldInt: Long,
        quality: Int
    ): Triple<Int, Float, Long> {

        val newEF = max(
            MIN_EF,
            oldEF + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        )

        var newRep: Int
        var newInterval: Long

        if (quality < 3) {
            newRep = 0
            newInterval = ONE_MINUTE_MS
        } else {
            newRep = oldRep + 1

            newInterval = when (newRep) {
                1 -> ONE_MINUTE_MS
                2 -> SIX_MINUTES_MS
                else -> {
                    if (oldInt <= SIX_MINUTES_MS) {
                        ONE_DAY_MS
                    } else {
                        (oldInt * newEF).toLong()
                    }
                }
            }
        }
        if (newInterval < ONE_MINUTE_MS && newRep > 0) {
            newInterval = ONE_MINUTE_MS
        }

        return Triple(newRep, newEF, newInterval)
    }

    fun determineStatus(repetition: Int, interval: Long): String = when {
        repetition == 0 && interval <= ONE_MINUTE_MS -> "new"
        repetition > 0 && interval <= SIX_MINUTES_MS -> "learning"
        repetition > 0 && interval > SIX_MINUTES_MS && interval <= TimeUnit.DAYS.toMillis(21) -> "review"
        repetition > 0 && interval > TimeUnit.DAYS.toMillis(21) -> "mastered"
        else -> "new"
    }


    fun isWordConsideredLearned(status: String): Boolean {
        return status == "mastered" || status == "review"
    }

    fun calculateProgress(repetition: Int, interval: Long): Float {
        val currentStatus = determineStatus(repetition, interval)
        val progress: Float = when (currentStatus) {
            "mastered" -> 1.0f
            "review" -> {
                if (interval <= ONE_DAY_MS * 2) 0.7f
                else if (interval <= TimeUnit.DAYS.toMillis(7)) 0.85f
                else 0.95f
            }
            "learning" -> {
                if (repetition == 1 && interval <= ONE_MINUTE_MS) 0.2f
                else if (repetition == 2 && interval <= SIX_MINUTES_MS) 0.4f
                else 0.6f
            }
            "new" -> if (repetition == 0 && interval <= ONE_MINUTE_MS) 0.1f else 0.05f
            else -> 0.05f
        }
        return kotlin.math.min(progress, 1.0f)
    }
}