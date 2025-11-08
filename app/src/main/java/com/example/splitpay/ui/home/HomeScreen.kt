package com.example.splitpay.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.splitpay.logger.logE
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.activity.ActivityScreen
import com.example.splitpay.ui.friends.FriendsScreenContent
import com.example.splitpay.ui.friends.FriendsTopBarActions
import com.example.splitpay.ui.friends.FriendsViewModel
import com.example.splitpay.ui.groups.ActivityTopBarActions
import com.example.splitpay.ui.groups.GroupsContent
import com.example.splitpay.ui.groups.GroupsTopBarActions
import com.example.splitpay.ui.groups.GroupsUiEvent
import com.example.splitpay.ui.profile.ProfileTopBarActions
import com.example.splitpay.ui.profile.UserProfileScreen
import kotlinx.coroutines.delay


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

    val friendsViewModel: FriendsViewModel = viewModel()
    val friendsUiState by friendsViewModel.uiState.collectAsState()

    // --- Focus Requester for Search Field ---
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Request focus and show keyboard when search becomes active
    LaunchedEffect(friendsUiState.isSearchActive) {
        if (friendsUiState.isSearchActive) {
            // Slight delay might be needed for UI to recompose before requesting focus
            delay(100)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                // Log error if focus request fails
                logE("Focus request failed: ${e.message}")
            }
        } else {
            keyboardController?.hide() // Hide keyboard when search closes
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // TOP BAR: Call the generic AppTopBar and inject the actions dynamically
        topBar = {
            // --- Use SINGLE AppTopBar, pass search state conditionally ---
            val isFriendsScreen = currentHomeRoute == "friends_screen"
            AppTopBar(
                title = title, // Standard title (hidden during search by AppTopBar)
                scrollBehavior = scrollBehavior,
                // Pass search state ONLY if on friends screen
                isSearchActive = isFriendsScreen && friendsUiState.isSearchActive,
                searchQuery = if (isFriendsScreen) friendsUiState.searchQuery else "",
                onSearchQueryChange = if (isFriendsScreen) friendsViewModel::onSearchQueryChange else { {} },
                onSearchClose = if (isFriendsScreen) friendsViewModel::onSearchCloseClick else { {} },
                focusRequester = focusRequester, // Pass focus requester
                // Standard actions (hidden during search by AppTopBar)
                actions = {
                    // Define actions based on route
                    when (currentHomeRoute) {
                        "groups_screen" -> GroupsTopBarActions(
                            onNavigateToCreateGroup = { mainNavController.navigate(Screen.CreateGroup) }
                        )
                        "friends_screen" -> FriendsTopBarActions(
                            onSearchClick = friendsViewModel::onSearchIconClick, // To activate search
                            onAddFriendClick = { mainNavController.navigate(Screen.AddFriend) },
                            onFilterClick = friendsViewModel::onFilterIconClick
                        )
                        "activity_screen" -> ActivityTopBarActions()
                        "profile_screen" -> ProfileTopBarActions(onEditProfile = { /* TODO */ })
                        else -> { /* Default Menu */ }
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
                    overallBalance = friendsUiState.totalNetBalance, // <-- PASS BALANCE HERE
                    // GroupsViewModel instance is created internally by default
                    onNavigate = { event ->
                        when (event) {
                            GroupsUiEvent.NavigateToCreateGroup -> mainNavController.navigate(Screen.CreateGroup)
                            is GroupsUiEvent.NavigateToGroupDetail -> { mainNavController.navigate("group_detail/${event.groupId}") }
                        }
                    },
                    navController = mainNavController // Pass main NavController for non-group navigation
                )
            }
            composable("friends_screen") {
                // Pass the same friendsViewModel instance down
                FriendsScreenContent(
                    innerPadding = innerPadding,
                    viewModel = friendsViewModel,
                    // --- Add friend click navigation ---
                    onFriendClick = { friendId ->
                        // Navigate to detail screen, passing the friend's UID
                        mainNavController.navigate("${Screen.FriendDetail}/$friendId")
                    }
                )
            }
            composable("activity_screen") {
                ActivityScreen(
                    innerPadding = innerPadding,
                    navController = mainNavController
                )
            }
            composable("profile_screen") {
                UserProfileScreen(mainNavController = mainNavController)
            }
        }
    }
}



