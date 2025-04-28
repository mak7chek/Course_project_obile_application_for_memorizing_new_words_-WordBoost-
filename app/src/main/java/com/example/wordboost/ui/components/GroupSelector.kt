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
import com.example.wordboost.data.model.Group

@Composable
fun CustomGroupDialog(
    groups: List<Group>,
    selectedGroupId: String?,
    onGroupSelected: (String?) -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var creatingNewGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var editingGroupName by remember { mutableStateOf("") }

    var deletingGroupIdConfirm by remember { mutableStateOf<String?>(null) }
    var groupNameConfirm by remember { mutableStateOf<String?>("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            )
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {

        Surface(
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .widthIn(min = 280.dp, max = 560.dp)
                .padding(24.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            AnimatedContent(
                targetState = creatingNewGroup || editingGroupId != null,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "dialogContent"
            ) { isEditing ->
                if (isEditing) {
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
                            Button(
                                onClick = onConfirmEditCreate,
                                enabled = nameState.isNotBlank()
                            ) {
                                Text("ОК")
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Оберіть групу", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGroupSelected(null) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedGroupId == null,
                                    onClick = { onGroupSelected(null) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Основний словник")
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            groups.forEach { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onGroupSelected(group.id) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedGroupId == group.id,
                                        onClick = { onGroupSelected(group.id) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = group.name,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(onClick = {
                                        editingGroupId = group.id
                                        editingGroupName = group.name
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Редагувати групу ${group.name}")
                                    }
                                    IconButton(onClick = {
                                        deletingGroupIdConfirm = group.id
                                        groupNameConfirm = group.name.orEmpty()
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Видалити групу ${group.name}")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(onClick = { creatingNewGroup = true }) {
                            Text("➕ Створити групу")
                        }

                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = true
                        ) {
                            Text("Виконати")
                        }
                    }
                }
            }
        }
    }

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
                        onDeleteGroup(deletingGroupIdConfirm!!)
                        deletingGroupIdConfirm = null
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