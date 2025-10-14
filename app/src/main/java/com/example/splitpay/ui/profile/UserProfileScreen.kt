package com.example.splitpay.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.profile.ProfileUiEvent
import com.example.splitpay.ui.profile.ProfileViewModel
import com.example.splitpay.ui.signup.SignUpUiEvent

@Composable
fun UserProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    mainNavController: NavHostController
) {

// Handle UI events
    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            ProfileUiEvent.NavigateToWelcome -> {
                mainNavController.navigate(Screen.Welcome) {
                    popUpTo(0) { inclusive = true } // clears backstack
                }
            }
        }
    }

    val uiState = viewModel.uiState.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    if (uiState.isLoading) {
        Text("Loading profile...")
    } else if (uiState.error != null) {
        Text("Error: ${uiState.error}")
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.fullName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "@${uiState.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Email: ${uiState.email}")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.signOut() }
            ) {
                Text("Sign Out")
            }
        }
    }
}
