package com.example.wordboost.ui.components // Ваш пакет

// Імпорти для BatteryProgressIndicator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.max
import androidx.compose.ui.graphics.graphicsLayer // Додано імпорт graphicsLayer
import com.example.wordboost.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

import com.example.wordboost.data.model.Word // Переконайтесь в імпорті Word
import com.example.wordboost.viewmodel.WordDisplayItem // Переконайтесь в імпорті WordDisplayItem

// Припускаємо, що BatteryProgressIndicator визначений і доступний в цьому ж пакеті
// import com.example.wordboost.ui.components.BatteryProgressIndicator // Не потрібен, якщо в тому ж файлі


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListItem(
    item: WordDisplayItem,
    onItemClick: (Word) -> Unit, // Колбек для кліку на весь елемент
    onEditClick: (Word) -> Unit, // Колбек для редагування (з меню)
    onResetClick: (Word) -> Unit, // Колбек для скидання (з меню)
    onDeleteClick: (String) -> Unit, // Колбек для видалення (з меню), приймає ID (String)
    onPlaySound: (Word) -> Unit, // Колбек для озвучення
    formatDate: (Long) -> String, // Функція форматування дати
    wordProgress: Float // Прогрес від 0f до 1f
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp) // Відступи навколо картки в списку
            .clickable { onItemClick(item.word) }, // Картка клікабельна для перегляду/редагування
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Невелика тінь
        shape = MaterialTheme.shapes.medium, // Форма картки з теми (зазвичай закруглені кути)
        // !!! Змінено колір фону картки для кращого контрасту з фоном екрану !!!
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) // Використовуємо колір контейнера з теми
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Внутрішній відступ контенту в картці
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Розподіл простору між лівою частиною та меню
        ) {
            // Ліва частина картки: переклад+динамік, текст слова, група, дата
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp), // Ліва колонка займає весь доступний простір, крім правої частини
                verticalArrangement = Arrangement.spacedBy(4.dp) // Невеликий відступ між елементами в колонці
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Кнопка прослуховування (іконка)
                    IconButton(
                        onClick = { onPlaySound(item.word) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.volume_up_svgrepo_com),
                            contentDescription = "Прослухати ${item.word.translation}",
                            tint = MaterialTheme.colorScheme.primary, // Колір іконки з теми
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp)) // Відступ між іконкою та текстом

                    // Текст перекладу (ОСНОВНИЙ)
                    Text(
                        text = item.word.translation,
                        style = MaterialTheme.typography.headlineSmall, // Стиль перекладу з теми
                        color = MaterialTheme.colorScheme.onSurface, // Колір тексту з теми
                        modifier = Modifier.weight(1f) // Переклад займає доступний простір у цьому Row
                    )
                }

                // Оригінальне слово (ДРУГОРЯДНИЙ)
                Text(
                    text = item.word.text,
                    style = MaterialTheme.typography.bodyMedium, // Стиль оригінального слова з теми
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Колір тексту з теми
                )

                // Spacer(modifier = Modifier.height(4.dp)) // Цей Spacer може бути не потрібен, якщо використовується Arrangement.spacedBy у батьківському Column

                // Рядок з групою, датою та індикатором прогресу
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, // Розподіл простору
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { // Колонка для групи та дати
                        // Група
                        Text(
                            text = "Група: ${item.groupName ?: "Без групи"}",
                            style = MaterialTheme.typography.bodySmall, // Стиль тексту з теми
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Колір тексту з теми
                        )
                        // Дата наступного повторення
                        Text(
                            text = "Наступне: ${formatDate(item.word.nextReview)}", // Додано "Наступне: "
                            style = MaterialTheme.typography.bodySmall, // Стиль тексту з теми
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Колір тексту з теми
                        )
                    }

                    // Індикатор прогресу (BatteryProgressIndicator)
                    BatteryProgressIndicator(
                        progress = wordProgress,
                        modifier = Modifier.size(32.dp) // Розмір індикатора
                    )


                }
            }

            // !!! Опціонально: Додайте вертикальний роздільник між контентом та меню !!!
            /*
            VerticalDivider(
                modifier = Modifier.height(IntrinsicSize.Max), // Висота роздільника за вмістом
                color = MaterialTheme.colorScheme.outlineVariant, // Колір роздільника
                thickness = 1.dp // Товщина роздільника
            )
             Spacer(modifier = Modifier.width(8.dp)) // Відступ після роздільника
            */


            // Права частина картки: Меню (MoreVert icon)
            Box { // Box для позиціонування меню
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню для слова ${item.word.text}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant // Колір іконки з теми
                    )
                }
                // Випадаюче меню
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Редагувати") },
                        onClick = { onEditClick(item.word); showMenu = false } // Передаємо Word
                    )
                    DropdownMenuItem(
                        text = { Text("Скинути статистику") },
                        onClick = { onResetClick(item.word); showMenu = false } // Передаємо Word
                    )
                    DropdownMenuItem(
                        text = { Text("Видалити") },
                        // Передаємо item.id (String), як очікує ViewModel.deleteWord
                        onClick = { onDeleteClick(item.id); showMenu = false }
                    )
                }
            }
        }
    }
}