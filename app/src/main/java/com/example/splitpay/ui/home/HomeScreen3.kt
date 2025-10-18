package com.example.splitpay.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.groups.ActivityTopBarActions
import com.example.splitpay.ui.groups.FriendsTopBarActions
import com.example.splitpay.ui.groups.GroupsContent
import com.example.splitpay.ui.groups.GroupsTopBarActions
import com.example.splitpay.ui.groups.GroupsUiEvent
import com.example.splitpay.ui.profile.UserProfileScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen3(
    mainNavController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val homeNavController = rememberNavController()

    // Determine the current route for FAB/TopBar customization
    val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
    val currentHomeRoute = navBackStackEntry?.destination?.route ?: uiState.items.first().route
    val currentItem = uiState.items.firstOrNull { it.route == currentHomeRoute }
    val title = currentItem?.label ?: "SplitPay"

    Scaffold(

        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        // TOP BAR: Call the generic AppTopBar and inject the actions dynamically
        topBar = {
            AppTopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                actions = {
                    when (currentHomeRoute) {
                        "groups_screen" -> GroupsTopBarActions(
                            onNavigateToCreateGroup = { mainNavController.navigate(Screen.CreateGroup) }
                        )
                        "friends_screen" -> FriendsTopBarActions()
                        "activity_screen" -> ActivityTopBarActions()
                        // Use a general menu/settings icon for all other screens (like Profile)
                        else -> {
                            IconButton(onClick = { /* open menu or settings */ }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    }
                }
            )
        },

        // ADDED FAB logic
        floatingActionButton = {
            if (currentHomeRoute != "profile_screen") {
                FloatingActionButton(
                    onClick = {
                        // **This FAB now directs to Add Expense (or other primary action)**
                        // Replace Screen.CreateGroup with your new "Screen.AddExpense" route
                        mainNavController.navigate(Screen.AddExpense) // TEMPORARY: Navigate to an expense/add screen
                    },
                    containerColor = Color(0xFF66BB6A)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        },

        bottomBar = {
            HomeBottomBar(uiState, homeNavController, viewModel)
        }
    ) { innerPadding ->
        NavHost(
            navController = homeNavController,
            startDestination = "groups_screen",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("groups_screen") {
                GroupsContent(
                    innerPadding = innerPadding,
                    onNavigate = { event ->
                        when (event) {
                            GroupsUiEvent.NavigateToCreateGroup -> mainNavController.navigate(Screen.CreateGroup)
                            is GroupsUiEvent.NavigateToGroupDetail -> { mainNavController.navigate("group_detail/${event.groupId}") }
                        }
                    }
                )
            }
            composable("friends_screen") { FriendsContent(innerPadding) }
            composable("activity_screen") { ActivityContent(innerPadding) }
            composable("profile_screen") {
                UserProfileScreen(mainNavController = mainNavController)
            }
        }
    }
}




@Composable
fun FriendsContent(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Friends Screen Content", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ActivityContent(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Activity Screen Content", style = MaterialTheme.typography.titleLarge)
    }
}
