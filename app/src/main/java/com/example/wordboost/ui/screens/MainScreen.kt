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
import com.example.wordboost.ui.screens.BrowseSharedSetScreen
import com.example.wordboost.viewmodel.TranslateViewModelFactory // Можливо, знадобиться, якщо TranslateScreen приймає factory
import com.example.wordboost.viewmodel.WordListViewModelFactory
import com.example.wordboost.viewmodel.SetsViewModelFactory // Додано factory для SetsScreen
import com.example.wordboost.ui.screens.createset.CreateSetScreen
import com.example.wordboost.viewmodel.BrowseSharedSetViewModel
import com.example.wordboost.viewmodel.BrowseSetViewModelFactory

enum class TopLevelScreenState {
    Loading,
    AuthFlow,
    AuthenticatedApp,
    TranslateWordFullScreen,
    PracticeSessionFullScreen,
    WordListFullScreen,
    EditWordFullScreen,
    CreateSetWizardFullScreen,
    BrowseSharedSetFullScreen
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
    var currentSetIdForBrowse by remember { mutableStateOf<String?>(null) }
    var editingSetId by remember { mutableStateOf<String?>(null) } // Для ID набору, що редагується

    // --- Factories ---
    // ... (твій код для factories, він виглядав правильно)
    val practiceViewModelFactory = remember(practiceRepo, ttsService, authRepo) { PracticeViewModelFactory(repository = practiceRepo, ttsService = ttsService, authRepository = authRepo) }
    val wordListViewModelFactory = remember(firebaseRepo, authRepo, ttsService) { WordListViewModelFactory(repository = firebaseRepo, authRepository = authRepo, ttsService = ttsService) }
    val editWordViewModelFactoryBuilder = remember(firebaseRepo) { { wordId: String? -> EditWordViewModelFactory(repository = firebaseRepo, wordId = wordId) } }
    val createSetViewModelFactory = remember(firebaseRepo, translationRepo, authRepo) { CreateSetViewModelFactory(firebaseRepository = firebaseRepo, translationRepository = translationRepo, authRepository = authRepo) }
    val setsViewModelFactory = remember(firebaseRepo, authRepo) { SetsViewModelFactory(firebaseRepository = firebaseRepo, authRepository = authRepo) }
    // Factory для BrowseSharedSetViewModel буде створюватися динамічно нижче
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
                Log.i("MainScreen_Render", "Showing AuthenticatedAppScaffold UI")
                AuthenticatedAppScaffold(
                    onNavigateToTranslate = { currentTopLevelScreenState = TopLevelScreenState.TranslateWordFullScreen },
                    onNavigateToPractice = { currentTopLevelScreenState = TopLevelScreenState.PracticeSessionFullScreen },
                    onNavigateToWordList = { currentTopLevelScreenState = TopLevelScreenState.WordListFullScreen },
                    onNavigateToCreateSet = {
                        editingSetId = null // Скидаємо ID редагування, бо це створення нового
                        currentTopLevelScreenState = TopLevelScreenState.CreateSetWizardFullScreen
                    },
                    onLogoutClick = { authRepo.logout() },
                    onNavigateToBrowseSet = { setId ->
                        Log.i("MainScreen_Nav", "Action to browse set ID: '$setId'. Changing TopLevelScreenState to BrowseSharedSetFullScreen.")
                        currentSetIdForBrowse = setId
                        currentTopLevelScreenState = TopLevelScreenState.BrowseSharedSetFullScreen
                    },
                    // !!! ДОДАНО КОЛБЕК onNavigateToEditSet !!!
                    onNavigateToEditSet = { setIdToEdit ->
                        Log.i("MainScreen_Nav", "Action to EDIT set ID: '$setIdToEdit'. Changing TopLevelScreenState to CreateSetWizardFullScreen for editing.")
                        editingSetId = setIdToEdit // Зберігаємо ID набору, що редагується
                        currentTopLevelScreenState = TopLevelScreenState.CreateSetWizardFullScreen // Йдемо на той самий екран створення/редагування
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
                Log.i("MainScreen_Render", "Showing CreateSetWizardFullScreen. Current editingSetId: $editingSetId")
                // Використовуємо editingSetId як ключ для ViewModel, щоб вона перестворювалася
                // для "створення нового" (коли editingSetId = null) або для редагування конкретного набору.
                val currentCreateEditSetId = editingSetId // Захоплюємо значення для ключа
                val createSetViewModel: CreateSetViewModel = viewModel(
                    key = currentCreateEditSetId ?: "create_new_set_mode", // Унікальний ключ
                    factory = createSetViewModelFactory
                )

                // LaunchedEffect для завантаження даних, якщо це режим редагування
                LaunchedEffect(currentCreateEditSetId, createSetViewModel) { // Залежить від ID та екземпляра ViewModel
                    if (currentCreateEditSetId != null) {
                        Log.d("MainScreen_CreateSet", "editingSetId is '$currentCreateEditSetId', calling loadSetForEditing.")
                        createSetViewModel.loadSetForEditing(currentCreateEditSetId)
                    } else {
                        Log.d("MainScreen_CreateSet", "editingSetId is null, resetting ViewModel for new set creation (if needed).")
                        // createSetViewModel.resetAllState() // Розглянь, чи це потрібно тут,
                        // оскільки новий ключ для viewModel() має створювати новий екземпляр.
                        // resetAllState викликається при onCloseOrNavigateBack.
                    }
                }

                CreateSetScreen(
                    viewModel = createSetViewModel,
                    isEditing = currentCreateEditSetId != null, // Передаємо прапорець режиму редагування
                    onCloseOrNavigateBack = {
                        Log.d("MainScreen_CreateSet", "Closing CreateSetWizard. Resetting ViewModel and editingSetId.")
                        createSetViewModel.resetAllState()
                        editingSetId = null // Дуже важливо скинути ID редагування при виході
                        currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                    }
                )
            }
            TopLevelScreenState.BrowseSharedSetFullScreen -> {
                val setIdForView = currentSetIdForBrowse
                Log.i("MainScreen_Nav", "Composing content for BrowseSharedSetFullScreen with setId: $setIdForView")

                if (setIdForView != null) {
                    Log.d("MainScreen_Nav", "Set ID '$setIdForView' is not null, proceeding to create ViewModel and Screen.")
                    val browseSetViewModelFactory = remember(firebaseRepo, authRepo, ttsService, setIdForView) {
                        Log.d("MainScreen_Nav", "Creating BrowseSetViewModelFactory for setId: $setIdForView")
                        BrowseSetViewModelFactory(
                            sharedSetId = setIdForView,
                            firebaseRepository = firebaseRepo,
                            authRepository = authRepo,
                            ttsService = ttsService
                        )
                    }
                    val browseSetViewModel: BrowseSharedSetViewModel = viewModel(key = "BrowseVM_$setIdForView", factory = browseSetViewModelFactory)
                    Log.d("MainScreen_Nav", "BrowseSharedSetViewModel instance obtained for setId: $setIdForView. ViewModel: $browseSetViewModel")

                    BrowseSharedSetScreen(
                        viewModel = browseSetViewModel,
                        onBack = {
                            Log.i("MainScreen_Nav", "Back pressed from BrowseSharedSetScreen. Set ID was: $setIdForView")
                            currentSetIdForBrowse = null
                            currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                        }
                    )
                    Log.d("MainScreen_Nav", "BrowseSharedSetScreen composed for $setIdForView.")
                } else {
                    Log.e("MainScreen_Nav", "Error: currentSetIdForBrowse is NULL when trying to display BrowseSharedSetFullScreen. Navigating back to AuthenticatedApp.")
                    LaunchedEffect(Unit) { // Щоб безпечно змінити стан не під час композиції
                        currentTopLevelScreenState = TopLevelScreenState.AuthenticatedApp
                    }
                }
            }

        }
    }
}