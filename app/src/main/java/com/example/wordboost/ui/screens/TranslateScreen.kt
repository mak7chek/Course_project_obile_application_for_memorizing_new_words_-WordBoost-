package com.example.wordboost.ui.screens


import androidx.activity.compose.BackHandler
import com.example.wordboost.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
// Імпорт ViewModel та Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordboost.viewmodel.TranslateViewModel
import com.example.wordboost.viewmodel.TranslateViewModelFactory
// Імпорт репозиторіїв (для Factory)
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.repository.TranslationRepository
import com.example.wordboost.ui.components.CustomGroupDialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    firebaseRepo: FirebaseRepository,
    translationRepo: TranslationRepository,
    initialEn: String? = null,
    initialUk: String? = null,
    onBack: () -> Unit
) {
    val viewModel: TranslateViewModel = viewModel(
        factory = TranslateViewModelFactory(firebaseRepo, translationRepo)
    )
    val editableGroups by viewModel.editableGroups.collectAsState(initial = emptyList())
    val ukText by viewModel.ukText.collectAsState()
    val enText by viewModel.enText.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val showGroupDialog by viewModel.showGroupDialog.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()

    BackHandler(enabled = true) { onBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearStatusMessage()
            }
        }
    }

    LaunchedEffect(initialEn, initialUk) {
        initialEn?.let {
            if (viewModel.enText.value != it) {
                viewModel.setEnText(it)
            }
        }
        initialUk?.let {
            if (viewModel.ukText.value != it) {
                viewModel.setUkText(it)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Перекласти слово") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    val canShowGroupIcon = ukText.isNotBlank() && enText.isNotBlank()

                    IconButton(
                        onClick = {
                            if (canShowGroupIcon) {
                                viewModel.showGroupDialog()
                            } else {
                                viewModel.setStatusMessage("Заповніть обидва поля для вибору групи")
                            }
                        },
                        enabled = canShowGroupIcon
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id  = R.drawable.folder_custom_icon),
                            contentDescription = "Вибрати групу",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = ukText,
                onValueChange = { viewModel.setUkText(it) },
                label = { Text("Українське слово") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = enText,
                onValueChange = { viewModel.setEnText(it) },
                label = { Text("English word") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            // Translate button
            Button(
                onClick = { viewModel.translate() },
                modifier = Modifier.fillMaxWidth(),
                enabled = (ukText.isNotBlank() xor enText.isNotBlank()) && !isLoading
            ) {
                if (isLoading && (ukText.isNotBlank() xor enText.isNotBlank())) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Перекласти")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Кнопка "Додати в словник"
            Button(
                onClick = { viewModel.saveWord() },
                modifier = Modifier.fillMaxWidth(),
                enabled = ukText.isNotBlank() && enText.isNotBlank() && !isLoading
            ) {
                if (isLoading && ukText.isNotBlank() && enText.isNotBlank()) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Додати в словник")
                }
            }

        } 
    }

    // Group selector dialog
    if (showGroupDialog) {
        CustomGroupDialog(
            // !!! Передаємо editableGroups !!!
            editableGroups = editableGroups, // <-- Передаємо фільтрований список
            // !!! Передаємо поточний selectedGroupId (String?) !!!
            selectedGroupId = selectedGroupId, // <-- Передаємо стан String?
            onGroupSelected = { groupId -> viewModel.setSelectedGroupId(groupId) },
            onCreateGroup = { name -> viewModel.createGroup(name) },
            onRenameGroup = { groupId, newName -> viewModel.renameGroup(groupId, newName) },
            onDeleteGroup = { groupId -> viewModel.deleteGroup(groupId) },
            onConfirm = { viewModel.hideGroupDialog() },
            onDismissRequest = { viewModel.hideGroupDialog() }
        )
    }
}