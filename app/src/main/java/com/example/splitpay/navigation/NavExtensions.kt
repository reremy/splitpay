package com.example.splitpay.navigation

import androidx.navigation.NavHostController

fun NavHostController.navigateSingleTopTo(route: String, popUpToRoute: String) {
    this.navigate(route) {
        popUpTo(popUpToRoute) { inclusive = true }
        launchSingleTop = true
    }
}
