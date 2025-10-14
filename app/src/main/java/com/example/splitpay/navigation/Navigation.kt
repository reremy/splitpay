package com.example.splitpay.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.splitpay.ui.groups.CreateGroupScreen
import com.example.splitpay.ui.home.HomeScreen3
import com.example.splitpay.ui.login.LoginScreen
import com.example.splitpay.ui.signup.SignUpScreen
import com.example.splitpay.ui.welcome.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth

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
//            enterTransition = {slideInFromRight() + fadeIn(animationSpec = tween(durationMillis = 500)) },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
//            popExitTransition = { slideOutToLeft() + fadeOut(animationSpec = tween(durationMillis = 500)) }
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
                onGroupCreated = { navController.popBackStack() }, // Go back to Home/Groups on success
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}