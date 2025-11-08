package com.example.splitpay.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.splitpay.ui.addfriend.AddFriendScreen
import com.example.splitpay.ui.addfriend.FriendProfilePreviewScreen
import com.example.splitpay.ui.expense.AddExpenseScreen
import com.example.splitpay.ui.friendsDetail.FriendSettingsScreen
import com.example.splitpay.ui.friendsDetail.FriendsDetailScreen
import com.example.splitpay.ui.groups.CreateGroupScreen
import com.example.splitpay.ui.groups.GroupDetailScreen
import com.example.splitpay.ui.groups.GroupSettingsScreen
import com.example.splitpay.ui.home.HomeScreen3
import com.example.splitpay.ui.login.LoginScreen
import com.example.splitpay.ui.signup.SignUpScreen
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.TextWhite
import com.example.splitpay.ui.welcome.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.splitpay.ui.groups.AddGroupMembersScreen // <-- ADD THIS IMPORT
import com.example.splitpay.ui.groups.GroupSettingsScreen
import com.example.splitpay.ui.recordPayment.RecordPaymentScreen
import com.example.splitpay.ui.settleUp.SettleUpScreen
import kotlin.math.absoluteValue

// Navigation.kt
@Composable
fun Navigation(
    navController: NavHostController,
) {

    // Reactive start destination
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Screen.Home
    } else {
        Screen.Welcome
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Helper function to show snackbar
    val showSnackbar: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = Screen.Welcome,
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
        ) {
            WelcomeScreen(
                onNavigateToSignUp = { navController.navigate(Screen.SignUp) },
                onNavigateToLogIn = { navController.navigate(Screen.Login) }
            )
        }
        composable(
            route = Screen.SignUp,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToRight() }
        ) {
            SignUpScreen(
                onNavigateToHome = {
                    navController.navigateSingleTopTo(Screen.Home, Screen.Welcome)
                },
                onNavigateToBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Login,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToRight() }
        ) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigateSingleTopTo(Screen.Home, Screen.Welcome)
                },
                onNavigateToBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Home,
        ) {
            HomeScreen3(mainNavController = navController/*, snackbarHostState = snackbarHostState */)
        }

        // Route for creating a group
        composable(
            route = Screen.CreateGroup,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            CreateGroupScreen(
                // On success, navigate to the group detail page, clearing the CreateGroup backstack entry
                onGroupCreated = { groupId ->
                    navController.navigate("group_detail/$groupId") {
                        popUpTo(Screen.CreateGroup) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Route for viewing a specific group's details
        composable(
            route = Screen.GroupDetail,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupDetailScreen(
                groupId = groupId,
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.NonGroupDetail, // Uses the NEW constant route
            // No arguments needed for this specific route
            enterTransition = { slideInFromRight() }, // Add transitions
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            // Call the existing screen but pass the SPECIAL identifier
            GroupDetailScreen(
                groupId = "non_group", // Pass the constant identifier
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- Add Expense Routes ---
        composable(
            route = Screen.AddExpenseRoute, // Use constant from Screen.kt
            arguments = listOf(
                // Define groupId as a query parameter, making it optional and nullable
                navArgument("groupId") {
                    type = NavType.StringType
                    nullable = true // Mark as nullable
                    defaultValue = null // Explicitly set default to null
                }
            )            // ... transitions ...
        ) { backStackEntry ->
            val groupIdFromBackStack = backStackEntry.arguments?.getString("groupId")
            Log.d(
                "AddExpenseDebug",
                "Nav->AddExpenseWithGroup: Received groupId = $groupIdFromBackStack"
            )
            AddExpenseScreen(
                groupId = groupIdFromBackStack,
                navBackStackEntry = backStackEntry,
                prefilledGroupId = groupIdFromBackStack,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navEvent -> 
                    // Navigate back to the previous screen (GroupDetailScreen)
                    navController.popBackStack() }
            )
        }

        // --- Add Friend Flow Routes ---
        composable(
            route = Screen.AddFriend,
            // ... transitions ...
        ) { // NavBackStackEntry not strictly needed here unless viewModel uses it
            AddFriendScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfilePreview = { userId, username ->
                    // Encode username if it might contain special characters (safer)
                    val encodedUsername = java.net.URLEncoder.encode(username, "UTF-8")
                    navController.navigate("${Screen.FriendProfilePreview}/$userId?username=$encodedUsername")
                }
            )
        }

        composable(
            route = Screen.FriendProfilePreviewRoute, // Use constant from Screen.kt
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                // Username is technically optional in the route, handle null if needed
                navArgument("username") { type = NavType.StringType; nullable = true }
            ),
            // ... transitions ...
        ) { backStackEntry ->
            // Pass the NavBackStackEntry for ViewModel factory
            FriendProfilePreviewScreen(
                navBackStackEntry = backStackEntry,
                onNavigateBack = { navController.popBackStack() },
                showSnackbar = showSnackbar // Pass the snackbar lambda
            )
        }
        composable(
            route = Screen.FriendDetailRoute,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType })
            // Add transitions if desired
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: "N/A"
            // Replace with your actual FriendDetailScreen composable later
            FriendsDetailScreen(
                friendId = friendId,
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        //Friend Settings Screen
        composable(
            route = Screen.FriendSettingsRoute,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() }, // Example transition
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            FriendSettingsScreen(
                friendId = friendId,
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        //Friend Settle Up Screen (Placeholder) 
        composable(
            route = Screen.SettleUpFriendRoute,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType }),
            // Add transitions
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            // --- Placeholder ---
            Box(
                modifier = Modifier.fillMaxSize().background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Settle Up with Friend: $friendId (Placeholder)", color = TextWhite)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
            // --- End Placeholder ---
        }
        composable(
            route = Screen.GroupSettings, // Use constant from Screen.kt
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() }, // Example transition
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupSettingsScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack(Screen.Home, inclusive = false) },
                // --- Pass navigation action for adding members ---
                onNavigateToAddMembers = { navController.navigate("add_group_members/$groupId") }
            )
        }

        composable(
            route = Screen.AddGroupMembers, // Use constant
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() }, // Example transitions
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            AddGroupMembersScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- NEW: Settle Up Flow ---
        composable(
            route = Screen.SettleUpRoute,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            // Add transitions (e.g., slide up from bottom)
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            SettleUpScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecordPayment = { gid, memberUid, balance ->
                    // Convert float to string for nav argument
                    val balanceStr = balance.toString()
                    navController.navigate("${Screen.RecordPayment}/$gid?memberUid=$memberUid&balance=$balanceStr")
                },
                onNavigateToMoreOptions = { gid ->
                    navController.navigate("${Screen.MoreOptions}/$gid")
                }
            )
        }

        composable(
            route = Screen.RecordPaymentRoute,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("memberUid") { type = NavType.StringType },
                navArgument("balance") { type = NavType.StringType } // Pass balance as string
            )
        ) { backStackEntry ->
            // --- REPLACE PLACEHOLDER ---
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val memberUid = backStackEntry.arguments?.getString("memberUid") ?: ""
            val balance = backStackEntry.arguments?.getString("balance") ?: "0.0"

            RecordPaymentScreen(
                groupId = groupId,
                memberUid = memberUid,
                balance = balance,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = {
                    // Pop back to the GroupDetailScreen, removing SettleUp from the stack
                    navController.popBackStack(Screen.SettleUpRoute, inclusive = true)
                }
            )
        }

        composable(
            route = Screen.MoreOptionsRoute,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            // Placeholder Screen
            Box(
                modifier = Modifier.fillMaxSize().background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("More Options (Placeholder)", color = TextWhite)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back (Placeholder)")
                    }
                }
            }
        }

    } // End NavHost
}

