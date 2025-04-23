package com.example.wordboost.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.wordboost.data.firebase.Group

// Перейменовуємо функцію, щоб уникнути конфлікту імен, якщо ви захочете зберегти стару.
// Або можете просто замінити вміст старої функції. Назвемо її CustomGroupDialog.
@Composable
fun CustomGroupDialog(
    groups: List<Group>,
    selectedGroupId: String?,
    onGroupSelected: (String?) -> Unit,
    onCreateGroup: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit // Ця лямбда буде викликатися вручну
) {
    var creatingNewGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    // Повноекранний Box, який слугує оверлеєм і фоном діалогу
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Напівпрозорий фон, який при кліку викликає onDismissRequest
            // Використовуємо indication = null та порожній interactionSource,
            // щоб зробити його просто зоною для кліку без візуальної реакції.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            )
            .background(Color.Black.copy(alpha = 0.5f)), // Колір фону оверлею
        contentAlignment = Alignment.Center // Центруємо вміст "діалогу" на екрані
    ) {
        // Вміст самого діалогу, розташований по центру
        // Використовуємо Surface, щоб імітувати вигляд діалогу
        Surface(
            modifier = Modifier
                // Важливо: Цей клікабельний модифікатор з empty lambda та null indication
                // перехоплює кліки всередині Surface і зупиняє їх розповсюдження до фону.
                // Це дозволяє клікати по елементах діалогу, не закриваючи його.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* Зупиняємо клік */ }
                .widthIn(min = 280.dp, max = 560.dp) // Обмеження ширини, схоже на стандартний діалог
                .padding(24.dp), // Внутрішній відступ контенту від країв Surface
            shape = MaterialTheme.shapes.medium, // Закруглені кути, схоже на стандартний діалог
            color = MaterialTheme.colorScheme.surface, // Колір фону діалогу
            tonalElevation = 6.dp // Тінь/глибина, схоже на стандартний діалог
        ) {
            // Використовуємо AnimatedContent для плавної анімації переходу
            // між списком груп та полем вводу нової групи
            AnimatedContent(
                targetState = creatingNewGroup,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut() // Проста анімація появи/зникнення
                }, label = "dialogContentAnimation"
            ) { isCreating ->
                if (isCreating) {
                    // Контент для створення нової групи
                    Column(
                        modifier = Modifier
                            .padding(24.dp) // Внутрішній відступ для цього контенту
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Нова група", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))
                        TextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text("Назва групи") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End // Кнопки справа
                        ) {
                            TextButton(onClick = { creatingNewGroup = false }) { // Просто закриваємо поле вводу, повертаємося до списку
                                Text("Скасувати")
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                onCreateGroup(newGroupName)
                                newGroupName = ""
                                creatingNewGroup = false // Закриваємо поле вводу, повертаємося до списку
                                // onDismissRequest() // Можливо, ви хотіли закривати весь діалог тут? Оригінал закривав. Якщо так, розкоментуйте.
                            }) {
                                Text("ОК")
                            }
                        }
                    }
                } else {
                    // Контент для вибору групи
                    Column(
                        modifier = Modifier
                            .padding(24.dp) // Внутрішній відступ для цього контенту
                            .fillMaxWidth()
                    ) {
                        Text("Оберіть групу", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))

                        // Список груп з радіо-кнопками
                        Column { // Додатковий Column для прокрутки, якщо груп багато
                            groups.forEach { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onGroupSelected(group.id) }
                                        .padding(vertical = 8.dp), // Вертикальний відступ для рядка
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedGroupId == group.id,
                                        onClick = { onGroupSelected(group.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(group.name)
                                }
                            }
                        }


                        Spacer(modifier = Modifier.height(8.dp)) // Відступ перед кнопкою "Створити"

                        TextButton(onClick = { creatingNewGroup = true }) {
                            Text("➕ Створити групу")
                        }

                        Spacer(modifier = Modifier.height(24.dp)) // Відступ перед кнопкою "Виконати"

                        // Кнопка виконання, яка закриває весь діалог
                        Button(
                            onClick = {
                                onConfirm() // Виконуємо дію
                                onDismissRequest() // Закриваємо діалог
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedGroupId != null // Кнопка активна, якщо обрано групу
                        ) {
                            Text("Виконати")
                        }
                    }
                }
            }
        }
    }
}