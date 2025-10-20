package com.example.splitpay.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.splitpay.ui.groups.CreateGroupScreen
import com.example.splitpay.ui.groups.GroupDetailScreen
import com.example.splitpay.ui.home.HomeScreen3
import com.example.splitpay.ui.login.LoginScreen
import com.example.splitpay.ui.signup.SignUpScreen
import com.example.splitpay.ui.welcome.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.splitpay.navigation.slideInFromLeft
import com.example.splitpay.navigation.slideInFromRight
import com.example.splitpay.navigation.slideOutToLeft
import com.example.splitpay.navigation.slideOutToRight
import com.example.splitpay.ui.expense.AddExpenseScreen
import com.example.splitpay.ui.expense.AddExpenseUiEvent // NEW IMPORT

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
            HomeScreen3(mainNavController = navController)
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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- UPDATED ADD EXPENSE ROUTE ---
        composable(
            route = Screen.AddExpense, // This will be "add_expense?groupId={groupId}"
            arguments = listOf(navArgument("groupId") {
                type = NavType.StringType
                nullable = true
            }),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            // Extract the optional groupId
            val groupId = backStackEntry.arguments?.getString("groupId")

            AddExpenseScreen(
                // Pass the prefilled groupId to the ViewModel
                prefilledGroupId = groupId,

                // Standard back navigation
                onNavigateBack = { navController.popBackStack() },

                // Handle dynamic navigation on save
                onSaveSuccess = { navEvent ->
                    // This navEvent is the AddExpenseUiEvent.SaveSuccess
                    // We assume it has a boolean 'isGroupDetail'
                    if (navEvent.isGroupDetail) {
                        // If user was on GroupDetail and didn't change group,
                        // just pop back to it.
                        navController.popBackStack()
                    } else {
                        // Otherwise, navigate to the main Groups page
                        // and clear the back stack.
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Home) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}