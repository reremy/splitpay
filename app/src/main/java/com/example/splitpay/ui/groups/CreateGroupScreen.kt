package com.example.splitpay.ui.groups

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // --- Top Bar/Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Text(text = "New Group", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(48.dp)) // Spacer to balance the back button
        }
        Spacer(Modifier.height(32.dp))

        // --- Group Icon Preview ---
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            val icon = availableIcons.find { it.first == uiState.selectedIcon }?.second
            if (icon != null) {
                Icon(icon, contentDescription = "Group Icon", tint = TextWhite, modifier = Modifier.size(48.dp))
            } else {
                // Placeholder for custom image upload
                Icon(Icons.Default.Image, contentDescription = "Custom Image", tint = TextWhite, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

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
            text = "Select an Icon or Upload Image:",
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
            // Icon Options
            availableIcons.forEach { (identifier, icon) ->
                IconOption(
                    icon = icon,
                    identifier = identifier,
                    isSelected = uiState.selectedIcon == identifier,
                    onSelect = viewModel::onIconSelected
                )
            }
            // Upload Image Placeholder
            UploadImageOption(onUploadClick = { viewModel.onIconSelected("custom_image") })
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

@Composable
fun UploadImageOption(onUploadClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0xFF2D2D2D))
            .border(2.dp, BorderGray, CircleShape)
            .clickable(onClick = onUploadClick), // Placeholder for image picker logic
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Image, contentDescription = "Upload Image", tint = TextWhite, modifier = Modifier.size(32.dp))
    }
}
