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
import com.example.splitpay.ui.groups.GroupDetailScreen // <--- NEW IMPORT
import com.example.splitpay.ui.home.HomeScreen3
import com.example.splitpay.ui.login.LoginScreen
import com.example.splitpay.ui.signup.SignUpScreen
import com.example.splitpay.ui.welcome.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.splitpay.navigation.slideInFromLeft
import com.example.splitpay.navigation.slideInFromRight
import com.example.splitpay.navigation.slideOutToLeft
import com.example.splitpay.navigation.slideOutToRight

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

        // Route for Add Expense (FAB Destination)
        composable(
            route = Screen.AddExpense,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            // Placeholder Screen for Add Expense
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                Text("Add Expense Screen (WIP)", color = Color.White)
            }
        }
    }
}
