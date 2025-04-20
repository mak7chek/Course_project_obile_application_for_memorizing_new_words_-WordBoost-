package com.example.wordboost.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordboost.data.firebase.Group

@Composable
fun GroupSelectorDialog(
    groups: List<Group>,
    selectedGroupId: String?,
    onGroupSelected: (String?) -> Unit,
    onCreateGroup: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var creatingNewGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        title = { Text("Оберіть групу") },
        text = {
            Column {
                groups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onGroupSelected(group.id)
                            }
                            .padding(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedGroupId == group.id,
                            onClick = { onGroupSelected(group.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(group.name)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(onClick = { creatingNewGroup = true }) {
                    Text("➕ Створити групу")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onConfirm()
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Виконати")
                }
            }
        }
    )

    if (creatingNewGroup) {
        AlertDialog(
            onDismissRequest = { creatingNewGroup = false },
            confirmButton = {
                TextButton(onClick = {
                    onCreateGroup(newGroupName)
                    newGroupName = ""
                    creatingNewGroup = false
                }) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    creatingNewGroup = false
                }) {
                    Text("Скасувати")
                }
            },
            title = { Text("Нова група") },
            text = {
                TextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Назва групи") }
                )
            }
        )
    }
}