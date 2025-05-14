package com.example.wordboost.ui.components

import androidx.compose.foundation.layout.size

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

import com.example.wordboost.data.model.Word
import com.example.wordboost.viewmodel.WordDisplayItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListItem(
    item: WordDisplayItem,
    onItemClick: (Word) -> Unit,
    onEditClick: (Word) -> Unit,
    onResetClick: (Word) -> Unit,
    onDeleteClick: (String) -> Unit,
    onPlaySound: (Word) -> Unit,
    formatDate: (Long) -> String,
    wordProgress: Float
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onItemClick(item.word) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onPlaySound(item.word) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = com.example.wordboost.R.drawable.volume_up_svgrepo_com),
                            contentDescription = "Прослухати ${item.word.translation}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.word.translation,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = item.word.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Група: ${item.groupName ?: "Без групи"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Наступне: ${formatDate(item.word.nextReview)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    BatteryProgressIndicator(
                        progress = wordProgress,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню для слова ${item.word.text}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Редагувати") },
                        onClick = { onEditClick(item.word); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Скинути статистику") },
                        onClick = { onResetClick(item.word); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Видалити") },
                        onClick = { onDeleteClick(item.id); showMenu = false }
                    )
                }
            }
        }
    }
}