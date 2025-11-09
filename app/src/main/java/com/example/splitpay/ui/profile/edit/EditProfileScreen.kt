package com.example.splitpay.ui.profile.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = viewModel(),
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Image cropper for profile picture (circular crop)
    val profilePictureLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { viewModel.onProfilePictureSelected(it) }
        }
    }

    // Image cropper for QR code (square crop)
    val qrCodeLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { viewModel.onQrCodeSelected(it) }
        }
    }

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            EditProfileUiEvent.NavigateBack -> {
                navController.popBackStack()
            }
            is EditProfileUiEvent.ShowMessage -> {
                // Handle show message event if needed
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState)
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Picture Section
                    Text(
                        text = "Profile Picture",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val hasProfilePicture = uiState.profilePictureUri != null || uiState.profilePictureUrl.isNotEmpty()

                        if (hasProfilePicture) {
                            // Show the selected or existing image
                            AsyncImage(
                                model = uiState.profilePictureUri ?: uiState.profilePictureUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2D2D2D))
                                    .clickable {
                                        profilePictureLauncher.launch(
                                            CropImageContractOptions(
                                                uri = null,
                                                cropImageOptions = CropImageOptions(
                                                    imageSourceIncludeGallery = true,
                                                    imageSourceIncludeCamera = false,
                                                    cropShape = CropImageView.CropShape.OVAL,
                                                    aspectRatioX = 1,
                                                    aspectRatioY = 1,
                                                    fixAspectRatio = true,
                                                    guidelines = CropImageView.Guidelines.ON
                                                )
                                            )
                                        )
                                    },
                                contentScale = ContentScale.Crop
                            )
                            // Remove button
                            IconButton(
                                onClick = { viewModel.showDeleteProfilePictureDialog() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // Default avatar with add button
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                                    .clickable {
                                        profilePictureLauncher.launch(
                                            CropImageContractOptions(
                                                uri = null,
                                                cropImageOptions = CropImageOptions(
                                                    imageSourceIncludeGallery = true,
                                                    imageSourceIncludeCamera = false,
                                                    cropShape = CropImageView.CropShape.OVAL,
                                                    aspectRatioX = 1,
                                                    aspectRatioY = 1,
                                                    fixAspectRatio = true,
                                                    guidelines = CropImageView.Guidelines.ON
                                                )
                                            )
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.fullName.isNotEmpty()) {
                                    Text(
                                        text = uiState.fullName.take(1).uppercase(Locale.getDefault()),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 48.sp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Add Photo",
                                        tint = TextWhite,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }

                        // Upload progress indicator
                        if (uiState.isUploadingProfilePicture) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(120.dp),
                                color = PrimaryBlue,
                                strokeWidth = 4.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // QR Code Section
                    Text(
                        text = "Payment QR Code",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clickable {
                                qrCodeLauncher.launch(
                                    CropImageContractOptions(
                                        uri = null,
                                        cropImageOptions = CropImageOptions(
                                            imageSourceIncludeGallery = true,
                                            imageSourceIncludeCamera = false,
                                            cropShape = CropImageView.CropShape.RECTANGLE,
                                            aspectRatioX = 1,
                                            aspectRatioY = 1,
                                            fixAspectRatio = true,
                                            guidelines = CropImageView.Guidelines.ON
                                        )
                                    )
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val hasQrCode = uiState.qrCodeUri != null || uiState.qrCodeUrl.isNotEmpty()

                            if (hasQrCode) {
                                AsyncImage(
                                    model = uiState.qrCodeUri ?: uiState.qrCodeUrl,
                                    contentDescription = "QR Code",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                                // Remove button
                                IconButton(
                                    onClick = { viewModel.showDeleteQrCodeDialog() },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Red, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.QrCode2,
                                        contentDescription = "Add QR Code",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tap to add QR code", color = Color.Gray, fontSize = 14.sp)
                                }
                            }

                            // Upload progress indicator
                            if (uiState.isUploadingQrCode) {
                                CircularProgressIndicator(color = PrimaryBlue)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Form Fields
                    Text(
                        text = "Personal Information",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Full Name
                    OutlinedTextField(
                        value = uiState.fullName,
                        onValueChange = { viewModel.onFullNameChange(it) },
                        label = { Text("Full Name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                        },
                        isError = uiState.fullNameError != null,
                        supportingText = {
                            uiState.fullNameError?.let { Text(it, color = Color.Red) }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = PrimaryBlue,
                            focusedLabelColor = PrimaryBlue,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Username (Read-only)
                    OutlinedTextField(
                        value = "@${uiState.username}",
                        onValueChange = {},
                        label = { Text("Username") },
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray)
                        },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = Color(0xFF454545),
                            disabledTextColor = Color.Gray,
                            disabledLabelColor = Color.Gray,
                            disabledLeadingIconColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue)
                        },
                        isError = uiState.emailError != null,
                        supportingText = {
                            uiState.emailError?.let { Text(it, color = Color.Red) }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = PrimaryBlue,
                            focusedLabelColor = PrimaryBlue,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Number
                    OutlinedTextField(
                        value = uiState.phoneNumber,
                        onValueChange = { viewModel.onPhoneNumberChange(it) },
                        label = { Text("Phone Number") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue)
                        },
                        placeholder = { Text("+60123456789") },
                        isError = uiState.phoneNumberError != null,
                        supportingText = {
                            uiState.phoneNumberError?.let { Text(it, color = Color.Red) }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = PrimaryBlue,
                            focusedLabelColor = PrimaryBlue,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error message
                    val errorMessage = uiState.error
                    errorMessage?.let { message -> // This block only runs if errorMessage is not null
                        Text(
                            text = message, // 'message' is now smart-cast to a non-nullable String
                            color = Color.Red,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Success message
                    val successMessage = uiState.successMessage
                    if (successMessage != null) {
                        Text(
                            text = successMessage,
                            color = Color.Green,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Save Button
                    Button(
                        onClick = { viewModel.saveProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = TextWhite
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Saving...", fontSize = 16.sp)
                        } else {
                            Text("Save Changes", fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cancel Button
                    OutlinedButton(
                        onClick = { viewModel.navigateBack() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !uiState.isSaving
                    ) {
                        Text("Cancel", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Delete Profile Picture Confirmation Dialog
    if (uiState.showDeleteProfilePictureDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteProfilePictureDialog() },
            title = { Text("Delete Profile Picture?", color = TextWhite) },
            text = {
                Text(
                    "This will permanently delete your profile picture from storage. You'll need to upload a new one if you want to add it again.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteProfilePicture() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteProfilePictureDialog() }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2D2D2D)
        )
    }

    // Delete QR Code Confirmation Dialog
    if (uiState.showDeleteQrCodeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteQrCodeDialog() },
            title = { Text("Delete QR Code?", color = TextWhite) },
            text = {
                Text(
                    "This will permanently delete your payment QR code from storage. You'll need to upload a new one if you want to add it again.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteQrCode() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteQrCodeDialog() }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2D2D2D)
        )
    }
}
