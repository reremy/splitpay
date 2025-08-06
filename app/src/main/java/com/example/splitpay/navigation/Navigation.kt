package com.example.splitpay.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.splitpay.ui.signup.SignUpScreen
import com.example.splitpay.ui.welcome.WelcomeScreen
import com.example.splitpay.ui.home.HomeScreen
import com.example.splitpay.ui.login.LoginScreen
import com.google.firebase.auth.FirebaseAuth

// Navigation.kt
@Composable
fun SplitPayNavHost(
    navController: NavHostController,
    startDestination: String = "welcome",
    onNavigateBack: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = "welcome",
//            enterTransition = {
//                slideInHorizontally(
//                    initialOffsetX = { it },
//                    animationSpec = tween(durationMillis = 500)
//                ) + fadeIn(animationSpec = tween(durationMillis = 500))
//            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis = 500)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = 500)
                )

            },
//            popExitTransition = {
//                slideOutHorizontally(
//                    targetOffsetX = { -it },
//                    animationSpec = tween(durationMillis = 500)
//                ) + fadeOut(animationSpec = tween(durationMillis = 500))
//            }
        ) {
            WelcomeScreen(
                onNavigateToSignUp = { navController.navigate("signup") },
                onNavigateToLogIn = { navController.navigate("login") }
            )
        }
        composable(
            route = "signup",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 500)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 500)
                )
            }
        ) {
            SignUpScreen(
                onNavigateToHome = { navController.navigate("home"){
                    popUpTo("welcome"){inclusive = true}
                    launchSingleTop = true
                } },
                onNavigateToBack = onNavigateBack
            )
        }
        composable(
            route = "login",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 500)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 500)
                )
            }
        ) {
            LoginScreen(
                onNavigateToHome = { navController.navigate("home"){
                    popUpTo("welcome"){inclusive = true}
                    launchSingleTop = true
                } },
                //onNavigateBack = onNavigateBack
            )
        }
        composable(
            route = "home",
        ) {
            HomeScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("welcome"){
                        popUpTo("welcome"){
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}