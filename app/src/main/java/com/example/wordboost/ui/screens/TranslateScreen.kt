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
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    firebaseRepo: FirebaseRepository,
    translationRepo: TranslationRepository,
    onBack: () -> Unit
) {
    val viewModel: TranslateViewModel = viewModel(
        factory = TranslateViewModelFactory(firebaseRepo, translationRepo)
    )

    val ukText by viewModel.ukText.collectAsState()
    val enText by viewModel.enText.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val showGroupDialog by viewModel.showGroupDialog.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()


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
                        Image(
                            painter = painterResource(id = R.drawable.folder_custom_icon),
                            contentDescription = "Вибрати групу",
                            modifier = Modifier.size(24.dp)
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
            groups = groups,
            selectedGroupId = selectedGroupId,
            onGroupSelected = { viewModel.setSelectedGroupId(it) },
            onCreateGroup = { name -> viewModel.createGroup(name) },
            onRenameGroup = { groupId, newName -> viewModel.renameGroup(groupId, newName) },
            onDeleteGroup = { groupId -> viewModel.deleteGroup(groupId) },
            onConfirm = { viewModel.hideGroupDialog() },
            onDismissRequest = { viewModel.hideGroupDialog() }
        )
    }
}