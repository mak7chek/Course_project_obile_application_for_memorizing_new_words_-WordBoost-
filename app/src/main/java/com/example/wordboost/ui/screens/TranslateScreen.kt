package com.example.wordboost.ui.screens

import androidx.activity.compose.BackHandler
import com.example.wordboost.R // Переконайтесь, що R імпортовано
import androidx.compose.foundation.Image // Переконайтесь, що Image імпортовано
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.local.AppDatabase
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.data.model.Word // Переконайтесь, що цей клас імпортовано
import com.example.wordboost.translation.RealTranslator // Переконайтесь, що цей клас імпортовано
// Імпортуємо НАШ КАСТОМНИЙ ДІАЛОГ
import com.example.wordboost.ui.components.CustomGroupDialog
import com.example.wordboost.data.firebase.Group // Переконайтесь, що цей клас імпортовано
import java.util.UUID // Переконайтесь, що цей клас імпортовано

// Імпорти для іконок
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.res.painterResource // Переконайтесь, що painterResource імпортовано
// import androidx.compose.material.icons.filled.Folder // Не потрібен, якщо використовуємо Image


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(onBack: () -> Unit) { // onBack параметр залишаємо
    val context = LocalContext.current

    // Ініціалізація репозиторіїв (як було в ваших прикладах)
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "wordboost-db")
            .fallbackToDestructiveMigration(true)
            .build()
    }
    val cacheDao = db.cacheDao()

    val firebaseRepo = remember { FirebaseRepository() }
    val realTranslator = remember { RealTranslator() }
    val translationRepo = remember { TranslationRepository(firebaseRepo, cacheDao, realTranslator) }

    // Стан для полів вводу та повідомлень
    var ukText by remember { mutableStateOf("") }
    var enText by remember { mutableStateOf("") }
    // translationResult використовується лише локально після translate, як у наданому коді
    // var translationResult by remember { mutableStateOf("") } // Цей стан не обов'язковий, якщо одразу оновлюєте поля

    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Стан для керування групами та діалогом
    var groups by remember { mutableStateOf(listOf<Group>()) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) } // ID обраної групи (String? або null)

    BackHandler(enabled = true) { onBack() }
    // Завантаження груп один раз (або при зміні user, якщо додасте автентифікацію)
    LaunchedEffect(Unit) {
        firebaseRepo.getGroups { fetched -> groups = fetched }
    }

    Scaffold(
        topBar = {
            TopAppBar( // Використовуємо звичайний TopAppBar для додавання actions
                title = { Text("WordBoost") },
                navigationIcon = { // Кнопка "Назад" у navigationIcon
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = { // Actions слот для іконки "Вибрати групу"
                    // Іконка активна, тільки коли обидва поля заповнені
                    val canShowGroupIcon = ukText.isNotBlank() && enText.isNotBlank()

                    IconButton(
                        onClick = {
                            if (canShowGroupIcon) {
                                showGroupDialog = true // Показуємо діалог вибору групи
                                statusMessage = null // Очищаємо статус
                                // selectedGroupId не скидаємо тут, щоб в діалозі показувався попередній вибір
                            } else {
                                statusMessage = "Заповніть обидва поля для вибору групи"
                            }
                        },
                        enabled = canShowGroupIcon // Активна, тільки коли обидва поля заповнені
                    ) {
                        // Використовуємо ваш ресурс іконки папки
                        Image(
                            painter = painterResource(id = R.drawable.folder_custom_icon),
                            contentDescription = "Вибрати групу",
                            modifier = Modifier.size(24.dp) // Розмір іконки
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column( // Основний Column екрана (як у наданому коді)
            modifier = Modifier
                .padding(padding) // Застосовуємо padding від Scaffold
                .fillMaxSize()
                .padding(16.dp), // Внутрішній відступ екрана
            verticalArrangement = Arrangement.spacedBy(16.dp), // Відступи між основними блоками
            horizontalAlignment = Alignment.CenterHorizontally // Центруємо по горизонталі
        ) {
            // Ukrainian input
            OutlinedTextField(
                value = ukText,
                onValueChange = {
                    ukText = it
                    // !!! ВИДАЛЯЄМО ЛОГІКУ ОЧИЩЕННЯ ІНШОГО ПОЛЯ !!!
                    // if (it.isNotBlank() && enText.isNotBlank()) enText = ""
                    // Скидаємо обрану групу при зміні тексту в будь-якому полі
                    selectedGroupId = null
                },
                label = { Text("Українське слово") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                // !!! ВИДАЛЯЄМО enabled = ... !!! Тепер поля завжди активні для редагування
                // enabled = enText.isBlank()
            )

            // English input
            OutlinedTextField(
                value = enText,
                onValueChange = {
                    enText = it
                    // !!! ВИДАЛЯЄМО ЛОГІКУ ОЧИЩЕННЯ ІНШОГО ПОЛЯ !!!
                    // if (it.isNotBlank() && ukText.isNotBlank()) ukText = ""
                    // Скидаємо обрану групу при зміні тексту в будь-якому полі
                    selectedGroupId = null
                },
                label = { Text("English word") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                // !!! ВИДАЛЯЄМО enabled = ... !!! Тепер поля завжди активні для редагування
                // enabled = ukText.isBlank()
            )

            // Translate button: enabled only when exactly one field is non-empty
            Button(
                onClick = {
                    statusMessage = null
                    // Скидаємо обрану групу при перекладі нового слова
                    selectedGroupId = null

                    val textToTranslate = ukText.ifBlank { enText }
                    val lang = if (ukText.isNotBlank()) "EN" else "UK"
                    translationRepo.translate(textToTranslate, lang) { result ->
                        // translationResult використовується лише локально
                        result?.let { translated ->
                            if (ukText.isNotBlank()) {
                                // ukText = ukText // Не потрібно встановлювати саме себе
                                enText = translated // Оновлюємо переклад в полі АНГЛ
                            } else {
                                // enText = enText // Не потрібно встановлювати саме себе
                                ukText = translated // Оновлюємо переклад в полі УКР
                            }
                        } ?: run {
                            statusMessage = "Не вдалося перекласти" // Повідомлення про помилку перекладу
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (ukText.isNotBlank() xor enText.isNotBlank()) // Активна, якщо заповнене ТІЛЬКИ одне поле
            ) {
                Text("Перекласти")
            }

            Spacer(Modifier.height(8.dp)) // Додамо відступ

            // Кнопка "Додати в словник" (єдина кнопка збереження)
            // Вона зберігає слово або в основний словник, або в обрану групу
            Button(
                onClick = {
                    statusMessage = null // Очищаємо статус

                    val original = ukText.trim()
                    val translated = enText.trim()
                    // Використовуємо selectedGroupId (може бути null, тоді orEmpty() дасть "")
                    val groupId = selectedGroupId.orEmpty()
                    val groupName = groups.find { it.id == selectedGroupId }?.name.orEmpty() // Назва обраної групи для повідомлення

                    // Перевірка, що обидва поля заповнені перед збереженням
                    if (original.isBlank() || translated.isBlank()) {
                        statusMessage = "Заповніть обидва поля для збереження"
                        return@Button
                    }

                    // Логіка збереження/оновлення
                    // Перевіримо, чи існує слово з ТАКИМ Ж ТЕКСТОМ
                    firebaseRepo.getWordObject(original) { existingWord ->
                        // Тепер перевірка на дублікат у ТАРГЕТ-ЛОКАЦІЇ (словник або конкретна група)
                        val isDuplicateInTargetLocation = existingWord != null && existingWord.dictionaryId == groupId

                        if (isDuplicateInTargetLocation) {
                            statusMessage = if (groupId.isBlank()) "Слово '$original' вже існує в словнику" else "Слово '$original' вже існує в групі '$groupName'"
                        } else {
                            // Якщо слова немає в цій конкретній групі (або словнику), зберігаємо нове або оновлюємо існуюче (якщо знайдене з іншою групою/без групи)
                            val wordToSave = if (existingWord != null) {
                                // Слово з таким текстом вже існує, оновлюємо його групу
                                existingWord.copy(dictionaryId = groupId)
                            } else {
                                // Слова не існує, створюємо нове
                                // Використовуємо ваш спосіб генерації UUID на основі тексту та groupId.
                                val id = UUID.nameUUIDFromBytes((original + translated + groupId).toByteArray()).toString()
                                Word(
                                    id = id,
                                    text = original,
                                    translation = translated,
                                    dictionaryId = groupId, // Призначаємо обрану групу (може бути пустий рядок)
                                    repetition = 0, easiness = 2.5f, interval = 0L, lastReviewed = 0L, nextReview = 0L, status = "new"
                                )
                            }

                            firebaseRepo.saveWord(wordToSave) { success ->
                                if (success) {
                                    statusMessage = if (groupId.isBlank()) "Слово '$original' додано до словника" else "Слово '$original' додано до групи '$groupName'"
                                } else {
                                    statusMessage = if (groupId.isBlank()) "Помилка збереження слова" else "Помилка збереження слова в групу"
                                }
                                // Очищаємо поля вводу після успішного збереження
                                ukText = ""
                                enText = ""
                                // Скидаємо обрану групу ПІСЛЯ успішного збереження слова
                                selectedGroupId = null
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                // Кнопка активна, тільки якщо обидва поля заповнені
                enabled = ukText.isNotBlank() && enText.isNotBlank()
            ) {
                // Текст кнопки тепер завжди "Додати в словник", як ви просили
                Text("Додати в словник")
            }


            // Status message
            statusMessage?.let {
                Text(
                    it,
                    color = when {
                        it.contains("Помилка") || it.contains("Не вдалося") -> MaterialTheme.colorScheme.error
                        it.contains("додано") || it.contains("оновлена") -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary // Інші повідомлення
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Кнопка "Назад" (тепер не потрібна в Column, бо є в Top Bar)
            /* OutlinedButton(...) */


        } // Кінець головного Column
    } // Кінець Scaffold content

    // Group selector dialog (НАШ КАСТОМНИЙ ОВЕРЛЕЙ З ІКОНКАМИ)
    if (showGroupDialog) {
        CustomGroupDialog( // Використовуємо наш кастомний оверлей
            groups = groups,
            selectedGroupId = selectedGroupId,
            onGroupSelected = { selectedGroupId = it }, // Оновлює стан обраної групи в TranslateScreen
            onCreateGroup = { name ->
                firebaseRepo.createGroup(name) { success ->
                    if (success) {
                        statusMessage = "Група '$name' створена"
                        firebaseRepo.getGroups { groups = it } // Оновлюємо список груп
                    } else {
                        statusMessage = "Помилка створення групи '$name'"
                    }
                }
            },
            onRenameGroup = { groupId, newName ->
                firebaseRepo.updateGroup(groupId, newName) { success ->
                    if (success) {
                        statusMessage = "Група перейменована на '$newName'"
                        firebaseRepo.getGroups { groups = it } // Оновлюємо список груп
                        // Якщо редагували обрану групу, оновлюємо selectedGroupId, щоб UI оновився
                        // Це може бути не потрібно, якщо groups оновлюється і selectedGroupId збігається
                    } else {
                        statusMessage = "Помилка перейменування групи"
                    }
                }
            },
            // Передаємо лямбду, яка викликає логіку видалення. Підтвердження відбувається ВНУТРІ CustomGroupDialog.
            onDeleteGroup = { groupId ->
                firebaseRepo.deleteGroup(groupId) { success ->
                    if (success) {
                        statusMessage = "Група видалена"
                        firebaseRepo.getGroups { groups = it } // Оновлюємо список
                        // Якщо видалили обрану групу, скидаємо вибір selectedGroupId
                        if (selectedGroupId == groupId) {
                            selectedGroupId = null
                        }
                    } else {
                        statusMessage = "Помилка видалення групи"
                    }
                }
            },
            onConfirm = {
                // Після натискання "Виконати" в CustomGroupDialog,
                // ми просто закриваємо діалог.
                // Обраний ID групи вже було встановлено в onGroupSelected при кліку на радіо-кнопку або рядок.
                // Логіка збереження слова з цією групою буде викликана окремо при натисканні кнопки "Додати в словник".
                showGroupDialog = false // Закриваємо діалог
            },
            onDismissRequest = { // Клік поза діалогом або "Скасувати" у стані редагування/створення
                showGroupDialog = false
                // selectedGroupId не скидаємо тут, користувач може захотіти зберегти з цією групою пізніше
            }
        )
    }

    // Діалог підтвердження видалення групи (AlertDialog) - ТЕПЕР ВНУТРІ CustomGroupDialog, НЕ ТУТ
    /*
    if (deletingGroupIdConfirm != null) {
       AlertDialog(...)
    }
     */
}