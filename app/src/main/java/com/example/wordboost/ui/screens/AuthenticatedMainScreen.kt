package com.example.wordboost.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Імпорт Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.contentColorFor // Імпорт contentColorFor


import com.example.wordboost.viewmodel.WordListViewModel
import com.example.wordboost.viewmodel.WordListViewModelFactory

// Імпорти репозиторіїв/сервісів, якщо вони потрібні для Factory
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.tts.TextToSpeechService

// Імпорт користувацьких кольорів (якщо ви їх визначили)
// import com.example.wordboost.ui.theme.LearnNowColor
// import com.example.wordboost.ui.theme.ShortTermColor
// import com.example.wordboost.ui.theme.LearnedColor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMainScreen(
    onTranslateClick: () -> Unit, // Колбек для переходу на екран перекладу (додавання слова)
    onPracticeClick: () -> Unit, // Колбек для переходу на екран практики
    onWordListClick: () -> Unit, // Колбек для переходу на екран списку слів
    onLogoutClick: () -> Unit, // Колбек для виходу з облікового запису
    // AuthenticatedMainScreen потребує Factory WordListViewModel для статистики
    wordListViewModelFactory: WordListViewModelFactory // Приймаємо Factory як параметр з MainScreen
) {
    // Отримуємо екземпляр WordListViewModel за допомогою Factory.
    // Цей ViewModel завантажує слова та статистику через Listener.
    val wordListViewModel: WordListViewModel = viewModel(factory = wordListViewModelFactory)

    // --- Спостерігаємо за статистикою з WordListViewModel ---
    // Ці State<Int> будуть автоматично оновлюватись при зміні статистики у ViewModel
    val wordsToLearnNowCount by wordListViewModel.wordsToLearnNowCount.collectAsState()
    val wordsInShortTermMemoryCount by wordListViewModel.wordsInShortTermMemoryCount.collectAsState()
    val learnedWordsCount by wordListViewModel.learnedWordsCount.collectAsState()

    // --- Стан UI ---
    // Можна додати стан для індикатора завантаження статистики, якщо потрібно
    // val isStatsLoading by wordListViewModel.isLoading.collectAsState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WordBoost") }, // Назва додатка як частина заголовка
                actions = {
                    // !!! КНОПКА ВИХОДУ В TOPAPPBAR !!!
                    // Потрібен імпорт ExitToApp, наприклад: import androidx.compose.material.icons.filled.ExitToApp
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Вийти")
                    }
                }
            )
        },
        // !!! КРУГЛА КНОПКА ДОДАВАННЯ СЛОВА (FAB) !!!
        floatingActionButton = {
            // При кліку переходимо на екран перекладу/додавання
            // Потрібен імпорт Add, наприклад: import androidx.compose.material.icons.filled.Add
            FloatingActionButton(onClick = onTranslateClick) {
                Icon(Icons.Default.Add, contentDescription = "Додати слово")
            }
        },
        // position = FabPosition.End, // Розташування FAB (End знизу праворуч за замовчуванням)
        // isFloatingActionButtonDocked = false // Не докований FAB
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues) // Застосовуємо паддінг від Scaffold та TopAppBar
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp), // Внутрішні відступи контенту
            horizontalAlignment = Alignment.CenterHorizontally, // Центруємо вміст по горизонталі
            verticalArrangement = Arrangement.spacedBy(16.dp) // Відступи між основними блоками
        ) {

            // --- Привітання ---
            Text(
                text = "Вітаємо у WordBoost!", // Або "Привіт, Користувач!"
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            // --- Блок Статистики (Три кольорові "Квадратики" в ОДИН РЯД) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly, // Розподіл квадратиків по ширині
                verticalAlignment = Alignment.CenterVertically // Вирівнювання по вертикалі
            ) {
                StatisticCard(
                    modifier = Modifier.weight(1f),
                    title = "Вчити зараз",
                    count = wordsToLearnNowCount,
                    color = Color(0xFF297C2D)
                )

                StatisticCard(
                    modifier = Modifier.weight(1f),
                    title = "Знаю",
                    count = wordsInShortTermMemoryCount,
                    color = Color(0xFF165B91)
                )

                StatisticCard(
                    modifier = Modifier.weight(1f),
                    title = "Навчився",
                    count = learnedWordsCount,
                    color = Color(0xFFB7921E)
                )
            }

            // !!! ВЕЛИКА КНОПКА ДЛЯ ПРАКТИКИ !!!
            Spacer(modifier = Modifier.height(24.dp)) // Відступ після статистики
            Button(
                onClick = onPracticeClick, // Перехід на екран практики
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Ширина кнопки (80%)
                    .height(72.dp) // Висота кнопки (зробимо її більшою)
            ) {
                Text(
                    text = "Почати практику",
                    style = MaterialTheme.typography.headlineSmall, // Більший шрифт
                    textAlign = TextAlign.Center
                )
            }

            // !!! КНОПКА "МІЙ СЛОВНИК" (МЕНШ ВИРАЖЕНА) !!!
            Spacer(modifier = Modifier.height(8.dp)) // Відступ після кнопки практики
            OutlinedButton( // Використовуємо OutlinedButton для меншої виразності
                onClick = onWordListClick, // Перехід на екран списку слів
                modifier = Modifier.fillMaxWidth(0.6f) // Менша ширина
            ) {
                Text("Мій словник")
            }


            // Використовуємо Spacer з weight, щоб "притиснути" вміст (крім FAB) до верхньої частини
            Spacer(modifier = Modifier.weight(1f))

            // FAB вже розміщений у слоті Scaffold
        }
    }
}

@Composable
fun StatisticCard(
    modifier: Modifier = Modifier, // <--- Приймаємо modifier як параметр
    title: String, // Заголовок квадратика
    count: Int, // Кількість слів
    color: Color // Колір фону картки
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Тінь
        shape = MaterialTheme.shapes.medium, // Форма картки
        colors = CardDefaults.cardColors(containerColor = color) // Встановлюємо колір фону
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Заповнюємо всю картку
                .padding(8.dp), // Внутрішні відступи вмісту картки
            horizontalAlignment = Alignment.CenterHorizontally, // Центруємо вміст по горизонталі
            verticalArrangement = Arrangement.Center // Центруємо вміст по вертикалі
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold, // Жирний шрифт
                color = contentColorFor(backgroundColor = color) // Колір тексту залежить від кольору фону
            )
            // !!! НАЗВА КАТЕГОРІЇ (ЗНИЗУ) !!!
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall, // Менший шрифт для назви
                textAlign = TextAlign.Center,
                color = contentColorFor(backgroundColor = color) // Колір тексту залежить від кольору фону
            )
        }
    }
}
