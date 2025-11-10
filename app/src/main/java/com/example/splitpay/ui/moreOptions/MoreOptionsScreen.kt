package com.example.splitpay.ui.moreOptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite

// --- ViewModel Factory ---
class MoreOptionsViewModelFactory(
    private val groupsRepository: GroupsRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoreOptionsViewModel::class.java)) {
            return MoreOptionsViewModel(groupsRepository, userRepository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRecordPayment: (String, String, String) -> Unit, // groupId, payerUid, recipientUid
    groupsRepository: GroupsRepository = GroupsRepository(),
    userRepository: UserRepository = UserRepository()
) {
    val savedStateHandle = SavedStateHandle(mapOf("groupId" to groupId))
    val factory = MoreOptionsViewModelFactory(groupsRepository, userRepository, savedStateHandle)
    val viewModel: MoreOptionsViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            MoreOptionsUiEvent.NavigateBack -> onNavigateBack()
            is MoreOptionsUiEvent.NavigateToRecordPayment -> {
                onNavigateToRecordPayment(event.groupId, event.payerUid, event.recipientUid)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.selectionStep) {
                            SelectionStep.SELECT_PAYER -> "Select Payer"
                            SelectionStep.SELECT_RECIPIENT -> "Select Recipient"
                        },
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClick() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2D2D2D)
                )
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    MoreOptionsContent(
                        uiState = uiState,
                        onMemberSelected = { member -> viewModel.onMemberSelected(member) }
                    )
                }
            }
        }
    }
}

@Composable
fun MoreOptionsContent(
    uiState: MoreOptionsUiState,
    onMemberSelected: (User) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Instructions
        Text(
            text = when (uiState.selectionStep) {
                SelectionStep.SELECT_PAYER -> "Select who will make the payment"
                SelectionStep.SELECT_RECIPIENT -> "Select who will receive the payment"
            },
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Show selected payer if on recipient selection step
        if (uiState.selectionStep == SelectionStep.SELECT_RECIPIENT && uiState.selectedPayer != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payer: ", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    MemberListItem(
                        member = uiState.selectedPayer,
                        onClick = {},
                        isClickable = false
                    )
                }
            }
        }

        // Member list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                // Filter out selected payer when selecting recipient
                if (uiState.selectionStep == SelectionStep.SELECT_RECIPIENT) {
                    uiState.members.filter { it.uid != uiState.selectedPayer?.uid }
                } else {
                    uiState.members
                },
                key = { it.uid }
            ) { member ->
                MemberListItem(
                    member = member,
                    onClick = { onMemberSelected(member) },
                    isClickable = true
                )
            }
        }
    }
}

@Composable
fun MemberListItem(
    member: User,
    onClick: () -> Unit,
    isClickable: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable) Modifier.clickable(onClick = onClick) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isClickable) Color(0xFF2D2D2D) else Color(0xFF3C3C3C)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            if (member.profilePictureUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = member.profilePictureUrl,
                    contentDescription = "${member.username}'s profile",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = member.username,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Member name
            Column {
                Text(
                    text = member.username.takeIf { it.isNotBlank() }
                        ?: member.fullName.takeIf { it.isNotBlank() }
                        ?: "Unknown",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (member.fullName.isNotBlank() && member.username != member.fullName) {
                    Text(
                        text = member.fullName,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
