package com.example.wordboost.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility // Для анімації
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Важливо для items з key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit // Іконка для редагування
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wordboost.data.model.DifficultyLevel
import com.example.wordboost.data.model.SharedCardSetSummary
import com.example.wordboost.navigation.BottomNavItem
import com.example.wordboost.ui.components.MyBottomNavigationBar
import com.example.wordboost.viewmodel.SetsViewModel
import com.example.wordboost.viewmodel.SetsViewModelFactory
import com.example.wordboost.viewmodel.WordListViewModelFactory
import kotlinx.coroutines.launch

// Імпорти для Material 3 Pull-to-Refresh
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.getValue // Для делегата 'by'

// --- Composable для заголовка секції ---
@Composable
fun SectionHeader(
    title: String,
    isExpanded: Boolean,
    isLoading: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(vertical = 8.dp), // Додав відступ для клікабельної зони
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Згорнути" else "Розгорнути"
            )
        }
    }
}

// --- Елемент списку для SharedCardSetSummary (ОНОВЛЕНИЙ) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCardSetSummaryItem(
    summary: SharedCardSetSummary,
    isMySet: Boolean,
    onViewClick: () -> Unit,     // Окремий колбек для основного кліку (перегляд)
    onEditClick: (() -> Unit)?,  // Опціональний колбек для кнопки редагування
    onDeleteClick: (() -> Unit)? // Опціональний колбек для кнопки видалення
) {
    Card(
        onClick = onViewClick, // Основний клік по картці - перегляд
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(summary.name_uk, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Слів: ${summary.wordCount}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = summary.difficultyLevelKey?.let { DifficultyLevel.fromKey(it)?.displayName } ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                summary.authorName?.let {
                    Text("Автор: $it", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                }
                if (summary.public) { // Використовуємо 'public' з маленької, як у моделі
                    Text("Публічний", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            // Кнопки дій для "Моїх наборів"
            if (isMySet) {
                onEditClick?.let { editClick -> // Якщо колбек для редагування передано
                    IconButton(onClick = editClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редагувати набір", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                onDeleteClick?.let { deleteClick -> // Якщо колбек для видалення передано
                    IconButton(onClick = deleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Видалити набір", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}


// --- Екран "Набори" (SetsScreen) (ОНОВЛЕНИЙ) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetsScreen(
    viewModel: SetsViewModel,
    onNavigateToCreateSet: () -> Unit,
    onNavigateToViewPublicSet: (setId: String) -> Unit, // Для перегляду публічного
    onNavigateToBrowseMySet: (setId: String) -> Unit,    // Для перегляду свого
    onNavigateToEditSet: (setId: String) -> Unit        // Для редагування свого
) {
    val mySets by viewModel.mySets.collectAsState()
    val publicSets by viewModel.publicSets.collectAsState()
    val isLoadingMySets by viewModel.isLoadingMySets.collectAsState()
    val isLoadingPublicSets by viewModel.isLoadingPublicSets.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showDeleteDialog by viewModel.showDeleteConfirmDialog.collectAsState()
    val isMySetsExpanded by viewModel.isMySetsExpanded.collectAsState()
    val isPublicSetsExpanded by viewModel.isPublicSetsExpanded.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val isOverallLoading = isLoadingMySets || isLoadingPublicSets
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(errorMessage) { errorMessage?.let { coroutineScope.launch { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); viewModel.clearErrorMessage() } } }
    if (showDeleteDialog) { AlertDialog(onDismissRequest = { viewModel.cancelDeleteSet() }, title = { Text("Підтвердити видалення") }, text = { Text("Ви впевнені, що хочете видалити набір '${viewModel.getSetNameToDelete() ?: "цей набір"}'? Цю дію неможливо буде скасувати.") }, confirmButton = { TextButton(onClick = { viewModel.confirmDeleteSet() }) { Text("Видалити", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { viewModel.cancelDeleteSet() }) { Text("Скасувати") } }) }
    if (pullRefreshState.isRefreshing) { LaunchedEffect(true) { Log.d("SetsScreen", "Pull to refresh triggered."); viewModel.loadAllSets() } }
    LaunchedEffect(isOverallLoading) { if (!isOverallLoading && pullRefreshState.isRefreshing) { pullRefreshState.endRefresh(); Log.d("SetsScreen", "Data loading finished, calling endRefresh().") } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateSet) {
                Icon(Icons.Filled.Add, contentDescription = "Створити набір")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), // Невеликі вертикальні відступи
                verticalArrangement = Arrangement.spacedBy(8.dp) // Відступ між елементами LazyColumn
            ) {
                // --- Секція "Мої набори" ---
                item { // Заголовок секції
                    SectionHeader(
                        title = "Мої набори",
                        isExpanded = isMySetsExpanded,
                        isLoading = isLoadingMySets && mySets.isEmpty(),
                        onToggleExpand = { viewModel.toggleMySetsExpanded() }
                    )
                }
                item { // Контент секції, що згортається
                    AnimatedVisibility(visible = isMySetsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (mySets.isNotEmpty()) {
                                mySets.forEach { setSummary ->
                                    SharedCardSetSummaryItem(
                                        summary = setSummary,
                                        isMySet = true,
                                        onViewClick = { onNavigateToBrowseMySet(setSummary.id) },
                                        onEditClick = { onNavigateToEditSet(setSummary.id) },
                                        onDeleteClick = { viewModel.requestDeleteSet(setSummary.id, setSummary.name_uk) }
                                    )
                                }
                            } else if (!isLoadingMySets) {
                                Text(
                                    "У вас ще немає створених наборів.",
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                            // Невеликий відступ знизу, якщо список розгорнутий і порожній або має елементи
                            if (mySets.isEmpty() && !isLoadingMySets || mySets.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // --- Секція "Публічні набори" ---
                item { // Заголовок секції
                    SectionHeader(
                        title = "Публічні набори",
                        isExpanded = isPublicSetsExpanded,
                        isLoading = isLoadingPublicSets && publicSets.isEmpty(),
                        onToggleExpand = { viewModel.togglePublicSetsExpanded() }
                    )
                }
                item { // Контент секції, що згортається
                    AnimatedVisibility(visible = isPublicSetsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (publicSets.isNotEmpty()) {
                                publicSets.forEach { setSummary ->
                                    SharedCardSetSummaryItem(
                                        summary = setSummary,
                                        isMySet = false,
                                        onViewClick = { onNavigateToViewPublicSet(setSummary.id) },
                                        onEditClick = null,
                                        onDeleteClick = null
                                    )
                                }
                            } else if (!isLoadingPublicSets) {
                                Text(
                                    "Публічних наборів поки що немає.",
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (publicSets.isEmpty() && !isLoadingPublicSets || publicSets.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}


// --- Екран "Статті" (ArticlesScreen) ---
@Composable
fun ArticlesScreen() { /* ... (код без змін) ... */ Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text("Екран Статей",style=MaterialTheme.typography.headlineMedium);Text("Ця функція в розробці.")} }


// --- AuthenticatedAppScaffold (ОНОВЛЕНИЙ виклик SetsScreen) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedAppScaffold(
    onNavigateToTranslate: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToWordList: () -> Unit,
    onNavigateToCreateSet: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavigateToBrowseSet: (String) -> Unit,
    onNavigateToEditSet: (String) -> Unit,
    setsViewModelFactory: SetsViewModelFactory,
    wordListViewModelFactory: WordListViewModelFactory
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { MyBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                AuthenticatedMainScreen(
                    navController = navController,
                    onNavigateToTranslate = onNavigateToTranslate,
                    onNavigateToPractice = onNavigateToPractice,
                    onNavigateToWordList = onNavigateToWordList,
                    onLogoutClick = onLogoutClick,
                    wordListViewModelFactory = wordListViewModelFactory
                )
            }
            composable(BottomNavItem.Sets.route) {
                val viewModel: SetsViewModel = viewModel(factory = setsViewModelFactory)
                SetsScreen(
                    viewModel = viewModel,
                    onNavigateToCreateSet = onNavigateToCreateSet,
                    onNavigateToViewPublicSet = { setId -> onNavigateToBrowseSet(setId) },
                    onNavigateToBrowseMySet = { setId -> onNavigateToBrowseSet(setId) }, // Можна тимчасово вести на той самий екран
                    onNavigateToEditSet = { setId -> onNavigateToEditSet(setId) }     // Передаємо новий колбек
                )
            }
            composable(BottomNavItem.Articles.route) {
                ArticlesScreen()
            }
        }
    }
}
