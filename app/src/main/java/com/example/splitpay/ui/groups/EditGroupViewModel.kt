package com.example.splitpay.ui.groups

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.repository.FileStorageRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface EditGroupUiEvent {
    object GroupUpdated : EditGroupUiEvent
    object NavigateBack : EditGroupUiEvent
}

data class EditGroupUiState(
    val groupName: String = "",
    val selectedTag: String = "friends",
    val currentPhotoUrl: String = "",
    val selectedPhotoUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class EditGroupViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val fileStorageRepository: FileStorageRepository = FileStorageRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(EditGroupUiState())
    val uiState: State<EditGroupUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<EditGroupUiEvent>()
    val uiEvent: SharedFlow<EditGroupUiEvent> = _uiEvent.asSharedFlow()

    private var groupId: String = ""

    fun loadGroup(id: String) {
        groupId = id
        viewModelScope.launch {
            try {
                val group = groupsRepository.getGroupById(id)
                if (group != null) {
                    _uiState.value = _uiState.value.copy(
                        groupName = group.name,
                        selectedTag = group.iconIdentifier,
                        currentPhotoUrl = group.photoUrl
                    )
                }
            } catch (e: Exception) {
                logE("Failed to load group: ${e.message}")
            }
        }
    }

    fun onGroupNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(groupName = newName, error = null)
    }

    fun onTagSelected(tag: String) {
        _uiState.value = _uiState.value.copy(selectedTag = tag)
    }

    fun onPhotoSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedPhotoUri = uri)
    }

    fun onSaveClick() {
        val name = _uiState.value.groupName.trim()

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Group name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                var photoUrl = _uiState.value.currentPhotoUrl

                // Upload new photo if selected
                if (_uiState.value.selectedPhotoUri != null) {
                    logI("New photo selected, uploading...")
                    val uploadResult = fileStorageRepository.uploadGroupPhoto(groupId, _uiState.value.selectedPhotoUri!!)
                    uploadResult.fold(
                        onSuccess = { url ->
                            logI("Photo uploaded successfully: $url")
                            // Delete old photo if exists
                            if (photoUrl.isNotEmpty()) {
                                fileStorageRepository.deleteFile(photoUrl)
                            }
                            photoUrl = url
                        },
                        onFailure = { error ->
                            logE("Failed to upload photo: ${error.message}")
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to upload photo: ${error.message}",
                                isLoading = false
                            )
                            return@launch
                        }
                    )
                }

                // Update group
                groupsRepository.updateGroup(
                    groupId = groupId,
                    name = name,
                    iconIdentifier = _uiState.value.selectedTag,
                    photoUrl = photoUrl
                ).fold(
                    onSuccess = {
                        logI("Group updated successfully")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _uiEvent.emit(EditGroupUiEvent.GroupUpdated)
                    },
                    onFailure = { e ->
                        logE("Failed to update group: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            error = e.message ?: "Failed to update group",
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                logE("Error updating group: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "An error occurred",
                    isLoading = false
                )
            }
        }
    }

    fun onDeletePhoto() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val currentUrl = _uiState.value.currentPhotoUrl

                if (currentUrl.isNotEmpty()) {
                    fileStorageRepository.deleteFile(currentUrl)
                }

                // Update group to remove photo URL
                groupsRepository.updateGroup(
                    groupId = groupId,
                    photoUrl = ""
                ).fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            currentPhotoUrl = "",
                            selectedPhotoUri = null,
                            isLoading = false
                        )
                        logI("Group photo deleted successfully")
                    },
                    onFailure = { e ->
                        logE("Failed to delete photo: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to delete photo",
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                logE("Error deleting photo: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "An error occurred",
                    isLoading = false
                )
            }
        }
    }
}
