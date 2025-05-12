package com.example.wordboost.data.util

import kotlin.math.max
import java.util.concurrent.TimeUnit

object PracticeUtils {

    // Константи для інтервалів у мілісекундах
    private val ONE_MINUTE_MS = TimeUnit.MINUTES.toMillis(1)
    private val SIX_MINUTES_MS = TimeUnit.MINUTES.toMillis(6)
    private val ONE_DAY_MS = TimeUnit.DAYS.toMillis(1)
    private val MIN_EF = 1.3f

    /**
     * Розраховує нові параметри SM-2 для слова на основі якості відповіді.
     * (Ваш модифікований варіант SM-2)
     *
     * @param oldRep Поточна кількість успішних послідовних повторень.
     * @param oldEF Поточний коефіцієнт легкості (Easiness Factor).
     * @param oldInt Попередній інтервал між повтореннями (у мілісекундах).
     * @param quality Оцінка якості відповіді користувачем (0-5).
     * @return Потрійний результат: (новий лічильник повторень, новий коефіцієнт легкості, новий інтервал у мілісекундах).
     */
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
            // Відповідь невдала - повертаємо на перший крок навчання
            newRep = 0
            newInterval = ONE_MINUTE_MS // 1 хвилина
        } else {
            // Відповідь вдала
            newRep = oldRep + 1

            newInterval = when (newRep) {
                1 -> ONE_MINUTE_MS    // Перший успіх -> 1 хвилина
                2 -> SIX_MINUTES_MS   // Другий успіх -> 6 хвилин
                else -> {
                    // Наступні повторення
                    if (oldInt <= SIX_MINUTES_MS) {
                        ONE_DAY_MS // Перший інтервал після кроків навчання - 1 день
                    } else {
                        (oldInt * newEF).toLong() // Далі розраховуємо
                    }
                }
            }
        }
        // Забезпечуємо мінімальний інтервал для розрахованих інтервалів (крім скидання в 0)
        if (newInterval < ONE_MINUTE_MS && newRep > 0) {
            newInterval = ONE_MINUTE_MS
        }

        return Triple(newRep, newEF, newInterval)
    }

    /**
     * Статус слова за repetition та interval.
     */
    fun determineStatus(repetition: Int, interval: Long): String = when {
        repetition == 0 && interval <= ONE_MINUTE_MS -> "learning" // Нове або після помилки, на короткому інтервалі
        repetition > 0 && interval <= SIX_MINUTES_MS -> "learning" // На кроках навчання
        repetition > 0 && interval > SIX_MINUTES_MS && interval <= TimeUnit.DAYS.toMillis(21) -> "review" // На етапі повторення (до 3 тижнів)
        repetition > 0 && interval > TimeUnit.DAYS.toMillis(21) -> "mastered" // "Опановане" (інтервал більше 3 тижнів)
        else -> "new" // Можливо, для слів, які ще не переглядалися взагалі (nextReview=0, rep=0)
    }

    /**
     * Розраховує прогрес слова для UI (напр., для батарейки).
     * Базуємо на repetition count та інтервалі, щоб показати перехід від коротких до довгих інтервалів.
     * @return Прогрес від 0.0 до 1.0
     */
    fun calculateProgress(repetition: Int, interval: Long): Float {
        // Приклад: мапимо перші кроки навчання (rep 0, 1, 2) та перший довгий інтервал (1 день)
        // до певного прогресу, а далі - зростання інтервалу.
        val progress: Float = when {
            repetition == 0 && interval <= ONE_MINUTE_MS -> 0.1f 
            repetition == 1 && interval <= ONE_MINUTE_MS -> 0.2f
            repetition == 2 && interval <= SIX_MINUTES_MS -> 0.4f
            repetition > 2 && interval <= ONE_DAY_MS -> 0.6f // Перехід на 1 день
            repetition > 2 && interval > ONE_DAY_MS && interval <= TimeUnit.DAYS.toMillis(7) -> 0.7f // До тижня
            repetition > 2 && interval > TimeUnit.DAYS.toMillis(7) && interval <= TimeUnit.DAYS.toMillis(30) -> 0.85f // До місяця
            else -> 1.0f
        }
        return minOf(progress, 1.0f)
    }
}