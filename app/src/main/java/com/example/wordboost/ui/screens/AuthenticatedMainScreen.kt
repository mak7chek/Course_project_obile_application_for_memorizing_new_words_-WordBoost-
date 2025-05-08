// ui.screens/AuthenticatedMainScreen.kt
package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
// !!! Додано імпорт іконки книги !!!
import androidx.compose.material.icons.filled.List

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.foundation.shape.RoundedCornerShape

// !!! Імпорти для градієнта !!!
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight

// Імпорти для зображення (якщо вирішите додати)
// import androidx.compose.foundation.Image
// import androidx.compose.ui.layout.ContentScale
// import androidx.compose.ui.res.painterResource
// import com.example.wordboost.R

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp // Якщо використовуєте явно (Typography це робить)

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.wordboost.viewmodel.WordListViewModel
import com.example.wordboost.viewmodel.WordListViewModelFactory

// Імпорти ViewModel та Factory мають бути правильними
// import com.example.wordboost.data.firebase.AuthRepository
// import com.example.wordboost.data.firebase.FirebaseRepository
// import com.example.wordboost.data.tts.TextToSpeechService

// Якщо StatisticCard в окремому файлі, розкоментуйте імпорт
// import com.example.wordboost.ui.components.StatisticCard


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMainScreen(
    onTranslateClick: () -> Unit,
    onPracticeClick: () -> Unit,
    onWordListClick: () -> Unit,
    onLogoutClick: () -> Unit,
    wordListViewModelFactory: WordListViewModelFactory
) {
    val wordListViewModel: WordListViewModel = viewModel(factory = wordListViewModelFactory)

    val wordsToLearnNowCount by wordListViewModel.wordsToLearnNowCount.collectAsState()
    val wordsInShortTermMemoryCount by wordListViewModel.wordsInShortTermMemoryCount.collectAsState()
    val learnedWordsCount by wordListViewModel.learnedWordsCount.collectAsState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WordBoost") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Вийти")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onTranslateClick) {
                Icon(Icons.Default.Add, contentDescription = "Додати слово")
            }
        },
    ) { paddingValues ->
        // !!! Використовуємо Box для шарів !!!
        Box(
            modifier = Modifier
                .fillMaxSize() // Box заповнює весь доступний простір під Scaffold
                // !!! ДОДАЄМО ГРАДІЄНТНИЙ ФОН ВИКОРИСТОВУЮЧИ КОЛЬОРИ ТЕМИ !!!
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            // Використовуйте кольори з вашої палітри теми (приклад)
                            MaterialTheme.colorScheme.surfaceContainerHigh, // Колір зверху
                            MaterialTheme.colorScheme.background // Колір знизу
                            // Або custom кольори з Color.kt:
                            // listOf(lightGradientTop, lightGradientBottom)
                            // Додайте логіку для темної теми, якщо потрібно, наприклад:
                            // if (isSystemInDarkTheme()) listOf(darkGradientTop, darkGradientBottom) else listOf(lightGradientTop, lightGradientBottom)
                        )
                    )
                )
            // Якщо ви хочете додати зображення поверх градієнта, додайте Image тут ПЕРЕД Column
            /*
            Image(
                // ... параметри зображення ...
            )
            */
        ) {
            // !!! Існуючий вміст Column розміщується НАД фоном (градієнтом або зображенням) !!!
            Column(
                modifier = Modifier
                    .padding(paddingValues) // !!! ЗАСТОСОВУЄМО ПАДДІНГ ВІД SCAFFOLD ТУТ !!!
                    .fillMaxSize() // Column заповнює простір, доступний після паддінга
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ), // Ваші існуючі ВНУТРІШНІ відступи
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp) // Відступ між елементами Column
            ) {

                Text(
                    text = "Вітаємо у WordBoost!",
                    style = MaterialTheme.typography.headlineMedium, // Використовуємо стиль з теми
                    // !!! ЗБІЛЬШЕНО ВІДСТУП ЗНИЗУ !!!
                    modifier = Modifier.padding(
                        top = 8.dp,
                        bottom = 32.dp
                    ), // Збільшено нижній відступ
                    color = MaterialTheme.colorScheme.onBackground // Колір тексту для фону з теми
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // !!! Картки Статистики - використовують кольори теми або специфічні кольори !!!
                    StatisticCard(
                        modifier = Modifier.weight(1f),
                        title = "Вчити зараз",
                        count = wordsToLearnNowCount,
                        // Використовуємо кольори контейнерів з теми як фон для карток
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                        // Або використовуйте ваші специфічні кольори з Color.kt:
                        // containerColor = LearnNowColor
                    )

                    StatisticCard(
                        modifier = Modifier.weight(1f),
                        title = "Знаю",
                        count = wordsInShortTermMemoryCount,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )

                    StatisticCard(
                        modifier = Modifier.weight(1f),
                        title = "Навчився",
                        count = learnedWordsCount,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        // Або: containerColor = LearnedColor
                    )
                }

                // !!! ДОДАНО SPACER З ВАГОЮ ДЛЯ ПОСУВАННЯ КНОПОК ВНИЗ !!!
                Spacer(modifier = Modifier.weight(1f))

                // !!! ВЕЛИКА КНОПКА ДЛЯ ПРАКТИКИ (ТЕПЕР ПОСУНУТА ВНИЗ) !!!
                // Spacer(modifier = Modifier.height(24.dp)) // Цей Spacer більше не потрібен після додавання Spacer(weight=1f) перед ним
                Button(
                    onClick = onPracticeClick,
                    modifier = Modifier
                        .fillMaxWidth(0.8f) // Ширина кнопки (80%)
                        .height(72.dp), // Висота кнопки (зробимо її більшою)
                    shape = RoundedCornerShape(50) // Заокруглені кути
                    // Button автоматично використовує primary колір з теми та onPrimary для тексту
                ) {
                    Text(
                        text = "Почати практику",
                        style = MaterialTheme.typography.headlineSmall, // Використовуємо стиль з теми
                        textAlign = TextAlign.Center
                    )
                }

                // !!! КНОПКА "МІЙ СЛОВНИК" (СТИЛІЗОВАНО ТА ПОСУНУТО ВНИЗ) !!!
                Spacer(modifier = Modifier.height(8.dp)) // Невеликий відступ між кнопками
                TextButton( // Змінено на TextButton для меншої виразності
                    onClick = onWordListClick,
                    modifier = Modifier.fillMaxWidth(0.6f) // Менша ширина
                    // TextButton не має фону чи контуру за замовчуванням
                ) {
                    // !!! Додано іконку книги перед текстом !!!
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Мій словник",
                        // Колір іконки TextButton за замовчуванням - primary
                        tint = MaterialTheme.colorScheme.primary // Або інший колір з теми
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Відступ між іконкою та текстом
                    Text(
                        "Мій словник",
                        style = MaterialTheme.typography.titleMedium
                    ) // Використовуємо стиль з теми
                }
            }
            }
        }
    }

// !!! ЦЕЙ SPACER ТЕПЕР ВІДСУВАЄ ЗМІСТ НА
// Компонент StatisticCard (помістіть його у цей файл або імпортуйте)
@Composable
fun StatisticCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    Card(
        modifier = modifier
            .aspectRatio(1f) // Робимо квадратним
            .padding(4.dp), // Відступ навколо картки
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Тінь
        shape = MaterialTheme.shapes.medium, // Форма картки з теми
        colors = CardDefaults.cardColors(containerColor = containerColor) // Встановлюємо колір фону
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Заповнюємо всю картку
                .padding(8.dp), // Внутрішні відступи вмісту картки
            horizontalAlignment = Alignment.CenterHorizontally, // Центруємо вміст
            verticalArrangement = Arrangement.Center // Центруємо вміст
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall, // Стиль для числа з теми
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                // Колір тексту автоматично підбирається для контрасту з фоном картки
                color = contentColorFor(backgroundColor = containerColor)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall, // Стиль для підпису з теми
                textAlign = TextAlign.Center,
                color = contentColorFor(backgroundColor = containerColor) // Колір тексту автоматично підбирається
            )
        }
    }
}
