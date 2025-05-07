package com.example.wordboost

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.local.AppDatabase
import com.example.wordboost.data.local.CacheClearWorker
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.ui.screens.MainScreen
import com.example.wordboost.translation.RealTranslator
import com.example.wordboost.ui.theme.Course_project_obile_application_for_memorizing_new_words_WordBoostTheme
import com.google.firebase.FirebaseApp
import java.util.concurrent.TimeUnit
import com.example.wordboost.data.tts.TextToSpeechService

class MainActivity : ComponentActivity() {

    private val CACHE_CLEAR_WORK_NAME = "CacheClearWork"
    private val REPEAT_INTERVAL_DAYS: Long = 6

    private lateinit var firebaseRepo: FirebaseRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var practiceRepo: PracticeRepository
    private lateinit var appDatabase: AppDatabase
    private lateinit var realTranslator: RealTranslator
    private lateinit var translationRepo: TranslationRepository
    private lateinit var ttsService: TextToSpeechService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        scheduleCacheClearWork()
        authRepo = AuthRepository()
        firebaseRepo = FirebaseRepository(authRepo)
        practiceRepo = PracticeRepository(firebaseRepo)

        appDatabase = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "wordboost-db")
            .fallbackToDestructiveMigration(true)
            .build()
        val cacheDao = appDatabase.cacheDao()

        realTranslator = RealTranslator()

        translationRepo = TranslationRepository(firebaseRepo, cacheDao, realTranslator)

        ttsService = TextToSpeechService(applicationContext)

        setContent {
            Course_project_obile_application_for_memorizing_new_words_WordBoostTheme { // Ваша тема
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        authRepo = authRepo,
                        practiceRepo = practiceRepo,
                        firebaseRepo = firebaseRepo,
                        translationRepo = translationRepo,
                        ttsService = ttsService
                    )
                }
            }
        }
    }

    private fun scheduleCacheClearWork() {
        val cacheClearRequest = PeriodicWorkRequestBuilder<CacheClearWorker>(
            repeatInterval = REPEAT_INTERVAL_DAYS,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            CACHE_CLEAR_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cacheClearRequest
        )
        Log.d("WorkManager", "$CACHE_CLEAR_WORK_NAME заплановано з політикою KEEP на повторення кожні $REPEAT_INTERVAL_DAYS днів.")
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsService.shutdown()
        // appDatabase.close()
    }
}
