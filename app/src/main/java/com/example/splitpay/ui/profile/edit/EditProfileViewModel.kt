package com.example.splitpay.ui.profile.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.repository.FileStorageRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val fileStorageRepository: FileStorageRepository = FileStorageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<EditProfileUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            logD("Loading user profile for editing")

            try {
                val currentUser = userRepository.getCurrentUser()
                if (currentUser == null) {
                    logE("Cannot load profile: User not signed in")
                    _uiState.update { it.copy(error = "User not signed in", isLoading = false) }
                    return@launch
                }

                logD("Fetching profile for user: ${currentUser.uid}")
                val userDoc = userRepository.getUserProfile(currentUser.uid)
                if (userDoc != null) {
                    logI("Profile loaded successfully for editing: ${userDoc.username}")
                    _uiState.update {
                        it.copy(
                            fullName = userDoc.fullName,
                            username = userDoc.username,
                            email = currentUser.email ?: "",
                            phoneNumber = userDoc.phoneNumber,
                            profilePictureUrl = userDoc.profilePictureUrl,
                            qrCodeUrl = userDoc.qrCodeUrl,
                            isLoading = false
                        )
                    }
                } else {
                    logE("User profile not found in Firestore for UID: ${currentUser.uid}")
                    _uiState.update { it.copy(error = "User profile not found", isLoading = false) }
                }

            } catch (e: Exception) {
                logE("Error loading user profile for editing: ${e.message}", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to load profile", isLoading = false) }
            }
        }
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, fullNameError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPhoneNumberChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value, phoneNumberError = null) }
    }

    fun onProfilePictureSelected(uri: Uri) {
        logD("Profile picture selected: $uri")
        _uiState.update { it.copy(profilePictureUri = uri) }
    }

    fun onQrCodeSelected(uri: Uri) {
        logD("QR code selected: $uri")
        _uiState.update { it.copy(qrCodeUri = uri) }
    }

    fun removeProfilePicture() {
        logD("Removing profile picture")
        _uiState.update { it.copy(profilePictureUri = null, profilePictureUrl = "") }
    }

    fun removeQrCode() {
        logD("Removing QR code")
        _uiState.update { it.copy(qrCodeUri = null, qrCodeUrl = "") }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value

            // Validate inputs
            if (!validateInputs()) {
                logD("Profile save validation failed")
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            logI("Starting profile save process")

            try {
                val currentUser = userRepository.getCurrentUser()
                if (currentUser == null) {
                    logE("Cannot save profile: User not signed in")
                    _uiState.update { it.copy(error = "User not signed in", isSaving = false) }
                    return@launch
                }

                val uid = currentUser.uid
                val updates = mutableMapOf<String, Any>()

                // Upload profile picture if selected
                if (state.profilePictureUri != null) {
                    logD("Uploading new profile picture")
                    _uiState.update { it.copy(isUploadingProfilePicture = true) }

                    val uploadResult = fileStorageRepository.uploadProfilePicture(uid, state.profilePictureUri)
                    uploadResult.fold(
                        onSuccess = { url ->
                            logI("Profile picture uploaded successfully")
                            updates["profilePictureUrl"] = url
                            _uiState.update { it.copy(profilePictureUrl = url, isUploadingProfilePicture = false) }
                        },
                        onFailure = { e ->
                            logE("Failed to upload profile picture: ${e.message}", e)
                            _uiState.update {
                                it.copy(
                                    error = "Failed to upload profile picture: ${e.message}",
                                    isSaving = false,
                                    isUploadingProfilePicture = false
                                )
                            }
                            return@launch
                        }
                    )
                } else if (state.profilePictureUrl.isEmpty()) {
                    // User removed the profile picture
                    updates["profilePictureUrl"] = ""
                }

                // Upload QR code if selected
                if (state.qrCodeUri != null) {
                    logD("Uploading new QR code")
                    _uiState.update { it.copy(isUploadingQrCode = true) }

                    val uploadResult = fileStorageRepository.uploadQrCode(uid, state.qrCodeUri)
                    uploadResult.fold(
                        onSuccess = { url ->
                            logI("QR code uploaded successfully")
                            updates["qrCodeUrl"] = url
                            _uiState.update { it.copy(qrCodeUrl = url, isUploadingQrCode = false) }
                        },
                        onFailure = { e ->
                            logE("Failed to upload QR code: ${e.message}", e)
                            _uiState.update {
                                it.copy(
                                    error = "Failed to upload QR code: ${e.message}",
                                    isSaving = false,
                                    isUploadingQrCode = false
                                )
                            }
                            return@launch
                        }
                    )
                } else if (state.qrCodeUrl.isEmpty()) {
                    // User removed the QR code
                    updates["qrCodeUrl"] = ""
                }

                // Update text fields
                updates["fullName"] = state.fullName
                updates["email"] = state.email
                updates["phoneNumber"] = state.phoneNumber

                // Save all updates to Firestore
                logD("Saving profile updates to Firestore")
                val updateResult = userRepository.updateUserProfile(uid, updates)

                updateResult.fold(
                    onSuccess = {
                        logI("Profile saved successfully")
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                successMessage = "Profile updated successfully",
                                profilePictureUri = null,
                                qrCodeUri = null
                            )
                        }
                        // Navigate back after short delay
                        kotlinx.coroutines.delay(500)
                        _uiEvent.emit(EditProfileUiEvent.NavigateBack)
                    },
                    onFailure = { e ->
                        logE("Failed to save profile: ${e.message}", e)
                        _uiState.update {
                            it.copy(
                                error = "Failed to save profile: ${e.message}",
                                isSaving = false
                            )
                        }
                    }
                )

            } catch (e: Exception) {
                logE("Error saving profile: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to save profile",
                        isSaving = false,
                        isUploadingProfilePicture = false,
                        isUploadingQrCode = false
                    )
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val state = _uiState.value
        var isValid = true

        // Validate full name
        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(fullNameError = "Full name is required") }
            isValid = false
        }

        // Validate email
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email format") }
            isValid = false
        }

        // Validate phone number
        if (state.phoneNumber.isNotBlank() && !isValidPhoneNumber(state.phoneNumber)) {
            _uiState.update { it.copy(phoneNumberError = "Invalid phone number") }
            isValid = false
        }

        return isValid
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length < 10) return false
        val phonePattern = Regex("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$")
        return phonePattern.matches(phoneNumber)
    }

    fun navigateBack() {
        viewModelScope.launch {
            logD("Navigating back from Edit Profile screen")
            _uiEvent.emit(EditProfileUiEvent.NavigateBack)
        }
    }
}
