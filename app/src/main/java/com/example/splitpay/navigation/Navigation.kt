package com.example.splitpay.navigation

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.splitpay.ui.addfriend.AddFriendScreen
import com.example.splitpay.ui.addfriend.FriendProfilePreviewScreen
import com.example.splitpay.ui.expense.addExpense.AddExpenseScreen
import com.example.splitpay.ui.expense.expenseDetail.ExpenseDetailScreen
import com.example.splitpay.ui.payment.paymentDetail.PaymentDetailScreen
import com.example.splitpay.ui.friends.friendSettings.FriendSettingsScreen
import com.example.splitpay.ui.friends.friendSettleUp.SelectBalanceToSettleScreen
import com.example.splitpay.ui.friends.friendSettleUp.SelectPayerFriendScreen
import com.example.splitpay.ui.friends.friendsDetail.FriendsDetailScreen
import com.example.splitpay.ui.groups.createGroup.CreateGroupScreen
import com.example.splitpay.ui.groups.editGroup.EditGroupScreen
import com.example.splitpay.ui.groups.groupDetail.GroupDetailScreen
import com.example.splitpay.ui.groups.groupSettings.GroupSettingsScreen
import com.example.splitpay.ui.home.HomeScreen3
import com.example.splitpay.ui.login.LoginScreen
import com.example.splitpay.ui.signup.SignUpScreen
import com.example.splitpay.ui.welcome.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.splitpay.ui.groups.addGroupMember.AddGroupMembersScreen // <-- ADD THIS IMPORT
import com.example.splitpay.ui.payment.recordPayment.RecordPaymentScreen
import com.example.splitpay.ui.settleUp.SettleUpScreen
import com.example.splitpay.ui.activity.activityDetail.ActivityDetailScreen
import com.example.splitpay.ui.profile.edit.EditProfileScreen
import com.example.splitpay.ui.blockedUsers.BlockedUsersScreen
import com.example.splitpay.ui.moreOptions.MoreOptionsScreen
import com.example.splitpay.ui.receipt.ReceiptReviewScreen
import com.example.splitpay.ui.receipt.ReceiptScannerScreen
import java.net.URLEncoder

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

        // Route for editing a group
        composable(
            route = Screen.EditGroup,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            EditGroupScreen(
                groupId = groupId,
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
                },
                // Define expenseId for edit mode
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )            // ... transitions ...
        ) { backStackEntry ->
            val groupIdFromBackStack = backStackEntry.arguments?.getString("groupId")
            val expenseIdFromBackStack = backStackEntry.arguments?.getString("expenseId")
            Log.d(
                "AddExpenseDebug",
                "Nav->AddExpenseWithGroup: Received groupId = $groupIdFromBackStack, expenseId = $expenseIdFromBackStack"
            )
            AddExpenseScreen(
                groupId = groupIdFromBackStack,
                expenseId = expenseIdFromBackStack,
                navBackStackEntry = backStackEntry,
                prefilledGroupId = groupIdFromBackStack,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navEvent ->
                    // Set refresh flag if we're editing (going back to ActivityDetail)
                    if (expenseIdFromBackStack != null) {
                        // We came from ActivityDetail, set the refresh flag
                        Log.d("Navigation", "Setting refresh_needed flag for ActivityDetail")
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
                        Log.d("Navigation", "Flag set, previous entry: ${navController.previousBackStackEntry?.destination?.route}")
                    }
                    // Navigate back to the previous screen
                    navController.popBackStack()
                }
            )
        }

        // --- Expense Detail Route ---
        composable(
            route = Screen.ExpenseDetailRoute,
            arguments = listOf(
                navArgument("expenseId") {
                    type = NavType.StringType
                }
            ),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            ExpenseDetailScreen(
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { expId, groupId ->
                    // Navigate to AddExpenseScreen in edit mode
                    navController.navigate("${Screen.AddExpense}?groupId=$groupId&expenseId=$expId")
                }
            )
        }

        // --- Payment Detail Route ---
        composable(
            route = Screen.PaymentDetailRoute,
            arguments = listOf(
                navArgument("paymentId") {
                    type = NavType.StringType
                }
            ),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val paymentId = backStackEntry.arguments?.getString("paymentId") ?: ""
            PaymentDetailScreen(
                paymentId = paymentId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { payId, groupId ->
                    // Navigate to RecordPaymentScreen in edit mode
                    navController.navigate("${Screen.RecordPayment}/$groupId?paymentId=$payId")
                }
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
                    val encodedUsername = URLEncoder.encode(username, "UTF-8")
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroupDetail = { groupId ->
                    navController.navigate("group_detail/$groupId")
                }
            )
        }

        // Select Balance to Settle Screen (Friend Settle Up)
        composable(
            route = Screen.SelectBalanceToSettleRoute,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            SelectBalanceToSettleScreen(
                friendId = friendId,
                onNavigateBack = { navController.popBackStack() },
                onGroupBalanceClick = { groupId, balance ->
                    // Navigate to Record Payment with pre-filled balance and friend as memberUid
                    navController.navigate("record_payment/$groupId?memberUid=$friendId&balance=$balance")
                },
                onMoreOptionsClick = {
                    // Navigate to Select Payer screen
                    navController.navigate("${Screen.SelectPayerFriend}/$friendId")
                }
            )
        }

        // Select Payer Friend Screen (More Options for Friend Settle Up)
        composable(
            route = Screen.SelectPayerFriendRoute,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            SelectPayerFriendScreen(
                friendId = friendId,
                onNavigateBack = { navController.popBackStack() },
                onPayerSelected = { payerUid, recipientUid ->
                    // Navigate to Record Payment with custom payer/recipient
                    // Use "non_group" as groupId for friend-to-friend payments
                    navController.navigate("record_payment/non_group?payerUid=$payerUid&recipientUid=$recipientUid")
                }
            )
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
                onNavigateToAddMembers = { navController.navigate("add_group_members/$groupId") },
                // --- Pass navigation action for editing group ---
                onNavigateToEditGroup = { navController.navigate("edit_group/$groupId") }
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
                navArgument("memberUid") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("balance") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("paymentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("payerUid") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("recipientUid") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val memberUid = backStackEntry.arguments?.getString("memberUid")
            val balance = backStackEntry.arguments?.getString("balance")
            val paymentId = backStackEntry.arguments?.getString("paymentId")
            val payerUid = backStackEntry.arguments?.getString("payerUid")
            val recipientUid = backStackEntry.arguments?.getString("recipientUid")

            RecordPaymentScreen(
                groupId = groupId,
                memberUid = memberUid ?: "",
                balance = balance ?: "0.0",
                paymentId = paymentId,
                payerUid = payerUid,
                recipientUid = recipientUid,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = {
                    // Check if we came from friend settle up or group settle up
                    val currentBackStack = navController.currentBackStack.value
                    val hasFriendSettleUp = currentBackStack.any { entry ->
                        entry.destination.route?.contains("select_balance_to_settle") == true ||
                        entry.destination.route?.contains("select_payer_friend") == true
                    }

                    if (hasFriendSettleUp) {
                        // Pop back to Friend Detail screen
                        navController.popBackStack("friend_detail/{friendId}", inclusive = false)
                    } else {
                        // Pop back to Group Detail screen
                        navController.popBackStack(Screen.SettleUpRoute, inclusive = true)
                    }
                }
            )
        }

        composable(
            route = Screen.MoreOptionsRoute,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            val groupId = it.arguments?.getString("groupId") ?: ""
            MoreOptionsScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecordPayment = { gid, payerUid, recipientUid ->
                    // Navigate to RecordPayment with custom payer and recipient
                    // Balance is set to 0.0 for manual entry, payerUid and recipientUid are passed
                    navController.navigate("${Screen.RecordPayment}/$gid?payerUid=$payerUid&recipientUid=$recipientUid&balance=0.0")
                }
            )
        }

        // --- Activity Detail Screen ---
        composable(
            route = Screen.ActivityDetailRoute,
            arguments = listOf(
                navArgument("activityId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            Log.d(
                "ActivityDetailNav",
                "Navigating to ActivityDetail - activityId: $activityId, expenseId: $expenseId"
            )
            ActivityDetailScreen(
                activityId = activityId,
                expenseId = expenseId,
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- Edit Profile Screen ---
        composable(
            route = Screen.EditProfile,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            EditProfileScreen(
                navController = navController
            )
        }

        // --- Blocked Users Screen ---
        composable(
            route = Screen.BlockedUsers,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            BlockedUsersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ReceiptScannerRoute,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""

            // Get parent NavBackStackEntry to share ViewModel
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.ReceiptScannerRoute.replace("{groupId}", groupId))
            }

            ReceiptScannerScreen(
                groupId = groupId,
                navController = navController,
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(parentEntry)
            )
        }

        composable(
            route = Screen.ReceiptScannerRoute,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ReceiptScannerScreen(
                groupId = groupId,
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ReceiptReviewRoute,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ReceiptReviewScreen(
                groupId = groupId,
                navController = navController
            )
        }
        composable(
            route = Screen.ReceiptReviewRoute,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""

            // Share ViewModel with scanner screen
            val scannerRoute = "${Screen.ReceiptScanner}/$groupId"
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(scannerRoute)
            }

            ReceiptReviewScreen(
                groupId = groupId,
                navController = navController,
                viewModel = viewModel(parentEntry)
            )
        }
        composable(
            route = "${Screen.Home}?tab={tab}",
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getString("tab")
            HomeScreen3(
                mainNavController = navController,
                initialTab = initialTab
            )
        }

    } // End NavHost
}

