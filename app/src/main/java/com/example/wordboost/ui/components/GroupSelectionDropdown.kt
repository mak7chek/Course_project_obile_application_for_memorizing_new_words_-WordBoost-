package com.example.wordboost.ui.components // Ваш пакет

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.model.Group // Переконайтесь в імпорті Group

@Composable
fun GroupFilterDropdown(
    groups: List<Group>,
    selectedGroupId: String?,
    onGroupSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val allGroupsOption = Group(id = "all_groups_filter", name = "Всі групи")
    val groupsWithOptions = listOf(allGroupsOption) + groups

    val selectedGroupName = groupsWithOptions.find { it.id == selectedGroupId }?.name ?: "Всі групи"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp) // Горизонтальний та вертикальний відступ
            .clickable { expanded = true },
        contentAlignment = Alignment.CenterStart
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            // !!! ЗМІНЕНО: Заповнюємо тільки ШИРИНУ батьківського Box !!!
            modifier = Modifier.fillMaxWidth(), // Кнопка заповнює ШИРИНУ Box
            contentPadding = PaddingValues(horizontal = 16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedGroupName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Виберіть групу")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(200.dp)
        ) {
            groupsWithOptions.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.name, maxLines = 1) },
                    onClick = {
                        onGroupSelected(if (group.id == "all_groups_filter") null else group.id)
                        expanded = false
                    }
                )
            }
        }
    }
}