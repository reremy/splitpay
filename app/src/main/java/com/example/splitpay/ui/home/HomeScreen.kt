package com.example.splitpay.ui.home

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.splitpay.logger.logE
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.activity.activityTab.ActivityScreen
import com.example.splitpay.ui.friends.friendsTab.FriendsScreenContent
import com.example.splitpay.ui.friends.friendsTab.FriendsTopBarActions
import com.example.splitpay.ui.friends.friendsTab.FriendsViewModel
import com.example.splitpay.ui.activity.activityTab.ActivityTopBarActions
import com.example.splitpay.ui.activity.activityTab.ActivityViewModel
import com.example.splitpay.ui.groups.groupsTab.GroupsTabContent
import com.example.splitpay.ui.groups.GroupsTopBarActions
import com.example.splitpay.ui.groups.groupsTab.GroupsUiEvent
import com.example.splitpay.ui.groups.groupsTab.GroupsViewModel
import com.example.splitpay.ui.profile.ProfileTopBarActions
import com.example.splitpay.ui.profile.UserProfileScreen
import com.example.splitpay.ui.theme.PositiveGreen
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen3(
    mainNavController: NavHostController,
    viewModel: HomeViewModel = viewModel(),
    initialTab: String? = null
) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        val notificationPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()){
            isGranted ->
            if (isGranted){
                Log.i("HomeScreen","Notification permission granted")
            } else {
                Log.w("HomeScreen","Notification permission denied")
            }
        }

        LaunchedEffect(Unit){
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val homeNavController = rememberNavController()

    LaunchedEffect(initialTab) {
        if (initialTab == "activity") {
            homeNavController.navigate("activity_screen") {
                // Optional: Clear back stack to avoid back button confusion
                popUpTo("groups_screen") {
                    inclusive = false
                }
            }
        }
    }

    // Determine the current route for FAB/TopBar customization
    val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
    val currentHomeRoute = navBackStackEntry?.destination?.route ?: uiState.items.first().route
    val currentItem = uiState.items.firstOrNull { it.route == currentHomeRoute }
    val title = currentItem?.label ?: "SplitPay"

    val friendsViewModel: FriendsViewModel = viewModel()
    val friendsUiState by friendsViewModel.uiState.collectAsState()

    val groupsViewModel: GroupsViewModel = viewModel()
    val groupsUiState by groupsViewModel.uiState.collectAsState()

    val activityViewModel: ActivityViewModel = viewModel()
    val activityUiState by activityViewModel.uiState.collectAsState()

    // --- Focus Requester for Search Field ---
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Request focus and show keyboard when search becomes active (for friends, groups, or activity)
    LaunchedEffect(friendsUiState.isSearchActive, groupsUiState.isSearchActive, activityUiState.isSearchActive) {
        if (friendsUiState.isSearchActive || groupsUiState.isSearchActive || activityUiState.isSearchActive) {
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
            val isGroupsScreen = currentHomeRoute == "groups_screen"
            val isActivityScreen = currentHomeRoute == "activity_screen"

            AppTopBar(
                title = title, // Standard title (hidden during search by AppTopBar)
                scrollBehavior = scrollBehavior,
                // Pass search state for friends, groups, and activity screens
                isSearchActive = (isFriendsScreen && friendsUiState.isSearchActive) ||
                                (isGroupsScreen && groupsUiState.isSearchActive) ||
                                (isActivityScreen && activityUiState.isSearchActive),
                searchQuery = when {
                    isFriendsScreen -> friendsUiState.searchQuery
                    isGroupsScreen -> groupsUiState.searchQuery
                    isActivityScreen -> activityUiState.searchQuery
                    else -> ""
                },
                searchPlaceholder = when {
                    isFriendsScreen -> "Search friends..."
                    isGroupsScreen -> "Search groups..."
                    isActivityScreen -> "Search activities..."
                    else -> "Search..."
                },
                onSearchQueryChange = when {
                    isFriendsScreen -> friendsViewModel::onSearchQueryChange
                    isGroupsScreen -> groupsViewModel::onSearchQueryChange
                    isActivityScreen -> activityViewModel::onSearchQueryChange
                    else -> { {} }
                },
                onSearchClose = when {
                    isFriendsScreen -> friendsViewModel::onSearchCloseClick
                    isGroupsScreen -> groupsViewModel::onSearchCloseClick
                    isActivityScreen -> activityViewModel::onSearchDismiss
                    else -> { {} }
                },
                focusRequester = focusRequester, // Pass focus requester
                // Standard actions (hidden during search by AppTopBar)
                actions = {
                    // Define actions based on route
                    when (currentHomeRoute) {
                        "groups_screen" -> GroupsTopBarActions(
                            selectedFilter = groupsUiState.selectedFilter,
                            showFilterDropdown = groupsUiState.showFilterDropdown,
                            onSearchIconClick = groupsViewModel::onSearchIconClick,
                            onFilterSelected = groupsViewModel::onFilterSelected,
                            onToggleFilterDropdown = groupsViewModel::toggleFilterDropdown,
                            onNavigateToCreateGroup = { mainNavController.navigate(Screen.CreateGroup) }
                        )
                        "friends_screen" -> FriendsTopBarActions(
                            onSearchClick = friendsViewModel::onSearchIconClick,
                            onAddFriendClick = { mainNavController.navigate(Screen.AddFriend) },
                            onFilterClick = friendsViewModel::onFilterIconClick,
                            isFilterMenuExpanded = friendsUiState.isFilterMenuExpanded,
                            currentFilter = friendsUiState.currentFilter,
                            onDismissFilterMenu = friendsViewModel::onDismissFilterMenu,
                            onApplyFilter = friendsViewModel::applyFilter
                        )
                        "activity_screen" -> ActivityTopBarActions(
                            isSearchActive = activityUiState.isSearchActive,
                            searchQuery = activityUiState.searchQuery,
                            onSearchClick = activityViewModel::onSearchIconClick,
                            onSearchQueryChange = activityViewModel::onSearchQueryChange,
                            onSearchDismiss = activityViewModel::onSearchDismiss,
                            onFilterClick = activityViewModel::onFilterIconClick,
                            isFilterMenuExpanded = activityUiState.isFilterMenuExpanded,
                            onDismissFilterMenu = activityViewModel::onDismissFilterMenu,
                            activityFilter = activityUiState.activityFilter,
                            timePeriodFilter = activityUiState.timePeriodFilter,
                            sortOrder = activityUiState.sortOrder,
                            onActivityFilterChange = activityViewModel::onActivityFilterChange,
                            onTimePeriodFilterChange = activityViewModel::onTimePeriodFilterChange,
                            onSortOrderChange = activityViewModel::onSortOrderChange
                        )
                        "profile_screen" -> ProfileTopBarActions(
                            onEditProfile = { mainNavController.navigate(Screen.EditProfile) }
                        )
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
                    containerColor = PositiveGreen
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        },

        bottomBar = {
            HomeBottomBar(uiState, homeNavController, viewModel)
        }
    ) { innerPadding ->
        // ========================================
        // Material Motion: Smooth Flowing Tab Transitions
        // ========================================
        // Define tab order for directional animations
        val tabOrder = listOf("groups_screen", "friends_screen", "activity_screen", "profile_screen")

        NavHost(
            navController = homeNavController,
            startDestination = "groups_screen",
            modifier = Modifier.padding(innerPadding),
            // Material motion: Slide + Fade with spring physics for natural feel
            enterTransition = {
                // Determine slide direction based on tab order
                val targetIndex = tabOrder.indexOf(targetState.destination.route)
                val initialIndex = tabOrder.indexOf(initialState.destination.route)

                if (targetIndex > initialIndex) {
                    // Sliding forward (left to right tabs) - slide in from right
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 3 }, // Start 33% off screen
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                } else {
                    // Sliding backward - slide in from left
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            },
            exitTransition = {
                // Current screen slides out and fades
                val targetIndex = tabOrder.indexOf(targetState.destination.route)
                val initialIndex = tabOrder.indexOf(initialState.destination.route)

                if (targetIndex > initialIndex) {
                    // Sliding forward - current slides left
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                } else {
                    // Sliding backward - current slides right
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        ) {
            composable("groups_screen") {
                GroupsTabContent(
                    innerPadding = innerPadding,
                    overallBalance = friendsUiState.totalNetBalance, // <-- PASS BALANCE HERE
                    viewModel = groupsViewModel, // Pass the same GroupsViewModel instance
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
                    navController = mainNavController,
                    viewModel = activityViewModel
                )
            }
            composable("profile_screen") {
                UserProfileScreen(mainNavController = mainNavController)
            }
        }
    }
}



