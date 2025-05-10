// com.example.wordboost.ui.screens.MainScreen.kt
package com.example.wordboost.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text // Додано, якщо буде використовуватися Text напряму
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.PracticeRepository
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.data.tts.TextToSpeechService
import com.example.wordboost.viewmodel.CreateSetViewModel
import com.example.wordboost.viewmodel.CreateSetViewModelFactory
import com.example.wordboost.viewmodel.EditWordViewModelFactory
import com.example.wordboost.viewmodel.PracticeViewModelFactory
import com.example.wordboost.viewmodel.TranslateViewModelFactory // Можливо, знадобиться, якщо TranslateScreen приймає factory
import com.example.wordboost.viewmodel.WordListViewModelFactory
import com.example.wordboost.viewmodel.SetsViewModelFactory // Додано factory для SetsScreen
import com.example.wordboost.ui.screens.createset.CreateSetScreen

enum class TopLevelScreenState {
    Loading,
    AuthFlow,
    AuthenticatedApp,
    TranslateWordFullScreen,
    PracticeSessionFullScreen,
    WordListFullScreen,
    EditWordFullScreen,
    CreateSetWizardFullScreen
}

enum class AuthSubState {
    AuthChoice, Login, Register
}

@Composable
fun MainScreen(
    authRepo: AuthRepository,
    practiceRepo: PracticeRepository,
    firebaseRepo: FirebaseRepository,
    translationRepo: TranslationRepository,
    ttsService: TextToSpeechService
) {
    var currentTopLevelScreenState by remember { mutableStateOf(TopLevelScreenState.Loading) }
    var currentAuthSubState by remember { mutableStateOf(AuthSubState.AuthChoice) }
    var editingWordIdForFullScreen by remember { mutableStateOf<String?>(null) }

    // --- Factories ---
    val practiceViewModelFactory = remember(practiceRepo, ttsService, authRepo) {
        PracticeViewModelFactory(repository = practiceRepo, ttsService = ttsService, authRepository = authRepo)
    }
    val wordListViewModelFactory = remember(firebaseRepo, authRepo, ttsService) {
        WordListViewModelFactory(repository = firebaseRepo, authRepository = authRepo, ttsService = ttsService)
    }
    val editWordViewModelFactoryBuilder = remember(firebaseRepo) {
        // Приймає wordId і повертає Factory
        { wordId: String? -> EditWordViewModelFactory(repository = firebaseRepo, wordId = wordId) }
    }
    // translateViewModelFactory тут не створюється, оскільки TranslateScreen напряму приймає репозиторії.
    // Якщо TranslateScreen буде використовувати ViewModel, тут потрібно буде створити factory.
    val createSetViewModelFactory = remember(firebaseRepo, translationRepo, authRepo) {
        CreateSetViewModelFactory(firebaseRepository = firebaseRepo, translationRepository = translationRepo, authRepository = authRepo)
    }
    val setsViewModelFactory = remember(firebaseRepo, authRepo) {
        SetsViewModelFactory(firebaseRepository = firebaseRepo, authRepository = authRepo)
    }

    LaunchedEffect(key1 = authRepo) { // key1 для уникнення попередження, можна Unit, якщо authRepo не змінюється
        Log.d("MainScreen", "Auth state listener collection started.")
        authRepo.getAuthState().collect { user ->
            val previousState = currentTopLevelScreenState
            Log.d("MainScreen", "Auth state changed: User UID = ${user?.uid}, Verified = ${user?.isEmailVerified}. Previous TopLevelState = $previousState")
            if (user != null && user.isEmailVerified) {
                Log.d("MainScreen", "User authenticated and verified.")
                if (previousState == TopLevelScreenState.Loading || previousState == TopLevelScreenState.AuthFlow) {
                    currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                    Log.d("MainScreen", "Transitioning to AuthenticatedApp")
                } else {
                    Log.d("MainScreen", "User already in an authenticated session state ($previousState), no state change needed here.")
                }
            } else if (user != null && !user.isEmailVerified) {
                Log.d("MainScreen", "User authenticated but NOT verified. Directing to AuthFlow/Login.")
                currentTopLevelScreenState = TopLevelScreenState.AuthFlow
                currentAuthSubState = AuthSubState.Login // Направляємо на логін, де може бути повідомлення про верифікацію
            } else { // user is null
                Log.d("MainScreen", "User is null (logged out). Directing to AuthFlow/AuthChoice.")
                currentTopLevelScreenState = TopLevelScreenState.AuthFlow
                currentAuthSubState = AuthSubState.AuthChoice
            }
            Log.d("MainScreen", "TopLevelScreenState updated to: $currentTopLevelScreenState, AuthSubState: $currentAuthSubState")
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (currentTopLevelScreenState) {
            TopLevelScreenState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Log.d("MainScreen", "Showing Loading state")
                }
            }
            TopLevelScreenState.AuthFlow -> {
                Log.d("MainScreen", "Showing AuthFlow, subState: $currentAuthSubState")
                when (currentAuthSubState) {
                    AuthSubState.AuthChoice -> AuthChoiceScreen(
                        onLoginClick = { currentAuthSubState = AuthSubState.Login },
                        onRegisterClick = { currentAuthSubState = AuthSubState.Register }
                    )
                    AuthSubState.Login -> LoginScreen( // Переконайся, що LoginScreen імпортовано або в цьому файлі
                        authRepo = authRepo,
                        onSuccess = { /* Стан зміниться через LaunchedEffect */ },
                        onBack = { currentAuthSubState = AuthSubState.AuthChoice },
                        onNavigateToRegister = { currentAuthSubState = AuthSubState.Register }
                    )
                    AuthSubState.Register -> RegisterScreen( // Переконайся, що RegisterScreen імпортовано
                        authRepo = authRepo,
                        onRegistrationSuccess = { currentAuthSubState = AuthSubState.Login },
                        onBack = { currentAuthSubState = AuthSubState.AuthChoice }
                    )
                }
            }
            TopLevelScreenState.AuthenticatedApp -> {
                Log.d("MainScreen", "Showing AuthenticatedAppScaffold")
                AuthenticatedAppScaffold(
                    onNavigateToTranslate = { currentTopLevelScreenState = TopLevelScreenState.TranslateWordFullScreen },
                    onNavigateToPractice = { currentTopLevelScreenState = TopLevelScreenState.PracticeSessionFullScreen },
                    onNavigateToWordList = { currentTopLevelScreenState = TopLevelScreenState.WordListFullScreen },
                    onNavigateToCreateSet = { currentTopLevelScreenState = TopLevelScreenState.CreateSetWizardFullScreen },
                    onLogoutClick = { authRepo.logout() },
                    onNavigateToBrowseSet = { setId ->
                        Log.d("MainScreen", "Navigate to browse set ID: $setId (TODO: Implement this navigation)")
                        // Тут буде логіка переходу на екран перегляду конкретного набору,
                        // можливо, встановлення editingWordIdForFullScreen = setId та новий TopLevelScreenState.
                    },
                    wordListViewModelFactory = wordListViewModelFactory,
                    setsViewModelFactory = setsViewModelFactory
                )
            }
            TopLevelScreenState.TranslateWordFullScreen -> {
                Log.d("MainScreen", "Showing TranslateWordFullScreen")
                TranslateScreen( // Переконайся, що TranslateScreen імпортовано
                    firebaseRepo = firebaseRepo,
                    translationRepo = translationRepo,
                    onBack = { currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp }
                )
            }
            TopLevelScreenState.PracticeSessionFullScreen -> {
                Log.d("MainScreen", "Showing PracticeSessionFullScreen")
                PracticeScreen( // Переконайся, що PracticeScreen імпортовано
                    factory = practiceViewModelFactory,
                    onBack = { currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp }
                )
            }
            TopLevelScreenState.WordListFullScreen -> {
                Log.d("MainScreen", "Showing WordListFullScreen")
                WordListScreen( // Переконайся, що WordListScreen імпортовано
                    repository = firebaseRepo,
                    ttsService = ttsService,
                    authRepository = authRepo,
                    onWordEdit = { wordId ->
                        editingWordIdForFullScreen = wordId
                        currentTopLevelScreenState = TopLevelScreenState.EditWordFullScreen
                    },
                    onBack = { currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp }
                )
            }
            TopLevelScreenState.EditWordFullScreen -> {
                Log.d("MainScreen", "Showing EditWordFullScreen for wordId: $editingWordIdForFullScreen")
                EditWordScreen( // Переконайся, що EditWordScreen імпортовано
                    wordId = editingWordIdForFullScreen,
                    factory = editWordViewModelFactoryBuilder(editingWordIdForFullScreen),
                    onBack = {
                        editingWordIdForFullScreen = null
                        currentTopLevelScreenState = TopLevelScreenState.WordListFullScreen
                    }
                )
            }
            TopLevelScreenState.CreateSetWizardFullScreen -> {
                Log.d("MainScreen", "Showing CreateSetWizardFullScreen")
                val createSetViewModel: CreateSetViewModel = viewModel(factory = createSetViewModelFactory)
                CreateSetScreen( // Цей виклик тепер має працювати, якщо CreateSetScreen імпортовано
                    viewModel = createSetViewModel,
                    onCloseOrNavigateBack = {
                        createSetViewModel.resetAllState()
                        currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                    }
                )
            }
        }
    }
}