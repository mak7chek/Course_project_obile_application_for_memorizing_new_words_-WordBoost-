package com.example.wordboost.data.util

import org.junit.Assert.* // Для assertEquals, assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class PracticeUtilsTest {

    private val ONE_MINUTE_MS_TEST = TimeUnit.MINUTES.toMillis(1)
    private val SIX_MINUTES_MS_TEST = TimeUnit.MINUTES.toMillis(6)
    private val ONE_DAY_MS_TEST = TimeUnit.DAYS.toMillis(1)
    private val MIN_EF_TEST = 1.3f

    @Test
    fun `sm2 - first repetition, correct answer quality 4`() {
        val oldRep = 0
        val oldEF = 2.5f
        val oldInt = 0L
        val quality = 4 // Правильна відповідь

        val (newRep, newEF, newInterval) = PracticeUtils.sm2(oldRep, oldEF, oldInt, quality)

        assertEquals("Repetition should be 1", 1, newRep)
        assertTrue("EF ($newEF) should be >= MIN_EF ($MIN_EF_TEST)", newEF >= MIN_EF_TEST)
        assertEquals("Interval should be 1 minute", ONE_MINUTE_MS_TEST, newInterval)
    }

    @Test
    fun `sm2 - second repetition, correct answer quality 5`() {
        val oldRep = 1
        val oldEF = 2.6f // Припустимо, EF змінився
        val oldInt = ONE_MINUTE_MS_TEST
        val quality = 5 // Відмінна відповідь

        val (newRep, newEF, newInterval) = PracticeUtils.sm2(oldRep, oldEF, oldInt, quality)

        assertEquals("Repetition should be 2", 2, newRep)
        assertTrue("EF ($newEF) should be >= previous EF or MIN_EF", newEF >= oldEF || (oldEF == MIN_EF_TEST && newEF == MIN_EF_TEST) || newEF > oldEF)
        assertTrue("EF ($newEF) should be >= MIN_EF ($MIN_EF_TEST)", newEF >= MIN_EF_TEST)
        assertEquals("Interval should be 6 minutes", SIX_MINUTES_MS_TEST, newInterval)
    }

    @Test
    fun `sm2 - third repetition, correct answer quality 3 (after learning steps)`() {
        val oldRep = 2
        val oldEF = 2.5f
        val oldInt = SIX_MINUTES_MS_TEST
        val quality = 3

        val (newRep, newEF, newInterval) = PracticeUtils.sm2(oldRep, oldEF, oldInt, quality)

        assertEquals("Repetition should be 3", 3, newRep)
        assertTrue("EF ($newEF) should be >= MIN_EF ($MIN_EF_TEST)", newEF >= MIN_EF_TEST)
        assertEquals("Interval should be 1 day", ONE_DAY_MS_TEST, newInterval)
    }

    @Test
    fun `sm2 - subsequent repetition, correct answer quality 4`() {
        val oldRep = 3
        val oldEF = 2.5f
        val oldInt = ONE_DAY_MS_TEST
        val quality = 4

        val (newRep, newEF, newInterval) = PracticeUtils.sm2(oldRep, oldEF, oldInt, quality)

        assertEquals("Repetition should be 4", 4, newRep)
        assertTrue("EF ($newEF) should be >= MIN_EF ($MIN_EF_TEST)", newEF >= MIN_EF_TEST)
        assertEquals("Interval should be oldInterval * newEF", (oldInt * newEF).toLong(), newInterval)
    }

    @Test
    fun `sm2 - incorrect answer quality 0, resets progress`() {
        val oldRep = 5
        val oldEF = 2.0f
        val oldInt = TimeUnit.DAYS.toMillis(10)
        val quality = 0

        val (newRep, _, newInterval) = PracticeUtils.sm2(oldRep, oldEF, oldInt, quality)

        assertEquals("Repetition should reset to 0", 0, newRep)
        assertEquals("Interval should reset to 1 minute", ONE_MINUTE_MS_TEST, newInterval)
    }

    @Test
    fun `sm2 - incorrect answer quality 2, resets progress`() {
        val oldRep = 3
        val oldEF = 2.3f
        val oldInt = ONE_DAY_MS_TEST
        val quality = 2

        val (newRep, _, newInterval) = PracticeUtils.sm2(oldRep, oldEF, oldInt, quality)

        assertEquals("Repetition should reset to 0", 0, newRep)
        assertEquals("Interval should reset to 1 minute", ONE_MINUTE_MS_TEST, newInterval)
    }

    @Test
    fun `sm2 - easiness factor does not go below MIN_EF with low quality answers`() {
        var currentEF = MIN_EF_TEST + 0.2f
        val oldRep = 3
        val oldInt = ONE_DAY_MS_TEST

        for (i in 1..5) {
            val (_, newEF, _) = PracticeUtils.sm2(oldRep, currentEF, oldInt, 3)
            currentEF = newEF
            assertTrue("EF ($currentEF) should be >= MIN_EF ($MIN_EF_TEST)", currentEF >= MIN_EF_TEST)
        }
        assertEquals("EF should eventually reach MIN_EF", MIN_EF_TEST, currentEF, 0.01f)
    }
}