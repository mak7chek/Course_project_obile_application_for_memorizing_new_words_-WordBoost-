package com.example.wordboost

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wordboost.ui.theme.Course_project_obile_application_for_memorizing_new_words_WordBoostTheme
import com.google.firebase.FirebaseApp
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.ui.screens.*

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.wordboost.data.local.CacheClearWorker

enum class AppState {
    Loading,
    UnauthenticatedChoice,
    Login,
    Register,
    AuthenticatedMain,
    AuthenticatedTranslate,
    AuthenticatedPractice
}

class MainActivity : ComponentActivity() {

    // !!! Визначаємо константи для WorkManager !!!
    private val CACHE_CLEAR_WORK_NAME = "CacheClearWork"
    private val REPEAT_INTERVAL_DAYS: Long = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        scheduleCacheClearWork()
        val firebaseRepo = FirebaseRepository()
        val practiceRepo = PracticeRepository(firebaseRepo)
        val authRepo = com.example.wordboost.data.firebase.AuthRepository()

        setContent {
            Course_project_obile_application_for_memorizing_new_words_WordBoostTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(authRepo = authRepo, practiceRepo = practiceRepo)
                }
            }
        }
    }

    private fun scheduleCacheClearWork() {
        val cacheClearRequest = PeriodicWorkRequestBuilder<CacheClearWorker>(
            repeatInterval = REPEAT_INTERVAL_DAYS,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            // Додайте constraints(), якщо потрібно, наприклад:
            // .setConstraints(Constraints.Builder().setRequiresCharging(true).build()) // Запускати лише під час зарядки
            // .setInitialDelay(1, TimeUnit.HOURS) // Затримати перший запуск на 1 годину
            .build()

        // Enqueue - ставимо завдання в чергу на виконання
        // enqueueUniquePeriodicWork - гарантує, що буде лише одне активне завдання з таким ім'ям
        // ExistingPeriodicWorkPolicy.KEEP - якщо завдання вже існує, залишаємо старе і не створюємо нове
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            CACHE_CLEAR_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cacheClearRequest
        )

        Log.d("WorkManager", "$CACHE_CLEAR_WORK_NAME заплановано з політикою KEEP на повторення кожні $REPEAT_INTERVAL_DAYS днів.")
    }
}

@Composable
fun MainScreen(authRepo: com.example.wordboost.data.firebase.AuthRepository, practiceRepo: PracticeRepository) {
    // Стан для керування поточним екраном/станом програми
    var currentAppState by remember { mutableStateOf(AppState.Loading) }

    // Ефект для початкової перевірки аутентифікації
    LaunchedEffect(Unit) {
        // Перевіряємо, чи є поточний користувач і чи його email верифіковано
        val currentUser = authRepo.getCurrentUser()
        currentAppState = if (currentUser != null && currentUser.isEmailVerified) {
            AppState.AuthenticatedMain
        } else {
            AppState.UnauthenticatedChoice
        }
    }

    // Вибираємо, який екран показувати залежно від поточного стану
    when (currentAppState) {
        AppState.Loading -> {
            // Можна показати спінер або екран завантаження
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AppState.UnauthenticatedChoice -> {
            AuthChoiceScreen(
                onLoginClick = { currentAppState = AppState.Login },
                onRegisterClick = { currentAppState = AppState.Register }
            )
        }
        AppState.Login -> {
            LoginScreen(
                authRepo = authRepo,
                onSuccess = { currentAppState = AppState.AuthenticatedMain },
                onBack = { currentAppState = AppState.UnauthenticatedChoice } // Додаємо можливість повернутись
            )
        }
        AppState.Register -> {
            RegisterScreen(
                authRepo = authRepo,
                onRegistrationSuccess = {
                    // Після успішної реєстрації, можливо, переходимо до екрану входу
                    currentAppState = AppState.Login
                },
                onBack = { currentAppState = AppState.UnauthenticatedChoice } // Додаємо можливість повернутись
            )
        }
        AppState.AuthenticatedMain -> {
            AuthenticatedMainScreen( // Нова композитна функція для головного меню після входу
                onTranslateClick = { currentAppState = AppState.AuthenticatedTranslate },
                onPracticeClick = { currentAppState = AppState.AuthenticatedPractice },
                onLogoutClick = {
                    authRepo.logout()
                    currentAppState = AppState.UnauthenticatedChoice // Після виходу переходимо до вибору
                }
            )
        }
        AppState.AuthenticatedTranslate -> {
            TranslateScreen(onBack = { currentAppState = AppState.AuthenticatedMain }) // Додаємо можливість повернутись
        }
        AppState.AuthenticatedPractice -> {
            PracticeScreen(practiceRepo = practiceRepo, onBack = { currentAppState = AppState.AuthenticatedMain }) // Додаємо можливість повернутись
        }
    }
}

@Composable
fun AuthChoiceScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally // Вирівнюємо по центру горизонтально
    ) {
        Text(
            "Ласкаво просимо до WordBoost!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center, // Вирівнюємо текст по центру
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(0.8f) // Кнопка займає 80% ширини
        ) {
            Text("Увійти")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(0.8f) // Кнопка займає 80% ширини
        ) {
            Text("Зареєструватись")
        }
    }
}

@Composable
fun AuthenticatedMainScreen(
    onTranslateClick: () -> Unit,
    onPracticeClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally // Вирівнюємо по центру горизонтально
    ) {
        Text(
            "Привіт у WordBoost!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onTranslateClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Перекласти слово")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPracticeClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Практика")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLogoutClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), // Можна змінити колір для кнопки виходу
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Вийти")
        }
    }
}


