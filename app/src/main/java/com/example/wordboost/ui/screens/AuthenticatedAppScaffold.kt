package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add // Для FAB на SetsScreen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wordboost.navigation.BottomNavItem
import com.example.wordboost.ui.components.MyBottomNavigationBar
import com.example.wordboost.viewmodel.WordListViewModelFactory // Тільки factory для AuthenticatedMainScreen

// --- Екран "Набори" (SetsScreen) ---
@Composable
fun SetsScreen(
    onNavigateToCreateSet: () -> Unit // Колбек для переходу на екран створення набору (повноекранний)
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateSet) {
                Icon(Icons.Filled.Add, contentDescription = "Створити набір")
            }
        }
    ) { paddingValues ->
        Column(
            Modifier.padding(paddingValues).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Екран Наборів Карток", style = MaterialTheme.typography.headlineMedium)
            Text("Тут буде список публічних та ваших наборів.")
        }
    }
}

// --- Екран "Статті" (ArticlesScreen) ---
@Composable
fun ArticlesScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Екран Статей", style = MaterialTheme.typography.headlineMedium)
        Text("Ця функція в розробці.")
    }
}

// --- AuthenticatedAppScaffold ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedAppScaffold(
    // Колбеки для навігації на повноекранні режими
    onNavigateToTranslate: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToWordList: () -> Unit,
    onNavigateToCreateSet: () -> Unit,
    onLogoutClick: () -> Unit,
    // Factories
    wordListViewModelFactory: WordListViewModelFactory
) {
    val navController = rememberNavController() // Цей NavController для вкладок BottomNav

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
                SetsScreen(
                    onNavigateToCreateSet = onNavigateToCreateSet
                )
            }
            composable(BottomNavItem.Articles.route) {
                ArticlesScreen()
            }
        }
    }
}