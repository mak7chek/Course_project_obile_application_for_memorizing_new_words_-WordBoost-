package com.example.wordboost.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.util.Log

// Worker для очищення старих записів кешу перекладів
class CacheClearWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // Визначаємо, що вважається "старим" записом
    // Наприклад, видаляємо записи, які не оновлювалися/використовувалися 30 днів
    private val OLD_ENTRY_THRESHOLD_DAYS = 30
    private val OLD_ENTRY_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(OLD_ENTRY_THRESHOLD_DAYS.toLong())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("CacheClearWorker", "Запуск очищення старих записів кешу перекладів...")

            val database = AppDatabase.getInstance(applicationContext)
            val cacheDao = database.cacheDao()

            // Вираховуємо відмітку часу, старіші за яку записи потрібно видалити
            val currentTime = System.currentTimeMillis()
            val thresholdTime = currentTime - OLD_ENTRY_THRESHOLD_MILLIS

            // !!! Викликаємо новий метод для видалення старих записів !!!
            val deletedRowCount = cacheDao.clearOldEntries(thresholdTime)

            Log.d("CacheClearWorker", "Очищення старих записів кешу завершено. Видалено $deletedRowCount записів (старше $OLD_ENTRY_THRESHOLD_DAYS днів).")

            Result.success()
        } catch (e: Exception) {
            Log.e("CacheClearWorker", "Помилка під час очищення старих записів кешу", e)
            Result.failure() // Або Result.retry()
        }
    }
}