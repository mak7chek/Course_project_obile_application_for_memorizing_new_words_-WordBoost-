package com.example.wordboost.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.SharedCardSetSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf

class SetsViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _mySets = MutableStateFlow<List<SharedCardSetSummary>>(emptyList())
    val mySets: StateFlow<List<SharedCardSetSummary>> = _mySets.asStateFlow()

    private val _publicSets = MutableStateFlow<List<SharedCardSetSummary>>(emptyList())
    val publicSets: StateFlow<List<SharedCardSetSummary>> = _publicSets.asStateFlow()

    private val _isLoadingMySets = MutableStateFlow(false)
    val isLoadingMySets: StateFlow<Boolean> = _isLoadingMySets.asStateFlow()

    private val _isLoadingPublicSets = MutableStateFlow(false)
    val isLoadingPublicSets: StateFlow<Boolean> = _isLoadingPublicSets.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog.asStateFlow()
    private val _isMySetsExpanded = MutableStateFlow(true)
    val isMySetsExpanded: StateFlow<Boolean> = _isMySetsExpanded.asStateFlow()

    private val _isPublicSetsExpanded = MutableStateFlow(true)
    val isPublicSetsExpanded: StateFlow<Boolean> = _isPublicSetsExpanded.asStateFlow()

    private var setToDeleteId: String? = null
    private var setToDeleteName: String? = null
    init {
        loadAllSets()
    }
    fun toggleMySetsExpanded() {
        _isMySetsExpanded.value = !_isMySetsExpanded.value
        Log.d("SetsVM", "My Sets expanded state: ${_isMySetsExpanded.value}")
    }

    fun togglePublicSetsExpanded() {
        _isPublicSetsExpanded.value = !_isPublicSetsExpanded.value
        Log.d("SetsVM", "Public Sets expanded state: ${_isPublicSetsExpanded.value}")
    }
    fun loadAllSets() {
        loadMySets()
        loadPublicSets()
    }

    fun loadMySets() {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            _errorMessage.value = "Користувач не авторизований для завантаження 'Моїх наборів'."
            _mySets.value = emptyList()
            return
        }
        _isLoadingMySets.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = firebaseRepository.getMySharedSets(userId)
            result.fold(
                onSuccess = { sets -> _mySets.value = sets },
                onFailure = { exception ->
                    _errorMessage.value = "Помилка завантаження 'Моїх наборів': ${exception.message}"
                    Log.e("SetsVM", "Error loading my sets", exception)
                }
            )
            _isLoadingMySets.value = false
        }
    }

    fun loadPublicSets() {
        val userId = authRepository.getCurrentUser()?.uid ?: ""
        _isLoadingPublicSets.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = firebaseRepository.getPublicSharedSets(userId)
            result.fold(
                onSuccess = { sets -> _publicSets.value = sets },
                onFailure = { exception ->
                    _errorMessage.value = "Помилка завантаження публічних наборів: ${exception.message}"
                    Log.e("SetsVM", "Error loading public sets", exception)
                }
            )
            _isLoadingPublicSets.value = false
        }
    }
    fun requestDeleteSet(setId: String, setName: String) {
        setToDeleteId = setId
        setToDeleteName = setName
        _showDeleteConfirmDialog.value = true
        _errorMessage.value = null
    }

    fun confirmDeleteSet() {
        val currentSetId = setToDeleteId
        if (currentSetId == null) {
            _errorMessage.value = "Помилка: ID набору для видалення не вказано."
            _showDeleteConfirmDialog.value = false
            return
        }

        _isLoadingMySets.value = true
        _showDeleteConfirmDialog.value = false

        viewModelScope.launch {
            val result = firebaseRepository.deleteSharedCardSet(currentSetId)
            result.fold(
                onSuccess = {
                    _errorMessage.value = "Набір '${setToDeleteName ?: currentSetId}' успішно видалено."
                    Log.i("SetsVM", "Set $currentSetId deleted successfully.")
                    loadMySets()
                },
                onFailure = { exception ->
                    _errorMessage.value = "Помилка видалення набору: ${exception.message}"
                    Log.e("SetsVM", "Error deleting set $currentSetId", exception)
                }
            )
            _isLoadingMySets.value = false
            setToDeleteId = null
            setToDeleteName = null
        }
    }

    fun cancelDeleteSet() {
        _showDeleteConfirmDialog.value = false
        setToDeleteId = null
        setToDeleteName = null
    }
    fun getSetNameToDelete(): String? = setToDeleteName

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}