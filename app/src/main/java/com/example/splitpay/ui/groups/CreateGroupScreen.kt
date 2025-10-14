package com.example.splitpay.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground

@Composable
fun CreateGroupScreen(
    viewModel: CreateGroupViewModel = viewModel(),
    onGroupCreated: () -> Unit,
    onNavigateBack: () -> Unit
) {
    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            CreateGroupUiEvent.GroupCreated -> onGroupCreated()
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
        verticalArrangement = Arrangement.Center
    ) {
        // Placeholder UI elements
        Text(text = "Create New Group", color = Color.White)

        TextField(
            value = uiState.groupName,
            onValueChange = viewModel::onGroupNameChange,
            label = { Text("Group Name") },
            modifier = Modifier.padding(16.dp),
            singleLine = true
        )

        Button(
            onClick = viewModel::onCreateGroupClick,
            enabled = !uiState.isLoading
        ) {
            Text(text = if (uiState.isLoading) "Creating..." else "Create Group")
        }

        if (uiState.error != null) {
            Text(text = "Error: ${uiState.error}", color = Color.Red)
        }
    }
}