//package com.example.wordboost.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import com.example.wordboost.data.firebase.FirebaseRepository
//import com.example.wordboost.data.model.Word
//import com.example.wordboost.data.repository.PracticeRepository
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PracticeScreen() {
//    // Інстанції репозиторіїв
//    val firebaseRepo = remember { FirebaseRepository() }
//    val practiceRepo = remember { PracticeRepository(firebaseRepo) }
//    val scope = rememberCoroutineScope()
//
//    // Список слів для практики
//    var words by remember { mutableStateOf<List<Word>>(emptyList()) }
//    // Поточний індекс
//    var currentIndex by remember { mutableStateOf(0) }
//    // Стан фліпу картки
//    var isFlipped by remember { mutableStateOf(false) }
//
//    // Завантаження слів один раз
//    LaunchedEffect(Unit) {
//        firebaseRepo.getUserWords { fetched ->
//            words = fetched
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(title = { Text("Практика") })
//        }
//    ) { padding ->
//        Box(
//            modifier = Modifier
//                .padding(padding)
//                .fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            if (words.isEmpty()) {
//                Text("Немає слів для практики", style = MaterialTheme.typography.bodyLarge)
//            } else {
//                val word = words[currentIndex]
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.spacedBy(24.dp)
//                ) {
//                    // Індикатор прогресу
//                    Text(
//                        text = "${currentIndex + 1}/${words.size}",
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//
//                    // Картка з фліпом
//                    Card(
//                        modifier = Modifier
//                            .fillMaxWidth(0.8f)
//                            .height(200.dp)
//                            .clickable { isFlipped = !isFlipped },
//                        shape = MaterialTheme.shapes.large,
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .background(MaterialTheme.colorScheme.surface)
//                                .fillMaxSize(),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = if (!isFlipped) word.original else word.translation,
//                                style = MaterialTheme.typography.headlineSmall,
//                                textAlign = TextAlign.Center,
//                                modifier = Modifier.padding(16.dp)
//                            )
//                        }
//                    }
//
//                    // Кнопки "Не знаю" / "Знаю"
//                    Row(
//                        horizontalArrangement = Arrangement.spacedBy(16.dp)
//                    ) {
//                        Button(
//                            onClick = {
//                                scope.launch {
//                                    practiceRepo.updateWordStatus(word.id, Word.Status.LEARNING)
//                                    // наступне слово
//                                    if (currentIndex < words.lastIndex) {
//                                        currentIndex++
//                                        isFlipped = false
//                                    }
//                                }
//                            }
//                        ) {
//                            Text("Не знаю")
//                        }
//
//                        Button(
//                            onClick = {
//                                scope.launch {
//                                    practiceRepo.updateWordStatus(word.id, Word.Status.LEARNED)
//                                    if (currentIndex < words.lastIndex) {
//                                        currentIndex++
//                                        isFlipped = false
//                                    }
//                                }
//                            }
//                        ) {
//                            Text("Знаю")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
