package com.example.wordboost.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.firebase.Group
import com.example.wordboost.data.firebase.FirebaseRepository // Залежність, якщо використовується всередині

@Composable
fun CustomGroupDialog( // ВИКОРИСТОВУЄМО ЦЕЙ КОМПОНЕНТ (КАСТОМНИЙ ОВЕРЛЕЙ)
    groups: List<Group>,
    selectedGroupId: String?,
    onGroupSelected: (String?) -> Unit, // Вибирає групу в батьківському компоненті (приймає String? або null)
    onCreateGroup: (String) -> Unit, // Викликає зовнішню логіку створення
    onRenameGroup: (String, String) -> Unit, // Викликає зовнішню логіку редагування
    onDeleteGroup: (String) -> Unit, // Викликає зовнішню логіку видалення (без підтвердження тут)
    onConfirm: () -> Unit, // Викликається при натисканні "Виконати" в основному стані
    onDismissRequest: () -> Unit // Викликається для закриття всього оверлея (при кліку поза діалогом або Скасувати у стані редагування)
) {
    var creatingNewGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var editingGroupName by remember { mutableStateOf("") }

    // Додаємо стани для діалогу підтвердження видалення
    var deletingGroupIdConfirm by remember { mutableStateOf<String?>(null) }
    var groupNameConfirm by remember { mutableStateOf<String?>("") }

    // Зовнішній Box для затемнення фону та закриття по кліку
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable( // Обробка кліка поза діалогом
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest // Закриває діалог при кліку поза поверхнею
            )
            .background(Color.Black.copy(alpha = 0.5f)), // Затемнення
        contentAlignment = Alignment.Center // Центрування внутрішнього Surface
    ) {
        // Внутрішній Surface, який виглядає як діалог
        Surface(
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {} // Запобігає закриттю по кліку НА діалозі
                .widthIn(min = 280.dp, max = 560.dp) // Обмеження ширини
                .padding(24.dp), // Внутрішній відступ Surface
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            // AnimatedContent для анімації переходу між станами (список vs створити/редагувати)
            AnimatedContent(
                targetState = creatingNewGroup || editingGroupId != null,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "dialogContent"
            ) { isEditing ->
                if (isEditing) { // Стан "Створити" або "Редагувати"
                    val label = if (editingGroupId != null) "Редагувати групу" else "Нова група"
                    val nameState = if (editingGroupId != null) editingGroupName else newGroupName
                    val onValueChange: (String) -> Unit = {
                        if (editingGroupId != null) editingGroupName = it else newGroupName = it
                    }
                    val onConfirmEditCreate: () -> Unit = {
                        if (editingGroupId != null) {
                            onRenameGroup(editingGroupId!!, editingGroupName)
                            editingGroupId = null
                            editingGroupName = ""
                        } else {
                            onCreateGroup(newGroupName)
                            newGroupName = ""
                        }
                        creatingNewGroup = false // Закриваємо діалог створення/редагування
                    }
                    val onCancelEditCreate: () -> Unit = {
                        creatingNewGroup = false
                        editingGroupId = null
                        newGroupName = ""
                        editingGroupName = ""
                    }

                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))
                        TextField(
                            value = nameState,
                            onValueChange = onValueChange,
                            label = { Text("Назва групи") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onCancelEditCreate) {
                                Text("Скасувати")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button( // Використовуємо Button замість TextButton для підтвердження
                                onClick = onConfirmEditCreate,
                                enabled = nameState.isNotBlank()
                            ) {
                                Text("ОК")
                            }
                        }
                    }
                } else { // Стан "Список груп"
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Оберіть групу", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))

                        // Список груп з радіо-кнопками та іконками редагування/видалення
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // --- Опція "Основний словник" (Без групи) ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGroupSelected(null) } // Клік встановлює selectedGroupId в null
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedGroupId == null, // Обрано, якщо selectedGroupId == null
                                    onClick = { onGroupSelected(null) } // Клік встановлює selectedGroupId в null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Основний словник") // Текст для опції "без групи"
                            }
                            // --- Кінець опції "Основний словник" ---

                            Divider(modifier = Modifier.padding(vertical = 8.dp)) // Роздільник

                            // --- Список реальних груп ---
                            groups.forEach { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onGroupSelected(group.id) } // Клік по рядку вибирає групу
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedGroupId == group.id, // Обрано, якщо ID групи співпадає з обраним
                                        onClick = { onGroupSelected(group.id) } // Клік по радіо-кнопці вибирає групу
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = group.name,
                                        modifier = Modifier.weight(1f) // Дозволяє тексту займати доступний простір
                                    )
                                    // Іконка Редагувати
                                    IconButton(onClick = {
                                        editingGroupId = group.id
                                        editingGroupName = group.name // Заповнюємо поле поточною назвою
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Редагувати групу ${group.name}")
                                    }
                                    // Іконка Видалити - викликає діалог підтвердження
                                    IconButton(onClick = {
                                        deletingGroupIdConfirm = group.id
                                        groupNameConfirm = group.name.orEmpty() // Зберігаємо назву для підтвердження
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Видалити групу ${group.name}")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        // Кнопка "Створити групу"
                        TextButton(onClick = { creatingNewGroup = true }) {
                            Text("➕ Створити групу")
                        }

                        Spacer(Modifier.height(24.dp))
                        // Кнопка "Виконати" - підтвердження вибору групи та закриття діалогу
                        Button(
                            onClick = onConfirm, // Викликаємо зовнішню логіку підтвердження (яка просто закриє діалог)
                            modifier = Modifier.fillMaxWidth(),
                            enabled = true // Кнопка завжди активна, бо можна зберегти і без групи
                        ) {
                            Text("Виконати") // Текст кнопки
                        }
                        // Кнопка Скасувати - ВИДАЛЯЄМО З ОСНОВНОГО ВИГЛЯДУ
                        /*
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onDismissRequest, modifier = Modifier.align(Alignment.End)) {
                           Text("Скасувати")
                        }
                        */
                    }
                }
            }
        }
    }

    // --- Діалог підтвердження видалення групи (AlertDialog) ---
    if (deletingGroupIdConfirm != null) {
        AlertDialog(
            onDismissRequest = { deletingGroupIdConfirm = null; groupNameConfirm = "" },
            title = { Text("Видалити групу?") },
            text = {
                val groupToDelete = groupNameConfirm?.ifBlank { "обрану групу" }
                Text("Ви впевнені, що хочете видалити групу \"$groupToDelete\"? Ця дія незворотня.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(deletingGroupIdConfirm!!) // Викликаємо зовнішню лямбду видалення
                        // Після успішного видалення (це обробляється в TranslateScreen),
                        // стан selectedGroupId скидається, якщо видалили обрану групу.
                        deletingGroupIdConfirm = null // Закриваємо діалог підтвердження
                        groupNameConfirm = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroupIdConfirm = null; groupNameConfirm = "" }) {
                    Text("Скасувати")
                }
            }
        )
    }
}