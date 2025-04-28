package com.example.wordboost.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.util.Log

class CacheClearWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {


    private val OLD_ENTRY_THRESHOLD_DAYS = 6
    private val OLD_ENTRY_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(OLD_ENTRY_THRESHOLD_DAYS.toLong())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("CacheClearWorker", "Запуск очищення старих записів кешу перекладів...")

            val database = AppDatabase.getInstance(applicationContext)
            val cacheDao = database.cacheDao()

            val currentTime = System.currentTimeMillis()
            val thresholdTime = currentTime - OLD_ENTRY_THRESHOLD_MILLIS

            val deletedRowCount = cacheDao.clearOldEntries(thresholdTime)

            Log.d("CacheClearWorker", "Очищення старих записів кешу завершено. Видалено $deletedRowCount записів (старше $OLD_ENTRY_THRESHOLD_DAYS днів).")

            Result.success()
        } catch (e: Exception) {
            Log.e("CacheClearWorker", "Помилка під час очищення старих записів кешу", e)
            Result.failure()
        }
    }
}