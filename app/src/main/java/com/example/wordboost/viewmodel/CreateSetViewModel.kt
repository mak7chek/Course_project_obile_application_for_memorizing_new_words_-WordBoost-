package com.example.wordboost.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordboost.data.firebase.AuthRepository
import com.example.wordboost.data.firebase.FirebaseRepository
import com.example.wordboost.data.model.DifficultyLevel
import com.example.wordboost.data.model.SharedCardSet
import com.example.wordboost.data.model.SharedSetWordItem
import com.example.wordboost.data.repository.TranslationRepository
import kotlinx.coroutines.launch
import java.util.UUID

class CreateSetViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val translationRepository: TranslationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val setNameUk = mutableStateOf("")
    val setNameUkError = mutableStateOf<String?>(null)
    val setNameEn = mutableStateOf("")
    val selectedDifficulty = mutableStateOf(DifficultyLevel.MEDIUM)
    val isSetPublic = mutableStateOf(true)
    val currentStep = mutableStateOf(1)
    val isLoading = mutableStateOf(false)
    val operationMessage = mutableStateOf<String?>(null)

    val temporaryWordsList = mutableStateListOf<SharedSetWordItem>()
    val currentOriginalWord = mutableStateOf("")
    val currentTranslationWord = mutableStateOf("")
    val editingWordUiId = mutableStateOf<String?>(null)

    private var editingSetId: String? = null
    val isEditingMode: Boolean
        get() = editingSetId != null

    fun onSetNameUkChanged(newName: String) { setNameUk.value = newName; if (newName.isNotBlank()) setNameUkError.value = null }
    fun proceedToStep2() {
        if (setNameUk.value.isBlank()) {
            setNameUkError.value = "Назва набору не може бути порожньою."
            return
        }
        setNameUkError.value = null
        operationMessage.value = null
        isLoading.value = true
        val textToTranslate = setNameUk.value.trim()
        Log.d("CreateSetVM", "[proceedToStep2] Translating set name '$textToTranslate' to EN.")
        viewModelScope.launch {
            val translatedName = try {
                translationRepository.translateForSetCreationSuspend(textToTranslate, "en")
            } catch (e: Exception) {
                Log.e("CreateSetVM", "[proceedToStep2] Error translating set name", e)
                null
            }
            isLoading.value = false
            setNameEn.value = translatedName?.trim() ?: ""
            currentStep.value = 2
            if (translatedName == null) {
                operationMessage.value = "Авто-переклад назви набору не вдався."
            }
            Log.d("CreateSetVM", "[proceedToStep2] Translated to '${setNameEn.value}'. Step: ${currentStep.value}")
        }
    }
    fun onSetNameEnChanged(newName: String) { setNameEn.value = newName }
    fun onDifficultyChanged(newDifficulty: DifficultyLevel) { selectedDifficulty.value = newDifficulty }
    fun proceedToStep3() { operationMessage.value = null; currentStep.value = 3; Log.d("CreateSetVM", "Proceeding to step 3") }
    fun onOriginalWordChanged(text: String) { currentOriginalWord.value = text }
    fun onTranslationWordChanged(text: String) { currentTranslationWord.value = text }
    fun translateSetCreationInputFields() {
        operationMessage.value=null;val original=currentOriginalWord.value.trim();val translation=currentTranslationWord.value.trim();val textToTranslate:String;val targetLang:String;if(original.isNotBlank()&&translation.isBlank()){textToTranslate=original;targetLang="uk"}else if(translation.isNotBlank()&&original.isBlank()){textToTranslate=translation;targetLang="en"}else if(original.isNotBlank()&&translation.isNotBlank()){operationMessage.value="Обидва поля заповнені. Очистіть одне для перекладу.";return}else{operationMessage.value="Введіть слово в одне з полів для перекладу.";return};isLoading.value=true;Log.d("CreateSetVM","[translateInputFields] Calling translateForSetCreationSuspend for '$textToTranslate' -> $targetLang");viewModelScope.launch{val translatedText=try{translationRepository.translateForSetCreationSuspend(textToTranslate,targetLang)}catch(e:Exception){Log.e("CreateSetVM","[translateInputFields] Error calling suspend translation for word",e);null};Log.i("CreateSetVM","[translateInputFields] Suspend translation result: '$translatedText'. isLoading was: ${isLoading.value} before setting to false.");isLoading.value=false;if(translatedText!=null){val cleanTranslatedText=translatedText.trim();if(targetLang=="uk"){currentTranslationWord.value=cleanTranslatedText;Log.d("CreateSetVM","Updated currentTranslationWord to: '$cleanTranslatedText'")}else{currentOriginalWord.value=cleanTranslatedText;Log.d("CreateSetVM","Updated currentOriginalWord to: '$cleanTranslatedText'")}}}
    }
    fun addOrUpdateTemporaryWord() {
        val original = currentOriginalWord.value.trim()
        val translation = currentTranslationWord.value.trim()
        val currentUser = authRepository.getCurrentUser()

        if (original.isBlank() || translation.isBlank()) {
            operationMessage.value = "Обидва поля (оригінал та переклад) мають бути заповнені."
            return
        }
        if (currentUser == null || currentUser.uid.isBlank()) {
            operationMessage.value = "Помилка: не вдалося визначити автора. Увійдіть знову."
            return
        }

        editingWordUiId.value?.let { uiId ->
            val index = temporaryWordsList.indexOfFirst { it.id == uiId }
            if (index != -1) {
                temporaryWordsList[index] = temporaryWordsList[index].copy(
                    originalText = original,
                    translationText = translation
                )
            }
            editingWordUiId.value = null
        } ?: run {
            temporaryWordsList.add(
                SharedSetWordItem(
                    id = UUID.randomUUID().toString(),
                    originalText = original,
                    translationText = translation,
                    authorId = currentUser.uid
                )
            )
        }
        currentOriginalWord.value = ""
        currentTranslationWord.value = ""
        operationMessage.value = null
    }
    fun startEditTemporaryWord(wordItem: SharedSetWordItem) { editingWordUiId.value=wordItem.id;currentOriginalWord.value=wordItem.originalText;currentTranslationWord.value=wordItem.translationText}
    fun deleteTemporaryWord(wordItem: SharedSetWordItem) { temporaryWordsList.remove(wordItem);if(editingWordUiId.value==wordItem.id){currentOriginalWord.value="";currentTranslationWord.value="";editingWordUiId.value=null}}
    fun proceedToStep4() { if(temporaryWordsList.isEmpty()){operationMessage.value="Додайте хоча б одне слово до набору.";return};operationMessage.value=null;currentStep.value=4}
    fun onVisibilityChanged(isPublic: Boolean) { this.isSetPublic.value = isPublic }

    fun loadSetForEditing(setId: String) {
        if (isLoading.value || (editingSetId == setId && temporaryWordsList.isNotEmpty())) {
            Log.d("CreateSetVM", "loadSetForEditing: Already loading or set $setId data already present.")
            if (editingSetId == setId && temporaryWordsList.isNotEmpty()) return
        }

        Log.i("CreateSetVM", "[loadSetForEditing] Loading set with ID: $setId")
        resetInternalStateForEditing()
        this.editingSetId = setId
        isLoading.value = true
        operationMessage.value = null

        viewModelScope.launch {
            val result = firebaseRepository.getSharedCardSetWithWords(setId)

            result.fold(
                onSuccess = { setDetails ->
                    val setInfo = setDetails.setInfo
                    val words = setDetails.words

                    Log.d("CreateSetVM", "[loadSetForEditing] Successfully loaded set: ${setInfo.name_uk}, words: ${words.size}")
                    setNameUk.value = setInfo.name_uk
                    setNameEn.value = setInfo.name_en ?: ""
                    selectedDifficulty.value = DifficultyLevel.fromKey(setInfo.difficultyLevel) ?: DifficultyLevel.MEDIUM
                    isSetPublic.value = setInfo.public

                    temporaryWordsList.clear()
                    temporaryWordsList.addAll(words)

                    currentStep.value = 1
                    operationMessage.value = "Набір '${setInfo.name_uk}' завантажено для редагування."
                    isLoading.value = false
                },
                onFailure = { exception ->
                    isLoading.value = false
                    Log.e("CreateSetVM", "[loadSetForEditing] Failed to load set for editing", exception)
                    operationMessage.value = "Помилка завантаження набору: ${exception.message}"
                    this@CreateSetViewModel.editingSetId = null
                }
            )
        }
    }

    private fun resetInternalStateForEditing() {
        setNameUk.value = ""
        setNameUkError.value = null
        setNameEn.value = ""
        selectedDifficulty.value = DifficultyLevel.MEDIUM
        isSetPublic.value = true
        temporaryWordsList.clear()
        currentOriginalWord.value = ""
        currentTranslationWord.value = ""
        editingWordUiId.value = null
        Log.d("CreateSetVM", "Internal state reset for editing.")
    }

    fun saveFullSet() {
        val user = authRepository.getCurrentUser()
        if (user == null || user.uid.isBlank()) {
            operationMessage.value = "Помилка: користувач не авторизований."
            return
        }
        if (setNameUk.value.isBlank()) {
            operationMessage.value = "Назва набору українською не може бути порожньою."
            currentStep.value = 1
            return
        }
        if (temporaryWordsList.isEmpty()) {
            operationMessage.value = "Набір не може бути порожнім. Додайте слова."
            currentStep.value = 3
            return
        }

        isLoading.value = true
        operationMessage.value = null

        val wordsToSave = temporaryWordsList.map { wordItem ->
            if (wordItem.authorId.isBlank()) {
                wordItem.copy(authorId = user.uid)
            } else {
                wordItem
            }
        }

        val sharedSetObject = SharedCardSet(
            id = editingSetId ?: "",
            name_uk = setNameUk.value.trim(),
            name_en = setNameEn.value.trim().ifBlank { null },
            authorId = user.uid,
            authorName = user.displayName ?: user.email,
            difficultyLevel = selectedDifficulty.value.key,
            public = isSetPublic.value,
            wordCount = wordsToSave.size
        )

        viewModelScope.launch {
            val result: Result<*> = if (editingSetId != null) {
                Log.i("CreateSetVM", "[saveFullSet] Updating existing set with ID: $editingSetId")
                firebaseRepository.updateSharedCardSetAndWords(sharedSetObject, wordsToSave)
            } else {
                Log.i("CreateSetVM", "[saveFullSet] Creating new set.")
                firebaseRepository.createSharedCardSetInFirestore(sharedSetObject, wordsToSave)
            }

            isLoading.value = false
            result.fold(
                onSuccess = { resultData ->
                    val successMessage = if (editingSetId != null) {
                        "Набір '${sharedSetObject.name_uk}' успішно оновлено!"
                    } else {
                        "Набір '${sharedSetObject.name_uk}' успішно створено!"
                    }
                    operationMessage.value = successMessage
                    val finalSetId = if (resultData is String) resultData else editingSetId
                    Log.i("CreateSetVM", "Set operation successful. ID: $finalSetId. Editing: ${editingSetId != null}")
                },
                onFailure = { exception ->
                    val actionType = if (editingSetId != null) "оновлення" else "створення"
                    operationMessage.value = "Помилка $actionType набору: ${exception.message}"
                    Log.e("CreateSetVM", "Failed to $actionType set", exception)
                }
            )
        }
    }

    fun goBackStep() { if(currentStep.value>1){currentStep.value-=1;operationMessage.value=null}}

    fun resetAllState() {
        Log.d("CreateSetVM", "Resetting all state. Previous editingSetId: $editingSetId")
        setNameUk.value = ""
        setNameUkError.value = null
        setNameEn.value = ""
        selectedDifficulty.value = DifficultyLevel.MEDIUM
        temporaryWordsList.clear()
        currentOriginalWord.value = ""
        currentTranslationWord.value = ""
        editingWordUiId.value = null
        isSetPublic.value = true
        isLoading.value = false
        operationMessage.value = null
        currentStep.value = 1
        editingSetId = null
        Log.d("CreateSetVM", "All state has been reset. editingSetId is now: $editingSetId")
    }
}