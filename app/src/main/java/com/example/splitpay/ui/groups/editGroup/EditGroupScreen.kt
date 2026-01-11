package com.example.splitpay.ui.groups.editGroup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import coil.compose.AsyncImage
import com.example.splitpay.ui.common.InputField
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.groups.createGroup.availableTags
import com.example.splitpay.ui.groups.createGroup.TagOption
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.SurfaceDark
import com.example.splitpay.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupScreen(
    groupId: String,
    viewModel: EditGroupViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    // Load group data on first composition
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            EditGroupUiEvent.GroupUpdated -> onNavigateBack()
            EditGroupUiEvent.NavigateBack -> onNavigateBack()
        }
    }

    val uiState = viewModel.uiState.value

    // Image picker for group photo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onPhotoSelected(uri)
    }

    // Delete photo confirmation dialog
    var showDeletePhotoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Group",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(24.dp))

            // --- Group Photo Preview (Clickable) ---
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    // Show selected new photo, or current photo, or camera icon
                    when {
                        uiState.selectedPhotoUri != null -> {
                            AsyncImage(
                                model = uiState.selectedPhotoUri,
                                contentDescription = "New Group Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        uiState.currentPhotoUrl.isNotEmpty() -> {
                            AsyncImage(
                                model = uiState.currentPhotoUrl,
                                contentDescription = "Group Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Upload Photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Delete photo button (X) if photo exists
                if (uiState.currentPhotoUrl.isNotEmpty() || uiState.selectedPhotoUri != null) {
                    IconButton(
                        onClick = { showDeletePhotoDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete Photo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = if (uiState.currentPhotoUrl.isNotEmpty() || uiState.selectedPhotoUri != null)
                    "Tap to change photo" else "Tap to upload photo",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(24.dp))

            // --- Group Name Input ---
            InputField(
                label = "Group Name",
                placeholder = "e.g., Summer Trip, Roommates",
                value = uiState.groupName,
                onValueChange = viewModel::onGroupNameChange,
                isError = uiState.error != null,
                supportingText = uiState.error,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            // --- Tag Selector ---
            Text(
                text = "Select a tag:",
                color = TextWhite,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                availableTags.forEach { (identifier, icon) ->
                    TagOption(
                        icon = icon,
                        label = identifier.replaceFirstChar { it.uppercase() },
                        identifier = identifier,
                        isSelected = uiState.selectedTag == identifier,
                        onSelect = viewModel::onTagSelected
                    )
                }
            }
            Spacer(Modifier.height(48.dp))

            // --- Save Button ---
            Button(
                onClick = viewModel::onSaveClick,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text(text = "Save Changes", fontSize = 20.sp, color = TextWhite)
                }
            }

            if (uiState.error != null) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Delete Photo Confirmation Dialog
    if (showDeletePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePhotoDialog = false },
            title = { Text("Delete Group Photo?", color = TextWhite) },
            text = {
                Text(
                    "This will permanently delete the group photo. You can upload a new one later.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDeletePhoto()
                        showDeletePhotoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePhotoDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
