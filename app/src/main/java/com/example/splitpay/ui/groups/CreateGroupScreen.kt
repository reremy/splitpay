package com.example.splitpay.ui.groups

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.splitpay.ui.common.InputField
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.BorderGray
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite


val availableIcons = listOf(
    "travel" to Icons.Default.TravelExplore,
    "family" to Icons.Default.FamilyRestroom,
    "kitchen" to Icons.Default.Dining,
    "other" to Icons.Default.Group,
    "place" to Icons.Default.Place,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: CreateGroupViewModel = viewModel(),
    onGroupCreated: (String) -> Unit, // Changed to pass group ID
    onNavigateBack: () -> Unit
) {
    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            is CreateGroupUiEvent.GroupCreated -> onGroupCreated(event.groupId) // Navigate to Detail
            CreateGroupUiEvent.NavigateBack -> onNavigateBack()
        }
    }

    val uiState = viewModel.uiState.value

    // Image picker for group photo
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            viewModel.onIconSelected("custom_image")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Group",
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

            // --- Group Icon/Photo Preview (Clickable) ---
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    // Display selected image
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Group Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Display selected icon or placeholder
                    val icon = availableIcons.find { it.first == uiState.selectedIcon }?.second
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = "Group Icon",
                            tint = TextWhite,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Upload Photo",
                            tint = TextWhite,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Text(
                text = "Tap to upload photo",
                color = Color.Gray,
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

            // --- Icon Selector ---
            Text(
                text = "Select an Icon:",
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
                // Icon Options (removed the upload image option)
                availableIcons.forEach { (identifier, icon) ->
                    IconOption(
                        icon = icon,
                        identifier = identifier,
                        isSelected = uiState.selectedIcon == identifier && selectedImageUri == null,
                        onSelect = {
                            selectedImageUri = null // Clear selected image when icon is selected
                            viewModel.onIconSelected(identifier)
                        }
                    )
                }
            }
            Spacer(Modifier.height(48.dp))

            // --- Done Button ---
            Button(
                onClick = viewModel::onCreateGroupClick,
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
                    Text(text = "Done", fontSize = 20.sp, color = TextWhite)
                }
            }

            if (uiState.error != null) {
                Text(text = "Error: ${uiState.error}", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun IconOption(
    icon: ImageVector,
    identifier: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF66BB6A) else BorderGray
    val backgroundColor = if (isSelected) Color(0xFF3C3C3C) else Color(0xFF2D2D2D)

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable { onSelect(identifier) },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = identifier, tint = TextWhite, modifier = Modifier.size(32.dp))
    }
}
