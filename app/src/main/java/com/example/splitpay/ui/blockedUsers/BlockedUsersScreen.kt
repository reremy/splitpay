package com.example.splitpay.ui.blockedUsers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.splitpay.data.model.User
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onNavigateBack: () -> Unit,
    viewModel: BlockedUsersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Users", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = viewModel::clearError,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("OK")
                        }
                    }
                }
                uiState.blockedUsers.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = "No blocked users",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No blocked users",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Users you block will appear here",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.blockedUsers) { user ->
                            BlockedUserItem(
                                user = user,
                                isUnblocking = uiState.isUnblocking == user.uid,
                                onUnblockClick = { viewModel.unblockUser(user.uid) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedUserItem(
    user: User,
    isUnblocking: Boolean,
    onUnblockClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            if (user.profilePictureUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.profilePictureUrl,
                    contentDescription = "${user.username}'s profile picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "User",
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.fullName,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "@${user.username}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            // Unblock Button
            OutlinedButton(
                onClick = onUnblockClick,
                enabled = !isUnblocking,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryBlue,
                    disabledContentColor = Color.Gray
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isUnblocking) Color.Gray else PrimaryBlue)
                )
            ) {
                if (isUnblocking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.Gray
                    )
                } else {
                    Text("Unblock")
                }
            }
        }
    }
}
