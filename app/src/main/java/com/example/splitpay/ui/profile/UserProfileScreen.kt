package com.example.splitpay.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import com.example.splitpay.ui.home.AppTopBar // Import AppTopBar and TopBarActions from Home package
import java.util.Locale

// Placeholder for the Profile Top Bar Actions (now defined in HomeScreen.kt)


@Composable
fun UserProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    mainNavController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            ProfileUiEvent.NavigateToWelcome -> {
                mainNavController.navigate(Screen.Welcome) {
                    // Clear the backstack and navigate to Welcome screen
                    popUpTo(mainNavController.graph.id) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = Color.Red)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Profile Header ---
                ProfileHeader(uiState.fullName, uiState.username)

                Spacer(modifier = Modifier.height(32.dp))

                // --- Account Details Section ---
                SettingsCard(title = "Account Details", sections = listOf(
                    SettingsItemData(Icons.Default.Person, "Full Name", uiState.fullName),
                    SettingsItemData(Icons.Default.AccountCircle, "Username", "@${uiState.username}"),
                    SettingsItemData(Icons.Default.Email, "Email", uiState.email),
                ))

                Spacer(modifier = Modifier.height(16.dp))

                // --- Preferences Section ---
                SettingsCard(title = "Preferences", sections = listOf(
                    SettingsItemData(Icons.Default.AttachMoney, "Default Currency", "MYR", isClickable = true),
                    SettingsItemData(Icons.Default.Settings, "App Settings", "", isClickable = true),
                ))

                Spacer(modifier = Modifier.height(16.dp))

                // --- Security & Actions Section ---
                SettingsCard(title = "Security & Actions", sections = listOf(
                    SettingsItemData(Icons.Default.Lock, "Change Password", "", isClickable = true),
                ))

                Spacer(modifier = Modifier.height(32.dp))

                // --- Log Out Button ---
                Button(
                    onClick = { viewModel.signOut() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Log Out")
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out", fontSize = 16.sp)
                }
            }
        }
    }
}

// --- Helper Data Class ---
data class SettingsItemData(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val isClickable: Boolean = false
)

// --- Profile Header Component ---
@Composable
fun ProfileHeader(fullName: String, username: String) {
    // Circle profile picture placeholder
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(PrimaryBlue),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = fullName.take(1).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.headlineLarge,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = fullName,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = TextWhite
    )

    Text(
        text = "@$username",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
}

// --- Settings Card Component ---
@Composable
fun SettingsCard(title: String, sections: List<SettingsItemData>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)), // Dark gray card background
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column {
                sections.forEachIndexed { index, item ->
                    SettingsListItem(item)
                    if (index < sections.size - 1) {
                        Divider(
                            color = Color(0xFF454545),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 56.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Settings List Item Component ---
@Composable
fun SettingsListItem(item: SettingsItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.isClickable) {
                /* TODO: Handle click for navigation/action */
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(item.label, color = TextWhite, fontSize = 16.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.value, color = Color.Gray, fontSize = 16.sp)
            if (item.isClickable) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = Color.Gray
                )
            }
        }
    }
}
