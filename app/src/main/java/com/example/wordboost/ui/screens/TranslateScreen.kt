package com.example.wordboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.firebase.*
import com.example.wordboost.data.local.AppDatabase
import com.example.wordboost.data.model.Word
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.translation.RealTranslator
import com.example.wordboost.ui.components.CustomGroupDialog
import com.example.wordboost.data.firebase.Group
import com.example.wordboost.data.firebase.AuthRepository
import androidx.room.Room
import java.util.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "wordboost-db")
            .fallbackToDestructiveMigration(true)
            .build()
    }
    val cacheDao = db.cacheDao()

    val firebaseRepo    = remember { FirebaseRepository() }
    val realTranslator  = remember { RealTranslator() }
    val translationRepo = remember { TranslationRepository(firebaseRepo, cacheDao, realTranslator) }


    // Input fields
    var ukText by remember { mutableStateOf("") }
    var enText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Groups
    var groups by remember { mutableStateOf(listOf<Group>()) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }

    // Load groups once
    LaunchedEffect(Unit) {
        firebaseRepo.getGroups { fetched -> groups = fetched }
    }

    Scaffold(
        topBar = {
            Button(onClick = onBack) { // <-- Викликаємо onBack при натисканні
                Text("Назад")
            }
            CenterAlignedTopAppBar(title = { Text("WordBoost") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ukrainian input
            OutlinedTextField(
                value = ukText,
                onValueChange = { ukText = it },
                label = { Text("Українське слово") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            // English input
            OutlinedTextField(
                value = enText,
                onValueChange = { enText = it },
                label = { Text("English word") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            // Translate button: enabled only when exactly one field is non-empty
            Button(
                onClick = {
                    statusMessage = null
                    val originalText = ukText.ifBlank { enText }
                    val targetLang = if (ukText.isNotBlank()) "EN" else "UK"
                    translationRepo.translate(originalText, targetLang) { result ->
                        result?.let { translated ->
                            if (ukText.isNotBlank()) {
                                enText = translated
                            } else {
                                ukText = translated
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (ukText.isNotBlank() xor enText.isNotBlank())
            ) {
                Text("Перекласти")
            }

            // Status message
            statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            // Save buttons row - only when both fields have text
            // Кнопки збереження/додавання до групи - завжди присутні в компонуванні,
            // але їх активність контролюється умовою
            val canSave = ukText.isNotBlank() && enText.isNotBlank() // Умова активності кнопок

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Додати в словник

                Button(
                    onClick = {
                        // ... ваша логіка збереження в словник ...
                        // Скопіюйте сюди всю логіку, яка була всередині onClick цієї кнопки
                        statusMessage = null
                        val original = ukText.trim()
                        val translated = enText.trim() // Отримати переклад тут

                        firebaseRepo.getWordObject(original) { existingWord ->
                            if (existingWord != null) {
                                statusMessage = "Слово вже в словнику"
                            } else {
                                val id = UUID.nameUUIDFromBytes((original + translated).toByteArray()).toString()
                                val word = Word(
                                    id = id,
                                    text = original,
                                    translation = translated,
                                    dictionaryId = "", // Для словника dictionaryId пустий
                                    repetition = 0, easiness = 2.5f, interval = 0L, lastReviewed = 0L, nextReview = 0L, status = "new"
                                )
                                firebaseRepo.saveWord(word) { success ->
                                    statusMessage = if (success) "Додано до словника" else "Помилка збереження"
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canSave // <-- Кнопка активна, тільки коли обидва поля заповнені
                ) {
                    Text("Додати в словник")
                }
                // Додати до групи
                Button(
                    onClick = { showGroupDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = canSave // <-- Кнопка активна, тільки коли обидва поля заповнені
                ) {
                    Text("Додати до групи")
                }
            }

                }
            }

            // Group selector dialog
            if (showGroupDialog) {
                CustomGroupDialog( // Використовуємо ваш новий компонент
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    onGroupSelected = { selectedGroupId = it },
                    onCreateGroup = { name ->
                        // Логіка створення групи. Переконайтесь, що getGroups викликається після успіху
                        firebaseRepo.createGroup(name) { success ->
                            if (success) {
                                firebaseRepo.getGroups { fetchedGroups ->
                                    groups = fetchedGroups
                                    // Можливо, тут варто знайти нову групу і встановити selectedGroupId = it.id?
                                    // Це залежить від того, чи getGroups повертає ID нової групи,
                                    // або вам потрібен інший метод для отримання ID після створення.
                                }
                                statusMessage = "Група '${name}' створена" // Опціонально: повідомлення про створення
                            } else {
                                statusMessage = "Помилка створення групи '${name}'" // Повідомлення про помилку
                            }
                        }
                    },
                    onConfirm = { // ВИПРАВЛЕНА ЛЯМБДА ONCONFIRM
                        statusMessage = null // Очищаємо попередній статус
                        val original = ukText.trim() // Забираємо зайві пробіли
                        val translated = enText.trim() // Забираємо зайві пробіли
                        val groupId = selectedGroupId.orEmpty()

                        // Валідація: перевірка, чи обрана група
                        if (groupId.isBlank()) {
                            statusMessage = "Будь ласка, оберіть групу або створіть нову."
                            // Не закриваємо діалог, щоб користувач міг обрати/створити групу
                            return@CustomGroupDialog // Виходимо з цієї лямбди
                        }

                        // Валідація: перевірка, чи є текст
                        if (original.isBlank() || translated.isBlank()) {
                            statusMessage = "Поля 'Українське слово' та 'English word' мають бути заповнені."
                            // Не закриваємо діалог
                            return@CustomGroupDialog // Виходимо з цієї лямбди
                        }


                        // Правильна логіка: шукаємо об'єкт слова за оригінальним текстом
                        firebaseRepo.getWordObject(original) { wordObj ->
                            if (wordObj != null) {
                                // Слово ЗНАЙДЕНО, оновлюємо лише його групу (dictionaryId)
                                val updated = wordObj.copy(dictionaryId = groupId)
                                firebaseRepo.saveWord(updated) { success ->
                                    statusMessage = if (success) "Група слова оновлена" else "Помилка оновлення групи"
                                    // ТЕПЕР закриваємо діалог після завершення операції збереження
                                    showGroupDialog = false
                                    selectedGroupId = null // Скидаємо обрану групу
                                }
                            } else {
                                // Слово НЕ ЗНАЙДЕНО, створюємо НОВЕ слово з вказаною групою
                                val id = UUID.nameUUIDFromBytes((original + translated + groupId).toByteArray()).toString()
                                val newWord = Word(
                                    id = id,
                                    text = original,
                                    translation = translated,
                                    dictionaryId = groupId, // Вказуємо обрану групу
                                    repetition = 0,
                                    easiness = 2.5f,
                                    interval = 0L,
                                    lastReviewed = 0L,
                                    nextReview = 0L,
                                    status = "new"
                                )
                                firebaseRepo.saveWord(newWord) { success ->
                                    statusMessage = if (success) "Слово додано до групи" else "Помилка збереження слова в групу"
                                    // ТЕПЕР закриваємо діалог після завершення операції збереження
                                    showGroupDialog = false
                                    selectedGroupId = null // Скидаємо обрану групу
                                }
                            }
                        }
                        // !!! ВИДАЛИТИ ЦІ РЯДКИ ЗВІДСИ !!! Вони закривали діалог передчасно.
                        // showGroupDialog = false
                        // selectedGroupId = null
                    },
                    onDismissRequest = { // Ця лямбда викликається при кліку на фон або кнопку Скасувати
                        showGroupDialog = false // Просто приховуємо діалог
                        selectedGroupId = null // Скидаємо обрану групу
                    }
                ) }
}

