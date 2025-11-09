package com.example.splitpay.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import java.util.Locale

@Composable
fun UserProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    mainNavController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            ProfileUiEvent.NavigateToWelcome -> {
                mainNavController.navigate(Screen.Welcome) {
                    popUpTo(mainNavController.graph.id) { inclusive = true }
                }
            }
            ProfileUiEvent.NavigateToEditProfile -> {
                mainNavController.navigate(Screen.EditProfile)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // QR Code Bottom Sheet
    if (uiState.showQrCode) {
        QrCodeBottomSheet(
            qrCodeUrl = uiState.qrCodeUrl,
            onDismiss = { viewModel.toggleQrCodeVisibility() }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        val currentError = uiState.error
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (currentError != null && !currentError.contains("Sign out", ignoreCase = true)) {            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = Color.Red)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.profilePictureUrl.isNotEmpty()) {
                        AsyncImage(
                            model = uiState.profilePictureUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D2D2D)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Default avatar with first letter
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.fullName.take(1).uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name and Username
                Text(
                    text = uiState.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 24.sp
                )

                Text(
                    text = "@${uiState.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Button
                if (uiState.qrCodeUrl.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.toggleQrCodeVisibility() },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = "QR Code", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Show Payment QR", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Account Details Card
                Text(
                    text = "Account Details",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column {
                        AccountDetailItem(Icons.Default.Person, "Full Name", uiState.fullName)
                        Divider(color = Color(0xFF454545), thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                        AccountDetailItem(Icons.Default.AccountCircle, "Username", "@${uiState.username}")
                        Divider(color = Color(0xFF454545), thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                        AccountDetailItem(Icons.Default.Email, "Email", uiState.email)
                        Divider(color = Color(0xFF454545), thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                        AccountDetailItem(Icons.Default.Phone, "Phone", uiState.phoneNumber)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Edit Profile Button
                OutlinedButton(
                    onClick = { viewModel.navigateToEditProfile() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Edit Profile")
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Log Out Button
                Button(
                    onClick = { viewModel.signOut() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !uiState.isLoggingOut
                ) {
                    if (uiState.isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = TextWhite
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Logging out...", fontSize = 16.sp)
                    } else {
                        Icon(Icons.Default.Logout, contentDescription = "Log Out")
                        Spacer(Modifier.width(8.dp))
                        Text("Log Out", fontSize = 16.sp)
                    }
                }

                // Show logout error if any
                if (!uiState.isLoading && currentError != null && currentError.contains("Sign out", ignoreCase = true)) {                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentError,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AccountDetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = TextWhite, fontSize = 16.sp)
        }
    }
}

@Composable
fun QrCodeBottomSheet(
    qrCodeUrl: String,
    onDismiss: () -> Unit
) {
    // This is a placeholder - you'll implement the actual bottom sheet
    // For now, showing a simple dialog
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Payment QR Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(Modifier.height(16.dp))

                if (qrCodeUrl.isNotEmpty()) {
                    AsyncImage(
                        model = qrCodeUrl,
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No QR Code uploaded", color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Scan to send payment",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
