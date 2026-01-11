package com.example.splitpay.ui.groups.createGroup

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Activity // <-- NEW
import com.example.splitpay.data.model.ActivityType // <-- NEW
import com.example.splitpay.data.repository.ActivityRepository // <-- NEW
import com.example.splitpay.data.repository.FileStorageRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository // <-- NEW
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface CreateGroupUiEvent {
    data class GroupCreated(val groupId: String) : CreateGroupUiEvent
    object NavigateBack : CreateGroupUiEvent
}

data class CreateGroupUiState(
    val groupName: String = "",
    val selectedIcon: String = "friends", // Default tag
    val selectedPhotoUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
class CreateGroupViewModel(
    private val repository: GroupsRepository = GroupsRepository(),
    private val activityRepository: ActivityRepository = ActivityRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val fileStorageRepository: FileStorageRepository = FileStorageRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(CreateGroupUiState())
    val uiState: State<CreateGroupUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<CreateGroupUiEvent>()
    val uiEvent: SharedFlow<CreateGroupUiEvent> = _uiEvent.asSharedFlow()

    fun onGroupNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(groupName = newName, error = null)
    }

    fun onIconSelected(iconIdentifier: String) {
        _uiState.value = _uiState.value.copy(selectedIcon = iconIdentifier)
    }

    fun onPhotoSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(selectedPhotoUri = uri)
    }

    fun onCreateGroupClick() {
        val name = _uiState.value.groupName.trim() //remove leading/trailing spaces
        val icon = _uiState.value.selectedIcon
        val photoUri = _uiState.value.selectedPhotoUri

        //show error if group name is empty
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Group name cannot be empty")
            return
        }

        // start background process to create group
        viewModelScope.launch {
            // show loading
            _uiState.value = _uiState.value.copy(isLoading = true)

            var photoUrl = ""

            // if got photo, try upload, if fail show error and stop process
            if (photoUri != null) {
                logI("Photo selected, uploading to Firebase Storage...")
                val tempGroupId = FirebaseFirestore.getInstance().collection("groups").document().id

                val uploadResult = fileStorageRepository.uploadGroupPhoto(tempGroupId, photoUri)
                uploadResult.fold(
                    onSuccess = { url ->
                        logI("Photo uploaded successfully: $url")
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

            // create group in Firestore, if success emit event to navigate back, if fail show error
            repository.createGroup(name, icon, photoUrl)
                //if firestore create success, create activity log in firestore
                .onSuccess { newGroup ->
                    viewModelScope.launch {
                        val actor = userRepository.getCurrentUser()
                        val actorName = userRepository.getUserProfile(actor?.uid ?: "")?.username
                            ?: actor?.displayName
                            ?: "Someone"

                        val activity = Activity(
                            activityType = ActivityType.GROUP_CREATED.name,
                            actorUid = newGroup.createdByUid,
                            actorName = actorName,
                            involvedUids = newGroup.members,
                            groupId = newGroup.id,
                            groupName = newGroup.name,
                            displayText = newGroup.name
                        )
                        //create activity log in firestore
                        activityRepository.logActivity(activity)
                    }
                    // emit event to navigate to group tab with updated group list
                    _uiEvent.emit(CreateGroupUiEvent.GroupCreated(newGroup.id))
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to create group",
                        isLoading = false
                    )
                }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}